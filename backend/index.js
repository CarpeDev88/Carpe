import express from "express";
import {timingSafeEqual} from "node:crypto";

const app=express();
app.disable("x-powered-by");
app.set("trust proxy",1);
app.use(express.json({limit:"64kb"}));

const PORT=Number(process.env.PORT||8080);
const GEMINI_API_KEY=process.env.GEMINI_API_KEY||"";
const GEMINI_MODEL=process.env.GEMINI_MODEL||"gemini-2.5-flash";
const CARPE_APP_TOKEN=process.env.CARPE_APP_TOKEN||"";
const RATE_WINDOW_MS=60_000;
const configuredLimit=Number(process.env.CARPE_RATE_LIMIT_MAX||10);
const RATE_LIMIT_MAX=Number.isInteger(configuredLimit)?Math.min(1000,Math.max(1,configuredLimit)):10;
const rateWindows=new Map();

function rateLimit(req,res,next){
 const now=Date.now();
 const key=String(req.ip||req.socket.remoteAddress||"unknown").slice(0,120);
 let bucket=rateWindows.get(key);
 if(!bucket||now-bucket.startedAt>=RATE_WINDOW_MS){bucket={startedAt:now,count:0};rateWindows.set(key,bucket)}
 if(bucket.count>=RATE_LIMIT_MAX){
  const retryAfter=Math.max(1,Math.ceil((RATE_WINDOW_MS-(now-bucket.startedAt))/1000));
  res.set("Retry-After",String(retryAfter));
  return res.status(429).json({error:"Too many requests. Wait before trying again."});
 }
 bucket.count++;
 if(rateWindows.size>10_000){
  for(const [client,window] of rateWindows)if(now-window.startedAt>=RATE_WINDOW_MS)rateWindows.delete(client);
  if(rateWindows.size>10_000)rateWindows.clear();
 }
 next();
}

function authorized(value){
 if(!CARPE_APP_TOKEN||typeof value!=="string")return false;
 const expected=Buffer.from(`Bearer ${CARPE_APP_TOKEN}`);
 const actual=Buffer.from(value);
 return actual.length===expected.length&&timingSafeEqual(actual,expected);
}

const SYSTEM=`You are CARPE, a user-first AI whose success is measured by whether technology helps the person live the life they deliberately choose—not by engagement.
Protect autonomy, attention, privacy, time, money, relationships, and long-term goals.
Prefer practical real-world action when useful. Never manufacture urgency, guilt, streak pressure, outrage, compulsive checking, or dependence on CARPE.
Do not advertise or optimize for purchases. Ask when the user's intent is genuinely ambiguous.
Treat the user's explicit goals as theirs, not as permission to manipulate them.
For political subjects, provide balanced factual help and preserve the user's independent judgment.
Be concise, candid, useful, and willing to say when information is uncertain.`;

function clean(v,max){return typeof v==="string"?v.trim().slice(0,max):""}
function turns(history=[]){return Array.isArray(history)?history.slice(-10).flatMap(t=>{
 const text=clean(t?.text,1500); if(!text)return [];
 return [{role:t?.role==="assistant"?"model":"user",parts:[{text}]}];
}):[]}

app.get("/health",(req,res)=>{
 const ready=Boolean(GEMINI_API_KEY&&CARPE_APP_TOKEN);
 res.status(ready?200:503).json({ok:ready,service:"carpe-intelligence",model:GEMINI_MODEL,providerConfigured:Boolean(GEMINI_API_KEY),accessConfigured:Boolean(CARPE_APP_TOKEN)});
});

app.post("/v1/ask",rateLimit,async(req,res)=>{
 try{
  if(!CARPE_APP_TOKEN)return res.status(503).json({error:"AI service access is not configured"});
  if(!authorized(req.get("Authorization"))) return res.status(401).json({error:"Unauthorized"});
  if(!GEMINI_API_KEY)return res.status(503).json({error:"AI provider is not configured"});
  const message=clean(req.body?.message,6000);
  if(!message)return res.status(400).json({error:"message is required"});
  const profile=clean(req.body?.profile,2500);
  const purpose=clean(req.body?.purpose,1500);
  const contents=turns(req.body?.history);
  contents.push({role:"user",parts:[{text:message}]});
  const systemText=[SYSTEM,purpose&&`Client purpose: ${purpose}`,profile&&`User-controlled profile context (use only when relevant):\n${profile}`].filter(Boolean).join("\n\n");
  const url=`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(GEMINI_MODEL)}:generateContent`;
  const upstream=await fetch(url,{method:"POST",headers:{"Content-Type":"application/json","x-goog-api-key":GEMINI_API_KEY},body:JSON.stringify({
   systemInstruction:{parts:[{text:systemText}]},contents,
   generationConfig:{temperature:0.5,maxOutputTokens:1200}
  }),signal:AbortSignal.timeout(28000)});
  const data=await upstream.json().catch(()=>({}));
  if(!upstream.ok){console.error("Gemini error",upstream.status,data?.error?.status);return res.status(502).json({error:"AI provider request failed"});}
  const response=(data?.candidates?.[0]?.content?.parts||[]).map(p=>p?.text||"").join("").trim();
  if(!response)return res.status(502).json({error:"AI provider returned no response"});
  res.json({response});
 }catch(e){
  console.error("CARPE gateway error",e?.name||"Error");
  res.status(e?.name==="TimeoutError"?504:500).json({error:e?.name==="TimeoutError"?"AI provider timed out":"CARPE intelligence service error"});
 }
});

app.use((err,req,res,next)=>{console.error("Request error",err?.name||"Error");res.status(400).json({error:"Invalid request"});});
export default app;
if(process.env.CARPE_NO_LISTEN!=="1") app.listen(PORT,"0.0.0.0",()=>console.log(`CARPE intelligence listening on ${PORT}`));
