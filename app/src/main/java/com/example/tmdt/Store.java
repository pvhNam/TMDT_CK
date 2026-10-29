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
        /** Writes only the member's own answer to the class's proposal. */
        void answer(GroupClass item, String student, String answer);
    }
    /** Hours of notice a moved session and its new slot need, and how long before the earlier of the two answers close. */
    public static final int MOVE_NOTICE = 24, MOVE_ANSWER = 12;

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
            if (!item.active() || !item.meetsAt(date,hour*60,minutes)) continue;
            if (item.attends(student)) return "Khung giờ này trùng với lớp nhóm \""+item.title+"\" của bạn. Hãy chọn giờ khác.";
            if (item.tutorId.equals(tutorId)) return "Gia sư đã có lịch dạy lớp nhóm trong khung giờ này. Hãy chọn giờ khác.";
        }
        return null;
    }

    /** A new booking or trial request (UC22, screen 03). */
    public String request(Lesson lesson) {
        if (!lesson.start().isAfter(LocalDateTime.now())) return "Thời gian học cần ở trong tương lai.";
        if (lesson.tutorId.equals(lesson.studentId)) return "Bạn không thể đặt lịch với chính mình.";
        if (lesson.hour*60+lesson.minute+lesson.minutes > Lesson.DAY) return "Buổi học cần kết thúc trong ngày.";
        if (lesson.price > Lesson.MAX_PRICE) return "Học phí một buổi vượt quá "+Tutor.money(Lesson.MAX_PRICE)+". Hãy chọn thời lượng ngắn hơn.";
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
            if (item.tutorId.equals(lesson.tutorId) && item.active() && item.meetsAt(lesson.date,lesson.hour*60+lesson.minute,lesson.minutes))
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
        if (hour*60+lesson.minutes > Lesson.DAY) return "Với giờ này buổi học sẽ kéo sang ngày hôm sau. Hãy chọn giờ sớm hơn.";
        String clash = conflict(lesson.tutorId, lesson.studentId, date.toString(), hour, lesson.minutes, lesson.id);
        if (clash != null) return clash;
        lesson.proposedDate = date.toString(); lesson.proposedHour = hour; lesson.proposalReason = reason; lesson.reply = ""; remote.save(lesson); return null;
    }
    /** The lesson only moves after the student agrees and the proposed slot is still free. */
    public String acceptProposal(Lesson lesson) {
        if (!lesson.proposal()) return "Đề nghị đổi lịch không còn hiệu lực.";
        if (!LocalDate.parse(lesson.proposedDate).atTime(lesson.proposedHour,0).isAfter(LocalDateTime.now())) return "Thời gian đề xuất đã qua. Lịch hiện tại được giữ nguyên.";
        String clash = conflict(lesson.tutorId, lesson.studentId, lesson.proposedDate, lesson.proposedHour, lesson.minutes, lesson.id);
        if (clash != null) return "Khung giờ đề xuất không còn trống. Lịch hiện tại được giữ nguyên.";
        lesson.date = lesson.proposedDate; lesson.hour = lesson.proposedHour; lesson.minute = 0; lesson.reply = Lesson.REPLY_ACCEPTED; clearProposal(lesson); return null;
    }
    /** The student keeps the current slot; the answer stays on the lesson so the tutor learns about it. */
    public String keepSchedule(Lesson lesson) {
        if (!lesson.proposal()) return "Đề nghị đổi lịch không còn hiệu lực.";
        lesson.reply = Lesson.REPLY_KEPT; clearProposal(lesson); return null;
    }
    public void clearProposal(Lesson lesson) { lesson.proposedDate = null; lesson.proposedHour = -1; lesson.proposalReason = ""; remote.save(lesson); }
    public String setRoomLink(Lesson lesson, String link) {
        if (!lesson.confirmed() || lesson.ended()) return "Chỉ gắn link cho buổi học đã xác nhận và chưa kết thúc.";
        if (!lesson.online()) return "Buổi học tại nhà không cần link phòng học.";
        String value = roomLink(link);
        if (value == null) return "Link phòng học chưa hợp lệ. Ví dụ: meet.google.com/abc-defg-hij";
        if (value.length() > 300) return "Link phòng học quá dài.";
        lesson.roomLink = value; remote.save(lesson); return null;
    }

    /** Accepts links copied without "https://" or inside a shared invitation text; empty means the app's own room. */
    static String roomLink(String text) {
        String value = text.trim();
        if (value.isEmpty()) return "";
        java.util.regex.Matcher match = java.util.regex.Pattern
                .compile("(?i)(https?://\\S+|(?:[a-z0-9-]+\\.)+[a-z]{2,}(?:[/?#]\\S*)?)").matcher(value);
        if (!match.find()) return null;
        String found = match.group(1).replaceAll("[).,;\\]]+$","");
        return found.matches("(?i)https?://.*") ? found : "https://"+found;
    }

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
    public String classClash(String tutorId, int[] days, int begin, int minutes, LocalDate start, int sessions, String ignoreId) {
        GroupClass editing = groupClass(ignoreId);
        for (GroupClass other : classes) {
            if (other.id.equals(ignoreId) || !other.active() || !other.clashes(days,begin,minutes)) continue;
            if (other.tutorId.equals(tutorId)) return "Lịch học trùng với lớp \""+other.title+"\" ("+other.scheduleLabel()+").";
            if (editing != null) for (String student : other.members)
                if (editing.attends(student)) return "Lịch mới trùng với lớp \""+other.title+"\" của học viên "+editing.nameOf(student)+".";
        }
        List<LocalDate> dates = GroupClass.dates(start,days,sessions);
        for (Lesson lesson : lessons) {
            if (!lesson.active() || lesson.ended() || !GroupClass.overlap(begin,minutes,lesson.hour*60+lesson.minute,lesson.minutes)
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
        if (item.cancelled()) return "Lớp đã bị gia sư hủy.";
        if (!GroupClass.OPEN.equals(item.status)) return "Lớp đã đóng tuyển sinh.";
        if (item.full()) return "Lớp đã đủ chỗ.";
        if (item.members.contains(student)) return "Bạn đã là thành viên của lớp này.";
        if (item.registrationOf(student) != null) return "Bạn đã gửi đăng ký lớp này và đang chờ gia sư duyệt.";
        for (Lesson lesson : lessons)
            if (lesson.studentId.equals(student) && lesson.active() && !lesson.ended() && item.meetsAt(lesson.date,lesson.hour*60+lesson.minute,lesson.minutes))
                return "Lớp có buổi trùng với buổi học \""+lesson.title+"\" của bạn ("+lesson.dateLabel()+" · "+lesson.timeLabel()+").";
        for (GroupClass other : classes)
            if (other != item && other.active() && other.attends(student) && other.clashes(item.days,item.begin(),item.minutes))
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
    /** One room link for every session of an online class, set by its tutor. */
    public String setClassLink(GroupClass item, String link) {
        if (!item.tutorId.equals(user)) return "Chỉ gia sư của lớp mới gắn được link phòng học.";
        if (!item.active()) return "Lớp đã kết thúc hoặc đã bị hủy.";
        if (!item.online()) return "Lớp học tại nhà không cần link phòng học.";
        String value = roomLink(link);
        if (value == null) return "Link phòng học chưa hợp lệ. Ví dụ: meet.google.com/abc-defg-hij";
        if (value.length() > 300) return "Link phòng học quá dài.";
        item.roomLink = value; remote.save(item, Collections.emptyList()); return null;
    }
    /** The tutor cancels a class that has not ended; registrations still waiting close and members see the reason. */
    public String cancelClass(GroupClass item, String reason) {
        if (!item.tutorId.equals(user)) return "Chỉ gia sư của lớp mới hủy được lớp.";
        if (!item.cancellable()) return "Lớp đã kết thúc hoặc đã bị hủy.";
        String text = reason.trim();
        if (text.isEmpty()) return "Vui lòng nhập lý do hủy lớp.";
        if (text.length() > 150) return "Lý do hủy tối đa 150 ký tự.";
        item.status = GroupClass.CANCELLED; item.cancelReason = text;
        remote.save(item, closeWaiting(item)); return null;
    }

    /** A session the tutor may still move: at least a day ahead, in a class that has not ended. */
    public boolean movable(GroupClass item, GroupClass.Session session) {
        return item.tutorId.equals(user) && item.active() && !session.start().isBefore(LocalDateTime.now().plusHours(MOVE_NOTICE));
    }
    /**
     * The tutor moves one session of a class. Without members it moves at once; otherwise every member is asked and the
     * session keeps its time until nobody is busy. The new slot stays between the sessions before and after it.
     */
    public String proposeMove(GroupClass item, int number, LocalDate date, int hour, int minute, String reason) {
        List<GroupClass.Session> list = item.sessionList();
        if (number < 1 || number > list.size()) return "Buổi học không còn trong lịch của lớp.";
        GroupClass.Session session = list.get(number-1);
        if (!item.tutorId.equals(user)) return "Chỉ gia sư của lớp mới đổi lịch được.";
        if (!movable(item,session)) return "Chỉ đổi lịch buổi học còn ít nhất "+MOVE_NOTICE+" giờ nữa mới bắt đầu.";
        if (item.proposalOpen() && item.proposal.number != number)
            return "Học viên đang trả lời đề nghị đổi buổi "+item.proposal.number+". Hãy đợi đề nghị đó xong hoặc giữ giờ cũ cho buổi đó.";
        String text = reason.trim();
        if (text.isEmpty()) return "Vui lòng nhập lý do đổi lịch.";
        if (text.length() > 120) return "Lý do đổi lịch tối đa 120 ký tự.";
        LocalDateTime start = date.atTime(hour,minute), end = start.plusMinutes(item.minutes);
        if (start.isBefore(LocalDateTime.now().plusHours(MOVE_NOTICE))) return "Giờ mới cần cách hiện tại ít nhất "+MOVE_NOTICE+" giờ để học viên kịp sắp xếp.";
        if (start.equals(session.start())) return "Giờ mới đang trùng với lịch hiện tại của buổi này.";
        if (hour*60+minute+item.minutes > Lesson.DAY) return "Với giờ này buổi học sẽ kéo sang ngày hôm sau. Hãy chọn giờ sớm hơn.";
        if (number > 1 && start.isBefore(list.get(number-2).end()))
            return "Buổi "+number+" cần diễn ra sau buổi "+(number-1)+" ("+list.get(number-2).label()+").";
        if (number < list.size() && end.isAfter(list.get(number).start()))
            return "Buổi "+number+" cần kết thúc trước buổi "+(number+1)+" ("+list.get(number).label()+").";
        String clash = moveClash(item,date.toString(),hour*60+minute);
        if (clash != null) return clash;
        item.settleProposal();
        GroupClass.Move move = new GroupClass.Move(date.toString(),hour,minute,text);
        LocalDateTime first = start.isBefore(session.start()) ? start : session.start();
        if (item.members.isEmpty()) { item.moves.put(number,move); item.proposal = null; }
        else item.proposal = new GroupClass.Proposal(number,move,first.minusHours(MOVE_ANSWER));
        remote.save(item, Collections.emptyList()); return null;
    }
    /** The tutor's other lessons and classes, and the classes of every member; members' own lessons the tutor cannot see are left to their answers. */
    private String moveClash(GroupClass item, String date, int begin) {
        for (Lesson lesson : lessons) {
            if (!lesson.active() || !lesson.date.equals(date) || !GroupClass.overlap(begin,item.minutes,lesson.hour*60+lesson.minute,lesson.minutes)) continue;
            if (lesson.studentId.equals(item.tutorId) || (lesson.tutorId.equals(item.tutorId) && lesson.confirmed()))
                return "Giờ mới trùng với buổi \""+lesson.title+"\" của bạn ("+lesson.timeLabel()+").";
            if (item.members.contains(lesson.studentId)) return "Giờ mới trùng với buổi học của học viên "+item.nameOf(lesson.studentId)+" ("+lesson.timeLabel()+").";
        }
        for (GroupClass other : classes) {
            if (other.id.equals(item.id) || !other.active() || !other.meetsAt(date,begin,item.minutes)) continue;
            if (other.tutorId.equals(item.tutorId) || other.attends(item.tutorId)) return "Giờ mới trùng với lớp \""+other.title+"\" của bạn.";
            for (String member : item.members)
                if (other.attends(member)) return "Giờ mới trùng với lớp \""+other.title+"\" của học viên "+item.nameOf(member)+".";
        }
        return null;
    }
    /** The member's own lesson or class at the proposed time, named so they can answer "busy"; null when the slot is free. */
    public String moveConflict(GroupClass item) {
        if (item.proposal == null) return null;
        GroupClass.Move move = item.proposal.move;
        for (Lesson lesson : lessons)
            if (lesson.studentId.equals(user) && lesson.active() && lesson.date.equals(move.date)
                    && GroupClass.overlap(move.begin(),item.minutes,lesson.hour*60+lesson.minute,lesson.minutes))
                return "buổi \""+lesson.title+"\" ("+lesson.timeLabel()+")";
        for (GroupClass other : classes)
            if (!other.id.equals(item.id) && other.active() && other.attends(user) && other.meetsAt(move.date,move.begin(),item.minutes))
                return "lớp \""+other.title+"\"";
        return null;
    }
    /** A member says whether they can come; the answer can change until the deadline or until the proposal applies. */
    public String answerMove(GroupClass item, boolean join) {
        if (!item.members.contains(user)) return "Chỉ học viên trong lớp mới trả lời được đề nghị này.";
        if (!item.proposalOpen()) return "Đề nghị đổi lịch đã hết hạn trả lời hoặc đã được áp dụng.";
        String answer = join ? GroupClass.Proposal.JOIN : GroupClass.Proposal.BUSY;
        item.proposal.answers.put(user,answer); remote.answer(item,user,answer); return null;
    }
    /** The tutor withdraws a proposal that has not applied, so the session keeps its time. */
    public String keepSessionTime(GroupClass item) {
        if (!item.tutorId.equals(user)) return "Chỉ gia sư của lớp mới giữ được giờ cũ.";
        if (item.proposal == null || item.proposalAccepted()) return "Đề nghị đổi lịch không còn hiệu lực.";
        item.proposal = null; remote.save(item, Collections.emptyList()); return null;
    }

    /**
     * Follows the clock: a class whose last session is over has ended (UC17). A class that no longer
     * recruits closes the registrations still waiting, which frees the students' time slots.
     * A proposal members can no longer answer becomes part of the schedule, or is dropped when someone was busy.
     * Only the class's tutor may write it, and only once per class or proposal, so a refused write is not retried in a loop.
     */
    public void updateClasses() {
        for (GroupClass item : classes) {
            boolean ended = item.active() && item.over();
            if (ended) item.status = GroupClass.ENDED;
            List<GroupClass.Registration> closed = GroupClass.OPEN.equals(item.status) ? Collections.emptyList() : closeWaiting(item);
            boolean mine = item.tutorId.equals(user);
            boolean moved = mine && item.proposal != null && !item.proposalOpen() && settled.add(item.id+"#"+item.proposal.key()) && item.settleProposal();
            boolean changed = (ended || !closed.isEmpty()) && mine && settled.add(item.id);
            if (changed || moved) remote.save(item, closed);
        }
    }
    private static List<GroupClass.Registration> closeWaiting(GroupClass item) {
        List<GroupClass.Registration> closed = new ArrayList<>();
        for (GroupClass.Registration registration : item.registrations)
            if (registration.pending()) { registration.status = Lesson.REJECTED; closed.add(registration); }
        return closed;
    }
}
