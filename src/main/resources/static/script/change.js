let myDataTable;

document.addEventListener("DOMContentLoaded", function() {
    myDataTable = new simpleDatatables.DataTable("#table2", {
        searchable: true,
        fixedColumns: false,
        perPage: 25
    });
});

function select_os() {
    const selected_os = document.querySelector('input[name="group-os"]:checked');
    const searchInput = document.querySelector(".dataTable-input");
    
    if (selected_os && searchInput && myDataTable) {
        const val = (selected_os.value === "all") ? "" : selected_os.value;
        
        // 1. ยัดค่าลงไปในช่องค้นหาตาม Concept บอส
        searchInput.value = val;

        // 2. สร้าง Event 'keyup' จำลองขึ้นมา เพื่อบอกให้ Library รู้ว่า "มีคนพิมพ์แล้วนะ!"
        const event = new Event('keyup', { bubbles: true });
        searchInput.dispatchEvent(event);
        
        // หรือใช้วิธีเรียกตรงๆ ผ่านตัวแปรตาราง (ชัวร์กว่า)
        myDataTable.search(val);
    }
}