package com.example.tmdt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Phần chung của các ViewModel Thành viên 4 (hợp đồng, đánh giá, báo cáo, quản trị):
 * tín hiệu thay đổi cho giao diện, trạng thái đang xử lý, lỗi và thông báo một lần.
 */
public abstract class MemberState extends ViewModel {
    protected final FirebaseAuth auth = FirebaseAuth.getInstance();
    protected final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    protected boolean busy;
    protected String notice = "";

    public LiveData<Integer> changes() { return changes; }
    protected void notifyUi() { Integer value = changes.getValue(); changes.setValue(value == null ? 1 : value + 1); }
    protected String uid() { FirebaseUser user = auth.getCurrentUser(); return user == null ? null : user.getUid(); }
    public String myId() { String me = uid(); return me == null ? "" : me; }
    public boolean signedIn() { return uid() != null; }
    public boolean busy() { return busy; }
    /** Thông báo hiển thị một lần (Snackbar). */
    public String takeNotice() { String value = notice; notice = ""; return value; }
    protected void done(String message) { busy = false; notice = message; notifyUi(); }
    protected void failed(Exception e) { busy = false; notice = Docs.message(e); notifyUi(); }
}
