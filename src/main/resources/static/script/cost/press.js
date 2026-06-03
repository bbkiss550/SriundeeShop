document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("costPressFilterForm");
    if (!filterForm) {
        return;
    }

    filterForm.addEventListener("submit", function(event) {
        event.preventDefault();
        load_cost_press_data();
    });
});

function load_cost_press_data() {
    const params = new URLSearchParams();
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const status = document.getElementById("status").value;

    if (startDate) {
        params.append("startDate", startDate);
    }
    if (endDate) {
        params.append("endDate", endDate);
    }
    if (status) {
        params.append("status", status);
    }

    fetch("/cost/press/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            document.getElementById("costPressRows").innerHTML = html;

            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading cost press data:", error);
        });
}

function clear_cost_press_filter() {
    set_full_year_filter();
    document.getElementById("status").value = "";
    load_cost_press_data();
}

function set_full_year_filter() {
    document.getElementById("startDate").value = "2026-01-01";
    document.getElementById("endDate").value = "2026-12-31";
}

function open_cost_detail(id) {
    fetch("/cost/press/detail/" + id)
        .then(response => response.text())
        .then(html => {
            document.getElementById("costDetailRows").innerHTML = html;
            moveDetailSummaryToFooter("costDetailRows", "costDetailSummary");

            if (window.feather) {
                feather.replace();
            }

            const modalElement = document.getElementById("modalCostDetail");
            let modal = bootstrap.Modal.getInstance(modalElement);
            if (!modal) {
                modal = new bootstrap.Modal(modalElement);
            }
            modal.show();
        })
        .catch(error => {
            console.error("Error loading cost detail:", error);
        });
}

function cancel_cost_press(id, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "ยืนยันการยกเลิก",
        text: "ต้องการยกเลิกข้อมูลการกดของนี้หรือไม่",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ยืนยัน",
        cancelButtonText: "ปิด",
        confirmButtonColor: "#dc3545"
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/cost/press/cancel/" + id, {
            method: "POST"
        })
        .then(response => {
            if (!response.ok) {
                throw new Error("Cancel failed");
            }
            return response.text();
        })
        .then(() => {
            Swal.fire({
                title: "ยกเลิกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then(() => {
                load_cost_press_data();
            });
        })
        .catch(error => {
            console.error("Error canceling cost press:", error);
            Swal.fire({
                title: "ยกเลิกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function edit_cost_press(id, currentPrice, currentNote, currentRecordDate, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "แก้ไขการกดของ",
        html:
            "<div class='input-group mb-3'>" +
                "<span class='input-group-text' style='width: 110px;'>วันที่บันทึก</span>" +
                "<input type='date' id='swalPressRecordDate' class='form-control' value='" + escape_html(currentRecordDate || get_local_press_record_date()) + "'>" +
            "</div>" +
            "<div class='input-group mb-3'>" +
                "<span class='input-group-text' style='width: 110px;'>ยอดเงิน</span>" +
                "<input type='text' id='swalPressPrice' class='form-control text-end' inputmode='decimal' value='" + escape_html(currentPrice || "") + "'>" +
                "<span class='input-group-text'>บาท</span>" +
            "</div>" +
            "<div class='input-group'>" +
                "<span class='input-group-text' style='width: 110px;'>หมายเหตุ</span>" +
                "<input type='text' id='swalPressNote' class='form-control' value='" + escape_html(currentNote || "") + "'>" +
            "</div>",
        showCancelButton: true,
        confirmButtonText: "บันทึก",
        cancelButtonText: "ปิด",
        didOpen: () => {
            const priceInput = document.getElementById("swalPressPrice");
            if (!priceInput) {
                return;
            }
            priceInput.addEventListener("input", function() {
                const cursorAtEnd = priceInput.selectionStart === priceInput.value.length;
                priceInput.value = format_press_price(priceInput.value);
                if (cursorAtEnd) {
                    priceInput.setSelectionRange(priceInput.value.length, priceInput.value.length);
                }
            });
            priceInput.focus();
            priceInput.select();
        },
        preConfirm: () => {
            const recordDate = document.getElementById("swalPressRecordDate").value.trim();
            const price = document.getElementById("swalPressPrice").value.trim();
            const normalizedPrice = normalize_press_price(price);
            const note = document.getElementById("swalPressNote").value.trim();

            if (!recordDate) {
                Swal.showValidationMessage("กรุณาเลือกวันที่บันทึก");
                return false;
            }

            if (!normalizedPrice || !/^\d+(\.\d*)?$/.test(normalizedPrice)) {
                Swal.showValidationMessage("กรุณากรอกยอดเงิน");
                return false;
            }

            return {
                recordDate: recordDate,
                pressPrice: normalizedPrice,
                pressNote: note
            };
        }
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/cost/press/update/" + id, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(result.value)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error("Update failed");
            }
            return response.text();
        })
        .then(() => {
            Swal.fire({
                title: "บันทึกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then(() => {
                load_cost_press_data();
            });
        })
        .catch(error => {
            console.error("Error updating cost press:", error);
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function format_press_price(value) {
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

function normalize_press_price(value) {
    return value.replace(/,/g, "");
}

function get_local_press_record_date() {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

function escape_html(value) {
    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}
