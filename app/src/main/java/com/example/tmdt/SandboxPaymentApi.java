package com.example.tmdt;

import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import org.json.JSONObject;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.function.LongSupplier;

/** Classroom-only ledger. No write from this class targets the real financial collections. */
final class SandboxPaymentApi {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2);
    private static final ZoneId VN=ZoneId.of("Asia/Ho_Chi_Minh");
    private final FirebaseFirestore db;
    private final VnpaySandbox gateway;
    private final LongSupplier clock;
    SandboxPaymentApi(){this(FirebaseFirestore.getInstance(),VnpaySandbox.configured(),System::currentTimeMillis);}
    SandboxPaymentApi(FirebaseFirestore db,VnpaySandbox gateway,LongSupplier clock){this.db=db;this.gateway=gateway;this.clock=clock;}
    static String collection(String name){return "sandbox_"+name;}
    private DocumentReference ref(String name,String id){if(!id.matches("[A-Za-z0-9_-]{1,180}"))throw bad("Mã dữ liệu chưa hợp lệ.");return db.collection(collection(name)).document(id);}
    private static FieldValue stamp(){return FieldValue.serverTimestamp();}
    private static RuntimeException bad(String message){return new IllegalArgumentException(message);}
    private static void check(boolean ok,String message){if(!ok)throw bad(message);}
    private static String text(Map<String,Object> d,String key){return d.get(key) instanceof String?(String)d.get(key):"";}
    private static long num(Map<String,Object> d,String key){return d.get(key) instanceof Number?((Number)d.get(key)).longValue():0;}
    private static long millis(Object o){return o instanceof Timestamp?((Timestamp)o).toDate().getTime():0;}
    private static Map<String,Object> map(Object... pairs){Map<String,Object> m=new HashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    private static Map<String,Object> data(DocumentSnapshot doc){return doc.exists()?doc.getData():null;}
    private static Map<String,Object> must(DocumentSnapshot doc){check(doc.exists(),"Không tìm thấy dữ liệu thanh toán.");return doc.getData();}
    private static boolean paid(String status){return Arrays.asList("PAID","PAID_REVIEW","CLOSED_REFUNDED").contains(status);}
    private void member(String uid,boolean admin,Map<String,Object> p){check(admin||uid.equals(p.get("student_id"))||uid.equals(p.get("tutor_id")),"Bạn không có quyền xem giao dịch này.");}
    private static void admin(boolean value){check(value,"Chỉ quản trị viên được thực hiện thao tác này.");}
    private static String reason(JSONObject body,int min,int max,String key){String value=body.optString(key).trim();check(value.length()>=min&&value.length()<=max,"Nhập "+key+" từ "+min+"–"+max+" ký tự.");return value;}
    Task<JSONObject> request(FirebaseUser user,String method,String path,JSONObject body){
        if(user==null)return Tasks.forException(new PaymentApi.Failure("Vui lòng đăng nhập lại."));
        return Tasks.call(IO,()->{
            try{
                Map<String,Object> account=must(Tasks.await(db.collection("users").document(user.getUid()).get(Source.SERVER)));
                check("ACTIVE".equals(account.get("trang_thai")),"Tài khoản chưa được kích hoạt.");
                boolean admin="ADMIN".equals(account.get("vai_tro"));String uid=user.getUid();
                if(method.equals("POST")&&path.equals("/payments"))return checkout(uid,body.optString("type"),body.optString("source_id"));
                if(method.equals("PUT")&&path.equals("/bank")){saveBank(uid,body);return PaymentState.json("ok",true);}
                if(method.equals("POST")&&path.equals("/admin/payroll")){admin(admin);return payroll(uid,body.optString("period"));}
                String[] parts=path.split("/");
                if(method.equals("POST")&&parts.length>=4&&parts[1].equals("payments")){
                    String id=parts[2],action=parts[3];
                    if(parts.length==4&&action.equals("return"))return returned(uid,admin,id,body.optString("url"));
                    if(parts.length==4&&action.equals("refresh")){Map<String,Object> p=must(Tasks.await(ref("payments",id).get(Source.SERVER)));member(uid,admin,p);
                        return PaymentState.json("ok",true,"message","Đã tải trạng thái đã lưu. Nếu đóng VNPay trước khi trả kết quả, hãy mở lại phiên thanh toán.");}
                    if(parts.length==6&&action.equals("sessions")){session(uid,admin,id,Integer.parseInt(parts[4]),parts[5],body);return PaymentState.json("ok",true);}
                    if(parts.length==4&&action.equals("refund")){refund(uid,admin,id,body);return PaymentState.json("ok",true);}
                }
                if(method.equals("POST")&&parts.length==5&&parts[1].equals("admin")){
                    admin(admin);
                    if(parts[2].equals("payouts")){payoutAction(uid,parts[3],parts[4],body);return PaymentState.json("ok",true);}
                    if(parts[2].equals("refunds")){refundAction(uid,parts[3],parts[4],body);return PaymentState.json("ok",true);}
                }
                throw bad("Thao tác thanh toán thử chưa hợp lệ.");
            }catch(Exception e){Throwable cause=e;while(cause.getCause()!=null&&!(cause instanceof IllegalArgumentException)&&!(cause instanceof FirebaseFirestoreException))cause=cause.getCause();
                String message=cause instanceof IllegalArgumentException?cause.getMessage():cause instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)cause).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED?
                        "Firebase từ chối thanh toán thử. Hãy Publish FIRESTORE_RULES_FULL.txt có các collection sandbox_ trong project.":"Không tải được thanh toán thử. Kiểm tra kết nối rồi thử lại.";
                throw new PaymentApi.Failure(message);
            }
        });
    }
    private static final class SourceInfo {
        final Map<String,Object> values;final long end;final Map<String,Object> group;
        SourceInfo(Map<String,Object> values,long end,Map<String,Object> group){this.values=values;this.end=end;this.group=group;}
    }
    private SourceInfo source(Transaction tx,String type,String id)throws FirebaseFirestoreException{
        check(id.matches("[A-Za-z0-9_-]{1,180}"),"Mã đăng ký chưa hợp lệ.");
        String name=type.equals("LESSON")?"lessons":type.equals("CONTRACT")?"contracts":type.equals("GROUP")?"class_registrations":"";
        check(!name.isEmpty(),"Loại thanh toán chưa hợp lệ.");Map<String,Object> d=must(tx.get(db.collection(name).document(id)));
        String title,studentName,tutorName;long amount,count,end;Map<String,Object> group=null;
        if(type.equals("LESSON")){
            check("CONFIRMED".equals(d.get("trang_thai"))&&!Boolean.TRUE.equals(d.get("da_hoan_thanh")),"Buổi học chưa được chấp nhận hoặc đã hoàn thành.");
            title=text(d,"tieu_de");studentName=text(d,"ten_hoc_vien");tutorName=text(d,"ten_gia_su");amount=num(d,"hoc_phi");count=1;end=millis(d.get("bat_dau"))+num(d,"thoi_luong")*60000;
        }else if(type.equals("CONTRACT")){
            check(Arrays.asList("CHO_KY_QUY","HIEU_LUC").contains(text(d,"status"))&&Boolean.TRUE.equals(d.get("student_confirmed"))&&Boolean.TRUE.equals(d.get("tutor_confirmed")),"Khóa học chưa được hai bên chấp nhận.");
            title=text(d,"subject");studentName=text(d,"student_name");tutorName=text(d,"tutor_name");count=num(d,"sessions");amount=Math.multiplyExact(num(d,"fee"),count);end=Math.max(millis(d.get("start_at")),millis(d.get("confirmed_at")));
        }else{
            group=must(tx.get(db.collection("group_classes").document(text(d,"class_id"))));
            check("CONFIRMED".equals(d.get("trang_thai"))&&d.get("tutor_id").equals(group.get("tutor_id"))&&group.get("thanh_vien") instanceof List&&((List<?>)group.get("thanh_vien")).contains(d.get("student_id")),"Đăng ký lớp nhóm chưa được chấp nhận.");
            title=text(group,"ten_lop");studentName=text(d,"ten_hoc_vien");tutorName=text(group,"ten_gia_su");count=num(group,"so_buoi");amount=Math.multiplyExact(num(group,"hoc_phi"),Boolean.TRUE.equals(group.get("tinh_theo_buoi"))?count:1);end=0;
        }
        check(amount>0&&amount<=120000000&&count>=1&&count<=60&&!text(d,"student_id").isEmpty()&&!text(d,"tutor_id").isEmpty()&&!d.get("student_id").equals(d.get("tutor_id")),"Học phí hoặc thông tin đăng ký chưa hợp lệ.");
        return new SourceInfo(map("type",type,"source_id",id,"student_id",d.get("student_id"),"tutor_id",d.get("tutor_id"),"student_name",studentName,"tutor_name",tutorName,"title",title,"amount",amount,"session_count",count,"currency","VND"),end,group);
    }
    private JSONObject checkout(String uid,String type,String sourceId)throws Exception{
        check(gateway.ready(),"Chưa có cấu hình VNPay TEST trong payment-server/.env. Build lại sau khi điền cấu hình.");
        String id=PaymentState.id(type,sourceId);DocumentReference ref=ref("payments",id);long now=clock.getAsLong();
        Map<String,Object> payment=Tasks.await(db.runTransaction(tx->{
            Map<String,Object> old=data(tx.get(ref));
            if(old!=null){check(uid.equals(old.get("student_id")),"Chỉ học sinh thanh toán học phí.");
                if(paid(text(old,"status"))||Arrays.asList("REFUND_REQUESTED","GATEWAY_REVIEW").contains(text(old,"status")))return old;
                // Preserve the same TxnRef across retries. Never create another charge for an uncertain order.
                if(text(old,"status").equals("WAITING")){check(millis(old.get("expires_at"))>now,"Phiên đã hết hạn và chưa có kết quả trả về. Liên hệ admin để đối soát, không thanh toán lại đơn này.");return old;}
            }
            SourceInfo source=source(tx,type,sourceId);check(uid.equals(source.values.get("student_id")),"Chỉ học sinh thanh toán học phí.");
            String code=String.valueOf(100000000000000L+new java.security.SecureRandom().nextInt(900000000)*1000L+new java.security.SecureRandom().nextInt(1000));
            long expires=now+15*60000;Map<String,Object> p=new HashMap<>(source.values);
            p.putAll(map("is_sandbox",true,"provider","VNPAY","order_code",Long.parseLong(code),"status","WAITING","checkout_url",gateway.checkout(code,num(p,"amount"),now,expires),"expires_at",new Timestamp(new Date(expires)),"created_at",old==null?stamp():old.get("created_at"),"updated_at",stamp(),"received_amount",0L,"earned_amount",0L,"refund_total",0L));
            tx.set(ref,p);return p;
        }));
        return PaymentState.json("ok",true,"id",id,"checkoutUrl",text(payment,"status").equals("WAITING")?text(payment,"checkout_url"):"","status",text(payment,"status"));
    }
    private JSONObject returned(String uid,boolean admin,String id,String url)throws Exception{
        String status=Tasks.await(db.runTransaction(tx->{
            DocumentReference ref=ref("payments",id);Map<String,Object> p=must(tx.get(ref));member(uid,admin,p);
            check(uid.equals(p.get("student_id")),"Chỉ học sinh nhận kết quả thanh toán.");
            VnpaySandbox.Result result=gateway.verify(url,String.valueOf(num(p,"order_code")),num(p,"amount"));
            if(paid(text(p,"status"))||text(p,"status").equals("REFUND_REQUESTED"))return text(p,"status");
            check(Arrays.asList("WAITING","FAILED","CANCELLED","GATEWAY_REVIEW").contains(text(p,"status")),"Phiên thanh toán không còn hợp lệ.");
            // Source eligibility is checked again before a paid course becomes usable.
            if(result.status.equals("PAID")){
                SourceInfo s=source(tx,text(p,"type"),text(p,"source_id"));check(s.values.get("student_id").equals(p.get("student_id"))&&s.values.get("tutor_id").equals(p.get("tutor_id"))&&s.values.get("amount").equals(p.get("amount")),"Đăng ký đã thay đổi. Cần admin đối soát kết quả TEST.");
                tx.update(ref,map("status","PAID","received_amount",num(p,"amount"),"paid_at",stamp(),"provider_transaction_no",result.transactionNo,"return_url",url,"updated_at",stamp()));
            }else tx.update(ref,map("status",result.status,"response_code",result.code,"return_url",url,"updated_at",stamp()));
            return result.status;
        }));return PaymentState.json("ok",true,"status",status);
    }
    private long share(Map<String,Object> p,int number){long count=num(p,"session_count"),amount=num(p,"amount");check(number>=1&&number<=count&&count>0,"Buổi học chưa hợp lệ.");return amount/count+(number<=amount%count?1:0);}
    private long end(SourceInfo s,int number){
        if(s.group==null)return s.end;Map<String,Object> g=s.group;LocalDate day=LocalDate.parse(text(g,"ngay_bat_dau"));int seen=0;
        for(int i=0;i<500;i++,day=day.plusDays(1)){boolean matches=false;if(g.get("thu") instanceof List)for(Object v:(List<?>)g.get("thu"))if(v instanceof Number&&((Number)v).intValue()==dayOfWeek(day))matches=true;
            if(matches&&++seen==number)return day.atStartOfDay(VN).toInstant().toEpochMilli()+(num(g,"gio")*60+num(g,"thoi_luong"))*60000;}
        throw bad("Lịch lớp học chưa hợp lệ.");
    }
    private static int dayOfWeek(LocalDate day){return day.getDayOfWeek().getValue();}
    private void session(String uid,boolean admin,String id,int number,String action,JSONObject body)throws Exception{
        Tasks.await(db.runTransaction(tx->{DocumentReference ref=ref("payments",id),sr=ref.collection("sessions").document(String.valueOf(number));
            Map<String,Object> p=must(tx.get(ref));member(uid,admin,p);Map<String,Object> old=data(tx.get(sr));long value=share(p,number);
            check(text(p,"status").equals("PAID"),"Học phí chưa được xác nhận hoặc đang chờ hoàn tiền.");
            if(action.equals("reopen")){admin(admin);check(old!=null&&text(old,"status").equals("DISPUTED"),"Buổi học không có phản ánh.");tx.update(sr,map("status","PROPOSED","admin_note",reason(body,5,500,"reason"),"reviewer_id",uid,"updated_at",stamp()));return null;}
            if(action.equals("dispute")){check(uid.equals(p.get("student_id"))&&old!=null&&text(old,"status").equals("PROPOSED"),"Buổi học không còn chờ học sinh xác nhận.");tx.update(sr,map("status","DISPUTED","reason",reason(body,5,500,"reason"),"updated_at",stamp()));return null;}
            check(action.equals("propose")||action.equals("confirm"),"Thao tác buổi học chưa hợp lệ.");
            SourceInfo s=source(tx,text(p,"type"),text(p,"source_id"));check(clock.getAsLong()>=end(s,number),"Chưa đến thời điểm xác nhận hoàn thành buổi học.");
            if(action.equals("propose")){
                check(uid.equals(p.get("tutor_id")),"Chỉ gia sư báo hoàn thành buổi học.");if(old!=null)return null;
                tx.set(sr,map("is_sandbox",true,"number",number,"status","PROPOSED","amount",value,"reported_at",stamp(),"created_at",stamp(),"updated_at",stamp()));
            }else{
                check(uid.equals(p.get("student_id")),"Chỉ học sinh xác nhận đã học.");if(old!=null&&text(old,"status").equals("COMPLETED"))return null;
                check(old!=null&&text(old,"status").equals("PROPOSED"),"Gia sư cần báo hoàn thành trước.");
                DocumentReference er=ref("tutor_earnings",id+"_"+number);check(!tx.get(er).exists(),"Buổi học đã được tính tiền.");
                tx.set(er,map("is_sandbox",true,"payment_id",id,"session_number",number,"student_id",p.get("student_id"),"tutor_id",p.get("tutor_id"),"tutor_name",p.get("tutor_name"),"title",p.get("title"),"amount",value,"period",Instant.ofEpochMilli(clock.getAsLong()).atZone(VN).toLocalDate().toString().substring(0,7),"payout_id",null,"created_at",stamp()));
                tx.update(sr,map("status","COMPLETED","confirmed_at",stamp(),"updated_at",stamp()));tx.update(ref,map("earned_amount",num(p,"earned_amount")+value,"updated_at",stamp()));
            }return null;
        }));
    }
    private void saveBank(String uid,JSONObject body)throws Exception{
        Map<String,Object> teacher=must(Tasks.await(db.collection("tutor_profiles").document(uid).get(Source.SERVER)));check(text(teacher,"trang_thai_xac_minh").equals("VERIFIED"),"Chỉ gia sư đã duyệt được lưu tài khoản nhận tiền.");
        String bank=reason(body,2,100,"bank_name"),name=reason(body,2,100,"account_name"),number=body.optString("account_number");check(number.matches("[0-9]{6,30}"),"Số tài khoản phải gồm 6–30 chữ số.");
        Tasks.await(ref("payout_accounts",uid).set(map("is_sandbox",true,"bank_name",bank,"account_number",number,"account_name",name,"updated_at",stamp())));
    }
    private JSONObject payroll(String uid,String period)throws Exception{
        check(period.matches("20[0-9]{2}-(0[1-9]|1[0-2])"),"Kỳ thanh toán phải có dạng YYYY-MM.");YearMonth month=YearMonth.parse(period);
        check(month.isBefore(YearMonth.from(Instant.ofEpochMilli(clock.getAsLong()).atZone(VN))),"Chỉ lập bảng cho tháng đã kết thúc.");
        List<DocumentSnapshot> candidates=Tasks.await(db.collection(collection("tutor_earnings")).whereEqualTo("period",period).get(Source.SERVER)).getDocuments();
        Map<String,List<DocumentReference>> teachers=new TreeMap<>();for(DocumentSnapshot row:candidates)if(row.get("payout_id")==null)teachers.computeIfAbsent(row.getString("tutor_id"),k->new ArrayList<>()).add(row.getReference());
        int count=0;for(Map.Entry<String,List<DocumentReference>> entry:teachers.entrySet())for(int offset=0;offset<entry.getValue().size();offset+=100){
            List<DocumentReference> group=entry.getValue().subList(offset,Math.min(offset+100,entry.getValue().size()));DocumentReference payout=ref("tutor_payouts",UUID.randomUUID().toString());
            boolean created=Tasks.await(db.runTransaction(tx->{List<DocumentSnapshot> rows=new ArrayList<>();for(DocumentReference r:group){DocumentSnapshot row=tx.get(r);if(row.exists()&&row.get("payout_id")==null)rows.add(row);}if(rows.isEmpty())return false;
                Map<String,Object> bank=must(tx.get(ref("payout_accounts",entry.getKey())));long total=0;for(DocumentSnapshot row:rows)total+=num(row.getData(),"amount");
                tx.set(payout,map("is_sandbox",true,"period",period,"tutor_id",entry.getKey(),"tutor_name",rows.get(0).getString("tutor_name"),"amount",total,"session_count",rows.size(),"earning_ids",rows.stream().map(DocumentSnapshot::getId).collect(java.util.stream.Collectors.toList()),"bank",map("bank_name",bank.get("bank_name"),"account_number",bank.get("account_number"),"account_name",bank.get("account_name")),"status","READY","due_at",new Timestamp(Date.from(month.plusMonths(1).atDay(5).atStartOfDay(VN).toInstant())),"created_by",uid,"created_at",stamp(),"updated_at",stamp()));
                for(DocumentSnapshot row:rows)tx.update(row.getReference(),"payout_id",payout.getId());return true;
            }));if(created)count++;
        }return PaymentState.json("ok",true,"created",count);
    }
    private void payoutAction(String uid,String id,String action,JSONObject body)throws Exception{
        check(Arrays.asList("paid","cancel").contains(action),"Thao tác chi trả chưa hợp lệ.");
        Tasks.await(db.runTransaction(tx->{DocumentReference ref=ref("tutor_payouts",id);Map<String,Object> p=must(tx.get(ref));
            if(action.equals("paid")&&text(p,"status").equals("PAID"))return null;check(text(p,"status").equals("READY"),"Chỉ xử lý bảng chưa chi trả.");
            if(action.equals("paid")){check(clock.getAsLong()>=millis(p.get("due_at")),"Đến ngày 5 tháng sau mới được xác nhận chi trả.");tx.update(ref,map("status","PAID","bank_reference",reason(body,3,150,"reference"),"paid_by",uid,"paid_at",stamp(),"updated_at",stamp()));}
            else{String reason=reason(body,5,500,"reason");List<DocumentReference> rows=new ArrayList<>();for(Object earning:(List<?>)p.get("earning_ids")){DocumentReference er=ref("tutor_earnings",String.valueOf(earning));check(id.equals(tx.get(er).getString("payout_id")),"Bảng chi trả đã thay đổi.");rows.add(er);}for(DocumentReference er:rows)tx.update(er,"payout_id",null);tx.update(ref,map("status","CANCELLED","cancel_reason",reason,"cancelled_by",uid,"updated_at",stamp()));}return null;
        }));
    }
    private void refund(String uid,boolean admin,String id,JSONObject body)throws Exception{
        String reason=reason(body,5,500,"reason");DocumentReference rr=ref("refund_requests",UUID.randomUUID().toString());
        Tasks.await(db.runTransaction(tx->{DocumentReference pr=ref("payments",id);Map<String,Object> p=must(tx.get(pr));check(admin||uid.equals(p.get("student_id")),"Chỉ học sinh yêu cầu hoàn học phí.");
            if(text(p,"status").equals("REFUND_REQUESTED"))return null;check(paid(text(p,"status")),"Chưa có học phí để hoàn.");long amount=num(p,"received_amount")-num(p,"refund_total")-num(p,"earned_amount");check(amount>0,"Không còn học phí chưa sử dụng.");
            tx.set(rr,map("is_sandbox",true,"payment_id",id,"student_id",p.get("student_id"),"tutor_id",p.get("tutor_id"),"title",p.get("title"),"amount",amount,"reason",reason,"status","PENDING","resume_status",p.get("status"),"created_at",stamp(),"updated_at",stamp()));tx.update(pr,map("status","REFUND_REQUESTED","refund_request_id",rr.getId(),"updated_at",stamp()));return null;
        }));
    }
    private void refundAction(String uid,String id,String action,JSONObject body)throws Exception{
        check(Arrays.asList("paid","reject").contains(action),"Thao tác hoàn tiền chưa hợp lệ.");
        Tasks.await(db.runTransaction(tx->{DocumentReference rr=ref("refund_requests",id);Map<String,Object> r=must(tx.get(rr));if(action.equals("paid")&&text(r,"status").equals("REFUNDED"))return null;
            DocumentReference pr=ref("payments",text(r,"payment_id"));Map<String,Object> p=must(tx.get(pr));check(text(r,"status").equals("PENDING")&&text(p,"status").equals("REFUND_REQUESTED")&&id.equals(p.get("refund_request_id")),"Yêu cầu không còn chờ xử lý.");
            if(action.equals("paid")){tx.update(rr,map("status","REFUNDED","bank_reference",reason(body,3,150,"reference"),"refunded_by",uid,"refunded_at",stamp(),"updated_at",stamp()));tx.update(pr,map("status","CLOSED_REFUNDED","refund_total",num(p,"refund_total")+num(r,"amount"),"updated_at",stamp()));}
            else{tx.update(rr,map("status","REJECTED","review_note",reason(body,5,500,"reason"),"reviewed_by",uid,"updated_at",stamp()));tx.update(pr,map("status",r.get("resume_status"),"updated_at",stamp()));}return null;
        }));
    }
}
