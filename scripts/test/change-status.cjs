// Exercise the actual status-modal script without connecting to the user's browser or API.
const vm = require('node:vm');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const ids = ['newOrderStatus','costInputGroup','costPrice','costNote','lotInputGroup',
    'l_lot_number','l_start_date','l_end_date','shippingInputGroup','shippingPrice',
    'shippingNote','lotArriveDate','postalInputGroup','postalPrice','postalNote','statusRecordDate'];
const nodes = Object.fromEntries(ids.map(id => [id,{value:'',style:{}}]));
nodes.newOrderStatus.options = Array.from({length:6}, (_,i) => ({value:String(i+1)}));
const calls=[];
const sandbox = {document:{addEventListener(){},getElementById:id=>nodes[id]},console,
    fetch:(url,options)=>{calls.push({url,body:JSON.parse(options.body)});return new Promise(()=>{});},window:{}};
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync('src/main/resources/static/script/change.js','utf8'),sandbox);
const groups=['costInputGroup','lotInputGroup','shippingInputGroup','postalInputGroup'];
for(const [from,shown] of [[1,'costInputGroup'],[2,'lotInputGroup'],[3,null],[4,'shippingInputGroup'],[5,'postalInputGroup']]) {
    vm.runInContext(`selectedStatusId='${from}';configureNewStatusOptions()`,sandbox);
    assert.equal(nodes.newOrderStatus.value,String(from+1));
    assert.deepEqual(nodes.newOrderStatus.options.filter(o=>!o.disabled).map(o=>o.value),[String(from+1)]);
    for(const id of groups) assert.equal(nodes[id].style.display,id===shown?'':'none',`${from}: ${id}`);
    // A valid form must POST only to the existing update endpoint.
    nodes.costPrice.value='125';nodes.shippingPrice.value='125';nodes.postalPrice.value='125';
    nodes.lotArriveDate.value='2026-11-02';nodes.l_lot_number.value='LOT-A';
    nodes.l_start_date.value='2026-11-10';nodes.l_end_date.value='2026-11-15';
    vm.runInContext(`selectedRows.clear();selectedRows.set('10',{id:10});save_change_status()`,sandbox);
    assert.equal(calls.at(-1).body.orderStatus,from+1);
    assert.equal(calls.at(-1).url,'/change/status/update');
}
// 3 -> 4 works with every cost/lot/date field empty (except the existing recording date).
vm.runInContext("selectedStatusId='3';configureNewStatusOptions();save_change_status()",sandbox);
assert.equal(calls.length,6);
assert.equal(calls.at(-1).body.orderStatus,4);
assert.equal(calls.at(-1).body.l_lot_number,'');
assert.equal(calls.at(-1).body.shippingPrice,'');
console.log('PASS: five transition targets, visible form sections, payloads, bare 3 -> 4');
