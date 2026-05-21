let customerChoices = null; 
let checkoutReceiptOrderId = null;

function refreshCustomerChoices(htmlContent) {
    const selectElement = document.getElementById('customer_Name');
    if (!selectElement) return;

    if (customerChoices) {
        customerChoices.destroy();
    }

    selectElement.innerHTML = htmlContent;

    customerChoices = new Choices(selectElement, {
        searchEnabled: true,
        shouldSort: false,
        allowHTML: true,
        placeholder: true,
        itemSelectText: ''
    });
}

document.addEventListener('DOMContentLoaded', () => {
    refreshCustomerChoices();
});

function checkout() {
	checkoutReceiptOrderId = null;
	const receiptButton = document.getElementById('btn_checkout_receipt');
	const saveButton = document.getElementById('btn_save');
	if (receiptButton) receiptButton.style.display = '';
	if (saveButton) saveButton.style.display = '';

    fetch('/order/loadcart')
        .then(response => response.json())
        .then(data => {
            document.getElementById('list_item').innerHTML = data.listDetail || '';
		
            const formatter = new Intl.NumberFormat('en-US', {
                minimumFractionDigits: 0,
                maximumFractionDigits: 0
            });

			refreshCustomerChoices(data.listCustomerName);
			document.getElementById('IdPayMethod').innerHTML = data.listPaymentMethod;
			document.getElementById('IdPayType').innerHTML = data.listPaymentType;
            document.getElementById('co_sum_price_total').value = formatter.format(data.total_price);
            document.getElementById('co_sum_price_pledge').value = formatter.format(data.pledge_price);
            document.getElementById('co_sum_price_balance').value = formatter.format(data.balance_price);
    
            if (typeof feather !== 'undefined') {
                feather.replace();
            }
			
			document.getElementById('IdPayMethod').value = '2';
			payMethod(2);
			document.getElementById('send_cost').value = '50';
			document.getElementById('order_date').value = '';
			calculateSummary();
			check_new_cus();
			payType(1);
			
            const modalElement = document.getElementById('modalCheckout');
            let myModal = bootstrap.Modal.getInstance(modalElement); 
            if (!myModal) {
                myModal = new bootstrap.Modal(modalElement);
            }
            myModal.show();
        })
        .catch(error => console.error('Error:', error));
}

function refreshList() {
	fetch('/order/loadcart')
	    .then(response => response.json())
		.then(data => {
			document.getElementById('list_item').innerHTML = data.listDetail || '';

			const formatter = new Intl.NumberFormat('en-US', {
			    minimumFractionDigits: 0,
			    maximumFractionDigits: 0
			});

			document.getElementById('co_sum_price_total').value = formatter.format(data.total_price);
			document.getElementById('co_sum_price_pledge').value = formatter.format(data.pledge_price);
			document.getElementById('co_sum_price_balance').value = formatter.format(data.balance_price);

			if (typeof feather !== 'undefined') {
	            feather.replace();
	        }
	    })
	    .catch(error => console.error('Error:', error));
}

function qty_down(id) {
	var qtyInput = document.getElementById('qty_' + id);
	var qty = parseInt(qtyInput.value) || 0;
	if (qty != 1) {
		qty = qty - 1;
		qtyInput.value = qty;
	}
	
	const payload = {
		qty: qty
	};

	fetch('/order/detail/update/' + id, {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json' },
	    body: JSON.stringify(payload)
	})
	.then(response => {
		if (response.ok) {
			refreshList();
	    }
	})
	.catch(err => console.error("Error:", err));
}

function save_new_order_duplicate_old() {
	const payload = buildCheckoutPayload();
	if (!payload) {
		return;
	}

	fetch('/order/save', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error("Save failed");
			}
			return response.json();
		})
		.then(() => {
			Swal.fire({
				title: "บันทึกสำเร็จ",
				icon: "success",
				confirmButtonText: "ตกลง"
			}).then((result) => {
				if (result.isConfirmed) {
					getCartCount();
					const modalElement = document.getElementById('modalCheckout');
					let myModal = bootstrap.Modal.getInstance(modalElement);
					if (!myModal) {
						myModal = new bootstrap.Modal(modalElement);
					}
					myModal.hide();
				}
			});
		})
		.catch(error => {
			console.error("Error:", error);
			Swal.fire({
				title: "บันทึกไม่สำเร็จ",
				text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
				icon: "error"
			});
		});
}

function buildCheckoutPayload() {
	const checkbox = document.getElementById("old_customer").checked;
	let customerName = '';
	if (!checkbox) {
		customerName = document.getElementById('customer_Name_new').value;
	} else {
		customerName = document.getElementById('customer_Name').value;
	}

	const idPayMethod = document.getElementById("IdPayMethod").value;
	const orderDate = document.getElementById("order_date").value;
	let idPayType = "";
	let lastPayDate = "";
	if (idPayMethod == 2) {
		idPayType = document.getElementById("IdPayType").value;
		if (idPayType == 2) {
			lastPayDate = document.getElementById("last_pay_date").value;
		}
	}

	if (!customerName || customerName == '') {
		Swal.fire({
			title: "กรุณากรอกชื่อลูกค้า",
			icon: "error",
			confirmButtonText: "ตกลง"
		});
		return null;
	}

	if (!orderDate) {
		Swal.fire({
			title: "กรุณาเลือกวันที่บันทึก",
			icon: "error",
			confirmButtonText: "ตกลง"
		});
		return null;
	}

	if (idPayMethod == 2 && idPayType == 2 && (!lastPayDate || lastPayDate == '')) {
		Swal.fire({
			title: "กรุณากรอกวันที่เก็บยอดที่เหลือ",
			icon: "error",
			confirmButtonText: "ตกลง"
		});
		return null;
	}

	const getNumber = (id) => {
		const value = document.getElementById(id).value || "0";
		return parseFloat(value.replace(/,/g, '')) || 0;
	};

	return {
		customer_name: customerName,
		pay_method: parseInt(idPayMethod),
		pay_type: idPayType ? parseInt(idPayType) : null,
		order_date: orderDate,
		last_pay_date: lastPayDate,
		send_cost: getNumber("send_cost"),
		discount: getNumber("discount"),
		price_total: getNumber("co_sum_price_total"),
		price_pledge: getNumber("co_sum_price_pledge"),
		price_balance: getNumber("co_sum_price_balance"),
		net: getNumber("net"),
		remark: document.getElementById("remark").value || ""
	};
}

function create_checkout_receipt() {
	const payload = buildCheckoutPayload();
	if (!payload) {
		return;
	}

	const preview = document.getElementById("checkoutReceiptPreview");
	preview.innerHTML = "<div class='text-center text-muted py-4'>กำลังโหลด...</div>";

	const modalElement = document.getElementById("checkoutReceiptModal");
	let receiptModal = bootstrap.Modal.getInstance(modalElement);
	if (!receiptModal) {
		receiptModal = new bootstrap.Modal(modalElement);
	}
	receiptModal.show();

	fetch("/order/receipt-preview", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(payload)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error("Receipt preview failed");
			}
			return response.text();
		})
		.then(html => {
			preview.innerHTML = html;
		})
		.catch(error => {
			console.error("Error loading checkout receipt preview:", error);
			preview.innerHTML = "<div class='text-center text-danger py-4'>โหลด Preview ใบเสร็จไม่สำเร็จ</div>";
		});
}

function save_new_order_preview_old() {
	const payload = buildCheckoutPayload();
	if (!payload) {
		return;
	}

	fetch('/order/save', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(payload)
	})
		.then(response => {
			if (!response.ok) {
				throw new Error("Save failed");
			}
			return response.json();
		})
		.then(() => {
			Swal.fire({
				title: "บันทึกสำเร็จ",
				icon: "success",
				confirmButtonText: "ตกลง"
			}).then((result) => {
				if (result.isConfirmed) {
					getCartCount();
					const modalElement = document.getElementById('modalCheckout');
					let myModal = bootstrap.Modal.getInstance(modalElement);
					if (!myModal) {
						myModal = new bootstrap.Modal(modalElement);
					}
					myModal.hide();
				}
			});
		})
		.catch(error => {
			console.error("Error:", error);
			Swal.fire({
				title: "บันทึกไม่สำเร็จ",
				text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
				icon: "error"
			});
		});
}

function create_checkout_receipt_legacy() {
	if (!checkoutReceiptOrderId) {
		Swal.fire({
			title: "ยังไม่มีข้อมูลใบเสร็จ",
			icon: "warning",
			confirmButtonText: "ตกลง"
		});
		return;
	}

	const preview = document.getElementById("checkoutReceiptPreview");
	preview.innerHTML = "<div class='text-center text-muted py-4'>กำลังโหลด...</div>";

	const modalElement = document.getElementById("checkoutReceiptModal");
	let receiptModal = bootstrap.Modal.getInstance(modalElement);
	if (!receiptModal) {
		receiptModal = new bootstrap.Modal(modalElement);
	}
	receiptModal.show();

	fetch("/orders/" + checkoutReceiptOrderId + "/receipt-fragment")
		.then(response => {
			if (!response.ok) {
				throw new Error("Receipt not found");
			}
			return response.text();
		})
		.then(html => {
			preview.innerHTML = html;
		})
		.catch(error => {
			console.error("Error loading checkout receipt:", error);
			preview.innerHTML = "<div class='text-center text-danger py-4'>โหลดใบเสร็จไม่สำเร็จ</div>";
		});
}

function copy_checkout_receipt_image() {
	const receiptElement = document.querySelector("#checkoutReceiptPreview .receipt");
	if (!receiptElement) {
		return;
	}

	if (typeof html2canvas === "undefined") {
		Swal.fire({
			title: "ไม่สามารถคัดลอกรูปได้",
			text: "ไม่พบตัวสร้างรูปภาพ",
			icon: "error"
		});
		return;
	}

	html2canvas(receiptElement, {
		backgroundColor: "#ffffff",
		scale: 2
	})
		.then(canvas => new Promise((resolve, reject) => {
			canvas.toBlob(blob => {
				if (!blob) {
					reject(new Error("Cannot create receipt image"));
					return;
				}
				resolve(blob);
			}, "image/png");
		}))
		.then(blob => navigator.clipboard.write([
			new ClipboardItem({ "image/png": blob })
		]))
		.then(() => {
			Swal.fire({
				title: "คัดลอกรูปใบเสร็จแล้ว",
				icon: "success",
				confirmButtonText: "ตกลง"
			});
		})
		.catch(error => {
			console.error("Error copying checkout receipt image:", error);
			Swal.fire({
				title: "คัดลอกรูปไม่สำเร็จ",
				text: "เบราว์เซอร์อาจไม่อนุญาตให้คัดลอกรูปภาพ",
				icon: "error"
			});
		});
}

function qty_up(id) {
	var qtyInput = document.getElementById('qty_' + id);
	var qty = parseInt(qtyInput.value) || 0;
	qty = qty + 1;
	qtyInput.value = qty;

	const payload = {
		qty: qty
	};

	fetch('/order/detail/update/' + id, {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json' },
	    body: JSON.stringify(payload)
	})
	.then(response => {
		if (response.ok) {
			refreshList();
	    }
	})
	.catch(err => console.error("Error:", err));
}

function delete_detail(id) {
	Swal.fire({
		title: "ต้องการลบหรือไม่",
		icon: "info",
		showDenyButton: true,
		confirmButtonText: "ยืนยัน",
		denyButtonText: "ยกเลิก"
	}).then((result) => {
		if (result.isConfirmed) {
			fetch('/order/detail/delete/' + id , {
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
							refreshList();
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

function calculateSummary() {
    const getNum = (id) => {
        const val = document.getElementById(id).value || "0";
        return parseFloat(val.replace(/,/g, '')) || 0;
    };

    const full = getNum('co_sum_price_total');
    const pledge = getNum('co_sum_price_pledge');
    const send = getNum('send_cost');
    const discount = getNum('discount');

    let net_total = (send + full) - discount;
    let balance = net_total - pledge;

    const formatter = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });

    document.getElementById('net').value = formatter.format(net_total);
    document.getElementById('co_sum_price_balance').value = formatter.format(balance);
}

function check_new_cus() {
	const checkbox = document.getElementById("old_customer");
	if (!checkbox.checked) {
		document.getElementById('div_customer_Name_new').style.display = '';
		document.getElementById('div_customer_Name').style.display = 'none';
	} else {
		document.getElementById('div_customer_Name_new').style.display = 'none';
		document.getElementById('div_customer_Name').style.display = '';
	}
}

function payMethod(val) {
	if (val == 1) {
		document.getElementById('full_price').style.display = '';
		document.getElementById('pledge_price').style.display = 'none';
		document.getElementById('balance_price').style.display = 'none';
		document.getElementById('div_PayType').style.display = 'none';
		payType(1);
	} else {
		document.getElementById('full_price').style.display = 'none';
		document.getElementById('pledge_price').style.display = '';
		document.getElementById('balance_price').style.display = '';
		document.getElementById('div_PayType').style.display = '';
		payType(document.getElementById('IdPayType').value);
	}
}

function payType(val) {
	if (val == 1) {
		document.getElementById('div_last_pay_date').style.display = 'none';
	} else {
		document.getElementById('div_last_pay_date').style.display = '';
	}
}

function save_new_order_legacy() {
	const checkbox = document.getElementById("old_customer").checked;
	var customer_Name = '';
	if (!checkbox) {
		customer_Name = document.getElementById('customer_Name_new').value;
	} else {
		customer_Name = document.getElementById('customer_Name').value;
	}
	const IdPayMethod = document.getElementById("IdPayMethod").value;
	var IdPayType = "";
	var last_pay_date = "";
	if (IdPayMethod == 2) {
		IdPayType = document.getElementById("IdPayType").value;
		if (IdPayType == 2) {
			last_pay_date = document.getElementById("last_pay_date").value;
		}
	}
	const send_cost = document.getElementById("send_cost").value || "0";
	const discount = document.getElementById("discount").value || "0";
	const sum_price_total = document.getElementById("co_sum_price_total").value;
	const sum_price_pledge = document.getElementById("co_sum_price_pledge").value;
	const sum_price_balance = document.getElementById("co_sum_price_balance").value;
	const net = document.getElementById("net").value;
	
	if (!customer_Name || customer_Name == '') {
		Swal.fire({
		  title: "กรุณากรอกชื่อลูกค้า",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}
	
	if (IdPayMethod == 2) {
		if (IdPayType == 2) {
			if (!last_pay_date || last_pay_date == '') {
				Swal.fire({
				  title: "กรุณากรอกวันที่เก็บยอดที่เหลือ",
				  text: "",
				  icon: "error",
				  confirmButtonText: "ตกลง"
				});
			    return;
			}
		}
	}
	
	const payload = {
	    customer_name: customer_Name,
		pay_method: IdPayMethod,
		pay_type: IdPayType,
		last_pay_date: last_pay_date,
		send_cost: parseFloat(send_cost.replace(/,/g, '')),
		discount: parseFloat(discount.replace(/,/g, '')),
		price_total: parseFloat(sum_price_total.replace(/,/g, '')),
		price_pledge: parseFloat(sum_price_pledge.replace(/,/g, '')),
		price_balance: parseFloat(sum_price_balance.replace(/,/g, '')),
		net: parseFloat(net.replace(/,/g, ''))
	};
	

	fetch('/order/save', {
	    method: 'POST',
	    headers: { 'Content-Type': 'application/json' },
	    body: JSON.stringify(payload)
	})
	.then(response => {
		if (response.ok) {
			return response.json().then(data => {
				checkoutReceiptOrderId = data.orderId;
				Swal.fire({
					title: "บันทึกสำเร็จ",
					icon: "success",
					confirmButtonText: "ตกลง"
				}).then((result) => {
					if (result.isConfirmed) {
						getCartCount();
						const saveButton = document.getElementById('btn_save');
						const receiptButton = document.getElementById('btn_checkout_receipt');
						if (saveButton) saveButton.style.display = 'none';
						if (receiptButton) receiptButton.style.display = '';
						if (typeof feather !== 'undefined') {
							feather.replace();
						}
					}
				});
			});
	        Swal.fire({
	            title: "บันทึกสำเร็จ",
	            icon: "success",
	            confirmButtonText: "ตกลง"
	        }).then((result) => {
	            if (result.isConfirmed) {
	                getCartCount();
					const saveButton = document.getElementById('btn_save');
					const receiptButton = document.getElementById('btn_checkout_receipt');
					if (saveButton) saveButton.style.display = 'none';
					if (receiptButton) receiptButton.style.display = '';
					if (typeof feather !== 'undefined') {
						feather.replace();
					}
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

function save_new_order() {
	const payload = buildCheckoutPayload();
	if (!payload) {
		return;
	}

	Swal.fire({
		title: "ยืนยันการบันทึก",
		text: "ต้องการบันทึกออร์เดอร์นี้หรือไม่",
		icon: "question",
		showCancelButton: true,
		confirmButtonText: "บันทึก",
		cancelButtonText: "ยกเลิก"
	}).then((result) => {
		if (!result.isConfirmed) {
			return;
		}

		fetch('/order/save', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(payload)
		})
			.then(response => {
				if (!response.ok) {
					throw new Error("Save failed");
				}
				return response.json();
			})
			.then(() => {
				Swal.fire({
					title: "บันทึกสำเร็จ",
					icon: "success",
					confirmButtonText: "ตกลง"
				}).then((result) => {
					if (result.isConfirmed) {
						getCartCount();
						const modalElement = document.getElementById('modalCheckout');
						let myModal = bootstrap.Modal.getInstance(modalElement);
						if (!myModal) {
							myModal = new bootstrap.Modal(modalElement);
						}
						myModal.hide();
					}
				});
			})
			.catch(error => {
				console.error("Error:", error);
				Swal.fire({
					title: "บันทึกไม่สำเร็จ",
					text: "เกิดข้อผิดพลาดที่ระบบหลังบ้าน",
					icon: "error"
				});
			});
	});
}
