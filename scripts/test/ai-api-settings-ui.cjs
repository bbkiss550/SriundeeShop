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
let html = read('settings/ai.html');
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
    if (req.url === '/settings/ai') {
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
        const page=await browser.newPage({viewport:{width:1440,height:1100}});
        const errors=[];page.on('pageerror',error=>errors.push(error.message));
        let saved={provider:'OLLAMA',baseUrl:'http://localhost:11434',model:'',keyConfigured:false,allowRemote:false,timeoutSeconds:15};
        let failSave=false, failTest=false, failLoad=false;const tests=[],saves=[];
        await page.route('**/settings/ai/config',route=>{
            if(route.request().method()==='GET')return route.fulfill(failLoad?{status:500,json:{message:'Load failed'}}:{json:saved});
            const body=route.request().postDataJSON();saves.push(body);
            if(failSave)return route.fulfill({status:500,json:{message:'Save failed'}});
            saved={...body,keyConfigured:body.removeKey?false:!!body.apiKey||saved.keyConfigured};delete saved.apiKey;delete saved.removeKey;
            return route.fulfill({json:saved});
        });
        await page.route('**/settings/ai/test',route=>{
            tests.push(route.request().postDataJSON());
            return route.fulfill(failTest?{status:400,json:{message:'Connection failed'}}:{json:{connected:true,modelFound:false,models:['local-model','second-model'],message:'Connected'}});
        });
        await page.goto(`http://127.0.0.1:${server.address().port}/settings/ai`);
        await page.locator('#aiSettingsFields:enabled').waitFor({state:'attached'});
        assert.equal(await page.locator('#aiAllowRemote').isChecked(),false);
        await page.locator('#aiTest').click();await page.waitForFunction(()=>document.querySelectorAll('#aiModels option').length===2);
        assert.equal(tests.length,1);assert.equal(saves.length,0,'test does not save');
        await page.locator('#aiModel').fill('local-model');await page.locator('#aiKey').fill('dummy-secret');
        await page.locator('#aiShowKey').click();assert.equal(await page.locator('#aiKey').getAttribute('type'),'text');
        await page.locator('#aiSave').click();await page.waitForFunction(()=>document.getElementById('aiRemoveKeyRow').hidden===false);
        assert.equal(saves[0].apiKey,'dummy-secret');assert.equal(await page.locator('#aiKey').inputValue(),'');
        assert.equal(await page.locator('#aiKey').getAttribute('type'),'password');
        await page.locator('#aiRemoveKey').check();await page.locator('#aiSave').click();
        await page.waitForFunction(()=>document.getElementById('aiRemoveKeyRow').hidden===true);
        assert.equal(await page.locator('#aiKey').isEnabled(),true,'key field is usable again after deletion');
        failSave=true;await page.locator('#aiModel').fill('unsaved-model');await page.locator('#aiSave').click();
        await page.waitForFunction(()=>document.getElementById('aiSettingsStatus').textContent==='Save failed');
        assert.equal(await page.locator('#aiModel').inputValue(),'unsaved-model');
        failTest=true;await page.locator('#aiTest').click();await page.waitForFunction(()=>document.getElementById('aiSettingsStatus').textContent==='Connection failed');
        failSave=false;await page.locator('#aiReload').click();await page.locator('#aiSettingsFields:enabled').waitFor({state:'attached'});
        for(const theme of ['light','dark']){
            await page.evaluate(theme=>document.documentElement.dataset.theme=theme,theme);
            for(const width of [1440,768,390]){
                await page.setViewportSize({width,height:1100});
                assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
                await page.screenshot({path:path.join(output,`ai-settings-${theme}-${width}.png`),fullPage:true});
            }
        }
        failLoad=true;await page.reload();await page.locator('#aiRetry:not([hidden])').waitFor();
        assert.equal(await page.locator('#aiSettingsFields').evaluate(node=>node.disabled),true);
        failLoad=false;await page.locator('#aiRetry').click();await page.locator('#aiSettingsFields:enabled').waitFor({state:'attached'});
        await page.locator('#aiProvider').selectOption('LM_STUDIO');assert.equal(await page.locator('#aiBaseUrl').inputValue(),'http://localhost:1234/v1');
        await page.locator('#aiProvider').selectOption('GEMINI');
        assert.equal(await page.locator('#aiBaseUrl').inputValue(),'https://generativelanguage.googleapis.com/v1beta');
        assert.equal(await page.locator('#aiGeminiHint').isVisible(),true);
        assert.equal(await page.locator('#aiAllowRemote').isChecked(),false,'provider change preserves local-only preference');
        assert.equal(await page.locator('#aiKey').getAttribute('required'),'');
        await page.locator('#aiKey').fill('dummy-gemini-key');await page.locator('#aiAllowRemote').check();await page.locator('#aiModel').fill('gemini-test-model');
        await page.locator('#aiSave').click();await page.locator('#aiSettingsFields:enabled').waitFor({state:'attached'});
        assert.equal(saves.at(-1).provider,'GEMINI');assert.equal(saves.at(-1).allowRemote,true);
        assert.equal(await page.locator('#aiKey').inputValue(),'');
        await page.screenshot({path:path.join(output,'ai-settings-gemini.png'),fullPage:true});
        assert.deepEqual(errors,[]);
        console.log('PASS: local defaults, model list, test/save separation, key hiding/deletion, retained edits on errors, retry, light/dark and responsive layouts');
    }finally{await browser.close();server.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
