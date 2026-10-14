package com.example.tmdt.ui.exchange;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import com.example.tmdt.ReportState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Chọn ảnh minh chứng (màn 47, 48): mở trình chọn ảnh, nén từng ảnh ở luồng nền rồi trả kết quả trên luồng chính.
 * Phải tạo trong lúc khởi tạo Fragment (trường final) vì dùng registerForActivityResult.
 */
public final class EvidencePicker {
    public interface Result { void picked(List<ReportState.Attachment> files, int failed); }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private final Fragment fragment;
    private final ActivityResultLauncher<String> launcher;
    private Result result;
    private Consumer<Boolean> busy = value -> {};

    public EvidencePicker(Fragment fragment) {
        this.fragment = fragment;
        launcher = fragment.registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), this::process);
    }
    public void onResult(Result result, Consumer<Boolean> busy) { this.result = result; this.busy = busy; }
    public void pick() { launcher.launch("image/*"); }

    private void process(List<Uri> uris) {
        if (uris == null || uris.isEmpty() || fragment.getContext() == null) return;
        Context context = fragment.requireContext().getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        busy.accept(true);
        WORKER.execute(() -> {
            List<ReportState.Attachment> files = new ArrayList<>();
            int failed = 0;
            for (Uri uri : uris) {
                try { files.add(ExUi.compress(context, uri, name(context, uri))); }
                catch (IOException | RuntimeException e) { failed++; }
            }
            final int errors = failed;
            main.post(() -> {
                busy.accept(false);
                if (fragment.isAdded() && result != null) result.picked(files, errors);
            });
        });
    }

    private static String name(Context context, Uri uri) {
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (RuntimeException ignored) { /* Dùng tên mặc định. */ }
        if (name == null || name.isEmpty()) name = "minh_chung.jpg";
        int dot = name.lastIndexOf('.');
        return (dot > 0 ? name.substring(0, dot) : name) + ".jpg";
    }
}
