import assert from 'node:assert/strict';

// Deliberately fixed to loopback: these tests must never write to production.
const project = 'tmdt-3e274';
const firestore = `http://127.0.0.1:8080/v1/projects/${project}/databases/(default)/documents`;
const auth = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const stamp = `test${Date.now()}`;
let passed = 0;
async function request(url, method, body, token) {
  const response = await fetch(url, {method, headers: {'Content-Type':'application/json', ...(token?{Authorization:`Bearer ${token}`}:{})}, ...(body?{body:JSON.stringify(body)}:{})});
  return {status:response.status, data:await response.json()};
}
function fields(values) {
  return Object.fromEntries(Object.entries(values).map(([key,value])=>[key,
    typeof value==='boolean'?{booleanValue:value}:typeof value==='number'?{integerValue:String(value)}:{stringValue:value}]));
}
function check(condition, message) { assert.ok(condition,message); passed++; console.log(`PASS ${message}`); }
async function signup(suffix) {
  const email=`${stamp}${suffix}@example.com`;
  const result=await request(`${auth}/accounts:signUp?key=emulator`, 'POST', {email,password:'HocTap123!',returnSecureToken:true});
  assert.equal(result.status,200); return {...result.data,email};
}
async function write(path,values,token,create=false) {
  const write={update:{name:`projects/${project}/databases/(default)/documents/${path}`,fields:fields(values)},
    updateTransforms:[{fieldPath:'updated_at',setToServerValue:'REQUEST_TIME'}]};
  if(create) {write.currentDocument={exists:false};write.updateTransforms.push({fieldPath:'created_at',setToServerValue:'REQUEST_TIME'});}
  else write.updateMask={fieldPaths:Object.keys(values)};
  return request(`${firestore}:commit`,'POST',{writes:[write]},token);
}
const first=await signup('a'), second=await signup('b');
const profile={ho_ten:'Học viên kiểm thử',email:first.email,so_dien_thoai:'0912345678',dia_chi:'TP. Hồ Chí Minh',anh_dai_dien:'',vai_tro:'STUDENT',la_gia_su:false,trang_thai:'ACTIVE',cap_hoc:'THPT',khu_vuc:'TP. Hồ Chí Minh',muc_tieu_hoc_tap:'Học Toán'};
let result=await write(`users/${first.localId}`,profile,first.idToken,true);
check(result.status===200,'Register Auth account and create schema-compatible private profile');
result=await request(`${firestore}/users/${first.localId}`,'GET',null,first.idToken);
check(result.status===200 && result.data.fields.ho_ten.stringValue===profile.ho_ten,'Owner reads profile');
check(!result.data.fields.password_hash && !result.data.fields.ly_do_khoa,'Password and removed fields are absent');
check((await request(`${firestore}/users/${first.localId}`,'GET',null,second.idToken)).status===403,'Another user cannot read private profile');
check((await request(`${firestore}/users/${first.localId}`,'GET')).status===403,'Guest cannot read private profile');
check((await write(`users/${first.localId}`,{ho_ten:'Tên đã cập nhật',dia_chi:'Hà Nội'},first.idToken)).status===200,'Owner updates profile');
for(const [key,value] of Object.entries({vai_tro:'ADMIN',la_gia_su:true,trang_thai:'BANNED',email:second.email,ly_do_khoa:'forbidden',so_lan_dang_nhap_sai:1,khoa_dang_nhap_den:'forbidden',so_dien_thoai:'invalid'})) {
  check((await write(`users/${first.localId}`,{[key]:value},first.idToken)).status===403,`Reject unauthorized change: ${key}`);
}
check((await write(`users/${second.localId}`,{...profile,email:second.email,vai_tro:'ADMIN'},second.idToken,true)).status===403,'Cannot create own ADMIN account');
check((await write(`users/${second.localId}`,{...profile,email:second.email},second.idToken,true)).status===200,'Second student registers normally');
result=await request(`${auth}/accounts:signInWithPassword?key=emulator`,'POST',{email:first.email,password:'wrong-password',returnSecureToken:true});
check(result.status===400,'Wrong password is rejected');
result=await request(`${auth}/accounts:signInWithPassword?key=emulator`,'POST',{email:first.email,password:'HocTap123!',returnSecureToken:true});
check(result.status===200,'Correct password signs in');
result=await request(`${auth}/accounts:sendOobCode?key=emulator`,'POST',{requestType:'PASSWORD_RESET',email:first.email});
check(result.status===200,'Password reset email requested');
check((await request(`${auth}/accounts:signUp?key=emulator`,'POST',{email:first.email,password:'HocTap123!',returnSecureToken:true})).status===400,'Duplicate email rejected');
// Administrative fixtures are written only into the emulator; production app never seeds data.
for(const [path,values] of Object.entries({
  'subjects/toan':{ten_mon:'Toán',nhom_mon:'Tự nhiên',dang_su_dung:true},
  'subjects/tieng_anh':{ten_mon:'Tiếng Anh',nhom_mon:'Ngoại ngữ',dang_su_dung:true},
  'subjects/hidden':{ten_mon:'Đã ẩn',dang_su_dung:false},
  'tutor_profiles/demo_math':{user_id:'demo_math',ho_ten:'Nguyễn Minh Anh',hoc_van:'Đại học',truong:'Đại học Sư phạm',chuyen_nganh:'Toán học',so_nam_kinh_nghiem:3,gioi_thieu:'Hướng dẫn dễ hiểu, bám sát mục tiêu và năng lực của học viên.',khu_vuc:'TP. Hồ Chí Minh',hinh_thuc_day:'CA_HAI',nhan_lop:true,trang_thai_xac_minh:'VERIFIED'},
  'tutor_profiles/pending':{user_id:'pending',ho_ten:'Hồ sơ chưa duyệt',nhan_lop:true,trang_thai_xac_minh:'PENDING'},
  'tutor_subjects/demo_math_toan_thpt':{tutor_id:'demo_math',subject_id:'toan',cap_lop:'THPT',hoc_phi_moi_buoi:150000}
})) { assert.equal((await request(`${firestore}/${path}`,'PATCH',{fields:fields(values)},'owner')).status,200); }
check((await request(`${firestore}/tutor_profiles/demo_math`,'GET')).status===200,'Guests can read verified tutor');
check((await request(`${firestore}/tutor_profiles/pending`,'GET')).status===403,'Pending tutor is not public');
check((await request(`${firestore}/subjects/hidden`,'GET')).status===403,'Inactive subject is not public');
check((await request(`${firestore}/tutor_subjects/demo_math_toan_thpt`,'GET')).status===200,'Published subject pricing is readable');
for(const [collection,field,value] of [['subjects','dang_su_dung',true],['tutor_profiles','trang_thai_xac_minh','VERIFIED']]) {
  const filterValue=typeof value==='boolean'?{booleanValue:value}:{stringValue:value};
  const query={structuredQuery:{from:[{collectionId:collection}],where:{fieldFilter:{field:{fieldPath:field},op:'EQUAL',value:filterValue}}}};
  check((await request(`${firestore}:runQuery`,'POST',query)).status===200,`Home query allowed: ${collection}`);
}
check((await request(`${firestore}:runQuery`,'POST',{structuredQuery:{from:[{collectionId:'users'}]}},first.idToken)).status===403,'Users collection cannot be listed');
check((await write('tutor_profiles/demo_math',{trang_thai_xac_minh:'VERIFIED'},first.idToken)).status===403,'Student cannot publish tutor profile');
for(const collection of ['wallets','wallet_transactions','escrows','escrow_items','payos_transactions','payout_requests','certificates','job_posts','applications','invitations','invitation_terms','contracts','attendances','reviews','conversations','messages','system_configs']) {
  check((await write(`${collection}/test`,{user_id:first.localId},first.idToken,true)).status===403,`Removed collection denied: ${collection}`);
}
await request(`${firestore}/users/${first.localId}`,'PATCH',{fields:{trang_thai:{stringValue:'BANNED'}}},'owner');
check((await write(`users/${first.localId}`,{ho_ten:'Cannot update'},first.idToken)).status===403,'Banned profile cannot be edited');
console.log(`\n${passed} checks passed. Emulator catalog fixtures are ready for Android UI tests.`);
