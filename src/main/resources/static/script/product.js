function updateImage() {
    const url = document.getElementById('pPic').value;
    if (url.trim() !== "") {
        document.getElementById('showImage').src = url;
    } else {
        alert("กรุณาวางลิงก์รูปภาพก่อนครับ");
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
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function edit_data(id) {
	document.getElementById('IdProduct').value = id;
    fetch('/product/get/' + id)
        .then(response => response.json())
        .then(data => {
			document.getElementById('pName').value = data.name
			document.getElementById('IdType').value = data.type;
			document.getElementById('IdArtist').value = data.artist;
			if (data.end_date) {
				document.getElementById('end_date').value = data.end_date.split('T')[0];
			} else {
				document.getElementById('end_date').value = "";
			}
			if (data.send_date) {
				document.getElementById('send_date').value = data.send_date.split('T')[0];
			} else {
				document.getElementById('send_date').value = ""
			}
			if (data.second_pay_date) {
				document.getElementById('second_pay_date').value = data.second_pay_date.split('T')[0];
			} else {
				document.getElementById('second_pay_date').value = ""
			}
			document.getElementById('IdPaymentType').value = data.payment_type;
			if (data.last_pay_date) {
			document.getElementById('last_pay_date').value = data.last_pay_date.split('T')[0];
			} else {
				document.getElementById('last_pay_date').value = ""
			}
			document.getElementById('IdProductStatus').value = data.product_status;
			document.getElementById('pPic').value = data.pic;
			document.getElementById('showImage').src = data.pic;
			
			document.getElementById('btn_edit').style.display = '';
			document.getElementById('btn_save').style.display = 'none';
			
            var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
            myModal.show();
        })
        .catch(err => console.error("Error fetching data:", err));
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
			fetch('/manage/website/delete/' + id , {
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

function save_new_data() {
	const pName = document.getElementById('pName').value;
	const IdType = document.getElementById('IdType').value;
	const IdArtist = document.getElementById('IdArtist').value;
	const end_date = document.getElementById('end_date').value;
	const send_date = document.getElementById('send_date').value;
	const second_pay_date = document.getElementById('second_pay_date').value;
	const IdPaymentType = document.getElementById('IdPaymentType').value;
	const last_pay_date = document.getElementById('last_pay_date').value;
	const IdProductStatus = document.getElementById('IdProductStatus').value;
	const pPic = document.getElementById('pPic').value;

    if (!pName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อเว็บไศต์",
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

function save_edit_data() {
	const IdWebsite = document.getElementById('IdWebsite').value;
	const wName = document.getElementById('wName').value;

	if (!IdWebsite) {
		Swal.fire({
		  title: "ไม่มี IdWebsite",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}
	
    if (!wName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อเว็บไซต์",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
        websiteName: wName
    };
	
	fetch('/manage/website/update/' + IdWebsite , {
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