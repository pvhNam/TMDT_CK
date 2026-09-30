package com.example.tmdt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.example.tmdt.data.GroupClass;
import com.example.tmdt.data.Lesson;
import com.example.tmdt.data.Store;

/** Scheduling rules between lessons and group classes (UC16, UC20, UC24). Uses its own storage, not the app's. */
@RunWith(AndroidJUnit4.class)
public class StoreRulesTest {
    private Store store;
    private GroupClass maths, exam;

    @Before public void fresh() {
        SharedPreferences preferences = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("store_rules_test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        store = new Store(preferences);
        for (GroupClass item : store.classes) {
            if (item.title.equals("Ôn Toán lớp 12")) maths = item;
            if (item.title.equals("Toán 12 · Ôn thi đại học")) exam = item;
        }
    }

    private Lesson add(int tutor, String student, LocalDate date, int hour, String status) {
        Lesson lesson = new Lesson(store.nextId(), tutor, student, "Buổi kiểm thử", date.toString(), hour, 60, "Trực tuyến", "Mục tiêu", "", false, status);
        store.lessons.add(lesson); return lesson;
    }
    private LocalDate first(GroupClass item) { return item.sessionList().get(0).date; }

    @Test public void sessionsFollowTheClassDays() {
        List<GroupClass.Session> sessions = exam.sessionList();
        assertEquals(exam.sessions, sessions.size());
        assertEquals(LocalDate.parse(exam.startDate), sessions.get(0).date);
        for (int i = 0; i < sessions.size(); i++) {
            DayOfWeek day = sessions.get(i).date.getDayOfWeek();
            assertTrue(day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY);
            assertEquals(i+1, sessions.get(i).number);
        }
    }

    @Test public void sampleDataHasNoClash() {
        for (Lesson lesson : store.lessons) {
            if (!lesson.active() || lesson.ended()) continue;
            assertNull(lesson.title+" "+lesson.date, store.conflict(lesson.tutorId,lesson.student,lesson.date,lesson.hour,lesson.minutes,lesson.id));
            if (lesson.proposal())
                assertNull("đề nghị đổi lịch", store.conflict(lesson.tutorId,lesson.student,lesson.proposedDate,lesson.proposedHour,lesson.minutes,lesson.id));
        }
    }

    @Test public void bookingCannotTakeTheTutorsClassSlot() {
        String clash = store.conflict(0,"Học viên mới",first(exam).toString(),19,60,-1);
        assertNotNull(clash); assertTrue(clash, clash.contains("lớp nhóm"));
        assertNull(store.conflict(0,"Học viên mới",first(exam).toString(),7,60,-1));
    }

    @Test public void bookingCannotTakeTheStudentsClassSlot() {
        String member = store.conflict(1,"An Khang",first(exam).toString(),19,60,-1);
        assertNotNull(member); assertTrue(member, member.contains("của bạn"));
        // A registration still waiting for approval keeps the slot too.
        String waiting = store.conflict(1,"Ngọc Mai",first(maths).toString(),19,60,-1);
        assertNotNull(waiting); assertTrue(waiting, waiting.contains("của bạn"));
    }

    @Test public void tutorCannotAcceptARequestDuringAClass() {
        Lesson request = add(0,"Học viên mới",first(exam),19,Lesson.PENDING);
        assertNotNull(store.acceptRequest(request));
        assertTrue(request.pending());
    }

    @Test public void classCannotCoverAConfirmedLesson() {
        LocalDate wednesday = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        add(1,"Học viên mới",wednesday,10,Lesson.CONFIRMED);
        String clash = store.classClash(1,new int[]{3},10,60,wednesday,4,-1);
        assertNotNull(clash); assertTrue(clash, clash.contains("buổi dạy"));
        assertNull(store.classClash(1,new int[]{3},12,60,wednesday,4,-1));
    }

    @Test public void editedClassCannotCoverAMembersLesson() {
        LocalDate monday = LocalDate.parse(maths.startDate).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        add(1,"Lan Anh",monday,8,Lesson.CONFIRMED);
        String clash = store.classClash(0,new int[]{1},8,60,monday,4,maths.id);
        assertNotNull(clash); assertTrue(clash, clash.contains("Lan Anh"));
    }

    @Test public void studentCannotJoinAClassOverTheirLesson() {
        add(1,"Học viên mới",first(maths),19,Lesson.CONFIRMED);
        int before = maths.registrations.size();
        assertNotNull(store.registrationError(maths,"Học viên mới"));
        assertNotNull(store.register(maths,"Học viên mới","Mục tiêu"));
        assertEquals(before, maths.registrations.size());
        assertNull(store.registrationError(maths,"Học viên khác"));
    }

    @Test public void studentCannotJoinTwoClassesAtTheSameTime() {
        GroupClass other = new GroupClass(store.nextId(),1);
        other.title="Lớp trùng giờ"; other.subject="Tiếng Anh"; other.level="Lớp 12"; other.mode="Trực tuyến"; other.address=""; other.description="";
        other.sessions=4; other.price=200000; other.capacity=5; other.days=new int[]{2}; other.hour=19; other.minutes=60; other.status=GroupClass.OPEN;
        other.startDate=LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY)).toString();
        store.classes.add(other);
        assertNotNull(store.registrationError(other,"An Khang"));
        assertNull(store.registrationError(other,"Học viên mới"));
    }
}
