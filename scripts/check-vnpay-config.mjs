import {readFileSync,existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';

const root=new URL('../',import.meta.url);
const file=new URL('vnpay.properties',root),config={};
if(existsSync(file))for(const line of readFileSync(file,'utf8').split(/\r?\n/)){
  const match=line.match(/^\s*([A-Z_]+)\s*=(.*)$/);if(match)config[match[1]]=match[2].trim();
}
const merchant=/^[A-Za-z0-9]{8}$/.test(config.VNPAY_TMN_CODE||'');
const secret=(config.VNPAY_HASH_SECRET||'').length>=16;
let callback=false;
try{const u=new URL(config.VNPAY_RETURN_URL);callback=u.protocol==='https:'&&!u.username&&!u.password&&!u.port&&!u.search&&!u.hash&&u.pathname!=='/';}catch{}
let firebase=false;
try{const json=JSON.parse(readFileSync(new URL('app/google-services.json',root),'utf8'));firebase=json.project_info.project_id==='tmdt-3e274'&&json.client.some(c=>c.client_info.android_client_info?.package_name==='com.example.tmdt');}catch{}
const rules=readFileSync(new URL('firestore.rules',root),'utf8');
const ledger=['sandbox_payments','sandbox_tutor_earnings','sandbox_tutor_payouts','sandbox_payout_accounts','sandbox_refund_requests'].every(name=>rules.includes('match /'+name+'/'));
const gitignore=readFileSync(new URL('.gitignore',root),'utf8').split(/\r?\n/).includes('/vnpay.properties');
const missing=[...(!merchant?['VNPAY_TMN_CODE']:[]),...(!secret?['VNPAY_HASH_SECRET']:[]),...(!callback?['VNPAY_RETURN_URL']:[]),...(!firebase?['Firebase Android configuration']:[]),...(!ledger?['Firestore rules for VNPay TEST']:[]),...(!gitignore?['Ignore private VNPay config in Git']:[])];
console.log(JSON.stringify({mode:'Native VNPay TEST / WebView',configFile:fileURLToPath(file),merchantConfigured:merchant,secretConfigured:secret,returnUrlValid:callback,firebaseProjectMatches:firebase,firestoreRulesPresent:ledger,privateConfigIgnored:gitignore,backendRequired:false,missing,
  note:'Checks local configuration only; VNPay merchant approval and published Firestore rules are checked separately.'},null,2));
if(missing.length)process.exitCode=1;
