package com.example.tmdt;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;

/** Release reads the real ledger; the native sandbox uses a separate Firestore namespace. */
public final class PaymentState extends ViewModel {
    public static final class Row {
        public final String id;public final Map<String,Object> data;
        public Row(String id,Map<String,Object> data){this.id=id;this.data=data;}
        public String text(String key){Object v=data.get(key);return v instanceof String?(String)v:"";}
        public long number(String key){Object v=data.get(key);return v instanceof Number?((Number)v).longValue():0;}
        public long time(String key){Object v=data.get(key);return v instanceof com.google.firebase.Timestamp?((com.google.firebase.Timestamp)v).toDate().getTime():0;}
        public boolean flag(String key){return Boolean.TRUE.equals(data.get(key));}
        public Map<String,Object> map(String key){Object v=data.get(key);return v instanceof Map?(Map<String,Object>)v:Collections.emptyMap();}
    }
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final FirebaseAuth auth=FirebaseAuth.getInstance();
    PaymentApi api=new PaymentApi();
    public final MutableLiveData<Integer> changes=new MutableLiveData<>(0);
    public final Map<String,Row> payments=new LinkedHashMap<>(),earnings=new LinkedHashMap<>(),payouts=new LinkedHashMap<>(),refunds=new LinkedHashMap<>(),sessions=new TreeMap<>();
    private final Map<String,Map<String,Row>> sources=new HashMap<>();
    private final List<ListenerRegistration> listeners=new ArrayList<>();
    private ListenerRegistration sessionListener,bankListener;
    private String sessionOwner="",key="";private int generation;
    public String owner="",selected="",selectedPayout="",selectedRefund="",requestedType="",requestedSource="",requestedStudent="",origin="account",destination,error="",notice="";
    public String incomePeriod=YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString(),payrollPeriod=YearMonth.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMonths(1).toString();
    public boolean admin,tutor,busy;
    public Map<String,Object> bank=new HashMap<>();
    public void account(AccountState account){
        UserProfile profile=account.profile();
        String next=account.signedIn()&&profile!=null&&"ACTIVE".equals(profile.status)?account.uid():"";
        String nextKey=next+(profile==null?"":profile.role+profile.tutor);
        if(nextKey.equals(key))return;key=nextKey;generation++;
        for(ListenerRegistration listener:listeners)listener.remove();listeners.clear();
        if(sessionListener!=null)sessionListener.remove();sessionListener=null;sessionOwner="";
        if(bankListener!=null)bankListener.remove();bankListener=null;
        owner=next;admin=profile!=null&&"ADMIN".equals(profile.role);tutor=profile!=null&&profile.tutor;
        payments.clear();earnings.clear();payouts.clear();refunds.clear();sessions.clear();sources.clear();bank.clear();busy=false;error="";notice="";
        if(owner.isEmpty()){selected="";changed();return;}
        if(admin){listen("payments",db.collection(api.collection("payments")),payments);listen("payouts",db.collection(api.collection("tutor_payouts")),payouts);listen("refunds",db.collection(api.collection("refund_requests")),refunds);}
        else{
            listen("student-payments",db.collection(api.collection("payments")).whereEqualTo("student_id",owner),payments);
            listen("tutor-payments",db.collection(api.collection("payments")).whereEqualTo("tutor_id",owner),payments);
            listen("payouts",db.collection(api.collection("tutor_payouts")).whereEqualTo("tutor_id",owner),payouts);
            listen("refunds",db.collection(api.collection("refund_requests")).whereEqualTo("student_id",owner),refunds);
        }
        listen("earnings",admin?db.collection(api.collection("tutor_earnings")):db.collection(api.collection("tutor_earnings")).whereEqualTo("tutor_id",owner),earnings);
        final int version=generation;
        bankListener=db.collection(api.collection("payout_accounts")).document(owner).addSnapshotListener((doc,failure)->{
            if(version!=generation)return;
            if(failure!=null){failRead(failure);return;}
            bank=doc!=null&&doc.exists()?doc.getData():new HashMap<>();changed();
        });changed();
    }
    private void listen(String name,Query query,Map<String,Row> target){
        final int version=generation;
        listeners.add(query.addSnapshotListener((snapshot,failure)->{
            if(version!=generation)return;
            if(failure!=null){failRead(failure);return;}
            Map<String,Row> rows=new LinkedHashMap<>();if(snapshot!=null)for(DocumentSnapshot doc:snapshot)rows.put(doc.getId(),new Row(doc.getId(),doc.getData()));
            sources.put(name,rows);target.clear();
            if(target==payments){for(Map.Entry<String,Map<String,Row>> source:sources.entrySet())if(source.getKey().equals("payments")||source.getKey().endsWith("-payments"))target.putAll(source.getValue());}
            else target.putAll(rows);
            watchSessions();changed();
        }));
    }
    private void watchSessions(){
        String next=payments.containsKey(selected)?selected:"";
        if(next.equals(sessionOwner))return;
        if(sessionListener!=null)sessionListener.remove();sessionListener=null;sessionOwner=next;sessions.clear();
        if(next.isEmpty())return;final int version=generation;
        sessionListener=db.collection(api.collection("payments")).document(next).collection("sessions").addSnapshotListener((snapshot,failure)->{
            if(version!=generation||!next.equals(sessionOwner))return;
            if(failure!=null){failRead(failure);return;}
            sessions.clear();if(snapshot!=null)for(DocumentSnapshot doc:snapshot)sessions.put(doc.getId(),new Row(doc.getId(),doc.getData()));changed();
        });
    }
    private void failRead(Exception e){error=e instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)e).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED?
            "Chưa có quyền xem thanh toán. Vui lòng liên hệ quản trị viên để cập nhật cấu hình truy cập.":"Chưa tải được dữ liệu thanh toán. Kiểm tra kết nối và thử lại.";changed();}
    public void open(String type,String source,String from){open(type,source,from,owner);}
    public void open(String type,String source,String from,String student){requestedType=type;requestedSource=source;requestedStudent=student;selected=id(type,source);origin=from;error="";notice="";destination="payment_detail";watchSessions();changed();}
    public void openExisting(String id,String from){selected=id;requestedType="";requestedSource="";origin=from;error="";notice="";destination="payment_detail";watchSessions();changed();}
    public Row current(){return payments.get(selected);}
    public Row paymentFor(String type,String source){return payments.get(id(type,source));}
    public boolean paid(String type,String source){Row row=paymentFor(type,source);return row!=null&&"PAID".equals(row.text("status"));}
    public boolean configured(){return api.configured();}
    public boolean sandbox(){return api.sandbox();}
    public void returned(String paymentId,String url){
        if(!sandbox())return;
        request("POST","/payments/"+paymentId+"/return",json("url",url),r->{String status=r.optString("status");notice=status.equals("PAID")?"Thanh toán TEST thành công. Học phí thử đã được ghi nhận.":"Kết quả VNPay TEST: "+status(status);});
    }
    public void changed(){changes.setValue(changes.getValue()+1);}
    public void clearFeedback(){error="";notice="";}
    public void checkout(Consumer<String> open){
        Row row=current();String type=row==null?requestedType:row.text("type"),source=row==null?requestedSource:row.text("source_id");
        request("POST","/payments",json("type",type,"source_id",source),result->{String url=result.optString("checkoutUrl");if(!url.isEmpty())open.accept(url);else notice="Trạng thái thanh toán đã được cập nhật.";});
    }
    public void refresh(){request("POST","/payments/"+selected+"/refresh",json(),result->notice=sandbox()?result.optString("message"):"Đã đối chiếu trạng thái với VNPay.");}
    public void session(int number,String action,String reason){
        request("POST","/payments/"+selected+"/sessions/"+number+"/"+action,json("reason",reason),result->notice=action.equals("confirm")?"Đã xác nhận. Tiền dạy được ghi nhận vào kỳ tháng này.":"Đã cập nhật buổi học.");
    }
    public void saveBank(String name,String number,String holder){request("PUT","/bank",json("bank_name",name,"account_number",number,"account_name",holder),r->notice="Đã lưu tài khoản nhận tiền.");}
    public void preparePayroll(){request("POST","/admin/payroll",json("period",payrollPeriod),r->notice="Đã lập "+r.optInt("created")+" bảng chi trả. Buổi đã đưa vào bảng sẽ không được tính lại.");}
    public void markPayout(String reference){request("POST","/admin/payouts/"+selectedPayout+"/paid",json("reference",reference),r->notice="Đã ghi nhận khoản chuyển tiền cho gia sư.");}
    public void cancelPayout(String reason){request("POST","/admin/payouts/"+selectedPayout+"/cancel",json("reason",reason),r->notice="Đã hủy bảng chưa chi. Cập nhật tài khoản nhận tiền rồi lập lại kỳ thanh toán.");}
    public void refund(String reason){request("POST","/payments/"+selected+"/refund",json("reason",reason),r->notice="Đã gửi yêu cầu. Khóa tạm dừng ghi nhận buổi mới trong lúc xử lý hoàn tiền.");}
    public void finishRefund(String reference){request("POST","/admin/refunds/"+selectedRefund+"/paid",json("reference",reference),r->notice="Đã ghi nhận hoàn tiền cho học sinh.");}
    public void rejectRefund(String reason){request("POST","/admin/refunds/"+selectedRefund+"/reject",json("reason",reason),r->notice="Đã cập nhật kết quả đối soát yêu cầu hoàn tiền.");}
    private void request(String method,String path,JSONObject body,Consumer<JSONObject> success){
        if(busy)return;busy=true;clearFeedback();changed();final int version=generation;
        api.request(auth.getCurrentUser(),method,path,body).addOnCompleteListener(task->{
            if(version!=generation)return;busy=false;
            if(task.isSuccessful())success.accept(task.getResult());else error=task.getException() instanceof PaymentApi.Failure?task.getException().getMessage():"Phiên đăng nhập chưa hợp lệ. Hãy đăng nhập lại.";
            changed();
        });
    }
    public static JSONObject json(Object...pairs){JSONObject value=new JSONObject();try{for(int i=0;i<pairs.length;i+=2)value.put((String)pairs[i],pairs[i+1]);}catch(Exception e){throw new IllegalArgumentException(e);}return value;}
    public static String id(String type,String source){
        try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte value:bytes)hex.append(String.format(Locale.ROOT,"%02x",value&255));return type.toLowerCase(Locale.ROOT)+"_"+hex.substring(0,32);}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static String money(long value){return String.format(new Locale("vi","VN"),"%,d đ",value);}
    public static String status(String value){switch(value){
        case "CREATING":return "Đang tạo phiên";case "WAITING":return "Chờ thanh toán";case "PAID":return "Đã thanh toán";
        case "PAID_REVIEW":return "Đã nhận tiền · Cần đối soát";case "REFUND_REQUESTED":return "Chờ hoàn tiền";case "CLOSED_REFUNDED":case "REFUNDED":return "Đã hoàn tiền";
        case "EXPIRED":return "Phiên hết hạn";case "CANCELLED":return "Phiên đã hủy";case "ERROR":return "Chưa tạo được phiên";case "FAILED":return "Thanh toán thất bại";
        case "GATEWAY_REVIEW":return "VNPay đang cần đối soát";
        case "READY":return "Chờ chi trả";case "PROPOSED":return "Chờ học sinh xác nhận";case "COMPLETED":return "Đã hoàn thành";case "DISPUTED":return "Có phản ánh";
        case "PENDING":return "Chờ xử lý";case "REJECTED":return "Đã từ chối";
        default:return value;
    }}
    public static String payoutStatus(String value){return "PAID".equals(value)?"Đã chi trả":"CANCELLED".equals(value)?"Đã hủy bảng chi":status(value);}
    @Override protected void onCleared(){for(ListenerRegistration listener:listeners)listener.remove();if(sessionListener!=null)sessionListener.remove();if(bankListener!=null)bankListener.remove();super.onCleared();}
}
