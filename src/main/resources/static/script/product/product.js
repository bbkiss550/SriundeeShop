function updateImage() {
    const url = document.getElementById('pPic').value;
    if (url.trim() !== "") {
        document.getElementById('showImage').src = url;
    } else {
        alert("กรุณาวางลิงก์รูปภาพก่อนครับ");
    }
}

function pt1_check(id) {
	if (id == 1 ) {
		document.getElementById('pt1').style.display = 'none';
	} else if (id == 2 ) {
		document.getElementById('pt1').style.display = '';
	}
}

function new_data() {
	document.getElementById('IdProduct').value = "";
	document.getElementById('pName').value = "";
	document.getElementById('IdType').selectedIndex = 0;
	document.getElementById('IdArtist').selectedIndex = 0;
	document.getElementById('end_date').value = "";
	document.getElementById('send_date').value = "";
	document.getElementById('second_pay_date').value = "";
	document.getElementById('IdPaymentType').selectedIndex = 0;
	document.getElementById('last_pay_date').value = "";
	document.getElementById('IdProductStatus').selectedIndex = 0;
	document.getElementById('pPic').value = "";
	document.getElementById('showImage').src = "/mazer/dist/assets/images/samples/no-photo.png";

	document.getElementById('btn_save').style.display = '';
	document.getElementById('btn_edit').style.display = 'none';
	
	pt1_check(1);
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function save_new_data() {
	const pName = document.getElementById('pName').value;
	const IdType = document.getElementById('IdType').value;
	const IdArtist = document.getElementById('IdArtist').value;
	const end_date = document.getElementById('end_date').value;
	const send_date = document.getElementById('send_date').value;
	const second_pay_date = document.getElementById('second_pay_date').value;
	const IdPaymentType = document.getElementById('IdPaymentType').value;
	var last_pay_date = document.getElementById('last_pay_date').value;
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

	if (!end_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่ปิดรับ",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!send_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่หำหนดส่ง",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}
	
		if (!second_pay_date) {
			Swal.fire({
			  title: "กรุณากรอกวันที่ชำระงวดที่สอง",
			  text: "",
			  icon: "error",
			  confirmButtonText: "ตกลง"
			});
		    return;
		}

	if (!IdPaymentType == 1) {
		if (!last_pay_date) {
			Swal.fire({
			  title: "กรุณากรอกวันที่เก็บยอดที่เหลือ",
			  text: "",
			  icon: "error",
			  confirmButtonText: "ตกลง"
			});
		    return;
		}
	} else {
		last_pay_date = null;
	}

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
			document.getElementById('second_pay_date').value = check_date_null(data.second_pay_date);
			document.getElementById('IdPaymentType').value = data.payment_type;
			document.getElementById('last_pay_date').value = check_date_null(data.last_pay_date);
			document.getElementById('IdProductStatus').value = data.product_status;
			document.getElementById('pPic').value = data.pic;
			document.getElementById('showImage').src = check_pic_null(data.pic);
			
			pt1_check(data.product_status);
			
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
	const end_date = document.getElementById('end_date').value;
	const send_date = document.getElementById('send_date').value;
	const second_pay_date = document.getElementById('second_pay_date').value;
	const IdPaymentType = document.getElementById('IdPaymentType').value;
	var last_pay_date = document.getElementById('last_pay_date').value;
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

	if (!end_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่ปิดรับ",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!send_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่หำหนดส่ง",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!second_pay_date) {
		Swal.fire({
		  title: "กรุณากรอกวันที่ชำระงวดที่สอง",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}

	if (!IdPaymentType == 1) {
		if (!last_pay_date) {
			Swal.fire({
			  title: "กรุณากรอกวันที่เก็บยอดที่เหลือ",
			  text: "",
			  icon: "error",
			  confirmButtonText: "ตกลง"
			});
		    return;
		}
	} else {
		last_pay_date = null;
	}

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