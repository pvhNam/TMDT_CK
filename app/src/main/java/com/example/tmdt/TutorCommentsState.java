package com.example.tmdt;

import androidx.lifecycle.SavedStateHandle;

/** Public comments and replies on a verified tutor profile, separate from ratings. */
public final class TutorCommentsState extends QuestionThreadState {
    public TutorCommentsState(SavedStateHandle saved) { super(saved, Scope.TUTOR); }
}
