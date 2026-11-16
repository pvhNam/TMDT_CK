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
    public static void list(androidx.recyclerview.widget.RecyclerView list){
        if(!enabled()){list.setItemAnimator(null);return;}
        androidx.recyclerview.widget.DefaultItemAnimator animator=new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);animator.setAddDuration(160);animator.setRemoveDuration(120);animator.setMoveDuration(180);
        list.setItemAnimator(animator);
    }
    public static void press(View view) {
        if(!enabled()||Boolean.TRUE.equals(view.getTag(R.id.motion_bound)))return;
        view.setTag(R.id.motion_bound,true);
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
    /** Animate changed content once, keeping repeated data notifications and typing still. */
    public static void contentChanged(View view){contentChanged(view,view);}
    public static void contentChanged(View owner,View content){
        long signature=signature(content);
        Object previous=owner.getTag(R.id.motion_content_signature);
        owner.setTag(R.id.motion_content_signature,signature);
        if(previous instanceof Long&&((Long)previous)!=signature&&enabled()){
            androidx.core.view.OneShotPreDrawListener.add(content,()->{
                if(content.isAttachedToWindow())reveal(content);
            });
        }
    }
    private static long signature(View view){
        if(view instanceof android.widget.EditText||view instanceof android.widget.ProgressBar)return 0;
        long hash=31L*view.getClass().getName().hashCode()+view.getVisibility()+(view.isSelected()?1:0);
        if(view.getVisibility()==View.GONE)return hash;
        if(view instanceof android.widget.TextView)hash=31*hash+((android.widget.TextView)view).getText().toString().hashCode();
        if(view instanceof android.widget.CompoundButton)hash=31*hash+(((android.widget.CompoundButton)view).isChecked()?1:0);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)hash=31*hash+signature(group.getChildAt(i));}
        return hash;
    }
    /** XML controls use the same feedback as controls created with Ui. */
    public static void bindControls(View view){
        if(view.isClickable()&&!(view instanceof android.widget.EditText)&&!(view instanceof android.widget.CompoundButton)
                &&!(view instanceof android.webkit.WebView))press(view);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)bindControls(group.getChildAt(i));
        }
    }
}
