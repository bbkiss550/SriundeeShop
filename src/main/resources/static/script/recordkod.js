let myDataTable;
const selectedItems = new Map(); // key: id (string), value: data object

document.addEventListener("DOMContentLoaded", function () {
    myDataTable = new simpleDatatables.DataTable("#tableRecordKod", {
        searchable: true,
        fixedColumns: false,
        perPage: 50,
        perPageSelect: [50, 100, 150, 200]
    });

    // Event delegation — รองรับ checkbox ทั้งที่ render ตอนโหลดและหลัง search
    document.addEventListener("change", function (e) {
        if (e.target && e.target.classList.contains("row-check")) {
            handleCheckbox(e.target);
        }
    });
});

// ========== Checkbox Logic ==========

function handleCheckbox(checkbox) {
    const id = checkbox.value;

    if (checkbox.checked) {
        selectedItems.set(id, {
            id: id,
            customer: checkbox.dataset.customer,
            product: checkbox.dataset.product,
            web: checkbox.dataset.web,
            version: checkbox.dataset.version,
            cover: checkbox.dataset.cover,
            qty: checkbox.dataset.qty,
            status: checkbox.dataset.status,
            color: checkbox.dataset.color
        });
    } else {
        selectedItems.delete(id);
    }

    updateBadge();
    renderDraftTable();
}

function removeItem(id) {
    selectedItems.delete(id);
    // Uncheck checkbox ในตารางหลัก (ถ้าปรากฏอยู่)
    const cb = document.getElementById("check_" + id);
    if (cb) cb.checked = false;
    updateBadge();
    renderDraftTable();
}

function clearAll() {
    selectedItems.forEach((item, id) => {
        const cb = document.getElementById("check_" + id);
        if (cb) cb.checked = false;
    });
    selectedItems.clear();
    updateBadge();
    renderDraftTable();
}

// ========== Badge (floating button) ==========

function updateBadge() {
    const count = selectedItems.size;
    const btn = document.getElementById("draftBtn");
    const badge = document.getElementById("draftBadge");
    const badgeM = document.getElementById("draftCountModal");

    badge.textContent = count;
    if (badgeM) badgeM.textContent = count;

    btn.style.display = count > 0 ? "block" : "none";
}

// ========== Render ตารางทดใน Modal ==========

function renderDraftTable() {
    const tbody = document.getElementById("draftTableBody");
    const emptyRow = document.getElementById("draftEmptyRow");

    tbody.innerHTML = "";

    if (selectedItems.size === 0) {
        tbody.innerHTML = `
            <tr id="draftEmptyRow">
                <td colspan="8" class="text-center text-muted py-4">
                    <i class="bi bi-inbox fs-3 d-block mb-2"></i>
                    ยังไม่มีรายการที่เลือก
                </td>
            </tr>`;
        return;
    }

    selectedItems.forEach((item) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${item.customer}</td>
            <td>${item.product}</td>
            <td>${item.web}</td>
            <td>${item.version}</td>
            <td>${item.cover}</td>
            <td class="text-center">${item.qty}</td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-${item.color} btn-sm">
                    ${item.status}
                </button>
            </td>
            <td class="text-center">
                <button type="button" class="btn btn-outline-danger btn-sm"
                    onclick="removeItem('${item.id}')">
                    <i class="bi bi-x-lg"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ========== บันทึก (placeholder) ==========

function saveDraft() {
    if (selectedItems.size === 0) {
        alert("ยังไม่มีรายการที่เลือก");
        return;
    }
    // TODO: เพิ่ม logic บันทึกข้อมูลในภายหลัง
    alert("บันทึก " + selectedItems.size + " รายการ");
}

// ========== Search ==========

function search_data() {
    const artistId = document.getElementById("selectArtist").value;
    const websiteId = document.getElementById("selectWebsite").value;
    const customerName = document.getElementById("inputCustomer").value;

    let params = new URLSearchParams();
    if (artistId) params.append("artistId", artistId);
    if (websiteId) params.append("websiteId", websiteId);
    if (customerName) params.append("customerName", customerName);

    fetch("/recordkod/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            if (myDataTable && myDataTable.table) {
                const tbody = myDataTable.table.tBodies[0];
                if (tbody) {
                    tbody.innerHTML = html;

                    // Restore checked state สำหรับรายการที่เลือกไว้แล้ว
                    selectedItems.forEach((item, id) => {
                        const cb = document.getElementById("check_" + id);
                        if (cb) cb.checked = true;
                    });

                    myDataTable.refresh();
                }
            }
        })
        .catch(err => console.error("Search error:", err));
}

function reset_search() {
    document.getElementById("selectArtist").value = "";
    document.getElementById("selectWebsite").value = "";
    document.getElementById("inputCustomer").value = "";
    search_data();
}
