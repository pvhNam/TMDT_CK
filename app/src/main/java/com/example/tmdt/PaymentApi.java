package com.example.tmdt;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Release uses the trusted backend. Debug can use the native VNPay classroom sandbox. */
public final class PaymentApi {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private final String base;
    private final SandboxPaymentApi sandbox;
    public PaymentApi(){base=BuildConfig.PAYMENT_API_BASE_URL;sandbox=VnpaySandbox.enabled()?new SandboxPaymentApi():null;}
    PaymentApi(String url){base=url==null?"":url.replaceAll("/+$","");sandbox=null;}
    PaymentApi(SandboxPaymentApi sandbox){base="";this.sandbox=sandbox;}
    public boolean sandbox(){return sandbox!=null;}
    public String collection(String name){return sandbox()?SandboxPaymentApi.collection(name):name;}
    public boolean configured(){
        if(sandbox())return VnpaySandbox.configured().ready();
        try{URI uri=URI.create(base);return uri.getHost()!=null&&uri.getUserInfo()==null&&uri.getQuery()==null&&uri.getFragment()==null
                &&("https".equals(uri.getScheme())||(BuildConfig.DEBUG&&"http".equals(uri.getScheme())));}
        catch(Exception e){return false;}
    }
    public Task<JSONObject> request(FirebaseUser user,String method,String path,JSONObject body){
        if(sandbox())return sandbox.request(user,method,path,body);
        if(!configured())return Tasks.forException(new Failure("Thanh toán đang được thiết lập. Vui lòng liên hệ quản trị viên."));
        if(user==null)return Tasks.forException(new Failure("Vui lòng đăng nhập lại."));
        return user.getIdToken(false).continueWithTask(token->{
            if(!token.isSuccessful())throw token.getException();
            return Tasks.call(IO,()->{
                HttpURLConnection connection=(HttpURLConnection)new URL(base+path).openConnection();
                connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(30000);
                connection.setRequestMethod(method);connection.setRequestProperty("Authorization","Bearer "+token.getResult().getToken());
                connection.setRequestProperty("Content-Type","application/json");connection.setDoOutput(true);
                try{
                    try(OutputStream output=connection.getOutputStream()){output.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                    int status=connection.getResponseCode();InputStream source=status>=200&&status<300?connection.getInputStream():connection.getErrorStream();
                    if(source==null)throw new Failure("Dịch vụ thanh toán chưa phản hồi. Vui lòng thử lại.");
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                    try(InputStream input=source){byte[] buffer=new byte[1024];int count;while((count=input.read(buffer))!=-1){if(bytes.size()+count>32768)throw new Failure("Phản hồi thanh toán không hợp lệ.");bytes.write(buffer,0,count);}}
                    JSONObject result=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                    if(status<200||status>=300||!result.optBoolean("ok"))throw new Failure(result.optString("message","Không hoàn tất được yêu cầu thanh toán."));
                    return result;
                }catch(Failure e){throw e;}catch(Exception e){throw new Failure("Không kết nối được dịch vụ thanh toán. Hãy kiểm tra mạng và thử lại.");}
                finally{connection.disconnect();}
            });
        });
    }
    public static final class Failure extends Exception { public Failure(String message){super(message);} }
}
