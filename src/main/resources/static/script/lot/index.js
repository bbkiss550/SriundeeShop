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

    if (startDate) {
        params.append("startDate", startDate);
    }
    if (endDate) {
        params.append("endDate", endDate);
    }
    if (status) {
        params.append("status", status);
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
    set_current_month_filter();
    document.getElementById("status").value = "";
    load_lot_data();
}

function set_current_month_filter() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();
    const startDate = new Date(year, month, 1);
    const endDate = new Date(year, month + 1, 0);
    document.getElementById("startDate").value = format_date_input(startDate);
    document.getElementById("endDate").value = format_date_input(endDate);
}

function format_date_input(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return year + "-" + month + "-" + day;
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
