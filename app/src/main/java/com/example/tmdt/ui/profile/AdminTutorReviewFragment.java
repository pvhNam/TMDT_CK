package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import java.util.*;

public final class AdminTutorReviewFragment extends TutorReviewScreen {
    private String filter="PENDING";
    private boolean detail;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null){detail=state.getBoolean("detail");filter=state.getString("filter","PENDING");}}
    @Override public void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("detail",detail);out.putString("filter",filter);}
    @Override protected View build(){
        LinearLayout root=ui.column();root.setBackgroundColor(Ui.WHITE);
        ui.header(root,detail?"Xét duyệt hồ sơ":"Duyệt gia sư",()->{if(detail){detail=false;refresh();}else go("account");},null);
        LinearLayout body=ui.page(root);feedback(body);
        if(!review.admin){ui.add(body,ui.text("Trang này dành cho quản trị viên.",16,Ui.INK,true));return root;}
        if(detail){details(body);return root;}
        String[] keys={"PENDING","NEEDS_REVISION","VERIFIED","REJECTED"},labels={"Chờ duyệt","Bổ sung","Đã duyệt","Từ chối"};
        int index=Arrays.asList(keys).indexOf(filter);ui.tabs(body,labels,Math.max(0,index),i->{filter=keys[i];refresh();});ui.space(body,20);
        int count=0;
        for(Map<String,Object> item:review.queue){String status=TutorReviewState.text(item,"trang_thai_xac_minh");
            if(!filter.equals(status)&&!(filter.equals("PENDING")&&status.equals("IN_REVIEW")))continue;count++;
            LinearLayout card=ui.card(),row=ui.row();String uid=TutorReviewState.text(item,"id");row.addView(ui.avatar(uid,TutorReviewState.text(item,"ho_ten"),48));ui.gap(row,12);
            LinearLayout copy=ui.column();ui.add(copy,ui.text(TutorReviewState.text(item,"ho_ten"),17,Ui.INK,true));ui.space(copy,5);ui.add(copy,ui.text(label(status),13,Ui.MUTED,false));ui.weight(row,copy);row.addView(ui.text("›",26,Ui.BLUE,false));ui.add(card,row);
            card.setTag(uid);ui.clickable(card,()->{review.select(uid);detail=true;refresh();});ui.add(body,card);ui.space(body,12);
        }
        if(count==0)ui.add(body,ui.note(R.drawable.ic_info,"Chưa có hồ sơ trong mục này.",Ui.PALE,Ui.BLUE,Ui.MUTED));
        ui.add(body,ui.text("Hiển thị tối đa 100 hồ sơ.",12,Ui.MUTED,false));return root;
    }
    private void details(LinearLayout body){
        Map<String,Object> item=review.selected;summary(body,item,review.selectedId);ui.label(body,"Trường / đơn vị");ui.add(body,ui.text(TutorReviewState.text(item,"truong"),15,Ui.INK,false));ui.space(body,14);
        ui.label(body,"Chuyên ngành");ui.add(body,ui.text(TutorReviewState.text(item,"chuyen_nganh"),15,Ui.INK,false));ui.space(body,18);
        ui.label(body,"Thông tin giảng dạy");
        ui.add(body,ui.text("Kinh nghiệm: "+item.getOrDefault("so_nam_kinh_nghiem",0)+" năm",14,Ui.MUTED,false));
        String mode=TutorReviewState.text(item,"hinh_thuc_day");ui.add(body,ui.text("Hình thức: "+(mode.equals("CA_HAI")?"Trực tuyến · Tại nhà":mode.equals("TAI_NHA")?"Tại nhà":"Trực tuyến"),14,Ui.MUTED,false));
        Object price=item.get("hoc_phi");if(price instanceof Number)ui.add(body,ui.text("Học phí: "+Tutor.money(((Number)price).intValue())+" / 90 phút",14,Ui.BLUE,true));ui.space(body,18);
        ui.label(body,"Minh chứng");files(body,TutorReviewState.files(item));ui.space(body,16);
        ui.add(body,ui.text("Gửi ngày "+date(item,item.containsKey("submitted_at")?"submitted_at":"created_at"),13,Ui.MUTED,false));ui.space(body,18);
        String status=TutorReviewState.text(item,"trang_thai_xac_minh");
        if(!Arrays.asList("PENDING","IN_REVIEW").contains(status)){ui.add(body,ui.text(label(status)+"\n"+TutorReviewState.text(item,"ly_do"),15,Ui.INK,true));return;}
        if(status.equals("PENDING"))action(body,"Bắt đầu kiểm tra",Ui.OUTLINE,()->review.decide(review.selectedId,"IN_REVIEW",""));
        boolean hasPhoto=TutorReviewState.files(item).stream().anyMatch(f->"diploma".equals(f.get("kind"))&&TutorEvidenceDraft.isFirestoreImage(f));
        if(hasPhoto)action(body,"Chấp nhận làm giáo viên",Ui.PRIMARY,()->ui.dialog().setTitle("Chấp nhận làm giáo viên?").setMessage("Xác nhận ảnh minh chứng hợp lệ. Người dùng sẽ được mở chức năng giáo viên và nhận lớp.").setNegativeButton("Hủy",null).setPositiveButton("Chấp nhận",(d,w)->review.decide(review.selectedId,"VERIFIED","")).show());
        else{ui.add(body,ui.note(R.drawable.ic_info,"Chưa có ảnh bằng cấp/thẻ sinh viên. Yêu cầu người dùng bổ sung ảnh trước khi chấp nhận.",Ui.ORANGE_BG,Ui.ORANGE,Ui.INK));ui.space(body,12);}
        action(body,"Yêu cầu bổ sung",Ui.OUTLINE,()->reason("NEEDS_REVISION","Yêu cầu bổ sung"));
        action(body,"Từ chối hồ sơ",Ui.DANGER,()->reason("REJECTED","Lý do từ chối"));
    }
    private void reason(String status,String title){
        EditText input=ui.entry("Nhập lý do (tối đa 500 ký tự)",true);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        ui.dialog().setTitle(title).setView(input).setNegativeButton("Hủy",null).setPositiveButton("Xác nhận",(d,w)->review.decide(review.selectedId,status,input.getText().toString())).show();
    }
}
