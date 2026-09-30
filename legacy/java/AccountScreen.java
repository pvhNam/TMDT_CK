package com.example.tmdt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.time.LocalDateTime;

/** Account pages 07, 13, 14, 15, 16 and 53 from giaodiendangnhap.docx. */
final class AccountScreen {
    private final MainActivity activity;
    private final AccountState state;
    private final Ui ui;
    private LinearLayout body;
    AccountScreen(MainActivity activity) { this.activity=activity;state=activity.account;ui=activity.ui; }
    View build(String page) {
        LinearLayout root=ui.column();
        String title;
        switch(page){
            case "register":title="Đăng ký tài khoản";break;
            case "forgot":title="Quên mật khẩu";break;
            case "edit_account":title="Chỉnh sửa hồ sơ";break;
            case "account":title="Cá nhân";break;
            case "otp":title="Xác thực OTP";break;
            default:title="Đăng nhập";
        }
        ui.header(root,title,activity::back,null);
        body=ui.body(root);
        if(!state.error.isEmpty()) banner(state.error,0xFFB42318,0xFFFFF1F0);
        if(!state.notice.isEmpty()) banner(state.notice,Ui.GREEN,0xFFECFDF3);
        if(state.busy){
            ProgressBar progress=new ProgressBar(activity);body.addView(progress,ui.lp(36,36));
            ui.space(body,10);ui.add(body,ui.text("Đang xử lý…",14,Ui.MUTED,false));ui.space(body,12);
        }
        switch(page){
            case "register":register();break;
            case "forgot":forgot();break;
            case "account":account();break;
            case "edit_account":edit();break;
            case "otp":otp();break;
            default:login();
        }
        if(state.busy)enable(body,false);
        ui.space(body,20);
        return root;
    }
    private void enable(View v,boolean value){v.setEnabled(value);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)enable(g.getChildAt(i),value);}}
    private void banner(String text,int color,int background){
        TextView label=ui.text(text,14,color,false);ui.pad(label,12,12);ui.surface(label,background,10,0);
        label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);ui.add(body,label);ui.space(body,12);
    }
    private void hero(String icon,String title,String caption){
        LinearLayout image=ui.column();image.setGravity(Gravity.CENTER);ui.pad(image,18,20);ui.surface(image,Ui.PALE,28,0);
        image.addView(new LineIcon(activity,icon,Ui.BLUE),ui.lp(80,70));ui.add(body,image);ui.space(body,22);
        TextView heading=ui.text(title,27,Ui.INK,true);heading.setGravity(Gravity.CENTER);ui.add(body,heading);ui.space(body,8);
        TextView sub=ui.text(caption,14,Ui.MUTED,false);sub.setGravity(Gravity.CENTER);ui.add(body,sub);ui.space(body,25);
    }
    private EditText field(String key,String label,String fallback,int type){
        ui.add(body,ui.text(label,14,Ui.INK,true));ui.space(body,7);
        EditText field=ui.input(label);field.setContentDescription(label);field.setInputType(type);
        field.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
        field.setSaveEnabled(false);field.setText(state.draft.getOrDefault(key,fallback));
        field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
        if(label.equals("Email"))field.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        if(type==InputType.TYPE_CLASS_PHONE)field.setAutofillHints(View.AUTOFILL_HINT_PHONE);
        int max=key.endsWith("goal")?500:key.endsWith("avatar")?90000:key.endsWith("password")||key.endsWith("confirm")?128:254;
        field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max)});
        field.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){state.draft.put(key,s.toString());field.setError(null);}
            public void afterTextChanged(Editable e){}
        });
        ui.add(body,field);ui.space(body,15);return field;
    }
    private EditText password(String key,String label){
        EditText field=field(key,label,"",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setAutofillHints(key.startsWith("register")?"newPassword":View.AUTOFILL_HINT_PASSWORD);
        CheckBox show=new CheckBox(activity);show.setText("Hiện "+label.toLowerCase(java.util.Locale.ROOT));show.setTextColor(Ui.MUTED);show.setMinHeight(ui.dp(48));
        show.setOnCheckedChangeListener((button,checked)->{int end=field.getSelectionStart();field.setTransformationMethod(checked?null:PasswordTransformationMethod.getInstance());field.setSelection(Math.max(0,end));});
        ui.add(body,show);return field;
    }
    private String text(EditText field){return field.getText().toString().trim();}
    private boolean check(EditText field,boolean valid,String message){if(valid)return true;field.setError(message);field.requestFocus();return false;}
    private boolean email(EditText field){return check(field,AccountValidation.validEmail(text(field)),"Vui lòng nhập email hợp lệ");}
    private boolean phone(EditText field){return check(field,AccountValidation.validPhone(text(field)),"Nhập số di động Việt Nam hợp lệ (10 số hoặc +84)");}
    private boolean name(EditText field){return check(field,AccountValidation.validName(text(field)),"Họ tên cần từ 2 đến 100 ký tự");}
    private void button(String label,boolean primary,Runnable action){ui.space(body,9);ui.add(body,ui.button(label,null,primary,action));}
    private void go(String page){if(!page.equals("login"))state.draft.remove("login.password");state.clearFeedback();activity.hideKeyboard();activity.show(page);}
    private void login(){
        hero("cap","Chào mừng trở lại","Đăng nhập để tiếp tục học tập");
        EditText email=field("login.email","Email","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password=password("login.password","Mật khẩu");
        button("Quên mật khẩu?",false,()->{state.draft.put("forgot.email",text(email));go("forgot");});
        button("Đăng nhập",true,()->{if(email(email)&&check(password,!password.getText().toString().isEmpty(),"Vui lòng nhập mật khẩu")){activity.hideKeyboard();state.signIn(AccountValidation.email(text(email)),password.getText().toString());}});
        ui.space(body,15);button("Chưa có tài khoản? Đăng ký",false,()->go("register"));
    }
    private void register(){
        ui.heading(body,"Tạo tài khoản học viên");
        ui.add(body,ui.text("Bắt đầu hành trình học tập với gia sư phù hợp ngay hôm nay.",14,Ui.MUTED,false));ui.space(body,20);
        EditText name=field("register.name","Họ và tên","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText email=field("register.email","Email","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText phone=field("register.phone","Số điện thoại","",InputType.TYPE_CLASS_PHONE);
        EditText password=password("register.password","Mật khẩu");
        ui.add(body,ui.text("Sử dụng từ 8 đến 128 ký tự.",12,Ui.MUTED,false));ui.space(body,10);
        EditText confirm=password("register.confirm","Xác nhận mật khẩu");
        CheckBox terms=new CheckBox(activity);terms.setText("Tôi đồng ý với điều khoản sử dụng và chính sách bảo mật");terms.setTextColor(Ui.INK);terms.setChecked(state.terms);terms.setMinHeight(ui.dp(48));
        terms.setOnCheckedChangeListener((button,checked)->{state.terms=checked;terms.setError(null);});ui.add(body,terms);
        button("Điều khoản và chính sách bảo mật",false,()->activity.dialog("Điều khoản sử dụng và bảo mật","Ứng dụng phục vụ học tập và kết nối gia sư. Bạn cần cung cấp thông tin chính xác và bảo mật tài khoản.\n\nEmail, họ tên, số điện thoại và thông tin hồ sơ được lưu để cung cấp chức năng tài khoản. Số điện thoại được gửi tới Google để xác thực và phòng chống lạm dụng. Không chia sẻ mật khẩu hoặc mã OTP.\n\nBạn có thể xem gia sư, cập nhật hồ sơ và yêu cầu đặt lại mật khẩu qua email."));
        button("Tạo tài khoản",true,()->{
            if(!name(name)||!email(email)||!phone(phone)||!check(password,AccountValidation.validPassword(password.getText().toString()),"Mật khẩu cần từ 8 đến 128 ký tự")||!check(confirm,confirm.getText().toString().equals(password.getText().toString()),"Mật khẩu xác nhận không khớp"))return;
            if(!terms.isChecked()){terms.setError("Bạn cần đồng ý điều khoản");terms.requestFocus();return;}
            UserProfile profile=new UserProfile();profile.name=text(name);profile.email=AccountValidation.email(text(email));profile.phone=AccountValidation.phone(text(phone));
            activity.hideKeyboard();state.register(profile,password.getText().toString());
        });
        button("Đã có tài khoản? Đăng nhập",false,()->{state.draft.remove("register.password");state.draft.remove("register.confirm");go("login");});
    }
    private void forgot(){
        hero("chat","Lấy lại mật khẩu","Nhập email đã đăng ký để nhận liên kết đặt lại mật khẩu.");
        EditText email=field("forgot.email","Email","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        button("Gửi liên kết",true,()->{if(email(email)){activity.hideKeyboard();state.resetPassword(AccountValidation.email(text(email)));}});
        button("Quay lại đăng nhập",false,()->go("login"));
    }
    private void otp(){
        hero("person","Xác thực số điện thoại","Nhận và nhập mã 6 số để hoàn tất đăng ký.");
        String phone=state.verificationPhone.isEmpty()?(state.profile==null?"":state.profile.phone):state.verificationPhone;
        EditText number=field("otp.phone","Số điện thoại",phone,InputType.TYPE_CLASS_PHONE);
        EditText code=field("otp.code","Mã OTP","",InputType.TYPE_CLASS_NUMBER);
        code.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});
        number.setEnabled(!state.sendingCode && state.verificationId==null);
        LinearLayout send=ui.button(state.verificationId==null?"Gửi mã OTP":"Gửi lại mã",null,false,()->{
            if(phone(number))state.sendCode(activity,AccountValidation.phone(text(number)),state.verificationId!=null);
        });ui.add(body,send);
        TextView timer=ui.text("",13,Ui.MUTED,false);ui.add(body,timer);
        Runnable tick=new Runnable(){public void run(){
            long remaining=Math.max(0,(state.resendAt-SystemClock.elapsedRealtime()+999)/1000);
            send.setEnabled(!state.busy&&!state.sendingCode&&remaining==0);send.setAlpha(send.isEnabled()?1f:0.5f);
            timer.setText(state.sendingCode?"Đang gửi mã…":remaining>0?"Có thể gửi lại sau "+remaining+" giây":"");
            if(timer.isAttachedToWindow())timer.postDelayed(this,1000);
        }};timer.post(tick);
        button("Xác nhận",true,()->{if(check(code,text(code).matches("[0-9]{6}"),"Mã OTP cần đúng 6 chữ số")){activity.hideKeyboard();state.verifyCode(text(code));}});
        button("Đổi số điện thoại",false,()->{
            if(state.sendingCode)return;
            // Preserve the cooldown when changing numbers, to avoid SMS request loops.
            state.changePhone();
        });
        button("Về đăng nhập",false,state::signOut);
    }
    private void avatar(String encoded,String name){
        LinearLayout center=ui.column();center.setGravity(Gravity.CENTER);ui.space(center,7);
        Bitmap bitmap=null;
        if(!encoded.isEmpty()&&encoded.length()<=90000){try{byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);}catch(IllegalArgumentException ignored){}}
        if(bitmap!=null){ImageView photo=new ImageView(activity);photo.setImageBitmap(bitmap);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setContentDescription("Ảnh đại diện");ui.surface(photo,Ui.PALE,48,0);photo.setClipToOutline(true);center.addView(photo,ui.lp(96,96));}
        else{TextView initials=ui.text(name.isEmpty()?"HV":name.substring(0,1).toUpperCase(java.util.Locale.ROOT),36,Ui.BLUE,true);initials.setGravity(Gravity.CENTER);ui.surface(initials,Ui.PALE,48,0);center.addView(initials,ui.lp(96,96));}
        ui.add(body,center);ui.space(body,16);
    }
    private void account(){
        if(!state.signedIn()){button("Đăng nhập",true,()->go("login"));return;}
        if(state.profile==null){
            if(!state.busy)button("Tải lại hồ sơ",true,()->state.loadProfile(false));
            button("Đăng xuất",false,state::signOut);return;
        }
        UserProfile p=state.profile;avatar(p.avatar,p.name);
        TextView name=ui.text(p.name,23,Ui.INK,true);name.setGravity(Gravity.CENTER);ui.add(body,name);ui.space(body,8);
        String role="TUTOR".equals(p.role)?"Gia sư":"ADMIN".equals(p.role)?"Quản trị viên":"Học viên";
        TextView badge=ui.text(role,14,Ui.MUTED,false);badge.setGravity(Gravity.CENTER);ui.add(body,badge);
        button("Chỉnh sửa hồ sơ",false,()->{state.draft.clear();go("edit_account");});ui.space(body,18);
        ui.heading(body,"Thông tin cá nhân");detail("Email",p.email);detail("Số điện thoại",p.phone);
        detail("Xác thực điện thoại",state.verified()?"Đã xác thực "+state.user().getPhoneNumber():"Chưa xác thực");
        detail("Địa chỉ",p.address);detail("Cấp học",p.level);detail("Khu vực",p.region);detail("Mục tiêu học tập",p.goal);
        if(!state.verified())button("Xác thực số điện thoại",true,()->go("otp"));
        button("Đổi mật khẩu qua email",false,()->state.resetPassword(p.email));
        button("Đăng xuất",false,()->new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Đăng xuất?").setMessage("Bạn có thể đăng nhập lại để xem thông tin cá nhân.").setNegativeButton("Hủy",null).setPositiveButton("Đăng xuất",(d,w)->state.signOut()).show());
    }
    private void detail(String label,String value){ui.add(body,ui.text(label,12,Ui.MUTED,false));ui.space(body,5);ui.add(body,ui.text(value.isEmpty()?"Chưa cập nhật":value,15,Ui.INK,false));ui.space(body,14);}
    private void edit(){
        if(state.profile==null){if(!state.busy)button("Tải lại hồ sơ",true,()->state.loadProfile(false));return;}
        UserProfile p=state.profile;
        avatar(state.draft.getOrDefault("edit.avatar",p.avatar),state.draft.getOrDefault("edit.name",p.name));
        button("Thay ảnh đại diện",false,activity::pickAvatar);ui.space(body,17);
        EditText name=field("edit.name","Họ và tên",p.name,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        detail("Email",p.email);ui.add(body,ui.text("Email đăng nhập không thay đổi tại đây.",12,Ui.MUTED,false));ui.space(body,12);
        EditText phone=field("edit.phone","Số điện thoại",p.phone,InputType.TYPE_CLASS_PHONE);
        EditText address=field("edit.address","Địa chỉ",p.address,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText level=field("edit.level","Cấp học",p.level,InputType.TYPE_CLASS_TEXT);
        level.setFocusable(false);level.setOnClickListener(v->{String[] levels={"Tiểu học","THCS","THPT","Đại học","Người đi làm","Khác"};new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Chọn cấp học").setItems(levels,(d,w)->level.setText(levels[w])).show();});
        EditText region=field("edit.region","Khu vực",p.region,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText goal=field("edit.goal","Mục tiêu học tập",p.goal,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);goal.setSingleLine(false);goal.setMinLines(3);goal.setGravity(Gravity.TOP);
        button("Lưu thay đổi",true,()->{
            if(!name(name)||!phone(phone)||!check(address,text(address).length()<=255,"Địa chỉ tối đa 255 ký tự")||!check(region,text(region).length()<=120,"Khu vực tối đa 120 ký tự")||!check(goal,text(goal).length()<=500,"Mục tiêu tối đa 500 ký tự"))return;
            UserProfile value=new UserProfile();value.name=text(name);value.email=p.email;value.phone=AccountValidation.phone(text(phone));
            value.address=text(address);value.level=text(level);value.region=text(region);value.goal=text(goal);value.avatar=state.draft.getOrDefault("edit.avatar",p.avatar);
            activity.hideKeyboard();state.saveProfile(value);
        });
    }
}

