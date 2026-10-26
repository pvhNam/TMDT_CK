package com.example.tmdt.ui.payments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import com.example.tmdt.VnpaySandbox;
import com.example.tmdt.ui.common.Ui;

/** Non-exported WebView; callback is intercepted locally, like LTMobile. */
public final class VnpayPaymentActivity extends AppCompatActivity {
    public static final String URL="checkout_url",PAYMENT="payment_id",CALLBACK="callback_url",ERROR="payment_error";
    private WebView web;
    private boolean finished;
    private String payment;
    @SuppressLint("SetJavaScriptEnabled") @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);setResult(RESULT_CANCELED);payment=getIntent().getStringExtra(PAYMENT);
        String url=getIntent().getStringExtra(URL);
        try{Uri uri=Uri.parse(url);if(!VnpaySandbox.enabled()||payment==null||!payment.matches("[A-Za-z0-9_-]{1,180}")||!"https".equals(uri.getScheme())||!"sandbox.vnpayment.vn".equals(uri.getHost())||!"/paymentv2/vpcpay.html".equals(uri.getPath())||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getFragment()!=null)throw new IllegalArgumentException();}
        catch(Exception e){fail("Phiên thanh toán TEST không hợp lệ.");return;}
        Ui ui=new Ui(this);LinearLayout root=ui.column();root.setBackgroundColor(Color.WHITE);ui.header(root,"VNPay · Thanh toán TEST",this::askClose,null);
        TextView hint=ui.text("Môi trường thử nghiệm · Không dùng thẻ hoặc tiền thật",13,Ui.BLUE,false);ui.pad(hint,16,10);ui.add(root,hint);
        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(3)));
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportMultipleWindows(false);
        web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView view,int value){progress.setProgress(value,true);progress.setVisibility(value==100?View.GONE:View.VISIBLE);}});
        web.setWebViewClient(new WebViewClient(){
            private boolean handle(String value){
                if(finished)return true;
                if(VnpaySandbox.callback(value)){finished=true;setResult(RESULT_OK,new Intent().putExtra(PAYMENT,payment).putExtra(CALLBACK,value));finish();return true;}
                Uri uri=Uri.parse(value);if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null)return true;
                if("sandbox.vnpayment.vn".equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().endsWith("/Payment/Error.html")){
                    String code=uri.getQueryParameter("code");if("71".equals(code)){fail("VNPay TEST chưa duyệt mã website của bạn (lỗi 71). Cần kích hoạt merchant với VNPay rồi thử lại.");return true;}
                }return false;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return request.isForMainFrame()&&handle(request.getUrl().toString());}
            @Override public void onPageStarted(WebView view,String value,android.graphics.Bitmap icon){if(handle(value))view.stopLoading();}
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError error){handler.cancel();fail("Không xác thực được kết nối an toàn đến VNPay.");}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame())fail("Không tải được VNPay. Kiểm tra mạng rồi mở lại phiên thanh toán.");}
        });
        if(saved==null||web.restoreState(saved)==null)web.loadUrl(url);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){askClose();}});
    }
    private void askClose(){new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Đóng trang thanh toán?").setMessage("Nếu chưa nhận kết quả, đơn vẫn chờ thanh toán. Bạn có thể mở lại cùng phiên.").setNegativeButton("Tiếp tục",null).setPositiveButton("Đóng",(d,w)->finish()).show();}
    private void fail(String message){if(finished)return;finished=true;setResult(RESULT_CANCELED,new Intent().putExtra(ERROR,message));finish();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(web!=null)web.saveState(out);}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();web=null;}super.onDestroy();}
}
