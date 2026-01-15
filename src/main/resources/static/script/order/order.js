function modal_order(id) {
	//document.getElementById('cov_IdProduct').value = id;
	fetch('/product/cover/' + id)
	    .then(response => response.json())
		.then(data => {
			console.log(data);
			document.getElementById('cov_pName').value = data.product.p_name;
			document.getElementById('cov_tName').value = data.product.t_name;
			document.getElementById('cov_aName').value = data.product.a_name;
			document.getElementById('end_date').value = data.product.p_end_date;
			document.getElementById('cov_showImage').src = check_pic_null(data.product.p_pic);

			var myModal = new bootstrap.Modal(document.getElementById('modalOrder'));
			myModal.show();
	    })
	    .catch(error => console.error('Error:', error));
}

function check_pic_null(data) {
	var res = "/mazer/dist/assets/images/samples/no-photo.png";
	if (data) {
		res = data;
	}
	return res;
}