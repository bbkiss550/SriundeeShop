document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("lotFilterForm");
    if (!filterForm) {
        return;
    }

    filterForm.addEventListener("submit", function(event) {
        event.preventDefault();
        load_lot_data();
    });
});

let currentLotId = null;

function load_lot_data() {
    const params = new URLSearchParams();
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const status = document.getElementById("status").value;
    const lotNumber = document.getElementById("lotNumber").value;

    if (startDate) {
        params.append("startDate", startDate);
    }
    if (endDate) {
        params.append("endDate", endDate);
    }
    if (status) {
        params.append("status", status);
    }
    if (lotNumber) {
        params.append("lotNumber", lotNumber);
    }

    fetch("/lot/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            document.getElementById("lotRows").innerHTML = html;

            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading lot data:", error);
        });
}

function clear_lot_filter() {
    set_full_year_filter();
    document.getElementById("status").value = "";
    document.getElementById("lotNumber").value = "";
    load_lot_data();
}

function set_full_year_filter() {
    document.getElementById("startDate").value = "2026-01-01";
    document.getElementById("endDate").value = "2026-12-31";
}

function open_lot_detail(id) {
    currentLotId = id;
    fetch("/lot/detail/" + id)
        .then(response => response.text())
        .then(html => {
            document.getElementById("lotDetailRows").innerHTML = html;

            if (window.feather) {
                feather.replace();
            }

            const modalElement = document.getElementById("modalLotDetail");
            let modal = bootstrap.Modal.getInstance(modalElement);
            if (!modal) {
                modal = new bootstrap.Modal(modalElement);
            }
            modal.show();
        })
        .catch(error => {
            console.error("Error loading lot detail:", error);
        });
}

function cancel_lot(id, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "ยืนยันการยกเลิก",
        text: "ต้องการยกเลิก LOT นี้และเปลี่ยนสถานะสินค้ากลับเป็นกดของแล้ว รอเข้าโกดังหรือไม่",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ยืนยัน",
        cancelButtonText: "ปิด",
        confirmButtonColor: "#dc3545"
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/lot/cancel/" + id, {
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
                load_lot_data();
            });
        })
        .catch(error => {
            console.error("Error canceling lot:", error);
            Swal.fire({
                title: "ยกเลิกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function edit_lot_number(id, currentLotNumber, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "แก้เลขที่ LOT",
        input: "text",
        inputValue: currentLotNumber || "",
        inputPlaceholder: "เลขที่ LOT",
        showCancelButton: true,
        confirmButtonText: "บันทึก",
        cancelButtonText: "ปิด",
        inputValidator: (value) => {
            if (!value || !value.trim()) {
                return "กรุณากรอกเลขที่ LOT";
            }
            return null;
        }
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/lot/update-number/" + id, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                lotNumber: result.value.trim()
            })
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
                load_lot_data();
            });
        })
        .catch(error => {
            console.error("Error updating lot number:", error);
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function delete_lot_detail(id) {
    Swal.fire({
        title: "ยืนยันการลบ",
        text: "ต้องการลบรายการสินค้านี้ออกจาก LOT และเปลี่ยนสถานะกลับหรือไม่",
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ยืนยัน",
        cancelButtonText: "ปิด",
        confirmButtonColor: "#dc3545"
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/lot/detail/delete/" + id, {
            method: "POST"
        })
        .then(response => {
            if (!response.ok) {
                throw new Error("Delete failed");
            }
            return response.text();
        })
        .then(() => {
            Swal.fire({
                title: "ลบสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then(() => {
                if (currentLotId) {
                    open_lot_detail(currentLotId);
                }
                load_lot_data();
            });
        })
        .catch(error => {
            console.error("Error deleting lot detail:", error);
            Swal.fire({
                title: "ลบไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function edit_lot_number(id, currentLotNumber, currentStartDate, currentEndDate, currentArriveDate, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "แก้ไข LOT",
        html: `
            <div class="text-start edit-lot-form">
                <div class="input-group mb-3">
                    <span class="input-group-text">เลข LOT</span>
                    <input type="text" id="editLotNumber" class="form-control" value="${escape_lot_attr(currentLotNumber || "")}" placeholder="เลข LOT">
                </div>
                <div class="input-group mb-3">
                    <span class="input-group-text">วันที่คาดว่าจะถึงร้าน</span>
                    <input type="date" id="editLotStartDate" class="form-control" value="${escape_lot_attr(currentStartDate || "")}">
                </div>
                <div class="input-group mb-3">
                    <span class="input-group-text">ถึง</span>
                    <input type="date" id="editLotEndDate" class="form-control" value="${escape_lot_attr(currentEndDate || "")}">
                </div>
                <div class="input-group">
                    <span class="input-group-text">วันที่ของถึงร้าน</span>
                    <input type="date" id="editLotArriveDate" class="form-control" value="${escape_lot_attr(currentArriveDate || "")}">
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: "บันทึก",
        cancelButtonText: "ปิด",
        focusConfirm: false,
        preConfirm: () => {
            const lotNumber = document.getElementById("editLotNumber").value.trim();
            const startDate = document.getElementById("editLotStartDate").value;
            const endDate = document.getElementById("editLotEndDate").value;
            const arriveDate = document.getElementById("editLotArriveDate").value;
            if (!lotNumber) {
                Swal.showValidationMessage("กรุณากรอกเลข LOT");
                return false;
            }
            if ((startDate && !endDate) || (!startDate && endDate)) {
                Swal.showValidationMessage("กรุณากรอกวันที่คาดว่าจะถึงร้านให้ครบ");
                return false;
            }
            if (startDate && endDate && endDate < startDate) {
                Swal.showValidationMessage("วันที่ ถึง ต้องไม่น้อยกว่าวันที่คาดว่าจะถึงร้าน");
                return false;
            }
            return { lotNumber, startDate, endDate, arriveDate };
        }
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/lot/update-number/" + id, {
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
                load_lot_data();
            });
        })
        .catch(error => {
            console.error("Error updating lot:", error);
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function escape_lot_attr(value) {
    return String(value || "")
        .replace(/&/g, "&amp;")
        .replace(/"/g, "&quot;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}
