package com.example.tmdt.ui.groupclass;

import android.view.View;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import com.example.tmdt.GroupClass;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;

/** Course work is reachable outside the timed meeting, only for the class tutor and approved members. */
public final class GroupAssignmentEntry {
    private GroupAssignmentEntry() {}
    public static void add(Fragment host, Ui ui, LinearLayout parent, GroupClass item, String uid) {
        if (item == null || !"Trực tuyến".equals(item.mode) || uid == null || uid.isEmpty()
                || (!uid.equals(item.tutorId) && !item.members.contains(uid))) return;
        View action = ui.action("Bài tập lớp online", R.drawable.ic_file, Ui.OUTLINE,
                () -> GroupAssignmentsDialog.open(host, item.id));
        action.setId(R.id.open_group_assignments);
        ui.addAction(parent, action, 48);
        ui.space(parent, 12);
    }
}
