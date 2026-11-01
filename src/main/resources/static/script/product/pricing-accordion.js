/* Logical state is independent of native <details open>, which stays open until slide-up ends. */
window.PricingAccordion = (() => {
    const running = new Map();
    const selector = '.pricing-group, .pricing-version, .pricing-cover';
    const expanded = node => node.dataset.expanded === 'true';
    const children = node => [...node.querySelectorAll(selector)];
    const level = node => node.classList.contains('pricing-group') ? '.pricing-group' : node.classList.contains('pricing-version') ? '.pricing-version' : '.pricing-cover';
    const body = node => node.querySelector(':scope > .pricing-motion-body');
    function set(node, open, animate = true) {
        const content = body(node);
        if (!content) return Promise.resolve(true);
        const previous = running.get(node);
        const height = node.open ? content.getBoundingClientRect().height : 0;
        const opacity = node.open ? getComputedStyle(content).opacity : '0';
        // Freeze the currently painted frame BEFORE cancelling a previous animation.
        content.style.height = `${height}px`;
        content.style.opacity = opacity;
        if (previous) { previous.cancel(); running.delete(node); }
        if (open && previous && !expanded(node)) children(node).forEach(child => set(child, false, false));
        node.dataset.expanded = String(open);
        const summary = node.querySelector(':scope > summary');
        summary.setAttribute('aria-expanded', String(open));
        if (!open && content.contains(document.activeElement)) summary.focus({preventScroll:true});
        if (!animate || (!open && !node.open && !previous)) {
            content.style.height = open ? 'auto' : '0px'; content.style.opacity = open ? '1' : '0';
            content.style.willChange = ''; node.open = open; content.style.overflow = ''; content.inert = !open; return Promise.resolve(true);
        }
        node.open = true;
        content.inert = !open;
        content.style.overflow = 'hidden';
        content.style.willChange = 'height, opacity';
        const targetHeight = open ? content.scrollHeight : 0;
        content.getBoundingClientRect();
        const animation = content.animate([{height:`${height}px`,opacity}, {height:`${targetHeight}px`,opacity:open?1:0}], {duration:280,easing:'cubic-bezier(.25,.1,.25,1)',fill:'both'});
        running.set(node, animation);
        return animation.finished.then(() => {
            if (running.get(node) !== animation) return;
            running.delete(node);
            content.style.height = open ? 'auto' : '0px'; content.style.opacity = open ? '1' : '0';
            animation.cancel(); node.open = open; content.style.overflow = ''; content.style.willChange = '';
            return true;
        }).catch(() => false);
    }
    function close(node, animate = true) {
        // Children reset immediately inside the collapsing parent; no hidden stale state remains.
        const result = set(node, false, animate);
        children(node).forEach(child => { child.dataset.expanded = 'false'; child.querySelector(':scope > summary')?.setAttribute('aria-expanded','false'); });
        return result.then(completed => { if (completed && !expanded(node)) children(node).forEach(child => set(child, false, false)); });
    }
    function open(node, animate = true) {
        const peers = level(node) === '.pricing-group' ? node.closest('.pricing-groups').querySelectorAll(':scope > .pricing-group')
            : level(node) === '.pricing-version' ? node.closest('.pricing-group').querySelectorAll('.pricing-version')
            : node.closest('.pricing-version').querySelectorAll('.pricing-cover');
        [...peers].filter(peer => peer !== node).forEach(peer => close(peer, animate));
        // Mark descendants closed now, while their visible pixels finish sliding with their parent.
        [...peers].filter(peer => peer !== node).forEach(peer => children(peer).forEach(child => { child.dataset.expanded = 'false'; child.querySelector(':scope > summary')?.setAttribute('aria-expanded','false'); }));
        return set(node, true, animate);
    }
    function normalize(root) {
        let groupUsed = false;
        root.querySelectorAll('.pricing-group').forEach(group => {
            const groupOpen = group.open && !groupUsed; groupUsed ||= groupOpen;
            set(group, groupOpen, false);
            let versionUsed = false;
            group.querySelectorAll('.pricing-version').forEach(version => {
                const versionOpen = groupOpen && version.open && !versionUsed; versionUsed ||= versionOpen;
                set(version, versionOpen, false);
                let coverUsed = false;
                version.querySelectorAll('.pricing-cover').forEach(cover => {
                    const coverOpen = versionOpen && cover.open && !coverUsed; coverUsed ||= coverOpen; set(cover, coverOpen, false);
                });
            });
        });
    }
    function mount(root) {
        running.forEach(animation => animation.cancel()); running.clear();
        root.querySelectorAll(selector).forEach(node => {
            const summary = node.querySelector(':scope > summary');
            const content = document.createElement('div'); content.className = 'pricing-motion-body';
            [...node.childNodes].filter(child => child !== summary).forEach(child => content.append(child)); node.append(content);
            node.id = `pricing-${node.dataset.fold}`;
            content.id = `${node.id}-content`;
            summary.setAttribute('aria-controls',content.id);
        });
        normalize(root);
        if (root.dataset.accordionBound) return;
        root.dataset.accordionBound = 'true';
        root.addEventListener('click', event => {
            const summary = event.target.closest('summary');
            if (!summary || !summary.parentElement.matches(selector)) return;
            if (event.target.closest('input,button,select,textarea,a,label')) return;
            event.preventDefault();
            if (document.getElementById('productSetupFields')?.disabled) return;
            const node = summary.parentElement;
            if (expanded(node)) close(node); else open(node);
        });
    }
    async function reveal(target, {animate = true} = {}) {
        const path = [];
        for (let node = target; node; node = node.parentElement) if (node.matches?.(selector)) path.unshift(node);
        for (const node of path) if (!expanded(node)) await open(node, animate);
    }
    function collapseAll(root, animate = true) { root.querySelectorAll('.pricing-group').forEach(node => close(node,animate)); }
    function restore(root, states) {
        root.querySelectorAll('[data-fold]').forEach(node => { node.open = states.get(node.dataset.fold) === true; }); normalize(root);
    }
    return {mount, reveal, collapseAll, restore, expanded, open};
})();
