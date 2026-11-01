/* Product setup keeps edits locally until the single transactional save. */
const ProductSetup = (() => {
    let websites = [];
    let variants = [];
    let loading = false;
    let saving = false;
    let importing = false;
    let addingWebsite = false;
    let addingArtist = false;
    let addingType = false;
    let websiteSource = '';
    let websiteTargetGroup = null;
    const websiteAliases = new Map();
    let dirty = false;
    let requestToken = 0;
    let sequence = 0;
    let rowRenderTimer = null;
    let deleteConfirmOpen = false;
    let websitePanel = null;
    let pricingQuery = '';
    let pricingSearchTimer;
    const searchFoldStates = new Map();
    let pricingToastTimer;
    function focusWebsiteManager(bucket) {
        byId('setupVersions').querySelector(`.pricing-group[data-bucket="${bucket}"] > summary [data-setup-action="manage-sites"]`)?.focus({preventScroll:true});
    }

    function applyPricingSearch() {
        const query = pricingQuery.trim().toLowerCase();
        let count = 0;
        byId('setupVersions').querySelectorAll('.pricing-group').forEach(card => {
            const siteMatch = query && card.dataset.searchSites.includes(query);
            let match = !query || siteMatch;
            card.querySelectorAll('.pricing-version').forEach(version => {
                const versionMatch = !query || siteMatch || version.dataset.searchVersion.includes(query);
                let found = versionMatch;
                version.querySelectorAll('[data-search-cover]').forEach(row => {
                    row.hidden = !(versionMatch || row.dataset.searchCover.includes(query));
                    if (!row.hidden) found = true;
                });
                version.hidden = !found;
                match ||= found;
            });
            card.hidden = !match;
            if (match) count++;
        });
        if (query) {
            const target = byId('setupVersions').querySelector('.pricing-group:not([hidden]) .pricing-version:not([hidden]) tr:not([hidden]) .pricing-cover');
            if (target) PricingAccordion.reveal(target, {animate:false});
        } else if (searchFoldStates.size) { PricingAccordion.restore(byId('setupVersions'), searchFoldStates); searchFoldStates.clear(); }
        if (byId('pricingSearchEmpty')) byId('pricingSearchEmpty').hidden = count > 0;
    }

    function pricingToast(message, error = false) {
        clearTimeout(pricingToastTimer);
        const toast = byId('pricingToast');
        if (toast) { toast.textContent = message; toast.classList.toggle('pricing-toast-error', error); }
        if (!error) pricingToastTimer = setTimeout(() => { if (byId('pricingToast')) byId('pricingToast').textContent = ''; }, 4500);
    }
    const byId = id => document.getElementById(id);
    const normalizedName = value => String(value || '').normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{M}\p{N}]/gu, '');
    const findWebsite = name => websites.find(site => site.delete === 'A' && normalizedName(site.name) === normalizedName(name))
        || websites.find(site => site.delete === 'A' && site.id === websiteAliases.get(normalizedName(name)));
    const money = value => Number(value || 0).toLocaleString('th-TH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const displayAmount = value => value === '' || value == null ? '—' : money(value);
    const balance = offer => offer.price === '' || offer.price == null || offer.deposit === '' || offer.deposit == null ? '—' : money(Number(offer.price) - Number(offer.deposit));
    const escape = value => String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
    const group = (data = {}) => ({ key: ++sequence, name: data.name || 'ทั่วไป', websiteIds: [...(data.websiteIds || [])],
        price: data.price ?? '', deposit: data.deposit ?? 0, coverIds: { ...(data.coverIds || {}) }, unmatchedWebsites: [...(data.unmatchedWebsites || [])] });
    const variant = (data = {}) => ({ key: ++sequence, id: data.id ?? null, name: data.name ?? 'Standard',
        groups: data.groups?.length ? data.groups.map(group) : [group()] });

    async function json(url, options) {
        const response = await fetch(url, options);
        if (response.redirected || !response.headers.get('content-type')?.includes('application/json')) {
            throw new Error('เซสชันอาจหมดอายุ กรุณาเข้าสู่ระบบในแท็บใหม่แล้วลองอีกครั้ง ข้อมูลที่กรอกยังอยู่');
        }
        const data = await response.json();
        if (!response.ok) {
            const error = new Error(data.message || 'ไม่สามารถโหลดหรือบันทึกข้อมูลได้ กรุณาลองใหม่');
            error.field = data.field;
            throw error;
        }
        return data;
    }

    function clearErrors() {
        byId('productSetupError').hidden = true;
        byId('productSetupForm').querySelectorAll('.setup-field-error').forEach(node => node.remove());
        byId('productSetupForm').querySelectorAll('[aria-invalid]').forEach(node => {
            node.removeAttribute('aria-invalid');
            node.removeAttribute('aria-describedby');
        });
    }

    function fieldError(field, message) {
        const target = field === 'product.artist' ? byId('setupArtistChoice') : [...byId('productSetupForm').querySelectorAll('[data-field]')].find(node => node.dataset.field === field);
        if (!target) return;
        target.setAttribute('aria-invalid', 'true');
        const note = document.createElement('p');
        note.className = 'setup-field-error';
        note.id = `setup-error-${++sequence}`;
        note.textContent = message;
        target.setAttribute('aria-describedby', note.id);
        target.after(note);
    }

    function clearFieldError(target) {
        if (!target || target.getAttribute('aria-invalid') !== 'true') return;
        byId(target.getAttribute('aria-describedby'))?.remove();
        target.removeAttribute('aria-invalid');
        target.removeAttribute('aria-describedby');
        if (!byId('productSetupForm').querySelector('[aria-invalid="true"]')) byId('productSetupError').hidden = true;
    }

    function showError(message) {
        pricingToast(message, true);
        const invalidPanel = byId('productSetupForm').querySelector('[aria-invalid="true"]')?.closest('[role="tabpanel"]');
        if (invalidPanel) window.ProductSetupSteps?.show(Number(invalidPanel.id.slice(-1)));
        const firstInvalid = byId('setupVersions').querySelector('[aria-invalid="true"]');
        [firstInvalid].filter(Boolean).forEach(node => {
            for (let parent = node.parentElement; parent && parent !== byId('setupVersions'); parent = parent.parentElement) {
                parent.hidden = false;
            }
            node.closest('tr')?.removeAttribute('hidden');
            PricingAccordion.reveal(node, {animate:false});
        });
        const error = byId('productSetupError');
        error.textContent = message;
        error.hidden = false;
        error.focus();
    }

    function setBusy() {
        byId('productSetupFields').disabled = loading || saving || importing || addingWebsite || addingArtist || addingType;
        byId('setupSaveButton').disabled = loading || saving || importing || addingWebsite || addingArtist || addingType;
        byId('setupSaveButton').textContent = saving ? 'กำลังบันทึก…' : 'บันทึกสินค้าทั้งหมด';
        byId('productSetupLoading').hidden = !loading;
        byId('productSetupForm').setAttribute('aria-busy', String(loading || saving || importing || addingWebsite || addingArtist || addingType));
    }

    function groupHtml(item, version, vi, gi) {
        const path = `versions.${vi}.groups.${gi}`;
        const prefix = `setup-group-${item.key}`;
        const label = `แถว ${vi + 1}.${gi + 1}`;
        return `<tr class="setup-price-group" data-version-key="${version.key}" data-group-key="${item.key}">
            <td data-label="เว็บ"><div class="setup-site-list setup-search-picker" role="group" aria-label="เว็บ ${label}" data-field="${path}.websites">
                <div class="setup-inline-delete"><details class="setup-search-dropdown"><summary class="form-select" aria-label="เลือกเว็บ ${label}">เลือกเว็บ</summary>
                    <div class="setup-dropdown-panel"><input type="search" class="form-control" data-picker-search placeholder="ค้นหาเว็บไซต์…" aria-label="ค้นหาเว็บไซต์ ${label}" autocomplete="off">
                    <div class="setup-dropdown-options">${websites.map(site => {
                        const inactive = site.delete !== 'A';
                        const allowed = !inactive || Boolean(item.coverIds[site.id]);
                        return `<label class="setup-site"><input type="checkbox" data-website-id="${site.id}" ${item.websiteIds.includes(site.id) ? 'checked' : ''} ${allowed ? '' : 'disabled'}><span>${escape(site.name)}${inactive ? ' (หยุดใช้งาน)' : ''}</span></label>`;
                    }).join('')}</div><p class="setup-hint setup-search-empty" hidden>ไม่พบเว็บไซต์ที่ค้นหา</p>
                    <button type="button" class="setup-text-button setup-add-website" data-setup-action="add-website">＋ เพิ่มเว็บใหม่</button></div>
                </details><button type="button" class="setup-delete-x" data-setup-action="remove-bucket" aria-label="ลบกลุ่มเว็บ ${label}" title="ลบกลุ่มเว็บ">×</button></div><div class="setup-selected-sites" aria-live="polite"></div></div>
                ${item.unmatchedWebsites.length ? `<div class="setup-unmatched">${item.unmatchedWebsites.map(name => `<div class="setup-unmatched-entry"><span>${escape(name)}</span><button type="button" class="btn btn-sm btn-primary" data-setup-action="add-website" data-website-name="${escape(name)}">＋ เพิ่มเว็บ</button></div>`).join('')}</div>` : ''}
            </td>
            <td data-label="เวอ"><div class="setup-inline-delete"><input class="form-control" id="${prefix}-version" value="${escape(version.name)}" maxlength="255" list="setupVersionNames" autocomplete="off" data-row-version data-field="versions.${vi}.name" aria-label="เวอ ${label}"><button type="button" class="setup-delete-x" data-setup-action="remove-nested-version" aria-label="ลบเวอร์ชัน ${label}" title="ลบเวอร์ชัน">×</button></div></td>
            <td data-label="ปก"><div class="setup-inline-delete"><input class="form-control" id="${prefix}-name" value="${escape(item.name)}" maxlength="255" placeholder="ทั่วไป / เดี่ยว / SET" data-prop="name" data-field="${path}.name" aria-label="ปก ${label}"><button type="button" class="setup-delete-x" data-setup-action="remove-row" aria-label="ลบปก ${label}" title="ลบปก">×</button></div></td>
            <td data-label="ราคาเต็ม"><input class="form-control" id="${prefix}-price" type="number" inputmode="decimal" min="0.01" max="999999999.99" step="0.01" value="${escape(item.price)}" placeholder="0.00" data-prop="price" data-field="${path}.price" aria-label="ราคาเต็ม ${label}"></td>
            <td data-label="มัดจำ"><input class="form-control" id="${prefix}-deposit" type="number" inputmode="decimal" min="0" max="999999999.99" step="0.01" value="${escape(item.deposit)}" data-prop="deposit" data-field="${path}.deposit" aria-label="มัดจำ ${label}"></td>
            <td data-label="คงเหลือ"><output class="setup-row-balance" data-balance aria-label="คงเหลือ ${label}">${balance(item)}</output></td>
            <td data-label="คัดลอกปก"><button type="button" class="setup-copy-button" data-setup-action="copy-row" aria-label="คัดลอกปก ${escape(item.name)}" title="คัดลอกปกนี้"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4"/></svg></button></td>
        </tr>`;
    }

    function render() {
        clearTimeout(rowRenderTimer);
        rowRenderTimer = null;
        const buckets = new Map();
        variants.forEach((version, vi) => version.groups.forEach((offer, gi) => {
            const signature = offer.websiteIds.length || offer.unmatchedWebsites.length
                ? JSON.stringify([ [...offer.websiteIds].sort((a,b) => a-b), [...offer.unmatchedWebsites].sort() ])
                : `empty-${offer.bucket || offer.key}`;
            if (!buckets.has(signature)) buckets.set(signature, []);
            buckets.get(signature).push({ version, vi, offer, gi });
        }));
        const states = new Map([...byId('setupVersions').querySelectorAll('[data-fold]')].map(node => [node.dataset.fold, PricingAccordion.expanded(node)]));
        byId('setupVersions').innerHTML = ProductPricingView.build(buckets, websites, groupHtml, websitePanel, states);
        PricingAccordion.mount(byId('setupVersions'));
        byId('pricingSearch').value = pricingQuery;
        applyPricingSearch();
        updateSummary();
        const websiteModal = byId('pricingWebsiteModal');
        if (websiteModal) {
            websiteModal.addEventListener('cancel', event => {
                event.preventDefault();
                if (!addingWebsite) { const bucket = websitePanel?.bucket; websitePanel = null; render(); focusWebsiteManager(bucket); }
            });
            websiteModal.showModal();
        }
    }

    function changeRowVersion(target) {
        const { currentVersion, currentGroup } = context(target);
        if (!currentVersion || !currentGroup) return;
        const name = target.value.trim();
        clearErrors();
        if (name === currentVersion.name) return;
        const existing = variants.find(item => item !== currentVersion && item.name.trim().toLowerCase() === name.toLowerCase());
        const keys = target.closest('[data-version-members]')?.dataset.versionMembers.split(',').map(Number) || [currentGroup.key];
        const moving = currentVersion.groups.filter(item => keys.includes(item.key));
        if (!existing && currentVersion.groups.length === moving.length) currentVersion.name = name;
        else {
            const destination = existing || variant({ name });
            if (!existing) { destination.groups = []; variants.push(destination); }
            currentVersion.groups = currentVersion.groups.filter(item => !keys.includes(item.key));
            // A persisted cover belongs to its original version. Moving retires that offer and creates another.
            moving.forEach(item => { item.coverIds = {}; destination.groups.push(item); });
            variants = variants.filter(item => item.groups.length);
        }
        dirty = true;
        // Let a click/Tab finish moving focus before replacing rows after a version move.
        clearTimeout(rowRenderTimer);
        rowRenderTimer = setTimeout(() => {
            const focused = document.activeElement;
            const id = focused?.id;
            const start = focused?.selectionStart;
            const end = focused?.selectionEnd;
            render();
            const replacement = id && byId(id);
            if (replacement && focused !== replacement) {
                replacement.focus();
                if (typeof start === 'number') replacement.setSelectionRange(start, end);
            }
        }, 0);
    }

    function syncSitePicker(picker) {
        const checked = [...picker.querySelectorAll('[data-website-id]:checked')];
        picker.querySelector('.setup-selected-sites').innerHTML = checked.map(input => {
            const name = input.closest('label').querySelector('span').textContent;
            return `<span class="setup-site-chip"><span>${escape(name)}</span><button type="button" data-remove-site="${input.dataset.websiteId}" aria-label="นำ ${escape(name)} ออก">×</button></span>`;
        }).join('');
        picker.querySelector('summary').textContent = checked.length ? 'เปลี่ยนเว็บ' : 'เลือกเว็บ';
    }

    function syncArtistPicker() {
        const select = byId('IdArtist');
        byId('setupArtistChoice').textContent = select.selectedOptions[0]?.value ? select.selectedOptions[0].textContent : 'เลือกศิลปิน';
        byId('setupArtistOptions').innerHTML = [...select.options].filter(option => option.value).map(option =>
            `<button type="button" class="setup-dropdown-option" data-artist-id="${escape(option.value)}" aria-pressed="${option.selected}">${escape(option.textContent)}${option.selected ? ' ✓' : ''}</button>`).join('');
        filterPicker(byId('setupArtistSearch'));
    }

    function filterPicker(input) {
        const panel = input.closest('.setup-dropdown-panel');
        const query = input.value.normalize('NFKC').toLocaleLowerCase().trim();
        let visible = 0;
        panel.querySelectorAll('.setup-site, .setup-dropdown-option').forEach(option => {
            option.hidden = !option.textContent.normalize('NFKC').toLocaleLowerCase().includes(query);
            if (!option.hidden) visible++;
        });
        panel.querySelector('.setup-search-empty').hidden = visible > 0;
    }

    function updateSummary() {
        let count = 0;
        variants.forEach(item => item.groups.forEach(offer => {
            count += offer.websiteIds.length;
        }));
        const rowCount = variants.reduce((sum, item) => sum + item.groups.length, 0);
        byId('setupSaveSummary').textContent = `${variants.length} เวอร์ชัน · ${rowCount} แถว · ${count} รายการขาย`;
        byId('setupReviewCount').textContent = `(${count} รายการ)`;
    }

    function context(target) {
        const versionNode = target.closest('[data-version-key]');
        let currentVersion = variants.find(value => value.key === Number(versionNode?.dataset.versionKey));
        const groupNode = target.closest('[data-group-key]');
        const key = Number(groupNode?.dataset.groupKey);
        if (!currentVersion?.groups.some(value => value.key === key)) currentVersion = variants.find(version => version.groups.some(value => value.key === key));
        return { currentVersion, currentGroup: currentVersion?.groups.find(value => value.key === key), groupNode };
    }

    function update(event) {
        const target = event.target;
        if (target.id === 'pricingSearch') {
            if (!pricingQuery.trim() && target.value.trim()) byId('setupVersions').querySelectorAll('[data-fold]').forEach(node => searchFoldStates.set(node.dataset.fold, PricingAccordion.expanded(node)));
            pricingQuery = target.value;
            clearTimeout(pricingSearchTimer); pricingSearchTimer = setTimeout(applyPricingSearch, 180); return;
        }
        if (target.id === 'pricingSiteSearch') {
            target.closest('#pricingWebsiteModal').querySelectorAll('[data-site-name]').forEach(node => { node.hidden = !node.dataset.siteName.includes(target.value.trim().toLowerCase()); }); return;
        }
        if (target.hasAttribute('data-panel-site')) {
            const id = Number(target.dataset.panelSite);
            const dummy = websites.find(site => normalizedName(site.name) === normalizedName('ไม่มีเว็บ'))?.id;
            websitePanel.ids = target.checked ? id === dummy ? [id] : [...websitePanel.ids.filter(value => value !== dummy && value !== id), id] : websitePanel.ids.filter(value => value !== id);
            if (websitePanel.ids.length && byId('pricingWebsiteError')) byId('pricingWebsiteError').hidden = true;
            target.closest('#pricingWebsiteModal').querySelectorAll('[data-panel-site]').forEach(input => { input.checked = websitePanel.ids.includes(Number(input.dataset.panelSite)); }); return;
        }
        if (target.hasAttribute('data-picker-search')) { filterPicker(target); return; }
        if (target.hasAttribute('data-ocr-control')) return;
        if (target.hasAttribute('data-row-version')) { dirty = true; return; }
        const { currentVersion, currentGroup, groupNode } = context(target);
        clearFieldError(target.hasAttribute('data-website-id') ? groupNode.querySelector('.setup-site-list') : target);
        if (target.id === 'IdArtist') { clearFieldError(byId('setupArtistChoice')); syncArtistPicker(); }
        if (target.id === 'end_date_unscheduled') clearFieldError(byId('end_date'));
        if (target.id === 'send_date_unscheduled') clearFieldError(byId('send_date'));
        if (target.hasAttribute('data-prop')) currentGroup[target.dataset.prop] = target.value;
        if (target.dataset.prop === 'name') groupNode.dataset.searchCover = target.value.toLowerCase();
        if (currentGroup) {
            const title = groupNode.querySelector('[data-cover-title]');
            if (title) title.textContent = currentGroup.name || 'ยังไม่มีชื่อปก';
            if (groupNode.querySelector('[data-cover-price]')) groupNode.querySelector('[data-cover-price]').textContent = displayAmount(currentGroup.price);
            if (groupNode.querySelector('[data-cover-deposit]')) groupNode.querySelector('[data-cover-deposit]').textContent = displayAmount(currentGroup.deposit);
        }
        if (target.hasAttribute('data-website-id')) {
            const id = Number(target.dataset.websiteId);
            const dummy = websites.find(site => normalizedName(site.name) === normalizedName('ไม่มีเว็บ'))?.id;
            currentGroup.websiteIds = target.checked ? id === dummy ? [id] : [...currentGroup.websiteIds.filter(value => value !== dummy && value !== id), id]
                : currentGroup.websiteIds.filter(value => value !== id);
            const members = target.closest('[data-bucket-members]').dataset.bucketMembers.split(',').map(Number);
            variants.forEach(version => version.groups.forEach(offer => { if (members.includes(offer.key)) offer.websiteIds = [...currentGroup.websiteIds]; }));
            groupNode.querySelectorAll('[data-website-id]').forEach(input => { input.checked = currentGroup.websiteIds.includes(Number(input.dataset.websiteId)); });
            syncSitePicker(target.closest('.setup-site-list'));
        }
        if (currentGroup) {
            groupNode.querySelector('[data-balance]').textContent = balance(currentGroup);
            if (['price', 'deposit'].includes(target.dataset.prop)) {
                const deposit = groupNode.querySelector('[data-prop="deposit"]');
                clearFieldError(deposit);
                if (Number(currentGroup.deposit) > Number(currentGroup.price)) fieldError(deposit.dataset.field, 'มัดจำต้องไม่เกินราคาเต็ม');
            }
        }
        dirty = true;
        updateSummary();
    }

    async function confirmRemoval(text) {
        if (deleteConfirmOpen) return false;
        deleteConfirmOpen = true;
        try {
            const result = await Swal.fire({
                target: byId('modalManageData'), title: 'ยืนยันการลบ?', text,
                icon: 'warning', showCancelButton: true, confirmButtonText: 'ลบ', cancelButtonText: 'ยกเลิก',
                confirmButtonColor: '#dc3545', cancelButtonColor: '#6c757d',
                focusCancel: true, reverseButtons: true, keydownListenerCapture: true
            });
            return result.isConfirmed;
        } finally { deleteConfirmOpen = false; }
    }

    async function action(event) {
        const button = event.target.closest('[data-setup-action]');
        if (!button || loading || saving || importing || addingWebsite || addingArtist || addingType) return;
        const { currentVersion, currentGroup } = context(button);
        const type = button.dataset.setupAction;
        if (type === 'open-first' || type === 'collapse-all') {
            PricingAccordion.collapseAll(byId('setupVersions'));
            if (type === 'open-first') { const first = byId('setupVersions').querySelector('.pricing-group'); if (first) PricingAccordion.open(first); } return;
        }
        if (type === 'manage-sites') { websitePanel = { bucket: currentGroup.bucket, ids: [...currentGroup.websiteIds] }; render(); byId('pricingSiteSearch')?.focus(); return; }
        if (type === 'cancel-sites') { const bucket = websitePanel.bucket; websitePanel = null; render(); focusWebsiteManager(bucket); return; }
        if (type === 'new-panel-site') { openWebsite(); return; }
        if (type === 'save-sites') {
            if (!websitePanel.ids.length) {
                let note = byId('pricingWebsiteError');
                if (!note) { note = document.createElement('p'); note.id = 'pricingWebsiteError'; note.setAttribute('role','alert'); note.className = 'pricing-warning'; byId('pricingWebsiteModal').append(note); }
                note.hidden = false; note.textContent = 'เลือกอย่างน้อย 1 เว็บไซต์ หรือเลือก ไม่มีเว็บ'; return;
            }
            variants.forEach(version => version.groups.forEach(offer => { if (offer.bucket === websitePanel.bucket) offer.websiteIds = [...websitePanel.ids]; }));
            const bucket = websitePanel.bucket;
            websitePanel = null; dirty = true; clearErrors(); render(); focusWebsiteManager(bucket); pricingToast('อัปเดตเว็บไซต์ในกลุ่มแล้ว · ยังไม่บันทึกสินค้า'); return;
        }
        if (type === 'remove-tag') {
            const id = Number(button.dataset.site);
            if (!await confirmRemoval(`นำเว็บ “${websites.find(site => site.id === id)?.name || ''}” ออกจากกลุ่มนี้?`)) return;
            variants.forEach(version => version.groups.forEach(offer => { if (offer.bucket === currentGroup.bucket) offer.websiteIds = offer.websiteIds.filter(value => value !== id); }));
            if (websitePanel?.bucket === currentGroup.bucket) websitePanel.ids = websitePanel.ids.filter(value => value !== id);
            dirty = true; render(); pricingToast('นำเว็บไซต์ออกจากกลุ่มแล้ว'); return;
        }
        if (type === 'copy-version') {
            const base = `${currentVersion.name.slice(0, 235)} (สำเนา)`;
            let name = base, n = 2;
            while (variants.some(version => version.name === name)) name = `${base} ${n++}`;
            const copied = variant({ name });
            copied.groups = currentVersion.groups.filter(offer => offer.bucket === currentGroup.bucket).map(offer => {
                const next = group({ name: offer.name, websiteIds: offer.websiteIds, price: offer.price, deposit: offer.deposit, unmatchedWebsites: offer.unmatchedWebsites }); next.bucket = currentGroup.bucket; return next;
            });
            variants.push(copied); dirty = true; render();
            const input = byId(`setup-group-${copied.groups[0].key}-version`);
            if (input) { await PricingAccordion.reveal(input); input.focus({preventScroll:true}); input.select(); }
            pricingToast('คัดลอกเวอร์ชันแล้ว'); return;
        }
        if (type.startsWith('remove-')) {
            const text = type === 'remove-bucket' ? 'ลบกลุ่มเว็บนี้ รวมทุกเวอร์ชันและปกภายในกลุ่ม?'
                : type === 'remove-nested-version' ? `ลบเวอร์ชัน “${currentVersion.name}” และทุกปกในกลุ่มเว็บนี้?`
                : `ลบปก “${currentGroup.name}” ออกจากเวอร์ชันนี้?`;
            const token = requestToken;
            if (!await confirmRemoval(text) || token !== requestToken || loading || saving || importing || addingWebsite || addingArtist || addingType) return;
        }
        if (type === 'add-website') { openWebsite(button.dataset.websiteName || '', currentGroup?.key); return; }
        let focusKey;
        if (type === 'add-nested-version' || type === 'add-cover') {
            const next = group({ websiteIds: currentGroup.websiteIds, unmatchedWebsites: currentGroup.unmatchedWebsites });
            next.bucket = currentGroup.bucket;
            if (type === 'add-cover') currentVersion.groups.push(next);
            else { const added = variant({ name: '' }); added.groups = [next]; variants.push(added); }
            focusKey = `setup-group-${next.key}-${type === 'add-cover' ? 'name' : 'version'}`;
        } else if (type === 'remove-bucket' || type === 'remove-nested-version') {
            const candidates = type === 'remove-bucket' ? variants.flatMap(version => version.groups) : currentVersion.groups;
            const keys = candidates.filter(offer => offer.bucket === currentGroup.bucket).map(offer => offer.key);
            variants.forEach(version => { version.groups = version.groups.filter(offer => !keys.includes(offer.key)); });
            variants = variants.filter(version => version.groups.length);
        } else if (type === 'add-row') {
            let target = variants.at(-1);
            if (!target) { target = variant(); variants.push(target); }
            else target.groups.push(group());
            focusKey = `setup-group-${target.groups.at(-1).key}-version`;
        } else if (type === 'copy-row') {
            const next = group({ name: currentGroup.name, websiteIds: currentGroup.websiteIds, price: currentGroup.price, deposit: currentGroup.deposit });
            next.bucket = currentGroup.bucket;
            currentVersion.groups.splice(currentVersion.groups.indexOf(currentGroup) + 1, 0, next);
            focusKey = `setup-group-${next.key}-name`;
        } else if (type === 'remove-row') {
            currentVersion.groups = currentVersion.groups.filter(value => value !== currentGroup);
            variants = variants.filter(value => value.groups.length);
        }
        clearErrors();
        dirty = true;
        if (websitePanel && !variants.some(version => version.groups.some(offer => offer.bucket === websitePanel.bucket))) websitePanel = null;
        render();
        pricingToast(type.startsWith('remove-') ? 'ลบข้อมูลจากร่างแล้ว' : type === 'copy-row' ? 'คัดลอกปกแล้ว' : 'เพิ่มรายการแล้ว');
        if (focusKey && byId(focusKey)) { await PricingAccordion.reveal(byId(focusKey)); byId(focusKey)?.focus({preventScroll:true}); byId(focusKey)?.select(); }
    }

    function productInput() {
        return { id: Number(byId('IdProduct').value) || null, name: byId('pName').value.trim(),
            type: Number(byId('IdType').value) || null, artist: Number(byId('IdArtist').value) || null,
            product_status: Number(byId('IdProductStatus').value) || null,
            end_date: byId('end_date_unscheduled').checked ? null : byId('end_date').value,
            send_date: byId('send_date_unscheduled').checked ? null : byId('send_date').value,
            pic: byId('pPic').value.trim() };
    }

    function validate(product) {
        const errors = [];
        const add = (field, message) => errors.push({ field, message });
        if (!product.name) add('product.name', 'กรุณากรอกชื่อสินค้า');
        if (!product.type) add('product.type', 'กรุณาเลือกประเภทสินค้า');
        if (!product.artist) add('product.artist', 'กรุณาเลือกศิลปิน');
        if (!product.product_status) add('product.product_status', 'กรุณาเลือกสถานะสินค้า');
        if (!product.end_date && !byId('end_date_unscheduled').checked) add('product.end_date', 'เลือกวันที่ปิดรับ หรือเลือกไม่มีกำหนด');
        if (!product.send_date && !byId('send_date_unscheduled').checked) add('product.send_date', 'เลือกวันที่กำหนดส่ง หรือเลือกไม่มีกำหนด');
        if (!variants.length) add('versions', 'เพิ่มอย่างน้อย 1 เวอร์ชัน');
        const names = new Set();
        const validMoney = (value, allowZero) => value !== '' && Number.isFinite(Number(value)) && Number(value) <= 999999999.99
            && (allowZero ? Number(value) >= 0 : Number(value) > 0) && /^\d+(\.\d{1,2})?$/.test(String(value));
        variants.forEach((item, vi) => {
            const path = `versions.${vi}`;
            const name = item.name.trim().toLowerCase();
            if (!name) add(`${path}.name`, 'กรอกชื่อเวอร์ชัน หรือใช้ชื่อ Standard');
            else if (names.has(name)) add(`${path}.name`, 'ชื่อเวอร์ชันซ้ำกัน');
            names.add(name);
            if (!item.groups.length) add(`${path}.groups`, 'เพิ่มกลุ่มราคาอย่างน้อย 1 กลุ่ม');
            const offers = new Set();
            item.groups.forEach((offer, gi) => {
                const gp = `${path}.groups.${gi}`;
                if (!offer.websiteIds.length) add(`${gp}.websites`, 'เลือกอย่างน้อย 1 เว็บ');
                if (!validMoney(offer.price, false)) add(`${gp}.price`, 'กรอกราคามากกว่า 0 ทศนิยมไม่เกิน 2 ตำแหน่ง');
                if (!validMoney(offer.deposit, true)) add(`${gp}.deposit`, 'กรอกมัดจำตั้งแต่ 0 ทศนิยมไม่เกิน 2 ตำแหน่ง');
                else if (Number(offer.deposit) > Number(offer.price)) add(`${gp}.deposit`, 'มัดจำต้องไม่เกินราคาเต็ม');
                if (!offer.name.trim()) add(`${gp}.name`, 'กรอกชื่อปก หรือปิดตัวเลือกปกเพิ่มเติม');
                let duplicate = false;
                offer.websiteIds.forEach(site => {
                    const key = `${site}:${offer.name.trim().toLowerCase()}`;
                    if (offers.has(key)) duplicate = true;
                    offers.add(key);
                });
                if (duplicate) add(`${gp}.websites`, 'เว็บและปกเดียวกันซ้ำในเวอร์ชันนี้');
            });
        });
        errors.forEach(error => fieldError(error.field, error.message));
        if (errors.length) showError(`มี ${errors.length} จุดที่ต้องแก้ไข กรุณาตรวจช่องที่มีข้อความสีแดง`);
        return !errors.length;
    }

    async function open(id) {
        if (saving || importing || addingWebsite || addingArtist || addingType || (dirty && !window.confirm('มีข้อมูลที่ยังไม่ได้บันทึก ต้องการเริ่มใหม่หรือไม่?'))) return;
        const token = ++requestToken;
        clearErrors();
        byId('productSetupForm').reset();
        websiteAliases.clear();
        websitePanel = null; pricingQuery = ''; searchFoldStates.clear();
        byId('setupWebsiteNotice').hidden = true;
        byId('modalManageData').dispatchEvent(new Event('setup:reset'));
        byId('IdProduct').value = id || '';
        byId('productSetupTitle').textContent = id ? 'แก้ไขสินค้าและราคา' : 'เพิ่มสินค้าและตั้งราคา';
        byId('pName').value = '';
        byId('pPic').value = '';
        byId('IdType').value = '';
        byId('IdArtist').value = '';
        byId('IdProductStatus').value = '1';
        setProductScheduleCheckbox('end_date', 'end_date_unscheduled', true);
        setProductScheduleCheckbox('send_date', 'send_date_unscheduled', true);
        updateImagePreview();
        variants = [];
        dirty = false;
        loading = true;
        render();
        setBusy();
        const modal = bootstrap.Modal.getInstance(byId('modalManageData')) || new bootstrap.Modal(byId('modalManageData'));
        modal.show();
        window.ProductSetupSteps?.show(id ? 1 : 0);
        try {
            if (id) {
                const data = await json(`/product/setup/${id}`);
                if (token !== requestToken) return;
                websites = data.websites;
                const product = data.product;
                byId('pName').value = product.name || '';
                byId('IdType').value = product.type;
                byId('IdArtist').value = product.artist;
                byId('IdProductStatus').value = product.product_status;
                byId('pPic').value = product.pic || '';
                byId('end_date').value = check_date_null(product.end_date);
                byId('send_date').value = check_date_null(product.send_date);
                setProductScheduleCheckbox('end_date', 'end_date_unscheduled', !byId('end_date').value);
                setProductScheduleCheckbox('send_date', 'send_date_unscheduled', !byId('send_date').value);
                updateImagePreview();
                variants = data.versions.length ? data.versions.map(variant) : [variant()];
            } else {
                const options = await json('/product/setup/options');
                if (token !== requestToken) return;
                websites = options;
                variants = [variant()];
            }
            render();
            syncArtistPicker();
        } catch (error) {
            if (token === requestToken) showError(error.message);
        } finally {
            if (token === requestToken) {
                loading = false;
                setBusy();
                // A failed load must never enable saving an empty replacement for an existing product.
                if (!variants.length) {
                    byId('productSetupFields').disabled = true;
                    byId('setupSaveButton').disabled = true;
                }
            }
        }
    }

    async function save() {
        if (loading || saving || importing || addingWebsite || byId('productSetupFields').disabled) return;
        if (websitePanel) { showError('กรุณาบันทึกหรือยกเลิกการเลือกเว็บไซต์ก่อนบันทึกสินค้า'); return; }
        if (document.activeElement?.hasAttribute('data-row-version')) changeRowVersion(document.activeElement);
        if (rowRenderTimer !== null) render();
        clearErrors();
        const product = productInput();
        if (!validate(product)) return;
        saving = true;
        setBusy();
        try {
            const result = await json('/product/setup/save', { method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ product, versions: variants.map(item => ({ id: item.id, name: item.name.trim(),
                    groups: item.groups.map(offer => ({ name: offer.name.trim(), websiteIds: offer.websiteIds,
                        price: offer.price, deposit: offer.deposit, coverIds: offer.coverIds })) })) }) });
            dirty = false;
            byId('IdProduct').value = result.productId;
            websitePanel = null;
            saving = false; setBusy();
            const modal = byId('modalManageData');
            const closed = new Promise(resolve => modal.addEventListener('hidden.bs.modal', resolve, { once: true }));
            (bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal)).hide();
            await closed;
            await Swal.fire({ icon: 'success', title: 'บันทึกสินค้าสำเร็จ',
                text: `${result.versionCount} เวอร์ชัน · ${result.offerCount} รายการขาย`,
                confirmButtonText: 'ตกลง', confirmButtonColor: '#198754',
                allowOutsideClick: false, allowEscapeKey: false });
            window.location.reload();
        } catch (error) {
            saving = false;
            setBusy();
            if (error.field) fieldError(error.field, error.message);
            showError(error.message);
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const modal = byId('modalManageData');
        if (!modal || !byId('productSetupForm')) return;
        ['IdType', 'IdArtist'].forEach(id => {
            if (!byId(id).querySelector('option[value=""]')) byId(id).prepend(new Option('กรุณาเลือก', ''));
        });
        syncArtistPicker();
        modal.addEventListener('click', async event => {
            const remove = event.target.closest('[data-remove-site]');
            if (remove && !byId('productSetupFields').disabled) {
                const input = remove.closest('.setup-site-list').querySelector(`[data-website-id="${remove.dataset.removeSite}"]`);
                const token = requestToken;
                const name = input.closest('label').querySelector('span').textContent;
                if (!await confirmRemoval(`นำเว็บ “${name}” ออกจากกลุ่มนี้? เวอร์ชันและปกจะยังอยู่`) || token !== requestToken || byId('productSetupFields').disabled) return;
                input.checked = false;
                input.dispatchEvent(new Event('change', { bubbles: true }));
            }
            const artist = event.target.closest('[data-artist-id]');
            if (artist && !byId('productSetupFields').disabled) {
                byId('IdArtist').value = artist.dataset.artistId;
                byId('IdArtist').dispatchEvent(new Event('input', { bubbles: true }));
                syncArtistPicker();
                byId('setupArtistPicker').open = false;
                byId('setupArtistChoice').focus();
            }
        });
        modal.addEventListener('toggle', event => {
            if (!event.target.matches('.setup-search-dropdown') || !event.target.open) return;
            modal.querySelectorAll('.setup-search-dropdown[open]').forEach(other => { if (other !== event.target) other.open = false; });
            const input = event.target.querySelector('[data-picker-search]');
            filterPicker(input); input.focus();
        }, true);
        document.addEventListener('click', event => {
            modal.querySelectorAll('.setup-search-dropdown[open]').forEach(picker => { if (!picker.contains(event.target)) picker.open = false; });
        });
        modal.addEventListener('keydown', event => {
            const picker = event.target.closest('.setup-search-dropdown[open]');
            if (picker && event.target.hasAttribute('data-picker-search') && ['Enter', 'ArrowDown', 'ArrowUp'].includes(event.key)) {
                event.preventDefault();
                const options = [...picker.querySelectorAll('.setup-dropdown-options .setup-site:not([hidden]) input:not(:disabled), .setup-dropdown-option:not([hidden])')];
                const option = event.key === 'ArrowUp' ? options.at(-1) : options[0];
                if (event.key === 'Enter') option?.click(); else option?.focus();
            }
            if (picker && event.key === 'Escape') {
                event.preventDefault(); event.stopPropagation(); picker.open = false; picker.querySelector('summary').focus();
            }
        });
        byId('setupArtistAdd').addEventListener('click', openArtist);
        byId('setupArtistAddOutside').addEventListener('click', openArtist);
        byId('setupTypeAdd').addEventListener('click', () => {
            if (byId('productSetupFields').disabled) return;
            byId('setupTypeName').value = '';
            byId('setupTypeError').hidden = true;
            byId('setupTypeDialog').showModal();
            byId('setupTypeName').focus();
        });
        byId('setupTypeForm').addEventListener('submit', event => { event.preventDefault(); saveType(); });
        byId('setupTypeSave').addEventListener('click', saveType);
        byId('setupTypeCancel').addEventListener('click', () => { if (!addingType) byId('setupTypeDialog').close(); });
        byId('setupTypeDialog').addEventListener('cancel', event => { if (addingType) event.preventDefault(); });
        byId('setupTypeDialog').addEventListener('keydown', event => {
            if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); if (!addingType) byId('setupTypeDialog').close(); }
        });
        byId('setupArtistSave').addEventListener('click', saveArtist);
        byId('setupArtistForm').addEventListener('submit', event => { event.preventDefault(); saveArtist(); });
        byId('setupArtistCancel').addEventListener('click', () => { if (!addingArtist) byId('setupArtistDialog').close(); });
        byId('setupArtistDialog').addEventListener('cancel', event => { if (addingArtist) event.preventDefault(); });
        byId('setupArtistDialog').addEventListener('keydown', event => {
            if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); if (!addingArtist) byId('setupArtistDialog').close(); }
        });
        modal.addEventListener('click', action);
        modal.addEventListener('keydown', event => {
            if (event.key === 'Enter' && event.target.matches('.pricing-screen input')) {
                event.preventDefault();
                if (event.target.id === 'pricingSearch') applyPricingSearch();
                else if (!['search', 'checkbox'].includes(event.target.type)) event.target.blur();
            }
            if (event.key === 'Escape' && websitePanel && !byId('setupWebsiteDialog').open && !deleteConfirmOpen) {
                event.preventDefault(); event.stopImmediatePropagation(); websitePanel = null; render();
                byId('setupVersions').querySelector('[data-setup-action="manage-sites"]')?.focus();
            }
        }, true);
        // Checkboxes fire both input and change; handle each field only once.
        byId('productSetupForm').addEventListener('input', event => {
            if (!['checkbox', 'radio'].includes(event.target.type)) update(event);
        });
        byId('productSetupForm').addEventListener('change', event => {
            if (event.target.hasAttribute('data-row-version')) changeRowVersion(event.target);
            else if (['checkbox', 'radio'].includes(event.target.type)) update(event);
        });
        byId('productSetupForm').addEventListener('submit', event => { event.preventDefault(); save(); });
        byId('setupWebsiteSave').addEventListener('click', saveWebsite);
        byId('setupWebsiteForm').addEventListener('submit', event => { event.preventDefault(); saveWebsite(); });
        byId('setupWebsiteCancel').addEventListener('click', () => { if (!addingWebsite) byId('setupWebsiteDialog').close(); });
        byId('setupWebsiteDialog').addEventListener('cancel', event => { if (addingWebsite) event.preventDefault(); });
        byId('setupWebsiteDialog').addEventListener('keydown', event => {
            if (event.key === 'Escape') {
                event.preventDefault(); event.stopPropagation();
                if (!addingWebsite) byId('setupWebsiteDialog').close();
            }
        });
        modal.addEventListener('hide.bs.modal', event => {
            if (deleteConfirmOpen || websitePanel || byId('setupTypeDialog').open) { event.preventDefault(); return; }
            if (saving || importing || addingWebsite || addingArtist || addingType || byId('setupArtistDialog').open || byId('setupWebsiteDialog').open || (dirty && !window.confirm('ยังไม่ได้บันทึกการเปลี่ยนแปลง ต้องการปิดหรือไม่?'))) event.preventDefault();
        });
        modal.addEventListener('hidden.bs.modal', () => { requestToken++; dirty = false; });
        window.addEventListener('beforeunload', event => {
            if (dirty || importing || addingWebsite || addingArtist || addingType) { event.preventDefault(); event.returnValue = ''; }
        });
        const notice = sessionStorage.getItem('productSetupNotice');
        if (notice) {
            const banner = document.createElement('div');
            banner.className = 'alert alert-success';
            banner.setAttribute('role', 'status');
            banner.textContent = notice;
            document.querySelector('.page-content')?.prepend(banner);
            sessionStorage.removeItem('productSetupNotice');
        }
    });
    async function saveType() {
        if (addingType) return;
        const name = byId('setupTypeName').value.trim();
        const fail = message => { byId('setupTypeError').textContent = message; byId('setupTypeError').hidden = false; };
        if (!name || name.length > 255 || !normalizedName(name)) { fail('กรุณากรอกชื่อประเภท 1–255 ตัวอักษร'); return; }
        addingType = true; setBusy(); byId('setupTypeFields').disabled = true;
        byId('setupTypeError').hidden = true;
        byId('setupTypeSave').textContent = 'กำลังเพิ่มประเภท…';
        try {
            const type = await json('/product/setup/types', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name }) });
            if (!type.id || !type.name || type.delete !== 'A') throw new Error('ข้อมูลประเภทที่ได้รับไม่สมบูรณ์ กรุณาลองใหม่');
            const select = byId('IdType');
            let option = [...select.options].find(value => value.value === String(type.id));
            if (!option) { option = new Option(type.name, type.id); select.add(option); }
            option.textContent = type.name; select.value = String(type.id);
            dirty = true; clearFieldError(select);
            byId('setupTypeDialog').close();
        } catch (error) { fail(error.message); }
        finally {
            addingType = false; setBusy(); byId('setupTypeFields').disabled = false;
            byId('setupTypeSave').textContent = 'บันทึกและเลือกประเภท';
            if (!byId('setupTypeDialog').open) byId('IdType').focus();
        }
    }
    async function openArtist() {
        if (loading || saving || importing || addingWebsite || addingArtist || addingType) return;
        byId('setupArtistName').value = byId('setupArtistSearch').value.trim();
        byId('setupArtistError').hidden = true;
        byId('setupArtistGroup').innerHTML = '<option value="">กำลังโหลดสังกัด…</option>';
        byId('setupArtistPicker').open = false;
        byId('setupArtistDialog').showModal();
        addingArtist = true; setBusy();
        byId('setupArtistSave').disabled = true;
        try {
            const groups = await json('/product/setup/artists/groups');
            byId('setupArtistGroup').innerHTML = '<option value="">เลือกสังกัด</option>' + groups.map(g => `<option value="${g.id}">${escape(g.name)}</option>`).join('');
        } catch (error) { artistError(error.message); }
        finally { addingArtist = false; setBusy(); byId('setupArtistSave').disabled = false; byId('setupArtistName').focus(); }
    }

    function artistError(message) {
        byId('setupArtistError').textContent = message;
        byId('setupArtistError').hidden = false;
    }

    async function saveArtist() {
        if (addingArtist) return;
        const name = byId('setupArtistName').value.trim();
        const groupId = Number(byId('setupArtistGroup').value);
        if (!name || name.length > 255 || !normalizedName(name)) { artistError('กรุณากรอกชื่อศิลปิน 1–255 ตัวอักษร'); return; }
        if (!groupId) { artistError('กรุณาเลือกสังกัด'); return; }
        addingArtist = true; setBusy(); byId('setupArtistFields').disabled = true;
        byId('setupArtistError').hidden = true;
        byId('setupArtistSave').textContent = 'กำลังเพิ่มศิลปิน…';
        try {
            const artist = await json('/product/setup/artists', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name, groupId }) });
            if (!artist.id || !artist.name || artist.delete !== 'A') throw new Error('ข้อมูลศิลปินที่ได้รับไม่สมบูรณ์ กรุณาลองใหม่');
            const select = byId('IdArtist');
            let option = [...select.options].find(value => value.value === String(artist.id));
            if (!option) { option = new Option(artist.name, artist.id); select.add(option); }
            option.textContent = artist.name; select.value = String(artist.id);
            dirty = true; clearFieldError(byId('setupArtistChoice')); byId('setupArtistSearch').value = ''; syncArtistPicker();
            byId('setupArtistDialog').close(); byId('setupArtistChoice').focus();
        } catch (error) { artistError(error.message); }
        finally {
            addingArtist = false; setBusy(); byId('setupArtistFields').disabled = false;
            byId('setupArtistSave').textContent = 'บันทึกและเลือกศิลปิน';
        }
    }
    function importDraft(draft) {
        if (loading || saving || importing || addingWebsite || byId('productSetupFields').disabled) return [];
        const normalized = normalizedName;
        const warnings = [];
        // Replace only the untouched starter version. Existing IDs and edited groups remain intact.
        variants = variants.filter(item => item.id !== null || !['Standard', 'ทั่วไป'].includes(item.name)
            || item.groups.some(offer => offer.websiteIds.length || offer.price !== '' || Number(offer.deposit || 0) !== 0 || offer.name !== 'ทั่วไป'));
        for (const incoming of draft.versions || []) {
            let target = variants.find(item => normalized(item.name) === normalized(incoming.name));
            if (!target) {
                target = variant({ name: incoming.name, groups: [] });
                target.groups = [];
                variants.push(target);
            }
            for (const offer of incoming.groups || []) {
                const ids = [];
                const unmatched = [];
                let duplicates = 0;
                const sourceWebsites = offer.websites?.length ? offer.websites : ['ไม่มีเว็บ'];
                for (const sourceName of sourceWebsites) {
                    const site = findWebsite(sourceName);
                    if (!site) { unmatched.push(sourceName); continue; }
                    const exists = target.groups.some(value => value.websiteIds.includes(site.id) && normalized(value.name) === normalized(offer.name || 'ทั่วไป'));
                    if (exists) { duplicates++; continue; }
                    if (!ids.includes(site.id)) ids.push(site.id);
                }
                if (duplicates) warnings.push(`${incoming.name}: ข้าม ${duplicates} รายการที่มีอยู่แล้ว โดยเก็บราคาเดิมไว้`);
                if (unmatched.length) warnings.push(`${incoming.name}: ไม่พบเว็บ ${unmatched.join(', ')} ในตัวเลือก สามารถกดเพิ่มเว็บได้จากหน้านี้`);
                const sameUnassigned = !ids.length && !duplicates && target.groups.some(value => !value.websiteIds.length
                    && normalized(value.name) === normalized(offer.name || 'ทั่วไป')
                    && String(value.price) === String(offer.price ?? '') && String(value.deposit) === String(offer.deposit ?? '')
                    && value.unmatchedWebsites.map(normalized).sort().join(',') === unmatched.map(normalized).sort().join(','));
                if (sameUnassigned) {
                    warnings.push(`${incoming.name}: กลุ่มราคาที่ยังไม่ได้เลือกเว็บมีอยู่แล้ว จึงเก็บกลุ่มเดิมไว้`);
                    continue;
                }
                if (ids.length || unmatched.length) {
                    target.groups.push(group({ name: offer.name || 'ทั่วไป', websiteIds: ids,
                        price: offer.price ?? '', deposit: offer.deposit ?? '', unmatchedWebsites: unmatched }));
                }
            }
        }
        if (!variants.length) variants = [variant()];
        if (draft.productName && !byId('pName').value.trim()) byId('pName').value = draft.productName;
        clearErrors();
        dirty = true;
        render();
        return warnings;
    }

    function openWebsite(name = '', groupKey = null) {
        if (loading || saving || importing || addingWebsite) return;
        websiteSource = name;
        websiteTargetGroup = groupKey;
        byId('setupWebsiteName').value = name;
        byId('setupWebsiteError').hidden = true;
        byId('setupWebsiteName').removeAttribute('aria-invalid');
        byId('setupWebsiteDialog').showModal();
        byId('setupWebsiteName').focus();
        byId('setupWebsiteName').select();
    }

    async function saveWebsite() {
        if (addingWebsite) return;
        const name = byId('setupWebsiteName').value.trim();
        const fail = message => {
            byId('setupWebsiteError').textContent = message;
            byId('setupWebsiteError').hidden = false;
            byId('setupWebsiteName').setAttribute('aria-invalid', 'true');
        };
        if (!name || name.length > 255 || !normalizedName(name)) { fail('กรุณากรอกชื่อเว็บ 1–255 ตัวอักษร'); return; }
        addingWebsite = true;
        setBusy();
        byId('setupWebsiteFields').disabled = true;
        byId('setupWebsiteSave').textContent = 'กำลังเพิ่มเว็บ…';
        byId('setupWebsiteError').hidden = true;
        try {
            const site = await json('/product/setup/websites', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ name }) });
            if (!site.id || !site.name || site.delete !== 'A') throw new Error('ข้อมูลเว็บที่ได้รับไม่สมบูรณ์ กรุณาลองใหม่');
            const index = websites.findIndex(value => value.id === site.id);
            if (index < 0) websites.push(site); else websites[index] = site;
            if (websiteSource) websiteAliases.set(normalizedName(websiteSource), site.id);
            let selected = 0, collisions = 0;
            const targetBucket = variants.flatMap(item => item.groups).find(offer => offer.key === websiteTargetGroup)?.bucket;
            for (const item of variants) {
                for (const offer of item.groups) {
                    const matching = offer.unmatchedWebsites.some(value => normalizedName(value) === normalizedName(websiteSource)
                        || normalizedName(value) === normalizedName(site.name));
                    if (!matching && offer.key !== websiteTargetGroup && !(targetBucket && offer.bucket === targetBucket)) continue;
                    const exists = item.groups.some(other => other !== offer && other.websiteIds.includes(site.id)
                        && normalizedName(other.name) === normalizedName(offer.name));
                    if (exists) collisions++;
                    else if (!offer.websiteIds.includes(site.id)) {
                        offer.websiteIds = [site.id];
                        selected++;
                    }
                    offer.unmatchedWebsites = offer.unmatchedWebsites.filter(value => normalizedName(value) !== normalizedName(websiteSource)
                        && normalizedName(value) !== normalizedName(site.name));
                }
            }
            if (selected) dirty = true;
            clearErrors();
            render();
            const notice = byId('setupWebsiteNotice');
            notice.textContent = `ใช้เว็บ “${site.name}” ได้แล้ว${selected ? ` เลือกให้ ${selected} กลุ่มราคา` : ''}${collisions ? ` · ${collisions} กลุ่มมีรายการเว็บนี้อยู่แล้ว จึงเก็บราคาเดิมไว้` : ''}`;
            notice.className = `alert ${collisions ? 'alert-warning' : 'alert-success'}`;
            notice.hidden = false;
            byId('modalManageData').dispatchEvent(new CustomEvent('setup:websites-changed', { detail: { site, sourceName: websiteSource } }));
            byId('setupWebsiteDialog').close();
        } catch (error) { fail(error.message); }
        finally {
            addingWebsite = false;
            setBusy();
            byId('setupWebsiteFields').disabled = false;
            byId('setupWebsiteSave').textContent = 'บันทึกและใช้เว็บนี้';
        }
    }

    return { open, save, importDraft, openWebsite,
        missingWebsites(names) { return [...new Set(names)].filter(name => !findWebsite(name)); },
        setImportBusy(value) { importing = value; setBusy(); } };
})();
