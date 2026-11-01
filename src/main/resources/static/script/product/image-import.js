/* Sends only the selected crop to this backend, which selects configured AI or local OCR. */
document.addEventListener('DOMContentLoaded', () => {
    const byId = id => document.getElementById(id);
    const input = byId('setupImageFile');
    if (!input) return;
    let draft = null;
    let busy = false;
    let applied = false;
    let additionalWarnings = [];
    let fileTicket = 0;
    let activeRequest = null;
    let imageReadInfo = null;
    let source = 'image';
    function invalidateResult() {
        draft = null;
        applied = false;
        byId('setupImageResult').hidden = true;
        byId('setupUnknownWebsites').hidden = true;
        byId('setupImageError').hidden = true;
        byId('setupImageProgress').hidden = true;
        status('');
    }
    function selectSource(value) {
        if (busy) return;
        source = value;
        invalidateResult();
        imageReadInfo = null;
        byId('setupImageSource').hidden = value !== 'image';
        byId('setupTextSource').hidden = value !== 'text';
        document.querySelectorAll('[data-import-source]').forEach(button => {
            const selected = button.dataset.importSource === value;
            button.setAttribute('aria-pressed', String(selected));
            button.classList.toggle('btn-primary', selected);
            button.classList.toggle('btn-outline-primary', !selected);
        });
    }
    document.querySelectorAll('[data-import-source]').forEach(button => {
        button.addEventListener('click', () => {
            if (button.dataset.importSource === source) return;
            selectSource(button.dataset.importSource);
        });
    });
    function progress(percent, message) {
        byId('setupImageProgress').hidden = false;
        byId('setupImagePercent').textContent = `${percent}%`;
        byId('setupImageProgressBar').value = percent;
        byId('setupImageProgressStage').textContent = message;
    }
    const status = message => { byId('setupImageStatus').textContent = message; };
    const error = message => {
        byId('setupImageError').textContent = message;
        byId('setupImageError').hidden = false;
    };
    const crop = new ProductImageCrop(byId('setupImageCrop'), (selection, full) => {
        draft = null;
        applied = false;
        imageReadInfo = null;
        byId('setupImageResult').hidden = true;
        byId('setupUnknownWebsites').hidden = true;
        byId('setupImageError').hidden = true;
        byId('setupReadImage').textContent = full ? 'อ่านทั้งภาพ' : 'อ่านเฉพาะกรอบนี้';
        status('');
        // Keep the canvas at the same position throughout a drag after a previous read.
        if (!byId('setupImageProgress').hidden) progress(0, 'กรอบเปลี่ยนแล้ว พร้อมอ่านใหม่');
    });
    function reset() {
        fileTicket++;
        crop.clear();
        draft = null;
        applied = false;
        additionalWarnings = [];
        imageReadInfo = null;
        input.value = '';
        byId('setupImageResult').hidden = true;
        byId('setupUnknownWebsites').hidden = true;
        byId('setupImageError').hidden = true;
        byId('setupOcrText').value = '';
        byId('setupReadImage').disabled = true;
        byId('setupReadImage').textContent = 'อ่านภาพนี้';
        status('');
        byId('setupImageProgress').hidden = true;
        byId('setupTextInput').value = '';
        selectSource('image');
    }
    function warnings(messages) {
        byId('setupImageWarnings').replaceChildren(...messages.map(message => {
            const item = document.createElement('li');
            item.textContent = message;
            return item;
        }));
    }
    function renderUnknownWebsites() {
        const container = byId('setupUnknownWebsites');
        const names = (draft?.versions || []).flatMap(version => version.groups.flatMap(group => group.websites || []));
        const missing = ProductSetup.missingWebsites(names);
        container.replaceChildren();
        container.hidden = !missing.length;
        if (!missing.length) return;
        const heading = document.createElement('p');
        heading.className = 'setup-hint';
        heading.textContent = 'พบชื่อเว็บที่ยังไม่มีในตัวเลือก เพิ่มได้จากที่นี่ หรือแก้ชื่อก่อนเพิ่ม';
        container.append(heading);
        for (const name of missing) {
            const row = document.createElement('div');
            row.className = 'setup-unknown-entry';
            const label = document.createElement('span');
            label.textContent = name;
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-sm btn-outline-primary';
            button.textContent = '＋ เพิ่มเว็บ';
            button.setAttribute('aria-label', `เพิ่มเว็บ ${name}`);
            button.addEventListener('click', () => ProductSetup.openWebsite(name));
            row.append(label, button);
            container.append(row);
        }
    }
    function render(result,fromImage=false) {
        if(fromImage)imageReadInfo={source:result.readSource,message:result.readMessage};
        draft = result;
        applied = false;
        additionalWarnings = [];
        const count = (result.versions || []).reduce((sum, item) => sum + item.groups.length, 0);
        byId('setupImageSummary').textContent = `อ่านได้ ${result.versions.length} เวอร์ชัน · ${count} กลุ่มราคา`;
        const readMessage=result.readSource==='TEXT' && imageReadInfo?.message?`${imageReadInfo.message} · จัดกลุ่มข้อความที่แก้ไขด้วยกฎในระบบ`:result.readMessage;
        const readSource=result.readSource==='TEXT' && imageReadInfo?imageReadInfo.source:result.readSource;
        const method=byId('setupImageReadMethod');method.replaceChildren();method.hidden=!readMessage;
        if(readMessage){method.textContent=readMessage;if(readSource==='OCR'){const link=document.createElement('a');link.href='/settings/ai';link.target='_blank';link.rel='noopener noreferrer';link.textContent=' · ตั้งค่า / เปิดใช้ AI';method.append(link);}}
        byId('setupOcrText').value = result.rawText || '';
        const money = value => value == null || value === '' ? '—' : Number(value).toLocaleString('th-TH', {minimumFractionDigits:2,maximumFractionDigits:2});
        const rows = [];
        for (const version of result.versions || []) {
            for (const offer of version.groups || []) {
                const remaining = offer.price == null || offer.deposit == null ? null : Number(offer.price) - Number(offer.deposit);
                const row = document.createElement('tr');
                for (const value of [(offer.websites?.length ? offer.websites : ['ไม่มีเว็บ']).join(' / '),
                    version.name || 'Standard', offer.name || 'ทั่วไป', money(offer.price), money(offer.deposit), money(remaining)]) {
                    const cell = document.createElement('td'); cell.textContent = value; row.append(cell);
                }
                rows.push(row);
            }
        }
        byId('setupResultRows').replaceChildren(...rows);
        byId('setupResultTableWrap').hidden = !rows.length;
        warnings(result.warnings || []);
        byId('setupApplyImage').disabled = !count;
        byId('setupImageResult').hidden = false;
        renderUnknownWebsites();
    }
    async function request(url, options) {
        if (busy) return;
        const operation = { controller: new AbortController(), id: url.endsWith('/read') ? crypto.randomUUID() : null, stopped: false, timer: null };
        activeRequest = operation;
        busy = true;
        byId('setupStopImage').hidden = false;
        byId('setupStopImage').disabled = false;
        byId('setupStopImage').textContent = operation.id ? 'หยุดอ่านภาพ' : 'หยุดจัดกลุ่ม';
        ProductSetup.setImportBusy(true);
        byId('setupImageError').hidden = true;
        status(operation.id ? 'กำลังอ่านและจัดกลุ่มข้อมูลจากส่วนที่เลือก อาจใช้เวลาสักครู่…' : 'กำลังจัดกลุ่มจากข้อความ…');
        progress(0, operation.id ? 'กำลังเตรียมภาพส่วนที่เลือก' : 'กำลังเตรียมข้อความ');
        const poll = async () => {
            if (activeRequest !== operation || operation.stopped) return;
            try {
                const response = await fetch(`/product/setup/image/progress/${operation.id}`, { signal: operation.controller.signal });
                if (response.ok && response.headers.get('content-type')?.includes('application/json')) {
                    const state = await response.json();
                    if (activeRequest === operation && !operation.stopped) progress(Math.min(99, Math.max(byId('setupImageProgressBar').value, state.percent || 0)), state.message || 'กำลังอ่านภาพ');
                }
            } catch (_) { /* The main request reports errors; a poll can retry. */ }
            if (activeRequest === operation && !operation.stopped) operation.timer = setTimeout(poll, 500);
        };
        try {
            const prepared = typeof options === 'function' ? await options() : options;
            if (operation.stopped) return;
            progress(5, operation.id ? 'กำลังส่งภาพส่วนที่เลือก' : 'กำลังจัดกลุ่มจากข้อความ');
            if (operation.id) operation.timer = setTimeout(poll, 300);
            const response = await fetch(url, { ...prepared, signal: operation.controller.signal,
                headers: { ...(prepared.headers || {}), ...(operation.id ? { 'X-OCR-Request-Id': operation.id } : {}) } });
            if (response.redirected) throw new Error('กรุณาเข้าสู่ระบบในแท็บใหม่ แล้วลองอ่านภาพอีกครั้ง');
            if (!response.headers.get('content-type')?.includes('application/json')) {
                throw new Error(response.status === 413 ? 'ภาพต้องมีขนาดไม่เกิน 8 MB' : 'อ่านข้อมูลไม่ได้ กรุณาลองใหม่');
            }
            const result = await response.json();
            if (operation.stopped) return;
            if (!response.ok) throw new Error(response.status === 413 ? 'ภาพต้องมีขนาดไม่เกิน 8 MB'
                : result.message || 'อ่านภาพไม่ได้ ลองภาพที่ชัดขึ้นหรือครอปเฉพาะส่วนข้อความ');
            render(result,!!operation.id);
            progress(100, result.readSource==='AI'?'AI อ่านและจัดกลุ่มข้อมูลเสร็จแล้ว':result.readSource==='OCR'?'OCR อ่านและจัดกลุ่มข้อมูลเสร็จแล้ว':'จัดกลุ่มข้อความเสร็จแล้ว');
            const hasOffers = (result.versions || []).some(version => version.groups?.length);
            status(hasOffers ? 'จัดกลุ่มเสร็จแล้ว กด “เติมข้อมูลลงฟอร์ม” เพื่อตรวจแก้ก่อนบันทึก'
                : 'ยังไม่พบรายการราคา จึงยังเติมลงฟอร์มไม่ได้ กรุณาตรวจชื่อรายการและราคาในข้อความ');
        } catch (failure) {
            if (!operation.stopped) {
                error(failure.message || 'อ่านภาพไม่ได้ กรุณาลองใหม่');
                status('อ่านไม่สำเร็จ ข้อมูลในฟอร์มเดิมยังอยู่');
                byId('setupImageProgressStage').textContent = 'อ่านไม่สำเร็จ';
            }
        } finally {
            clearTimeout(operation.timer);
            if (!operation.stopped) finishRequest(operation);
        }
    }
    function finishRequest(operation) {
        if (activeRequest !== operation) return;
        activeRequest = null; busy = false;
        byId('setupStopImage').hidden = true;
        ProductSetup.setImportBusy(false);
    }
    byId('setupStopImage').addEventListener('click', async () => {
        const operation = activeRequest;
        if (!operation || operation.cancelling) return;
        operation.cancelling = true;
        operation.stopped = true;
        clearTimeout(operation.timer);
        operation.controller.abort();
        byId('setupStopImage').disabled = true;
        byId('setupStopImage').textContent = 'กำลังหยุด…';
        status(operation.id ? 'กำลังหยุดอ่านภาพ…' : 'กำลังหยุดจัดกลุ่ม…');
        try {
            if (operation.id) {
                const response = await fetch(`/product/setup/image/cancel/${operation.id}`, { method: 'POST' });
                if (!response.ok || response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('สั่งหยุดไม่สำเร็จ กรุณาลองอีกครั้ง');
                let state = await response.json();
                const deadline = Date.now() + 8000;
                while (!state.done && Date.now() < deadline) {
                    await new Promise(resolve => setTimeout(resolve, 250));
                    const next = await fetch(`/product/setup/image/progress/${operation.id}`);
                    if (!next.ok || next.redirected) throw new Error('ตรวจสถานะการหยุดไม่สำเร็จ กรุณาลองอีกครั้ง');
                    state = await next.json();
                }
                if (!state.done) throw new Error('ระบบยังหยุดไม่เสร็จ กรุณาลองหยุดอีกครั้ง');
            }
            byId('setupImageProgressStage').textContent = operation.id ? 'หยุดอ่านภาพแล้ว' : 'หยุดจัดกลุ่มแล้ว';
            byId('setupImageError').hidden = true;
            status(operation.id ? 'หยุดอ่านภาพแล้ว สามารถเลือกกรอบและอ่านใหม่ได้' : 'หยุดจัดกลุ่มแล้ว สามารถแก้ไขข้อความและลองใหม่ได้');
            finishRequest(operation);
        } catch (failure) {
            // Keep the form locked until cancellation can be confirmed, then allow retrying Stop.
            operation.cancelling = false;
            byId('setupStopImage').disabled = false;
            byId('setupStopImage').textContent = 'ลองหยุดอีกครั้ง';
            error(failure.message);
        }
    });
    document.addEventListener('paste', event => {
        const modal = byId('modalManageData');
        if (!modal.classList.contains('show') || busy || source !== 'image' || byId('productSetupFields').disabled || modal.querySelector('dialog[open]')) return;
        const clipboard = event.clipboardData;
        if (!clipboard) return;
        // Keep ordinary text paste working in product fields and the OCR text editor.
        const editable = event.target.closest('input:not([type="file"]), textarea, [contenteditable="true"]');
        if (editable && clipboard.getData('text/plain')) return;
        const item = [...clipboard.items].find(value => value.kind === 'file' && value.type.startsWith('image/'));
        const image = item?.getAsFile();
        if (!image) return;
        event.preventDefault();
        if (!['image/png', 'image/jpeg'].includes(image.type)) { error('รองรับเฉพาะภาพ PNG และ JPEG'); return; }
        if (image.size > 8 * 1024 * 1024) { error('ภาพต้องมีขนาดไม่เกิน 8 MB'); return; }
        const transfer = new DataTransfer();
        transfer.items.add(new File([image], image.name || `clipboard.${image.type === 'image/png' ? 'png' : 'jpg'}`, { type: image.type }));
        input.files = transfer.files;
        input.dispatchEvent(new Event('change', { bubbles: true }));
        byId('setupImagePaste').focus({ preventScroll: true });
    });
    input.addEventListener('change', async () => {
        imageReadInfo=null;
        const file = input.files[0];
        const ticket = ++fileTicket;
        crop.clear();
        draft = null;
        byId('setupImageResult').hidden = true;
        byId('setupUnknownWebsites').hidden = true;
        byId('setupImageError').hidden = true;
        byId('setupReadImage').disabled = true;
        status('');
        byId('setupImageProgress').hidden = true;
        if (!file) return;
        if (!['image/png', 'image/jpeg'].includes(file.type)) { error('รองรับเฉพาะภาพ PNG และ JPEG'); return; }
        if (file.size > 8 * 1024 * 1024) { error('ภาพต้องมีขนาดไม่เกิน 8 MB'); return; }
        status('กำลังเตรียมภาพสำหรับเลือกกรอบ…');
        try {
            const ready = await crop.load(file);
            if (ticket !== fileTicket || !ready) return;
            byId('setupReadImage').disabled = false;
        } catch (failure) {
            if (ticket === fileTicket) { error(failure.message); status('เปิดภาพไม่สำเร็จ กรุณาเลือกภาพใหม่'); }
        }
    });
    byId('setupReadImage').addEventListener('click', () => {
        const file = input.files[0];
        if (!file || busy) return;
        byId('setupImageResult').hidden = true;
        draft = null;
        request('/product/setup/image/read', async () => {
            const body = new FormData();
            // The original file is never included: the server receives only pixels inside the crop.
            body.append('image', await crop.file());
            return { method: 'POST', body };
        });
    });
    byId('setupParseText').addEventListener('click', () => {
        if (!byId('setupOcrText').value.trim()) { error('กรุณาระบุข้อความที่อ่านได้ก่อนจัดกลุ่ม'); return; }
        request('/product/setup/image/parse', { method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: byId('setupOcrText').value }) });
    });
    byId('setupTextInput').addEventListener('input', () => {
        if (source === 'text' && !busy) invalidateResult();
    });
    byId('setupReadText').addEventListener('click', () => {
        if (busy) return;
        const text = byId('setupTextInput').value;
        invalidateResult();
        imageReadInfo = null;
        if (!text.trim()) {
            error('กรุณาวางข้อความก่อนจัดกลุ่ม');
            byId('setupTextInput').focus();
            return;
        }
        request('/product/setup/image/parse', { method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text }) });
    });
    byId('setupApplyImage').addEventListener('click', () => {
        if (!draft || busy || applied) return;
        additionalWarnings = ProductSetup.importDraft(draft);
        warnings([...(draft.warnings || []), ...additionalWarnings]);
        applied = true;
        byId('setupApplyImage').disabled = true;
        status('เติมข้อมูลลงฟอร์มแล้ว ตรวจเวอร์ชัน เว็บ และราคาด้านล่างก่อนกดบันทึกสินค้าทั้งหมด');
        window.ProductSetupSteps?.show(2);
        byId('setupVariantsTitle').scrollIntoView({ block: 'start', behavior: 'auto' });
    });
    byId('modalManageData').addEventListener('setup:reset', reset);
    byId('modalManageData').addEventListener('hidden.bs.modal', reset);
    byId('modalManageData').addEventListener('setup:websites-changed', () => {
        if (!draft) return;
        additionalWarnings = additionalWarnings.filter(message => !message.includes(': ไม่พบเว็บ '));
        warnings([...(draft.warnings || []), ...additionalWarnings]);
        renderUnknownWebsites();
    });
});
