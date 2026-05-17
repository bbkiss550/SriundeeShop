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
    set_current_month_filter();
    document.getElementById("status").value = "";
    load_cost_press_data();
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

function open_cost_detail(id) {
    fetch("/cost/press/detail/" + id)
        .then(response => response.text())
        .then(html => {
            document.getElementById("costDetailRows").innerHTML = html;

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
