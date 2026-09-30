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
    public boolean signedIn() { return user()!=null; }
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
        if(busy)return; begin(); int token=generation;
        auth.createUserWithEmailAndPassword(value.email,password).addOnCompleteListener(task->{
            if(token!=generation)return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            draft.clear(); terms=false; profile=value;
            String uid=uid();
            Map<String,Object> data=value.fields(); data.put("vai_tro","STUDENT");
            data.put("la_gia_su",false); data.put("trang_thai","ACTIVE");
            data.put("created_at",FieldValue.serverTimestamp()); data.put("updated_at",FieldValue.serverTimestamp());
            db.runTransaction(tx->{ tx.set(db.collection("users").document(uid), data); return null; }).addOnCompleteListener(saved->{
                if(!current(token,uid))return;
                busy=false;
                if(saved.isSuccessful()) {
                    user().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(value.name).build());
                    destination="account"; notice="Tạo tài khoản thành công. Chào mừng bạn!";
                } else {
                    // Auth already exists: let the owner retry saving, never create another account.
                    destination="edit_account";
                    error="Tài khoản đã tạo nhưng hồ sơ chưa được lưu. Hãy lưu lại thông tin. " + message(saved.getException());
                }
                notifyUi();
            });
        });
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
                    profile=new UserProfile(); profile.email=user().getEmail()==null?"":user().getEmail();
                    profile.name=user().getDisplayName()==null?"":user().getDisplayName();
                    destination="edit_account"; notice="Hãy hoàn thiện hồ sơ của bạn.";
                }
                if(profile!=null && "BANNED".equals(profile.status)) {
                    signOut(); error="Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
                } else if(navigate && destination==null) destination="home";
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
            return null;
        }).addOnCompleteListener(task->{
            if(!current(token,uid))return;
            if(!task.isSuccessful()){failed(task.getException());return;}
            value.role=profile==null?"STUDENT":profile.role;
            value.tutor=profile!=null && profile.tutor;
            profile=value; draft.clear(); busy=false;
            user().updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(value.name).build());
            destination="account"; notice="Đã lưu thông tin cá nhân."; notifyUi();
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
        generation++;phoneAttempt++;auth.signOut();profile=null;draft.clear();terms=false;busy=false;sendingCode=false;
        verificationId=null;verificationPhone="";resendToken=null;resendAt=0;clearFeedback();
        destination="login";notifyUi();
    }
    static String message(Exception e) {
        if(e instanceof FirebaseNetworkException)return "Không có kết nối mạng. Vui lòng thử lại.";
        if(e instanceof FirebaseTooManyRequestsException)return "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.";
        if(e instanceof FirebaseAuthException){
            String code=((FirebaseAuthException)e).getErrorCode();
            switch(code){
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

