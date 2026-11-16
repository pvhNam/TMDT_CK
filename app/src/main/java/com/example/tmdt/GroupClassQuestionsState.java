package com.example.tmdt;

import androidx.lifecycle.SavedStateHandle;

/** Class Q&A compatibility API backed by the shared discussion state. */
public final class GroupClassQuestionsState extends QuestionThreadState {
    public GroupClassQuestionsState(SavedStateHandle saved) { super(saved, Scope.CLASS); }
}
