function new_data() {
	document.getElementById('IdType').value = "";
	document.getElementById('tName').value = "";

	document.getElementById('btn_save').style.display = '';
	document.getElementById('btn_edit').style.display = 'none';
	
    var myModal = new bootstrap.Modal(document.getElementById('modalManageData'));
    myModal.show();
}

function edit_data(id) {
	document.getElementById('IdType').value = id;
    fetch('/manage/type/get/' + id)
        .then(response => response.json())
        .then(data => {
            document.getElementById('tName').value = data.name;
			
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
			fetch('/manage/type/delete/' + id , {
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
	const tName = document.getElementById('tName').value;

    if (!tName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อประเภท",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
        typeName: tName
    };

    fetch('/manage/type/save', {
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
	const IdType = document.getElementById('IdType').value;
	const tName = document.getElementById('tName').value;

	if (!IdType) {
		Swal.fire({
		  title: "ไม่มี IdType",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
	    return;
	}
	
    if (!tName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อประเภท",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
        typeName: tName
    };
	
	fetch('/manage/type/update/' + IdType , {
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