function cov_search(id,type) {
	fetch('/product/cover/search/' + id)
	    .then(response => response.text())
	    .then(html => {
	        document.getElementById('mainCover').innerHTML = html;
	        
	        if (typeof feather !== 'undefined') {
	            feather.replace();
	        }
			
			if (type == "modal") {
				var myModal = new bootstrap.Modal(document.getElementById('modalCover'));
				myModal.show();
			} else {
				if(!document.getElementById('copydata').checked){
					cov_clr();
				}
			}
	    })
	    .catch(error => console.error('Error:', error));
}

function modal_cover(id) {
	document.getElementById('cov_IdProduct').value = id;
    fetch('/product/cover/' + id)
        .then(response => response.json())
    	.then(data => {
			console.log(data);
			document.getElementById('cov_pName').value = data.product.p_name;
			document.getElementById('cov_tName').value = data.product.t_name;
			document.getElementById('cov_aName').value = data.product.a_name;
			document.getElementById('cov_showImage').src = check_pic_null(data.product.p_pic);

			document.getElementById('cov_IdVersion').innerHTML = data.listVersion;
			document.getElementById('cov_IdWebsite').innerHTML = data.listWebsite;

			document.getElementById('cov_btn_save').style.display = '';
			document.getElementById('cov_btn_edit').style.display = 'none';
			
			cov_search(id,"modal");
        })
        .catch(error => console.error('Error:', error));
}

function cov_clr() {
	document.getElementById('cName').value = "";
	document.getElementById('price_total').value = "";
	document.getElementById('price_pledge').value = "";
	document.getElementById('price_balance').value = "";
	document.getElementById('price_1st').value = "";
	document.getElementById('price_2nd').value = "";
	document.getElementById('price_last').value = "";

	document.getElementById('cov_btn_save').style.display = '';
	document.getElementById('cov_btn_edit').style.display = 'none';
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
	const website = document.getElementById('cov_IdWebsite').value;
	const version = document.getElementById('cov_IdVersion').value;
	const name = document.getElementById('cName').value;
	const price_total = document.getElementById('price_total').value;
	const price_pledge = document.getElementById('price_pledge').value;
	const price_balance = document.getElementById('price_balance').value;
	const price_1st = document.getElementById('price_1st').value;
	const price_2nd = document.getElementById('price_2nd').value;
	const price_last = document.getElementById('price_last').value;

	if (!name) {
		Swal.fire({
		  title: "กรุณากรอกชื่อปก",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

    const payload = {
        product: product,
		website: website,
		version: version,
		name: name,
		price_total: price_total,
		price_pledge: price_pledge,
		price_balance: price_balance,
		price_1st: price_1st,
		price_2nd: price_2nd,
		price_last: price_last
    };

    fetch('/product/cover/save', {
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
                    cov_search(product,"load");
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

function cov_edit_data(id) {
	document.getElementById('IdCover').value = id;
    fetch('/product/cover/get/' + id)
        .then(response => response.json())
        .then(data => {

			document.getElementById('cName').value = data.name;
			document.getElementById('price_total').value = data.price_total;
			document.getElementById('price_pledge').value = data.price_pledge;
			document.getElementById('price_balance').value = data.price_balance;
			document.getElementById('price_1st').value = data.price_1st;
			document.getElementById('price_2nd').value = data.price_2nd;
			document.getElementById('price_last').value = data.price_last;
			
			document.getElementById('cov_btn_edit').style.display = '';
			document.getElementById('cov_btn_save').style.display = 'none';
        })
        .catch(err => console.error("Error fetching data:", err));
}

function cov_copy_data(id) {
	document.getElementById('IdCover').value = id;
    fetch('/product/cover/get/' + id)
        .then(response => response.json())
        .then(data => {

			document.getElementById('cName').value = data.name;
			document.getElementById('price_total').value = data.price_total;
			document.getElementById('price_pledge').value = data.price_pledge;
			document.getElementById('price_balance').value = data.price_balance;
			document.getElementById('price_1st').value = data.price_1st;
			document.getElementById('price_2nd').value = data.price_2nd;
			document.getElementById('price_last').value = data.price_last;
        })
        .catch(err => console.error("Error fetching data:", err));
}

function cov_save_edit_data() {
	const IdCover = document.getElementById('IdCover').value;
	const product = document.getElementById('cov_IdProduct').value;
	const website = document.getElementById('cov_IdWebsite').value;
	const version = document.getElementById('cov_IdVersion').value;
	const name = document.getElementById('cName').value;
	const price_total = document.getElementById('price_total').value;
	const price_pledge = document.getElementById('price_pledge').value;
	const price_balance = document.getElementById('price_balance').value;
	const price_1st = document.getElementById('price_1st').value;
	const price_2nd = document.getElementById('price_2nd').value;
	const price_last = document.getElementById('price_last').value;

	if (!name) {
		Swal.fire({
		  title: "กรุณากรอกชื่อปก",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	const payload = {
	    product: product,
		website: website,
		version: version,
		name: name,
		price_total: price_total,
		price_pledge: price_pledge,
		price_balance: price_balance,
		price_1st: price_1st,
		price_2nd: price_2nd,
		price_last: price_last
	};

    fetch('/product/cover/update/' + IdCover, {
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
					cov_search(product,"load");
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

function cov_delete_data(id) {
	const product = document.getElementById('cov_IdProduct').value;
	Swal.fire({
		title: "ต้องการลบหรือไม่",
		icon: "info",
		showDenyButton: true,
		confirmButtonText: "ยืนยัน",
		denyButtonText: "ยกเลิก"
	}).then((result) => {
		if (result.isConfirmed) {
			fetch('/product/cover/delete/' + id , {
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
							cov_search(product,"load");
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