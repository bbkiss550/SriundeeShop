document.addEventListener('DOMContentLoaded', () => {
    const byId = id => document.getElementById(id);
    const modal = byId('orderImportModal');
    if (!modal) return;
    const input=byId('orderImportFile'), error=byId('orderImportError'), selection=byId('orderImportSelection');
    const validateButton=byId('orderImportValidate'), confirmButton=byId('orderImportConfirm');
    let token='', report=null, busy=false, committing=false, request=null, generation=0;
    let mappingToken='', plan=null;
    const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money=value=>Number(value).toLocaleString('th-TH',{minimumFractionDigits:2,maximumFractionDigits:2});
    function setBusy(value) {
        busy=value; modal.setAttribute('aria-busy',String(value));
        validateButton.disabled=value || !input.files.length;
        confirmButton.disabled=value;
        byId('orderImportChooseFile').disabled=value;
        byId('orderImportRemoveFile').disabled=value;
        byId('orderImportMapping').querySelectorAll('select').forEach(select=>select.disabled=value);
        modal.querySelectorAll('[data-bs-dismiss]').forEach(button=>button.disabled=committing);
    }
    function clearResult(clearMapping=false) {
        token='';report=null;confirmButton.hidden=true;
        byId('orderImportDownload').hidden=true;
        byId('orderImportResult').hidden=true;
        byId('orderImportPreviewRows').replaceChildren();
        byId('orderImportIssues').replaceChildren();
        error.hidden=true;error.textContent='';
        if(clearMapping){mappingToken='';plan=null;byId('orderImportMapping').hidden=true;byId('orderImportMappingSheets').replaceChildren();validateButton.textContent='อ่านและตรวจสอบข้อมูล';}
    }
    function reset() {
        generation++;request?.abort();request=null;committing=false;
        input.value='';selection.hidden=true;clearResult(true);setBusy(false);
        byId('orderImportFilename').textContent='';byId('orderImportFilesize').textContent='';
    }
    function showReport(value) {
        report=value;byId('orderImportResult').hidden=false;
        confirmButton.hidden=!value.valid || !token;
        byId('orderImportDownload').hidden=value.issues.length===0;
        byId('orderImportPreview').hidden=!value.valid;
        byId('orderImportIssues').closest('.order-import-report').hidden=value.valid;
        byId('orderImportSummary').textContent=value.valid
            ? `ตรวจครบ ${value.checkedRows} แถว ผ่านทั้งหมด · ยอดคงเหลือรวม ${money(value.items.reduce((sum,item)=>sum+Number(item.balance),0))} บาท · ยังไม่บันทึกข้อมูล`
            : `ปฏิเสธทั้งไฟล์ · ตรวจครบ ${value.checkedRows} แถวคำสั่งซื้อ · พบปัญหา ${value.problemRows} แถว/จุด รวม ${value.issues.length} ข้อ · ยังไม่บันทึกข้อมูล`;
        byId('orderImportIssues').innerHTML=value.issues.map(issue=>`<tr><td>${esc(issue.sheet||'—')}</td><td>${issue.row||'—'}</td><td>${esc(issue.source)}</td><td>${esc(issue.message)}</td></tr>`).join('');
        byId('orderImportPreviewRows').innerHTML=value.items.map(item=>`<tr><td>${esc(item.sheet)} / ${item.row}</td><td>${esc(item.customer)}</td><td>${esc([item.product,item.website,item.version,item.cover].join(' / '))}</td><td>${item.qty}</td><td>${money(item.total)}</td><td>${money(item.paid)}</td><td>${money(item.balance)}</td></tr>`).join('');
    }
    async function json(response) {
        if(response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('เซสชันอาจหมดอายุ กรุณาเข้าสู่ระบบแล้วตรวจไฟล์ใหม่');
        const body=await response.json();
        if(!response.ok) {
            if(body.report) {token='';showReport(body.report);}
            throw new Error(body.message||'ดำเนินการไม่สำเร็จ กรุณาลองใหม่');
        }
        return body;
    }
    function renderMapping(result) {
        plan=result.plan;mappingToken=result.mappingToken;byId('orderImportResult').hidden=true;
        byId('orderImportMapping').hidden=false;byId('orderImportMappingMessage').textContent=plan.message;
        byId('orderImportMappingMessage').className=`alert alert-${plan.source==='FALLBACK'?'warning':'info'}`;
        byId('orderImportMappingSuggestions').innerHTML=plan.suggestions.map(text=>`<li>${esc(text)}</li>`).join('');
        byId('orderImportMappingSheets').innerHTML=plan.layouts.map(layout=>{
            const current=plan.mappings.find(mapping=>mapping.sheetIndex===layout.sheetIndex);
            return `<div class="mb-3" data-mapping-sheet="${layout.sheetIndex}" data-header-row="${layout.headerRow}"><strong>${esc(layout.sheet)} · หัวตารางแถว ${layout.headerRow}</strong><div class="table-responsive order-import-report mt-2"><table class="table"><thead><tr><th>คอลัมน์ Excel</th><th>ชื่อคอลัมน์</th><th>ชนิดตัวอย่าง</th><th>ฟิลด์ในระบบ</th></tr></thead><tbody>${layout.columns.map(column=>`<tr><td>${column.index+1}</td><td>${esc(column.header||'(ไม่มีชื่อ)')}</td><td>${esc(column.sampleType)}</td><td><select class="form-select" data-mapping-column="${column.index}" aria-label="จับคู่ ${esc(layout.sheet)} คอลัมน์ ${column.index+1}"><option value="">ไม่จับคู่</option>${Object.entries(plan.fields).map(([key,label])=>`<option value="${esc(key)}" ${current?.columns[column.index]===key?'selected':''}>${esc(label)}</option>`).join('')}</select></td></tr>`).join('')}</tbody></table></div></div>`;
        }).join('');
        validateButton.textContent='ตรวจข้อมูลตามการจับคู่';
    }
    function approvedMappings(){
        return [...byId('orderImportMappingSheets').querySelectorAll('[data-mapping-sheet]')].map(sheet=>{
            const columns={},used=new Set();sheet.querySelectorAll('[data-mapping-column]').forEach(select=>{
                if(!select.value)return;if(used.has(select.value))throw new Error(`ฟิลด์ ${plan.fields[select.value]} ถูกเลือกซ้ำในชีตเดียวกัน กรุณาแก้การจับคู่`);
                used.add(select.value);columns[select.dataset.mappingColumn]=select.value;
            });
            if(!used.has('customer')||!used.has('product'))throw new Error('กรุณาจับคู่ชื่อลูกค้าและสินค้าให้ครบทุกชีต');
            return {sheetIndex:Number(sheet.dataset.mappingSheet),headerRow:Number(sheet.dataset.headerRow),columns};
        });
    }
    async function validate() {
        const file=input.files[0];if(!file || busy)return;
        clearResult();const ticket=++generation;request=new AbortController();setBusy(true);
        byId('orderImportResult').hidden=false;
        byId('orderImportSummary').textContent=mappingToken?'กำลังตรวจสอบข้อมูลครบทุกแถวตามการจับคู่ที่เลือก…':'กำลังอ่านโครงสร้างและเสนอการจับคู่คอลัมน์…';
        byId('orderImportPreview').hidden=true;
        byId('orderImportIssues').closest('.order-import-report').hidden=true;
        try {
            let response;
            if(mappingToken)response=await fetch('/orders/import/validate-mapping',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token:mappingToken,mappings:approvedMappings()}),signal:request.signal});
            else {const body=new FormData();body.append('file',file);response=await fetch('/orders/import/inspect',{method:'POST',body,signal:request.signal});}
            const result=await json(response);
            if(ticket!==generation)return;
            if(result.plan)renderMapping(result);else{token=result.token;showReport(result.report);}
        } catch(failure) {
            if(ticket!==generation || failure.name==='AbortError')return;
            byId('orderImportSummary').textContent='อ่านหรือตรวจสอบไม่สำเร็จ ยังไม่บันทึกข้อมูล';
            error.textContent=failure.message;error.hidden=false;
        } finally {if(ticket===generation){request=null;setBusy(false);}}
    }
    byId('orderImportChooseFile').addEventListener('click',()=>input.click());
    byId('orderImportRemoveFile').addEventListener('click',()=>{reset();byId('orderImportChooseFile').focus();});
    input.addEventListener('change',()=>{
        clearResult(true);selection.hidden=true;const file=input.files[0];
        if(!file){setBusy(false);return;}
        const message=!/\.xlsx$/i.test(file.name)?'กรุณาเลือกไฟล์ Excel นามสกุล .xlsx'
            :!file.size?'ไฟล์ว่าง กรุณาเลือกไฟล์ที่มีข้อมูล':file.size>8*1024*1024?'ไฟล์ต้องมีขนาดไม่เกิน 8 MB':'';
        if(message){input.value='';error.textContent=message;error.hidden=false;setBusy(false);return;}
        byId('orderImportFilename').textContent=file.name;
        byId('orderImportFilesize').textContent=`${(file.size/1024).toLocaleString('th-TH',{maximumFractionDigits:1})} KB`;
        selection.hidden=false;setBusy(false);validate();
    });
    validateButton.addEventListener('click',validate);
    byId('orderImportMappingSheets').addEventListener('change',()=>{clearResult();});
    confirmButton.addEventListener('click',async()=>{
        if(!token || busy || !report?.valid)return;
        const reviewedToken=token;
        const result=await Swal.fire({title:'ยืนยันนำเข้าทั้งไฟล์?',text:`สร้างคำสั่งซื้อ ${report.items.length} รายการ หลังจากตรวจซ้ำกับข้อมูลล่าสุด`,icon:'question',showCancelButton:true,confirmButtonText:'ยืนยันนำเข้า',cancelButtonText:'ยกเลิก'});
        if(!result.isConfirmed || token!==reviewedToken || busy)return;
        committing=true;setBusy(true);error.hidden=true;
        try {
            const response=await json(await fetch('/orders/import/confirm',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({token})}));
            token='';mappingToken='';confirmButton.hidden=true;input.value='';selection.hidden=true;byId('orderImportMapping').hidden=true;
            byId('orderImportSummary').textContent=`นำเข้าสำเร็จ ${response.orderCount} คำสั่งซื้อ · ยอดคงเหลือรวม ${money(response.balance)} บาท`;
            if(typeof load_order_list_data==='function')load_order_list_data();
        } catch(failure) {
            token='';confirmButton.hidden=true;error.textContent=failure.message+' กรุณาตรวจไฟล์ใหม่ก่อนลองนำเข้าอีกครั้ง';error.hidden=false;
        } finally {committing=false;setBusy(false);}
    });
    byId('orderImportDownload').addEventListener('click',()=>{
        if(!report)return;
        const cell=value=>`"${String(value??'').replace(/^[\s]*[=+@-]/,"'$&").replace(/"/g,'""')}"`;
        const rows=[['ชีต','แถว Excel','ข้อมูลต้นฉบับ','ปัญหาที่พบ'],...report.issues.map(i=>[i.sheet,i.row,i.source,i.message])];
        const url=URL.createObjectURL(new Blob(['\uFEFF'+rows.map(row=>row.map(cell).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8'}));
        const link=document.createElement('a');link.href=url;link.download='order-import-errors.csv';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    });
    modal.addEventListener('hide.bs.modal',event=>{if(committing)event.preventDefault();});
    modal.addEventListener('hidden.bs.modal',reset);
});
