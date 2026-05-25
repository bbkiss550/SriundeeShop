(function() {
    const moneyFormatter = new Intl.NumberFormat("th-TH", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const moneyShareDatasets = new Set(["artistSalesAmount", "typeSalesAmount", "costByType"]);
    let runtimeCharts = [];

    document.addEventListener("DOMContentLoaded", function() {
        const widgets = Array.isArray(window.dashboardRuntimeWidgets) ? window.dashboardRuntimeWidgets : [];
        const data = window.dashboardRuntimeData || {};
        if (!widgets.length) {
            return;
        }

        const custom = document.getElementById("customDashboardContent");
        if (!custom) {
            return;
        }

        custom.classList.remove("d-none");
        custom.innerHTML = widgets.map((widget, index) => `
            <div class="card dashboard-custom-widget tone-${escapeHtml(widget.tone)}" data-width="${escapeHtml(widget.width)}">
                <div class="card-body">
                    <h4>${escapeHtml(widget.title)}</h4>
                    <div id="dashboardRuntimeWidget${index}" class="${widget.type === "metric" ? "" : "dashboard-custom-chart"}"></div>
                </div>
            </div>
        `).join("");

        widgets.forEach((widget, index) => {
            const element = document.getElementById("dashboardRuntimeWidget" + index);
            if (widget.type === "metric") {
                renderMetric(element, widget, data);
                return;
            }
            const chart = renderChart(element, widget, data);
            if (chart) {
                runtimeCharts.push(chart);
            }
        });

        const themeToggle = document.getElementById("themeToggle");
        if (themeToggle) {
            themeToggle.addEventListener("click", function() {
                setTimeout(function() {
                    runtimeCharts.forEach(chart => chart.updateOptions({
                        chart: { foreColor: chartTextColor() },
                        grid: { borderColor: chartGridColor() },
                        legend: { labels: { colors: chartTextColor() } },
                        tooltip: { theme: chartThemeMode() }
                    }));
                }, 120);
            });
        }
    });

    function renderMetric(element, widget, data) {
        const metric = data.metrics?.[widget.dataset];
        element.innerHTML = `
            <div class="dashboard-custom-metric">${escapeHtml(metric?.display || "0")}</div>
            <div class="text-muted font-semibold">${escapeHtml(metric?.label || widget.title)}</div>
            <div class="text-muted small mt-2">ช่วง ${escapeHtml(data.period || "")}</div>
        `;
    }

    function renderChart(element, widget, data) {
        const rows = widget.dataset === "salesTrend"
            ? (data.series?.salesTrend || [])
            : (data.shares?.[widget.dataset] || []);
        if (!rows.length) {
            element.innerHTML = "<div class='dashboard-empty'>ไม่มีข้อมูลสำหรับแสดงผล</div>";
            return null;
        }

        const options = widget.dataset === "salesTrend"
            ? buildSeriesChart(widget, rows)
            : buildShareChart(widget, rows);
        const chart = new ApexCharts(element, options);
        chart.render();
        return chart;
    }

    function buildSeriesChart(widget, rows) {
        return {
            chart: { type: widget.type === "bar" ? "bar" : widget.type, height: 330, toolbar: { show: false }, foreColor: chartTextColor() },
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
            chart: { type, height: 330, toolbar: { show: false }, foreColor: chartTextColor() },
            series: type === "bar" ? [{ name: widget.title, data: rows.map(row => Number(row.value || 0)) }] : rows.map(row => Number(row.value || 0)),
            labels: rows.map(row => row.label || "ไม่ระบุ"),
            xaxis: type === "bar" ? { categories: rows.map(row => row.label || "ไม่ระบุ") } : undefined,
            colors: ["#435ebe", "#198754", "#ffc107", "#0dcaf0", "#dc3545", "#f97316", "#14b8a6", "#8b5cf6"],
            dataLabels: { enabled: type !== "bar" },
            grid: { borderColor: chartGridColor() },
            tooltip: { theme: chartThemeMode(), y: { formatter: value => formatShareValue(widget.dataset, value) } },
            legend: { position: "bottom", labels: { colors: chartTextColor() } }
        };
    }

    function formatShareValue(dataset, value) {
        if (moneyShareDatasets.has(dataset)) {
            return moneyFormatter.format(Number(value || 0));
        }
        return new Intl.NumberFormat("th-TH").format(Number(value || 0));
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
