document.addEventListener("DOMContentLoaded", function() {
    const filterForm = document.getElementById("orderListFilterForm");
    if (!filterForm) {
        return;
    }

    filterForm.addEventListener("submit", function(event) {
        event.preventDefault();
        load_order_list_data();
    });
});

function load_order_list_data() {
    const params = new URLSearchParams();
    const orderDate = document.getElementById("orderDate").value;
    const customerName = document.getElementById("customerName").value.trim();
    const payMethod = document.getElementById("payMethod").value;
    const orderStatus = document.getElementById("orderStatus").value;

    if (orderDate) {
        params.append("orderDate", orderDate);
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
    document.getElementById("orderDate").value = "";
    document.getElementById("customerName").value = "";
    document.getElementById("payMethod").value = "";
    document.getElementById("orderStatus").value = "";
    load_order_list_data();
}

function open_order_detail(orderId) {
    fetch("/orders/" + orderId + "/details")
        .then(response => response.text())
        .then(html => {
            document.getElementById("orderDetailRows").innerHTML = html;

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
