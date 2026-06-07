function updateImage() {
	updateImagePreview();
}

function updateImagePreview() {
    const url = document.getElementById('pPic').value;
    if (url.trim() !== "") {
        document.getElementById('showImage').src = url;
    } else {
        document.getElementById('showImage').src = "/mazer/dist/assets/images/samples/no-photo.png";
    }
}

function new_data() {
	document.getElementById('IdProduct').value = "";
	document.getElementById('pName').value = "";
	document.getElementById('IdType').selectedIndex = 0;
	document.getElementById('IdArtist').selectedIndex = 0;
	document.getElementById('end_date').value = "";
	document.getElementById('send_date').value = "";
	setProductScheduleCheckbox('end_date', 'end_date_unscheduled', false);
	setProductScheduleCheckbox('send_date', 'send_date_unscheduled', false);
	document.getElementById('IdProductStatus').selectedIndex = 0;
	document.getElementById('pPic').value = "";
	document.getElementById('showImage').src = "/mazer/dist/assets/images/samples/no-photo.png";

	document.getElementById('btn_save').style.display = '';
	document.getElementById('btn_edit').style.display = 'none';
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function save_new_data() {
	const pName = document.getElementById('pName').value;
	const IdType = document.getElementById('IdType').value;
	const IdArtist = document.getElementById('IdArtist').value;
	const end_date_unscheduled = document.getElementById('end_date_unscheduled')?.checked;
	const send_date_unscheduled = document.getElementById('send_date_unscheduled')?.checked;
	const end_date = end_date_unscheduled ? null : document.getElementById('end_date').value;
	const send_date = send_date_unscheduled ? null : document.getElementById('send_date').value;
	const IdProductStatus = document.getElementById('IdProductStatus').value;
	const pPic = document.getElementById('pPic').value;

    if (!pName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อสินค้า",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

	if (!end_date_unscheduled && !end_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่ปิดรับ",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!send_date_unscheduled && !send_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่หำหนดส่ง",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

    const payload = {
        name: pName,
		type: IdType,
		artist: IdArtist,
		end_date: end_date,
		send_date: send_date,
		product_status: IdProductStatus,
		pic: pPic
    };

    fetch('/product/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(response => {
		if (response.ok) {
            Swal.fire({
                title: "บันทึกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then((result) => {
                if (result.isConfirmed) {
                    location.reload();
                }
            });
        } else {
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
                icon: "error"
            });
        }
    })
    .catch(err => console.error("Error:", err));
}

function edit_data(id) {
	document.getElementById('IdProduct').value = id;
    fetch('/product/get/' + id)
        .then(response => response.json())
        .then(data => {
			document.getElementById('pName').value = data.name
			document.getElementById('IdType').value = data.type; 
			document.getElementById('IdArtist').value = data.artist;
			document.getElementById('end_date').value = check_date_null(data.end_date);
			document.getElementById('send_date').value = check_date_null(data.send_date);
			setProductScheduleCheckbox('end_date', 'end_date_unscheduled', !document.getElementById('end_date').value);
			setProductScheduleCheckbox('send_date', 'send_date_unscheduled', !document.getElementById('send_date').value);
			document.getElementById('IdProductStatus').value = data.product_status;
			document.getElementById('pPic').value = data.pic;
			document.getElementById('showImage').src = check_pic_null(data.pic);
			
			document.getElementById('btn_edit').style.display = '';
			document.getElementById('btn_save').style.display = 'none';
			
            var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
            myModal.show();
        })
        .catch(err => console.error("Error fetching data:", err));
}

function save_edit_data() {
	const IdProduct = document.getElementById('IdProduct').value;
	const pName = document.getElementById('pName').value;
	const IdType = document.getElementById('IdType').value;
	const IdArtist = document.getElementById('IdArtist').value;
	const end_date_unscheduled = document.getElementById('end_date_unscheduled')?.checked;
	const send_date_unscheduled = document.getElementById('send_date_unscheduled')?.checked;
	const end_date = end_date_unscheduled ? null : document.getElementById('end_date').value;
	const send_date = send_date_unscheduled ? null : document.getElementById('send_date').value;
	const IdProductStatus = document.getElementById('IdProductStatus').value;
	const pPic = document.getElementById('pPic').value;

	if (!pName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อสินค้า",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!end_date_unscheduled && !end_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่ปิดรับ",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!send_date_unscheduled && !send_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่หำหนดส่ง",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

    const payload = {
		name: pName,
		type: IdType,
		artist: IdArtist,
		end_date: end_date,
		send_date: send_date,
		product_status: IdProductStatus,
		pic: pPic
    };
	
	fetch('/product/update/' + IdProduct , {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(response => {
		if (response.ok) {
            Swal.fire({
                title: "บันทึกสำเร็จ",
                icon: "success",
                confirmButtonText: "ตกลง"
            }).then((result) => {
                if (result.isConfirmed) {
                    location.reload();
                }
            });
        } else {
            Swal.fire({
                title: "บันทึกไม่สำเร็จ",
                text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
                icon: "error"
            });
        }
    })
    .catch(err => console.error("Error:", err));
}

function check_date_null(data) {
	var res = "";
	if (data) {
		res = data.split('T')[0];
	}
	return res;
}

function check_pic_null(data) {
	var res = "/mazer/dist/assets/images/samples/no-photo.png";
	if (data) {
		res = data;
	}
	return res;
}

function toggleProductScheduleDate(dateInputId, checkboxId) {
	const checkbox = document.getElementById(checkboxId);
	setProductScheduleDateState(dateInputId, checkbox?.checked);
}

function setProductScheduleCheckbox(dateInputId, checkboxId, checked) {
	const checkbox = document.getElementById(checkboxId);
	if (checkbox) {
		checkbox.checked = checked;
	}
	setProductScheduleDateState(dateInputId, checked);
}

function setProductScheduleDateState(dateInputId, unscheduled) {
	const dateInput = document.getElementById(dateInputId);
	const displayInput = document.querySelector(`[data-date-display-for="${dateInputId}"]`);
	if (!dateInput) {
		return;
	}
	if (unscheduled) {
		dateInput.value = "";
	} else if (!dateInput.value) {
		dateInput.value = getTodayNativeDate();
	}
	if (displayInput) {
		if (unscheduled) {
			displayInput.value = "";
			displayInput.placeholder = "ไม่มีกำหนด";
		} else {
			displayInput.placeholder = "วว/มม/ปปปป";
			displayInput.value = toDisplayProductDate(dateInput.value);
		}
		displayInput.disabled = !!unscheduled;
	}
	dateInput.disabled = !!unscheduled;
}

function getTodayNativeDate() {
	const today = new Date();
	const year = today.getFullYear();
	const month = String(today.getMonth() + 1).padStart(2, "0");
	const day = String(today.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

function toDisplayProductDate(value) {
	const match = String(value || "").match(/^(\d{4})-(\d{2})-(\d{2})$/);
	return match ? `${match[3]}/${match[2]}/${match[1]}` : "";
}

function delete_data(id) {
	Swal.fire({
		title: "ต้องการลบหรือไม่",
		icon: "info",
		showDenyButton: true,
		confirmButtonText: "ยืนยัน",
		denyButtonText: "ยกเลิก"
	}).then((result) => {
		if (result.isConfirmed) {
			fetch('/product/delete/' + id , {
			    method: 'POST'
			})
			.then(response => {
				if (response.ok) {
			        Swal.fire({
			            title: "บันทึกสำเร็จ",
			            icon: "success",
			            confirmButtonText: "ตกลง"
			        }).then((result) => {
			            if (result.isConfirmed) {
			                location.reload();
			            }
			        });
			    } else {
			        Swal.fire({
			            title: "บันทึกไม่สำเร็จ",
			            text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
			            icon: "error"
			        });
			    }
			})
			.catch(err => console.error("Error:", err));
		}
	});
}

function modal_order_summary(id, button) {
	const productName = button?.getAttribute("data-product-name") || "";
	const productNameEl = document.getElementById("order_summary_product_name");
	const summaryBody = document.getElementById("order_summary_body");

	if (productNameEl) {
		productNameEl.textContent = productName;
	}
	if (summaryBody) {
		summaryBody.innerHTML = "<tr><td colspan='4' class='text-center text-muted'>Loading...</td></tr>";
	}

	const modal = new bootstrap.Modal(document.getElementById("modalOrderSummary"));
	modal.show();

	fetch("/product/order-summary/" + id)
		.then(response => response.text())
		.then(html => {
			if (summaryBody) {
				summaryBody.innerHTML = html;
			}
		})
		.catch(err => {
			console.error("Error fetching order summary:", err);
			if (summaryBody) {
				summaryBody.innerHTML = "<tr><td colspan='4' class='text-center text-danger'>Error</td></tr>";
			}
		});
}

document.addEventListener("DOMContentLoaded", function() {
	bind_product_image_preview();
});

function bind_product_image_preview() {
	const table = document.querySelector(".product-table");
	if (!table) {
		return;
	}

	const preview = document.createElement("div");
	preview.className = "product-image-hover-preview";
	preview.setAttribute("aria-hidden", "true");
	preview.innerHTML = "<img alt=''>";
	document.body.appendChild(preview);

	const previewImage = preview.querySelector("img");
	const hidePreview = function() {
		preview.classList.remove("is-visible");
		previewImage.removeAttribute("src");
	};

	const showPreview = function(image) {
		if (!image || !image.src) {
			hidePreview();
			return;
		}

		previewImage.src = image.src;
		previewImage.alt = image.alt || "ภาพสินค้า";
		preview.classList.add("is-visible");
		position_product_image_preview(preview, image);
	};

	table.addEventListener("mouseover", function(event) {
		const image = event.target.closest(".product-table-img");
		if (image) {
			showPreview(image);
		}
	});

	table.addEventListener("mouseout", function(event) {
		const image = event.target.closest(".product-table-img");
		if (image && !image.contains(event.relatedTarget)) {
			hidePreview();
		}
	});

	table.addEventListener("focusin", function(event) {
		const image = event.target.closest(".product-table-img");
		if (image) {
			showPreview(image);
		}
	});

	table.addEventListener("focusout", function(event) {
		if (event.target.closest(".product-table-img")) {
			hidePreview();
		}
	});

	window.addEventListener("scroll", hidePreview, true);
	window.addEventListener("resize", hidePreview);
}

function position_product_image_preview(preview, image) {
	const gap = 16;
	const edge = 16;
	const imageRect = image.getBoundingClientRect();
	const previewRect = preview.getBoundingClientRect();

	let left = imageRect.right + gap;
	if (left + previewRect.width > window.innerWidth - edge) {
		left = imageRect.left - previewRect.width - gap;
	}
	left = Math.max(edge, Math.min(left, window.innerWidth - previewRect.width - edge));

	let top = imageRect.top + (imageRect.height / 2) - (previewRect.height / 2);
	top = Math.max(edge, Math.min(top, window.innerHeight - previewRect.height - edge));

	preview.style.left = left + "px";
	preview.style.top = top + "px";
}
