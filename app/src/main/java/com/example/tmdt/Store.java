package com.example.tmdt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lessons and group classes the signed-in user can see, with every rule of "Lớp học và lịch học".
 * Screens only display the result; {@link Remote} saves each change (Firestore in the app, memory in tests).
 */
public final class Store {
    public interface Remote {
        String newId(String collection);
        void save(Lesson lesson);
        void save(GroupClass item, List<GroupClass.Registration> registrations);
        void save(GroupClass.Registration registration);
    }

    public final List<Lesson> lessons = new ArrayList<>();
    public final List<GroupClass> classes = new ArrayList<>();
    private final Remote remote;
    private final Set<String> settled = new HashSet<>();
    /** uid of the signed-in user, empty for a guest. */
    public String user = "";

    public Store(Remote remote) { this.remote = remote; }

    public String newId(String collection) { return remote.newId(collection); }
    public Lesson lesson(String id) { for (Lesson lesson : lessons) if (lesson.id.equals(id)) return lesson; return null; }
    public GroupClass groupClass(String id) { for (GroupClass item : classes) if (item.id.equals(id)) return item; return null; }

    // Lessons ---------------------------------------------------------------------------------

    /** Returns a message when the slot overlaps the student's own lessons and classes or the tutor's confirmed lessons and classes. */
    public String conflict(String tutorId, String student, String date, int hour, int minutes, String ignoreId) {
        for (Lesson other : lessons) {
            if (other.id.equals(ignoreId) || !other.active() || !other.overlaps(date,hour,minutes)) continue;
            if (other.studentId.equals(student)) return "Bạn đã có buổi học hoặc yêu cầu trong khung giờ này. Hãy chọn giờ khác.";
            if (other.tutorId.equals(tutorId) && other.confirmed()) return "Gia sư đã có lịch dạy trong khung giờ này. Hãy chọn giờ khác.";
        }
        for (GroupClass item : classes) {
            if (!item.active() || !item.meetsAt(date,hour,minutes)) continue;
            if (item.attends(student)) return "Khung giờ này trùng với lớp nhóm \""+item.title+"\" của bạn. Hãy chọn giờ khác.";
            if (item.tutorId.equals(tutorId)) return "Gia sư đã có lịch dạy lớp nhóm trong khung giờ này. Hãy chọn giờ khác.";
        }
        return null;
    }

    /** A new booking or trial request (UC22, screen 03). */
    public String request(Lesson lesson) {
        if (!lesson.start().isAfter(LocalDateTime.now())) return "Thời gian học cần ở trong tương lai.";
        if (lesson.tutorId.equals(lesson.studentId)) return "Bạn không thể đặt lịch với chính mình.";
        if (lesson.trial) { String used = trialError(lesson.tutorId,lesson.studentId); if (used != null) return used; }
        String clash = conflict(lesson.tutorId,lesson.studentId,lesson.date,lesson.hour,lesson.minutes,"");
        if (clash != null) return clash;
        lessons.add(lesson); remote.save(lesson); return null;
    }

    public String acceptRequest(Lesson lesson) {
        if (!lesson.pending()) return "Yêu cầu này đã được xử lý.";
        if (lesson.started()) return "Thời gian học đã qua, không thể chấp nhận.";
        for (Lesson other : lessons)
            if (other != lesson && other.tutorId.equals(lesson.tutorId) && other.confirmed() && other.overlaps(lesson.date,lesson.hour,lesson.minutes))
                return "Khung giờ này trùng với một buổi dạy đã xác nhận.";
        for (GroupClass item : classes)
            if (item.tutorId.equals(lesson.tutorId) && item.active() && item.meetsAt(lesson.date,lesson.hour,lesson.minutes))
                return "Khung giờ này trùng với lớp nhóm \""+item.title+"\".";
        lesson.status = Lesson.CONFIRMED; remote.save(lesson); return null;
    }
    public String rejectRequest(Lesson lesson) {
        if (!lesson.pending()) return "Yêu cầu này đã được xử lý.";
        lesson.status = Lesson.REJECTED; remote.save(lesson); return null;
    }

    public String propose(Lesson lesson, LocalDate date, int hour, String reason) {
        if (!lesson.confirmed() || lesson.started()) return "Chỉ đề nghị đổi lịch cho buổi học đã xác nhận và chưa diễn ra.";
        if (lesson.proposal()) return "Buổi học này đang chờ học viên phản hồi một đề nghị khác.";
        if (!date.atTime(hour,0).isAfter(LocalDateTime.now())) return "Thời gian đề xuất cần ở trong tương lai.";
        if (date.toString().equals(lesson.date) && hour == lesson.hour) return "Thời gian đề xuất đang trùng với lịch hiện tại.";
        String clash = conflict(lesson.tutorId, lesson.studentId, date.toString(), hour, lesson.minutes, lesson.id);
        if (clash != null) return clash;
        lesson.proposedDate = date.toString(); lesson.proposedHour = hour; lesson.proposalReason = reason; remote.save(lesson); return null;
    }
    /** The lesson only moves after the student agrees and the proposed slot is still free. */
    public String acceptProposal(Lesson lesson) {
        if (!lesson.proposal()) return "Đề nghị đổi lịch không còn hiệu lực.";
        if (!LocalDate.parse(lesson.proposedDate).atTime(lesson.proposedHour,0).isAfter(LocalDateTime.now())) return "Thời gian đề xuất đã qua. Lịch hiện tại được giữ nguyên.";
        String clash = conflict(lesson.tutorId, lesson.studentId, lesson.proposedDate, lesson.proposedHour, lesson.minutes, lesson.id);
        if (clash != null) return "Khung giờ đề xuất không còn trống. Lịch hiện tại được giữ nguyên.";
        lesson.date = lesson.proposedDate; lesson.hour = lesson.proposedHour; clearProposal(lesson); return null;
    }
    public void clearProposal(Lesson lesson) { lesson.proposedDate = null; lesson.proposedHour = -1; lesson.proposalReason = ""; remote.save(lesson); }

    public String cancel(Lesson lesson, String reason) {
        if (!lesson.active()) return "Buổi học này không còn ở trạng thái có thể hủy.";
        if (lesson.started()) return "Buổi học đã bắt đầu hoặc đã diễn ra nên không thể hủy.";
        lesson.status = Lesson.CANCELLED; lesson.cancelReason = reason; lesson.proposedDate = null; lesson.proposedHour = -1; lesson.proposalReason = "";
        remote.save(lesson); return null;
    }

    /** Only the lesson's own student confirms, only once and only after the lesson ended (UC25). */
    public String confirmFinished(Lesson lesson, String viewer) {
        if (!lesson.studentId.equals(viewer)) return "Chỉ học viên của buổi học được xác nhận hoàn thành.";
        if (lesson.finished) return "Buổi học đã được xác nhận hoàn thành trước đó.";
        if (!lesson.confirmed() || !lesson.ended()) return "Chỉ xác nhận sau khi buổi học đã diễn ra.";
        lesson.finished = true; remote.save(lesson); return null;
    }

    /** One trial per student and tutor (UC22); a cancelled or rejected trial can be requested again. */
    public String trialError(String tutorId, String student) {
        for (Lesson other : lessons)
            if (other.trial && other.tutorId.equals(tutorId) && other.studentId.equals(student) && other.active())
                return "Mỗi học viên được học thử một lần với mỗi gia sư. Xem buổi học thử trong mục Lịch học.";
        return null;
    }

    // Group classes ---------------------------------------------------------------------------

    /**
     * Checks a new or edited class against the tutor's other classes and confirmed lessons and,
     * when an existing class is edited (ignoreId), against the lessons and classes of its students.
     */
    public String classClash(String tutorId, int[] days, int hour, int minutes, LocalDate start, int sessions, String ignoreId) {
        GroupClass editing = groupClass(ignoreId);
        for (GroupClass other : classes) {
            if (other.id.equals(ignoreId) || !other.active() || !other.clashes(days,hour,minutes)) continue;
            if (other.tutorId.equals(tutorId)) return "Lịch học trùng với lớp \""+other.title+"\" ("+other.scheduleLabel()+").";
            if (editing != null) for (String student : other.members)
                if (editing.attends(student)) return "Lịch mới trùng với lớp \""+other.title+"\" của học viên "+editing.nameOf(student)+".";
        }
        List<LocalDate> dates = GroupClass.dates(start,days,sessions);
        for (Lesson lesson : lessons) {
            if (!lesson.active() || lesson.ended() || !GroupClass.overlap(hour,minutes,lesson.hour,lesson.minutes)
                    || !dates.contains(LocalDate.parse(lesson.date))) continue;
            if (lesson.tutorId.equals(tutorId) && lesson.confirmed())
                return "Lịch học trùng với buổi dạy \""+lesson.title+"\" của "+lesson.studentName+" ("+lesson.dateLabel()+" · "+lesson.timeLabel()+").";
            if (editing != null && editing.attends(lesson.studentId))
                return "Lịch mới trùng với buổi học của học viên "+lesson.studentName+" ("+lesson.dateLabel()+" · "+lesson.timeLabel()+").";
        }
        return null;
    }
    /** Adds a new class (UC16) or saves an edited one (UC17). */
    public void saveClass(GroupClass item) {
        if (!classes.contains(item)) classes.add(item);
        remote.save(item, Collections.emptyList());
    }

    /** Why the student cannot register for the class, or null; checked before the deposit and again when saving. */
    public String registrationError(GroupClass item, String student) {
        if (item.tutorId.equals(student)) return "Bạn là gia sư của lớp này.";
        if (!GroupClass.OPEN.equals(item.status)) return "Lớp đã đóng tuyển sinh.";
        if (item.full()) return "Lớp đã đủ chỗ.";
        if (item.members.contains(student)) return "Bạn đã là thành viên của lớp này.";
        if (item.registrationOf(student) != null) return "Bạn đã gửi đăng ký lớp này và đang chờ gia sư duyệt.";
        for (Lesson lesson : lessons)
            if (lesson.studentId.equals(student) && lesson.active() && !lesson.ended() && item.meetsAt(lesson.date,lesson.hour,lesson.minutes))
                return "Lớp có buổi trùng với buổi học \""+lesson.title+"\" của bạn ("+lesson.dateLabel()+" · "+lesson.timeLabel()+").";
        for (GroupClass other : classes)
            if (other != item && other.active() && other.attends(student) && other.clashes(item.days,item.hour,item.minutes))
                return "Lớp trùng lịch với lớp \""+other.title+"\" bạn đã đăng ký ("+other.scheduleLabel()+").";
        return null;
    }
    public String register(GroupClass item, String student, String name, String goal) {
        String error = registrationError(item, student);
        if (error != null) return error;
        item.registrations.removeIf(old -> old.studentId.equals(student));
        GroupClass.Registration registration = new GroupClass.Registration(item.id,item.tutorId,student,name,goal,LocalDate.now().toString(),Lesson.PENDING);
        item.registrations.add(registration); remote.save(registration); return null;
    }
    /** Never accepts more students than the class limit (UC18). */
    public String accept(GroupClass item, GroupClass.Registration registration) {
        if (!GroupClass.OPEN.equals(item.status)) return "Lớp đã đóng tuyển sinh, không thể duyệt thêm.";
        if (!registration.pending()) return "Đăng ký này đã được xử lý.";
        if (item.full()) return "Lớp đã đủ "+item.capacity+" học viên. Không thể duyệt thêm.";
        registration.status = Lesson.CONFIRMED; item.members.add(registration.studentId);
        remote.save(item, Collections.singletonList(registration)); return null;
    }
    public String reject(GroupClass.Registration registration) {
        if (!registration.pending()) return "Đăng ký này đã được xử lý.";
        registration.status = Lesson.REJECTED; remote.save(registration); return null;
    }
    public void closeRecruiting(GroupClass item) { item.status = GroupClass.RUNNING; remote.save(item, closeWaiting(item)); }

    /**
     * Follows the clock: a class whose last session is over has ended (UC17). A class that no longer
     * recruits closes the registrations still waiting, which frees the students' time slots.
     * Only the class's tutor may write it, and only once per class, so a refused write is not retried in a loop.
     */
    public void updateClasses() {
        for (GroupClass item : classes) {
            boolean ended = item.active() && item.over();
            if (ended) item.status = GroupClass.ENDED;
            List<GroupClass.Registration> closed = GroupClass.OPEN.equals(item.status) ? Collections.emptyList() : closeWaiting(item);
            if ((ended || !closed.isEmpty()) && item.tutorId.equals(user) && settled.add(item.id)) remote.save(item, closed);
        }
    }
    private static List<GroupClass.Registration> closeWaiting(GroupClass item) {
        List<GroupClass.Registration> closed = new ArrayList<>();
        for (GroupClass.Registration registration : item.registrations)
            if (registration.pending()) { registration.status = Lesson.REJECTED; closed.add(registration); }
        return closed;
    }
}
