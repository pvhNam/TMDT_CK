package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC21 – Trao đổi qua tin nhắn. ViewModel dùng chung cho màn 05 (Tin nhắn) và 06 (Trò chuyện):
 * lắng nghe Firestore, gửi/gửi lại tin, đánh dấu đã đọc và trạng thái hoạt động.
 * Các Fragment chỉ hiển thị dữ liệu ở đây, không import Firebase.
 *
 * Dữ liệu:
 *  conversations/{uidA__uidB}            member_ids, members{uid:{ho_ten,vai_tro}}, last_message, last_sender_id,
 *                                        last_message_at, unread_count{uid:n}, read_at{uid:ts}, lesson{title,time,status},
 *                                        created_by, created_at, updated_at
 *  conversations/{id}/messages/{mid}     sender_id, client_message_id (= mid), content, sent_at
 *  presence/{uid}                        online, last_seen
 */
public final class ChatState extends ViewModel {
    public static final int MAX_LENGTH = 1000;
    private static final int PAGE = 300;
    private static final long ONLINE_WINDOW = 6 * 60 * 60 * 1000L;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);

    // Màn 05: danh sách hội thoại ------------------------------------------------------------------
    private final List<Conversation> conversations = new ArrayList<>();
    private ListenerRegistration listListener;
    private String listUid;
    private boolean listLoading;
    private String listError = "";
    private String query = "";
    private boolean unreadOnly;

    // Màn 06: hội thoại đang mở ---------------------------------------------------------------------
    private String openId, origin = "messages", presencePeer;
    private Conversation open;
    private final List<ChatMessage> serverMessages = new ArrayList<>();
    private final List<ChatMessage> messages = new ArrayList<>();
    /** Tin gửi lỗi, giữ theo mã tin để gửi lại đúng tin đó. */
    private final Map<String, ChatMessage> failed = new LinkedHashMap<>();
    private final Map<String, String> drafts = new HashMap<>();
    private ListenerRegistration conversationListener, messageListener, presenceListener;
    private boolean messagesLoading, visible, peerOnline;
    private long peerLastSeen, markedAt = -1;
    private String chatError = "", notice = "";
    /** Tăng mỗi lần đóng/mở hội thoại để bỏ qua kết quả trả về muộn của hội thoại cũ. */
    private int generation;

    public LiveData<Integer> changes() { return changes; }
    private void notifyUi() { changes.setValue(changes.getValue() == null ? 1 : changes.getValue() + 1); }
    private String uid() { FirebaseUser user = auth.getCurrentUser(); return user == null ? null : user.getUid(); }
    public boolean signedIn() { return uid() != null; }
    /** Mã người đang đăng nhập (để giao diện phân biệt tin của mình), rỗng nếu là khách. */
    public String myId() { String me = uid(); return me == null ? "" : me; }

    // ----------------------------------------------------------------------------------------------
    // Danh sách hội thoại
    // ----------------------------------------------------------------------------------------------

    /** Bắt đầu lắng nghe hội thoại của người đang đăng nhập; gọi lại nhiều lần không tạo listener trùng. */
    public void start() {
        final String me = uid();
        if (me == null) { reset(); return; }
        if (me.equals(listUid) && listListener != null) return;
        stopList();
        listUid = me; listLoading = true; listError = ""; notifyUi();
        presence(true);
        // Truy vấn array-contains uid: Firestore Rules chỉ trả về hội thoại có người dùng là thành viên.
        listListener = db.collection("conversations").whereArrayContains("member_ids", me)
                .addSnapshotListener((snapshot, error) -> {
                    if (!me.equals(uid()) || !me.equals(listUid)) return;
                    listLoading = false;
                    if (error != null || snapshot == null) {
                        listError = message(error); listListener = null; notifyUi(); return;
                    }
                    conversations.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Conversation item = parse(doc, me);
                        // Hội thoại chưa có tin chỉ hiện với người đã mở nó.
                        if (item.lastMessage.isEmpty() && !me.equals(item.createdBy)) continue;
                        conversations.add(item);
                    }
                    Collections.sort(conversations, (a, b) -> Long.compare(b.sortTime(), a.sortTime()));
                    listError = "";
                    notifyUi();
                });
    }
    public void retryList() { stopList(); start(); }
    private void stopList() {
        if (listListener != null) listListener.remove();
        listListener = null; listUid = null; conversations.clear(); listLoading = false;
    }
    /** Đăng xuất: bỏ mọi listener và dữ liệu của tài khoản cũ. */
    public void reset() {
        boolean had = listUid != null || openId != null;
        close(); stopList(); drafts.clear(); query = ""; unreadOnly = false; listError = "";
        if (had) notifyUi();
    }

    public boolean listLoading() { return listLoading; }
    public String listError() { return listError; }
    public String query() { return query; }
    public void query(String value) { query = value == null ? "" : value; notifyUi(); }
    public boolean unreadOnly() { return unreadOnly; }
    public void unreadOnly(boolean value) { if (unreadOnly != value) { unreadOnly = value; notifyUi(); } }
    public boolean hasConversations() { return !conversations.isEmpty(); }
    /** Tất cả hội thoại của người dùng (dùng khi chọn đối tượng báo cáo vi phạm). */
    public List<Conversation> conversations() { return Collections.unmodifiableList(conversations); }

    /** Hội thoại khớp tên tìm kiếm (không dấu) và bộ lọc "Chưa đọc". */
    public List<Conversation> results() {
        String keyword = CatalogState.normalize(query);
        List<Conversation> result = new ArrayList<>();
        for (Conversation item : conversations) {
            if (unreadOnly && item.unread == 0) continue;
            if (!keyword.isEmpty() && !CatalogState.normalize(item.peerName).contains(keyword)) continue;
            result.add(item);
        }
        return result;
    }
    /** Tổng số tin chưa đọc, dùng cho huy hiệu trên thanh điều hướng. */
    public int unreadTotal() { int total = 0; for (Conversation item : conversations) total += item.unread; return total; }

    // ----------------------------------------------------------------------------------------------
    // Mở / đóng hội thoại
    // ----------------------------------------------------------------------------------------------

    /** Mở hội thoại có sẵn từ danh sách. */
    public void open(Conversation item) {
        if (item == null || uid() == null) return;
        begin(item.id, item, "messages");
        attach(item.id);
    }

    /**
     * Mở (tạo nếu chưa có) hội thoại với một người dùng, ví dụ từ thẻ buổi học hoặc hồ sơ gia sư.
     * @param lesson buổi học liên quan để hiển thị trong hội thoại, có thể null
     * @return false nếu chưa đăng nhập hoặc không xác định được người nhận
     */
    public boolean openWith(String peerId, String peerName, String peerRole, String myName, String myRole,
                            Map<String, Object> lesson, String from) {
        final String me = uid();
        if (me == null || peerId == null || peerId.isEmpty() || peerId.equals(me)) return false;
        final String id = Conversation.idFor(me, peerId);
        Conversation known = find(id);
        Conversation preview = known != null ? known : new Conversation();
        preview.id = id; preview.peerId = peerId;
        if (preview.peerName.isEmpty()) preview.peerName = peerName;
        if (preview.peerRole.isEmpty()) preview.peerRole = peerRole;
        if (lesson != null) {
            preview.lessonTitle = String.valueOf(lesson.get("title"));
            preview.lessonTime = String.valueOf(lesson.get("time"));
            preview.lessonStatus = String.valueOf(lesson.get("status"));
        }
        begin(id, preview, from);
        final int token = generation;
        final DocumentReference ref = db.collection("conversations").document(id);
        if (known != null) {
            // Đã có hội thoại: chỉ cập nhật buổi học liên quan (ghi được cả khi ngoại tuyến).
            if (lesson != null) {
                Map<String, Object> update = new HashMap<>();
                update.put("lesson", lesson); update.put("updated_at", FieldValue.serverTimestamp());
                ref.update(update);
            }
            attach(id);
            return true;
        }
        db.runTransaction(tx -> {
            DocumentSnapshot snapshot = tx.get(ref);
            if (!snapshot.exists()) {
                List<String> ids = new ArrayList<>(Arrays.asList(me, peerId)); Collections.sort(ids);
                Map<String, Object> members = new HashMap<>();
                members.put(me, member(myName, myRole)); members.put(peerId, member(peerName, peerRole));
                Map<String, Object> unread = new HashMap<>(); unread.put(me, 0); unread.put(peerId, 0);
                Map<String, Object> data = new HashMap<>();
                data.put("member_ids", ids); data.put("members", members);
                data.put("last_message", ""); data.put("last_sender_id", ""); data.put("last_message_at", null);
                data.put("unread_count", unread); data.put("read_at", new HashMap<String, Object>());
                if (lesson != null) data.put("lesson", lesson);
                data.put("created_by", me);
                data.put("created_at", FieldValue.serverTimestamp()); data.put("updated_at", FieldValue.serverTimestamp());
                tx.set(ref, data);
            } else {
                Map<String, Object> update = new HashMap<>();
                if (lesson != null) update.put("lesson", lesson);
                if (myName != null && !myName.isEmpty()) update.put("members." + me + ".ho_ten", myName);
                if (!update.isEmpty()) { update.put("updated_at", FieldValue.serverTimestamp()); tx.update(ref, update); }
            }
            return null;
        }).addOnCompleteListener(task -> {
            if (token != generation) return;
            if (!task.isSuccessful()) { messagesLoading = false; chatError = message(task.getException()); notifyUi(); return; }
            attach(id);
        });
        return true;
    }
    private static Map<String, Object> member(String name, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("ho_ten", name == null ? "" : name); data.put("vai_tro", role == null ? "" : role);
        return data;
    }
    private Conversation find(String id) { for (Conversation item : conversations) if (item.id.equals(id)) return item; return null; }

    private void begin(String id, Conversation preview, String from) {
        close();
        openId = id; open = preview; origin = from == null ? "messages" : from;
        messagesLoading = true; chatError = ""; markedAt = -1;
        notifyUi();
    }

    private void attach(final String id) {
        final String me = uid();
        if (me == null) return;
        final int token = generation;
        DocumentReference ref = db.collection("conversations").document(id);
        conversationListener = ref.addSnapshotListener((snapshot, error) -> {
            if (token != generation) return;
            if (error != null) { chatError = message(error); notifyUi(); return; }
            if (snapshot == null || !snapshot.exists()) return;
            open = parse(snapshot, me);
            watchPresence(open.peerId, token);
            rebuild(); markRead(); notifyUi();
        });
        messageListener = ref.collection("messages").orderBy("sent_at", Query.Direction.ASCENDING).limitToLast(PAGE)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (token != generation) return;
                    messagesLoading = false;
                    if (error != null || snapshot == null) { chatError = message(error); notifyUi(); return; }
                    serverMessages.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        ChatMessage item = new ChatMessage();
                        item.id = doc.getId(); item.senderId = text(doc.get("sender_id")); item.content = text(doc.get("content"));
                        item.sentAt = millis(doc.getTimestamp("sent_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE));
                        if (item.sentAt == 0) item.sentAt = System.currentTimeMillis();
                        item.mine = me.equals(item.senderId);
                        boolean pending = doc.getMetadata().hasPendingWrites();
                        item.status = pending ? ChatMessage.SENDING : ChatMessage.SENT;
                        if (!pending) failed.remove(item.id);
                        serverMessages.add(item);
                    }
                    chatError = "";
                    rebuild(); markRead(); notifyUi();
                });
    }

    /** Rời màn trò chuyện: gỡ listener, giữ bản nháp theo từng hội thoại. */
    public void close() {
        generation++;
        if (conversationListener != null) conversationListener.remove();
        if (messageListener != null) messageListener.remove();
        if (presenceListener != null) presenceListener.remove();
        conversationListener = null; messageListener = null; presenceListener = null; presencePeer = null;
        openId = null; open = null; serverMessages.clear(); messages.clear(); failed.clear();
        messagesLoading = false; peerOnline = false; peerLastSeen = 0; chatError = ""; visible = false;
    }

    public String openId() { return openId; }
    public String origin() { return origin; }
    public Conversation conversation() { return open; }
    public List<ChatMessage> messages() { return Collections.unmodifiableList(messages); }
    public boolean messagesLoading() { return messagesLoading; }
    public String chatError() { return chatError; }
    public String draft() { return openId == null ? "" : drafts.getOrDefault(openId, ""); }
    public void draft(String value) { if (openId != null) drafts.put(openId, value); }
    /** Thông báo một lần (lỗi gửi, giới hạn độ dài…); trả về rỗng nếu không có. */
    public String takeNotice() { String value = notice; notice = ""; return value; }

    /** Màn trò chuyện đang hiển thị: chỉ khi đó mới đánh dấu đã đọc. */
    public void visible(boolean value) { visible = value; if (value) markRead(); }

    // ----------------------------------------------------------------------------------------------
    // Gửi tin
    // ----------------------------------------------------------------------------------------------

    /** @return true nếu tin đã được đưa vào hàng gửi (ô nhập có thể xóa). */
    public boolean send(String raw) {
        String me = uid();
        String content = raw == null ? "" : raw.trim();
        if (me == null || openId == null || content.isEmpty()) return false;
        if (content.length() > MAX_LENGTH) { notice = "Tin nhắn tối đa " + MAX_LENGTH + " ký tự."; notifyUi(); return false; }
        if (open == null || open.peerId.isEmpty()) { notice = "Đang mở cuộc trò chuyện, vui lòng thử lại."; notifyUi(); return false; }
        String id = db.collection("conversations").document(openId).collection("messages").document().getId();
        ChatMessage item = new ChatMessage(id, me, content, System.currentTimeMillis(), ChatMessage.SENDING, true);
        failed.put(id, item);  // Giữ bản cục bộ cho tới khi máy chủ xác nhận.
        drafts.remove(openId);
        write(item);
        rebuild(); notifyUi();
        return true;
    }

    /** Gửi lại tin lỗi. Kiểm tra máy chủ trước để không ghi nhận một tin hai lần. */
    public void retry(String messageId) {
        final ChatMessage item = failed.get(messageId);
        if (item == null || openId == null || item.status == ChatMessage.SENDING) return;
        item.status = ChatMessage.SENDING; rebuild(); notifyUi();
        final int token = generation;
        db.collection("conversations").document(openId).collection("messages").document(messageId)
                .get(Source.SERVER).addOnCompleteListener(task -> {
                    if (token != generation) return;
                    if (!task.isSuccessful()) { item.status = ChatMessage.FAILED; notice = message(task.getException()); rebuild(); notifyUi(); return; }
                    if (task.getResult() != null && task.getResult().exists()) { failed.remove(messageId); rebuild(); notifyUi(); return; }
                    write(item);
                });
    }

    private void write(final ChatMessage item) {
        final String me = uid();
        if (me == null || openId == null || open == null) return;
        final int token = generation;
        DocumentReference conversation = db.collection("conversations").document(openId);
        Map<String, Object> data = new HashMap<>();
        data.put("sender_id", me); data.put("client_message_id", item.id);
        data.put("content", item.content); data.put("sent_at", FieldValue.serverTimestamp());
        Map<String, Object> update = new HashMap<>();
        update.put("last_message", item.content); update.put("last_sender_id", me);
        update.put("last_message_at", FieldValue.serverTimestamp());
        update.put("unread_count." + open.peerId, FieldValue.increment(1));
        update.put("read_at." + me, FieldValue.serverTimestamp());
        update.put("updated_at", FieldValue.serverTimestamp());
        // Tin và tóm tắt hội thoại được ghi cùng lúc: hoặc cả hai, hoặc không.
        WriteBatch batch = db.batch();
        batch.set(conversation.collection("messages").document(item.id), data);
        batch.update(conversation, update);
        batch.commit().addOnFailureListener(e -> {
            if (token != generation) return;
            item.status = ChatMessage.FAILED; failed.put(item.id, item);
            notice = "Không gửi được tin nhắn. " + message(e);
            rebuild(); notifyUi();
        });
    }

    /** Ghép tin từ máy chủ với tin cục bộ chưa xác nhận/lỗi, rồi tính dấu "đã xem". */
    private void rebuild() {
        messages.clear();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (ChatMessage item : serverMessages) { messages.add(item); ids.add(item.id); }
        for (ChatMessage item : failed.values()) if (!ids.contains(item.id)) messages.add(item);
        Collections.sort(messages, (a, b) -> Long.compare(a.sentAt, b.sentAt));
        long readAt = open == null ? 0 : open.peerReadAt;
        for (ChatMessage item : messages)
            if (item.mine && item.status == ChatMessage.SENT && readAt >= item.sentAt) item.status = ChatMessage.READ;
    }

    private void markRead() {
        String me = uid();
        if (!visible || me == null || open == null || openId == null || !openId.equals(open.id)) return;
        boolean fromPeer = !open.lastSenderId.isEmpty() && !me.equals(open.lastSenderId);
        boolean behind = open.unread > 0 || (fromPeer && open.lastMessageAt > open.myReadAt);
        if (!behind || markedAt == open.lastMessageAt) return;
        markedAt = open.lastMessageAt;
        Map<String, Object> update = new HashMap<>();
        update.put("unread_count." + me, 0);
        update.put("read_at." + me, FieldValue.serverTimestamp());
        db.collection("conversations").document(openId).update(update);
    }

    // ----------------------------------------------------------------------------------------------
    // Trạng thái hoạt động
    // ----------------------------------------------------------------------------------------------

    /** Gọi khi ứng dụng vào/ra nền. */
    public void presence(boolean online) {
        String me = uid();
        if (me == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("online", online); data.put("last_seen", FieldValue.serverTimestamp());
        db.collection("presence").document(me).set(data);
    }
    private void watchPresence(String peer, final int token) {
        if (peer == null || peer.isEmpty() || peer.equals(presencePeer)) return;
        if (presenceListener != null) presenceListener.remove();
        presencePeer = peer;
        presenceListener = db.collection("presence").document(peer).addSnapshotListener((snapshot, error) -> {
            if (token != generation || error != null) return;
            peerOnline = snapshot != null && snapshot.exists() && Boolean.TRUE.equals(snapshot.getBoolean("online"));
            peerLastSeen = snapshot == null ? 0 : millis(snapshot.getTimestamp("last_seen", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE));
            notifyUi();
        });
    }
    /** Trạng thái chỉ "đang hoạt động" khi lần cập nhật gần đây (ứng dụng bị tắt đột ngột không giữ mãi). */
    public boolean peerOnline() { return peerOnline && System.currentTimeMillis() - peerLastSeen < ONLINE_WINDOW; }
    public long peerLastSeen() { return peerLastSeen; }

    // ----------------------------------------------------------------------------------------------
    // Đọc dữ liệu
    // ----------------------------------------------------------------------------------------------

    static Conversation parse(DocumentSnapshot doc, String me) {
        Conversation item = new Conversation();
        item.id = doc.getId();
        Object ids = doc.get("member_ids");
        if (ids instanceof List) for (Object value : (List<?>) ids) if (value instanceof String && !value.equals(me)) item.peerId = (String) value;
        Map<?, ?> peer = map(map(doc.get("members")).get(item.peerId));
        item.peerName = text(peer.get("ho_ten"));
        item.peerRole = text(peer.get("vai_tro"));
        item.myRole = text(map(map(doc.get("members")).get(me)).get("vai_tro"));
        if (item.peerName.isEmpty()) item.peerName = "Người dùng";
        item.lastMessage = text(doc.get("last_message"));
        item.lastSenderId = text(doc.get("last_sender_id"));
        item.createdBy = text(doc.get("created_by"));
        DocumentSnapshot.ServerTimestampBehavior estimate = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE;
        item.lastMessageAt = millis(doc.getTimestamp("last_message_at", estimate));
        item.createdAt = millis(doc.getTimestamp("created_at", estimate));
        Object unread = map(doc.get("unread_count")).get(me);
        item.unread = unread instanceof Number ? Math.max(0, ((Number) unread).intValue()) : 0;
        Map<?, ?> read = map(doc.get("read_at", estimate));
        item.peerReadAt = millis(read.get(item.peerId));
        item.myReadAt = millis(read.get(me));
        Map<?, ?> lesson = map(doc.get("lesson"));
        item.lessonTitle = text(lesson.get("title")); item.lessonTime = text(lesson.get("time")); item.lessonStatus = text(lesson.get("status"));
        return item;
    }
    private static Map<?, ?> map(Object value) { return value instanceof Map ? (Map<?, ?>) value : Collections.emptyMap(); }
    private static String text(Object value) { return value instanceof String ? (String) value : ""; }
    private static long millis(Object value) { return value instanceof Timestamp ? ((Timestamp) value).toDate().getTime() : 0; }

    static String message(Exception e) {
        if (e instanceof FirebaseNetworkException) return "Không có kết nối mạng. Vui lòng thử lại.";
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) e).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return "Bạn không có quyền truy cập cuộc trò chuyện này.";
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) return "Không kết nối được máy chủ. Vui lòng thử lại.";
            if (code == FirebaseFirestoreException.Code.UNAUTHENTICATED) return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        return "Không hoàn tất được yêu cầu. Vui lòng thử lại.";
    }

    @Override protected void onCleared() { close(); stopList(); }
}
