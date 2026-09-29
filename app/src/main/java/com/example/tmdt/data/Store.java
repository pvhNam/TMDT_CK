package com.example.tmdt.data;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.example.tmdt.R;

/**
 * Lessons and group classes of the demo, saved on the device as JSON.
 * Every rule of "Lớp học và lịch học" lives here so the screens only display the result.
 */
public final class Store {
    /** The signed-in student and the tutor shown in tutor mode (sample accounts). */
    public static final String STUDENT = "Phạm Nam";
    public static final int TUTOR = 0;
    private static final String KEY = "classroom_v1";

    public final List<Lesson> lessons = new ArrayList<>();
    public final List<GroupClass> classes = new ArrayList<>();
    private final SharedPreferences preferences;
    private int nextId = 1;

    public Store(SharedPreferences preferences) {
        this.preferences = preferences;
        try {
            JSONObject data = new JSONObject(preferences.getString(KEY, ""));
            JSONArray lessonList = data.getJSONArray("lessons"), classList = data.getJSONArray("classes");
            for (int i = 0; i < lessonList.length(); i++) lessons.add(Lesson.fromJson(lessonList.getJSONObject(i), 1000+i));
            for (int i = 0; i < classList.length(); i++) classes.add(GroupClass.fromJson(classList.getJSONObject(i)));
            nextId = data.getInt("nextId");
        } catch (JSONException | java.time.DateTimeException invalid) {
            // First launch or unreadable storage: start again from the sample data.
            lessons.clear(); classes.clear(); nextId = 1; seed(); importOldRequests(); save();
        }
    }

    /** Keeps the booking requests saved by the first version of the app (key "requests"). */
    private void importOldRequests() {
        try {
            JSONArray items = new JSONArray(preferences.getString("requests", "[]"));
            for (int i = 0; i < items.length(); i++) {
                try { lessons.add(Lesson.fromJson(items.getJSONObject(i), nextId())); }
                catch (JSONException | java.time.DateTimeException ignored) { /* Skip an invalid local entry. */ }
            }
        } catch (JSONException ignored) { /* Nothing to import. */ }
    }

    public int nextId() { return nextId++; }

    public void save() {
        try {
            JSONArray lessonList = new JSONArray(), classList = new JSONArray();
            for (Lesson lesson : lessons) lessonList.put(lesson.toJson());
            for (GroupClass item : classes) classList.put(item.toJson());
            JSONObject data = new JSONObject(); data.put("lessons",lessonList); data.put("classes",classList); data.put("nextId",nextId);
            preferences.edit().putString(KEY, data.toString()).apply();
        } catch (JSONException ignored) { /* Values are plain strings and numbers; this cannot fail in practice. */ }
    }

    public Lesson lesson(int id) { for (Lesson lesson : lessons) if (lesson.id == id) return lesson; return null; }
    public GroupClass groupClass(int id) { for (GroupClass item : classes) if (item.id == id) return item; return null; }

    public static int tutorPhoto(int tutorId) { return tutorId == 0 ? R.drawable.photo_minhanh : R.drawable.photo_hoangnam; }
    public static int studentPhoto(String student) { return student.equals("Ngọc Mai") ? R.drawable.photo_ngocmai : R.drawable.photo_hoangnam; }

    // Lessons ---------------------------------------------------------------------------------

    /** Returns a message when the slot overlaps the student's own lessons or the tutor's confirmed lessons. */
    public String conflict(int tutorId, String student, String date, int hour, int minutes, int ignoreId) {
        for (Lesson other : lessons) {
            if (other.id == ignoreId || !other.active() || !other.overlaps(date,hour,minutes)) continue;
            if (other.student.equals(student)) return "Bạn đã có buổi học hoặc yêu cầu trong khung giờ này. Hãy chọn giờ khác.";
            if (other.tutorId == tutorId && other.confirmed()) return "Gia sư đã có lịch dạy trong khung giờ này. Hãy chọn giờ khác.";
        }
        return null;
    }

    public String acceptRequest(Lesson lesson) {
        if (!lesson.pending()) return "Yêu cầu này đã được xử lý.";
        if (lesson.started()) return "Thời gian học đã qua, không thể chấp nhận.";
        for (Lesson other : lessons)
            if (other != lesson && other.tutorId == lesson.tutorId && other.confirmed() && other.overlaps(lesson.date,lesson.hour,lesson.minutes))
                return "Khung giờ này trùng với một buổi dạy đã xác nhận.";
        lesson.status = Lesson.CONFIRMED; save(); return null;
    }
    public String rejectRequest(Lesson lesson) {
        if (!lesson.pending()) return "Yêu cầu này đã được xử lý.";
        lesson.status = Lesson.REJECTED; save(); return null;
    }

    public String propose(Lesson lesson, LocalDate date, int hour, String reason) {
        if (!lesson.confirmed() || lesson.started()) return "Chỉ đề nghị đổi lịch cho buổi học đã xác nhận và chưa diễn ra.";
        if (lesson.proposal()) return "Buổi học này đang chờ học viên phản hồi một đề nghị khác.";
        if (!date.atTime(hour,0).isAfter(LocalDateTime.now())) return "Thời gian đề xuất cần ở trong tương lai.";
        if (date.toString().equals(lesson.date) && hour == lesson.hour) return "Thời gian đề xuất đang trùng với lịch hiện tại.";
        String clash = conflict(lesson.tutorId, lesson.student, date.toString(), hour, lesson.minutes, lesson.id);
        if (clash != null) return clash;
        lesson.proposedDate = date.toString(); lesson.proposedHour = hour; lesson.proposalReason = reason; save(); return null;
    }
    /** The lesson only moves after the student agrees and the proposed slot is still free. */
    public String acceptProposal(Lesson lesson) {
        if (!lesson.proposal()) return "Đề nghị đổi lịch không còn hiệu lực.";
        String clash = conflict(lesson.tutorId, lesson.student, lesson.proposedDate, lesson.proposedHour, lesson.minutes, lesson.id);
        if (clash != null) return "Khung giờ đề xuất không còn trống. Lịch hiện tại được giữ nguyên.";
        lesson.date = lesson.proposedDate; lesson.hour = lesson.proposedHour; clearProposal(lesson); return null;
    }
    public void clearProposal(Lesson lesson) { lesson.proposedDate = null; lesson.proposedHour = -1; lesson.proposalReason = ""; save(); }

    public String cancel(Lesson lesson, String reason) {
        if (!lesson.active()) return "Buổi học này không còn ở trạng thái có thể hủy.";
        if (lesson.started()) return "Buổi học đã bắt đầu hoặc đã diễn ra nên không thể hủy.";
        lesson.status = Lesson.CANCELLED; lesson.cancelReason = reason; lesson.proposedDate = null; save(); return null;
    }

    /** Only the lesson's own student confirms, only once and only after the lesson ended (UC25). */
    public String confirmFinished(Lesson lesson, String viewer) {
        if (!lesson.student.equals(viewer)) return "Chỉ học viên của buổi học được xác nhận hoàn thành.";
        if (lesson.finished) return "Buổi học đã được xác nhận hoàn thành trước đó.";
        if (!lesson.confirmed() || !lesson.ended()) return "Chỉ xác nhận sau khi buổi học đã diễn ra.";
        lesson.finished = true; save(); return null;
    }

    // Group classes ---------------------------------------------------------------------------

    public String classClash(int tutorId, int[] days, int hour, int minutes, int ignoreId) {
        for (GroupClass other : classes)
            if (other.id != ignoreId && other.tutorId == tutorId && !GroupClass.ENDED.equals(other.status) && other.clashes(days,hour,minutes))
                return "Lịch học trùng với lớp \""+other.title+"\" ("+other.scheduleLabel()+").";
        return null;
    }

    public String register(GroupClass item, String student, String goal) {
        if (!GroupClass.OPEN.equals(item.status)) return "Lớp đã đóng tuyển sinh.";
        if (item.full()) return "Lớp đã đủ chỗ.";
        if (item.members.contains(student)) return "Bạn đã là thành viên của lớp này.";
        if (item.registrationOf(student) != null) return "Bạn đã gửi đăng ký lớp này và đang chờ gia sư duyệt.";
        item.registrations.add(new GroupClass.Registration(student, goal, LocalDate.now().toString(), Lesson.PENDING)); save(); return null;
    }
    /** Never accepts more students than the class limit (UC18). */
    public String accept(GroupClass item, GroupClass.Registration registration) {
        if (!registration.pending()) return "Đăng ký này đã được xử lý.";
        if (item.full()) return "Lớp đã đủ "+item.capacity+" học viên. Không thể duyệt thêm.";
        registration.status = Lesson.CONFIRMED; item.members.add(registration.student); save(); return null;
    }
    public String reject(GroupClass.Registration registration) {
        if (!registration.pending()) return "Đăng ký này đã được xử lý.";
        registration.status = Lesson.REJECTED; save(); return null;
    }
    public void closeRecruiting(GroupClass item) { item.status = GroupClass.RUNNING; save(); }

    // Sample data (names, photos and figures follow the Figma mock-up) -----------------------

    private void seed() {
        LocalDate today = LocalDate.now();
        lessons.add(sample(0, STUDENT, "Toán lớp 12", today.plusDays(4), 19, "Ôn tập phương trình bậc hai", Lesson.CONFIRMED));
        Lesson moved = lessons.get(0);
        moved.proposedDate = today.plusDays(5).toString(); moved.proposedHour = 19; moved.proposalReason = "Gia sư có lịch công tác.";
        lessons.add(sample(1, STUDENT, "Tiếng Anh giao tiếp", today.plusDays(6), 18, "Luyện giao tiếp hằng ngày", Lesson.CONFIRMED));
        Lesson done = sample(0, STUDENT, "Toán lớp 12", today.minusDays(2), 19, "Ôn tập phương trình bậc hai", Lesson.CONFIRMED);
        done.content = "Ôn phương trình bậc hai và luyện bài tập."; lessons.add(done);
        lessons.add(sample(0, STUDENT, "Toán lớp 12", today.plusDays(8), 19, "Luyện đề chương hàm số", Lesson.PENDING));
        lessons.add(sample(0, "Minh Khang", "Toán lớp 10", today.plusDays(4), 20, "Củng cố hàm số bậc nhất", Lesson.CONFIRMED));
        lessons.add(sample(0, "Ngọc Mai", "Toán lớp 12", today, 18, "Luyện đề tích phân", Lesson.CONFIRMED));

        GroupClass maths = group(0, "Ôn Toán lớp 12", "Toán", "Lớp 12", 4, 300000, 5, new int[]{6}, 19, 60, 0, GroupClass.OPEN);
        maths.description = "Ôn kiến thức nền và luyện bài tập theo nhóm.";
        maths.members.addAll(Arrays.asList("Lan Anh", "Quốc Bảo", "Thu Trang"));
        maths.registrations.add(new GroupClass.Registration("Ngọc Mai", "Ôn kiến thức và luyện bài tập", today.minusDays(1).toString(), Lesson.PENDING));
        maths.registrations.add(new GroupClass.Registration("Minh Khang", "Ôn kiến thức và luyện bài tập", today.minusDays(1).toString(), Lesson.PENDING));
        GroupClass basics = group(0, "Toán nền tảng lớp 10", "Toán", "Lớp 10", 4, 250000, 5, new int[]{1}, 19, 60, 1, GroupClass.OPEN);
        basics.members.addAll(Arrays.asList("Hải Yến", "Đức Minh"));
        GroupClass english = group(1, "Tiếng Anh giao tiếp", "Tiếng Anh", "Giao tiếp", 6, 450000, 8, new int[]{6}, 19, 60, 2, GroupClass.OPEN);
        english.members.addAll(Arrays.asList("Hà My", "Tuấn Kiệt", "Bảo Ngọc", "Gia Huy", "Khánh Linh"));
        GroupClass exam = group(0, "Toán 12 · Ôn thi đại học", "Toán", "Lớp 12", 8, 150000, 10, new int[]{2,4}, 19, 90, 0, GroupClass.RUNNING);
        exam.mode = "Tại nhà"; exam.address = "Quận Cầu Giấy, Hà Nội"; exam.perSession = true;
        exam.members.addAll(Arrays.asList("An Khang", "Bích Ngọc", "Công Minh", "Diệu Linh", "Gia Bảo", "Hoàng Long", "Kim Chi", "Mạnh Hùng", "Phương Thảo", "Quang Vinh"));
        String[] ended = {"Hình học không gian lớp 11", "Ôn thi học kỳ lớp 10", "Luyện đề Toán lớp 9"};
        for (int i = 0; i < ended.length; i++) {
            GroupClass old = group(0, ended[i], "Toán", "Lớp "+(11-i), 4, 250000, 5, new int[]{7}, 8+i*2, 60, 1, GroupClass.ENDED);
            old.startDate = today.minusMonths(2+i).toString(); old.members.addAll(Arrays.asList("Học viên A", "Học viên B", "Học viên C"));
        }
    }
    private Lesson sample(int tutorId, String student, String title, LocalDate date, int hour, String goal, String status) {
        return new Lesson(nextId(), tutorId, student, title, date.toString(), hour, 60, "Trực tuyến", goal, "", false, status);
    }
    private GroupClass group(int tutorId, String title, String subject, String level, int sessions, int price, int capacity,
                             int[] days, int hour, int minutes, int art, String status) {
        GroupClass item = new GroupClass(nextId(), tutorId);
        item.title=title; item.subject=subject; item.level=level; item.mode="Trực tuyến"; item.address=""; item.description="";
        item.sessions=sessions; item.price=price; item.capacity=capacity; item.days=days; item.hour=hour; item.minutes=minutes;
        item.art=art; item.status=status;
        item.startDate = LocalDate.now().plusDays(3).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(days[0]))).toString();
        classes.add(item); return item;
    }
}
