window.ProductSetupSteps = (() => {
    let current = 0;
    const byId = id => document.getElementById(id);
    function show(index, focus = false) {
        current = Math.max(0, Math.min(2, index));
        document.querySelectorAll('[data-setup-step]').forEach((tab, i) => {
            tab.setAttribute('aria-selected', String(i === current));
            tab.tabIndex = i === current ? 0 : -1;
            byId(`setupStepPanel${i}`).hidden = i !== current;
        });
        byId('setupStepPrevious').hidden = current === 0;
        byId('setupStepNext').hidden = current === 2;
        byId('setupStepNext').textContent = current === 0 ? 'ถัดไป / ข้ามการอ่านข้อมูล' : 'ถัดไป';
        byId('setupSaveButton').hidden = current !== 2;
        if (focus) byId(`setupStepTab${current}`).focus();
        byId('modalManageData').querySelector('.modal-body').scrollTop = 0;
    }
    document.addEventListener('DOMContentLoaded', () => {
        const modal = byId('modalManageData');
        if (!modal) return;
        const busy = () => byId('productSetupFields').disabled;
        modal.querySelectorAll('[data-setup-step]').forEach(tab => {
            tab.addEventListener('click', () => { if (!busy()) show(Number(tab.dataset.setupStep)); });
            tab.addEventListener('keydown', event => {
                const target = { ArrowRight: (current + 1) % 3, ArrowLeft: (current + 2) % 3, Home: 0, End: 2 }[event.key];
                if (target !== undefined && !busy()) { event.preventDefault(); show(target, true); }
            });
        });
        byId('setupStepPrevious').addEventListener('click', () => { if (!busy()) show(current - 1, true); });
        byId('setupStepNext').addEventListener('click', () => { if (!busy()) show(current + 1, true); });
        new MutationObserver(() => {
            modal.querySelectorAll('[data-setup-step], #setupStepPrevious, #setupStepNext').forEach(button => { button.disabled = busy(); });
        }).observe(byId('productSetupFields'), { attributes: true, attributeFilter: ['disabled'] });
        show(0);
    });
    return { show };
})();
