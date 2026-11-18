package com.example.tmdt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Small Drive v3 transport. Access tokens only live in the caller's current operation. */
public final class AssignmentDriveApi {
    private static final String GOOGLE_ORIGIN = "https://www.googleapis.com";
    private static final long MULTIPART_LIMIT = 5_000_000L;
    private static final String CONTEXT_PROPERTY = "tmdt_path_sha256";
    interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
    interface Control {
        void check() throws IOException;
        void attach(HttpURLConnection connection) throws IOException;
        void release(HttpURLConnection connection);
    }
    interface Progress { void transferred(long bytes, long total); }
    static final class Uploaded {
        final String fileId;
        Uploaded(String fileId) { this.fileId = fileId; }
    }
    private final String origin;
    private final ConnectionFactory connections;
    private final boolean localTest;
    AssignmentDriveApi() {
        origin = GOOGLE_ORIGIN; connections = url -> (HttpURLConnection) url.openConnection(); localTest = false;
    }
    /** Tests can supply a loopback server; this constructor is never selected by app configuration. */
    AssignmentDriveApi(String testOrigin, ConnectionFactory factory) {
        URI value = URI.create(testOrigin);
        if (!"http".equals(value.getScheme()) || !("127.0.0.1".equals(value.getHost()) || "localhost".equals(value.getHost()))
                || value.getRawUserInfo() != null || value.getRawQuery() != null || value.getRawFragment() != null
                || (value.getPath() != null && !value.getPath().isEmpty()) || factory == null) throw new IllegalArgumentException("Loopback test origin required");
        origin = testOrigin; connections = factory; localTest = true;
    }
    void verifyAccount(String token, String expectedEmail, Control control) throws IOException {
        requireToken(token);
        JSONObject about = json("GET", origin + "/drive/v3/about?fields=user(emailAddress)", null, token, control);
        String actual = about.optJSONObject("user") == null ? "" : about.optJSONObject("user").optString("emailAddress", "");
        if (expectedEmail == null || expectedEmail.trim().isEmpty() || !actual.equalsIgnoreCase(expectedEmail.trim()))
            throw new DriveFailure("Chọn tài khoản Google trùng email đang đăng nhập trong ứng dụng.");
    }
    Uploaded upload(File file, String logicalPath, String name, String mime, String token, String expectedEmail,
                    List<String> readers, Control control, Progress progress) throws IOException {
        if (!file.isFile() || file.length() <= 0 || file.length() > 10L * 1024 * 1024)
            throw new DriveFailure("Chọn tệp có nội dung, tối đa 10 MB.");
        verifyAccount(token, expectedEmail, control);
        normalizedReaders(readers, expectedEmail);
        String id = "";
        try {
            JSONObject metadata = object("name", name, "mimeType", mime,
                    "description", "Tệp bài tập được tải từ ứng dụng TMDT.",
                    "appProperties", object(CONTEXT_PROPERTY, pathHash(logicalPath)));
            JSONObject result = file.length() <= MULTIPART_LIMIT
                    ? uploadMultipart(file, metadata, mime, token, control, progress)
                    : uploadResumable(file, metadata, mime, token, control, progress);
            id = result.optString("id", ""); requireFileId(id);
            if (result.optLong("size", -1) != file.length() || !mime.equals(result.optString("mimeType", "")))
                throw new DriveFailure("Drive chưa xác nhận đầy đủ tệp. Hãy chọn lại và tải lên.");
            addReaders(id, token, expectedEmail, readers, control);
            return new Uploaded(id);
        } catch (IOException failure) {
            // Only this unpublished new file is eligible for cleanup; no existing personal files are changed.
            if (id.matches("[A-Za-z0-9_-]{10,200}") && !Thread.currentThread().isInterrupted()) {
                try { json("DELETE", origin + "/drive/v3/files/" + id, null, token, control); }
                catch (IOException ignored) { }
            }
            throw failure;
        }
    }
    private JSONObject uploadMultipart(File file, JSONObject metadata, String mime, String token,
                                       Control control, Progress progress) throws IOException {
        String boundary = "tmdt_" + UUID.randomUUID().toString().replace("-", "");
        byte[] prefix = ("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n--" + boundary + "\r\nContent-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection request = open("POST", origin + "/upload/drive/v3/files?uploadType=multipart&fields=id,name,mimeType,size", token, control);
        try {
            request.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
            request.setFixedLengthStreamingMode(prefix.length + file.length() + suffix.length);
            request.setDoOutput(true);
            try (OutputStream output = request.getOutputStream()) {
                output.write(prefix); streamFile(file, output, control, progress); output.write(suffix);
            }
            int status = request.getResponseCode(); String body = readBody(request, control);
            if (status < 200 || status >= 300) throw failure(status, body, "tải tệp lên Drive");
            return parse(body);
        } finally { close(request, control); }
    }
    private JSONObject uploadResumable(File file, JSONObject metadata, String mime, String token,
                                       Control control, Progress progress) throws IOException {
        for (int attempt = 0; ; attempt++) {
            control.check();
            HttpURLConnection initiate = open("POST", origin + "/upload/drive/v3/files?uploadType=resumable&fields=id,name,mimeType,size", token, control);
            String location;
            try {
                initiate.setRequestProperty("X-Upload-Content-Type", mime);
                initiate.setRequestProperty("X-Upload-Content-Length", Long.toString(file.length()));
                writeJson(initiate, metadata);
                int status = initiate.getResponseCode();
                if (status < 200 || status >= 300) throw failure(status, readBody(initiate, control), "tạo phiên tải lên");
                location = initiate.getHeaderField("Location");
                if (!safeUploadLocation(location)) throw new DriveFailure("Địa chỉ tải lên Drive không hợp lệ. Hãy thử lại.");
            } finally { close(initiate, control); }
            HttpURLConnection transfer = open("PUT", location, token, control);
            try {
                // Google's session URI authorizes the data PUT; OAuth is used for initiation and Drive API operations.
                transfer.setRequestProperty("Content-Type", mime);
                transfer.setRequestProperty("Content-Range", "bytes 0-" + (file.length() - 1) + "/" + file.length());
                transfer.setFixedLengthStreamingMode(file.length()); transfer.setDoOutput(true);
                try (OutputStream output = transfer.getOutputStream()) { streamFile(file, output, control, progress); }
                int status = transfer.getResponseCode(); String body = readBody(transfer, control);
                if (status >= 200 && status < 300) return parse(body);
                DriveFailure problem = failure(status, body, "tải nội dung tệp");
                boolean expiredSession = problem.reason.isEmpty() && (status == 403 || status == 404 || status == 410);
                if (attempt == 0 && expiredSession) { control.check(); progress.transferred(0, file.length()); continue; }
                throw problem;
            } finally { close(transfer, control); }
        }
    }
    private static void streamFile(File file, OutputStream output, Control control, Progress progress) throws IOException {
        long sent = 0;
        try (InputStream input = new FileInputStream(file)) {
            byte[] bytes = new byte[32768]; int count;
            while ((count = input.read(bytes)) != -1) {
                control.check(); output.write(bytes, 0, count); sent += count; progress.transferred(sent, file.length());
            }
        }
    }
    void syncReaders(String token, String expectedEmail, List<GroupAssignment.Attachment> files,
                     List<String> readers, Control control) throws IOException {
        verifyAccount(token, expectedEmail, control);
        for (GroupAssignment.Attachment file : files) {
            control.check(); requireFileId(file.driveFileId);
            JSONObject current = json("GET", origin + "/drive/v3/files/" + file.driveFileId
                    + "?fields=id,trashed,appProperties,owners(emailAddress)", null, token, control);
            JSONObject properties = current.optJSONObject("appProperties"); boolean owned = false;
            JSONArray owners = current.optJSONArray("owners");
            if (owners != null) for (int i = 0; i < owners.length(); i++) {
                JSONObject owner = owners.optJSONObject(i);
                if (owner != null && expectedEmail.equalsIgnoreCase(owner.optString("emailAddress", ""))) owned = true;
            }
            if (!file.driveFileId.equals(current.optString("id", "")) || current.optBoolean("trashed", false) || !matchesContext(properties, file.path) || !owned)
                throw new DriveFailure("Tài liệu không thuộc Drive của bạn hoặc không còn khớp với bài tập này.");
            addReaders(file.driveFileId, token, expectedEmail, readers, control);
        }
    }
    private static boolean matchesContext(JSONObject properties, String logicalPath) throws IOException {
        if (properties == null) return false;
        if (properties.has(CONTEXT_PROPERTY))
            return pathHash(logicalPath).equals(properties.optString(CONTEXT_PROPERTY, ""));
        // Previously uploaded files used the full path. Only accept an exact match for that legacy format.
        return logicalPath.equals(properties.optString("tmdt_path", ""));
    }
    private static String pathHash(String logicalPath) throws IOException {
        // Drive allows only 124 UTF-8 bytes for a property key + value. Full Firebase paths exceed that limit.
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(logicalPath.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[digest.length * 2];
            final String digits = "0123456789abcdef";
            for (int i = 0; i < digest.length; i++) {
                int value = digest[i] & 0xff;
                hex[2 * i] = digits.charAt(value >>> 4); hex[2 * i + 1] = digits.charAt(value & 15);
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new DriveFailure("Chưa chuẩn bị được thông tin tệp tải lên.");
        }
    }
    private void addReaders(String id, String token, String expectedEmail, List<String> readers, Control control) throws IOException {
        Set<String> desired = normalizedReaders(readers, expectedEmail);
        if (desired.isEmpty()) return;
        Set<String> existing = new LinkedHashSet<>(); String page = ""; int pages = 0;
        do {
            if (++pages > 100) throw new DriveFailure("Danh sách quyền truy cập Drive quá lớn. Hãy kiểm tra lại tài liệu.");
            JSONObject result = json("GET", origin + "/drive/v3/files/" + id
                    + "/permissions?pageSize=100&fields=nextPageToken,permissions(type,role,emailAddress)"
                    + (page.isEmpty() ? "" : "&pageToken=" + encode(page)), null, token, control);
            JSONArray permissions = result.optJSONArray("permissions");
            if (permissions != null) for (int i = 0; i < permissions.length(); i++) {
                JSONObject permission = permissions.optJSONObject(i);
                if (permission != null && "user".equals(permission.optString("type", ""))
                        && !permission.optString("role", "").isEmpty()) existing.add(permission.optString("emailAddress", "").toLowerCase(Locale.ROOT));
            }
            page = result.optString("nextPageToken", "");
        } while (!page.isEmpty());
        for (String email : desired) if (!existing.contains(email)) {
            control.check();
            json("POST", origin + "/drive/v3/files/" + id + "/permissions?sendNotificationEmail=false&fields=id",
                    object("type", "user", "role", "reader", "emailAddress", email), token, control);
        }
    }
    private Set<String> normalizedReaders(List<String> readers, String ownEmail) throws IOException {
        Set<String> result = new LinkedHashSet<>();
        if (readers != null) for (String email : readers) {
            String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
            if (value.length() > 254 || !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new DriveFailure("Email người nhận tài liệu không hợp lệ. Hãy kiểm tra tài khoản lớp học.");
            if (!value.equalsIgnoreCase(ownEmail)) result.add(value);
        }
        return result;
    }
    private JSONObject json(String method, String url, JSONObject body, String token, Control control) throws IOException {
        HttpURLConnection connection = open(method, url, token, control);
        try {
            if (body != null) writeJson(connection, body);
            int status = connection.getResponseCode(); String response = readBody(connection, control);
            if (status < 200 || status >= 300) throw failure(status, response, operation(method, url));
            return response.trim().isEmpty() ? new JSONObject() : parse(response);
        } finally { close(connection, control); }
    }
    private HttpURLConnection open(String method, String address, String token, Control control) throws IOException {
        control.check(); requireToken(token);
        URL url = new URL(address);
        if (!isApiOrigin(url) && !("PUT".equals(method) && safeUploadLocation(address)))
            throw new DriveFailure("Địa chỉ Google Drive không hợp lệ.");
        HttpURLConnection connection = connections.open(url);
        try {
            control.attach(connection); connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(45000);
            connection.setRequestMethod(method); connection.setUseCaches(false);
            if (!("PUT".equals(method) && safeUploadLocation(address)))
                connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            return connection;
        } catch (IOException | RuntimeException failure) { close(connection, control); throw failure; }
    }
    private boolean isApiOrigin(URL url) {
        try {
            URI base = URI.create(origin);
            return base.getScheme().equals(url.getProtocol()) && base.getHost().equalsIgnoreCase(url.getHost())
                    && port(base) == port(url.toURI()) && url.getUserInfo() == null && url.getRef() == null;
        } catch (Exception ignored) { return false; }
    }
    private boolean safeUploadLocation(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value); String host = uri.getHost();
            if (host == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                    || !uri.getPath().startsWith("/upload/drive/v3/files")) return false;
            if (localTest) return isApiOrigin(uri.toURL());
            return "https".equals(uri.getScheme()) && port(uri) == 443
                    && ("googleapis.com".equalsIgnoreCase(host) || host.toLowerCase(Locale.ROOT).endsWith(".googleapis.com"));
        } catch (Exception ignored) { return false; }
    }
    private static int port(URI value) { return value.getPort() != -1 ? value.getPort() : "https".equals(value.getScheme()) ? 443 : 80; }
    private static void writeJson(HttpURLConnection connection, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bytes.length); connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
    }
    private static String readBody(HttpURLConnection connection, Control control) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) return "";
        try (InputStream input = stream; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[8192]; int count;
            while ((count = input.read(bytes)) != -1) {
                control.check(); if (result.size() + count > 262144) throw new DriveFailure("Phản hồi Google Drive quá lớn. Hãy thử lại.");
                result.write(bytes, 0, count);
            }
            return new String(result.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private static void close(HttpURLConnection connection, Control control) {
        control.release(connection); connection.disconnect();
    }
    private static JSONObject parse(String value) throws IOException {
        try { return new JSONObject(value); }
        catch (JSONException invalidJson) { throw new DriveFailure("Google Drive trả về dữ liệu chưa hợp lệ. Hãy thử lại."); }
    }
    private static JSONObject object(Object... pairs) throws IOException {
        JSONObject result = new JSONObject();
        try { for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]); return result; }
        catch (JSONException invalidJson) { throw new DriveFailure("Chưa chuẩn bị được thông tin tệp tải lên."); }
    }
    private static String encode(String value) throws IOException { return URLEncoder.encode(value, StandardCharsets.UTF_8.name()); }
    private static void requireToken(String value) throws IOException {
        if (value == null || value.isEmpty() || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new DriveFailure("Hãy kết nối Google Drive trước khi tải tệp lên.");
    }
    private static void requireFileId(String value) throws IOException {
        if (value == null || !value.matches("[A-Za-z0-9_-]{10,200}")) throw new DriveFailure("Mã tệp Google Drive không hợp lệ.");
    }
    private static String operation(String method, String address) {
        String path = URI.create(address).getPath();
        if (path.endsWith("/about")) return "kiểm tra tài khoản Drive";
        if (path.endsWith("/permissions")) return "POST".equals(method) ? "chia sẻ quyền xem" : "đọc quyền xem";
        return "DELETE".equals(method) ? "dọn tệp tạm" : "kiểm tra tài liệu";
    }
    private static DriveFailure failure(int status, String response, String operation) {
        String reason = providerReason(response), advice;
        if (status == 401) advice = "Quyền Google Drive đã hết hạn. Hãy kết nối lại rồi thử lại.";
        else if (status == 429) advice = "Google Drive đang giới hạn lượt truy cập. Hãy thử lại sau ít phút.";
        else if (status < 400 || status >= 500) advice = "Chưa hoàn tất thao tác trên Google Drive. Kiểm tra mạng rồi thử lại.";
        else if ("ACCESS_TOKEN_SCOPE_INSUFFICIENT".equals(reason) || "INSUFFICIENT_PERMISSIONS".equals(reason))
            advice = "Chưa cấp đủ quyền Google Drive. Thử lại và cho phép ứng dụng quản lý các tệp do ứng dụng tạo.";
        else if ("STORAGE_QUOTA_EXCEEDED".equals(reason))
            advice = "Google Drive của bạn đã đầy. Hãy dọn bộ nhớ rồi tải lại.";
        else if ("SHARING_RATE_LIMIT_EXCEEDED".equals(reason))
            advice = "Google đang giới hạn chia sẻ tệp của tài khoản. Hãy thử lại sau ít phút.";
        else if (status == 429 || reason.equals("RATE_LIMIT_EXCEEDED") || reason.equals("USER_RATE_LIMIT_EXCEEDED") || reason.equals("DAILY_LIMIT_EXCEEDED"))
            advice = "Google Drive đang giới hạn lượt truy cập. Hãy thử lại sau ít phút.";
        else if (reason.equals("SERVICE_DISABLED") || reason.equals("API_KEY_SERVICE_BLOCKED"))
            advice = "Drive API chưa bật hoặc bị chặn trong dự án Google của ứng dụng. Hãy liên hệ quản trị viên kiểm tra đúng dự án OAuth.";
        else if (reason.equals("DOMAIN_POLICY") || reason.equals("ORG_POLICY_VIOLATION") || reason.equals("CANNOT_SHARE_ACROSS_DOMAINS") || reason.equals("SHARING_DISABLED"))
            advice = "Chính sách tài khoản trường/công ty đang chặn Drive hoặc chia sẻ tệp. Cần quản trị viên tổ chức cho phép thao tác này.";
        else if (reason.equals("APP_NOT_AUTHORIZED_TO_FILE"))
            advice = "Ứng dụng chưa được cấp quyền với tệp này. Hãy mở lại tệp qua ứng dụng rồi thử lại.";
        else if (reason.equals("INSUFFICIENT_FILE_PERMISSIONS"))
            advice = "Không có quyền sửa hoặc chia sẻ tệp này. Hãy kiểm tra tài khoản sở hữu tệp.";
        else if (reason.equals("INVALID_SHARING_REQUEST"))
            advice = "Drive chưa cho phép chia sẻ với email người nhận. Kiểm tra tài khoản Google và chính sách chia sẻ của lớp.";
        else if (status == 403) advice = "Google từ chối thao tác với Drive. Hãy kiểm tra tài khoản và quyền ứng dụng.";
        else if (status == 404) advice = "Không tìm thấy tệp trên Drive hoặc bạn chưa có quyền truy cập.";
        else if (status == 400) advice = "Google Drive chưa chấp nhận tệp hoặc email người nhận. Hãy kiểm tra lại.";
        else advice = "Chưa hoàn tất thao tác trên Google Drive. Kiểm tra mạng rồi thử lại.";
        String code = reason.isEmpty() ? "HTTP_" + status : reason;
        return new DriveFailure(advice + " Bước: " + operation + ". Mã: " + status + "/" + code + ".", status, reason, operation);
    }
    private static String providerReason(String response) {
        // Only allowlisted provider codes leave this method. Never surface tokens, email, URLs or raw provider messages.
        try {
            JSONObject error = new JSONObject(response == null ? "{}" : response).optJSONObject("error");
            if (error != null) {
                JSONArray details = error.optJSONArray("details");
                if (details != null) for (int i = 0; i < details.length(); i++) {
                    JSONObject detail = details.optJSONObject(i);
                    String code = detail == null ? "" : knownReason(detail.optString("reason", ""));
                    if (!code.isEmpty()) return code;
                }
                JSONArray errors = error.optJSONArray("errors");
                if (errors != null) for (int i = 0; i < errors.length(); i++) {
                    JSONObject detail = errors.optJSONObject(i);
                    String code = detail == null ? "" : knownReason(detail.optString("reason", ""));
                    if (!code.isEmpty()) return code;
                }
                String code = knownReason(error.optString("status", ""));
                if (!code.isEmpty()) return code;
                if (error.optString("message", "").toLowerCase(Locale.ROOT).contains("has not been used")) return "SERVICE_DISABLED";
            }
        } catch (JSONException ignored) { }
        return "";
    }
    private static String knownReason(String value) {
        switch (value.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT)) {
            case "insufficientpermissions": return "INSUFFICIENT_PERMISSIONS";
            case "accesstokenscopeinsufficient": return "ACCESS_TOKEN_SCOPE_INSUFFICIENT";
            case "accessnotconfigured": case "servicedisabled": return "SERVICE_DISABLED";
            case "apikeyserviceblocked": return "API_KEY_SERVICE_BLOCKED";
            case "storagequotaexceeded": return "STORAGE_QUOTA_EXCEEDED";
            case "sharingratelimitexceeded": return "SHARING_RATE_LIMIT_EXCEEDED";
            case "ratelimitexceeded": return "RATE_LIMIT_EXCEEDED";
            case "userratelimitexceeded": return "USER_RATE_LIMIT_EXCEEDED";
            case "dailylimitexceeded": return "DAILY_LIMIT_EXCEEDED";
            case "domainpolicy": return "DOMAIN_POLICY";
            case "orgpolicyviolation": case "orgrestrictionviolation": return "ORG_POLICY_VIOLATION";
            case "cannotshareacrossdomains": case "sharingoutsidedomainnotallowed": return "CANNOT_SHARE_ACROSS_DOMAINS";
            case "sharingdisabled": return "SHARING_DISABLED";
            case "appnotauthorizedtofile": return "APP_NOT_AUTHORIZED_TO_FILE";
            case "insufficientfilepermissions": return "INSUFFICIENT_FILE_PERMISSIONS";
            case "invalidsharingrequest": return "INVALID_SHARING_REQUEST";
            default: return "";
        }
    }
    public static final class DriveFailure extends IOException {
        public final int httpStatus;
        public final String reason, operation;
        public DriveFailure(String message) { this(message, 0, "", ""); }
        private DriveFailure(String message, int httpStatus, String reason, String operation) {
            super(message); this.httpStatus = httpStatus; this.reason = reason; this.operation = operation;
        }
        public boolean requiresReauthorization() {
            return httpStatus == 401 || (httpStatus == 403 && (reason.equals("ACCESS_TOKEN_SCOPE_INSUFFICIENT") || reason.equals("INSUFFICIENT_PERMISSIONS")));
        }
    }
}
