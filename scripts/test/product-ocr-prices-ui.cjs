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
        sites.push({id: 50, name: 'Weverse Shop', delete: 'A'});
        await page.goto(url);
        await page.evaluate(() => new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="2"]').click();
        const expected = [['590','300'],['1750','900'],['400','200'],['2390','1200'],['2390','1200'],['300','150']];
        for (const report of ['full-set-draft.json', 'right-column-draft.json', 'ai-image-draft.json']) {
            const draft = JSON.parse(fs.readFileSync(path.join(root, '.runtime/product-setup-qa', report), 'utf8'));
            await page.evaluate(draft => ProductSetup.importDraft(draft), draft);
            const pairs = await page.locator('.setup-price-group').evaluateAll(rows => rows.map(row =>
                ['price','deposit'].map(prop => row.querySelector(`[data-prop="${prop}"]`).value)));
            assert.deepEqual(pairs, expected, report);
            assert.equal(await page.locator('.pricing-tag').first().innerText(), 'Weverse Shop\n×');
        }
        await page.locator('[data-setup-step="1"]').click();
        await page.locator('#pName').fill('OCR regression');
        await page.locator('#IdType').selectOption('1');
        await page.locator('#IdArtist').selectOption('1',{force:true});
        await page.locator('[data-setup-step="2"]').click();
        await page.locator('#setupSaveButton').click();
        await page.locator('#productSetupError:not([hidden])').waitFor();
        assert.equal(submissions.length, 1);
        assert.deepEqual(submissions[0].versions.flatMap(v => v.groups.map(g => [String(g.price),String(g.deposit)])), expected);
        await page.reload();
        await page.evaluate(() => new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="2"]').click();
        await page.evaluate(() => ProductSetup.importDraft({versions:[{name:'PHOTOBOOK VER.',groups:[
            {name:'เดี่ยว',websites:['Weverse Shop'],price:null,deposit:300}
        ]}]}));
        const row = page.locator('.setup-price-group').first();
        assert.equal(await row.locator('[data-prop="price"]').inputValue(), '');
        assert.equal(await row.locator('[data-prop="deposit"]').inputValue(), '300');
        assert.equal(await row.locator('[data-cover-price]').textContent(), '—');
        assert.equal(await row.locator('[data-balance]').textContent(), '—');
        await page.locator('[data-setup-step="1"]').click();
        await page.locator('#pName').fill('Uncertain OCR');
        await page.locator('#IdType').selectOption('1');
        await page.locator('#IdArtist').selectOption('1',{force:true});
        await page.locator('[data-setup-step="2"]').click();
        await page.locator('#setupSaveButton').click();
        await page.locator('#productSetupError:not([hidden])').waitFor();
        assert.equal(submissions.length, 1, 'uncertain OCR price cannot be submitted');
        assert.deepEqual(errors, []);
        console.log('PASS: all six OCR prices/deposits survive import, duplicate import, collapsed fields and save payload');
    } finally {
        await browser.close();
        server.close();
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
