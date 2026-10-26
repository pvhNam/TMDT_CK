import assert from 'node:assert/strict';
const project='tmdt-3e274',prefix=`projects/${project}/databases/(default)/documents/`;
const db=`http://127.0.0.1:${process.env.TEST_FIRESTORE_PORT||8080}/v1/${prefix.slice(0,-1)}`,auth=`http://127.0.0.1:${process.env.TEST_AUTH_PORT||9099}/identitytoolkit.googleapis.com/v1`;
const value=v=>Buffer.isBuffer(v)?{bytesValue:v.toString('base64')}:Array.isArray(v)?{arrayValue:{values:v.map(value)}}:typeof v==='object'?{mapValue:{fields:fields(v)}}:typeof v==='boolean'?{booleanValue:v}:typeof v==='number'?{integerValue:String(v)}:{stringValue:v};
const fields=v=>Object.fromEntries(Object.entries(v).map(([k,v])=>[k,value(v)]));
let passed=0;
async function request(url,method,body,token){const r=await fetch(url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});return {status:r.status,data:await r.json()};}
function check(result,label){assert.ok(result,label);console.log('PASS '+label);passed++;}
async function signup(role='STUDENT'){
 const email=`review-${Date.now()}-${Math.random()}@example.com`;
 const r=await request(auth+'/accounts:signUp?key=emulator','POST',{email,password:'HocTap123!',returnSecureToken:true});assert.equal(r.status,200);
 const verified=await request(auth+'/accounts:update?key=emulator','POST',{localId:r.data.localId,emailVerified:true},'owner');assert.equal(verified.status,200);
 const login=await request(auth+'/accounts:signInWithPassword?key=emulator','POST',{email,password:'HocTap123!',returnSecureToken:true});assert.equal(login.status,200);
 const u={...login.data,email};
 const profile={ho_ten:'Nguyễn Minh An',email,so_dien_thoai:'0912345678',dia_chi:'',anh_dai_dien:'',vai_tro:role,la_gia_su:false,trang_thai:'ACTIVE'};
 if(role==='ADMIN')await request(db+'/users/'+u.localId,'PATCH',{fields:fields(profile)},'owner');
 else assert.equal((await commit([write('users/'+u.localId,profile,true)],u.idToken)).status,200);
 return u;
}
function write(path,data,create=false,extra=[]){return {update:{name:prefix+path,fields:fields(data)},...(create?{currentDocument:{exists:false}}:{updateMask:{fieldPaths:Object.keys(data)}}),updateTransforms:[{fieldPath:'updated_at',setToServerValue:'REQUEST_TIME'},...(create?[{fieldPath:'created_at',setToServerValue:'REQUEST_TIME'}]:[]),...extra.map(fieldPath=>({fieldPath,setToServerValue:'REQUEST_TIME'}))]};}
const commit=(writes,token)=>request(db+':commit','POST',{writes},token);
const owner=await signup(),other=await signup(),admin=await signup('ADMIN'),uid=owner.localId;
const subjectId='review_toan_'+Date.now();
assert.equal((await request(db+'/subjects/'+subjectId,'PATCH',{fields:fields({dang_su_dung:true,ten_mon:'Toán'})},'owner')).status,200);
// The app writes exactly these four fields, without timestamp transforms.
function offering(tutorId,level,price=150000){return {update:{name:prefix+`tutor_subjects/${tutorId}_${subjectId}_${level}`,fields:fields({tutor_id:tutorId,subject_id:subjectId,cap_lop:level,hoc_phi_moi_buoi:price})},currentDocument:{exists:false}};}
const path=`tutor_applications/${uid}/evidence_images/proof-001`,photo=Buffer.from([255,216,255,224,0,16,255,217]);
function imageWrite(name,body=photo,type='image/jpeg',extra={}) {
 const data={user_id:name.split('/')[1],name:'bang_cap.jpg',type,size:body.length,kind:'diploma',anh:body,...extra};
 return {update:{name:prefix+name,fields:fields(data)},currentDocument:{exists:false},updateTransforms:[{fieldPath:'created_at',setToServerValue:'REQUEST_TIME'}]};
}
async function storage(token,name=path,method='GET',body,type='image/jpeg'){
 return method==='POST'?commit([imageWrite(name,body,type)],token):request(db+'/'+name,'GET',null,token);
}
const uploaded=await storage(owner.idToken,path,'POST',photo);check(uploaded.status===200,'Owner uploads private Firestore image before submitting: '+uploaded.status+' '+(uploaded.status===200?'':JSON.stringify(uploaded.data)));
check((await storage(owner.idToken,`tutor_applications/${uid}/evidence_images/pdf-only`,'POST',Buffer.from('%PDF-1.4'),'application/pdf')).status===403,'PDF cannot replace required photo evidence');
check((await storage()).status===403,'Guest cannot download evidence');
check((await storage(other.idToken)).status===403,'Other student cannot download evidence');
check((await storage(admin.idToken)).status===200,'Active administrator can inspect evidence');
check((await commit([imageWrite(`tutor_applications/${uid}/evidence_images/too-large`,Buffer.alloc(307201))],owner.idToken)).status===403,'Image bytes above 300 KiB are rejected');
check((await commit([imageWrite(`tutor_applications/${uid}/evidence_images/fake-size`,photo,'image/jpeg',{size:100})],owner.idToken)).status===403,'Image size must match the real byte count');
check((await commit([imageWrite(`tutor_applications/${other.localId}/evidence_images/forged-owner`)],owner.idToken)).status===403,'Cannot upload bytes to another account');
check((await commit([write(path,{anh:photo})],owner.idToken)).status===403,'Uploaded image bytes are immutable');
const file={name:'bang_cap.jpg',path,type:'image/jpeg',size:photo.length,kind:'diploma'};
const tutor={user_id:uid,ho_ten:'Nguyễn Minh An',hoc_van:'Đại học',truong:'Đại học Sư phạm',chuyen_nganh:'Toán',gioi_thieu:'',khu_vuc:'',so_nam_kinh_nghiem:2,hinh_thuc_day:'TRUC_TUYEN',nhan_lop:true,trang_thai_xac_minh:'PENDING'};
const application={user_id:uid,ho_ten:tutor.ho_ten,truong:tutor.truong,chuyen_nganh:tutor.chuyen_nganh,ghi_chu:'',minh_chung:[file],mon_hoc:'Toán',cap_lop:'THPT',hoc_phi:150000,trang_thai_xac_minh:'PENDING'};
const batch=(app=application)=>[write('tutor_profiles/'+uid,tutor,true),write('tutor_applications/'+uid,app,true,['submitted_at']),offering(uid,'THPT'),write('users/'+uid,{la_gia_su:true,ho_ten:tutor.ho_ten,so_dien_thoai:'0912345678'})];
check((await commit(batch({...application,minh_chung:[{...file,path:`tutor_applications/${other.localId}/evidence_images/x`}]}),owner.idToken)).status===403,'Cannot attach another user’s evidence');
check((await commit(batch({...application,minh_chung:[]}),owner.idToken)).status===403,'Empty evidence is rejected');
check((await commit(batch({...application,minh_chung:[{...file,path:`tutor_applications/${uid}/evidence_images/missing`}]}),owner.idToken)).status===403,'Cannot submit metadata for an image that was never uploaded');
check((await commit(batch({...application,minh_chung:[{...file,size:file.size+1}]}),owner.idToken)).status===403,'Application metadata must match stored image');
check((await commit(batch({...application,minh_chung:[{...file,type:'application/pdf'}]}),owner.idToken)).status===403,'PDF metadata is rejected by Firestore too');
check((await commit([batch()[0],batch()[3]],owner.idToken)).status===403,'Cannot bypass application evidence by submitting only public profile');
check((await commit(batch({...application,minh_chung:[{...file,size:10485761}]}),owner.idToken)).status===403,'Oversized evidence metadata is rejected');
const four=[file];
for(let i=1;i<=3;i++){
 const imagePath=`tutor_applications/${uid}/evidence_images/certificate-${i}`;
 check((await commit([imageWrite(imagePath,photo,'image/jpeg',{kind:'certificate'})],owner.idToken)).status===200,'Owner uploads certificate '+i);
 four.push({...file,path:imagePath,kind:'certificate'});
}
const registered=await commit(batch({...application,minh_chung:four}),owner.idToken);
check(registered.status===200,'Four referenced images and teaching offering fit within rules limits: '+JSON.stringify(registered.data));
check((await request(db+'/tutor_applications/'+uid,'GET',null,owner.idToken)).status===200,'Owner reads their application');
check((await request(db+'/tutor_applications/'+uid,'GET',null,other.idToken)).status===403,'Another account cannot read private application');
check((await request(db+'/tutor_applications/'+uid,'GET',null,admin.idToken)).status===200,'Admin reads private application');
const decision=(status,reason='',reviewer=admin.localId)=>[
 write('tutor_profiles/'+uid,{trang_thai_xac_minh:status}),
 write('tutor_applications/'+uid,{trang_thai_xac_minh:status,ly_do:reason,nguoi_duyet_id:reviewer},false,['reviewed_at'])];
check((await commit(decision('VERIFIED','',uid),owner.idToken)).status===403,'Tutor cannot approve themselves');
check((await commit([decision('VERIFIED')[0]],admin.idToken)).status===403,'Admin cannot change status without audit transaction');
check((await commit(decision('NEEDS_REVISION'),admin.idToken)).status===403,'Request for evidence requires a reason');
check((await commit(decision('IN_REVIEW'),admin.idToken)).status===200,'Admin starts checking profile');
check((await commit(decision('NEEDS_REVISION','Ảnh bằng cấp bị mờ.'),admin.idToken)).status===200,'Admin requests clearer evidence');
const replacement=`tutor_applications/${uid}/evidence_images/proof-002`;
check((await storage(owner.idToken,replacement,'POST',photo)).status===200,'Owner can upload replacement when requested');
const resubmit=[write('tutor_profiles/'+uid,{trang_thai_xac_minh:'PENDING',truong:tutor.truong,chuyen_nganh:tutor.chuyen_nganh}),write('tutor_applications/'+uid,{truong:tutor.truong,chuyen_nganh:tutor.chuyen_nganh,ghi_chu:'Đã thay ảnh rõ hơn',minh_chung:[{...file,path:replacement}],trang_thai_xac_minh:'PENDING'},false,['submitted_at'])];
check((await commit(resubmit,other.idToken)).status===403,'Other account cannot resubmit');
check((await commit(resubmit,owner.idToken)).status===200,'Requested supplement returns both documents to pending');
check((await commit(decision('VERIFIED'),admin.idToken)).status===200,'Admin approves resubmitted tutor');
check((await commit(decision('REJECTED','Đổi kết quả'),admin.idToken)).status===403,'An already reviewed profile cannot be changed by stale action');
check((await storage(owner.idToken,`tutor_applications/${uid}/evidence_images/proof-003`,'POST',photo)).status===403,'Owner cannot edit evidence after approval');
check((await request(db+'/tutor_profiles/'+uid,'GET')).status===200,'Approved tutor is public');
check((await request(db+'/tutor_applications/'+uid,'GET')).status===403,'Evidence remains private after approval');
const legacy=await signup();const legacyFields=fields({...tutor,user_id:legacy.localId});legacyFields.created_at={timestampValue:new Date().toISOString()};
await request(db+'/tutor_profiles/'+legacy.localId,'PATCH',{fields:legacyFields},'owner');
const reject=[write('tutor_profiles/'+legacy.localId,{trang_thai_xac_minh:'REJECTED'}),write('tutor_applications/'+legacy.localId,{user_id:legacy.localId,ho_ten:tutor.ho_ten,trang_thai_xac_minh:'REJECTED',ly_do:'Thông tin bằng cấp không phù hợp.',nguoi_duyet_id:admin.localId},true,['reviewed_at'])];
check((await commit([write('tutor_profiles/'+legacy.localId,{trang_thai_xac_minh:'VERIFIED'}),write('tutor_applications/'+legacy.localId,{user_id:legacy.localId,ho_ten:tutor.ho_ten,trang_thai_xac_minh:'VERIFIED',ly_do:'',nguoi_duyet_id:admin.localId},true,['reviewed_at'])],admin.idToken)).status===403,'Legacy profile without image cannot be approved');
check((await commit(reject,admin.idToken)).status===200,'Admin rejects a legacy profile and records private audit result');
check((await request(db+'/tutor_applications/'+legacy.localId,'GET',null,legacy.idToken)).data.fields.ly_do.stringValue==='Thông tin bằng cấp không phù hợp.','Rejected owner can read reason');
await request(db+'/users/'+admin.localId,'PATCH',{fields:fields({trang_thai:'BANNED'})},'owner');
check((await storage(admin.idToken)).status===403,'Banned admin cannot download evidence');
// Regression for the real registration transaction: images + application + public
// tutor profile + one offering per selected level + the account update.
const levelNames={TIEU_HOC:'Tiểu học',THCS:'THCS',THPT:'THPT',LUYEN_THI:'Luyện thi',KHAC:'Khác'};
for(const count of [1,3,4])for(const levels of [['LUYEN_THI'],Object.keys(levelNames)]){
 const user=await signup(),userId=user.localId,images=[];
 for(let i=0;i<count;i++){
  const imagePath=`tutor_applications/${userId}/evidence_images/registration-${i}`,kind=i?'certificate':'diploma';
  assert.equal((await commit([imageWrite(imagePath,photo,'image/jpeg',{kind})],user.idToken)).status,200);
  assert.equal((await request(db+'/'+imagePath,'GET',null,user.idToken)).status,200,'Owner must read each uploaded image from the server');
  images.push({...file,path:imagePath,kind});
 }
 const profile={...tutor,user_id:userId,ho_ten:'nam pham',hoc_van:'',truong:'nlu',chuyen_nganh:'it'};
 const applicationData={...application,user_id:userId,ho_ten:profile.ho_ten,truong:profile.truong,chuyen_nganh:profile.chuyen_nganh,minh_chung:images,cap_lop:levels.map(level=>levelNames[level]).join(', '),hoc_phi:55555};
 const submitted=await commit([write('tutor_profiles/'+userId,profile,true),write('tutor_applications/'+userId,applicationData,true,['submitted_at']),...levels.map(level=>offering(userId,level,55555)),write('users/'+userId,{la_gia_su:true,ho_ten:profile.ho_ten,so_dien_thoai:'0862228874'})],user.idToken);
 check(submitted.status===200,`Real registration: ${count} image(s), ${levels.length} teaching level(s), LUYEN_THI, price 55555: ${submitted.status===200?'OK':JSON.stringify(submitted.data)}`);
}
console.log(`\n${passed} Firestore Spark tutor review checks passed (localhost only).`);
