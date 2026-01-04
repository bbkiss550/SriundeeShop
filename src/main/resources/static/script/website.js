function new_data() {
	document.getElementById('IdWebsite').value = "";
	document.getElementById('wName').value = "";

	document.getElementById('btn_save').style.display = '';
	document.getElementById('btn_edit').style.display = 'none';
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function edit_data(id) {
	document.getElementById('IdWebsite').value = id;
    fetch('/manage/website/get/' + id)
        .then(response => response.json())
        .then(data => {
            document.getElementById('wName').value = data.name;
			
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
	const wName = document.getElementById('wName').value;

    if (!wName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อเว็บไศต์",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
        websiteName: wName
    };

    fetch('/manage/website/save', {
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