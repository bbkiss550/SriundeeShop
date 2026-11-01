# Commit ภาษาไทยและคำสั่งรันระบบ

วันที่ 2 พฤศจิกายน 2569 · ใช้ PowerShell ใน Windows

## ข้อความ Commit

```text
ปรับหน้าตั้งค่าสินค้าและเพิ่มอ่านภาพ/ข้อความด้วย AI พร้อมนำเข้าคำสั่งซื้อ Excel

- รวมการตั้งค่าสินค้าและราคาแบบกลุ่มเว็บ เวอร์ชัน และปกใน Modal เดียว
- เพิ่มค้นหาและเพิ่มประเภท ศิลปิน และเว็บไซต์จากฟอร์ม
- เพิ่ม Crop, วางภาพ/ข้อความ, Progress, ปุ่มหยุด และตารางตรวจผลก่อนเติมฟอร์ม
- เชื่อม AI API/Gemini ผ่าน Backend พร้อม OCR สำรองและตรวจโครงสร้างราคา
- แก้การอ่านคอลัมน์ วงเล็บ จำนวน SET เว็บไม่รู้จัก และขอบเขตไม่มีการ์ดเบเน
- กำหนดมัดจำที่ไม่ระบุเป็น 0 และยืนยันบันทึกสำเร็จด้วย SweetAlert
- เพิ่ม Excel Import พร้อมตรวจ Mapping และ reject ทั้งไฟล์เมื่อมีแถวผิด
- เพิ่มชุดทดสอบและเอกสารส่งมอบงาน
```

Commit นี้ไม่รวมรหัสผ่าน localhost, Log, Database Backup และงานแก้สถานะคำสั่งซื้อที่มีอยู่เดิมซึ่งอยู่นอกขอบเขตรอบนี้ ตรวจรายการจริงด้วย `git show --stat HEAD`

ภายหลัง Commit งานหลัก ผู้ใช้ให้เพิ่มไฟล์ `codex/dump-sriundee_shop-202611020136.backup` เข้า Git ด้วย จึงเพิ่มเป็น Commit แยก ไฟล์นี้ยังไม่ได้ถูกนำไป Restore หรือเขียนทับฐานข้อมูลจากการเพิ่มเข้า Git

ผู้ใช้ให้รวมงานที่คงค้างเพิ่มเติม จึง Commit โค้ดสถานะยกเลิกคำสั่งซื้อและการกรองรายการยกเลิกออกจากรายงาน/ล็อต/ปฏิทินในรอบถัดมา พร้อมค่าปลายทาง localhost โดยไม่ใส่รหัสผ่านลง Git และตั้งให้ข้ามไฟล์ Log ที่เกิดจากการรัน

## เปิดอ่านเอกสาร

```powershell
Set-Location 'D:\PreorderShop\git\SriundeeShop'
Start-Process '.\codex\HANDOFF-2026-11-02.md'
```

## ตั้งค่าเชื่อมต่อ localhost สำหรับ Terminal ที่จะรัน

คำสั่งนี้บังคับปลายทางเป็นฐานข้อมูลในเครื่อง ไม่ต้องใส่รหัสผ่านลงเอกสารหรือ Commit และต้องตั้งใหม่เมื่อเปิด Terminal ใหม่

```powershell
Set-Location 'D:\PreorderShop\git\SriundeeShop'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/sriundee_shop'
$env:SPRING_DATASOURCE_USERNAME = 'postgres'
$shopDbPassword = Read-Host 'รหัสผ่าน PostgreSQL localhost' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $shopDbPassword).Password
```

ต้องมีฐานข้อมูล `sriundee_shop` และตารางของระบบก่อนรัน การเปิดแอปอาจเรียก Initializer ที่ปรับข้อมูล/โครงสร้างบางส่วนตามโค้ดเดิม

ในเครื่องเดิม รักษาค่ารหัสผ่านไว้ที่ `.runtime/local-db-private.properties` ซึ่งถูก Git ข้ามอยู่แล้ว โปรไฟล์ `local` จะอ่านไฟล์นี้ถ้ามี ต้องรันจากโฟลเดอร์โปรเจกต์ ส่วนเครื่องใหม่ใช้ Environment Variables ตามคำสั่งด้านบน หรือกำหนด `SHOP_DB_PASSWORD` โดยไม่ใส่ค่าจริงลง Git

## ทดสอบ

ใช้ชุดทดสอบเฉพาะส่วน แทนการรัน SpringBootTest ทั้งระบบกับฐานข้อมูลใช้งานจริง

```powershell
.\mvnw.cmd '-Dtest=Product*Test,*QuickAdd*Test,Ai*Test,OrderExcel*Test,LocalProductImageReaderTest' test
python scripts/test/test_ocr_layout.py
```

ผลตรวจรวมก่อน Commit: 142 tests, 0 failures, 0 errors, 8 optional tests skipped

สำหรับ UI Test ต้องมี Playwright ใน `.runtime/product-setup-qa/node_modules` และ Fixture ที่สร้างจาก Parser tests ก่อน

```powershell
node scripts/test/product-shared-shops-ui.cjs
```

## เตรียม OCR ในเครื่องใหม่

ต้องมี Python และติดตั้งโมเดลก่อนใช้งาน OCR สำรอง ขั้นตอนเตรียมโมเดลมีการดาวน์โหลดไฟล์ ส่วน OCR ขณะอ่านภาพทำงานภายในเครื่อง

```powershell
python -m pip install -r scripts/ocr/requirements.txt
python scripts/ocr/prepare_models.py
```

## หยุดเซิร์ฟเวอร์เดิมของโปรเจกต์นี้ก่อน Build

บน Windows ไฟล์ JAR ที่กำลังใช้งานอาจถูกล็อก หากรันแอปใน Terminal ให้กด Ctrl+C ใน Terminal นั้น หรือใช้คำสั่งที่กรองเฉพาะแอป/โปรไฟล์/พอร์ตนี้

```powershell
$shopProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object {
        $_.CommandLine -like '*-jar target/preorder-0.0.1-SNAPSHOT.jar*' -and
        $_.CommandLine -like '*--spring.profiles.active=local*' -and
        $_.CommandLine -like '*--server.port=8080*'
    }
$shopProcesses | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

## Build และ Run

รันใน Terminal เดียวกับที่ตั้ง Environment Variables ของฐานข้อมูลไว้

```powershell
.\mvnw.cmd -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Build ไม่สำเร็จ กรุณาตรวจข้อความ Error' }
java -jar target/preorder-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.port=8080
```

เมื่อเห็น `Started PreorderApplication` ให้เปิด http://localhost:8080

ตรวจสถานะจากอีก Terminal:

```powershell
(Invoke-WebRequest 'http://localhost:8080/login' -UseBasicParsing).StatusCode
```

ค่าที่คาดหวัง: `200`

## ตรวจ Commit และไฟล์ที่ยังไม่ได้ Commit

```powershell
git log -1 --format=fuller
git show --stat HEAD
git status --short
```

ไม่ได้ Push ไป Remote อัตโนมัติ หากต้องการส่งขึ้น Remote ให้ตรวจ Branch และปลายทางก่อน

รายละเอียดฟังก์ชัน ผลทดสอบ ข้อจำกัด และไฟล์หลักอยู่ใน [HANDOFF-2026-11-02.md](HANDOFF-2026-11-02.md)
