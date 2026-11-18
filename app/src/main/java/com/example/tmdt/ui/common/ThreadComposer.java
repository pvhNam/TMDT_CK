package com.example.tmdt.ui.common;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/** A thread shares one editor between the new-comment form and the selected reply. */
public final class ThreadComposer {
    private final View form;
    private final EditText input;
    private final ViewGroup home;
    private final int homeIndex;

    public ThreadComposer(View form, EditText input) {
        this.form = form;
        this.input = input;
        home = (ViewGroup) form.getParent();
        homeIndex = home.indexOfChild(form);
    }

    public void placeIn(ViewGroup thread) {
        ViewGroup destination = thread == null ? home : thread;
        if (form.getParent() == destination) return;
        ((ViewGroup) form.getParent()).removeView(form);
        destination.addView(form, thread == null ? homeIndex : 0);
    }

    public void setVisible(boolean visible) {
        if (!visible && form.getVisibility() == View.VISIBLE && form.getWindowToken() != null && input.hasFocus()) {
            InputMethodManager keyboard = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(form.getWindowToken(), 0);
            input.clearFocus();
        }
        form.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void focusReply(ViewGroup thread) {
        input.post(() -> {
            if (form.getParent() != thread || !input.isShown() || !input.isEnabled()) return;
            input.requestFocus();
            input.setSelection(input.length());
            Rect bounds = new Rect();
            input.getDrawingRect(bounds);
            input.requestRectangleOnScreen(bounds, true);
            InputMethodManager keyboard = (InputMethodManager) input.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });
    }
}