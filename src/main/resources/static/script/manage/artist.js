function updateImage() {
    const url = document.getElementById('aPic').value;
    if (url.trim() !== "") {
        document.getElementById('showImage').src = url;
    } else {
        alert("กรุณาวางลิงก์รูปภาพก่อนครับ");
    }
}

function new_data() {
	document.getElementById('IdArt').value = "";
	document.getElementById('aName').value = "";
    document.getElementById('IdGroup').selectedIndex = 0;

	document.getElementById('btn_save').style.display = '';
	document.getElementById('btn_edit').style.display = 'none';
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function edit_data(id) {
	document.getElementById('IdArt').value = id;
    fetch('/manage/artist/get/' + id)
        .then(response => response.json())
        .then(data => {
            // นำข้อมูลที่ได้ไปใส่ใน Input ของ Modal
            document.getElementById('aName').value = data.name;
            document.getElementById('IdGroup').value = data.group;
			document.getElementById('aPic').value = data.logo;
			document.getElementById('showImage').src = check_pic_null(data.logo);
						
			document.getElementById('btn_edit').style.display = '';
			document.getElementById('btn_save').style.display = 'none';
			
            var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
            myModal.show();
        })
        .catch(err => console.error("Error fetching data:", err));
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
			fetch('/manage/artist/delete/' + id , {
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
	const aName = document.getElementById('aName').value;
    const idGroup = document.getElementById('IdGroup').value;
	const aPic = document.getElementById('aPic').value;

    if (!aName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อศิลปิน",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

	if (idGroup == -1) {
		Swal.fire({
		  title: "กรุณาเลือกค่าย",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }
	
    const payload = {
        artistName: aName,
        groupId: idGroup,
		logo: aPic
    };

    fetch('/manage/artist/save', {
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
                    location.reload(); // รีโหลดเฉพาะตอนบันทึกสำเร็จและกดตกลงแล้วเท่านั้น
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
	const IdArt = document.getElementById('IdArt').value;
	const aName = document.getElementById('aName').value;
    const idGroup = document.getElementById('IdGroup').value;
	const aPic = document.getElementById('aPic').value;

	if (!IdArt) {
		Swal.fire({
		  title: "ไม่มี IdArt",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}
	
    if (!aName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อศิลปิน",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

	if (idGroup == -1) {
		Swal.fire({
		  title: "กรุณาเลือกค่าย",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }
	
    const payload = {
        artistName: aName,
        groupId: idGroup,
		logo: aPic
    };
	
	fetch('/manage/artist/update/' + IdArt , {
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