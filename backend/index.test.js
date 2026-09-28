import test from "node:test";
import assert from "node:assert/strict";

process.env.CARPE_NO_LISTEN="1";
process.env.GEMINI_API_KEY="test-only-key";
const {default:app}=await import("./index.js");

test("the gateway validates requests and relays a real provider-shaped response",async()=>{
 const server=app.listen(0,"127.0.0.1");
 await new Promise((resolve,reject)=>{server.once("listening",resolve);server.once("error",reject)});
 const originalFetch=globalThis.fetch;
 try {
  const base=`http://127.0.0.1:${server.address().port}`;
  const health=await originalFetch(`${base}/health`);
  assert.equal(health.status,200);
  assert.equal((await health.json()).providerConfigured,true);
  const invalid=await originalFetch(`${base}/v1/ask`,{method:"POST",headers:{"Content-Type":"application/json"},body:"{}"});
  assert.equal(invalid.status,400);
  globalThis.fetch=async(url,options)=>{
   assert.match(url,/generativelanguage\.googleapis\.com/);
   assert.equal(options.headers["x-goog-api-key"],"test-only-key");
   const body=JSON.parse(options.body);
   assert.equal(body.contents.at(-1).parts[0].text,"Help me cook dinner");
   return new Response(JSON.stringify({candidates:[{content:{parts:[{text:"What ingredients do you have?"}]}}]}),{status:200});
  };
  const reply=await originalFetch(`${base}/v1/ask`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({message:"Help me cook dinner"})});
  assert.equal(reply.status,200);
  assert.equal((await reply.json()).response,"What ingredients do you have?");
  globalThis.fetch=async()=>new Response(JSON.stringify({error:{status:"RESOURCE_EXHAUSTED"}}),{status:429});
  const failed=await originalFetch(`${base}/v1/ask`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({message:"Hello"})});
  assert.equal(failed.status,502);
  assert.equal((await failed.json()).error,"AI provider request failed");
 } finally {
  globalThis.fetch=originalFetch;
  await new Promise((resolve,reject)=>server.close(err=>err?reject(err):resolve()));
 }
});
