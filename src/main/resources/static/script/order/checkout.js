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
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            });

			refreshCustomerChoices(data.listCustomerName);
			document.getElementById('IdPayMethod').innerHTML = data.listPaymentMethod;
            document.getElementById('co_sum_price_total').value = formatter.format(data.total_price);
            document.getElementById('co_sum_price_pledge').value = formatter.format(data.pledge_price);
            document.getElementById('co_sum_price_balance').value = formatter.format(data.balance_price);
    
            if (typeof feather !== 'undefined') {
                feather.replace();
            }
			
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
			    minimumFractionDigits: 2,
			    maximumFractionDigits: 2
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