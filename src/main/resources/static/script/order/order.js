function modal_order(id) {
	document.getElementById('IdProduct').value = id;
	fetch('/order/load/' + id)
	    .then(response => {
			if (!response.ok) {
				throw new Error('load-product');
			}
			return response.json();
		})
		.then(data => {
			document.getElementById('cov_pName').value = data.product.p_name;
			document.getElementById('cov_tName').value = data.product.t_name;
			document.getElementById('cov_aName').value = data.product.a_name;
			document.getElementById('end_date').value = data.product.p_end_date;
			document.getElementById('cov_showImage').src = check_pic_null(data.product.p_pic);

			document.getElementById('data_qty').innerHTML = data.listQty;
			document.getElementById('data_website').innerHTML = data.listWebsite || '';
			document.getElementById('data_version').innerHTML = data.listVersion || '';
			document.getElementById('data_cover').innerHTML = data.listCover || '';
			defaultOrderModalChoices();

			var myModal = new bootstrap.Modal(document.getElementById('modalOrder'));
			myModal.show();
	    })
	    .catch(error => {
			console.error('Error:', error);
			Swal.fire({
				title: "โหลดข้อมูลสินค้าไม่สำเร็จ",
				icon: "error"
			});
		});
}

let productArtistChoices = null;

function initProductArtistFilterChoices() {
	const select = document.getElementById('productArtistFilter');
	if (!select || productArtistChoices || !window.Choices) {
		return;
	}

	productArtistChoices = new Choices(select, {
		searchEnabled: true,
		shouldSort: false,
		itemSelectText: '',
		searchPlaceholderValue: 'ค้นหาศิลปิน',
		noResultsText: 'ไม่พบข้อมูล',
		noChoicesText: 'ไม่มีข้อมูล'
	});

	select.addEventListener('change', applyProductFilters);
}

function applyProductFilters() {
	const showClosed = document.getElementById('showClosedProductSwitch')?.checked;
	const artistId = document.getElementById('productArtistFilter')?.value || '';
	const productName = normalizeProductFilterText(document.getElementById('productNameFilter')?.value || '');

	document.querySelectorAll('.product-grid-item').forEach(item => {
		const isClosedProduct = item.dataset.productStatus === '2';
		const artistMatched = !artistId || item.dataset.artistId === artistId;
		const nameMatched = !productName || normalizeProductFilterText(item.dataset.productName || '').includes(productName);

		item.classList.toggle('is-hidden-closed', (isClosedProduct && !showClosed) || !artistMatched || !nameMatched);
	});
}

function toggleClosedProducts() {
	applyProductFilters();
}

function normalizeProductFilterText(value) {
	return value.toString().trim().toLowerCase();
}

function check_pic_null(data) {
	var res = "/mazer/dist/assets/images/samples/no-photo.png";
	if (data) {
		res = data;
	}
	return res;
}

function defaultOrderModalChoices() {
	selectFirstRadio('group-website');
	selectFirstRadio('group-version');
	select_cover();
}

function selectFirstRadio(name) {
	const radios = document.querySelectorAll('input[name="' + name + '"]');
	if (radios.length > 0 && !document.querySelector('input[name="' + name + '"]:checked')) {
		radios[0].checked = true;
	}
	return document.querySelector('input[name="' + name + '"]:checked');
}

function select_cover(selectFirstCover = true) {
	const select_product = document.getElementById('IdProduct').value 
	const select_website = document.querySelector('input[name="group-website"]:checked');
	const select_version = document.querySelector('input[name="group-version"]:checked');

	if (select_product && select_website?.value && select_version?.value) {
		fetch('/order/loadcover/' + select_product + '/' + select_website.value + '/' + select_version.value)
		    .then(response => response.json())
			.then(data => {
				document.getElementById('data_cover').innerHTML = data.listCover || '';
				document.getElementById('price_total').value = "";
				document.getElementById('price_pledge').value = "";
				document.getElementById('price_balance').value = "";
				if (selectFirstCover) {
					selectFirstRadio('group-cover');
					select_price();
					return;
				}
				cal();
		    })
		    .catch(error => console.error('Error:', error));
	}
}

function select_price() {
	const select_cover = document.querySelector('input[name="group-cover"]:checked');

	if (select_cover?.value) {
		fetch('/order/getprice/' + select_cover.value)
		    .then(response => response.json())
			.then(data => {
				document.getElementById('price_total').value = data.price_total;
				document.getElementById('price_pledge').value = data.price_pledge;
				document.getElementById('price_balance').value = data.price_balance;
				cal();
		    })
		    .catch(error => console.error('Error:', error));
	}
}

function cal() {
	const qtyElement = document.querySelector('input[name="group-qty"]:checked');
    const qtyValue = qtyElement ? parseFloat(qtyElement.value) : 0;

    const total = parseFloat(document.getElementById('price_total').value) || 0;
    const pledge = parseFloat(document.getElementById('price_pledge').value) || 0;
    const balance = parseFloat(document.getElementById('price_balance').value) || 0;

    const formatter = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0
    });

    document.getElementById('sum_price_total').value = formatter.format(total * qtyValue);
    document.getElementById('sum_price_pledge').value = formatter.format(pledge * qtyValue);
    document.getElementById('sum_price_balance').value = formatter.format(balance * qtyValue);
}

function save_new_detail() {
	const cover = document.querySelector('input[name="group-cover"]:checked');
	const qty = document.querySelector('input[name="group-qty"]:checked');
	const price_total = document.getElementById('sum_price_total').value;
	const price_pledge = document.getElementById('sum_price_pledge').value;
	const price_balance = document.getElementById('sum_price_balance').value;

    const payload = {
        cover: cover.value,
		qty: qty.value,
		price_total: parseFloat(price_total.replace(/,/g, '')),
		price_pledge: parseFloat(price_pledge.replace(/,/g, '')),
		price_balance: parseFloat(price_balance.replace(/,/g, ''))
    };

    fetch('/order/detail/save', {
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
                    getCartCount();
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

function getCartCount() {
	fetch('/order/getcartcount')
	    .then(response => response.json())
		.then(data => {
			document.getElementById('cart-count').innerHTML = data;
	    })
	    .catch(error => console.error('Error:', error));
}
