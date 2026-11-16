import { test } from 'node:test';
import assert from 'node:assert/strict';
import { OtpService } from '../src/otp-service.mjs';
import { createOtpServer } from '../src/http.mjs';

function fixture() {
  const records=new Map(), messages=[], user={uid:'alice',email:'alice@example.com',emailVerified:false};
  let time=1000000, updates=0, chain=Promise.resolve();
  const auth={getUser:async()=>({...user}),updateUser:async(uid,fields)=>{updates++;Object.assign(user,fields);}};
  const store={transaction(callback){
    const task=chain.then(async()=>{
      const writes=new Map();
      const result=await callback({get:async key=>structuredClone(writes.get(key)||records.get(key)||null),set:(key,value)=>writes.set(key,structuredClone(value))});
      for(const [key,value] of writes)records.set(key,value);return result;
    });chain=task.catch(()=>{});return task;
  }};
  const service=new OtpService({store,auth,mail:{send:async message=>messages.push(message)},secret:'s'.repeat(64),now:()=>time});
  return {service,auth,user,messages,records,advance:ms=>time+=ms,updates:()=>updates};
}
const rejected=(promise,code)=>assert.rejects(promise,error=>error.code===code);

test('Only successful code verification marks the Firebase account verified',async()=>{
  const f=fixture(), challenge=await f.service.send(f.user,'127.0.0.1');
  assert.match(f.messages[0].code,/^\d{6}$/);assert.equal(f.user.emailVerified,false);
  assert.equal(JSON.stringify([...f.records.values()]).includes(f.messages[0].code),false);
  assert.equal('code' in challenge,false);
  await f.service.verify({...f.user},challenge.challengeId,f.messages[0].code);
  assert.equal(f.user.emailVerified,true);assert.equal(f.updates(),1);
  await rejected(f.service.verify({...f.user,emailVerified:false},challenge.challengeId,f.messages[0].code),'CODE_USED');
});
test('Wrong codes never grant verification; five attempts lock the challenge',async()=>{
  const f=fixture(), c=await f.service.send(f.user,'ip'), wrong=f.messages[0].code==='000000'?'111111':'000000';
  for(let i=0;i<5;i++)await rejected(f.service.verify(f.user,c.challengeId,wrong),'CODE_INCORRECT');
  await rejected(f.service.verify(f.user,c.challengeId,f.messages[0].code),'ATTEMPTS_EXCEEDED');assert.equal(f.updates(),0);
});
test('Expired codes fail even when digits are correct',async()=>{
  const f=fixture(),c=await f.service.send(f.user,'ip');f.advance(300001);
  await rejected(f.service.verify(f.user,c.challengeId,f.messages[0].code),'CODE_EXPIRED');
});
test('Resending enforces cooldown and invalidates the previous challenge',async()=>{
  const f=fixture(),c=await f.service.send(f.user,'ip');
  await assert.rejects(f.service.send(f.user,'ip'),e=>e.code==='RESEND_WAIT'&&e.extra.retryAfter===60&&e.extra.challengeId===c.challengeId);
  f.advance(60000);const next=await f.service.send(f.user,'ip');assert.notEqual(next.challengeId,c.challengeId);
  await rejected(f.service.verify(f.user,c.challengeId,f.messages[0].code),'CODE_REPLACED');
  await f.service.verify(f.user,next.challengeId,f.messages[1].code);
});
test('A code is bound to the requesting UID and current email',async()=>{
  const f=fixture(),c=await f.service.send(f.user,'ip');
  await rejected(f.service.verify({...f.user,uid:'bob'},c.challengeId,f.messages[0].code),'CODE_REPLACED');
  await rejected(f.service.verify({...f.user,email:'changed@example.com'},c.challengeId,f.messages[0].code),'CODE_REPLACED');
});
test('Sending is limited per account and per day',async()=>{
  const f=fixture();for(let i=0;i<5;i++){await f.service.send(f.user,'ip');f.advance(60000);}
  await rejected(f.service.send(f.user,'ip'),'SEND_LIMIT');
  const d=fixture();d.service.dailyLimit=1;await d.service.send(d.user,'ip');d.advance(60000);
  await rejected(d.service.send({...d.user,uid:'bob',email:'bob@example.com'},'different-ip'),'SEND_LIMIT');
});
test('SMTP failure invalidates the reserved code without claiming success',async()=>{
  const f=fixture();f.service.mail.send=async()=>{throw new Error('SMTP failure');};
  await rejected(f.service.send(f.user,'ip'),'MAIL_UNAVAILABLE');
  assert.equal(f.records.get(f.service.challengeKey('alice')).status,'MAIL_FAILED');assert.equal(f.updates(),0);
});
test('Concurrent correct submissions only perform one account update',async()=>{
  const f=fixture(),c=await f.service.send(f.user,'ip');
  let release;f.auth.updateUser=()=>new Promise(resolve=>{release=resolve;});
  const first=f.service.verify(f.user,c.challengeId,f.messages[0].code);
  while(!release)await new Promise(resolve=>setImmediate(resolve));
  await rejected(f.service.verify(f.user,c.challengeId,f.messages[0].code),'VERIFYING');release();await first;
});
test('An Admin API failure permits retry without accepting an incorrect code',async()=>{
  const f=fixture(),c=await f.service.send(f.user,'ip'),original=f.auth.updateUser;
  f.auth.updateUser=async()=>{throw new Error('Unavailable');};
  await rejected(f.service.verify(f.user,c.challengeId,f.messages[0].code),'VERIFY_UNAVAILABLE');
  f.auth.updateUser=original;await f.service.verify(f.user,c.challengeId,f.messages[0].code);assert.equal(f.user.emailVerified,true);
});
test('The API requires a bearer token and ignores client-supplied recipient addresses',async t=>{
  const f=fixture();f.auth.verifyIdToken=async token=>{if(token!=='valid-token')throw new Error();return {uid:'alice'};};
  const server=createOtpServer({service:f.service,auth:f.auth,projectId:'test'});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(()=>new Promise(resolve=>server.close(resolve)));
  const base=`http://127.0.0.1:${server.address().port}`;
  const request=(body,token)=>fetch(base+'/email-otp/send',{method:'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},body:JSON.stringify(body)});
  assert.equal((await request({})).status,401);assert.equal((await request({},'forged')).status,401);
  const response=await request({email:'victim@example.com'},'valid-token');assert.equal(response.status,200);
  assert.equal(f.messages[0].to,'alice@example.com');assert.equal('code' in await response.json(),false);
  const malformed=await fetch(base+'/email-otp/verify',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer valid-token'},body:'{'});
  assert.equal(malformed.status,400);
});
