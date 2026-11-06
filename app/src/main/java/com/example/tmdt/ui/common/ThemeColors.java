package com.example.tmdt.ui.common;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import com.example.tmdt.R;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

/** Shared colors for code-built screens; photos and branded button fills keep their colors. */
public final class ThemeColors {
    private ThemeColors(){}
    public static boolean dark(Context context){return (context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
    public static int background(Context context){return context.getColor(R.color.app_background);}
    public static int text(Context context,int color){
        if(!dark(context)||Color.alpha(color)==0)return color;
        int r=Color.red(color),g=Color.green(color),b=Color.blue(color),alpha=Color.alpha(color);
        int mapped=color;
        if(r>225&&g>225&&b>225)return color; // White labels on filled buttons.
        if(b>140&&b>r*1.25f&&b>g*1.05f)mapped=context.getColor(R.color.app_accent);
        else if(g>60&&g>r*1.2f&&g>b*1.08f)mapped=context.getColor(R.color.app_success);
        else if(r>130&&g>60&&r>g*1.2f&&g>b*1.3f)mapped=context.getColor(R.color.app_warning);
        else if(r>160&&r>g*1.5f&&r>b*1.2f)mapped=context.getColor(R.color.app_error);
        else if(Math.max(r,Math.max(g,b))<135)mapped=context.getColor(R.color.app_ink);
        else if(r<180&&g<190&&b<215)mapped=context.getColor(R.color.app_muted);
        return (mapped&0x00FFFFFF)|(alpha<<24);
    }
    public static int fill(Context context,int color){
        if(!dark(context)||Color.alpha(color)==0)return color;
        int r=Color.red(color),g=Color.green(color),b=Color.blue(color),mapped=color;
        if(r>=249&&g>=249&&b>=249)mapped=context.getColor(R.color.app_surface);
        else if(r>=170&&g>=225&&g>r+7&&g>b+3)mapped=0xFF153B31;
        else if(r>=250&&g>=220&&b<225)mapped=0xFF3A2D18;
        else if(r>=245&&g>=205&&b>=205&&r>g+8)mapped=0xFF39232B;
        else if(r>=180&&g>=190&&b>=200)mapped=context.getColor(R.color.app_surface_variant);
        return (mapped&0x00FFFFFF)|(Color.alpha(color)<<24);
    }
    public static int border(Context context,int color){return dark(context)&&Color.red(color)>=170&&Color.green(color)>=180&&Color.blue(color)>=190?context.getColor(R.color.app_border):color;}
    private static final int[][] STATES={{-android.R.attr.state_enabled},
            {android.R.attr.state_enabled,android.R.attr.state_pressed},
            {android.R.attr.state_enabled,android.R.attr.state_checked},
            {android.R.attr.state_enabled,android.R.attr.state_selected},
            {android.R.attr.state_enabled,android.R.attr.state_focused},{android.R.attr.state_enabled},{}};
    private static ColorStateList colors(Context context,ColorStateList colors,boolean surface){
        int[] values=new int[STATES.length];for(int i=0;i<STATES.length;i++){
            int source=colors.getColorForState(STATES[i],colors.getDefaultColor());
            values[i]=surface?fill(context,source):text(context,source);
        }
        return new ColorStateList(STATES,values);
    }
    private static void background(Context context,Drawable drawable){
        if(drawable instanceof GradientDrawable){GradientDrawable shape=(GradientDrawable)drawable.mutate();
            int[] colors=shape.getColors();if(colors!=null){for(int i=0;i<colors.length;i++)colors[i]=fill(context,colors[i]);shape.setColors(colors);}
            else if(shape.getColor()!=null)shape.setColor(colors(context,shape.getColor(),true));
        }else if(drawable instanceof ColorDrawable){ColorDrawable color=(ColorDrawable)drawable.mutate();color.setColor(fill(context,color.getColor()));}
        else if(drawable instanceof LayerDrawable){LayerDrawable layers=(LayerDrawable)drawable;for(int i=0;i<layers.getNumberOfLayers();i++)background(context,layers.getDrawable(i));}
        else if(drawable instanceof InsetDrawable)background(context,((InsetDrawable)drawable).getDrawable());
        else if(drawable instanceof StateListDrawable){StateListDrawable states=(StateListDrawable)drawable;
            for(int i=0;i<states.getStateCount();i++)background(context,states.getStateDrawable(i));}
    }
    /** Also covers older views built with literal colors, without touching image pixels. */
    public static void apply(View view){
        Context context=view.getContext();if(!dark(context))return;
        if(view.getBackground()!=null)background(context,view.getBackground());
        if(view.getBackgroundTintList()!=null)view.setBackgroundTintList(colors(context,view.getBackgroundTintList(),true));
        if(view instanceof TextView){TextView text=(TextView)view;text.setTextColor(colors(context,text.getTextColors(),false));
            if(text.getHintTextColors()!=null)text.setHintTextColor(colors(context,text.getHintTextColors(),false));
            text.setLinkTextColor(context.getColor(R.color.app_accent));}
        if(view instanceof ImageView){ImageView image=(ImageView)view;if(image.getImageTintList()!=null)image.setImageTintList(colors(context,image.getImageTintList(),false));}
        if(view instanceof android.widget.CompoundButton){android.widget.CompoundButton button=(android.widget.CompoundButton)view;
            if(button.getButtonTintList()!=null)button.setButtonTintList(colors(context,button.getButtonTintList(),false));}
        if(view instanceof com.google.android.material.button.MaterialButton){com.google.android.material.button.MaterialButton button=(com.google.android.material.button.MaterialButton)view;
            if(button.getIconTint()!=null)button.setIconTint(colors(context,button.getIconTint(),false));}
        if(view instanceof com.google.android.material.card.MaterialCardView){com.google.android.material.card.MaterialCardView card=(com.google.android.material.card.MaterialCardView)view;card.setCardBackgroundColor(colors(context,card.getCardBackgroundColor(),true));}
        if(view instanceof com.google.android.material.tabs.TabLayout){com.google.android.material.tabs.TabLayout tabs=(com.google.android.material.tabs.TabLayout)view;
            tabs.setTabTextColors(context.getColor(R.color.app_muted),context.getColor(R.color.app_accent));
            tabs.setSelectedTabIndicatorColor(context.getColor(tabs.getId()==R.id.section_tabs?R.color.app_surface:R.color.app_accent));}
        if(view instanceof com.google.android.material.textfield.TextInputLayout){com.google.android.material.textfield.TextInputLayout input=(com.google.android.material.textfield.TextInputLayout)view;
            input.setDefaultHintTextColor(ColorStateList.valueOf(context.getColor(R.color.app_muted)));
            input.setErrorTextColor(ColorStateList.valueOf(context.getColor(R.color.app_error)));}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i));}
    }
}
