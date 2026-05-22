document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("orderListFilterForm");
    if (!filterForm) {
        return;
    }

    setDefaultOrderListDates(false);

    filterForm.addEventListener("submit", function(event) {
        event.preventDefault();
        load_order_list_data();
    });
});

let currentOrderDetailId = null;

function load_order_list_data() {
    const params = new URLSearchParams();
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const customerName = document.getElementById("customerName").value.trim();
    const payMethod = document.getElementById("payMethod").value;
    const orderStatus = document.getElementById("orderStatus").value;

    if (startDate) {
        params.append("startDate", startDate);
    }
    if (endDate) {
        params.append("endDate", endDate);
    }
    if (customerName) {
        params.append("customerName", customerName);
    }
    if (payMethod) {
        params.append("payMethod", payMethod);
    }
    if (orderStatus) {
        params.append("orderStatus", orderStatus);
    }

    fetch("/orders/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            document.getElementById("orderListRows").innerHTML = html;

            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading order list:", error);
        });
}

function clear_order_list_filter() {
    setDefaultOrderListDates(true);
    document.getElementById("customerName").value = "";
    document.getElementById("payMethod").value = "";
    document.getElementById("orderStatus").value = "";
    load_order_list_data();
}

function setDefaultOrderListDates(force) {
    const startDate = document.getElementById("startDate");
    const endDate = document.getElementById("endDate");
    if (!startDate || !endDate) {
        return;
    }
    if (force || !startDate.value) {
        startDate.value = "2026-01-01";
    }
    if (force || !endDate.value) {
        endDate.value = "2026-12-31";
    }
}

function open_order_detail(orderId) {
    currentOrderDetailId = orderId;
    fetch("/orders/" + orderId + "/details")
        .then(response => response.text())
        .then(html => {
            document.getElementById("orderDetailRows").innerHTML = html;
            moveDetailSummaryToFooter("orderDetailRows", "orderDetailSummary");

            if (window.feather) {
                feather.replace();
            }

            const modalElement = document.getElementById("orderDetailModal");
            let modal = bootstrap.Modal.getInstance(modalElement);
            if (!modal) {
                modal = new bootstrap.Modal(modalElement);
            }
            modal.show();
        })
        .catch(error => {
            console.error("Error loading order detail:", error);
        });
}

function create_order_receipt() {
    if (!currentOrderDetailId) {
        return;
    }
    const preview = document.getElementById("orderReceiptPreview");
    preview.innerHTML = "<div class='text-center text-muted py-4'>กำลังโหลด...</div>";

    const modalElement = document.getElementById("orderReceiptModal");
    let modal = bootstrap.Modal.getInstance(modalElement);
    if (!modal) {
        modal = new bootstrap.Modal(modalElement);
    }
    modal.show();

    fetch("/orders/" + currentOrderDetailId + "/receipt-fragment")
        .then(response => response.text())
        .then(html => {
            preview.innerHTML = html;

            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading order receipt:", error);
            preview.innerHTML = "<div class='text-center text-danger py-4'>โหลดใบเสร็จไม่สำเร็จ</div>";
        });
}

function copy_order_receipt_image() {
    const receiptElement = document.querySelector("#orderReceiptPreview .receipt");
    if (!receiptElement) {
        return;
    }
    if (!window.html2canvas || !navigator.clipboard || !window.ClipboardItem) {
        alert("เบราว์เซอร์นี้ยังไม่รองรับการ Copy image");
        return;
    }

    html2canvas(receiptElement, {
        backgroundColor: "#ffffff",
        scale: 2
    }).then(canvas => new Promise(resolve => {
        canvas.toBlob(blob => resolve(blob), "image/png");
    })).then(blob => {
        if (!blob) {
            throw new Error("Cannot create receipt image");
        }
        return navigator.clipboard.write([
            new ClipboardItem({ "image/png": blob })
        ]);
    }).then(() => {
        const button = document.querySelector("#orderReceiptModal .btn-primary");
        if (!button) {
            return;
        }
        const oldHtml = button.innerHTML;
        button.innerHTML = "Copied";
        button.disabled = true;
        setTimeout(() => {
            button.innerHTML = oldHtml;
            button.disabled = false;
            if (window.feather) {
                feather.replace();
            }
        }, 1400);
    }).catch(error => {
        console.error("Error copying receipt image:", error);
        alert("Copy image ไม่สำเร็จ");
    });
}
