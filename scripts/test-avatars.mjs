// Local Emulator Suite only. Never seeds production.
import assert from 'node:assert/strict';
const firestoreHost=process.env.FIRESTORE_EMULATOR_HOST||'127.0.0.1:8080';
const authHost=process.env.FIREBASE_AUTH_EMULATOR_HOST||'127.0.0.1:9099';
for(const host of [firestoreHost,authHost])assert.match(host,/^(127\.0\.0\.1|localhost):[0-9]+$/,'Tests require loopback emulators');
const root=`http://${firestoreHost}/v1/projects/tmdt-3e274/databases/(default)/documents`;
const auth=`http://${authHost}/identitytoolkit.googleapis.com/v1`;
const prefix='projects/tmdt-3e274/databases/(default)/documents/';
const fields=data=>Object.fromEntries(Object.entries(data).map(([k,v])=>[k,typeof v==='boolean'?{booleanValue:v}:{stringValue:v}]));
async function request(url,method,body,token){const r=await fetch(url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});return {status:r.status,data:await r.json()};}
function write(path,data,create=false){return {update:{name:prefix+path,fields:fields(data)},...(create?{}:{updateMask:{fieldPaths:Object.keys(data)}}),updateTransforms:[{fieldPath:'updated_at',setToServerValue:'REQUEST_TIME'},...(create&&path.startsWith('users/')?[{fieldPath:'created_at',setToServerValue:'REQUEST_TIME'}]:[])]};}
const commit=(writes,token)=>request(root+':commit','POST',{writes},token);
let passed=0;
function check(ok,label){assert.ok(ok,label);console.log('PASS '+label);passed++;}
async function signup(suffix){
 const email=`avatar-${Date.now()}-${suffix}@example.com`,password='HocTap123!';
 const r=await request(auth+'/accounts:signUp?key=emulator','POST',{email,password,returnSecureToken:true});assert.equal(r.status,200);
 const verified=await request(auth+'/accounts:update?key=emulator','POST',{localId:r.data.localId,emailVerified:true},'owner');assert.equal(verified.status,200);
 const login=await request(auth+'/accounts:signInWithPassword?key=emulator','POST',{email,password,returnSecureToken:true});assert.equal(login.status,200);return login.data;
}
const owner=await signup('owner'),other=await signup('other'),uid=owner.localId;
const profile={ho_ten:'Avatar kiểm thử',email:owner.email,so_dien_thoai:'0912345678',dia_chi:'',anh_dai_dien:'',vai_tro:'STUDENT',la_gia_su:false,trang_thai:'ACTIVE'};
check((await commit([write('users/'+uid,profile,true)],owner.idToken)).status===200,'Create account');
const image='iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=';
const save=(avatar,extra={})=>[write('users/'+uid,{anh_dai_dien:avatar}),write('profile_avatars/'+uid,{anh_dai_dien:avatar,...extra},true)];
check((await commit(save(image),owner.idToken)).status===200,'Save private and display images atomically');
check((await request(root+'/profile_avatars/'+uid,'GET',null,other.idToken)).data.fields.anh_dai_dien.stringValue===image,'Other signed-in user reads avatar');
check((await request(root+'/users/'+uid,'GET',null,other.idToken)).status===403,'Private contact profile stays hidden');
check((await request(root+'/profile_avatars/'+uid,'GET')).status===403,'Guest cannot read student avatar');
check((await request(root+'/profile_avatars','GET',null,owner.idToken)).status===403,'Avatar directory cannot be listed');
check((await commit([write('profile_avatars/'+uid,{anh_dai_dien:'fake'},true)],other.idToken)).status===403,'Another account cannot overwrite image');
check((await commit([write('profile_avatars/'+uid,{anh_dai_dien:'different'},true)],owner.idToken)).status===403,'Image must match saved private profile');
check((await commit(save(image,{email:owner.email}),owner.idToken)).status===403,'Contacts cannot leak into display document');
check((await commit(save('x'.repeat(90001)),owner.idToken)).status===403,'Oversized image rejected');
check((await request(root+'/users/'+uid,'GET',null,owner.idToken)).data.fields.anh_dai_dien.stringValue===image,'Rejected batch preserves saved image');
check((await commit(save(''),owner.idToken)).status===200,'Clearing image clears both documents');
// A trusted admin approves the tutor; guests can see that tutor image only.
await request(root+'/tutor_profiles/'+uid,'PATCH',{fields:fields({trang_thai_xac_minh:'VERIFIED'})},'owner');
check((await request(root+'/profile_avatars/'+uid,'GET')).status===200,'Verified tutor avatar is public');
await request(root+'/profile_avatars/'+uid,'DELETE',null,'owner');
await commit([write('users/'+uid,{anh_dai_dien:image})],owner.idToken);
check((await commit([write('profile_avatars/'+uid,{anh_dai_dien:image},true)],owner.idToken)).status===200,'Existing saved avatar can be backfilled');
await request(root+'/users/'+uid+'?updateMask.fieldPaths=trang_thai','PATCH',{fields:fields({trang_thai:'BANNED'})},'owner');
check((await commit([write('profile_avatars/'+uid,{anh_dai_dien:image},true)],owner.idToken)).status===403,'Banned owner cannot republish avatar');
console.log(`\n${passed} avatar checks passed (localhost only).`);
