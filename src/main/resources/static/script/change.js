let myDataTable;
let changeDataRequestId = 0;
let artistSearchTimer;
let lotSearchTimer;
let selectedStatusId = null;
let selectedStatusName = "";
const selectedRows = new Map();

document.addEventListener("DOMContentLoaded", function() {
    ensureLotColumnHeader();
    moveLotArriveDateField();
    initDataTable();
    bindCostPriceFormatter();
});

function ensureLotColumnHeader() {
    const table = document.getElementById("table2");
    if (!table) {
        return;
    }
    const headerRow = table.querySelector("thead tr");
    if (!headerRow || headerRow.querySelector("[data-lot-header='true']")) {
        return;
    }
    const headers = headerRow.querySelectorAll("th");
    const statusHeader = headers[headers.length - 1];
    const lotHeader = document.createElement("th");
    lotHeader.className = "status_width";
    lotHeader.dataset.lotHeader = "true";
    lotHeader.textContent = "LOT";
    headerRow.insertBefore(lotHeader, statusHeader);
}

function moveLotArriveDateField() {
    const arriveDate = document.getElementById("lotArriveDate");
    const shippingGroup = document.getElementById("shippingInputGroup");
    const shippingNote = document.getElementById("shippingNote");
    if (!arriveDate || !shippingGroup || !shippingNote) {
        return;
    }
    const arriveColumn = arriveDate.closest(".col-md-4");
    const noteColumn = shippingNote.closest(".col-12");
    if (arriveColumn && noteColumn && arriveColumn.parentElement !== shippingGroup) {
        shippingGroup.insertBefore(arriveColumn, noteColumn);
    }
}

function initDataTable() {
    myDataTable = new simpleDatatables.DataTable("#table2", {
        searchable: true,
        fixedColumns: false,
        perPage: 25
    });
    myDataTable.on("datatable.page", syncDataTableInfo);
    myDataTable.on("datatable.search", syncDataTableInfo);
    myDataTable.on("datatable.perpage", syncDataTableInfo);
    setTimeout(syncDataTableInfo, 0);
}

function destroyDataTable() {
    if (!myDataTable) {
        return;
    }

    const wrapper = myDataTable.wrapper;
    if (wrapper && wrapper.parentNode) {
        myDataTable.destroy();
    }

    myDataTable = null;
}

function resetSelectedRows() {
    selectedStatusId = null;
    selectedStatusName = "";
    selectedRows.clear();

    const checkAll = document.getElementById("checkAll");
    if (checkAll) {
        checkAll.checked = false;
    }

    updateSelectedAction();
}

function getSelectedOrderStatus() {
    const selectedStatus = document.querySelector('input[name="group-os"]:checked');
    return selectedStatus ? selectedStatus.value : "";
}

function getSelectedArtist() {
    const artistFilter = document.getElementById("artistFilter");
    return artistFilter ? artistFilter.value.trim() : "";
}

function getSelectedWebsite() {
    const websiteFilter = document.getElementById("websiteFilter");
    return websiteFilter ? websiteFilter.value : "";
}

function getSelectedLot() {
    const lotFilter = document.getElementById("lotFilter");
    return lotFilter ? lotFilter.value.trim() : "";
}

function search_artist() {
    clearTimeout(artistSearchTimer);
    artistSearchTimer = setTimeout(load_change_data, 300);
}

function search_lot() {
    clearTimeout(lotSearchTimer);
    lotSearchTimer = setTimeout(load_change_data, 300);
}

function syncDataTableInfo() {
    if (!myDataTable) {
        return;
    }

    const info = document.querySelector(".dataTable-info");
    if (!info) {
        return;
    }

    const total = (myDataTable.searching && myDataTable.searchData)
        ? myDataTable.searchData.length
        : (myDataTable.activeRows ? myDataTable.activeRows.length : 0);
    if (!total) {
        info.textContent = "";
        return;
    }

    const perPage = myDataTable.options.perPage || total;
    const currentPage = myDataTable.currentPage || 1;
    const start = ((currentPage - 1) * perPage) + 1;
    const end = Math.min(currentPage * perPage, total);
    info.textContent = "แสดง " + start + " ถึง " + end + " จาก " + total + " รายการ";
}

function load_change_data() {
    const requestId = ++changeDataRequestId;
    const params = new URLSearchParams();
    const orderStatus = getSelectedOrderStatus();
    const artist = getSelectedArtist();
    const website = getSelectedWebsite();
    const lot = getSelectedLot();

    if (orderStatus) {
        params.append("orderStatus", orderStatus);
    }

    if (artist) {
        params.append("artist", artist);
    }

    if (website) {
        params.append("website", website);
    }

    if (lot) {
        params.append("lot", lot);
    }

    fetch("/change/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            if (requestId !== changeDataRequestId) {
                return;
            }

            destroyDataTable();
            document.querySelector("#table2 tbody").innerHTML = html;
            resetSelectedRows();

            initDataTable();
            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading change data:", error);
        });
}

function toggle_all_rows() {
    const checkAll = document.getElementById("checkAll");
    const checkboxes = Array.from(document.querySelectorAll("#table2 tbody .row-check:not(:disabled)"));

    if (!checkAll.checked) {
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
        });
        resetSelectedRows();
        return;
    }

    let targetStatusId = selectedStatusId;
    let targetStatusName = selectedStatusName;
    if (!targetStatusId) {
        const firstCheckbox = checkboxes[0];
        if (firstCheckbox) {
            targetStatusId = firstCheckbox.dataset.statusId;
            targetStatusName = firstCheckbox.dataset.statusName;
        }
    }

    let skipped = 0;
    checkboxes.forEach(checkbox => {
        if (checkbox.dataset.statusId === targetStatusId) {
            checkbox.checked = true;
            addSelectedRow(checkbox);
        } else {
            checkbox.checked = false;
            skipped++;
        }
    });

    if (skipped > 0) {
        showStatusWarning(targetStatusName);
    }

    updateSelectedAction();
}

function select_os() {
    load_change_data();
}

function toggle_row_from_click(row, event) {
    if (event && event.target.closest("input, button, a, select, textarea, label")) {
        return;
    }

    const checkbox = row.querySelector(".row-check");
    if (!checkbox) {
        return;
    }

    checkbox.checked = !checkbox.checked;
    toggle_row_check(checkbox);
}

function toggle_row_check(checkbox) {
    if (checkbox.disabled || checkbox.dataset.statusId === "5") {
        checkbox.checked = false;
        showStatusWarning("ส่งของสำเร็จ");
        return;
    }

    if (checkbox.checked) {
        if (selectedStatusId && checkbox.dataset.statusId !== selectedStatusId) {
            checkbox.checked = false;
            showStatusWarning(selectedStatusName);
            return;
        }

        addSelectedRow(checkbox);
    } else {
        selectedRows.delete(checkbox.value);
        if (selectedRows.size === 0) {
            selectedStatusId = null;
            selectedStatusName = "";
        }
    }

    syncCheckAll();
    updateSelectedAction();
}

function addSelectedRow(checkbox) {
    const row = checkbox.closest("tr");
    const cells = row ? row.querySelectorAll("td") : [];

    selectedStatusId = checkbox.dataset.statusId;
    selectedStatusName = checkbox.dataset.statusName;
    selectedRows.set(checkbox.value, {
        id: parseInt(checkbox.value, 10),
        statusId: checkbox.dataset.statusId,
        statusName: checkbox.dataset.statusName,
        customer: cells[1] ? cells[1].textContent.trim() : "",
        artist: cells[2] ? cells[2].textContent.trim() : "",
        product: cells[3] ? cells[3].textContent.trim() : "",
        website: cells[4] ? cells[4].textContent.trim() : "",
        version: cells[5] ? cells[5].textContent.trim() : "",
        cover: cells[6] ? cells[6].textContent.trim() : "",
        qty: cells[7] ? cells[7].textContent.trim() : ""
    });
}

function syncCheckAll() {
    const checkAll = document.getElementById("checkAll");
    if (!checkAll) {
        return;
    }

    const checkboxes = Array.from(document.querySelectorAll("#table2 tbody .row-check:not(:disabled)"));
    const selectable = selectedStatusId
        ? checkboxes.filter(checkbox => checkbox.dataset.statusId === selectedStatusId)
        : checkboxes;

    checkAll.checked = selectable.length > 0 && selectable.every(checkbox => checkbox.checked);
}

function updateSelectedAction() {
    const button = document.getElementById("btnChangeSelected");
    const badge = document.getElementById("selected-count");
    const count = selectedRows.size;

    if (badge) {
        badge.textContent = count;
    }

    if (button) {
        button.style.display = count > 0 ? "flex" : "none";
    }
}

function showStatusWarning(statusName) {
    const message = "เลือกได้เฉพาะรายการที่มีสถานะเดียวกัน" + (statusName ? " (" + statusName + ")" : "");
    const stack = getStatusWarningStack();
    const alertBox = document.createElement("div");
    alertBox.className = "alert alert-danger alert-dismissible fade show change-status-alert";
    alertBox.setAttribute("role", "alert");

    const messageText = document.createElement("span");
    messageText.textContent = message;
    alertBox.appendChild(messageText);

    const closeButton = document.createElement("button");
    closeButton.type = "button";
    closeButton.className = "btn-close";
    closeButton.setAttribute("aria-label", "Close");
    closeButton.addEventListener("click", () => removeStatusWarningAlert(alertBox));
    alertBox.appendChild(closeButton);

    animateStatusWarningStack(stack, () => {
        stack.appendChild(alertBox);
    });

    alertBox.hideTimer = setTimeout(() => {
        removeStatusWarningAlert(alertBox);
    }, 5000);
}

function getStatusWarningStack() {
    let stack = document.getElementById("statusWarningStack");
    if (!stack) {
        stack = document.createElement("div");
        stack.id = "statusWarningStack";
        stack.className = "change-status-alert-stack";
        document.body.appendChild(stack);
    }
    return stack;
}

function animateStatusWarningStack(stack, updateStack) {
    const previousPositions = new Map();
    Array.from(stack.children).forEach(alert => {
        previousPositions.set(alert, alert.getBoundingClientRect());
    });

    updateStack();

    Array.from(stack.children).forEach(alert => {
        const previous = previousPositions.get(alert);
        if (!previous) {
            return;
        }

        const current = alert.getBoundingClientRect();
        const deltaY = previous.top - current.top;
        if (!deltaY) {
            return;
        }

        alert.style.transition = "none";
        alert.style.transform = "translateY(" + deltaY + "px)";
        alert.offsetHeight;
        requestAnimationFrame(() => {
            alert.style.transition = "";
            alert.style.transform = "";
        });
    });
}

function removeStatusWarningAlert(alertBox) {
    if (!alertBox || alertBox.classList.contains("is-hiding")) {
        return;
    }

    clearTimeout(alertBox.hideTimer);
    alertBox.classList.add("is-hiding");

    setTimeout(() => {
        const stack = alertBox.parentElement;
        if (!stack) {
            return;
        }

        alertBox.remove();
        if (!stack.children.length) {
            stack.remove();
        }
    }, 300);
}

function open_change_status_modal() {
    if (selectedRows.size === 0) {
        return;
    }

    if (selectedStatusId === "5") {
        showStatusWarning("ส่งของสำเร็จ");
        return;
    }

    document.getElementById("selectedStatusName").value = selectedStatusName;
    document.getElementById("selectedDetailCount").textContent = selectedRows.size;
    document.getElementById("selectedDetailList").innerHTML = buildSelectedDetailList();
    configureNewStatusOptions();

    const modalElement = document.getElementById("modalChangeStatus");
    let modal = bootstrap.Modal.getInstance(modalElement);
    if (!modal) {
        modal = new bootstrap.Modal(modalElement);
    }
    modal.show();
}

function buildSelectedDetailList() {
    let html = "";
    selectedRows.forEach(row => {
        html += "<div class='row gx-2 py-2 border-bottom'>";
        html += "<div class='col-md-3'><div class='fw-bold'>" + row.customer + "</div></div>";
        html += "<div class='col-md-8'>";
        html += "<div class='fw-bold text-dark'>" + row.product + "</div>";
        html += "<div class='text-muted small'>เว็บ : " + row.website + " เวอร์ชั่น : " + row.version + " ปก : " + row.cover + "</div>";
        html += "</div>";
        html += "<div class='col-md-1 text-end'>" + row.qty + "</div>";
        html += "</div>";
    });
    return html;
}

function configureNewStatusOptions() {
    const select = document.getElementById("newOrderStatus");
    const costInputGroup = document.getElementById("costInputGroup");
    const costPrice = document.getElementById("costPrice");
    const costNote = document.getElementById("costNote");
    const lotInputGroup = document.getElementById("lotInputGroup");
    const lotNumber = document.getElementById("l_lot_number");
    const lotStartDate = document.getElementById("l_start_date");
    const lotEndDate = document.getElementById("l_end_date");
    const shippingInputGroup = document.getElementById("shippingInputGroup");
    const shippingPrice = document.getElementById("shippingPrice");
    const shippingNote = document.getElementById("shippingNote");
    const lotArriveDate = document.getElementById("lotArriveDate");
    const postalInputGroup = document.getElementById("postalInputGroup");
    const postalPrice = document.getElementById("postalPrice");
    const postalNote = document.getElementById("postalNote");
    const statusRecordDate = document.getElementById("statusRecordDate");

    if (!select) {
        return;
    }

    Array.from(select.options).forEach(option => {
        option.hidden = false;
        option.disabled = false;
    });

    if (selectedStatusId === "1") {
        Array.from(select.options).forEach(option => {
            if (option.value !== "2") {
                option.hidden = true;
                option.disabled = true;
            }
        });
        select.value = "2";
        if (costInputGroup) {
            costInputGroup.style.display = "";
        }
        if (lotInputGroup) {
            lotInputGroup.style.display = "none";
        }
        if (shippingInputGroup) {
            shippingInputGroup.style.display = "none";
        }
        if (postalInputGroup) {
            postalInputGroup.style.display = "none";
        }
    } else if (selectedStatusId === "2") {
        Array.from(select.options).forEach(option => {
            if (option.value !== "3") {
                option.hidden = true;
                option.disabled = true;
            }
        });
        select.value = "3";
        if (costInputGroup) {
            costInputGroup.style.display = "none";
        }
        if (lotInputGroup) {
            lotInputGroup.style.display = "";
        }
        if (shippingInputGroup) {
            shippingInputGroup.style.display = "none";
        }
        if (postalInputGroup) {
            postalInputGroup.style.display = "none";
        }
    } else if (selectedStatusId === "3") {
        Array.from(select.options).forEach(option => {
            if (option.value !== "4") {
                option.hidden = true;
                option.disabled = true;
            }
        });
        select.value = "4";
        if (costInputGroup) {
            costInputGroup.style.display = "none";
        }
        if (lotInputGroup) {
            lotInputGroup.style.display = "none";
        }
        if (shippingInputGroup) {
            shippingInputGroup.style.display = "";
        }
        if (postalInputGroup) {
            postalInputGroup.style.display = "none";
        }
    } else if (selectedStatusId === "4") {
        Array.from(select.options).forEach(option => {
            if (option.value !== "5") {
                option.hidden = true;
                option.disabled = true;
            }
        });
        select.value = "5";
        if (costInputGroup) {
            costInputGroup.style.display = "none";
        }
        if (lotInputGroup) {
            lotInputGroup.style.display = "none";
        }
        if (shippingInputGroup) {
            shippingInputGroup.style.display = "none";
        }
        if (postalInputGroup) {
            postalInputGroup.style.display = "";
        }
    } else {
        if (select.value === selectedStatusId) {
            const nextOption = Array.from(select.options).find(option => option.value !== selectedStatusId);
            if (nextOption) {
                select.value = nextOption.value;
            }
        }
        if (costInputGroup) {
            costInputGroup.style.display = "none";
        }
        if (lotInputGroup) {
            lotInputGroup.style.display = "none";
        }
        if (shippingInputGroup) {
            shippingInputGroup.style.display = "none";
        }
        if (postalInputGroup) {
            postalInputGroup.style.display = "none";
        }
    }

    if (costPrice) {
        costPrice.value = "";
    }
    if (costNote) {
        costNote.value = "";
    }
    if (lotNumber) {
        lotNumber.value = "";
    }
    if (lotStartDate) {
        lotStartDate.value = "";
    }
    if (lotEndDate) {
        lotEndDate.value = "";
    }
    if (shippingPrice) {
        shippingPrice.value = "";
    }
    if (shippingNote) {
        shippingNote.value = "";
    }
    if (lotArriveDate) {
        lotArriveDate.value = "";
    }
    if (postalPrice) {
        postalPrice.value = "";
    }
    if (postalNote) {
        postalNote.value = "";
    }
    if (statusRecordDate) {
        statusRecordDate.value = "";
    }
}

function bindCostPriceFormatter() {
    document.querySelectorAll(".money-input, #costPrice").forEach(input => {
        input.addEventListener("input", function() {
            const cursorAtEnd = input.selectionStart === input.value.length;
            input.value = formatCostPrice(input.value);
            if (cursorAtEnd) {
                input.setSelectionRange(input.value.length, input.value.length);
            }
        });
    });
}

function formatCostPrice(value) {
    let cleaned = value.replace(/[^\d.,]/g, "").replace(/,/g, "");
    const dotIndex = cleaned.indexOf(".");

    if (dotIndex !== -1) {
        cleaned = cleaned.slice(0, dotIndex + 1) + cleaned.slice(dotIndex + 1).replace(/\./g, "");
    }

    const parts = cleaned.split(".");
    const integerPart = parts[0].replace(/^0+(?=\d)/, "");
    const formattedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",");

    if (parts.length > 1) {
        return (formattedInteger || "0") + "." + parts[1];
    }

    return formattedInteger;
}

function normalizeCostPrice(value) {
    return value.replace(/,/g, "");
}

function save_change_status() {
    const newOrderStatus = document.getElementById("newOrderStatus").value;
    const recordDate = document.getElementById("statusRecordDate") ? document.getElementById("statusRecordDate").value : "";
    const costPrice = document.getElementById("costPrice") ? document.getElementById("costPrice").value.trim() : "";
    const normalizedCostPrice = normalizeCostPrice(costPrice);
    const costNote = document.getElementById("costNote") ? document.getElementById("costNote").value.trim() : "";
    const lotNumber = document.getElementById("l_lot_number") ? document.getElementById("l_lot_number").value.trim() : "";
    const shippingPrice = document.getElementById("shippingPrice") ? document.getElementById("shippingPrice").value.trim() : "";
    const normalizedShippingPrice = normalizeCostPrice(shippingPrice);
    const shippingNote = document.getElementById("shippingNote") ? document.getElementById("shippingNote").value.trim() : "";
    const lotArriveDate = document.getElementById("lotArriveDate") ? document.getElementById("lotArriveDate").value : "";
    const postalPrice = document.getElementById("postalPrice") ? document.getElementById("postalPrice").value.trim() : "";
    const normalizedPostalPrice = normalizeCostPrice(postalPrice);
    const postalNote = document.getElementById("postalNote") ? document.getElementById("postalNote").value.trim() : "";
    const lotStartDate = document.getElementById("l_start_date") ? document.getElementById("l_start_date").value : "";
    const lotEndDate = document.getElementById("l_end_date") ? document.getElementById("l_end_date").value : "";
    const ids = Array.from(selectedRows.values()).map(row => row.id);

    if (!newOrderStatus || ids.length === 0) {
        return;
    }

    if (!recordDate) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณาเลือกวันที่บันทึก",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "5") {
        showStatusWarning("ส่งของสำเร็จ");
        return;
    }

    if (selectedStatusId === "1" && newOrderStatus !== "2") {
        showStatusWarning("รอกดของ");
        return;
    }

    if (selectedStatusId === "2" && newOrderStatus !== "3") {
        showStatusWarning("กดของแล้ว รอเข้าโกดัง");
        return;
    }

    if (selectedStatusId === "3" && newOrderStatus !== "4") {
        showStatusWarning("รอของถึงร้าน");
        return;
    }

    if (selectedStatusId === "4" && newOrderStatus !== "5") {
        showStatusWarning("ถึงร้านแล้วรอส่ง");
        return;
    }

    if (selectedStatusId === "1" && (!normalizedCostPrice || !/^\d+(\.\d*)?$/.test(normalizedCostPrice))) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกต้นทุน",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "2" && !lotNumber) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกเลข Lot",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "3" && (!normalizedShippingPrice || !/^\d+(\.\d*)?$/.test(normalizedShippingPrice))) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกค่าส่ง",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "3" && !lotArriveDate) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกวันที่ของถึงร้าน",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "4" && (!normalizedPostalPrice || !/^\d+(\.\d*)?$/.test(normalizedPostalPrice))) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกค่าส่งไปรษณีย์",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "2" && (!lotStartDate || !lotEndDate)) {
        if (window.Swal) {
            Swal.fire({
                title: "กรุณากรอกช่วงวันที่คาดว่าจะถึงร้าน",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    if (selectedStatusId === "2" && lotEndDate < lotStartDate) {
        if (window.Swal) {
            Swal.fire({
                title: "วันที่สิ้นสุดต้องไม่น้อยกว่าวันที่เริ่มต้น",
                icon: "warning",
                confirmButtonText: "ตกลง"
            });
        }
        return;
    }

    fetch("/change/status/update", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            ids: ids,
            orderStatus: parseInt(newOrderStatus, 10),
            recordDate: recordDate,
            costPrice: normalizedCostPrice,
            costNote: costNote,
            l_lot_number: lotNumber,
            l_start_date: lotStartDate,
            l_end_date: lotEndDate,
            shippingPrice: normalizedShippingPrice,
            shippingNote: shippingNote,
            l_arrive_date: lotArriveDate,
            postalPrice: normalizedPostalPrice,
            postalNote: postalNote
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Update failed");
        }
        return response.text();
    })
    .then(() => {
        if (window.Swal) {
            Swal.fire({
                title: "บันทึกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            });
        }

        const modalElement = document.getElementById("modalChangeStatus");
        const modal = bootstrap.Modal.getInstance(modalElement);
        if (modal) {
            modal.hide();
        }

        load_change_data();
    })
    .catch(error => {
        console.error("Error updating status:", error);
        if (window.Swal) {
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        }
    });
}
