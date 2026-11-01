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
        page.on('dialog',dialog=>dialog.accept());
        await page.evaluate(()=>new_data());
        await page.locator('#setupSaveButton:enabled').waitFor({state:'attached'});
        await page.locator('[data-setup-step="2"]').click();
        await page.evaluate(()=>ProductSetup.importDraft({versions:[
            {name:'PHOTOBOOK',groups:[{name:'Single',websites:['Ktown4u'],price:590,deposit:300},{name:'SET',websites:['Ktown4u'],price:1750,deposit:900}]},
            {name:'ENGENE',groups:[{name:'DAY SET',websites:['Ktown4u'],price:2390,deposit:1200},{name:'NIGHT SET',websites:['Ktown4u'],price:2390,deposit:1200}]},
            {name:'OTHER',groups:[{name:'Other cover',websites:['YES24'],price:400,deposit:200}]}
        ]}));
        const groups=()=>page.locator('.pricing-group');
        const versions=()=>groups().first().locator('.pricing-version');
        const covers=()=>versions().first().locator('.pricing-cover');
        const expanded=locator=>locator.evaluateAll(nodes=>nodes.filter(n=>n.dataset.expanded==='true').length);
        const foldingRequests=[];
        page.on('request',request=>{if(request.url().includes('/product/setup/'))foldingRequests.push(request.url());});
        const toggle=async locator=>locator.locator(':scope > summary').click({position:{x:10,y:10}});
        const settled=()=>page.waitForTimeout(330);
        assert.equal(await expanded(groups()),1);
        await toggle(groups().first());
        await page.waitForTimeout(60);
        const closingGroup=await groups().first().locator(':scope > .pricing-motion-body').evaluate(n=>({height:n.getBoundingClientRect().height,total:n.scrollHeight}));
        assert.ok(closingGroup.height>0 && closingGroup.height<closingGroup.total,JSON.stringify(closingGroup));
        await settled();
        await toggle(groups().first());
        await page.waitForTimeout(60);
        const openingGroup=await groups().first().locator(':scope > .pricing-motion-body').evaluate(n=>({height:n.getBoundingClientRect().height,total:n.scrollHeight}));
        assert.ok(openingGroup.height>0 && openingGroup.height<openingGroup.total,JSON.stringify(openingGroup));
        await settled();
        await toggle(versions().first());
        await page.waitForTimeout(60);
        const openingVersion=await versions().first().locator(':scope > .pricing-motion-body').evaluate(n=>({height:n.getBoundingClientRect().height,total:n.scrollHeight}));
        assert.ok(openingVersion.height>0 && openingVersion.height<openingVersion.total,JSON.stringify(openingVersion));
        await settled();
        await toggle(covers().first());
        await page.waitForTimeout(70);
        const moving=await covers().first().locator(':scope > .pricing-motion-body').evaluate(n=>({height:n.getBoundingClientRect().height,total:n.scrollHeight,animations:n.getAnimations().length}));
        assert.ok(moving.height>0 && moving.height<moving.total,JSON.stringify(moving));
        assert.equal(moving.animations,1);
        await settled();
        await covers().first().locator('[data-prop="price"]').fill('610');
        await covers().first().locator('[data-prop="name"]').fill('Edited single');
        assert.equal(await covers().first().locator('[data-balance]').innerText(),'310.00');
        assert.equal(await covers().first().locator('[data-cover-title]').innerText(),'Edited single');
        await toggle(covers().nth(1)); await settled();
        assert.equal(await expanded(covers()),1);
        assert.equal(await covers().first().evaluate(n=>n.open),false);
        assert.equal(await covers().first().locator('[data-prop="price"]').inputValue(),'610');
        await toggle(covers().nth(1)); await settled(); assert.equal(await expanded(covers()),0);
        await toggle(versions().nth(1)); await settled();
        assert.equal(await expanded(versions()),1);
        assert.equal(await expanded(covers()),0);
        await toggle(versions().nth(1).locator('.pricing-cover').first()); await settled();
        await toggle(groups().nth(1)); await settled();
        assert.equal(await expanded(groups()),1);
        assert.equal(await expanded(versions()),0);
        assert.equal(await expanded(groups().first().locator('.pricing-cover')),0);
        await toggle(groups().first()); await settled();
        assert.equal(await expanded(versions()),0);
        await toggle(versions().first()); await settled();
        await toggle(covers().first()); await settled();
        assert.equal(await covers().first().locator('[data-prop="price"]').inputValue(),'610');
        // Inputs and copy buttons do not toggle their enclosing headers.
        await covers().first().locator('[data-prop="deposit"]').click();
        assert.equal(await expanded(covers()),1);
        await covers().first().locator('[data-setup-action="copy-row"]').click();
        await settled();
        assert.equal(await versions().first().locator('.pricing-cover').count(),3);
        assert.equal(await expanded(versions().first().locator('.pricing-cover')),1);
        await page.locator('[data-setup-action="collapse-all"]').click(); await settled();
        assert.equal(await expanded(page.locator('[data-fold]')),0);
        await page.locator('[data-setup-action="open-first"]').click(); await settled();
        assert.equal(await expanded(groups()),1);
        assert.equal(await expanded(page.locator('.pricing-version')),0);
        // Rapid reversals must settle without leftover animations or multiple expanded nodes.
        await groups().first().evaluate(async node=>{for(let i=0;i<9;i++){node.querySelector(':scope > summary').click();await new Promise(r=>setTimeout(r,25));}});
        await settled();
        assert.equal(await expanded(groups()),0);
        assert.equal(await page.locator('.pricing-motion-body').evaluateAll(nodes=>nodes.reduce((n,e)=>n+e.getAnimations().length,0)),0);
        await page.locator('#pricingSearch').fill('SET');
        await page.waitForFunction(()=>document.querySelectorAll('.pricing-cover[data-expanded="true"]').length===1);
        assert.equal(await expanded(groups()),1);
        assert.equal(await expanded(page.locator('.pricing-version')),1);
        await page.locator('#pricingSearch').fill(''); await page.waitForTimeout(250);
        assert.equal(await expanded(groups()),0);
        await page.emulateMedia({reducedMotion:'reduce'});
        await toggle(groups().first());
        assert.equal(await groups().first().evaluate(n=>n.open),true);
        await page.waitForTimeout(60);
        assert.ok(await page.locator('.pricing-motion-body').evaluateAll(nodes=>nodes.some(e=>e.getAnimations().length>0)), 'motion starts automatically without an enable button');
        assert.equal(await page.locator('[data-setup-action="enable-motion"]').count(),0);
        await settled();
        assert.equal(submissions.length,0,'accordion changes never save or fetch a product');
        assert.deepEqual(foldingRequests,[]);
        await toggle(versions().first());
        await toggle(covers().nth(1));
        await covers().nth(1).locator('[data-prop="name"]').fill('Copy cover');
        await page.locator('[data-setup-step="1"]').click();
        await page.locator('#pName').fill('Accordion draft');
        await page.locator('#IdType').selectOption('1');
        await page.locator('#IdArtist').selectOption('1',{force:true});
        await page.locator('[data-setup-step="2"]').click();
        await page.locator('#setupSaveButton').click();
        await page.locator('#productSetupError:not([hidden])').waitFor();
        assert.equal(submissions.length,1);
        assert.equal(submissions[0].versions[0].groups[0].price,'610');
        assert.equal(submissions[0].versions[0].groups[1].name,'Copy cover');
        await covers().nth(1).locator('[data-setup-action="remove-row"]').click();
        await page.locator('.swal2-cancel').click();
        await page.locator('.swal2-container').waitFor({state:'hidden'});
        assert.equal(await covers().count(),3);
        assert.equal(await expanded(covers()),1,'cancel delete does not toggle accordion');
        await covers().nth(1).locator('[data-setup-action="remove-row"]').click();
        await page.locator('.swal2-confirm').click();
        await page.locator('.swal2-container').waitFor({state:'hidden'});
        assert.equal(await covers().count(),2);
        assert.equal(await groups().count(),2);
        await toggle(covers().first());
        await page.waitForTimeout(60);
        assert.equal(await covers().first().locator(':scope > .pricing-motion-body').evaluate(n=>n.getAnimations().length),1,'motion remains automatic even when OS reduced motion is enabled');
        await settled();
        assert.deepEqual(errors,[]);
        console.log('PASS: three-level single-open, reset descendants, editable cover summaries, draft preservation, slide animation/reversal, search invariant, controls and automatic motion');
    } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;}).finally(()=>server.close());
