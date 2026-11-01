/* Presentation only; ProductSetup owns validation, IDs and transactional persistence. */
window.ProductPricingView = (() => {
    const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const icon = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V4a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4"/></svg>';
    const chevron = '<span class="pricing-chevron" aria-hidden="true"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 15 6-6 6 6"/></svg></span>';
    function build(buckets, sites, rowHtml, panel, states) {
        const groups = [...buckets.values()];
        const cards = groups.map((entries, index) => {
            const first = entries[0], bucket = first.offer.bucket || first.offer.key;
            entries.forEach(e => { e.offer.bucket = bucket; });
            const versions = new Map();
            entries.forEach(e => { if (!versions.has(e.version.key)) versions.set(e.version.key, []); versions.get(e.version.key).push(e); });
            const attrs = `data-version-key="${first.version.key}" data-group-key="${first.offer.key}"`;
            const selected = first.offer.websiteIds.map(id => sites.find(s => s.id === id)).filter(Boolean);
            const tags = selected.map(site => `<span class="pricing-tag">${esc(site.name)}<button type="button" data-setup-action="remove-tag" data-site="${site.id}" aria-label="นำ ${esc(site.name)} ออก">×</button></span>`).join('');
            const content = [...versions.values()].map(list => {
                const e = list[0], key = `version-${bucket}-${e.version.key}`;
                const rows = list.map((entry, i) => {
                    const html = rowHtml(entry.offer, entry.version, entry.vi, entry.gi);
                    const cells = html.match(/<td[\s\S]*?<\/td>/g);
                    const deletion = cells[2].match(/<button[\s\S]*?<\/button>/)?.[0] || '';
                    cells[2] = cells[2].replace(deletion, '');
                    cells[6] = cells[6].replace('</td>', deletion + '</td>');
                    const key = `cover-${entry.offer.key}`;
                    const fields = cells.slice(2,6).map(cell => cell.replace(/<td data-label="([^"]+)">/, '<div class="pricing-cover-field"><span class="pricing-cover-label">$1</span>').replace('</td>','</div>')).join('');
                    return `<tr class="setup-price-group" data-search-cover="${esc(entry.offer.name.toLowerCase())}" data-version-key="${entry.version.key}" data-group-key="${entry.offer.key}"><td colspan="6"><details class="pricing-cover" data-fold="${key}" ${states.get(key) === true ? 'open' : ''}><summary>${chevron}<span class="pricing-cover-number">${i+1}</span><strong data-cover-title>${esc(entry.offer.name || 'ยังไม่มีชื่อปก')}</strong><span class="pricing-cover-totals"><span data-cover-price>${esc(entry.offer.price || '—')}</span> บาท · มัดจำ <span data-cover-deposit>${esc(entry.offer.deposit ?? '—')}</span></span><span class="pricing-cover-actions">${cells[6].replace(/<td[^>]*>|<\/td>/g,'')}</span></summary><div class="pricing-cover-fields">${fields}</div></details></td></tr>`;
                }).join('');
                return `<details class="pricing-version" data-fold="${key}" ${states.get(key) === true ? 'open' : ''} data-search-version="${esc(e.version.name.toLowerCase())}" data-version-key="${e.version.key}" data-group-key="${e.offer.key}">
                    <summary>${chevron}<div class="pricing-version-name" data-version-members="${list.map(x => x.offer.key).join(',')}"><input class="form-control" id="setup-group-${e.offer.key}-version" data-row-version data-field="versions.${e.vi}.name" value="${esc(e.version.name)}" maxlength="255" aria-label="ชื่อเวอร์ชัน กลุ่มที่ ${index+1}"></div><span class="pricing-badge">${list.length} ปก</span>
                    <div class="pricing-actions"><button type="button" class="btn btn-success" data-setup-action="add-cover">＋ เพิ่มปก</button><button type="button" class="btn pricing-secondary" data-setup-action="copy-version">${icon} คัดลอกเวอร์ชัน</button><button type="button" class="btn btn-danger" data-setup-action="remove-nested-version">ลบเวอร์ชัน</button></div></summary>
                    <div class="pricing-table-scroll"><table class="table pricing-cover-table"><thead><tr><th>ลำดับ</th><th>ชื่อปก</th><th>ราคาเต็ม (บาท)</th><th>มัดจำ (บาท)</th><th>คงเหลือ (บาท)</th><th>การจัดการ</th></tr></thead><tbody>${rows}</tbody></table></div></details>`;
            }).join('');
            const key = `group-${bucket}`;
            return `<details class="pricing-group" data-fold="${key}" data-bucket="${bucket}" data-search-sites="${esc(selected.map(s=>s.name).join(' ').toLowerCase())}" ${states.has(key) ? states.get(key) ? 'open' : '' : index === 0 ? 'open' : ''} ${attrs}>
                <summary><span class="pricing-number">${index+1}</span><strong>กลุ่มที่ ${index+1}</strong><span class="pricing-badge">${selected.length} เว็บไซต์</span><span class="pricing-badge">${versions.size} เวอร์ชัน</span><div class="pricing-actions"><button type="button" class="btn pricing-secondary" data-setup-action="manage-sites">⚙ จัดการเว็บไซต์</button><button type="button" class="btn btn-danger" data-setup-action="remove-bucket">ลบกลุ่มร้าน</button></div>${chevron}</summary>
                <div class="pricing-group-body"><div class="pricing-tags"><span>เว็บไซต์</span>${tags}<button type="button" class="btn pricing-secondary" data-setup-action="manage-sites">＋ เพิ่มเว็บไซต์</button></div>${first.offer.unmatchedWebsites.length ? `<p class="pricing-warning">เว็บที่ยังไม่มีในรายการ: ${esc(first.offer.unmatchedWebsites.join(', '))}</p>` : ''}${content}<button type="button" class="pricing-add-version" data-setup-action="add-nested-version">＋ เพิ่มเวอร์ชันในกลุ่มนี้</button></div></details>`;
        }).join('');
        let aside = '';
        if (panel) {
            const entries = groups.find(g => g[0].offer.bucket === panel.bucket);
            const index = groups.indexOf(entries);
            if (entries) aside = `<dialog id="pricingWebsiteModal" class="pricing-website-modal pricing-sidebar" aria-modal="true" aria-label="จัดการเว็บไซต์" data-version-key="${entries[0].version.key}" data-group-key="${entries[0].offer.key}"><div class="pricing-side-heading"><h5>จัดการเว็บไซต์ (กลุ่มที่ ${index+1})</h5><button type="button" data-setup-action="cancel-sites" aria-label="ปิดหน้าต่างเว็บไซต์">×</button></div><p>เลือกเว็บไซต์ที่ใช้เวอร์ชัน ปก และราคาเดียวกัน</p><input type="search" class="form-control" id="pricingSiteSearch" placeholder="ค้นหาเว็บไซต์…" aria-label="ค้นหาเว็บไซต์ในแผง"><div class="pricing-site-options">${sites.map(site => {
                const allowed = site.delete === 'A' || entries.every(e => e.offer.coverIds[site.id]);
                return `<label data-site-name="${esc(site.name.toLowerCase())}"><input type="checkbox" data-panel-site="${site.id}" ${panel.ids.includes(site.id)?'checked':''} ${allowed?'':'disabled'}><span class="pricing-site-avatar" aria-hidden="true">${esc(site.name.slice(0,1))}</span><span>${esc(site.name)}${site.delete !== 'A'?' (หยุดใช้งาน)':''}</span></label>`;
            }).join('')}</div><button type="button" class="pricing-add-version" data-setup-action="new-panel-site">＋ เพิ่มเว็บไซต์ใหม่</button><p class="pricing-hint">การเลือกเว็บจะใช้กับข้อมูลร่าง บันทึกสินค้าทั้งหมดเมื่อเสร็จ</p><div class="pricing-side-footer"><button type="button" class="btn pricing-secondary" data-setup-action="cancel-sites">ยกเลิก</button><button type="button" class="btn btn-primary" data-setup-action="save-sites">บันทึก</button></div></dialog>`;
        }
        return `<div class="pricing-screen"><header class="pricing-toolbar"><div><h4>จัดการราคาสินค้า</h4><p>จัดการเว็บไซต์ เวอร์ชัน และปก พร้อมกำหนดราคาเต็มและมัดจำ</p></div><div class="pricing-tools"><input type="search" id="pricingSearch" class="form-control" placeholder="ค้นหาเว็บไซต์ เวอร์ชัน หรือชื่อปก…" aria-label="ค้นหารายการราคา"><button type="button" class="btn pricing-secondary" data-setup-action="open-first">เปิดกลุ่มแรก</button><button type="button" class="btn pricing-secondary" data-setup-action="collapse-all">ยุบทั้งหมด</button><button type="button" class="btn btn-success" data-setup-action="add-row">＋ เพิ่มกลุ่มร้าน</button></div></header><div class="pricing-workspace"><main class="pricing-groups">${cards || '<p>ยังไม่มีกลุ่มร้าน กดเพิ่มกลุ่มร้านเพื่อเริ่มต้น</p>'}</main></div>${aside}<p id="pricingSearchEmpty" hidden>ไม่พบรายการที่ค้นหา</p><div id="pricingToast" role="status" aria-live="polite"></div></div>`;
    }
    return { build };
})();
