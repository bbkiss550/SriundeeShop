function modal_cover(id) {
	document.getElementById('ver_IdProduct').value = id;
    fetch('/product/version/' + id)
        .then(response => response.text())
        .then(html => {
            document.getElementById('mainVersion').innerHTML = html;
            
            if (typeof feather !== 'undefined') { feather.replace(); }
			
			var myModal = new bootstrap.Modal(document.getElementById('modalCover'));
			myModal.show();
        })
        .catch(error => console.error('Error:', error));
}