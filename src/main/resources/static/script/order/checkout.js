let customerChoices = null; 

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
    fetch('/order/loadcart')
        .then(response => response.json())
        .then(data => {
            document.getElementById('list_item').innerHTML = data.listDetail;
		
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
			
			payMethod(1);
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
			document.getElementById('list_item').innerHTML = data.listDetail;

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

function save_new_data() {
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
	
	console.log(
		" customer_Name : " + customer_Name + 
		" IdPayMethod : " + IdPayMethod + 
		" IdPayType : " + IdPayType +
		" last_pay_date : " + last_pay_date + 
		" send_cost : " + send_cost + 
		" discount : " + discount +
		" sum_price_total : " + sum_price_total + 
		" sum_price_pledge : " + sum_price_pledge +
		" sum_price_balance : " + sum_price_balance + 
		" net : " + net
	);
}