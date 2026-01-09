function modal_cover(id) {
	document.getElementById('cov_IdProduct').value = id;
    fetch('/product/cover/' + id)
        .then(response => response.json())
    	.then(data => {
			console.log(data);
			document.getElementById('cov_pName').value = data.p_name;
			document.getElementById('cov_tName').value = data.t_name;
			document.getElementById('cov_aName').value = data.a_name;
			document.getElementById('cov_showImage').src = check_pic_null(data.p_pic);

			document.getElementById('cov_btn_save').style.display = '';
			document.getElementById('cov_btn_edit').style.display = 'none';
			
			var myModal = new bootstrap.Modal(document.getElementById('modalCover'));
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

function calculateProductPrices() {
    const total = parseFloat(document.getElementById('price_total').value) || 0;
    const pledge = parseFloat(document.getElementById('price_pledge').value) || 0;
    const first = parseFloat(document.getElementById('price_1st').value) || 0;
    const second = parseFloat(document.getElementById('price_2nd').value) || 0;

    const balance = total - pledge;
    document.getElementById('price_balance').value = balance > 0 ? balance : 0;

    const last = total - (first + second);
    document.getElementById('price_last').value = last > 0 ? last : 0;
}

function allowOnlyNumber(event) {
    event.target.value = event.target.value.replace(/[^0-9]/g, '');
	calculateProductPrices();
}

function cov_save_new_data() {
	const product = document.getElementById('cov_IdProduct').value;
	const website = document.getElementById('cov_Idwebsite').value;
	const version = document.getElementById('cov_IdVersion').value;
	const name = document.getElementById('name').value;
	const price_total = document.getElementById('price_total').value;
	const price_pledge = document.getElementById('price_pledge').value;
	const price_balance = document.getElementById('price_balance').value;
	const price_1st = document.getElementById('price_1st').value;
	const price_2nd = document.getElementById('price_2nd').value;
	const price_last = document.getElementById('price_last').value;

    const payload = {
        name: pName,
		type: IdType,
		artist: IdArtist,
		end_date: end_date,
		send_date: send_date,
		second_pay_date: second_pay_date,
		payment_type: IdPaymentType,
		last_pay_date: last_pay_date,
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