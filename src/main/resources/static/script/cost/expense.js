document.addEventListener("DOMContentLoaded", function() {
    const priceInput = document.getElementById("expensePrice");
    if (!priceInput) {
        return;
    }

    priceInput.addEventListener("input", function() {
        priceInput.value = format_expense_price(priceInput.value);
    });
});

function open_expense_modal(button) {
    const form = document.getElementById("expenseForm");
    const title = document.getElementById("expenseModalTitle");
    const recordDate = document.getElementById("expenseRecordDate");
    const typeCost = document.getElementById("expenseTypeCost");
    const price = document.getElementById("expensePrice");
    const note = document.getElementById("expenseNote");
    const editing = button && button.dataset.expenseId;

    form.action = editing ? "/cost/expense/" + button.dataset.expenseId : "/cost/expense";
    title.textContent = editing ? "แก้ไขค่าใช้จ่าย" : "บันทึกค่าใช้จ่าย";
    recordDate.value = editing ? button.dataset.expenseDate || "" : get_local_expense_date();
    typeCost.value = editing ? button.dataset.expenseType || "" : "";
    price.value = editing ? button.dataset.expensePrice || "" : "";
    note.value = editing ? button.dataset.expenseNote || "" : "";

    const modalElement = document.getElementById("expenseModal");
    let modal = bootstrap.Modal.getInstance(modalElement);
    if (!modal) {
        modal = new bootstrap.Modal(modalElement);
    }
    modal.show();
}

function get_local_expense_date() {
    const today = new Date();
    const timezoneOffset = today.getTimezoneOffset() * 60000;
    return new Date(today.getTime() - timezoneOffset).toISOString().slice(0, 10);
}

function format_expense_price(value) {
    let cleaned = String(value || "").replace(/[^\d.,]/g, "").replace(/,/g, "");
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
