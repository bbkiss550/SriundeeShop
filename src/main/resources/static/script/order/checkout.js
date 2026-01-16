function checkout() {
	fetch('/order/loadcart')
	    .then(response => response.json())
		.then(data => {
			document.getElementById('list_item').innerHTML = data.listDetail;
	
			if (typeof feather !== 'undefined') {
	            feather.replace();
	        }
			
			var myModal = new bootstrap.Modal(document.getElementById('modalCheckout'));
			myModal.show();
	    })
	    .catch(error => console.error('Error:', error));
}

function qty_down(id) {
	var qty = document.querySelector('qty_' + id);
}