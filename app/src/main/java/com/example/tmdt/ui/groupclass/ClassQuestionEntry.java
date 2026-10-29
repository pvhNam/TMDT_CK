package com.example.tmdt.ui.groupclass;

import android.view.View;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import com.example.tmdt.R;
import com.example.tmdt.ui.common.Ui;

/** Shared entry into the public questions for a single group class. */
public final class ClassQuestionEntry {
    private ClassQuestionEntry() {}
    public static void add(Fragment host, Ui ui, LinearLayout parent, String classId) {
        View action = ui.action(host.getString(R.string.class_questions_title), R.drawable.ic_chat, Ui.OUTLINE,
                () -> ClassQuestionsDialog.open(host, classId));
        action.setId(R.id.open_class_questions);
        ui.addAction(parent, action, 46);
        ui.space(parent, 12);
    }
}
