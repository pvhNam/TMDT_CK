package com.example.tmdt;

import android.app.Activity;
import android.os.SystemClock;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Source;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Owns in-flight requests across rotation; credentials only live in memory. */
public final class AccountState extends ViewModel {
    final FirebaseAuth auth = FirebaseAuth.getInstance();
    final FirebaseFirestore db = FirebaseFirestore.getInstance();
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    final Map<String,String> draft = new HashMap<>();
    UserProfile profile;
    boolean busy, sendingCode, terms;
    String error = "", notice = "", destination;
    String verificationId, verificationPhone = "";
    PhoneAuthProvider.ForceResendingToken resendToken;
    long resendAt;
    private int generation;
    private int phoneAttempt;
    private android.os.CancellationSignal googlePicker;
    private TutorRegistration tutorRegistration;
    private boolean emailVerificationRequired;
    public boolean requiresEmailVerification(){return user()!=null&&emailVerificationRequired;}
    public String verificationEmail(){return user()==null||user().getEmail()==null?"":user().getEmail();}
    public long emailResendAt(){return SystemClock.elapsedRealtime()+PendingEmailRegistration.resendRemaining(uid());}
    public TutorRegistration tutorRegistration(){return tutorRegistration;}
    public void reviewTutor(TutorRegistration value){tutorRegistration=value;destination="review_tutor";notifyUi();}
    public void prepareTutor(TutorRegistration value){tutorRegistration=value;destination="tutor_evidence";notifyUi();}
    public void reportError(String value){error=value;notice="";notifyUi();}

    public androidx.lifecycle.LiveData<Integer> changes() { return changes; }
    public UserProfile profile() { return profile; }
    public boolean isBusy() { return busy; }
    public boolean isSendingCode() { return sendingCode; }
    public boolean acceptedTerms() { return terms; }
    public void acceptTerms(boolean accepted) { terms=accepted; }
    public String error() { return error; }
    public String notice() { return notice; }
    public String draft(String key,String fallback) { return draft.getOrDefault(key,fallback); }
    public void draft(String key,String value,boolean notify) { draft.put(key,value);if(notify)notifyUi(); }
    public void clearDraft() { draft.clear(); }
    public void removeDraft(String key) { draft.remove(key); }
    public long resendAt() { return resendAt; }
    public boolean codeSent() { return verificationId!=null; }
    public String verificationPhone() { return verificationPhone; }
    public String verifiedPhone() { return verified()?user().getPhoneNumber():""; }

    public AccountState() { auth.setLanguageCode("vi"); if (auth.getCurrentUser()!=null) loadProfile(false); }
    FirebaseUser user() { return auth.getCurrentUser(); }
    public boolean signedIn() { return user()!=null&&!emailVerificationRequired; }
    private Onboarding onboarding() { return new Onboarding(com.google.firebase.FirebaseApp.getInstance().getApplicationContext()); }
    public boolean verified() { return user()!=null && user().getPhoneNumber()!=null; }
    String uid() { return user()==null ? "guest" : user().getUid(); }
    void notifyUi() { changes.setValue(changes.getValue()+1); }
    private void begin() { busy=true; error=""; notice=""; notifyUi(); }
    private boolean current(int token, String uid) { return token==generation && user()!=null && uid.equals(user().getUid()); }
    private void failed(Exception e) { busy=false; error=message(e); notifyUi(); }
    public void clearFeedback() { error=""; notice=""; }
    public void signIn(String email, String password) {
        if(busy)return; begin(); int token=generation;
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(task->{
            if(token!=generation)return;
            if(!task.isSuccessful()) { failed(task.getException()); return; }
            draft.clear(); loadProfile(true);
        });
    }
    public void register(UserProfile value, String password) {
        if(busy)return; onboarding().applyStudent(value); begin(); int token=generation;
        auth.createUserWithEmailAndPassword(value.email,password).addOnCompleteListener(task->{
            if(token!=generation)return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            PendingEmailRegistration.save(uid(),value);draft.clear();terms=false;profile=null;
            emailVerificationRequired=true;busy=false;destination="email_verification";sendVerificationEmail();
        });
    }
    public void sendVerificationEmail(){
        if(busy||!requiresEmailVerification())return;
        if(PendingEmailRegistration.resendRemaining(uid())>0){reportError("Vui lòng chờ trước khi gửi lại email.");return;}
        begin();int token=generation;String owner=uid();
        user().sendEmailVerification().addOnCompleteListener(task->{
            if(!current(token,owner))return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            PendingEmailRegistration.emailSent(owner);
            busy=false;notice="Đã gửi liên kết xác thực đến "+verificationEmail()+". Hãy kiểm tra cả thư rác.";notifyUi();
        });
    }
    public void checkEmailVerification(){
        if(busy||!requiresEmailVerification())return;
        begin();completeEmailVerification(generation,uid());
    }
    private void completeEmailVerification(int token,String owner){
        user().reload().continueWithTask(loaded->{
            if(!current(token,owner))throw new IllegalStateException("Account changed");
            if(!loaded.isSuccessful())throw loaded.getException();
            if(!user().isEmailVerified())throw new FirebaseAuthException("ERROR_EMAIL_NOT_VERIFIED","Email chưa được xác thực.");
            return user().getIdToken(true);
        }).addOnCompleteListener(refreshed->{
            if(!current(token,owner))return;
            if(!refreshed.isSuccessful()){failed(refreshed.getException());return;}
            emailVerificationRequired=false;
            UserProfile value=PendingEmailRegistration.load(owner,verificationEmail());
            if(!AccountValidation.validName(value.name)||!AccountValidation.validPhone(value.phone)){
                profile=value;busy=false;destination="edit_account";notice="Email đã xác thực. Hãy hoàn thiện thông tin tài khoản.";notifyUi();return;
            }
            Map<String,Object> data=value.fields();data.put("vai_tro","STUDENT");data.put("la_gia_su",false);data.put("trang_thai","ACTIVE");
            data.put("created_at",FieldValue.serverTimestamp());data.put("updated_at",FieldValue.serverTimestamp());
            db.runTransaction(tx->{
                com.google.firebase.firestore.DocumentReference ref=db.collection("users").document(owner);
                com.google.firebase.firestore.DocumentSnapshot existing=tx.get(ref);
                if(existing.exists())return UserProfile.from(existing);
                tx.set(ref,data);return value;
            }).addOnCompleteListener(saved->{
                if(!current(token,owner))return;busy=false;
                if(saved.isSuccessful()){
                    profile=saved.getResult();PendingEmailRegistration.clear(owner);
                    user().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(profile.name).build());
                    destination=onboarding().finish(profile,"account");notice="Xác thực email và đăng ký thành công.";
                }else{
                    profile=value;destination="edit_account";
                    error="Email đã xác thực nhưng hồ sơ chưa được lưu. Hãy lưu lại thông tin. "+message(saved.getException());
                }
                notifyUi();
            });
        });
    }
    public void leaveEmailVerification(){
        if(busy)return;
        generation++;auth.signOut();profile=null;emailVerificationRequired=false;
        draft.clear();terms=false;clearFeedback();destination="login";
        notice="Bạn có thể đăng nhập lại để tiếp tục xác thực email.";notifyUi();
    }
    public void loadProfile(boolean navigate) {
        if(user()==null)return;
        begin(); final int token=generation; final String uid=uid();
        db.collection("users").document(uid).get(Source.SERVER).addOnCompleteListener(task->{
            if(!current(token,uid))return;
            busy=false;
            if(task.isSuccessful()) {
                if(task.getResult().exists()) profile=UserProfile.from(task.getResult());
                else {
                    if(!user().isEmailVerified()){
                        emailVerificationRequired=true;profile=null;destination="email_verification";
                        notice="Hãy xác thực email để hoàn tất đăng ký.";notifyUi();return;
                    }
                    UserProfile pending=PendingEmailRegistration.load(uid,verificationEmail());
                    if(AccountValidation.validName(pending.name)&&AccountValidation.validPhone(pending.phone)){
                        busy=true;completeEmailVerification(token,uid);return;
                    }
                    profile=new UserProfile(); profile.email=user().getEmail()==null?"":user().getEmail();
                    profile.name=user().getDisplayName()==null?"":user().getDisplayName();
                    onboarding().applyStudent(profile);
                    destination="edit_account"; notice="Hãy hoàn thiện hồ sơ của bạn.";
                }
                if(profile!=null && "BANNED".equals(profile.status)) {
                    signOut(); error="Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
                } else {
                    if(task.getResult().exists())syncSavedAvatar(token,uid);
                    if(task.getResult().exists() && destination==null && (navigate||onboarding().pending()))
                        destination=onboarding().finish(profile,navigate?"home":"account");
                }
            } else {
                error="Không tải được hồ sơ. " + message(task.getException());
                if(navigate)destination="account";
            }
            notifyUi();
        });
    }
    public void saveProfile(UserProfile value) {
        if(busy || user()==null)return;
        begin(); final int token=generation; final String uid=uid();
        // Transaction creates missing profiles after an interrupted registration, preserving server roles.
        db.runTransaction(tx->{
            com.google.firebase.firestore.DocumentReference ref=db.collection("users").document(uid);
            boolean exists=tx.get(ref).exists();
            Map<String,Object> data=value.fields(); data.put("updated_at",FieldValue.serverTimestamp());
            if(!exists){data.put("vai_tro","STUDENT");data.put("la_gia_su",false);data.put("trang_thai","ACTIVE");data.put("created_at",FieldValue.serverTimestamp());tx.set(ref,data);}
            else tx.update(ref,data);
            Map<String,Object> avatar=new HashMap<>();
            avatar.put("anh_dai_dien",value.avatar);avatar.put("updated_at",FieldValue.serverTimestamp());
            tx.set(db.collection("profile_avatars").document(uid),avatar);
            return null;
        }).addOnCompleteListener(task->{
            if(!current(token,uid))return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            value.role=profile==null?"STUDENT":profile.role;
            value.tutor=profile!=null && profile.tutor;
            profile=value; PendingEmailRegistration.clear(uid);draft.clear(); busy=false;
            user().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(value.name).build());
            destination=onboarding().finish(profile,"account"); notice="Đã lưu thông tin cá nhân."; notifyUi();
        });
    }
    public void resetPassword(String email) {
        if(busy)return;begin();int token=generation;
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task->{
            if(token!=generation)return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            busy=false;notice="Nếu email đã đăng ký, bạn sẽ nhận được liên kết đặt lại mật khẩu. Hãy kiểm tra cả thư rác.";notifyUi();
        });
    }
    public void startGoogleSignIn(Activity activity) {
        if(busy)return;
        if(GoogleSignIn.clientId(activity).isEmpty()) {
            reportError(activity.getString(R.string.google_not_configured));return;
        }
        begin();
        final android.os.CancellationSignal picker=new android.os.CancellationSignal();
        googlePicker=picker;
        GoogleSignIn.launch(activity,picker,new GoogleSignIn.Callback() {
            @Override public void token(String idToken) {
                if(googlePicker!=picker)return;
                googlePicker=null;busy=false;signInWithGoogle(idToken);
            }
            @Override public void stopped(String message) {
                if(googlePicker!=picker)return;
                googlePicker=null;busy=false;error=message;notifyUi();
            }
        });
    }
    public void cancelGooglePicker() {
        if(googlePicker==null)return;
        android.os.CancellationSignal picker=googlePicker;googlePicker=null;
        picker.cancel();busy=false;notifyUi();
    }
    public void signInWithGoogle(String idToken) {
        if(busy)return;
        if(idToken==null || idToken.trim().isEmpty()) {reportError("Không nhận được thông tin đăng nhập Google. Hãy thử lại.");return;}
        begin();int token=generation;
        auth.signInWithCredential(com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken,null))
                .addOnCompleteListener(task->{
                    if(token!=generation)return;
                    if(!task.isSuccessful()) {
                        busy=false;
                        Exception cause=task.getException();
                        if(cause instanceof FirebaseAuthException) {
                            String code=((FirebaseAuthException)cause).getErrorCode();
                            if("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL".equals(code))
                                error="Email này đã có tài khoản. Hãy đăng nhập bằng phương thức đã đăng ký trước đó.";
                            else if("ERROR_INVALID_CREDENTIAL".equals(code))
                                error="Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Hãy chọn lại tài khoản.";
                            else if("ERROR_OPERATION_NOT_ALLOWED".equals(code))
                                error="Đăng nhập Google chưa được bật trong Firebase Authentication.";
                            else error=message(cause);
                        } else error=message(cause);
                        notifyUi();return;
                    }
                    draft.clear();destination=null;profile=null;loadProfile(true);
                });
    }
    public void submitTutor(){
        TutorRegistration value=tutorRegistration;
        if(busy||user()==null||profile==null||value==null)return;
        if(!value.error().isEmpty()){reportError(value.error());return;}
        android.content.Context context=com.google.firebase.FirebaseApp.getInstance().getApplicationContext();
        TutorEvidenceDraft evidence=TutorEvidenceDraft.load(context,uid());
        if(!evidence.error().isEmpty()){reportError(evidence.error());return;}
        begin();final int token=generation;final String owner=uid();
        notice="Đang kiểm tra ảnh minh chứng trên Firebase…";notifyUi();
        TutorEvidenceAccess.verify(evidence.files,owner).addOnCompleteListener(checked->{
            if(!current(token,owner))return;
            if(!checked.isSuccessful()){
                busy=false;notice="";error=TutorEvidenceAccess.message(checked.getException());notifyUi();return;
            }
            notice="Đang gửi hồ sơ xét duyệt…";notifyUi();
            submitVerifiedTutor(value,evidence,token,owner,context);
        });
    }
    private void submitVerifiedTutor(TutorRegistration value,TutorEvidenceDraft evidence,int token,String owner,android.content.Context context){
        db.runTransaction(tx->{
            com.google.firebase.firestore.DocumentReference userRef=db.collection("users").document(owner);
            com.google.firebase.firestore.DocumentReference tutorRef=db.collection("tutor_profiles").document(owner);
            com.google.firebase.firestore.DocumentSnapshot accountDoc=tx.get(userRef),existing=tx.get(tutorRef);
            if(existing.exists()){
                if(Boolean.TRUE.equals(accountDoc.getBoolean("la_gia_su")))return UserProfile.from(accountDoc);
                throw new IllegalStateException("Existing tutor profile needs administrator review");
            }
            if(!accountDoc.exists())throw new IllegalStateException("Missing account");
            Map<String,Object> data=new HashMap<>();
            data.put("user_id",owner);data.put("ho_ten",value.name);data.put("hoc_van",accountDoc.getString("cap_hoc")==null?"":accountDoc.getString("cap_hoc"));
            data.put("truong",evidence.school.trim());data.put("chuyen_nganh",evidence.major.trim());data.put("gioi_thieu","");
            data.put("khu_vuc",accountDoc.getString("khu_vuc")==null?"":accountDoc.getString("khu_vuc"));
            data.put("so_nam_kinh_nghiem",value.experience);data.put("hinh_thuc_day",value.mode);
            data.put("nhan_lop",true);data.put("trang_thai_xac_minh","PENDING");
            data.put("created_at",FieldValue.serverTimestamp());data.put("updated_at",FieldValue.serverTimestamp());
            tx.set(tutorRef,data);
            Map<String,Object> application=evidence.fields();application.put("user_id",owner);application.put("ho_ten",value.name);
            application.put("mon_hoc",value.subjectName);application.put("cap_lop",value.levelLabel());application.put("hoc_phi",value.price);
            application.put("trang_thai_xac_minh","PENDING");application.put("submitted_at",FieldValue.serverTimestamp());
            application.put("created_at",FieldValue.serverTimestamp());application.put("updated_at",FieldValue.serverTimestamp());
            tx.set(db.collection("tutor_applications").document(owner),application);
            for(String level:value.levels){
                Map<String,Object> offering=new HashMap<>();offering.put("tutor_id",owner);
                offering.put("subject_id",value.subjectId);offering.put("cap_lop",level);offering.put("hoc_phi_moi_buoi",value.price);
                tx.set(db.collection("tutor_subjects").document(owner+"_"+value.subjectId+"_"+level),offering);
            }
            Map<String,Object> update=new HashMap<>();update.put("la_gia_su",true);update.put("ho_ten",value.name);
            update.put("so_dien_thoai",value.phone);update.put("updated_at",FieldValue.serverTimestamp());
            tx.update(userRef,update);return null;
        }).addOnCompleteListener(task->{
            if(!current(token,owner))return;
            if(!task.isSuccessful()){
                android.util.Log.w("TutorEvidence","Tutor application transaction failed",task.getException());
                if(task.getException() instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)task.getException()).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED){
                    busy=false;notice="";error="Ảnh đã được kiểm tra nhưng Firebase từ chối gửi hồ sơ. Kiểm tra rules của hồ sơ, môn giảng dạy và thông tin tài khoản. (PERMISSION_DENIED)";notifyUi();
                }else failed(task.getException());
                return;
            }
            if(task.getResult()!=null)profile=task.getResult();
            else {profile.tutor=true;profile.name=value.name;profile.phone=value.phone;}
            tutorRegistration=null;draft.keySet().removeIf(key->key.startsWith("tutor."));
            TutorEvidenceDraft.clear(context,owner);
            busy=false;destination="tutor_pending";
            notice="Đã gửi hồ sơ gia sư thành công. Bạn có thể theo dõi kết quả xét duyệt.";
            notifyUi();
        });
    }
    /** Backfill only the image for accounts created before the shared avatar document existed. */
    private void syncSavedAvatar(int token,String owner) {
        db.runTransaction(tx->{
            com.google.firebase.firestore.DocumentSnapshot userDoc=tx.get(db.collection("users").document(owner));
            com.google.firebase.firestore.DocumentReference avatarRef=db.collection("profile_avatars").document(owner);
            com.google.firebase.firestore.DocumentSnapshot avatarDoc=tx.get(avatarRef);
            if(!userDoc.exists() || !"ACTIVE".equals(userDoc.getString("trang_thai")))return null;
            String image=userDoc.getString("anh_dai_dien");
            if(image==null || (image.isEmpty()&&!avatarDoc.exists()) || image.equals(avatarDoc.getString("anh_dai_dien")))return null;
            Map<String,Object> data=new HashMap<>();data.put("anh_dai_dien",image);data.put("updated_at",FieldValue.serverTimestamp());
            tx.set(avatarRef,data);return null;
        }).addOnFailureListener(e->{
            if(current(token,owner)&&!busy&&notice.isEmpty()){
                notice="Chưa đồng bộ ảnh đại diện. "+message(e);notifyUi();
            }
        });
    }
    public void sendCode(Activity activity, String phone, boolean resend) {
        if(sendingCode || busy || user()==null)return;
        if(SystemClock.elapsedRealtime()<resendAt)return;
        verificationPhone=phone; sendingCode=true; error=""; notice="Đang gửi mã xác thực…";notifyUi();
        final int token=generation; final String uid=uid(); final int attempt=++phoneAttempt;
        PhoneAuthOptions.Builder options=PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phone)
                .setTimeout(60L,TimeUnit.SECONDS).setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                    @Override public void onVerificationCompleted(PhoneAuthCredential credential){
                        if(!current(token,uid)||attempt!=phoneAttempt)return;sendingCode=false;linkPhone(credential);
                    }
                    @Override public void onVerificationFailed(FirebaseException e){
                        if(!current(token,uid)||attempt!=phoneAttempt)return;sendingCode=false;notice="";error=message(e);notifyUi();
                    }
                    @Override public void onCodeSent(String id,PhoneAuthProvider.ForceResendingToken tokenValue){
                        if(!current(token,uid)||attempt!=phoneAttempt)return;
                        verificationId=id;resendToken=tokenValue;sendingCode=false;
                        resendAt=SystemClock.elapsedRealtime()+60000;notice="Đã gửi mã 6 số đến "+phone;notifyUi();
                    }
                    @Override public void onCodeAutoRetrievalTimeOut(String id){
                        if(!current(token,uid)||attempt!=phoneAttempt)return;sendingCode=false;verificationId=id;notifyUi();
                    }
                });
        if(resend && resendToken!=null)options.setForceResendingToken(resendToken);
        PhoneAuthProvider.verifyPhoneNumber(options.build());
    }
    public void changePhone() {
        phoneAttempt++;verificationId=null;resendToken=null;verificationPhone="";
        draft.remove("otp.code");clearFeedback();notifyUi();
    }
    public void verifyCode(String code) {
        if(verificationId==null){error="Hãy gửi mã xác thực trước.";notifyUi();return;}
        linkPhone(PhoneAuthProvider.getCredential(verificationId,code));
    }
    private void linkPhone(PhoneAuthCredential credential) {
        if(busy || user()==null)return; begin(); final int token=generation; final String uid=uid();
        user().linkWithCredential(credential).addOnCompleteListener(task->{
            if(!current(token,uid))return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            signOut();notice="Xác thực thành công. Bạn có thể đăng nhập.";destination="login";notifyUi();
        });
    }
    public void signOut() {
        onboarding().existingAccount();
        cancelGooglePicker();
        GoogleSignIn.clearSession(com.google.firebase.FirebaseApp.getInstance().getApplicationContext());
        tutorRegistration=null;emailVerificationRequired=false;
        generation++;phoneAttempt++;auth.signOut();profile=null;draft.clear();terms=false;busy=false;sendingCode=false;
        verificationId=null;verificationPhone="";resendToken=null;resendAt=0;clearFeedback();
        destination="login";notifyUi();
    }
    @Override protected void onCleared() {
        if(googlePicker!=null){googlePicker.cancel();googlePicker=null;}
        super.onCleared();
    }
    static String message(Exception e) {
        if(e instanceof FirebaseNetworkException)return "Không có kết nối mạng. Vui lòng thử lại.";
        if(e instanceof FirebaseTooManyRequestsException)return "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.";
        if(e instanceof FirebaseAuthException){
            String code=((FirebaseAuthException)e).getErrorCode();
            switch(code){
                case "ERROR_EMAIL_NOT_VERIFIED":return "Email chưa được xác thực. Hãy bấm liên kết trong email, rồi quay lại đây để kiểm tra.";
                case "ERROR_EMAIL_ALREADY_IN_USE":return "Email này đã được đăng ký. Hãy đăng nhập hoặc lấy lại mật khẩu.";
                case "ERROR_INVALID_EMAIL":return "Email không hợp lệ.";
                case "ERROR_WEAK_PASSWORD":return "Mật khẩu chưa đáp ứng chính sách bảo mật của hệ thống.";
                case "ERROR_USER_DISABLED":return "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.";
                case "ERROR_INVALID_VERIFICATION_CODE":return "Mã OTP không đúng. Vui lòng kiểm tra và thử lại.";
                case "ERROR_SESSION_EXPIRED":return "Mã OTP đã hết hạn. Vui lòng gửi lại mã.";
                case "ERROR_CREDENTIAL_ALREADY_IN_USE":return "Số điện thoại đã liên kết với tài khoản khác.";
                case "ERROR_INVALID_PHONE_NUMBER":return "Số điện thoại không hợp lệ.";
                case "ERROR_OPERATION_NOT_ALLOWED":return "Phương thức xác thực chưa được bật trên hệ thống.";
                case "ERROR_INVALID_APP_CREDENTIAL": case "ERROR_APP_NOT_AUTHORIZED":
                    return "Ứng dụng chưa được cấu hình xác thực điện thoại. Vui lòng liên hệ quản trị viên.";
                case "ERROR_INVALID_CREDENTIAL": case "ERROR_WRONG_PASSWORD": case "ERROR_USER_NOT_FOUND":
                    return "Email hoặc mật khẩu không đúng.";
                default:return "Không thể xác thực lúc này. Vui lòng thử lại. ("+code+")";
            }
        }
        if(e instanceof FirebaseFirestoreException){
            FirebaseFirestoreException.Code code=((FirebaseFirestoreException)e).getCode();
            if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED)return "Chưa có quyền truy cập hồ sơ. Hãy kiểm tra cấu hình Firestore.";
            if(code==FirebaseFirestoreException.Code.UNAVAILABLE)return "Không kết nối được máy chủ hồ sơ. Vui lòng thử lại.";
        }
        return "Không hoàn tất được yêu cầu. Vui lòng thử lại.";
    }
}

