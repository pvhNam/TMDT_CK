package com.example.tmdt.ui.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.example.tmdt.*;
import com.example.tmdt.ui.common.Ui;
import com.example.tmdt.ui.common.StableViews;
import com.example.tmdt.ui.common.RenderSnapshot;
import com.example.tmdt.ui.common.ThemeColors;
import java.util.*;

public final class AdminTutorReviewFragment extends TutorReviewScreen {
    private static final String[] FILTERS={"PENDING","NEEDS_REVISION","VERIFIED","REJECTED","ALL"};
    private static final String[] FILTER_LABELS={"Chờ duyệt","Cần bổ sung","Đã duyệt","Từ chối","Tất cả"};
    private String filter="PENDING";
    private String query="",restoredId="";
    private boolean detail;
    private LinearLayout body,header,controls;
    private EditText searchInput;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(state!=null){detail=state.getBoolean("detail");filter=state.getString("filter","PENDING");query=state.getString("query","");restoredId=state.getString("review_tutor_id","");}
        if(!Arrays.asList(FILTERS).contains(filter))filter="PENDING";
    }
    @Override public void onViewCreated(View view,Bundle state){
        super.onViewCreated(view,state);
        review.changes.observe(getViewLifecycleOwner(),ignored->{
            if(detail&&review.admin&&review.selectedId.isEmpty()&&!restoredId.isEmpty())review.select(restoredId);
        });
    }
    @Override public void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);out.putBoolean("detail",detail);out.putString("filter",filter);out.putString("query",query);out.putString("review_tutor_id",review==null?restoredId:review.selectedId);
    }
    @Override protected View build(){
        LinearLayout root=ui.column();root.setBackgroundColor(ThemeColors.background(requireContext()));
        header=ui.column();ui.add(root,header);body=ui.page(root);
        controls=ui.column();ui.pad(controls,16,12);ui.surface(controls,Ui.WHITE,0,0);ui.add(root,controls);
        controls.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
            if(bottom-top!=oldBottom-oldTop&&isAdded())((MainActivity)requireActivity()).updateFloatingHelp();
        });
        render();return root;
    }
    @Override protected boolean updateContent(){if(body==null)return false;render();return true;}
    @Override protected Object dataSnapshot(){return RenderSnapshot.of(review.admin,review.busy,review.error,review.notice,review.queueLoading,review.detailLoading,review.queueError,review.detailError,detail,filter,query,review.selectedId,detail?review.selected:review.queue);}
    public boolean backToList(){
        if(!detail)return false;if(review!=null&&review.busy)return true;
        detail=false;hideKeyboard();scrollToTop();refreshData();return true;
    }
    private void render(){
        StableViews heading=StableViews.at(header);heading.begin();
        heading.add("header",Arrays.asList(detail,review.busy),()->{
            LinearLayout box=ui.column();TextView refresh=ui.link("Làm mới",12,review::reloadAdmin);refresh.setEnabled(!review.busy);
            ui.header(box,detail?"Chi tiết hồ sơ":"Duyệt gia sư",()->{if(!backToList())go("admin_management");},refresh);return box;
        },0);heading.commit();
        renderControls();
        StableViews rows=StableViews.at(body);rows.begin();
        if(review.busy||!review.error.isEmpty()||!review.notice.isEmpty())rows.add("feedback",Arrays.asList(review.busy,review.error,review.notice),()->{LinearLayout box=ui.column();feedback(box);return box;},0);
        if(!review.admin){rows.add("permission",true,()->ui.text("Trang này dành cho quản trị viên.",16,Ui.INK,true),0);rows.commit();return;}
        if(detail){details(rows);rows.commit();return;}
        int[] counts=new int[FILTERS.length];for(int i=0;i<FILTERS.length;i++)for(Map<String,Object> item:review.queue)if(inFilter(item,FILTERS[i]))counts[i]++;
        rows.add("search",true,this::searchField,ui.dp(12));
        rows.add("filters",Arrays.asList(filter,counts,query.isEmpty()),()->filters(counts),ui.dp(18));
        List<Map<String,Object>> applications=new ArrayList<>();String normalized=CatalogState.normalize(query);
        for(Map<String,Object> item:review.queue){
            if(!inFilter(item,filter))continue;
            String searchable=text(item,"ho_ten")+" "+text(item,"truong")+" "+text(item,"chuyen_nganh")+" "+text(item,"khu_vuc");
            if(CatalogState.normalize(searchable).contains(normalized))applications.add(item);
        }
        applications.sort(Comparator.comparingLong((Map<String,Object> item)->millis(item,"created_at")).reversed());
        rows.add("count",applications.size(),()->{
            LinearLayout row=ui.row();ui.weight(row,ui.text("Hồ sơ đăng ký",16,Ui.INK,true));row.addView(ui.text(applications.size()+" hồ sơ",12,Ui.MUTED,false));return row;
        },ui.dp(12));
        for(Map<String,Object> item:applications){String uid=text(item,"id");rows.add("application/"+uid,item,()->applicationCard(item),ui.dp(12));}
        if(applications.isEmpty())rows.add("empty",Arrays.asList(query,filter,review.queueLoading,review.queueError),this::emptyState,0);
        rows.commit();
    }
    private View filters(int[] counts){
        int selected=Math.max(0,Arrays.asList(FILTERS).indexOf(filter));
        LinearLayout row=ui.row();TextView value=ui.text(FILTER_LABELS[selected]+" ("+counts[selected]+")",14,Ui.INK,true);value.setMaxLines(1);
        LinearLayout picker=ui.picker(R.drawable.ic_field_shield,value,()->showFilters(counts));picker.setMinimumHeight(ui.dp(48));
        picker.setContentDescription("Lọc hồ sơ: "+FILTER_LABELS[selected]);ui.weight(row,picker);
        if(!"ALL".equals(filter)||!query.isEmpty()){
            ui.gap(row,12);TextView reset=ui.link("Xóa lọc",13,this::resetFilters);reset.setMinHeight(ui.dp(48));row.addView(reset);
        }return row;
    }
    private void showFilters(int[] counts){
        if(review.busy)return;hideKeyboard();String[] labels=new String[FILTERS.length];
        for(int i=0;i<labels.length;i++)labels[i]=FILTER_LABELS[i]+" ("+counts[i]+")";
        ui.dialog().setTitle("Lọc theo trạng thái").setSingleChoiceItems(labels,Math.max(0,Arrays.asList(FILTERS).indexOf(filter)),(dialog,index)->{
            dialog.dismiss();chooseFilter(FILTERS[index]);
        }).setNegativeButton("Đóng",null).show();
    }
    private void resetFilters(){
        if(review.busy)return;filter="ALL";query="";if(searchInput!=null)searchInput.setText("");
        hideKeyboard();scrollToTop();refreshData();
    }
    private View searchField(){
        LinearLayout row=ui.row();ui.surface(row,Ui.WHITE,12,Ui.BORDER);ui.pad(row,12,0);row.addView(ui.icon(R.drawable.ic_search,21,Ui.MUTED));
        EditText input=ui.input("Tìm tên, trường hoặc chuyên ngành");input.setBackground(null);ui.pad(input,10,12);
        searchInput=input;
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(150)});input.setText(query);input.setContentDescription("Tìm hồ sơ gia sư");
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((view,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){hideKeyboard();return true;}return false;});
        ui.weight(row,input);TextView clear=ui.link("Xóa",12,()->input.setText(""));clear.setMinWidth(ui.dp(40));clear.setGravity(Gravity.CENTER);row.addView(clear);
        clear.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void afterTextChanged(Editable value){}
            public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();clear.setVisibility(s.length()==0?View.GONE:View.VISIBLE);refreshData();}
        });return row;
    }
    private View applicationCard(Map<String,Object> item){
        String uid=text(item,"id"),status=text(item,"trang_thai_xac_minh"),name=text(item,"ho_ten");
        LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();ui.pad(card,14,12);row.addView(ui.avatar(uid,name,44));ui.gap(row,12);
        TextView title=ui.text(name.isEmpty()?"Gia sư":name,16,Ui.INK,true);title.setMaxLines(2);ui.add(copy,title);
        if(!text(item,"truong").isEmpty()){ui.space(copy,5);TextView school=ui.text(text(item,"truong"),13,Ui.MUTED,false);school.setMaxLines(2);school.setEllipsize(android.text.TextUtils.TruncateAt.END);ui.add(copy,school);}
        ui.weight(row,copy);ui.gap(row,8);row.addView(ui.icon(R.drawable.ic_right,18,Ui.MUTED));ui.add(card,row);ui.space(card,10);
        LinearLayout bottom=ui.row();bottom.addView(statusPill(status));ui.gap(bottom,10);
        String submitted=millis(item,"created_at")>0?new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(millis(item,"created_at"))):"";
        TextView date=ui.text(submitted,12,Ui.MUTED,false);date.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);date.setMaxLines(1);
        // A plain View with WRAP_CONTENT height expands in a fillViewport ScrollView. Text stays intrinsic.
        ui.weight(bottom,date);
        ui.add(card,bottom);card.setContentDescription("Xem hồ sơ "+name+", "+statusLabel(status));
        ui.clickable(card,()->{if(review.busy)return;hideKeyboard();detail=true;restoredId=uid;scrollToTop();review.select(uid);refreshData();});return card;
    }
    private View emptyState(){
        LinearLayout card=ui.card();ui.pad(card,20,28);card.setGravity(Gravity.CENTER);
        if(review.queueLoading){card.addView(new ProgressBar(requireContext()),ui.lp(30,30));ui.space(card,14);card.addView(ui.text("Đang tải hồ sơ…",14,Ui.MUTED,false));return card;}
        card.addView(ui.icon(R.drawable.ic_file,36,Ui.MUTED));ui.space(card,12);
        TextView text=ui.text(!review.queueError.isEmpty()?"Chưa tải được danh sách hồ sơ":query.trim().isEmpty()?"Chưa có hồ sơ trong mục này":"Không tìm thấy hồ sơ phù hợp",15,Ui.MUTED,false);text.setGravity(Gravity.CENTER);ui.add(card,text);
        if(!review.queueError.isEmpty()){ui.space(card,12);ui.addAction(card,ui.action("Tải lại hồ sơ",0,Ui.OUTLINE,review::reloadAdmin),44);}return card;
    }
    private void details(StableViews rows){
        Map<String,Object> item=review.selected;String uid=review.selectedId;
        if(uid.isEmpty()){rows.add("missing",true,()->ui.text("Đang mở hồ sơ…",14,Ui.MUTED,false),0);return;}
        rows.add("identity",Arrays.asList(uid,item,review.detailLoading,review.busy,review.detailError),()->identity(item,uid),ui.dp(20));
        rows.add("information",Arrays.asList(item.get("truong"),item.get("chuyen_nganh"),item.get("hoc_van"),item.get("so_nam_kinh_nghiem"),item.get("khu_vuc")),()->{
            LinearLayout box=ui.column(),card=section(box,"Thông tin chuyên môn");
            field(card,"Trường / đơn vị",text(item,"truong"));field(card,"Chuyên ngành",text(item,"chuyen_nganh"));
            if(!text(item,"hoc_van").isEmpty())field(card,"Trình độ",text(item,"hoc_van"));
            if(item.get("so_nam_kinh_nghiem") instanceof Number)field(card,"Kinh nghiệm",item.get("so_nam_kinh_nghiem")+" năm");
            if(!text(item,"khu_vuc").isEmpty())field(card,"Khu vực",text(item,"khu_vuc"));return box;
        },ui.dp(20));
        rows.add("teaching",Arrays.asList(item.get("mon_hoc"),item.get("cap_lop"),item.get("hinh_thuc_day"),item.get("hoc_phi")),()->{
            LinearLayout box=ui.column(),card=section(box,"Thông tin giảng dạy");
            if(!text(item,"mon_hoc").isEmpty())field(card,"Môn học",text(item,"mon_hoc"));
            if(!text(item,"cap_lop").isEmpty())field(card,"Cấp lớp",text(item,"cap_lop"));
            field(card,"Hình thức",mode(text(item,"hinh_thuc_day")));
            if(item.get("hoc_phi") instanceof Number)field(card,"Học phí",PaymentState.money(((Number)item.get("hoc_phi")).longValue())+" / buổi");return box;
        },ui.dp(20));
        if(!text(item,"gioi_thieu").isEmpty())rows.add("introduction",item.get("gioi_thieu"),()->{
            LinearLayout box=ui.column(),card=section(box,"Giới thiệu");ui.add(card,ui.text(text(item,"gioi_thieu"),14,Ui.INK,false));return box;
        },ui.dp(20));
        List<Map<String,Object>> evidence=TutorReviewState.files(item);
        rows.add("evidence-title",evidence.size(),()->{LinearLayout row=ui.row();ui.weight(row,ui.text("Minh chứng",17,Ui.INK,true));row.addView(ui.pill(evidence.size()+" tệp",11,Ui.BLUE,Ui.PALE));return row;},ui.dp(12));
        for(int i=0;i<evidence.size();i++){Map<String,Object> file=evidence.get(i);rows.add("evidence/"+uid+"/"+i,file,()->{
            LinearLayout box=ui.column();ui.add(box,ui.text("diploma".equals(file.get("kind"))?"Bằng cấp / thẻ sinh viên":"Chứng chỉ bổ sung",12,Ui.MUTED,true));ui.space(box,7);
            ui.add(box,TutorReviewDesign.file(ui,file,null));return box;
        },ui.dp(14));}
        if(evidence.isEmpty())rows.add("empty-evidence",review.detailLoading,()->ui.text(review.detailLoading?"Đang tải minh chứng…":"Chưa có tệp minh chứng.",14,Ui.MUTED,false),ui.dp(16));
        if(!review.detailLoading&&review.detailError.isEmpty()&&pending(item)&&!hasDiploma(item))rows.add("missing-diploma",true,()->ui.note(R.drawable.ic_info,"Chưa có bằng cấp hoặc thẻ sinh viên hợp lệ để duyệt hồ sơ.",Ui.ORANGE_BG,Ui.ORANGE,Ui.INK),ui.dp(16));
        if(!text(item,"ghi_chu").isEmpty())rows.add("note",item.get("ghi_chu"),()->{
            LinearLayout box=ui.column(),card=section(box,"Ghi chú từ gia sư");ui.add(card,ui.text(text(item,"ghi_chu"),14,Ui.INK,false));return box;
        },ui.dp(20));
        String status=text(item,"trang_thai_xac_minh");
        if(!pending(item)||!text(item,"ly_do").isEmpty())rows.add("result",Arrays.asList(status,item.get("ly_do"),item.get("reviewed_at")),()->{
            LinearLayout box=ui.column(),card=section(box,pending(item)?"Phản hồi trước đó":"Kết quả xét duyệt");
            if(!pending(item)){card.addView(statusPill(status));ui.space(card,10);}
            if(!text(item,"ly_do").isEmpty())ui.add(card,ui.text(text(item,"ly_do"),14,Ui.INK,false));
            if(millis(item,"reviewed_at")>0){ui.space(card,8);ui.add(card,ui.text(date(item,"reviewed_at"),12,Ui.MUTED,false));}return box;
        },ui.dp(16));
    }
    private View identity(Map<String,Object> item,String uid){
        LinearLayout card=ui.card(),row=ui.row(),copy=ui.column();row.addView(ui.avatar(uid,text(item,"ho_ten"),64));ui.gap(row,14);
        ui.add(copy,ui.text(text(item,"ho_ten").isEmpty()?"Hồ sơ gia sư":text(item,"ho_ten"),21,Ui.INK,true));ui.space(copy,8);
        copy.addView(statusPill(text(item,"trang_thai_xac_minh")));ui.weight(row,copy);ui.add(card,row);ui.space(card,12);
        ui.add(card,ui.text("Gửi ngày "+date(item,item.containsKey("submitted_at")?"submitted_at":"created_at"),12,Ui.MUTED,false));
        if(review.detailLoading){ui.space(card,8);ui.add(card,ui.text("Đang tải chi tiết…",12,Ui.BLUE,false));}
        else if("PENDING".equals(text(item,"trang_thai_xac_minh"))&&review.detailError.isEmpty()){
            ui.space(card,12);View start=ui.link("Bắt đầu kiểm tra",14,()->{if(!review.busy)review.decide(uid,"IN_REVIEW","");});start.setEnabled(!review.busy);ui.add(card,start);
        }return card;
    }
    private LinearLayout section(LinearLayout box,String title){ui.section(box,title,17);ui.space(box,10);LinearLayout card=ui.card();ui.add(box,card);return card;}
    private void field(LinearLayout card,String title,String value){
        if(card.getChildCount()>0){ui.space(card,12);ui.line(card);ui.space(card,12);}
        ui.add(card,ui.text(title,12,Ui.MUTED,false));ui.space(card,5);TextView text=ui.text(value.isEmpty()?"Chưa cập nhật":value,15,Ui.INK,false);text.setTextIsSelectable(true);ui.add(card,text);
    }
    private void renderControls(){
        StableViews rows=StableViews.at(controls);rows.begin();
        boolean visible=detail&&review.admin&&!review.selectedId.isEmpty()&&pending(review.selected);
        boolean changed=controls.getVisibility()!=(visible?View.VISIBLE:View.GONE);
        controls.setVisibility(visible?View.VISIBLE:View.GONE);
        if(changed)controls.post(()->{if(isAdded())((MainActivity)requireActivity()).updateFloatingHelp();});
        if(!visible){rows.commit();return;}
        String uid=review.selectedId;boolean ready=!review.busy&&!review.detailLoading&&review.detailError.isEmpty();
        rows.add("approve",Arrays.asList(uid,ready,hasDiploma(review.selected),review.busy),()->ui.action(review.busy?"Đang xử lý…":"Duyệt hồ sơ",R.drawable.ic_pf_shield,ready&&hasDiploma(review.selected)?Ui.PRIMARY:Ui.DISABLED,()->approve(uid)),ui.dp(8));
        rows.add("secondary",Arrays.asList(uid,ready),()->{
            LinearLayout row=ui.row();ui.weightAction(row,ui.action("Yêu cầu bổ sung",0,ready?Ui.OUTLINE:Ui.DISABLED,()->reason("NEEDS_REVISION","Yêu cầu bổ sung")),46);ui.gap(row,10);
            ui.weightAction(row,ui.action("Từ chối",0,ready?Ui.DANGER:Ui.DISABLED,()->reason("REJECTED","Từ chối hồ sơ")),46);return row;
        },0);rows.commit();
    }
    private void approve(String uid){
        if(review.busy||!uid.equals(review.selectedId)||!pending(review.selected)||!hasDiploma(review.selected))return;
        ui.dialog().setTitle("Duyệt hồ sơ gia sư?").setMessage("Xác nhận duyệt hồ sơ của "+text(review.selected,"ho_ten")+"?")
                .setNegativeButton("Đóng",null).setPositiveButton("Duyệt hồ sơ",(dialog,which)->{if(uid.equals(review.selectedId)&&!review.busy)review.decide(uid,"VERIFIED","");}).show();
    }
    private void reason(String status,String title){
        if(review.busy||review.detailLoading||!pending(review.selected))return;
        LinearLayout content=ui.column();ui.pad(content,20,8);ui.add(content,ui.text(text(review.selected,"ho_ten"),14,Ui.INK,true));ui.space(content,12);
        EditText input=ui.entry("Nội dung gửi cho gia sư",true);input.setMinLines(3);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});ui.add(content,input);ui.space(content,6);
        TextView counter=ui.text("0/500",12,Ui.MUTED,false);counter.setGravity(Gravity.END);ui.add(content,counter);
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable text){}
            public void onTextChanged(CharSequence s,int start,int before,int count){counter.setText(s.length()+"/500");input.setError(null);}});
        String uid=review.selectedId;androidx.appcompat.app.AlertDialog dialog=ui.dialog().setTitle(title).setView(content).setNegativeButton("Đóng",null).setPositiveButton("REJECTED".equals(status)?"Từ chối":"Gửi yêu cầu",null).show();
        if("REJECTED".equals(status))dialog.getButton(-1).setTextColor(ThemeColors.text(requireContext(),Ui.RED));
        dialog.getButton(-1).setOnClickListener(v->{
            if(review.busy||!uid.equals(review.selectedId))return;String value=input.getText().toString().trim();
            if(value.isEmpty()){input.setError("Vui lòng nhập nội dung gửi cho gia sư.");input.requestFocus();return;}review.decide(uid,status,value);dialog.dismiss();
        });
    }
    private void chooseFilter(String value){if(review.busy)return;filter=value;hideKeyboard();scrollToTop();refreshData();}
    private void hideKeyboard(){((MainActivity)requireActivity()).hideKeyboard();}
    private static String text(Map<String,Object> item,String key){return TutorReviewState.text(item,key);}
    private static boolean pending(Map<String,Object> item){return Arrays.asList("PENDING","IN_REVIEW").contains(text(item,"trang_thai_xac_minh"));}
    private static boolean hasDiploma(Map<String,Object> item){return TutorReviewState.files(item).stream().anyMatch(file->"diploma".equals(file.get("kind"))&&TutorEvidenceDraft.isFirestoreImage(file));}
    private static long millis(Map<String,Object> item,String key){Object value=item.get(key);return value instanceof com.google.firebase.Timestamp?((com.google.firebase.Timestamp)value).toDate().getTime():0;}
    private static boolean inFilter(Map<String,Object> item,String filter){return "ALL".equals(filter)||filter.equals(text(item,"trang_thai_xac_minh"))||("PENDING".equals(filter)&&"IN_REVIEW".equals(text(item,"trang_thai_xac_minh")));}
    private static String statusLabel(String status){return "REJECTED".equals(status)?"Đã từ chối":label(status);}
    private View statusPill(String status){int color="VERIFIED".equals(status)?Ui.GREEN:"REJECTED".equals(status)?Ui.RED:"NEEDS_REVISION".equals(status)?Ui.ORANGE:Ui.BLUE;
        int fill="VERIFIED".equals(status)?Ui.GREEN_BG:"REJECTED".equals(status)?0xFFFFE9EC:"NEEDS_REVISION".equals(status)?Ui.ORANGE_BG:Ui.PALE;return ui.pill(statusLabel(status),11,color,fill);}
    private static String mode(String key){switch(key){case "CA_HAI":return "Trực tuyến · Tại nhà";case "TAI_NHA":return "Tại nhà";case "TRUC_TUYEN":return "Trực tuyến";default:return "";}}
    public int actionInset(){return controls!=null&&controls.getVisibility()==View.VISIBLE?controls.getHeight():0;}
    @Override public void onDestroyView(){super.onDestroyView();body=null;header=null;controls=null;searchInput=null;}
}
