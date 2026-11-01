// Run after: npm install --prefix .runtime/product-setup-qa playwright --no-audit --no-fund
// Uses mock API responses and real templates/assets; never connects to the shop database.
const { chromium } = require('../../.runtime/product-setup-qa/node_modules/playwright');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../..');
const templates = path.join(root, 'src/main/resources/templates');
const staticRoot = path.join(root, 'src/main/resources/static');
const output = path.join(root, '.runtime/product-setup-qa/screenshots');
fs.mkdirSync(output, { recursive: true });
const read = name => fs.readFileSync(path.join(templates, name), 'utf8');
let html = read('product/product.html');
let head = read('common/header.html').match(/<head[^>]*>[\s\S]*?<\/head>/)[0];
head = head.replace(/<script src="https:[^>]+><\/script>/g, '<script>window.feather = { replace() {} };</script>')
    .replace(/<link[^>]+https:[^>]+>/g, '');
html = html.replace(/<head th:replace[^>]*><\/head>/, head)
    .replace(/<div th:replace="common\/menu :: menu"><\/div>/, '')
    .replace(/<header th:replace[^>]*><\/header>/, '')
    .replace(/<div th:include="\.\/([^"\s]+)"><\/div>/g, (_, name) => read(`${name}.html`))
    .replace(/<footer th:replace[^>]*><\/footer>/, `<script src="/mazer/dist/assets/js/bootstrap.bundle.min.js"></script>
        <script src="/mazer/dist/assets/vendors/sweetalert2/sweetalert2.all.min.js"></script>
        <script src="/script/date-display.js"></script><script src="/script/submit-guard.js"></script>`)
    .replace(/th:(src|href)="@\{([^}]+)\}"/g, '$1="$2"')
    .replace(/<tbody th:utext="\$\{mainProduct\}"><\/tbody>/, '<tbody></tbody>');
for (const id of ['IdType', 'IdArtist', 'IdProductStatus']) {
    html = html.replace(new RegExp(`(<select[^>]*id="${id}"[^>]*>)[\\s\\S]*?(</select>)`), '$1<option value="1">ตัวอย่าง</option>$2');
}
// The surrounding navigation is irrelevant to modal QA; keep the real product layout and styles.
html = html.replace('</head>', '<style>#main { margin: 0 !important; padding: 24px !important; }</style></head>');

const server = http.createServer((req, res) => {
    if (req.url === '/product') {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end(html);
    }
    const requested = path.resolve(staticRoot, '.' + decodeURIComponent(req.url.split('?')[0]));
    if (!requested.startsWith(staticRoot + path.sep) || !fs.existsSync(requested) || !fs.statSync(requested).isFile()) {
        res.writeHead(404); return res.end();
    }
    const types = { '.css': 'text/css', '.js': 'application/javascript', '.png': 'image/png', '.woff2': 'font/woff2' };
    res.writeHead(200, { 'Content-Type': types[path.extname(requested)] || 'application/octet-stream' });
    fs.createReadStream(requested).pipe(res);
});

const sites = ['Ktown4u', 'YES24', 'Aladin', 'Makestar', 'Plus Chat', 'Mnet Plus Merch', 'FNC STORE'].map((name, index) => ({ id: index + 1, name, delete: 'A' }));
sites.push({ id: 100, name: 'ไม่มีเว็บ', delete: 'A' });
const existing = {
    product: { id: 10, name: 'ALD1 Garage', type: 1, artist: 1, product_status: 1, end_date: '2026-11-15', send_date: null, pic: '' },
    websites: sites,
    versions: [{ id: 20, name: 'T-SHIRT VER.', groups: [
        { name: 'ทั่วไป', websiteIds: [1, 2], price: 1350, deposit: 700, coverIds: { 1: 30, 2: 31 } },
        { name: 'ทั่วไป', websiteIds: [3, 4, 5, 6], price: 1450, deposit: 700, coverIds: { 3: 32, 4: 33, 5: 34, 6: 35 } }
    ] }]
};
const imageDraft = {
    productName: null,
    versions: [
        { name: 'T-SHIRT VER.', groups: [
            { name: 'ทั่วไป', websites: ['Ktown4u', 'YES24'], price: 1350, deposit: 700 },
            { name: 'ทั่วไป', websites: ['Aladin', 'Makestar', 'Plus Chat', 'Mnet Plus Merch'], price: 1450, deposit: 700 }
        ] },
        { name: 'DESK VER.', groups: [{ name: 'ทั่วไป', websites: [], price: 950, deposit: 500 }] }
    ],
    warnings: ['DESK VER.: ไม่ได้ระบุเว็บ เลือกไม่มีเว็บให้แล้ว กรุณาตรวจทาน'],
    rawText: 'Ktown4u / YES24\nT-SHIRT VER.\nกล่องละ 1,350 (มัดจำ 700)\nAladin / Makestar / Plus Chat / Mnet Plus Merch\nT-SHIRT VER.\nกล่องละ 1,450 (มัดจำ 700)\nDESK VER.\nกล่องละ 950 (มัดจำ 500)'
};

(async () => {
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const url = `http://127.0.0.1:${server.address().port}/product`;
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1440, height: 1050 } });
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const submissions = [];
        let failSave = true;
        let failLoad = false;
        let failImage = false;
        let imageReads = 0;
        let slowImage = false;
        let cancelledReads = 0;
        let textParses = 0;
        const imageUploads = [];
        let nextImageDraft = imageDraft;
        let failWebsite = false;
        let failArtist = false;
        const artistAdds = [];
        const websiteAdds = [];
        await page.route('**/product/setup/**', async route => {
            const request = route.request();
            if (request.url().includes('/image/progress/')) return route.fulfill({ json: { percent: 50, message: 'กำลังอ่านข้อความในภาพ', done: cancelledReads > 0, cancelled: cancelledReads > 0 } });
            if (request.url().includes('/image/cancel/')) {
                cancelledReads++;
                return route.fulfill({ json: { percent: 50, message: 'หยุดอ่านภาพแล้ว', done: true, cancelled: true } });
            }
            if (request.url().endsWith('/artists/groups')) return route.fulfill({ json: [{ id: 2, name: 'Example Label', delete: 'A' }] });
            if (request.url().endsWith('/artists')) {
                artistAdds.push(request.postDataJSON());
                if (failArtist) return route.fulfill({ status: 500, json: { message: 'เพิ่มศิลปินไม่สำเร็จ' } });
                return route.fulfill({ json: { id: 22, name: request.postDataJSON().name, group: 2, delete: 'A' } });
            }
            if (request.url().endsWith('/image/read')) {
                if (slowImage) {
                    await new Promise(resolve => setTimeout(resolve, 15000));
                    return route.fulfill({ json: nextImageDraft }).catch(() => {});
                }
                imageReads++;
                assert.match(request.headers()['content-type'], /multipart\/form-data/);
                const body = request.postDataBuffer();
                assert.ok(body.includes(Buffer.from('name="image"')));
                assert.ok(body.includes(Buffer.from('filename="selected-crop.png"')), 'only a generated crop is uploaded');
                const start = body.indexOf(Buffer.from('\r\n\r\n')) + 4;
                const boundary = request.headers()['content-type'].split('boundary=')[1];
                const end = body.indexOf(Buffer.from(`\r\n--${boundary}`), start);
                const png = body.subarray(start, end);
                assert.equal(png.subarray(1, 4).toString(), 'PNG');
                imageUploads.push({ width: png.readUInt32BE(16), height: png.readUInt32BE(20), bytes: png });
                if (failImage) return route.fulfill({ status: 503, json: { message: 'ยังไม่พร้อมอ่านภาพในเครื่อง' } });
                return route.fulfill({ json: nextImageDraft });
            }
            if (request.url().endsWith('/image/parse')) {
                textParses++;
                assert.equal(request.postDataJSON().text, imageDraft.rawText);
                return route.fulfill({ json: imageDraft });
            }
            if (request.url().endsWith('/websites') && request.method() === 'POST') {
                const { name } = request.postDataJSON();
                websiteAdds.push(name);
                if (failWebsite) return route.fulfill({ status: 500, json: { message: 'เพิ่มเว็บไม่สำเร็จ กรุณาลองอีกครั้ง' } });
                const normalized = value => value.normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{M}\p{N}]/gu, '');
                let site = sites.find(value => normalized(value.name) === normalized(name));
                if (!site) { site = { id: sites.filter(value => value.id < 100).length + 1, name, delete: 'A' }; sites.push(site); }
                return route.fulfill({ json: site });
            }
            if (request.method() === 'POST') {
                submissions.push(request.postDataJSON());
                if (failSave) return route.fulfill({ status: 500, json: { message: 'ทดสอบข้อผิดพลาด ข้อมูลที่กรอกยังอยู่' } });
                return route.fulfill({ json: { productId: 10, versionCount: 1, offerCount: 6 } });
            }
            if (request.url().endsWith('/options')) return route.fulfill({ json: sites });
            if (failLoad) return route.fulfill({ status: 500, json: { message: 'โหลดสินค้าไม่สำเร็จ' } });
            return route.fulfill({ json: existing });
        });
        await page.goto(url);
        page.on('dialog', dialog => dialog.accept());
        const rows = () => page.locator('.setup-editor-table tr[data-group-key]');
        const start = async id => {
            if (await page.locator('#modalManageData').isVisible()) {
                await page.locator('#modalManageData .modal-header [data-bs-dismiss="modal"]').click();
                await page.locator('#modalManageData').waitFor({ state: 'hidden' });
            }
            await page.evaluate(id => id ? edit_data(id) : new_data(), id || null);
            await page.locator('#setupSaveButton:enabled').waitFor();
        };
        const chooseSite = async (index, name) => {
            const picker = rows().nth(index).locator('.setup-search-dropdown');
            await picker.locator('summary').click();
            await picker.locator('[data-picker-search]').fill(name);
            await picker.getByRole('radio', { name, exact: true }).check();
            assert.equal(await picker.evaluate(node => node.open), false);
        };
        const basic = async name => {
            await page.locator('#pName').fill(name);
            await page.locator('#IdType').selectOption('1');
            await page.locator('#setupArtistChoice').click();
            await page.locator('#setupArtistSearch').fill('ตัวอย่าง');
            await page.keyboard.press('Enter');
        };
        const submitFailure = async () => {
            const before = submissions.length;
            await page.locator('#setupSaveButton').click();
            await page.waitForFunction(count => window.__unused === undefined && document.getElementById('productSetupError').hidden === false, before);
            await page.locator('#setupSaveButton:enabled').waitFor();
            assert.equal(submissions.length, before + 1);
            return submissions.at(-1);
        };
        await start();
        assert.equal(await rows().count(), 1);
        assert.equal(await rows().first().locator('[data-row-version]').inputValue(), 'Standard');
        assert.equal(await page.locator('.setup-version').count(), 0, 'the old version cards are replaced by one editor');
        await page.locator('#setupSaveButton').click();
        assert.equal(submissions.length, 0);
        assert.equal(await page.locator('#productSetupForm [aria-invalid="true"]').count(), 5);
        await basic('Editable table');
        await rows().first().locator('[data-row-version]').fill('PHOTOBOOK VER.');
        await rows().first().locator('[data-row-version]').press('Tab');
        await rows().first().locator('[data-prop="price"]').fill('590');
        await rows().first().locator('[data-prop="deposit"]').fill('300');
        await rows().first().locator('[data-prop="name"]').fill('เดี่ยว');
        await chooseSite(0, 'YES24');
        assert.equal(await rows().first().locator('[data-balance]').innerText(), '290.00');
        await chooseSite(0, 'ไม่มีเว็บ');
        assert.equal(await rows().first().locator('[data-website-id]:checked').count(), 1);
        await chooseSite(0, 'YES24');
        await rows().first().locator('[data-setup-action="copy-row"]').click();
        assert.equal(await rows().count(), 2);
        assert.equal(await rows().nth(1).locator('[data-prop="price"]').inputValue(), '590');
        await page.locator('#setupSaveButton').click();
        assert.equal(submissions.length, 0, 'duplicate row is rejected before sending');
        await rows().nth(1).locator('[data-prop="name"]').fill('SET');
        await rows().nth(1).locator('[data-prop="price"]').fill('1750');
        await rows().nth(1).locator('[data-prop="deposit"]').fill('900');
        let payload = await submitFailure();
        assert.deepEqual(payload.versions[0].groups.map(g => [g.name, g.price, g.deposit]), [['เดี่ยว', '590', '300'], ['SET', '1750', '900']]);
        assert.equal(await rows().first().locator('[data-prop="price"]').inputValue(), '590');
        await page.locator('[data-setup-action="add-row"]').click();
        await rows().nth(2).locator('[data-row-version]').fill('ENGENE VER.');
        await rows().nth(2).locator('[data-prop="name"]').click();
        await rows().nth(2).locator('[data-prop="name"]').fill('DAY SET');
        await rows().nth(2).locator('[data-prop="price"]').fill('2390');
        await rows().nth(2).locator('[data-prop="deposit"]').fill('1200');
        await chooseSite(2, 'Ktown4u');
        await rows().nth(2).locator('[data-setup-action="copy-row"]').click();
        await rows().nth(3).locator('[data-prop="name"]').fill('NIGHT SET');
        assert.equal(await rows().nth(3).locator('[data-balance]').innerText(), '1,190.00');
        payload = await submitFailure();
        assert.deepEqual(payload.versions.map(v => v.name), ['PHOTOBOOK VER.', 'ENGENE VER.']);
        assert.equal(payload.versions[1].groups.length, 2);
        await rows().nth(3).locator('[data-setup-action="remove-row"]').click();
        assert.equal(await rows().count(), 3);

        // Existing multi-site groups become independent rows, retaining every cover ID.
        await start(10);
        assert.equal(await rows().count(), 6);
        assert.equal(await page.locator('#end_date').inputValue(), '2026-11-15');
        await rows().first().locator('[data-prop="price"]').fill('1400');
        assert.equal(await rows().nth(1).locator('[data-prop="price"]').inputValue(), '1350');
        payload = await submitFailure();
        assert.equal(payload.versions[0].id, 20);
        assert.deepEqual(payload.versions[0].groups.map(g => g.coverIds[g.websiteIds[0]]), [30, 31, 32, 33, 34, 35]);
        await rows().first().locator('[data-row-version]').fill('DESK VER.');
        await rows().first().locator('[data-row-version]').press('Tab');
        await page.waitForFunction(() => [...document.querySelectorAll('[data-row-version]')].at(-1).value === 'DESK VER.');
        payload = await submitFailure();
        assert.equal(payload.versions[0].id, 20);
        assert.deepEqual(payload.versions[0].groups.map(g => g.coverIds[g.websiteIds[0]]), [31, 32, 33, 34, 35]);
        assert.equal(payload.versions[1].id, null);
        assert.deepEqual(payload.versions[1].groups[0].coverIds, {}, 'moving a persisted offer cannot submit its old cover ID under another version');
        assert.equal(payload.versions[1].groups[0].price, '1400');

        // A failed edit load cannot save an empty replacement.
        await page.locator('#modalManageData .modal-header [data-bs-dismiss="modal"]').click();
        await page.locator('#modalManageData').waitFor({ state: 'hidden' });
        failLoad = true;
        await page.evaluate(() => edit_data(10));
        await page.locator('#productSetupError:not([hidden])').waitFor();
        assert.equal(await page.locator('#setupSaveButton').isDisabled(), true);
        failLoad = false;
        await start();
        await basic('Clipboard draft');
        const imageInput = page.locator('#setupImageFile');
        const testImage = Buffer.from(await page.evaluate(() => {
            const c = document.createElement('canvas'); c.width = 300; c.height = 200;
            const ctx = c.getContext('2d'); ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 300, 200);
            ctx.fillStyle = 'blue'; ctx.fillRect(60, 50, 120, 80);
            return c.toDataURL('image/png').split(',')[1];
        }), 'base64');
        await page.locator('#setupImagePaste').focus();
        await page.evaluate(encoded => {
            const transfer = new DataTransfer(); transfer.items.add(new File([Uint8Array.from(atob(encoded), c => c.charCodeAt(0))], 'clipboard.png', { type: 'image/png' }));
            document.activeElement.dispatchEvent(new ClipboardEvent('paste', { clipboardData: transfer, bubbles: true, cancelable: true }));
        }, testImage.toString('base64'));
        await page.locator('#setupReadImage:enabled').waitFor();
        assert.equal(await imageInput.evaluate(node => node.files[0].name), 'clipboard.png');
        slowImage = true;
        await page.locator('#setupReadImage').click();
        await page.waitForFunction(() => document.getElementById('setupImagePercent').textContent === '50%');
        await page.locator('#setupStopImage').click();
        await page.locator('#setupReadImage:enabled').waitFor();
        assert.equal(cancelledReads, 1);
        assert.equal(await page.locator('#pName').inputValue(), 'Clipboard draft');
        slowImage = false;
        const canvas = page.locator('#setupCropCanvas');
        await canvas.focus();
        await canvas.scrollIntoViewIfNeeded();
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const bounds = await canvas.boundingBox();
        await page.mouse.move(bounds.x + bounds.width * .2, bounds.y + bounds.height * .25);
        await page.mouse.down();
        await page.mouse.move(bounds.x + bounds.width * .6, bounds.y + bounds.height * .65, { steps: 8 });
        await page.mouse.up();
        assert.equal(await page.locator('.setup-crop-fields').isVisible(), false);
        await page.locator('#setupReadImage').click();
        await page.locator('#setupImageResult:not([hidden])').waitFor();
        assert.equal(imageUploads.at(-1).width, 120);
        assert.equal(imageUploads.at(-1).height, 80);
        const onlyBlue = await page.evaluate(async encoded => {
            const bitmap = await createImageBitmap(new Blob([Uint8Array.from(atob(encoded), c => c.charCodeAt(0))], { type: 'image/png' }));
            const c = document.createElement('canvas'); c.width = bitmap.width; c.height = bitmap.height;
            const ctx = c.getContext('2d'); ctx.drawImage(bitmap, 0, 0);
            const pixels = ctx.getImageData(0, 0, c.width, c.height).data;
            for (let i = 0; i < pixels.length; i += 4) if (pixels[i] !== 0 || pixels[i + 1] !== 0 || pixels[i + 2] !== 255) return false;
            return true;
        }, imageUploads.at(-1).bytes.toString('base64'));
        assert.equal(onlyBlue, true, 'clipboard crop sends no pixels outside the selection');
        await page.locator('#setupApplyImage').click();
        assert.equal(await rows().count(), 7, 'OCR multi-website groups are expanded to individual rows');
        assert.equal(await rows().nth(6).locator('[data-website-id="100"]').isChecked(), true);
        await rows().first().locator('[data-prop="price"]').fill('1400');
        await page.evaluate(draft => ProductSetup.importDraft(draft), imageDraft);
        assert.equal(await rows().count(), 7);
        assert.equal(await rows().first().locator('[data-prop="price"]').inputValue(), '1400', 'reimport preserves existing edits');

        await start();
        await basic('Quick-add keeps draft');
        await page.evaluate(() => ProductSetup.importDraft({ versions: [{ name: 'Standard', groups: [
            { name: 'SET', websites: ['New Music Shop'], price: 1000, deposit: 500 }
        ] }] }));
        await rows().first().locator('[data-setup-action="add-website"][data-website-name]').click();
        failWebsite = true;
        await page.locator('#setupWebsiteSave').click();
        await page.locator('#setupWebsiteError:not([hidden])').waitFor();
        failWebsite = false;
        const savesBeforeAdd = submissions.length;
        await page.locator('#setupWebsiteSave').click();
        await page.locator('#setupWebsiteDialog').waitFor({ state: 'hidden' });
        assert.equal(submissions.length, savesBeforeAdd);
        assert.equal(await rows().first().locator('.setup-site-chip').innerText(), 'New Music Shop\n×');
        await page.locator('#setupArtistAddOutside').click();
        await page.locator('#setupArtistSave:enabled').waitFor();
        await page.locator('#setupArtistName').fill('New Artist');
        await page.locator('#setupArtistGroup').selectOption('2');
        await page.locator('#setupArtistSave').click();
        await page.locator('#setupArtistDialog').waitFor({ state: 'hidden' });
        assert.equal(await page.locator('#IdArtist').inputValue(), '22');
        assert.equal(await page.locator('#pName').inputValue(), 'Quick-add keeps draft');

        // Final six-row example and responsive/dark visual QA.
        await start();
        await basic('ENHYPEN THE SIN : PARALLEL WORLD');
        await page.evaluate(() => ProductSetup.importDraft({ versions: [
            { name: 'PHOTOBOOK VER.', groups: [{ name: 'เดี่ยว', websites: ['Ktown4u'], price: 590, deposit: 300 }, { name: 'SET', websites: ['Ktown4u'], price: 1750, deposit: 900 }] },
            { name: 'ENGENE VER.', groups: [{ name: 'เดี่ยว', websites: ['Ktown4u'], price: 400, deposit: 200 }, { name: 'DAY SET', websites: ['Ktown4u'], price: 2390, deposit: 1200 }, { name: 'NIGHT SET', websites: ['Ktown4u'], price: 2390, deposit: 1200 }] },
            { name: 'WEVERSE ALBUMS VER.', groups: [{ name: 'เดี่ยว', websites: ['Ktown4u'], price: 300, deposit: 150 }] }
        ] }));
        assert.equal(await rows().count(), 6);
        for (const theme of ['light', 'dark']) {
            await page.evaluate(theme => document.documentElement.dataset.theme = theme, theme);
            for (const width of [1440, 768, 390]) {
                await page.setViewportSize({ width, height: 1050 });
                await page.locator('#setupVariantsTitle').scrollIntoViewIfNeeded();
                assert.equal(await page.locator('#modalManageData .modal-body').evaluate(node => node.scrollWidth > node.clientWidth + 1), false);
                assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
                await page.screenshot({ path: path.join(output, `product-edit-table-${theme}-${width}.png`) });
            }
        }
        await page.setViewportSize({ width: 1440, height: 1050 });
        failSave = false;
        await page.locator('#setupSaveButton').click();
        await page.waitForFunction(() => !document.getElementById('modalManageData')?.classList.contains('show'));
        assert.equal(submissions.at(-1).versions.reduce((sum, v) => sum + v.groups.length, 0), 6);
        assert.deepEqual(submissions.at(-1).versions[1].groups.map(g => g.name), ['เดี่ยว', 'DAY SET', 'NIGHT SET']);
        assert.deepEqual(errors, [], 'no uncaught JavaScript errors');
        console.log('PASS: editable table, independent website rows, copy/delete/version moves, preserved IDs, validation/retry, clipboard/crop exact pixels, OCR import, stop/progress, quick-add website/artist, responsive + dark');
        console.log(`Screenshots: ${output}`);

    } finally {
        await browser.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => server.close());
