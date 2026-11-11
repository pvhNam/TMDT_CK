package com.example.tmdt.ui.groupclass;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.browser.customtabs.CustomTabsIntent;
import com.example.tmdt.GroupAssignment;
import com.google.firebase.auth.FirebaseAuth;

/** Opens private Drive files in Google's viewer; grading remains in this application. */
public final class AssignmentAttachmentOpener {
    private AssignmentAttachmentOpener() {}
    public static void open(Fragment host, GroupAssignment.Attachment attachment) {
        if (!host.isAdded() || attachment == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            message(host, "Hãy đăng nhập để mở bài làm."); return;
        }
        if (!attachment.drive()) {
            message(host, "Tệp cũ cần được người gửi tải lại lên Google Drive."); return;
        }
        try {
            new CustomTabsIntent.Builder().setShowTitle(true).build()
                    .launchUrl(host.requireContext(), Uri.parse(attachment.driveUrl()));
        } catch (ActivityNotFoundException unavailable) {
            openLink(host, attachment.driveUrl());
        }
    }
    public static void openLink(Fragment host, String value) {
        if (!host.isAdded()) return;
        try {
            Uri uri = Uri.parse(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getHost().isEmpty() || uri.getUserInfo() != null) {
                message(host, "Đường dẫn phải bắt đầu bằng https:// và có địa chỉ hợp lệ."); return;
            }
            host.startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE));
        } catch (ActivityNotFoundException | IllegalArgumentException error) {
            message(host, "Không tìm thấy ứng dụng để mở bài làm. Hãy cài Google Drive hoặc trình duyệt.");
        }
    }
    private static void message(Fragment host, String text) {
        if (host.isAdded()) Toast.makeText(host.requireContext(), text, Toast.LENGTH_LONG).show();
    }
}
