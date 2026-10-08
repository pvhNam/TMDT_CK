import assert from 'node:assert/strict';
const root='http://127.0.0.1:8080/v1/projects/tmdt-3e274/databases/(default)/documents';
const auth='http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const prefix='projects/tmdt-3e274/databases/(default)/documents/';
let passed=0;
const field=v=>Array.isArray(v)?{arrayValue:{values:v.map(field)}}:typeof v==='boolean'?{booleanValue:v}:typeof v==='number'?{integerValue:String(v)}:{stringValue:v};
const fields=d=>Object.fromEntries(Object.entries(d).map(([k,v])=>[k,field(v)]));
async function request(url,method,body,token){const r=await fetch(url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});return {status:r.status,data:await r.json()};}
function check(value,label){assert.ok(value,label);console.log('PASS '+label);passed++;}
async function signup(suffix){const r=await request(auth+'/accounts:signUp?key=emulator','POST',{email:`tutor-${Date.now()}-${suffix}@example.com`,password:'HocTap123!',returnSecureToken:true});assert.equal(r.status,200);return r.data;}
function write(path,data,{create=false,stamp=false}={}){return {update:{name:prefix+path,fields:fields(data)},...(create?{currentDocument:{exists:false}}:{updateMask:{fieldPaths:Object.keys(data)}}),...(stamp?{updateTransforms:[{fieldPath:'updated_at',setToServerValue:'REQUEST_TIME'},...(create?[{fieldPath:'created_at',setToServerValue:'REQUEST_TIME'}]:[])]}:{})};}
const commit=(writes,token)=>request(root+':commit','POST',{writes},token);
const owner=await signup('owner'),other=await signup('other');
const uid=owner.localId;
const base={ho_ten:'Gia sư kiểm thử',email:owner.email,so_dien_thoai:'0912345678',dia_chi:'',anh_dai_dien:'',vai_tro:'STUDENT',la_gia_su:false,trang_thai:'ACTIVE'};
check((await commit([write('users/'+uid,base,{create:true,stamp:true})],owner.idToken)).status===200,'Create student');
await request(root+'/subjects/toan','PATCH',{fields:fields({ten_mon:'Toán',dang_su_dung:true})},'owner');
await request(root+'/subjects/hidden','PATCH',{fields:fields({ten_mon:'Đã ẩn',dang_su_dung:false})},'owner');
const profile={user_id:uid,ho_ten:base.ho_ten,hoc_van:'Đại học',truong:'',chuyen_nganh:'',gioi_thieu:'',khu_vuc:'TP. Hồ Chí Minh',so_nam_kinh_nghiem:2,hinh_thuc_day:'CA_HAI',nhan_lop:true,trang_thai_xac_minh:'PENDING'};
function registration({status='PENDING',price=150000,subject='toan',userExtra={},profileExtra={}}={}){
  return [write('tutor_profiles/'+uid,{...profile,trang_thai_xac_minh:status,...profileExtra},{create:true,stamp:true}),
    ...['THCS','THPT'].map(level=>write(`tutor_subjects/${uid}_${subject}_${level}`,{tutor_id:uid,subject_id:subject,cap_lop:level,hoc_phi_moi_buoi:price},{create:true})),
    write('users/'+uid,{la_gia_su:true,ho_ten:base.ho_ten,so_dien_thoai:base.so_dien_thoai,...userExtra},{stamp:true})];
}
check((await commit([write('users/'+uid,{la_gia_su:true},{stamp:true})],owner.idToken)).status===403,'Cannot enable tutor without a registration');
check((await commit([registration()[0]],owner.idToken)).status===403,'Cannot create an orphan tutor profile');
for(const [options,label] of [[{status:'VERIFIED'},'Self approval'],[{price:49999},'Price below minimum'],[{price:2000001},'Price above maximum'],[{subject:'hidden'},'Inactive subject'],[{userExtra:{vai_tro:'ADMIN'}},'Admin escalation'],[{profileExtra:{nguoi_duyet_id:uid}},'Forged reviewer'],[{profileExtra:{so_nam_kinh_nghiem:-1}},'Negative experience']]){
  check((await commit(registration(options),owner.idToken)).status===403,label+' rejected');
}
check((await commit(registration(),other.idToken)).status===403,'Other user cannot submit this profile');
check((await request(root+'/tutor_profiles/'+uid,'GET',null,owner.idToken)).status===404,'Failed batches leave no profile');
const saved=await commit(registration(),owner.idToken);check(saved.status===200,'Atomic tutor registration succeeds: '+JSON.stringify(saved.data.error??''));
check((await request(root+'/users/'+uid,'GET',null,owner.idToken)).data.fields.la_gia_su.booleanValue,'Tutor flag persists');
check((await request(root+'/users/'+uid,'GET',null,owner.idToken)).data.fields.vai_tro.stringValue==='STUDENT','Original account role preserved');
check((await request(root+'/tutor_profiles/'+uid,'GET',null,owner.idToken)).data.fields.trang_thai_xac_minh.stringValue==='PENDING','Owner reads pending status');
check((await request(root+'/tutor_profiles/'+uid,'GET')).status===403,'Pending profile is not public');
check((await request(root+'/tutor_profiles/'+uid,'GET',null,other.idToken)).status===403,'Another student cannot read pending profile');
check((await commit([write('tutor_profiles/'+uid,{trang_thai_xac_minh:'VERIFIED'},{stamp:true})],owner.idToken)).status===403,'Registered tutor cannot approve themselves');
check((await commit([write(`tutor_subjects/${uid}_toan_THPT`,{hoc_phi_moi_buoi:1})],owner.idToken)).status===403,'Cannot rewrite published offering rules');
check((await commit(registration(),owner.idToken)).status!==200,'Duplicate submission cannot overwrite registration');
const group={tutor_id:uid,ten_gia_su:base.ho_ten,ten_lop:'Toán THPT',mon_hoc:'Toán',cap_hoc:'THPT',hinh_thuc:'Trực tuyến',dia_chi:'',mo_ta:'Ôn tập Toán',ngay_bat_dau:'2026-12-01',thu:[2],gio:18,thoi_luong:90,so_buoi:4,hoc_phi:150000,tinh_theo_buoi:true,suc_chua:5,thanh_vien:[],trang_thai:'OPEN',hinh_minh_hoa:0};
check((await commit([write('group_classes/pending-'+uid,group,{create:true,stamp:true})],owner.idToken)).status===403,'Pending tutor cannot open classes');
await request(root+'/tutor_profiles/'+uid,'PATCH',{fields:fields({trang_thai_xac_minh:'VERIFIED'})},'owner');
check((await commit([write('group_classes/verified-'+uid,group,{create:true,stamp:true})],owner.idToken)).status===200,'Same class is allowed after trusted approval');
check((await request(root+`/tutor_subjects/${uid}_toan_THPT`,'GET')).data.fields.hoc_phi_moi_buoi.integerValue==='150000','Stored price remains intact');
console.log(`\n${passed} tutor registration checks passed (localhost only).`);
