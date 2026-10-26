package com.example.tmdt.ui.common;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import com.example.tmdt.R;

/** Short, interruptible feedback. The platform's animation-off setting is always respected. */
public final class Motion {
    private Motion() {}
    public static boolean enabled() { return ValueAnimator.areAnimatorsEnabled(); }
    public static void press(View view) {
        if(!enabled())return;
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
        if(enabled())TransitionManager.beginDelayedTransition(container,new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new Fade().setDuration(150))
                .addTransition(new ChangeBounds().setDuration(200))
                .setInterpolator(new DecelerateInterpolator()));
    }
    public static void reveal(View view) {
        boolean running=Boolean.TRUE.equals(view.getTag(R.id.motion_revealing));
        view.animate().cancel();
        if(!enabled() || !view.isAttachedToWindow()){
            view.setTag(R.id.motion_revealing,null);view.setAlpha(1f);view.setTranslationY(0f);return;
        }
        if(!running){view.setAlpha(0.82f);view.setTranslationY(4*view.getResources().getDisplayMetrics().density);}
        view.setTag(R.id.motion_revealing,true);
        view.animate().alpha(1f).translationY(0f).setDuration(180).setInterpolator(new DecelerateInterpolator())
                .withEndAction(()->view.setTag(R.id.motion_revealing,null)).start();
    }
    /** XML controls use the same feedback as controls created with Ui. */
    public static void bindControls(View view){
        if((view instanceof android.widget.Button && !(view instanceof android.widget.CompoundButton))
                || view instanceof android.widget.ImageButton)press(view);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)bindControls(group.getChildAt(i));
        }
    }
}
