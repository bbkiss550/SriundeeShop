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
let html = read('order/list.html');
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
    if (req.url === '/orders') {
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

(async()=>{
    await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
    const browser=await chromium.launch({channel:'msedge',headless:true});
    try {
        const page=await browser.newPage({viewport:{width:1440,height:1080},acceptDownloads:true});
        const errors=[];page.on('pageerror',error=>errors.push(error.message));
        const fixture=name=>JSON.parse(fs.readFileSync(path.join(root,'.runtime/product-setup-qa',name),'utf8'));
        const good=fixture('orders-valid-report.json');const bad=fixture('orders-invalid-report.json');
        let next=bad,reads=0,validations=0,commits=0,slow=false;
        const plan=fixture('orders-ai-mapping-plan.json');
        await page.route('**/orders/import/inspect',async route=>{
            reads++;assert.ok(route.request().postDataBuffer().includes(Buffer.from('name="file"')));
            if(slow)await new Promise(resolve=>setTimeout(resolve,1000));
            return route.fulfill({json:{plan,mappingToken:'mapping-token'}}).catch(()=>{});
        });
        await page.route('**/orders/import/validate-mapping',route=>{validations++;const approval=route.request().postDataJSON();assert.equal(approval.token,'mapping-token');assert.equal(approval.mappings[0].columns['0'],'customer');return route.fulfill({json:{report:next,token:next.valid?'session-token':''}});});
        await page.route('**/orders/import/confirm',route=>{commits++;assert.equal(route.request().postDataJSON().token,'session-token');return route.fulfill({json:{orderCount:1,balance:290}});});
        await page.route('**/orders/search?*',route=>route.fulfill({body:''}));
        await page.goto(`http://127.0.0.1:${server.address().port}/orders`);
        await page.locator('#openOrderImport').click();
        await page.locator('#orderImportModal.show').waitFor();
        assert.equal(await page.getByText('เลือกไฟล์คำสั่งซื้อ',{exact:true}).count(),0);
        const choose=()=>page.locator('#orderImportFile').setInputFiles(path.join(root,'.runtime/product-setup-qa/orders-valid.xlsx'));
        const upload=async()=>{await choose();await page.locator('#orderImportMapping:not([hidden])').waitFor();await page.locator('#orderImportValidate:enabled').waitFor({state:'attached'});};
        await upload();
        assert.equal(validations,0,'column mapping is reviewed before any full data validation');
        await page.locator('[data-mapping-column="6"]').selectOption('customer');
        await page.locator('#orderImportValidate').click();await page.locator('#orderImportError:not([hidden])').waitFor();
        assert.equal(validations,0,'duplicate mappings cannot proceed');
        await page.locator('[data-mapping-column="6"]').selectOption('qty');await page.locator('#orderImportValidate').click();
        await page.waitForFunction(()=>document.getElementById('orderImportSummary').textContent.includes('ปฏิเสธทั้งไฟล์'));
        assert.equal(await page.locator('#orderImportIssues tr').count(),bad.issues.length);
        assert.equal(await page.locator('#orderImportConfirm').isVisible(),false);
        assert.equal(commits,0);
        const download=await Promise.all([page.waitForEvent('download'),page.locator('#orderImportDownload').click()]);
        assert.equal(download[0].suggestedFilename(),'order-import-errors.csv');
        for(const theme of ['light','dark']){
            await page.evaluate(theme=>document.documentElement.dataset.theme=theme,theme);
            await page.screenshot({path:path.join(output,`excel-import-errors-${theme}.png`),fullPage:true});
        }
        next=good;await page.locator('#orderImportValidate').click();
        await page.locator('#orderImportConfirm:not([hidden])').waitFor();
        assert.equal(await page.locator('#orderImportPreviewRows tr').count(),1);
        assert.equal(commits,0,'preview never saves');
        await page.locator('#orderImportConfirm').click();await page.locator('.swal2-cancel').click();
        assert.equal(commits,0,'cancelling confirmation never saves');
        await page.locator('#orderImportConfirm').click();await page.locator('.swal2-confirm').click();
        await page.waitForFunction(()=>document.getElementById('orderImportSummary').textContent.includes('นำเข้าสำเร็จ'));
        assert.equal(commits,1);assert.equal(await page.locator('#orderImportConfirm').isVisible(),false);
        await page.locator('#orderImportModal .modal-footer [data-bs-dismiss]').click();
        await page.locator('#orderImportModal').waitFor({state:'hidden'});
        await page.locator('#openOrderImport').click();slow=true;await choose();
        await page.locator('#orderImportModal .modal-footer [data-bs-dismiss]').click();
        await page.locator('#orderImportModal').waitFor({state:'hidden'});
        await page.waitForTimeout(1100);await page.locator('#openOrderImport').click();
        assert.equal(await page.locator('#orderImportResult').isVisible(),false,'late response cannot restore a closed draft');
        slow=false;next=bad;await upload();await page.locator('#orderImportValidate').click();
        await page.waitForFunction(()=>document.getElementById('orderImportSummary').textContent.includes('ปฏิเสธทั้งไฟล์'));
        await page.setViewportSize({width:390,height:844});
        await page.screenshot({path:path.join(output,'excel-import-errors-mobile.png'),fullPage:true});
        assert.equal(commits,1);assert.equal(reads,3);assert.equal(validations,3);assert.deepEqual(errors,[]);
        console.log('PASS: mapping review/edit, duplicate rejection, all row errors, CSV, preview, one save, stale response protection, light/dark/mobile');
    } finally {await browser.close();server.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
