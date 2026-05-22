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
