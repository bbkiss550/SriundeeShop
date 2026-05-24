(function() {
    const moneyFormatter = new Intl.NumberFormat("th-TH", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    let widgets = Array.isArray(window.dashboardInitialWidgets) ? [...window.dashboardInitialWidgets] : [];
    let dashboardData = null;
    let previewCharts = [];
    let autoSaveTimer = null;
    let editingIndex = null;

    const chartTypeDatasets = new Set(["salesTrend", "artistSalesShare", "typeSalesShare", "orderStatusShare", "costByType"]);
    const metricDatasets = new Set(["totalSales", "totalFullPaid", "totalPledgePaid", "totalBalance", "totalCost", "totalOrders", "totalItems"]);

    document.addEventListener("DOMContentLoaded", function() {
        populateDatasetOptions();
        bindEvents();
        loadDashboardData().then(renderAll);
    });

    function bindEvents() {
        document.getElementById("dashboardWidgetType").addEventListener("change", populateDatasetOptions);
        document.getElementById("saveDashboardWidgetsButton").addEventListener("click", saveWidgets);
        document.getElementById("clearDashboardWidgetsButton").addEventListener("click", function() {
            confirmDelete("ล้าง Widget ทั้งหมด?", "รายการ Dashboard ที่จัดไว้จะถูกลบทั้งหมด").then(confirmed => {
                if (!confirmed) {
                    return;
                }
                widgets = [];
                editingIndex = null;
                clearForm();
                renderAll();
                scheduleAutoSave();
            });
        });
    }

    function populateDatasetOptions() {
        const type = document.getElementById("dashboardWidgetType").value;
        const select = document.getElementById("dashboardWidgetDataset");
        const catalog = window.dashboardCatalog || {};
        const options = type === "metric" ? (catalog.metrics || []) : (catalog.charts || []);
        select.innerHTML = options.map(item => `<option value="${escapeHtml(item.key)}">${escapeHtml(item.label)}</option>`).join("");
    }

    async function loadDashboardData() {
        const response = await fetch("/api/dashboard/data");
        dashboardData = await response.json();
    }

    function createWidgetFromForm() {
        const type = document.getElementById("dashboardWidgetType").value;
        const dataset = document.getElementById("dashboardWidgetDataset").value;
        if ((type === "metric" && !metricDatasets.has(dataset)) || (type !== "metric" && !chartTypeDatasets.has(dataset))) {
            Swal.fire({ title: "ข้อมูลไม่ตรงกับรูปแบบ", icon: "warning", confirmButtonText: "ตกลง" });
            return null;
        }

        return {
            id: "widget-" + Date.now(),
            title: document.getElementById("dashboardWidgetTitle").value.trim() || findDatasetLabel(dataset),
            type,
            dataset,
            width: document.getElementById("dashboardWidgetWidth").value,
            tone: "primary"
        };
    }

    function addWidget() {
        const widget = createWidgetFromForm();
        if (!widget) {
            return;
        }
        widgets.push(widget);
        clearForm();
        renderAll();
        scheduleAutoSave();
    }

    async function persistWidgets() {
        const response = await fetch("/settings/dashboard/widgets", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ widgets })
        });
        if (!response.ok) {
            throw new Error("Cannot save dashboard widgets.");
        }
        const data = await response.json();
        widgets = JSON.parse(data.widgets || "[]");
        renderAll();
        return data;
    }

    function scheduleAutoSave() {
        window.clearTimeout(autoSaveTimer);
        autoSaveTimer = window.setTimeout(function() {
            persistWidgets().catch(error => {
                console.error("Error auto-saving dashboard widgets:", error);
                Swal.fire({ title: "บันทึก Dashboard ไม่สำเร็จ", text: "ข้อมูลยังอยู่บนหน้าจอ กรุณากดบันทึกอีกครั้ง", icon: "error", confirmButtonText: "ตกลง" });
            });
        }, 350);
    }

    async function saveWidgets() {
        window.clearTimeout(autoSaveTimer);
        const widget = createWidgetFromForm();
        if (!widget) {
            return;
        }
        if (editingIndex !== null && widgets[editingIndex]) {
            widget.id = widgets[editingIndex].id || widget.id;
            widget.tone = widgets[editingIndex].tone || "primary";
            widgets[editingIndex] = widget;
        } else {
            widgets.push(widget);
        }
        editingIndex = null;
        clearForm();
        renderAll();
        await persistWidgets();
        Swal.fire({ title: "บันทึกสำเร็จ", icon: "success", confirmButtonText: "ตกลง" })
            .then(() => {
                window.location.href = "/";
            });
    }

    function renderAll() {
        renderWidgetList();
        renderPreview();
        if (window.feather) {
            feather.replace();
        }
    }

    function renderWidgetList() {
        const list = document.getElementById("dashboardWidgetList");
        if (!widgets.length) {
            list.innerHTML = "<div class='dashboard-builder-empty'>ยังไม่มี Widget ที่เลือก</div>";
            return;
        }
        list.innerHTML = widgets.map((widget, index) => `
            <div class="dashboard-widget-row">
                <div>
                    <div class="dashboard-widget-title">${escapeHtml(widget.title)}</div>
                    <div class="dashboard-widget-meta">${escapeHtml(typeLabel(widget.type))} / ${escapeHtml(findDatasetLabel(widget.dataset))} / ${widget.width}%</div>
                </div>
                <div class="dashboard-widget-actions">
                    <button type="button" class="btn icon btn-light-secondary" onclick="moveDashboardWidget(${index}, -1)" ${index === 0 ? "disabled" : ""}><i data-feather="arrow-up"></i></button>
                    <button type="button" class="btn icon btn-light-secondary" onclick="moveDashboardWidget(${index}, 1)" ${index === widgets.length - 1 ? "disabled" : ""}><i data-feather="arrow-down"></i></button>
                    <button type="button" class="btn icon btn-warning" onclick="editDashboardWidget(${index})"><i data-feather="edit-2"></i></button>
                    <button type="button" class="btn icon btn-danger" onclick="removeDashboardWidget(${index})"><i data-feather="trash-2"></i></button>
                </div>
            </div>
        `).join("");
    }

    window.moveDashboardWidget = function(index, direction) {
        const next = index + direction;
        if (next < 0 || next >= widgets.length) {
            return;
        }
        [widgets[index], widgets[next]] = [widgets[next], widgets[index]];
        renderAll();
        scheduleAutoSave();
    };

    window.removeDashboardWidget = function(index) {
        confirmDelete("ลบ Widget นี้?", "เมื่อลบแล้วรายการนี้จะหายจาก Dashboard").then(confirmed => {
            if (!confirmed) {
                return;
            }
            widgets.splice(index, 1);
            if (editingIndex === index) {
                editingIndex = null;
                clearForm();
            } else if (editingIndex !== null && editingIndex > index) {
                editingIndex -= 1;
            }
            renderAll();
            scheduleAutoSave();
        });
    };

    window.editDashboardWidget = function(index) {
        const widget = widgets[index];
        if (!widget) {
            return;
        }
        editingIndex = index;
        document.getElementById("dashboardWidgetTitle").value = widget.title || "";
        document.getElementById("dashboardWidgetType").value = widget.type || "metric";
        populateDatasetOptions();
        document.getElementById("dashboardWidgetDataset").value = widget.dataset || "";
        document.getElementById("dashboardWidgetWidth").value = widget.width || "50";
        document.getElementById("dashboardWidgetTitle").focus();
    };

    function renderPreview() {
        previewCharts.forEach(chart => chart.destroy());
        previewCharts = [];

        const preview = document.getElementById("dashboardPreview");
        if (!widgets.length) {
            preview.innerHTML = "";
            return;
        }

        preview.innerHTML = widgets.map((widget, index) => `
            <div class="card dashboard-preview-widget tone-${escapeHtml(widget.tone)}" data-width="${escapeHtml(widget.width)}">
                <div class="card-body">
                    <h5>${escapeHtml(widget.title)}</h5>
                    <div id="dashboardPreviewWidget${index}" class="${widget.type === "metric" ? "" : "dashboard-preview-chart"}"></div>
                </div>
            </div>
        `).join("");

        widgets.forEach((widget, index) => {
            const element = document.getElementById("dashboardPreviewWidget" + index);
            if (widget.type === "metric") {
                renderMetric(element, widget);
            } else {
                const chart = renderChart(element, widget);
                if (chart) {
                    previewCharts.push(chart);
                }
            }
        });
    }

    function renderMetric(element, widget) {
        const metric = dashboardData?.metrics?.[widget.dataset];
        element.innerHTML = `
            <div class="text-muted font-semibold">${escapeHtml(metric?.label || findDatasetLabel(widget.dataset))}</div>
            <div class="dashboard-preview-metric">${escapeHtml(metric?.display || "0")}</div>
            <div class="text-muted small">ช่วง ${escapeHtml(dashboardData?.period || "")}</div>
        `;
    }

    function renderChart(element, widget) {
        const rows = widget.dataset === "salesTrend"
            ? (dashboardData?.series?.salesTrend || [])
            : (dashboardData?.shares?.[widget.dataset] || []);
        if (!rows.length) {
            element.innerHTML = "<div class='dashboard-builder-empty'>ไม่มีข้อมูล</div>";
            return null;
        }

        const isShare = widget.dataset !== "salesTrend";
        const chartOptions = isShare ? buildShareChart(widget, rows) : buildSeriesChart(widget, rows);
        const chart = new ApexCharts(element, chartOptions);
        chart.render();
        return chart;
    }

    function buildSeriesChart(widget, rows) {
        return {
            chart: { type: widget.type === "bar" ? "bar" : widget.type, height: 260, toolbar: { show: false }, foreColor: chartTextColor() },
            series: [
                { name: "ยอดขาย", data: rows.map(row => Number(row.amount || 0)) },
                { name: "รับแล้ว", data: rows.map(row => Number(row.receivedPaid || 0)) },
                { name: "มัดจำ", data: rows.map(row => Number(row.pledgePaid || 0)) }
            ],
            xaxis: { categories: rows.map(row => row.label || "") },
            colors: ["#435ebe", "#198754", "#ffc107"],
            stroke: { curve: "smooth", width: 3 },
            dataLabels: { enabled: false },
            grid: { borderColor: chartGridColor() },
            tooltip: { theme: chartThemeMode(), y: { formatter: value => moneyFormatter.format(value) } },
            legend: { position: "bottom", labels: { colors: chartTextColor() } }
        };
    }

    function buildShareChart(widget, rows) {
        const type = widget.type === "donut" ? "donut" : widget.type === "pie" ? "pie" : "bar";
        return {
            chart: { type, height: 260, toolbar: { show: false }, foreColor: chartTextColor() },
            series: type === "bar" ? [{ name: widget.title, data: rows.map(row => Number(row.value || 0)) }] : rows.map(row => Number(row.value || 0)),
            labels: rows.map(row => row.label || "ไม่ระบุ"),
            xaxis: type === "bar" ? { categories: rows.map(row => row.label || "ไม่ระบุ") } : undefined,
            colors: ["#435ebe", "#198754", "#ffc107", "#0dcaf0", "#dc3545", "#f97316", "#14b8a6", "#8b5cf6"],
            dataLabels: { enabled: type !== "bar" },
            grid: { borderColor: chartGridColor() },
            tooltip: { theme: chartThemeMode() },
            legend: { position: "bottom", labels: { colors: chartTextColor() } }
        };
    }

    function findDatasetLabel(dataset) {
        const catalog = window.dashboardCatalog || {};
        const found = [...(catalog.metrics || []), ...(catalog.charts || [])].find(item => item.key === dataset);
        return found ? found.label : dataset;
    }

    function typeLabel(type) {
        return { metric: "ตัวเลข", line: "กราฟเส้น", bar: "กราฟแท่ง", area: "กราฟพื้นที่", pie: "กราฟวงกลม", donut: "กราฟโดนัท" }[type] || type;
    }

    function clearForm() {
        document.getElementById("dashboardWidgetTitle").value = "";
        document.getElementById("dashboardWidgetType").value = "metric";
        populateDatasetOptions();
        document.getElementById("dashboardWidgetWidth").value = "50";
    }

    async function confirmDelete(title, text) {
        const result = await Swal.fire({
            title,
            text,
            icon: "warning",
            showCancelButton: true,
            confirmButtonText: "ลบ",
            cancelButtonText: "ยกเลิก",
            confirmButtonColor: "#dc3545"
        });
        return result.isConfirmed;
    }

    function chartThemeMode() {
        return document.documentElement.dataset.theme === "dark" ? "dark" : "light";
    }

    function chartTextColor() {
        return chartThemeMode() === "dark" ? "#f8fafc" : "#1f2937";
    }

    function chartGridColor() {
        return chartThemeMode() === "dark" ? "#334155" : "#e5e7eb";
    }

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>"']/g, char => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
        }[char]));
    }
})();
