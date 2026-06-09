let myDataTable;

document.addEventListener("DOMContentLoaded", function () {
    myDataTable = new simpleDatatables.DataTable("#table2", {
        searchable: true,
        fixedColumns: false,
        perPage: 50,
        perPageSelect: [50, 100, 150, 200]
    });
});

function search_data() {
    const artistId = document.getElementById("selectArtist").value;
    const websiteId = document.getElementById("selectWebsite").value;
    const statusName = document.getElementById("selectStatus").value;
    const customerName = document.getElementById("inputCustomer").value;

    let params = new URLSearchParams();
    if (artistId) params.append("artistId", artistId);
    if (websiteId) params.append("websiteId", websiteId);
    if (statusName) params.append("statusName", statusName);
    if (customerName) params.append("customerName", customerName);

    fetch("/liststatus/search?" + params.toString())
        .then(response => response.text())
        .then(html => {
            if (myDataTable && myDataTable.table) {
                const tbody = myDataTable.table.tBodies[0];
                if (tbody) {
                    tbody.innerHTML = html;
                    myDataTable.refresh();
                }
            }
        })
        .catch(err => console.error("Search error:", err));
}

function reset_search() {
    document.getElementById("selectArtist").value = "";
    document.getElementById("selectWebsite").value = "";
    document.getElementById("selectStatus").value = "";
    document.getElementById("inputCustomer").value = "";
    search_data();
}
