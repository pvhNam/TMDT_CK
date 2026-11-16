package com.example.tmdt.ui.groupclass;

import android.accounts.Account;
import android.app.Activity;
import android.app.PendingIntent;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Collections;
import java.util.List;

/** Requests per-file Drive access only for an action initiated in the current assignment view. */
public final class AssignmentDriveAuthorization {
    public static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private final Fragment host;
    private final ActivityResultLauncher<IntentSenderRequest> resolution;
    private FirebaseAuth observedAuth;
    private FirebaseAuth.AuthStateListener authListener;
    private Callback pending;
    private View requestedView;
    private String requestedUid = "", requestedEmail = "";
    private int generation;
    private boolean awaitingResolution;

    public interface Callback {
        void authorized(String accessToken);
        void failed(String userMessage);
    }

    /** Construct with the Fragment, before onCreate, so its Activity Result registration is stable. */
    public AssignmentDriveAuthorization(Fragment host) {
        this.host = host;
        resolution = host.registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (!awaitingResolution || pending == null) return;
            int ticket = generation;
            awaitingResolution = false;
            if (!current(ticket)) { cancelIfCurrent(ticket); return; }
            if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                fail(ticket, "Bạn đã hủy cấp quyền Google Drive. Bản nháp bài làm vẫn được giữ.");
                return;
            }
            try {
                accept(ticket, Identity.getAuthorizationClient(host.requireActivity())
                        .getAuthorizationResultFromIntent(result.getData()));
            } catch (ApiException error) {
                fail(ticket, message(error));
            } catch (RuntimeException unavailable) {
                fail(ticket, "Chưa kết nối được Google Drive. Hãy thử lại.");
            }
        });
        host.getViewLifecycleOwnerLiveData().observe(host, owner -> {
            if (owner != null) owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                if (event == Lifecycle.Event.ON_DESTROY) cancel();
            });
        });
        host.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_DESTROY) cancel();
        });
    }

    @MainThread public boolean busy() { return pending != null; }

    @MainThread public void authorize(Callback callback) {
        if (callback == null) return;
        if (busy()) {
            callback.failed("Đang chờ kết nối Google Drive. Vui lòng hoàn tất bước cấp quyền trước.");
            return;
        }
        if (!host.isAdded() || host.getView() == null
                || !host.getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            callback.failed("Mở lại phần bài tập rồi thử kết nối Google Drive.");
            return;
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        String email = user == null || user.getEmail() == null ? "" : user.getEmail().trim();
        if (user == null || email.isEmpty()) {
            callback.failed("Đăng nhập bằng tài khoản có email trước khi kết nối Google Drive.");
            return;
        }
        if (!user.isEmailVerified()) {
            callback.failed("Xác minh email của tài khoản trước khi tải bài lên Google Drive.");
            return;
        }
        int services = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(host.requireContext());
        if (services != ConnectionResult.SUCCESS) {
            callback.failed("Thiết bị cần Google Play services đang hoạt động và được cập nhật để kết nối Drive.");
            return;
        }
        pending = callback;
        requestedView = host.getView();
        requestedUid = user.getUid();
        requestedEmail = email;
        int ticket = ++generation;
        observedAuth = auth;
        authListener = ignored -> {
            if (ticket == generation && pending != null && !sameUser()) {
                fail(ticket, "Phiên đăng nhập đã thay đổi. Hãy kết nối lại Google Drive.");
            }
        };
        auth.addAuthStateListener(authListener);
        try {
            AuthorizationRequest request = AuthorizationRequest.builder()
                    .setAccount(new Account(email, "com.google"))
                    .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_FILE_SCOPE)))
                    .build();
            Identity.getAuthorizationClient(host.requireActivity()).authorize(request)
                    .addOnSuccessListener(result -> {
                        if (!current(ticket)) { cancelIfCurrent(ticket); return; }
                        if (!result.hasResolution()) { accept(ticket, result); return; }
                        PendingIntent consent = result.getPendingIntent();
                        if (consent == null) {
                            fail(ticket, "Chưa mở được bước cấp quyền Google Drive. Hãy thử lại.");
                            return;
                        }
                        try {
                            awaitingResolution = true;
                            resolution.launch(new IntentSenderRequest.Builder(consent.getIntentSender()).build());
                        } catch (RuntimeException unavailable) {
                            fail(ticket, "Không mở được bước cấp quyền Google Drive trên thiết bị này.");
                        }
                    })
                    .addOnFailureListener(error -> fail(ticket, message(error)));
        } catch (RuntimeException unavailable) {
            fail(ticket, "Chưa khởi tạo được kết nối Google Drive. Hãy thử lại.");
        }
    }

    private void accept(int ticket, AuthorizationResult result) {
        if (!current(ticket)) { cancelIfCurrent(ticket); return; }
        if (result == null || result.hasResolution()) {
            fail(ticket, "Chưa hoàn tất cấp quyền Google Drive. Hãy thử lại.");
            return;
        }
        List<String> scopes = result.getGrantedScopes();
        String token = result.getAccessToken();
        if (token == null || token.isEmpty() || scopes == null || !scopes.contains(DRIVE_FILE_SCOPE)) {
            fail(ticket, "Cần cho phép ứng dụng quản lý các tệp Drive do ứng dụng tạo để tải bài lên.");
            return;
        }
        Callback callback = pending;
        cancel();
        callback.authorized(token);
    }

    private boolean sameUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && requestedUid.equals(user.getUid()) && user.getEmail() != null
                && requestedEmail.equalsIgnoreCase(user.getEmail().trim()) && user.isEmailVerified();
    }
    private boolean current(int ticket) {
        return ticket == generation && pending != null && sameUser() && host.isAdded()
                && requestedView != null && host.getView() == requestedView
                && host.getViewLifecycleOwner().getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED;
    }
    private void cancelIfCurrent(int ticket) { if (ticket == generation) cancel(); }
    private void fail(int ticket, String text) {
        if (ticket != generation || pending == null) return;
        Callback callback = pending;
        boolean deliver = host.isAdded() && requestedView != null && host.getView() == requestedView;
        cancel();
        if (deliver) callback.failed(text);
    }
    @MainThread public void cancel() {
        generation++;
        pending = null;
        requestedView = null;
        requestedUid = requestedEmail = "";
        awaitingResolution = false;
        if (observedAuth != null && authListener != null) observedAuth.removeAuthStateListener(authListener);
        observedAuth = null;
        authListener = null;
    }
    private static String message(Exception failure) {
        if (failure instanceof ApiException) {
            int code = ((ApiException) failure).getStatusCode();
            if (code == CommonStatusCodes.CANCELED) return "Bạn đã hủy cấp quyền Google Drive. Bản nháp bài làm vẫn được giữ.";
            if (code == CommonStatusCodes.NETWORK_ERROR || code == CommonStatusCodes.TIMEOUT)
                return "Chưa kết nối được Google. Kiểm tra mạng rồi thử lại.";
            if (code == CommonStatusCodes.SIGN_IN_REQUIRED)
                return "Thêm tài khoản Google trùng email đang dùng trong ứng dụng vào thiết bị, rồi thử lại.";
            if (code == CommonStatusCodes.DEVELOPER_ERROR)
                return "Kết nối Google Drive của ứng dụng chưa được cấu hình. Vui lòng liên hệ người quản trị.";
        }
        return "Chưa cấp được quyền Google Drive. Chọn tài khoản Google trùng email đang dùng trong ứng dụng rồi thử lại.";
    }
}
