package com.example.tmdt.ui.profile;

import android.view.*;
import android.widget.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.*;
import java.util.*;

/** Compact components shared by the evidence and review screens. */
final class TutorReviewDesign {
    private TutorReviewDesign(){}
    static LinearLayout file(Ui ui,Map<String,Object> file,Runnable remove){
        LinearLayout row=ui.row();ui.pad(row,10,10);ui.surface(row,Ui.WHITE,12,Ui.BORDER);
        EvidenceImageView preview=new EvidenceImageView(ui.context,file,true);row.addView(preview,ui.lp(52,52));ui.gap(row,10);
        LinearLayout copy=ui.column();TextView name=ui.text(TutorReviewState.text(file,"name"),13,Ui.INK,true);
        name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);ui.add(copy,name);ui.space(copy,5);
        Object bytes=file.get("size");String size=bytes instanceof Number?Math.max(1,Math.round(((Number)bytes).doubleValue()/1024))+" KB · ":"";
        ui.add(copy,ui.text(size+"Chạm ảnh để xem",11,Ui.MUTED,false));ui.weight(row,copy);
        if(remove!=null)row.addView(ui.iconButton("close","Xóa tệp "+TutorReviewState.text(file,"name"),remove));
        return row;
    }
    static void steps(Ui ui,LinearLayout body,int current){
        View steps=LayoutInflater.from(ui.context).inflate(R.layout.view_tutor_steps,body,false);
        int[] dots={R.id.step_info,R.id.step_evidence,R.id.step_review};
        int[] labels={R.id.label_info,R.id.label_evidence,R.id.label_review};
        for(int i=0;i<3;i++){
            TextView dot=steps.findViewById(dots[i]),label=steps.findViewById(labels[i]);
            dot.setSelected(i<current);dot.setText(i+1<current?"✓":String.valueOf(i+1));
            dot.setTextColor(i<current?Ui.WHITE:Ui.MUTED);label.setTextColor(i<current?Ui.BLUE:Ui.MUTED);
        }
        steps.findViewById(R.id.step_line_one).setBackgroundColor(current>=2?Ui.BLUE:Ui.BORDER);
        steps.findViewById(R.id.step_line_two).setBackgroundColor(current>=3?Ui.BLUE:Ui.BORDER);
        body.addView(steps);
    }
}
