package com.example.tmdt.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/** Moves the help button inside the content area while keeping a quick tap clickable. */
public final class FloatingHelpDrag implements View.OnTouchListener {
    private final View handle;
    private final View floating;
    private final SharedPreferences preferences;
    private final RectF bounds = new RectF();
    private final int touchSlop;
    private float horizontalPosition;
    private float verticalPosition;
    private float allowedBottom;
    private float downX;
    private float downY;
    private float startLeft;
    private float startTop;
    private boolean boundsReady;
    private boolean touching;
    private boolean dragging;
    private boolean cancelled;

    public FloatingHelpDrag(View handle, View floating) {
        this.handle = handle;
        this.floating = floating;
        preferences = floating.getContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        horizontalPosition = clamp(preferences.getFloat("help_position_x", 1f), 0f, 1f);
        verticalPosition = clamp(preferences.getFloat("help_position_y", 1f), 0f, 1f);
        touchSlop = ViewConfiguration.get(floating.getContext()).getScaledTouchSlop();
        handle.setOnTouchListener(this);
        floating.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (touching) cancelClick();
            applyPosition();
        });
    }

    public void updateBounds(float left, float top, float right, float bottom, float visibleBottom) {
        float maxLeft = Math.max(left, right);
        float maxTop = Math.max(top, bottom);
        float permittedBottom = clamp(visibleBottom, top, maxTop);
        if (boundsReady && bounds.left == left && bounds.top == top && bounds.right == maxLeft
                && bounds.bottom == maxTop && allowedBottom == permittedBottom) return;
        if (touching) cancelClick();
        bounds.set(left, top, maxLeft, maxTop);
        allowedBottom = permittedBottom;
        boundsReady = true;
        applyPosition();
    }

    private void applyPosition() {
        if (!boundsReady) return;
        floating.setX(bounds.left + horizontalPosition * bounds.width());
        floating.setY(Math.min(bounds.top + verticalPosition * bounds.height(), allowedBottom));
    }

    @Override public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!boundsReady) return false;
                touching = true;
                dragging = false;
                cancelled = false;
                downX = event.getRawX();
                downY = event.getRawY();
                startLeft = floating.getX();
                startTop = floating.getY();
                view.drawableHotspotChanged(event.getX(), event.getY());
                view.setPressed(true);
                interceptParent(true);
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                cancelClick();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!touching || cancelled) return true;
                float distanceX = event.getRawX() - downX;
                float distanceY = event.getRawY() - downY;
                if (!dragging && Math.hypot(distanceX, distanceY) <= touchSlop) return true;
                dragging = true;
                view.setPressed(false);
                float left = clamp(startLeft + distanceX, bounds.left, bounds.right);
                float top = clamp(startTop + distanceY, bounds.top, allowedBottom);
                floating.setX(left);
                floating.setY(top);
                horizontalPosition = bounds.width() > 0 ? (left - bounds.left) / bounds.width() : 1f;
                verticalPosition = bounds.height() > 0 ? (top - bounds.top) / bounds.height() : 1f;
                return true;
            case MotionEvent.ACTION_UP:
                boolean click = touching && !dragging && !cancelled;
                finishGesture();
                if (click) view.performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                finishGesture();
                return true;
            default:
                return touching;
        }
    }

    private void cancelClick() {
        cancelled = true;
        handle.setPressed(false);
    }

    private void finishGesture() {
        if (dragging) {
            preferences.edit().putFloat("help_position_x", horizontalPosition)
                    .putFloat("help_position_y", verticalPosition).apply();
        }
        touching = false;
        dragging = false;
        handle.setPressed(false);
        interceptParent(false);
    }

    private void interceptParent(boolean intercept) {
        ViewParent parent = handle.getParent();
        if (parent != null) parent.requestDisallowInterceptTouchEvent(intercept);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
