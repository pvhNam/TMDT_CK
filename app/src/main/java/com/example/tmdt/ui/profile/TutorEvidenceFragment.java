package com.example.tmdt.ui.profile;

import android.view.*;
import android.widget.*;
import android.text.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import java.util.*;

public final class TutorEvidenceFragment extends TutorReviewScreen {
    private String kind="diploma";
    @Override public void onCreate(android.os.Bundle state){super.onCreate(state);if(state!=null)kind=state.getString("evidence_kind","diploma");}
    @Override public void onSaveInstanceState(android.os.Bundle out){super.onSaveInstanceState(out);out.putString("evidence_kind",kind);}
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri!=null&&review!=null)review.upload(uri,kind,supplement());
    });
    private boolean supplement(){return getArguments()!=null&&getArguments().getBoolean("supplement");}
    public static TutorEvidenceFragment supplementScreen(){TutorEvidenceFragment f=new TutorEvidenceFragment();android.os.Bundle args=new android.os.Bundle();args.putBoolean("supplement",true);f.setArguments(args);return f;}
    @Override protected View build(){
        boolean supplement=supplement();TutorEvidenceDraft draft=review.draft(supplement);
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);
        ui.header(root,supplement?"Bổ sung hồ sơ":"Đăng ký làm gia sư",()->go(supplement?"tutor_pending":"register_tutor"),null);
        LinearLayout body=ui.page(root);feedback(body);
        if(!supplement)TutorReviewDesign.steps(ui,body,2);else{
            LinearLayout banner=ui.column();ui.pad(banner,14,14);ui.surface(banner,0xFFFFF5E4,12,0);
            LinearLayout heading=ui.row();heading.addView(ui.icon(R.drawable.ic_info,22,Ui.ORANGE));ui.gap(heading,8);
            heading.addView(ui.pill("Cần bổ sung",12,Ui.ORANGE,0xFFFFE3B5));ui.add(banner,heading);ui.space(banner,8);
            ui.add(banner,ui.text(TutorReviewState.text(review.own,"ly_do"),13,Ui.MUTED,false));ui.add(body,banner);ui.space(body,8);
            if(!"NEEDS_REVISION".equals(review.status())){ui.add(body,ui.text("Trạng thái đã thay đổi. Hãy quay lại hồ sơ.",15,Ui.MUTED,false));return root;}
        }
        if(!supplement){ui.add(body,ui.text("Bổ sung minh chứng",20,Ui.INK,true));ui.space(body,14);}
        LinearLayout details=ui.column();
        ui.label(details,"Trường hoặc đơn vị");entry(details,draft.school,"Nhập trường hoặc đơn vị",150,s->draft.school=s,draft);
        ui.label(details,"Chuyên ngành");entry(details,draft.major,"Nhập chuyên ngành",150,s->draft.major=s,draft);
        if(supplement){
            TextView edit=ui.link("Chỉnh sửa trường / chuyên ngành",13,()->{
                com.example.tmdt.ui.common.Motion.change(body);details.setVisibility(details.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
            });ui.add(body,edit);details.setVisibility(View.GONE);
        }
        ui.add(body,details);
        ui.label(body,"Ảnh bằng cấp hoặc thẻ sinh viên");
        for(Map<String,Object> file:new ArrayList<>(draft.files))if("diploma".equals(file.get("kind")))fileRow(body,draft,file);
        uploadBox(body,draft.hasDiploma()?"Thay ảnh bằng cấp":"Chọn ảnh minh chứng","JPG/PNG · Tự nén ảnh trước khi lưu","diploma");
        ui.label(body,"Chứng chỉ bổ sung (tùy chọn)");
        for(Map<String,Object> file:new ArrayList<>(draft.files))if(!"diploma".equals(file.get("kind")))fileRow(body,draft,file);
        uploadBox(body,"Thêm chứng chỉ","Tối đa 4 ảnh trong hồ sơ","certificate");
        if(supplement){
        ui.label(body,"Ghi chú bổ sung (tùy chọn)");EditText note=entry(body,draft.note,"Nhập ghi chú nếu có…",500,s->draft.note=s,draft);
        TextView counter=ui.text(draft.note.length()+"/500",11,Ui.MUTED,false);counter.setGravity(Gravity.END);ui.add(body,counter);ui.space(body,12);
        note.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}
            public void onTextChanged(CharSequence s,int st,int before,int count){counter.setText(s.length()+"/500");}});
        }
        ui.add(body,ui.note(R.drawable.ic_info,"Chỉ bạn và quản trị viên xét duyệt được xem ảnh minh chứng.",Ui.PALE,Ui.BLUE,Ui.MUTED));ui.space(body,18);
        LinearLayout controls=ui.column();controls.setPadding(ui.dp(16),ui.dp(8),ui.dp(16),ui.dp(6));ui.add(root,controls);
        action(controls,supplement?"Gửi lại để xét duyệt":"Tiếp tục",Ui.PRIMARY,()->{
            String error=draft.error();if(!error.isEmpty()){review.error=error;refresh();return;}
            ((MainActivity)requireActivity()).hideKeyboard();
            if(supplement)review.resubmit();else{review.error="";review.notice="";go("review_tutor");}
        });
        if(supplement)action(controls,"Lưu bản nháp",Ui.OUTLINE,()->{draft.save(requireContext(),review.owner);review.notice="Đã lưu bản nháp trên thiết bị này.";go("tutor_pending");});
        return root;
    }
    private EditText entry(LinearLayout body,String value,String hint,int max,java.util.function.Consumer<String> set,TutorEvidenceDraft draft){
        EditText input=ui.entry(hint,max==500);input.setText(value);input.setTextSize(14);
        input.setMinimumHeight(ui.dp(max==500?96:46));ui.pad(input,12,12);ui.surface(input,Ui.WHITE,10,Ui.BORDER);
        input.setEnabled(!review.busy);if(max==500){input.setGravity(Gravity.TOP);input.setMinLines(3);}
        input.setOnFocusChangeListener((v,focused)->ui.surface(v,Ui.WHITE,10,focused?Ui.BLUE:Ui.BORDER));input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}
            public void onTextChanged(CharSequence s,int st,int before,int count){set.accept(s.toString());TutorEvidenceDraft latest=TutorEvidenceDraft.load(requireContext(),review.owner);latest.school=draft.school;latest.major=draft.major;latest.note=draft.note;latest.save(requireContext(),review.owner);}});
        ui.add(body,input);ui.space(body,max==500?4:14);return input;
    }
    private void fileRow(LinearLayout body,TutorEvidenceDraft draft,Map<String,Object> file){
        ui.add(body,TutorReviewDesign.file(ui,file,()->{
            if(review.busy)return;draft.files.remove(file);draft.save(requireContext(),review.owner);refresh();
        }));ui.space(body,8);
    }
    private void uploadBox(LinearLayout body,String title,String note,String selectedKind){
        LinearLayout box=ui.column();ui.pad(box,12,16);box.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable surface=ui.background(0xFFF6FAFF,12,0);
        surface.setStroke(ui.dp(1),0xFF9EC5FF,ui.dp(5),ui.dp(4));box.setBackground(surface);
        box.addView(ui.icon(selectedKind.equals("diploma")?R.drawable.ic_image:R.drawable.ic_file,26,Ui.BLUE));ui.space(box,8);
        box.addView(ui.text(title,14,Ui.BLUE,true));ui.space(box,5);TextView caption=ui.text(note,12,Ui.MUTED,false);caption.setGravity(Gravity.CENTER);box.addView(caption);
        ui.clickable(box,()->{if(review.busy)return;if(review.draft(supplement()).files.size()>=4&&!selectedKind.equals("diploma")){review.error="Tối đa 4 tệp. Hãy xóa một tệp trước.";refresh();return;}kind=selectedKind;picker.launch(new String[]{"image/jpeg","image/png"});});
        box.setEnabled(!review.busy);box.setAlpha(review.busy?0.55f:1f);ui.add(body,box);ui.space(body,16);
    }
}
