document.addEventListener("DOMContentLoaded", function() {
    const toggle = document.getElementById("menuOrderToggle");
    const saveButton = document.getElementById("saveMenuOrderButton");
    if (toggle) {
        toggle.addEventListener("click", open_menu_order_modal);
    }
    if (saveButton) {
        saveButton.addEventListener("click", save_menu_order);
    }
});

let draggedMenuOrderRow = null;

function open_menu_order_modal() {
    const rows = document.getElementById("menuOrderRows");
    if (!rows) {
        return;
    }

    rows.innerHTML = "<div class='text-center text-muted py-3'>กำลังโหลด...</div>";
    show_menu_order_modal();

    fetch("/settings/menu-order")
        .then(response => {
            if (!response.ok) {
                throw new Error("Load menu order failed");
            }
            return response.json();
        })
        .then(menus => {
            rows.innerHTML = "";
            menus.forEach(menu => rows.appendChild(build_menu_order_row(menu)));
        })
        .catch(error => {
            console.error("Error loading menu order:", error);
            rows.innerHTML = "<div class='text-center text-danger py-3'>โหลดลำดับเมนูไม่สำเร็จ</div>";
        });
}

function show_menu_order_modal() {
    const modalElement = document.getElementById("menuOrderModal");
    let modal = bootstrap.Modal.getInstance(modalElement);
    if (!modal) {
        modal = new bootstrap.Modal(modalElement);
    }
    modal.show();
}

function build_menu_order_row(menu) {
    const row = document.createElement("div");
    row.className = "menu-order-row";
    row.dataset.menuId = menu.id;
    row.draggable = true;
    bind_menu_order_drag(row);

    const handle = document.createElement("span");
    handle.className = "menu-order-handle";
    handle.setAttribute("aria-hidden", "true");
    handle.innerHTML = "<i class='bi bi-grip-vertical'></i>";
    row.appendChild(handle);

    const name = document.createElement("div");
    name.className = "menu-order-row-name";
    name.textContent = menu.name || "";
    row.appendChild(name);
    return row;
}

function bind_menu_order_drag(row) {
    row.addEventListener("dragstart", function(event) {
        draggedMenuOrderRow = row;
        row.classList.add("is-dragging");
        event.dataTransfer.effectAllowed = "move";
        event.dataTransfer.setData("text/plain", row.dataset.menuId || "");
    });

    row.addEventListener("dragend", function() {
        row.classList.remove("is-dragging");
        document.querySelectorAll("#menuOrderRows .menu-order-row").forEach(item => item.classList.remove("is-drop-target"));
        draggedMenuOrderRow = null;
    });

    row.addEventListener("dragover", function(event) {
        if (!draggedMenuOrderRow || draggedMenuOrderRow === row) {
            return;
        }
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
        row.classList.add("is-drop-target");
    });

    row.addEventListener("dragleave", function() {
        row.classList.remove("is-drop-target");
    });

    row.addEventListener("drop", function(event) {
        if (!draggedMenuOrderRow || draggedMenuOrderRow === row) {
            return;
        }
        event.preventDefault();
        row.classList.remove("is-drop-target");

        const rows = row.parentElement;
        const bounds = row.getBoundingClientRect();
        const insertAfter = event.clientY > bounds.top + (bounds.height / 2);
        rows.insertBefore(draggedMenuOrderRow, insertAfter ? row.nextSibling : row);
    });
}

function save_menu_order() {
    const rows = Array.from(document.querySelectorAll("#menuOrderRows .menu-order-row"));
    const payload = rows.map((row, index) => ({
        id: Number(row.dataset.menuId),
        order: index + 1
    }));

    if (!payload.length || payload.some(item => !item.id)) {
        show_menu_order_error("ไม่พบเมนูสำหรับบันทึกลำดับ");
        return;
    }

    fetch("/settings/menu-order", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Save menu order failed");
            }
            return response.json();
        })
        .then(() => {
            if (window.Swal) {
                Swal.fire({
                    title: "บันทึกลำดับเมนูแล้ว",
                    icon: "success",
                    confirmButtonText: "ตกลง"
                }).then(() => window.location.reload());
                return;
            }
            window.location.reload();
        })
        .catch(error => {
            console.error("Error saving menu order:", error);
            show_menu_order_error("บันทึกลำดับเมนูไม่สำเร็จ");
        });
}

function show_menu_order_error(message) {
    if (window.Swal) {
        Swal.fire({
            title: message,
            icon: "error",
            confirmButtonText: "ตกลง"
        });
        return;
    }
    window.alert(message);
}
