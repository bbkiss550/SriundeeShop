/* Cropping happens in the browser. The OCR upload contains only the selected pixels. */
window.ProductImageCrop = class {
    constructor(root, onChange) {
        this.root = root;
        this.canvas = root.querySelector('#setupCropCanvas');
        this.preview = root.querySelector('#setupCropPreview');
        this.stage = root.querySelector('.setup-crop-stage');
        this.fields = [...root.querySelectorAll('[data-crop-field]')];
        this.onChange = onChange;
        this.ticket = 0;
        this.selection = null;
        this.drag = null;
        this.forceNew = false;
        this.canvas.addEventListener('pointerdown', event => this.start(event));
        this.canvas.addEventListener('pointermove', event => this.move(event));
        this.canvas.addEventListener('pointerup', event => this.finish(event));
        this.canvas.addEventListener('pointercancel', event => this.finish(event, true));
        this.canvas.addEventListener('keydown', event => this.key(event));
        this.fields.forEach(field => field.addEventListener('change', () => {
            if (this.disabled() || !this.selection) return;
            this.select({ ...this.selection, [field.dataset.cropField]: Number(field.value) });
        }));
        root.querySelector('[data-crop-action="full"]').addEventListener('click', () => {
            if (!this.image || this.disabled()) return;
            this.forceNew = false;
            this.select({ x: 0, y: 0, width: this.image.naturalWidth, height: this.image.naturalHeight });
        });
        root.querySelector('[data-crop-action="new"]').addEventListener('click', () => {
            if (this.disabled()) return;
            this.forceNew = true;
            this.canvas.focus({ preventScroll: true });
            root.querySelector('#setupCropHelp').textContent = 'ลากบนภาพเพื่อวาดกรอบใหม่ หรือกด Escape เพื่อยกเลิก';
        });
        this.observer = new ResizeObserver(() => {
            const width = this.stage.clientWidth;
            if (this.image && width > 0 && width !== this.lastWidth) this.draw();
        });
        this.observer.observe(this.stage);
    }

    disabled() { return Boolean(this.root.closest('fieldset')?.disabled); }

    clear() {
        this.ticket++;
        if (this.url) URL.revokeObjectURL(this.url);
        this.url = null;
        this.image = null;
        this.selection = null;
        this.drag = null;
        this.forceNew = false;
        this.root.hidden = true;
    }

    async load(file) {
        this.clear();
        const ticket = this.ticket;
        const url = URL.createObjectURL(file);
        this.url = url;
        const image = new Image();
        image.src = url;
        try { await image.decode(); }
        catch (_) {
            if (ticket !== this.ticket) return false;
            this.clear();
            throw new Error('เปิดภาพไม่ได้ กรุณาเลือกไฟล์ PNG หรือ JPEG ที่สมบูรณ์');
        }
        if (ticket !== this.ticket) return false;
        const width = image.naturalWidth, height = image.naturalHeight;
        if (width < 20 || height < 20 || width * height > 16_000_000) {
            this.clear();
            throw new Error('ภาพต้องมีขนาดอย่างน้อย 20 × 20 และไม่เกิน 16 ล้านพิกเซล');
        }
        this.image = image;
        this.root.hidden = false;
        this.select({ x: 0, y: 0, width, height });
        return true;
    }

    clamp(rectangle) {
        const width = this.image.naturalWidth, height = this.image.naturalHeight;
        const bound = (value, min, max) => Math.max(min, Math.min(max, Math.round(Number.isFinite(value) ? value : min)));
        const x = bound(rectangle.x, 0, width - 20), y = bound(rectangle.y, 0, height - 20);
        return { x, y, width: bound(rectangle.width, 20, width - x), height: bound(rectangle.height, 20, height - y) };
    }

    full() {
        const s = this.selection;
        return s && s.x === 0 && s.y === 0 && s.width === this.image.naturalWidth && s.height === this.image.naturalHeight;
    }

    select(rectangle) {
        this.selection = this.clamp(rectangle);
        this.draw();
        this.onChange?.({ ...this.selection }, this.full());
    }

    draw() {
        if (!this.image || !this.selection || this.root.hidden) return;
        this.lastWidth = this.stage.clientWidth;
        const ratio = Math.min((this.lastWidth || 700) / this.image.naturalWidth, 520 / this.image.naturalHeight, 1);
        const width = Math.max(1, Math.floor(this.image.naturalWidth * ratio));
        const height = Math.max(1, Math.floor(this.image.naturalHeight * ratio));
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        this.canvas.width = Math.round(width * dpr);
        this.canvas.height = Math.round(height * dpr);
        this.canvas.style.width = `${width}px`;
        this.canvas.style.height = `${height}px`;
        const ctx = this.canvas.getContext('2d');
        ctx.scale(dpr, dpr);
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, width, height);
        ctx.drawImage(this.image, 0, 0, width, height);
        const s = this.selection;
        const x = s.x * width / this.image.naturalWidth, y = s.y * height / this.image.naturalHeight;
        const w = s.width * width / this.image.naturalWidth, h = s.height * height / this.image.naturalHeight;
        ctx.fillStyle = 'rgba(12, 22, 38, .58)';
        ctx.fillRect(0, 0, width, y);
        ctx.fillRect(0, y, x, h);
        ctx.fillRect(x + w, y, width - x - w, h);
        ctx.fillRect(0, y + h, width, height - y - h);
        ctx.strokeStyle = '#4a74ff';
        ctx.lineWidth = 2;
        ctx.strokeRect(x, y, w, h);
        ctx.fillStyle = '#fff';
        for (const point of this.handles(x, y, w, h)) {
            ctx.fillRect(point.x - 5, point.y - 5, 10, 10);
            ctx.strokeRect(point.x - 5, point.y - 5, 10, 10);
        }
        this.fields.forEach(field => {
            field.value = s[field.dataset.cropField];
            const dimension = ['x', 'width'].includes(field.dataset.cropField) ? this.image.naturalWidth : this.image.naturalHeight;
            field.max = dimension - (['x', 'y'].includes(field.dataset.cropField) ? 20 : 0);
        });
        this.root.querySelector('#setupCropSize').textContent = this.full()
            ? `เลือกทั้งภาพ ${s.width} × ${s.height} px` : `อ่านเฉพาะกรอบ ${s.width} × ${s.height} px · ซ้าย ${s.x} · บน ${s.y}`;
        this.root.querySelector('#setupCropHelp').textContent = this.forceNew
            ? 'ลากบนภาพเพื่อวาดกรอบใหม่ หรือกด Escape เพื่อยกเลิก'
            : 'ลากบนภาพเพื่อเลือกส่วนที่ต้องการอ่าน ลากในกรอบเพื่อย้าย';
        const previewScale = Math.min(360 / s.width, 180 / s.height, 1);
        this.preview.width = Math.max(1, Math.round(s.width * previewScale));
        this.preview.height = Math.max(1, Math.round(s.height * previewScale));
        const preview = this.preview.getContext('2d');
        preview.fillStyle = '#fff';
        preview.fillRect(0, 0, this.preview.width, this.preview.height);
        preview.drawImage(this.image, s.x, s.y, s.width, s.height, 0, 0, this.preview.width, this.preview.height);
    }

    handles(x, y, width, height) {
        return [{ key: 'nw', x, y }, { key: 'ne', x: x + width, y },
            { key: 'sw', x, y: y + height }, { key: 'se', x: x + width, y: y + height },
            { key: 'n', x: x + width / 2, y }, { key: 's', x: x + width / 2, y: y + height },
            { key: 'w', x, y: y + height / 2 }, { key: 'e', x: x + width, y: y + height / 2 }];
    }

    point(event) {
        const bounds = this.canvas.getBoundingClientRect();
        return { x: Math.max(0, Math.min(this.image.naturalWidth, (event.clientX - bounds.left) / bounds.width * this.image.naturalWidth)),
            y: Math.max(0, Math.min(this.image.naturalHeight, (event.clientY - bounds.top) / bounds.height * this.image.naturalHeight)) };
    }

    start(event) {
        if (this.disabled() || !this.image || this.drag || event.button !== 0 || !event.isPrimary) return;
        event.preventDefault();
        this.canvas.focus({ preventScroll: true });
        const point = this.point(event), s = this.selection;
        const bounds = this.canvas.getBoundingClientRect();
        const thresholdX = 14 / bounds.width * this.image.naturalWidth;
        const thresholdY = 14 / bounds.height * this.image.naturalHeight;
        const handle = !this.forceNew && this.handles(s.x, s.y, s.width, s.height)
            .find(p => Math.abs(p.x - point.x) <= thresholdX && Math.abs(p.y - point.y) <= thresholdY);
        const inside = point.x > s.x && point.x < s.x + s.width && point.y > s.y && point.y < s.y + s.height;
        const mode = handle ? handle.key : !this.forceNew && !this.full() && inside ? 'move' : 'new';
        this.drag = { pointer: event.pointerId, point, original: { ...s }, mode, moved: false };
        this.canvas.setPointerCapture(event.pointerId);
    }

    move(event) {
        if (!this.drag || this.drag.pointer !== event.pointerId || this.disabled()) return;
        const point = this.point(event), { original: s, point: start, mode } = this.drag;
        const dx = point.x - start.x, dy = point.y - start.y;
        const bounds = this.canvas.getBoundingClientRect();
        if (!this.drag.moved && Math.hypot(dx * bounds.width / this.image.naturalWidth, dy * bounds.height / this.image.naturalHeight) < 3) return;
        this.drag.moved = true;
        if (mode === 'move') {
            this.select({ ...s, x: Math.max(0, Math.min(this.image.naturalWidth - s.width, s.x + dx)),
                y: Math.max(0, Math.min(this.image.naturalHeight - s.height, s.y + dy)) });
        } else {
            let left = s.x, top = s.y, right = s.x + s.width, bottom = s.y + s.height;
            if (mode === 'new') { left = start.x; top = start.y; right = point.x; bottom = point.y; }
            else {
                if (mode.includes('w')) left += dx;
                if (mode.includes('e')) right += dx;
                if (mode.includes('n')) top += dy;
                if (mode.includes('s')) bottom += dy;
            }
            this.select({ x: Math.min(left, right), y: Math.min(top, bottom), width: Math.abs(right - left), height: Math.abs(bottom - top) });
        }
    }

    finish(event, cancel = false) {
        if (!this.drag || this.drag.pointer !== event.pointerId) return;
        if (cancel && this.drag.moved) this.select(this.drag.original);
        if (this.canvas.hasPointerCapture(event.pointerId)) this.canvas.releasePointerCapture(event.pointerId);
        this.drag = null;
        this.forceNew = false;
        this.draw();
    }

    key(event) {
        if (this.disabled() || !this.selection) return;
        if (event.key === 'Escape') {
            if (this.drag?.moved) this.select(this.drag.original);
            this.drag = null;
            this.forceNew = false;
            this.draw();
            event.preventDefault();
            event.stopPropagation();
            return;
        }
        const direction = { ArrowLeft: [-1, 0], ArrowRight: [1, 0], ArrowUp: [0, -1], ArrowDown: [0, 1] }[event.key];
        if (!direction) return;
        event.preventDefault();
        const s = this.selection;
        if (event.shiftKey) this.select({ ...s, width: s.width + direction[0], height: s.height + direction[1] });
        else this.select({ ...s, x: Math.min(this.image.naturalWidth - s.width, s.x + direction[0]),
            y: Math.min(this.image.naturalHeight - s.height, s.y + direction[1]) });
    }

    async file() {
        if (!this.image || !this.selection) throw new Error('กรุณาเลือกภาพและกรอบที่ต้องการอ่านก่อน');
        const s = this.selection;
        const cropped = document.createElement('canvas');
        cropped.width = s.width;
        cropped.height = s.height;
        const ctx = cropped.getContext('2d');
        ctx.fillStyle = '#fff';
        ctx.fillRect(0, 0, s.width, s.height);
        ctx.drawImage(this.image, s.x, s.y, s.width, s.height, 0, 0, s.width, s.height);
        const encode = (type, quality) => new Promise((resolve, reject) => cropped.toBlob(blob => blob ? resolve(blob) : reject(new Error('เตรียมภาพส่วนที่เลือกไม่สำเร็จ')), type, quality));
        let blob = await encode('image/png');
        if (blob.size > 8 * 1024 * 1024) blob = await encode('image/jpeg', .92);
        if (blob.size > 8 * 1024 * 1024) throw new Error('ภาพส่วนที่เลือกใหญ่เกิน 8 MB กรุณาครอปให้เล็กลง');
        return new File([blob], blob.type === 'image/png' ? 'selected-crop.png' : 'selected-crop.jpg', { type: blob.type });
    }
};
