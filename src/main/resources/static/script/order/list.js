document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("orderListFilterForm");
    if (!filterForm) {
        return;
    }

    setDefaultOrderListDates(false);
    initOrderEditCalculation();

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

function edit_order(orderId, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    fetch("/orders/" + orderId)
        .then(response => {
            if (!response.ok) {
                throw new Error("Order not found");
            }
            return response.json();
        })
        .then(order => {
            document.getElementById("editOrderId").value = order.id || "";
            document.getElementById("editPriceTotal").value = normalizeNumber(order.price_total);
            document.getElementById("editOrderDate").value = order.order_date || "";
            document.getElementById("editCustomerName").value = order.customer_name || "";
            document.getElementById("editPayMethod").value = order.pay_method || "";
            document.getElementById("editSendCost").value = normalizeNumber(order.send_cost);
            document.getElementById("editDiscount").value = normalizeNumber(order.discount);
            document.getElementById("editPledge").value = normalizeNumber(order.price_pledge);
            document.getElementById("editBalance").value = normalizeNumber(order.price_balance);
            document.getElementById("editNet").value = normalizeNumber(order.net);
            document.getElementById("editRemark").value = order.remark || "";
            calculate_order_edit_summary();

            const modalElement = document.getElementById("orderEditModal");
            let modal = bootstrap.Modal.getInstance(modalElement);
            if (!modal) {
                modal = new bootstrap.Modal(modalElement);
            }
            modal.show();
        })
        .catch(error => {
            console.error("Error loading order:", error);
            Swal.fire({ title: "โหลดคำสั่งซื้อไม่สำเร็จ", icon: "error", confirmButtonText: "ตกลง" });
        });
}

function save_order_edit() {
    const orderId = document.getElementById("editOrderId").value;
    if (!orderId) {
        return;
    }
    calculate_order_edit_summary();
    const payload = {
        order_date: document.getElementById("editOrderDate").value,
        customer_name: document.getElementById("editCustomerName").value.trim(),
        pay_method: parseInt(document.getElementById("editPayMethod").value || "0", 10),
        pay_type: null,
        last_pay_date: "",
        send_cost: parseFloat(document.getElementById("editSendCost").value || "0"),
        discount: parseFloat(document.getElementById("editDiscount").value || "0"),
        price_pledge: parseFloat(document.getElementById("editPledge").value || "0"),
        price_balance: parseFloat(document.getElementById("editBalance").value || "0"),
        net: parseFloat(document.getElementById("editNet").value || "0"),
        remark: document.getElementById("editRemark").value.trim()
    };

    if (!payload.order_date || !payload.customer_name || !payload.pay_method) {
        Swal.fire({ title: "กรอกข้อมูลให้ครบ", icon: "warning", confirmButtonText: "ตกลง" });
        return;
    }

    fetch("/orders/" + orderId + "/update", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Update failed");
            }
            const modal = bootstrap.Modal.getInstance(document.getElementById("orderEditModal"));
            if (modal) {
                modal.hide();
            }
            load_order_list_data();
            Swal.fire({ title: "บันทึกสำเร็จ", icon: "success", confirmButtonText: "ตกลง" });
        })
        .catch(error => {
            console.error("Error updating order:", error);
            Swal.fire({ title: "บันทึกไม่สำเร็จ", icon: "error", confirmButtonText: "ตกลง" });
        });
}

function initOrderEditCalculation() {
    ["editPayMethod", "editSendCost", "editDiscount", "editPledge"].forEach(id => {
        const element = document.getElementById(id);
        if (!element) {
            return;
        }
        element.addEventListener("input", calculate_order_edit_summary);
        element.addEventListener("change", calculate_order_edit_summary);
    });
}

function calculate_order_edit_summary() {
    const total = parseEditNumber("editPriceTotal");
    const send = parseEditNumber("editSendCost");
    const discount = parseEditNumber("editDiscount");
    const payMethod = parseInt(document.getElementById("editPayMethod").value || "0", 10);
    const net = Math.max(total + send - discount, 0);
    let pledge = parseEditNumber("editPledge");
    let balance = 0;

    if (payMethod === 1) {
        pledge = 0;
        document.getElementById("editPledge").value = normalizeNumber(0);
    } else {
        balance = Math.max(net - pledge, 0);
    }

    document.getElementById("editBalance").value = normalizeNumber(balance);
    document.getElementById("editNet").value = normalizeNumber(net);
}

function parseEditNumber(id) {
    const element = document.getElementById(id);
    if (!element) {
        return 0;
    }
    const value = String(element.value || "0").replace(/,/g, "");
    const numberValue = Number(value);
    return Number.isFinite(numberValue) ? numberValue : 0;
}

function delete_order(orderId, event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    Swal.fire({
        title: "ลบคำสั่งซื้อนี้?",
        text: "รายการสินค้าและรายรับของคำสั่งซื้อนี้จะถูกลบไปด้วย",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ลบ",
        cancelButtonText: "ยกเลิก",
        confirmButtonColor: "#dc3545"
    }).then(result => {
        if (!result.isConfirmed) {
            return;
        }
        fetch("/orders/" + orderId + "/delete", { method: "POST" })
            .then(response => {
                if (!response.ok) {
                    throw new Error("Delete failed");
                }
                load_order_list_data();
                Swal.fire({ title: "ลบสำเร็จ", icon: "success", confirmButtonText: "ตกลง" });
            })
            .catch(error => {
                console.error("Error deleting order:", error);
                Swal.fire({ title: "ลบไม่สำเร็จ", icon: "error", confirmButtonText: "ตกลง" });
            });
    });
}

function delete_order_detail(orderId, detailId) {
    Swal.fire({
        title: "ลบรายการสินค้านี้?",
        text: "ลบได้เฉพาะรายการที่อยู่สถานะรอกดของเท่านั้น",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ลบ",
        cancelButtonText: "ยกเลิก",
        confirmButtonColor: "#dc3545"
    }).then(result => {
        if (!result.isConfirmed) {
            return;
        }
        fetch("/orders/" + orderId + "/details/" + detailId + "/delete", { method: "POST" })
            .then(response => {
                if (!response.ok) {
                    throw new Error("Delete detail failed");
                }
                return response.text();
            })
            .then(() => {
                open_order_detail(orderId);
                load_order_list_data();
                Swal.fire({ title: "ลบสำเร็จ", icon: "success", confirmButtonText: "ตกลง" });
            })
            .catch(error => {
                console.error("Error deleting order detail:", error);
                Swal.fire({ title: "ลบไม่สำเร็จ", text: "รายการนี้อาจไม่ได้อยู่สถานะรอกดของ", icon: "error", confirmButtonText: "ตกลง" });
            });
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

function normalizeNumber(value) {
    const numberValue = Number(value || 0);
    return Number.isFinite(numberValue) ? numberValue.toFixed(2) : "0.00";
}
