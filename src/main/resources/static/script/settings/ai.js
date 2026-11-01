document.addEventListener('DOMContentLoaded',()=>{
    const byId=id=>document.getElementById(id);
    if(!byId('aiSettingsForm'))return;
    let busy=false, saved=null;
    const status=(message,kind='info')=>{const node=byId('aiSettingsStatus');node.textContent=message;node.className=`alert alert-${kind} mt-3`;node.hidden=false;};
    const lock=value=>{busy=value;byId('aiSettingsFields').disabled=value;byId('aiSettingsForm').setAttribute('aria-busy',String(value));};
    const draft=()=>({provider:byId('aiProvider').value,baseUrl:byId('aiBaseUrl').value.trim(),model:byId('aiModel').value.trim(),apiKey:byId('aiKey').value,removeKey:byId('aiRemoveKey').checked,allowRemote:byId('aiAllowRemote').checked,timeoutSeconds:Number(byId('aiTimeout').value),enabled:byId('aiEnabled').checked});
    async function json(url,body){
        const response=await fetch(url,body===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
        if(response.redirected)throw new Error('เซสชันหมดอายุ กรุณาเข้าสู่ระบบใหม่');
        if(!response.headers.get('content-type')?.includes('application/json'))throw new Error('ไม่สามารถอ่านคำตอบจากเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง');
        const result=await response.json();if(!response.ok)throw new Error(result.message||'ดำเนินการไม่สำเร็จ กรุณาลองอีกครั้ง');return result;
    }
    function keyHint(){
        const changed=saved && (saved.provider!==byId('aiProvider').value || saved.baseUrl!==byId('aiBaseUrl').value.trim().replace(/\/+$/,''));
        const gemini=byId('aiProvider').value==='GEMINI';
        byId('aiGeminiHint').hidden=!gemini;
        byId('aiKey').required=gemini && (!saved?.keyConfigured || changed) && !byId('aiRemoveKey').checked;
        byId('aiKeyHint').textContent=changed?'ปลายทางเปลี่ยนแล้ว หากต้องใช้ API key ให้กรอกใหม่ ระบบจะไม่ส่ง key เดิมไปยังปลายทางใหม่'
            :saved?.keyConfigured?'มี API key ที่บันทึกแล้ว เว้นว่างเพื่อใช้ key เดิม หรือกรอกเพื่อเปลี่ยนใหม่':gemini?'Gemini ต้องใช้ API key จาก Google AI Studio':'ไม่จำเป็นสำหรับบริการในเครื่องที่ไม่ได้เปิดการยืนยันตัวตน';
    }
    function populate(value){
        saved=value;byId('aiProvider').value=value.provider;byId('aiBaseUrl').value=value.baseUrl;byId('aiModel').value=value.model;
        byId('aiKey').value='';byId('aiKey').type='password';byId('aiShowKey').textContent='แสดง';byId('aiShowKey').setAttribute('aria-pressed','false');
        byId('aiKey').disabled=false;byId('aiModelHint').textContent='ใช้ชื่อโมเดลตามที่บริการปลายทางระบุ';
        byId('aiAllowRemote').checked=value.allowRemote;byId('aiTimeout').value=value.timeoutSeconds;
        byId('aiEnabled').checked=!!value.enabled;
        byId('aiRemoveKey').checked=false;byId('aiRemoveKeyRow').hidden=!value.keyConfigured;byId('aiModels').replaceChildren();keyHint();
    }
    async function load(){if(busy)return;lock(true);byId('aiRetry').hidden=true;
        try{populate(await json('/settings/ai/config'));byId('aiSettingsStatus').hidden=true;lock(false);}
        catch(failure){status(failure.message,'danger');busy=false;byId('aiSettingsForm').setAttribute('aria-busy','false');byId('aiRetry').hidden=false;}
    }
    byId('aiSettingsForm').addEventListener('submit',async event=>{
        event.preventDefault();if(busy || !event.target.reportValidity())return;
        const body=draft();lock(true);
        try{populate(await json('/settings/ai/config',body));status('บันทึกการตั้งค่าการเชื่อมต่อ AI แล้ว','success');}
        catch(failure){status(failure.message,'danger');}finally{lock(false);}
    });
    // Submit explicitly from a regular button so the shared guard locks once,
    // while requestSubmit still performs the form's native validation.
    byId('aiSave').addEventListener('click',event=>{event.preventDefault();if(!busy)byId('aiSettingsForm').requestSubmit();});
    byId('aiTest').addEventListener('click',async()=>{
        if(busy)return;const body=draft();
        if(!body.baseUrl || !byId('aiBaseUrl').reportValidity() || !byId('aiTimeout').reportValidity())return;
        if(!byId('aiKey').reportValidity())return;
        lock(true);status('กำลังเชื่อมต่อและอ่านรายชื่อโมเดล…');
        try{
            const result=await json('/settings/ai/test',body);byId('aiModels').replaceChildren(...result.models.map(name=>{const option=document.createElement('option');option.value=name;return option;}));
            status(`${result.message} (${result.models.length} โมเดล) · การทดสอบนี้ยังไม่บันทึกการตั้งค่า`,result.modelFound?'success':'info');
            byId('aiModelHint').textContent=result.models.length?'พิมพ์หรือเลือกชื่อโมเดลจากรายการที่พบ':'ยังไม่พบโมเดล กรุณาเตรียมโมเดลที่ AI Server';
        }catch(failure){status(failure.message,'danger');}finally{lock(false);}
    });
    byId('aiProvider').addEventListener('change',()=>{
        const defaults={OLLAMA:'http://localhost:11434',LM_STUDIO:'http://localhost:1234/v1',GEMINI:'https://generativelanguage.googleapis.com/v1beta',COMPATIBLE:''};
        byId('aiRemoveKey').checked=false;byId('aiKey').disabled=false;
        byId('aiBaseUrl').value=defaults[byId('aiProvider').value];byId('aiModel').value='';byId('aiKey').value='';byId('aiModels').replaceChildren();byId('aiSettingsStatus').hidden=true;keyHint();
    });
    byId('aiBaseUrl').addEventListener('input',()=>{byId('aiModels').replaceChildren();keyHint();});
    byId('aiShowKey').addEventListener('click',()=>{const show=byId('aiKey').type==='password';byId('aiKey').type=show?'text':'password';byId('aiShowKey').textContent=show?'ซ่อน':'แสดง';byId('aiShowKey').setAttribute('aria-pressed',String(show));});
    byId('aiRemoveKey').addEventListener('change',()=>{byId('aiKey').disabled=byId('aiRemoveKey').checked;if(byId('aiRemoveKey').checked){byId('aiKey').value='';byId('aiEnabled').checked=false;}keyHint();});
    byId('aiReload').addEventListener('click',load);byId('aiRetry').addEventListener('click',load);load();
});
