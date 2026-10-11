package com.example.tmdt.ui.common;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/** Short, interruptible feedback. The platform's animation-off setting is always respected. */
public final class Motion {
    private Motion() {}
    public static boolean enabled() { return ValueAnimator.areAnimatorsEnabled(); }
    public static void press(View view) {
        StateListAnimator states = new StateListAnimator();
        states.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},scale(view,0.98f,80));
        states.addState(new int[]{},scale(view,1f,140));
        view.setStateListAnimator(states);
    }
    private static AnimatorSet scale(View view,float value,long duration) {
        AnimatorSet set=new AnimatorSet();
        set.playTogether(ObjectAnimator.ofFloat(view,View.SCALE_X,value),ObjectAnimator.ofFloat(view,View.SCALE_Y,value));
        set.setDuration(duration);set.setInterpolator(new DecelerateInterpolator());return set;
    }
    public static void change(ViewGroup container) {
        if(!container.isLaidOut() || !container.isAttachedToWindow())return;
        TransitionManager.endTransitions(container);
        if(enabled())TransitionManager.beginDelayedTransition(container,new AutoTransition().setDuration(180));
    }
    public static void reveal(View view) {
        view.animate().cancel();view.setAlpha(1f);view.setTranslationY(0f);
        if(!enabled() || !view.isAttachedToWindow())return;
        view.setAlpha(0.65f);view.setTranslationY(6*view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1f).translationY(0f).setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
    }
}
