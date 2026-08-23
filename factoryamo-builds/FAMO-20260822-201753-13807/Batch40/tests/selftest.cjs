const core=require('../app/src/main/assets/app.js');
const t=core.selfTestAll();
const bad=t.filter(x=>!x[1]);
console.log(`FactoryAMO Batch40 self-tests: ${t.length-bad.length}/${t.length}`);
for(const [name,ok] of t) console.log(`${ok?'PASS':'FAIL'} ${name}`);
if(t.length!==40 || bad.length) process.exit(1);
