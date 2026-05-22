document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("depositBalanceFilterForm");
    if (!filterForm) {
        return;
    }

    filterForm.addEventListener("submit", function(event) {
        event.preventDefault();
        load_deposit_balance_data();
    });
});

function load_deposit_balance_data() {
    const params = new URLSearchParams();
    const startDate = document.getElementById("startDate").value;
    const endDate = document.getElementById("endDate").value;
    const customerName = document.getElementById("customerName").value.trim();

    if (startDate) {
        params.append("startDate", startDate);
    }
    if (endDate) {
        params.append("endDate", endDate);
    }
    if (customerName) {
        params.append("customerName", customerName);
    }

    fetch("/deposit-balance/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            document.getElementById("depositBalanceRows").innerHTML = html;

            if (window.feather) {
                feather.replace();
            }
        })
        .catch(error => {
            console.error("Error loading deposit balance data:", error);
        });
}

function clear_deposit_balance_filter() {
    set_full_year_filter();
    document.getElementById("customerName").value = "";
    load_deposit_balance_data();
}

function set_full_year_filter() {
    document.getElementById("startDate").value = "2026-01-01";
    document.getElementById("endDate").value = "2026-12-31";
}

function open_deposit_detail(orderId) {
    fetch("/deposit-balance/" + orderId + "/details")
        .then(response => response.text())
        .then(html => {
            document.getElementById("depositDetailRows").innerHTML = html;
            moveDetailSummaryToFooter("depositDetailRows", "depositDetailSummary");

            if (window.feather) {
                feather.replace();
            }

            const modalElement = document.getElementById("depositDetailModal");
            let modal = bootstrap.Modal.getInstance(modalElement);
            if (!modal) {
                modal = new bootstrap.Modal(modalElement);
            }
            modal.show();
        })
        .catch(error => {
            console.error("Error loading deposit detail:", error);
        });
}

function receive_deposit_balance(orderId, event) {
    if (event) {
        event.stopPropagation();
    }

    Swal.fire({
        title: "ยืนยันรับเงินมัดจำที่เหลือ",
        text: "ต้องการบันทึกรับเงินที่เหลือของคำสั่งซื้อนี้หรือไม่",
        input: "date",
        inputLabel: "วันที่บันทึก",
        inputValue: get_local_deposit_record_date(),
        inputValidator: (value) => {
            if (!value) {
                return "กรุณาเลือกวันที่บันทึก";
            }
        },
        icon: "warning",
        showCancelButton: true,
        confirmButtonText: "ยืนยัน",
        cancelButtonText: "ปิด",
        confirmButtonColor: "#198754"
    }).then((result) => {
        if (!result.isConfirmed) {
            return;
        }

        fetch("/deposit-balance/" + orderId + "/receive?recordDate=" + encodeURIComponent(result.value), {
            method: "POST"
        })
        .then(response => {
            if (!response.ok) {
                throw new Error("Receive failed");
            }
            return response.text();
        })
        .then(() => {
            Swal.fire({
                title: "บันทึกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then(() => {
                load_deposit_balance_data();
            });
        })
        .catch(error => {
            console.error("Error receiving deposit balance:", error);
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                icon: "error",
                confirmButtonText: "ตกลง"
            });
        });
    });
}

function get_local_deposit_record_date() {
    const today = new Date();
    const timezoneOffset = today.getTimezoneOffset() * 60000;
    return new Date(today.getTime() - timezoneOffset).toISOString().slice(0, 10);
}
