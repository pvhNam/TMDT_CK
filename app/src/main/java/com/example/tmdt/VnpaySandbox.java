package com.example.tmdt;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Native VNPay TEST integration, matching LTMobile. */
public final class VnpaySandbox {
    public static final String GATEWAY="https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    // Intercepted by our WebView before any request; no website or hosting is needed here.
    public static final String RETURN_URL=BuildConfig.VNPAY_RETURN_URL;
    private final String merchant,secret;
    public VnpaySandbox(String merchant,String secret){this.merchant=merchant;this.secret=secret;}
    public static boolean enabled(){return BuildConfig.DEBUG&&BuildConfig.VNPAY_CLIENT_SANDBOX;}
    public static VnpaySandbox configured(){return new VnpaySandbox(BuildConfig.VNPAY_TMN_CODE,BuildConfig.VNPAY_HASH_SECRET);}
    public boolean ready(){return !merchant.isEmpty()&&!secret.isEmpty();}
    public static String date(long millis){return Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));}
    public String checkout(String ref,long amount,long created,long expires){
        if(!ready()||!ref.matches("[A-Za-z0-9]{1,100}")||amount<1||amount>120000000)throw new IllegalArgumentException("Cấu hình VNPay TEST hoặc số tiền chưa hợp lệ.");
        Map<String,String> p=new TreeMap<>();
        p.put("vnp_Version","2.1.0");p.put("vnp_Command","pay");p.put("vnp_TmnCode",merchant);
        p.put("vnp_Amount",String.valueOf(Math.multiplyExact(amount,100)));p.put("vnp_CurrCode","VND");
        p.put("vnp_TxnRef",ref);p.put("vnp_OrderInfo","TMDT TEST "+ref);p.put("vnp_OrderType","other");
        p.put("vnp_Locale","vn");p.put("vnp_ReturnUrl",RETURN_URL);p.put("vnp_IpAddr","127.0.0.1");
        p.put("vnp_CreateDate",date(created));p.put("vnp_ExpireDate",date(expires));
        return GATEWAY+"?"+canonical(p)+"&vnp_SecureHash="+sign(p);
    }
    public static boolean callback(String value){
        try{URI u=URI.create(value),expected=URI.create(RETURN_URL);return expected.getScheme().equals(u.getScheme())&&expected.getHost().equals(u.getHost())
                &&expected.getPath().equals(u.getPath())&&u.getUserInfo()==null&&u.getPort()==-1&&u.getFragment()==null;}
        catch(Exception e){return false;}
    }
    public static Map<String,String> parameters(String url){
        try{Map<String,String> p=new TreeMap<>();String raw=URI.create(url).getRawQuery();if(raw==null||raw.length()>16384)throw new IllegalArgumentException();
            for(String entry:raw.split("&",-1)){String[] pair=entry.split("=",2);String k=URLDecoder.decode(pair[0],"UTF-8"),v=pair.length==2?URLDecoder.decode(pair[1],"UTF-8"):"";
                if(k.isEmpty()||p.put(k,v)!=null)throw new IllegalArgumentException();}return p;
        }catch(Exception e){throw new IllegalArgumentException("Phản hồi VNPay không hợp lệ.");}
    }
    public static final class Result {
        public final String status,transactionNo,code;
        Result(String status,String transactionNo,String code){this.status=status;this.transactionNo=transactionNo;this.code=code;}
    }
    public Result verify(String callbackUrl,String expectedRef,long expectedAmount){
        if(!callback(callbackUrl))throw new IllegalArgumentException("Địa chỉ trả về VNPay không hợp lệ.");
        Map<String,String> p=parameters(callbackUrl);String hash=p.remove("vnp_SecureHash");p.remove("vnp_SecureHashType");
        if(hash==null||!hash.matches("[0-9a-fA-F]{128}")||!MessageDigest.isEqual(sign(p).getBytes(StandardCharsets.US_ASCII),hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("Không xác thực được chữ ký VNPay.");
        if(!merchant.equals(p.get("vnp_TmnCode"))||!expectedRef.equals(p.get("vnp_TxnRef"))||!String.valueOf(expectedAmount*100).equals(p.get("vnp_Amount")))
            throw new IllegalArgumentException("Mã đơn hoặc số tiền VNPay không khớp.");
        String code=p.getOrDefault("vnp_ResponseCode",""),state=p.getOrDefault("vnp_TransactionStatus","");
        if(code.equals("00")&&state.equals("00")){
            String number=p.getOrDefault("vnp_TransactionNo","");if(!number.matches("[0-9]{1,40}")||number.equals("0"))throw new IllegalArgumentException("Thiếu mã giao dịch VNPay.");
            return new Result("PAID",number,code);
        }
        // Inconsistent/pending results cannot be treated as a definitive failure or charged again.
        if(code.equals("00")||code.equals("07")||state.equals("00")||Arrays.asList("01","04","05","06","07","09").contains(state))return new Result("GATEWAY_REVIEW","",code);
        if(code.isEmpty()||state.isEmpty())throw new IllegalArgumentException("VNPay chưa trả đủ kết quả giao dịch.");
        return new Result(code.equals("24")?"CANCELLED":"FAILED","",code);
    }
    public String sign(Map<String,String> values){
        try{Mac hmac=Mac.getInstance("HmacSHA512");hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return hex(hmac.doFinal(canonical(values).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException("Không tạo được chữ ký VNPay.",e);}
    }
    public static String canonical(Map<String,String> p){
        StringJoiner join=new StringJoiner("&");new TreeMap<>(p).forEach((k,v)->{if(v!=null&&!v.isEmpty())join.add(encode(k)+"="+encode(v));});return join.toString();
    }
    private static String encode(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte value:bytes)b.append(String.format(Locale.ROOT,"%02x",value&255));return b.toString();}
}
