package com.example.tmdt;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Validates a private copy, uploads it to the user's Drive, then shares read-only access. */
public final class AssignmentFileTransfer {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    public static final String[] MIME_TYPES = {"image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(MIME_TYPES));
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newFixedThreadPool(2, work -> {
        Thread thread = new Thread(work, "assignment-drive"); thread.setDaemon(true); return thread;
    });
    private AssignmentFileTransfer() {}
    public interface Callback {
        void progress(int percent);
        void uploaded(GroupAssignment.Attachment attachment);
        void failed(Exception failure);
    }
    public interface SyncCallback { void done(); void failed(Exception failure); }
    public static final class Operation implements AssignmentDriveApi.Control {
        private volatile boolean cancelled;
        private volatile HttpURLConnection connection;
        private volatile Future<?> preparing;
        private volatile File local;
        public void cancel() {
            cancelled = true;
            HttpURLConnection current = connection; if (current != null) current.disconnect();
            Future<?> task = preparing; if (task != null) task.cancel(true);
            clean();
        }
        @Override public void check() throws IOException {
            if (cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Đã hủy tải tệp.");
        }
        @Override public void attach(HttpURLConnection value) throws IOException {
            connection = value;
            if (cancelled) { value.disconnect(); throw new InterruptedIOException("Đã hủy tải tệp."); }
            check();
        }
        @Override public void release(HttpURLConnection value) { if (connection == value) connection = null; }
        private void clean() { File file = local; if (file != null) file.delete(); }
    }
    public static boolean allowedMime(String mime) { return ALLOWED.contains(mime); }
    public static Operation upload(Context context, Uri source, String prefix, String accessToken, String expectedEmail,
                                   List<String> readerEmails, Callback callback) {
        Operation operation = new Operation();
        Context app = context.getApplicationContext();
        final List<String> readers = readerEmails == null ? new ArrayList<>() : new ArrayList<>(readerEmails);
        operation.preparing = IO.submit(() -> {
            try {
                operation.check();
                if (prefix == null || !prefix.matches("group_assignments/[^/]+/[^/]+/(task|submissions)/[^/]+/"))
                    throw problem("Thông tin lớp hoặc bài tập không hợp lệ. Hãy mở lại bài tập.");
                String name = "bai-tap"; long declared = -1;
                try (Cursor cursor = app.getContentResolver().query(source, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int n = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME), s = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (n >= 0 && !cursor.isNull(n)) name = cursor.getString(n);
                        if (s >= 0 && !cursor.isNull(s)) declared = cursor.getLong(s);
                    }
                }
                name = name == null ? "bai-tap" : name.replace((char) 47, (char) 95).replace((char) 92, (char) 95).replaceAll("\\p{Cntrl}", "_").trim();
                if (name.isEmpty() || name.equals(".") || name.equals("..")) name = "bai-tap";
                if (name.length() > 150) name = name.substring(name.length() - 150);
                String mime = app.getContentResolver().getType(source);
                mime = mime == null ? "" : mime.split(";")[0].trim().toLowerCase(Locale.ROOT);
                if ("image/jpg".equals(mime)) mime = "image/jpeg";
                if (mime.isEmpty() || "application/octet-stream".equals(mime)) {
                    int dot = name.lastIndexOf('.');
                    mime = dot < 0 ? "" : MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1).toLowerCase(Locale.ROOT));
                }
                if (!allowedMime(mime)) throw problem("Chọn ảnh, PDF, Word hoặc tệp văn bản được hỗ trợ.");
                if (declared > MAX_BYTES) throw problem("Tệp đính kèm tối đa 10 MB.");
                File directory = new File(app.getCacheDir(), "assignment_uploads");
                if (!directory.isDirectory() && !directory.mkdirs()) throw problem("Chưa chuẩn bị được tệp tải lên.");
                File file = File.createTempFile("upload-", ".tmp", directory); operation.local = file;
                long actual = 0;
                try (InputStream input = app.getContentResolver().openInputStream(source); FileOutputStream output = new FileOutputStream(file)) {
                    if (input == null) throw problem("Không đọc được tệp đã chọn. Hãy chọn lại.");
                    byte[] buffer = new byte[16384]; int length;
                    while ((length = input.read(buffer)) != -1) {
                        operation.check(); actual += length;
                        if (actual > MAX_BYTES) throw problem("Tệp đính kèm tối đa 10 MB.");
                        output.write(buffer, 0, length);
                    }
                }
                if (actual == 0) throw problem("Tệp đã chọn không có nội dung.");
                final String path = prefix + UUID.randomUUID(), filename = name, contentType = mime;
                final long bytes = actual; final int[] lastPercent = {-1};
                MAIN.post(() -> { if (!operation.cancelled) callback.progress(0); });
                AssignmentDriveApi.Uploaded result = new AssignmentDriveApi().upload(file, path, name, mime,
                        accessToken, expectedEmail, readers, operation, (sent, total) -> {
                            int percent = total <= 0 ? 0 : (int) Math.min(99, sent * 100 / total);
                            if (percent != lastPercent[0]) {
                                lastPercent[0] = percent;
                                MAIN.post(() -> { if (!operation.cancelled) callback.progress(percent); });
                            }
                        });
                GroupAssignment.Attachment attachment = new GroupAssignment.Attachment(path, filename, contentType, bytes, result.fileId);
                operation.clean();
                MAIN.post(() -> {
                    if (!operation.cancelled) { callback.progress(100); callback.uploaded(attachment); }
                });
            } catch (Exception failure) {
                operation.clean();
                Exception friendly = friendly(failure);
                MAIN.post(() -> { if (!operation.cancelled) callback.failed(friendly); });
            }
        });
        return operation;
    }
    public static Operation syncReaders(String accessToken, String expectedEmail, List<GroupAssignment.Attachment> files,
                                        List<String> readerEmails, SyncCallback callback) {
        Operation operation = new Operation();
        final List<GroupAssignment.Attachment> attachments = files == null ? new ArrayList<>() : new ArrayList<>(files);
        final List<String> readers = readerEmails == null ? new ArrayList<>() : new ArrayList<>(readerEmails);
        operation.preparing = IO.submit(() -> {
            try {
                operation.check();
                new AssignmentDriveApi().syncReaders(accessToken, expectedEmail, attachments, readers, operation);
                MAIN.post(() -> { if (!operation.cancelled) callback.done(); });
            } catch (Exception failure) {
                Exception friendly = friendly(failure);
                MAIN.post(() -> { if (!operation.cancelled) callback.failed(friendly); });
            }
        });
        return operation;
    }
    private static AssignmentDriveApi.DriveFailure problem(String message) { return new AssignmentDriveApi.DriveFailure(message); }
    private static Exception friendly(Exception failure) {
        if (failure instanceof AssignmentDriveApi.DriveFailure) return failure;
        if (failure instanceof SocketTimeoutException) return problem("Google Drive phản hồi chậm. Kiểm tra kết nối rồi thử lại.");
        if (failure instanceof InterruptedIOException) return problem("Đã hủy thao tác với Google Drive.");
        if (failure instanceof SecurityException) return problem("Không còn quyền đọc tệp đã chọn. Hãy chọn lại tệp.");
        if (failure instanceof IOException) return problem("Chưa đọc được tệp hoặc kết nối Google Drive. Kiểm tra mạng và chọn lại tệp nếu cần.");
        return problem("Chưa hoàn tất thao tác với Google Drive. Hãy kết nối lại rồi thử lại.");
    }
}
