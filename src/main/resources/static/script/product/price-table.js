/* Loads saved prices through the existing read API; no edit controls or writes. */
window.ProductPriceTable = (() => {
    let request = null;
    let sequence = 0;
    const byId = id => document.getElementById(id);
    const amount = value => value == null || value === '' || !Number.isFinite(Number(value)) ? null : Number(value);
    const money = value => value == null ? '—' : value.toLocaleString('th-TH', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    async function open(id) {
        if (!Number.isInteger(Number(id)) || Number(id) <= 0) return;
        const modal = byId('productPricesModal');
        if (!modal) return;
        const ticket = ++sequence;
        request?.abort();
        const operation = new AbortController(); request = operation;
        byId('productPricesName').textContent = '';
        byId('productPricesRows').replaceChildren();
        byId('productPricesError').hidden = true;
        byId('productPricesEmpty').hidden = true;
        byId('productPricesTableWrap').hidden = true;
        byId('productPricesLoading').hidden = false;
        (bootstrap.Modal.getInstance(modal) || new bootstrap.Modal(modal)).show();
        try {
            const response = await fetch(`/product/setup/${Number(id)}`, {signal: operation.signal});
            if (response.redirected || !response.headers.get('content-type')?.includes('application/json')) throw new Error('กรุณาเข้าสู่ระบบใหม่แล้วลองเปิดตารางราคาอีกครั้ง');
            const data = await response.json();
            if (!response.ok) throw new Error(data.message || 'โหลดตารางราคาไม่สำเร็จ กรุณาลองใหม่');
            if (ticket !== sequence) return;
            byId('productPricesName').textContent = data.product?.name || '';
            const sites = new Map((data.websites || []).map(site => [Number(site.id), site.name]));
            const rows = [];
            for (const version of data.versions || []) for (const offer of version.groups || []) {
                const price = amount(offer.price), deposit = amount(offer.deposit);
                const websites = [...new Set(offer.websiteIds || [])].map(site => sites.get(Number(site)) || `เว็บ #${site}`);
                const row = document.createElement('tr');
                for (const value of [websites.join(' / ') || 'ไม่มีเว็บ', version.name || 'Standard', offer.name || 'ทั่วไป',
                    money(price), money(deposit), money(price == null || deposit == null ? null : price - deposit)]) {
                    const cell = document.createElement('td'); cell.textContent = value; row.append(cell);
                }
                rows.push(row);
            }
            byId('productPricesRows').replaceChildren(...rows);
            byId('productPricesTableWrap').hidden = !rows.length;
            byId('productPricesEmpty').hidden = !!rows.length;
        } catch (error) {
            if (ticket !== sequence || operation.signal.aborted) return;
            byId('productPricesError').textContent = error.message || 'โหลดตารางราคาไม่สำเร็จ กรุณาลองใหม่';
            byId('productPricesError').hidden = false;
        } finally {
            if (ticket === sequence) { request = null; byId('productPricesLoading').hidden = true; }
        }
    }
    document.addEventListener('DOMContentLoaded', () => {
        byId('productPricesModal')?.addEventListener('hidden.bs.modal', () => {
            sequence++; request?.abort(); request = null;
        });
    });
    return {open};
})();
