function modal_version(id) {
	document.getElementById('ver_IdProduct').value = id;
    fetch('/product/version/' + id)
        .then(response => response.text())
        .then(html => {
            document.getElementById('mainVersion').innerHTML = html;
            
            if (typeof feather !== 'undefined') {
                feather.replace();
            }
			
			var myModal = new bootstrap.Modal(document.getElementById('modalVersion'));
			myModal.show();
        })
        .catch(error => console.error('Error:', error));
}

function clr() {
	document.getElementById('vName').value = "";

	document.getElementById('ver_btn_save').style.display = '';
	document.getElementById('ver_btn_edit').style.display = 'none';
}

function ver_delete_data(id) {
	const ver_IdProduct = document.getElementById('ver_IdProduct').value;
	Swal.fire({
		title: "ต้องการลบหรือไม่",
		icon: "info",
		showDenyButton: true,
		confirmButtonText: "ยืนยัน",
		denyButtonText: "ยกเลิก"
	}).then((result) => {
		if (result.isConfirmed) {
			fetch('/product/version/delete/' + id , {
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
						fetch('/product/version/' + ver_IdProduct)
						    .then(response => response.text())
						    .then(html => {
						        document.getElementById('mainVersion').innerHTML = html;
						        
						        if (typeof feather !== 'undefined') {
						            feather.replace();
						        }
								
								clr();
						    })
						    .catch(error => console.error('Error:', error));
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

function ver_save_new_data() {
	const ver_IdProduct = document.getElementById('ver_IdProduct').value;
	const vName = document.getElementById('vName').value;

    if (!vName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อเวอร์ชั่น",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
		product: ver_IdProduct,
        name: vName
    };

    fetch('/product/version/save', {
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
					fetch('/product/version/' + ver_IdProduct)
					    .then(response => response.text())
					    .then(html => {
					        document.getElementById('mainVersion').innerHTML = html;
					        
					        if (typeof feather !== 'undefined') {
					            feather.replace();
					        }
							
							clr();
					    })
					    .catch(error => console.error('Error:', error));
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

function ver_edit_data(id) {
	document.getElementById('IdVersion').value = id;
    fetch('/product/version/get/' + id)
        .then(response => response.json())
        .then(data => {
			document.getElementById('vName').value = data.name
			
			document.getElementById('ver_btn_edit').style.display = '';
			document.getElementById('ver_btn_save').style.display = 'none';
        })
        .catch(err => console.error("Error fetching data:", err));
}

function ver_save_edit_data() {
	const ver_IdProduct = document.getElementById('ver_IdProduct').value;
	const ver_IdVersion = document.getElementById('IdVersion').value;
	const vName = document.getElementById('vName').value;

    if (!vName) {
		Swal.fire({
		  title: "กรุณากรอกชื่อเวอร์ชั่น",
		  text: "",
		  icon: "error",
		  confirmButtonText: "ตกลง"
		});
        return;
    }

    const payload = {
        name: vName
    };

    fetch('/product/version/update/' + ver_IdVersion, {
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
					fetch('/product/version/' + ver_IdProduct)
					    .then(response => response.text())
					    .then(html => {
					        document.getElementById('mainVersion').innerHTML = html;
					        
					        if (typeof feather !== 'undefined') {
					            feather.replace();
					        }
							
							clr();
					    })
					    .catch(error => console.error('Error:', error));
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