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
        let failText = false;
        const imageUploads = [];
        let nextImageDraft = imageDraft;
        let failWebsite = false;
        let failArtist = false;
        const artistAdds = [];
        const websiteAdds = [];
        let failType = false;
        const typeAdds = [];
        await page.route('**/product/setup/**', async route => {
            const request = route.request();
            if (request.url().endsWith('/types')) {
                typeAdds.push(request.postDataJSON());
                if (failType) return route.fulfill({ status: 500, json: { message: 'เพิ่มประเภทไม่สำเร็จ' } });
                return route.fulfill({ json: { id: 44, name: request.postDataJSON().name, delete: 'A' } });
            }
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
                assert.equal(request.postDataJSON().text, nextImageDraft.rawText);
                if (failText) return route.fulfill({ status: 503, json: { message: 'จัดกลุ่มไม่สำเร็จ ลองอีกครั้ง' } });
                return route.fulfill({ json: nextImageDraft });
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
        sites.push({id:50,name:'Applemusic',delete:'A'},{id:51,name:'Soundwave',delete:'A'});
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/shared-shop-draft.json'),'utf8'));
        nextImageDraft.readSource='OCR';nextImageDraft.readMessage='OCR local; AI not enabled';
        await page.goto(url);await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();
        await page.locator('[data-import-source="text"]').click();
        assert.equal(await page.locator('#setupImageSource').isVisible(),false);
        await page.locator('#setupReadText').click();
        assert.match(await page.locator('#setupImageError').textContent(),/กรุณาวางข้อความ/);
        assert.equal(textParses,0,'empty text never reaches the server');
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);
        nextImageDraft={...nextImageDraft,readSource:'TEXT',readMessage:'จัดกลุ่มจากข้อความด้วยกฎในระบบ'};
        failText=true;
        await page.locator('#setupReadText').click();
        await page.waitForFunction(()=>document.getElementById('setupImageError').textContent.includes('ลองอีกครั้ง'));
        assert.equal(await page.locator('#setupTextInput').inputValue(),nextImageDraft.rawText);
        assert.equal(await page.locator('#setupImageResult').isVisible(),false);
        failText=false;
        await page.locator('#setupReadText').click();
        await page.locator('#setupImageResult:not([hidden])').waitFor();
        assert.equal(await page.locator('#setupResultRows tr').count(),7,'text shows all offers for review before import');
        assert.equal(imageReads,0,'text needs no image or OCR');
        assert.equal(submissions.length,0,'text analysis does not save products');
        assert.equal(await page.locator('#setupImageReadMethod a').count(),0,'text mode has no previous OCR source');
        assert.equal(await page.locator('#setupOcrText').inputValue(),nextImageDraft.rawText);
        for(const width of [1440,768,390]){
            await page.setViewportSize({width,height:1050});
            await page.screenshot({path:path.join(output,`text-import-${width}.png`)});
            const size=await page.locator('#setupTextInput').boundingBox();
            assert.ok(size.width>200 && size.x>=0 && size.x+size.width<=width,'text field fits viewport');
        }
        await page.setViewportSize({width:1440,height:1050});
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText+'\n');
        assert.equal(await page.locator('#setupImageResult').isVisible(),false,'editing invalidates stale results');
        await page.locator('[data-import-source="image"]').click();
        await page.locator('[data-import-source="text"]').click();
        assert.equal(await page.locator('#setupTextInput').inputValue(),nextImageDraft.rawText+'\n','switching preserves typed text');
        await page.locator('[data-import-source="image"]').click();
        nextImageDraft={...nextImageDraft,readSource:'OCR',readMessage:'OCR local; AI not enabled'};
        await page.locator('#setupImageFile').setInputFiles(path.join(root,'.runtime/product-setup-qa/screenshots/pricing-theme-light.png'));
        await page.locator('#setupReadImage:enabled').waitFor({state:'attached'});await page.locator('#setupReadImage').click();
        await page.locator('#setupImageResult:not([hidden])').waitFor();
        assert.match(await page.locator('#setupImageReadMethod').textContent(),/OCR local; AI not enabled/);
        assert.equal(await page.locator('#setupImageReadMethod a').getAttribute('href'),'/settings/ai');
        const ocrDetails=page.locator('.setup-ocr-text');await ocrDetails.locator('summary').click();
        nextImageDraft={...nextImageDraft,readSource:'TEXT',readMessage:'Reparsed locally'};
        await page.locator('#setupParseText').click();
        await page.waitForFunction(()=>document.getElementById('setupImageReadMethod').textContent.includes('จัดกลุ่มข้อความที่แก้ไข'));
        assert.match(await page.locator('#setupImageReadMethod').textContent(),/OCR local/,'reparse retains the real image-reading method');
        await page.locator('#setupApplyImage').click();
        assert.equal(await page.locator('.pricing-group').count(),1);assert.equal(await page.locator('.pricing-version').count(),4);
        const rows=page.locator('.setup-price-group');assert.equal(await rows.count(),7);
        assert.deepEqual(await rows.locator('[data-prop="name"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['สุ่ม','SET(3)','สุ่ม','SET(6)','เดี่ยว','สุ่ม','SET(6)']);
        const pairs=await rows.evaluateAll(nodes=>nodes.map(row=>['price','deposit'].map(prop=>row.querySelector(`[data-prop="${prop}"]`).value)));
        assert.deepEqual(pairs,[['550','300'],['1590','900'],['360','150'],['2100','1000'],['1050','600'],['360','150'],['2100','1000']]);
        assert.equal(await page.locator('.pricing-tag').count(),3);
        await page.locator('[data-setup-step="1"]').click();await page.locator('#pName').fill('Shared shops');
        await page.locator('#setupTypeAdd').click();
        await page.locator('#setupTypeSave').click();
        assert.equal(typeAdds.length,0,'empty type is not submitted');
        await page.locator('#setupTypeName').fill('NEW TYPE');
        failType=true;await page.locator('#setupTypeSave').click();
        await page.locator('#setupTypeError:not([hidden])').waitFor();
        assert.equal(await page.locator('#pName').inputValue(),'Shared shops');
        failType=false;await page.locator('#setupTypeSave').click();
        await page.waitForFunction(()=>!document.getElementById('setupTypeDialog').open);
        assert.equal(await page.locator('#IdType').inputValue(),'44');
        assert.equal(await page.locator('#IdType option[value="44"]').textContent(),'NEW TYPE');
        await page.locator('#setupArtistAddOutside').click();
        await page.locator('#setupArtistGroup option[value="2"]').waitFor({state:'attached'});
        await page.locator('#setupArtistName').fill('NEW ARTIST');
        await page.locator('#setupArtistGroup').selectOption('2');
        await page.locator('#setupArtistSave').click();
        await page.waitForFunction(()=>!document.getElementById('setupArtistDialog').open);
        assert.equal(await page.locator('#IdArtist').inputValue(),'22');
        for(const theme of ['light','dark']){
            await page.evaluate(value=>document.documentElement.setAttribute('data-theme',value),theme);
            for(const width of [1440,768,390]){
                await page.setViewportSize({width,height:1050});
                await page.screenshot({path:path.join(output,`classic-fields-${theme}-${width}.png`)});
                const aligned=await page.locator('label[for="end_date"]').evaluate(label=>{
                    const date=document.getElementById('end_date'),check=document.getElementById('end_date_unscheduled').closest('label');
                    const rect=node=>node.getBoundingClientRect();
                    return Math.abs(rect(label).top-rect(date).top)<1 && Math.abs(rect(date).top-rect(check).top)<1 && rect(check).right<=innerWidth;
                });
                assert.ok(aligned,'date label, field and checkbox share one row');
            }
        }
        await page.setViewportSize({width:1440,height:1050});
        await page.evaluate(()=>document.documentElement.setAttribute('data-theme','light'));
        await page.locator('#IdType').selectOption('1');await page.locator('#IdArtist').selectOption('1',{force:true});
        await page.locator('[data-setup-step="2"]').click();await page.locator('#setupSaveButton').click();
        await page.locator('#productSetupError:not([hidden])').waitFor();assert.equal(submissions.length,1);
        const offers=submissions[0].versions.flatMap(version=>version.groups);
        assert.equal(offers.length,7);for(const offer of offers)assert.deepEqual([...offer.websiteIds].sort((a,b)=>a-b),[1,50,51]);
        await page.locator('[data-setup-step="0"]').click();
        await page.locator('[data-import-source="text"]').click();
        assert.equal(await page.locator('#setupImageResult').isVisible(),false);
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);
        await page.locator('#setupReadText').click();
        await page.locator('#setupImageResult:not([hidden])').waitFor();
        assert.equal(await page.locator('#setupImageReadMethod a').count(),0);
        await page.locator('#setupApplyImage').click();
        assert.equal(await page.locator('.setup-price-group').count(),7,'text import retains existing rows without duplicates');
        assert.equal(submissions.length,1,'applying text still requires explicit save');
        page.once('dialog',dialog=>dialog.accept());
        await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();
        await page.locator('[data-import-source="text"]').click();
        nextImageDraft={productName:null,versions:[{name:'Standard',groups:[
            {name:'MEMBERSHIP',websites:['ไม่มีเว็บ'],price:610,deposit:0},
            {name:'KIT',websites:['ไม่มีเว็บ'],price:900,deposit:0},
            {name:'MEMBERSHIP + KIT',websites:['ไม่มีเว็บ'],price:1490,deposit:0}
        ]}],warnings:[],readSource:'TEXT',readMessage:'จัดกลุ่มจากข้อความด้วยกฎในระบบ',
        rawText:'[🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)MEMBERSHIP 610 [🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)KIT 900 [🤍](https://abs.twimg.com/emoji/v2/svg/1f90d.svg)MEMBERSHIP + KIT 1,490'};
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);
        await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        const previewRows=page.locator('#setupResultRows tr');
        assert.equal(await previewRows.count(),3);
        assert.deepEqual(await previewRows.first().locator('td').allTextContents(),['ไม่มีเว็บ','Standard','MEMBERSHIP','610.00','0.00','610.00']);
        for(const theme of ['light','dark']){
            await page.evaluate(value=>document.documentElement.setAttribute('data-theme',value),theme);
            for(const width of [1440,768,390]){
                await page.setViewportSize({width,height:1050});
                await page.locator('#setupResultTableWrap').scrollIntoViewIfNeeded();
                assert.equal(await page.locator('#modalManageData .modal-body').evaluate(node=>node.scrollWidth>node.clientWidth+1),false);
                await page.screenshot({path:path.join(output,`result-table-${theme}-${width}.png`)});
            }
        }
        await page.setViewportSize({width:1440,height:1050});
        await page.evaluate(()=>document.documentElement.setAttribute('data-theme','light'));
        await page.locator('#setupApplyImage').click();
        const membershipRows=page.locator('.setup-price-group');
        assert.equal(await membershipRows.count(),3,'text-only import replaces untouched starter');
        assert.deepEqual(await membershipRows.locator('[data-prop="name"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['MEMBERSHIP','KIT','MEMBERSHIP + KIT']);
        assert.deepEqual(await membershipRows.locator('[data-prop="price"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['610','900','1490']);
        assert.deepEqual(await membershipRows.locator('[data-prop="deposit"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['0','0','0']);
        assert.equal(submissions.length,1,'membership text is only staged, not saved');
        await page.locator('[data-setup-step="1"]').click();
        await page.locator('#pName').fill('Membership test');
        await page.locator('#IdType').selectOption('1');
        await page.locator('#IdArtist').selectOption('1',{force:true});
        await page.locator('[data-setup-step="2"]').click();
        await membershipRows.locator('[data-prop="deposit"]').evaluateAll(nodes=>nodes.forEach(node=>{
            node.value='100';node.dispatchEvent(new Event('input',{bubbles:true}));
        }));
        failSave=false;failLoad=true;
        await page.locator('#setupSaveButton').click();
        await page.waitForFunction(()=>document.querySelector('.swal2-popup') || !document.getElementById('productSetupError').hidden);
        assert.equal(await page.locator('#productSetupError').isVisible(),false,await page.locator('#productSetupError').textContent());
        await page.locator('.swal2-popup').waitFor({state:'visible'});
        assert.match(await page.locator('.swal2-title').textContent(),/บันทึกสินค้าสำเร็จ/);
        assert.equal(await page.locator('#modalManageData').isVisible(),false,'successful save closes the product modal');
        assert.equal(submissions.length,2,'success sends only one save request');
        await Promise.all([page.waitForEvent('load'),page.locator('.swal2-confirm').click()]);
        assert.equal(await page.locator('#modalManageData').isVisible(),false,'list reloads after success confirmation');
        sites.push({id:60,name:'Withmuu',delete:'A'},{id:61,name:'Jump Up',delete:'A'},{id:62,name:'Everline',delete:'A'});
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/explicit-cover-draft.json'),'utf8'));
        await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();
        await page.locator('[data-import-source="text"]').click();
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);
        await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        await page.locator('#setupApplyImage').click();
        const coverGroups=page.locator('.pricing-group');
        assert.equal(await coverGroups.count(),3,'cover directives keep three separate shop groups');
        assert.equal(await page.locator('.pricing-version').count(),3,'each shop group has only its Standard version');
        assert.deepEqual(await page.locator('.pricing-version [data-row-version]').evaluateAll(nodes=>nodes.map(node=>node.value)),['Standard','Standard','Standard']);
        assert.deepEqual(await coverGroups.nth(0).locator('[data-prop="name"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['Fallen Angel','Alt','SET(2)']);
        assert.deepEqual(await coverGroups.nth(1).locator('[data-prop="name"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['Fallen Angel','Alt']);
        assert.deepEqual(await coverGroups.nth(2).locator('[data-prop="name"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['สุ่ม (Fallen Angel / Alt)']);
        assert.equal(await page.locator('.setup-price-group').count(),6);
        assert.equal(submissions.length,2,'cover grouping does not save data');
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/bracket-version-draft.json'),'utf8'));
        sites.push({id:63,name:'Weverse Shop',delete:'A'});
        page.once('dialog',dialog=>dialog.accept());await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();await page.locator('[data-import-source="text"]').click();
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        assert.deepEqual(await page.locator('#setupResultRows tr td:nth-child(2)').allTextContents(),['CHEMICAL NIGHT [STUDIO CHOOM GIFT] VER.','COMPACT [STUDIO CHOOM GIFT] VER.','COMPACT [STUDIO CHOOM GIFT] VER.']);
        assert.equal(await page.locator('#setupResultRows tr').last().locator('td').nth(2).textContent(),'SET(5)');
        await page.locator('#setupApplyImage').click();
        assert.deepEqual(await page.locator('.pricing-version [data-row-version]').evaluateAll(nodes=>nodes.map(node=>node.value)),['CHEMICAL NIGHT [STUDIO CHOOM GIFT] VER.','COMPACT [STUDIO CHOOM GIFT] VER.']);
        assert.equal(submissions.length,2,'bracket parsing and import do not save data');
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/unknown-shop-draft.json'),'utf8'));
        page.once('dialog',dialog=>dialog.accept());await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();await page.locator('[data-import-source="text"]').click();
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        assert.deepEqual(await page.locator('#setupResultRows tr').first().locator('td').allTextContents(),['TBLshop','Standard','เดี่ยว','490.00','250.00','240.00']);
        await page.locator('#setupUnknownWebsites button').click();
        assert.equal(await page.locator('#setupWebsiteName').inputValue(),'TBLshop');
        await page.locator('#setupWebsiteSave').click();
        await page.waitForFunction(()=>!document.getElementById('setupWebsiteDialog').open);
        assert.equal(await page.locator('#setupUnknownWebsites').isVisible(),false);
        await page.locator('#setupApplyImage').click();
        assert.match(await page.locator('.pricing-tag').first().textContent(),/TBLshop/);
        assert.equal(submissions.length,2,'adding a missing shop and staging text never auto-saves a product');
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/two-column-draft.json'),'utf8'));
        sites.push({id:64,name:'FANS SHOP',delete:'A'},{id:65,name:'Musicplant',delete:'A'});
        page.once('dialog',dialog=>dialog.accept());await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();await page.locator('[data-import-source="text"]').click();
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        assert.equal(await page.locator('#setupResultRows tr').count(),14);
        assert.equal(await page.locator('#setupResultRows tr').filter({hasText:'Applemusic'}).count(),7);
        assert.equal(await page.locator('#setupResultRows tr').filter({hasText:'FANS SHOP'}).count(),7);
        await page.locator('#setupApplyImage').click();
        assert.equal(await page.locator('.pricing-group').count(),2);
        assert.equal(await page.locator('.pricing-group').nth(0).locator('.pricing-version').count(),5);
        assert.equal(await page.locator('.pricing-group').nth(1).locator('.pricing-version').count(),5);
        assert.deepEqual(await page.locator('.pricing-group').nth(0).locator('[data-prop="price"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['490','950','330','650','450','1090','1420']);
        assert.deepEqual(await page.locator('.pricing-group').nth(1).locator('[data-prop="price"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['550','1050','350','690','490','1150','1520']);
        nextImageDraft=JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa/no-benefit-draft.json'),'utf8'));
        sites.push({id:66,name:'KPOP TOGETHER',delete:'A'},{id:67,name:'Whosfan Store',delete:'A'});
        page.once('dialog',dialog=>dialog.accept());await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="0"]').click();await page.locator('[data-import-source="text"]').click();
        await page.locator('#setupTextInput').fill(nextImageDraft.rawText);await page.locator('#setupReadText').click();
        await page.locator('#setupApplyImage:enabled').waitFor({state:'visible'});
        assert.equal(await page.locator('#setupResultRows tr').count(),6);
        assert.equal(await page.locator('#setupResultRows tr').filter({hasText:'ไม่มีเว็บ'}).count(),4);
        assert.equal(await page.locator('#setupResultRows tr').filter({hasText:'Everline'}).count(),2);
        assert.deepEqual(await page.locator('#setupResultRows tr').filter({hasText:'SET(2)'}).locator('td:nth-child(n+3)').allTextContents(),['SET(2)','780.00','400.00','380.00']);
        await page.locator('#setupApplyImage').click();
        assert.equal(await page.locator('.pricing-group').count(),2);
        assert.deepEqual(await page.locator('.pricing-group').nth(0).locator('[data-prop="price"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['650','780']);
        assert.deepEqual(await page.locator('.pricing-group').nth(1).locator('[data-prop="price"]').evaluateAll(nodes=>nodes.map(node=>node.value)),['550','350','250','2220']);
        assert.deepEqual(errors,[]);
        console.log('PASS: image/text selection, text validation/retry/preservation/import, responsive text input, shared shops/prices and OCR provenance');
    }finally{await browser.close();server.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
