document.addEventListener("DOMContentLoaded", function() {
    const form = document.getElementById("pricingForm");
    if (!form) {
        return;
    }

    const loadExchangeRateButton = document.getElementById("loadExchangeRate");
    const exchangeRateStatus = document.getElementById("exchangeRateStatus");
    const formatter = new Intl.NumberFormat("th-TH", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const wholeBahtFormatter = new Intl.NumberFormat("th-TH", { maximumFractionDigits: 0 });
    let timerId;

    const fields = {
        recommendedSellingPrice: document.getElementById("recommendedSellingPrice"),
        koreanCostTHB: document.getElementById("koreanCostTHB"),
        tierInfo: document.getElementById("tierInfo"),
        shippingMethodInfo: document.getElementById("shippingMethodInfo"),
        estimatedItemWeightKg: document.getElementById("estimatedItemWeightKg"),
        returnShippingRatePerKg: document.getElementById("returnShippingRatePerKg"),
        estimatedReturnShippingTHB: document.getElementById("estimatedReturnShippingTHB"),
        priceBeforeRounding: document.getElementById("priceBeforeRounding"),
        estimatedProfit: document.getElementById("estimatedProfit"),
        estimatedMarginPercent: document.getElementById("estimatedMarginPercent"),
        warningBox: document.getElementById("warningBox"),
        highRiskBadge: document.getElementById("highRiskBadge"),
        apiError: document.getElementById("apiError")
    };

    form.addEventListener("input", scheduleCalculation);
    form.addEventListener("change", scheduleCalculation);
    loadExchangeRateButton.addEventListener("click", function() {
        loadExchangeRate(false);
    });

    loadExchangeRate(true);

    function scheduleCalculation() {
        window.clearTimeout(timerId);
        timerId = window.setTimeout(calculate, 200);
    }

    async function calculate() {
        fields.apiError.classList.add("d-none");

        if (!form.checkValidity()) {
            form.classList.add("was-validated");
            clearResult();
            return;
        }

        form.classList.add("was-validated");

        const payload = {
            koreanPriceKRW: numberValueOf("koreanPriceKRW"),
            koreanShippingKRW: numberValueOf("koreanShippingKRW"),
            exchangeRate: numberValueOf("exchangeRate"),
            shippingTier: valueOf("shippingTier"),
            shippingMethod: valueOf("shippingMethod"),
            roundMode: valueOf("roundMode")
        };

        try {
            const response = await fetch("/api/pricing/calculate", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });
            const data = await response.json();

            if (!response.ok) {
                showApiError(data);
                clearResult();
                return;
            }

            renderResult(data);
        } catch (error) {
            fields.apiError.textContent = "ไม่สามารถเรียก API คำนวณราคาได้";
            fields.apiError.classList.remove("d-none");
            clearResult();
        }
    }

    async function loadExchangeRate(isAutomatic) {
        loadExchangeRateButton.disabled = true;
        exchangeRateStatus.textContent = "กำลังโหลดเรทล่าสุด...";

        try {
            const response = await fetch("/api/exchange-rate/krw-thb");
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.message || "Cannot load exchange rate.");
            }

            document.getElementById("exchangeRate").value = Number(data.rate).toFixed(4);
            exchangeRateStatus.textContent = `อัปเดตจาก ${data.source} วันที่ ${data.date}`;
        } catch (error) {
            exchangeRateStatus.textContent = isAutomatic
                ? "โหลดเรทอัตโนมัติไม่ได้ ใช้ค่าเริ่มต้นหรือกรอกเองได้"
                : "โหลดเรทไม่ได้ กรุณากรอกเอง";
        } finally {
            loadExchangeRateButton.disabled = false;
            calculate();
            if (window.feather) {
                feather.replace();
            }
        }
    }

    function renderResult(data) {
        fields.recommendedSellingPrice.textContent = `${wholeBahtFormatter.format(data.recommendedSellingPrice)} THB`;
        fields.koreanCostTHB.textContent = `${formatter.format(data.koreanCostTHB)} THB`;
        fields.tierInfo.textContent = `${data.shippingTier} - ${data.shippingTierDescription}`;
        fields.shippingMethodInfo.textContent = data.shippingMethodDescription;
        fields.estimatedItemWeightKg.textContent = `${formatter.format(data.estimatedItemWeightKg)} kg`;
        fields.returnShippingRatePerKg.textContent = `${formatter.format(data.returnShippingRatePerKg)} THB`;
        fields.estimatedReturnShippingTHB.textContent = `${formatter.format(data.estimatedReturnShippingTHB)} THB`;
        fields.priceBeforeRounding.textContent = `${formatter.format(data.priceBeforeRounding)} THB`;
        fields.estimatedProfit.textContent = `${formatter.format(data.estimatedProfit)} THB`;
        fields.estimatedMarginPercent.textContent = `${formatter.format(data.estimatedMarginPercent)}%`;

        fields.warningBox.textContent = data.warning || "";
        fields.warningBox.classList.toggle("d-none", !data.warning);
        fields.highRiskBadge.classList.toggle("d-none", !data.highRiskProduct);
    }

    function clearResult() {
        fields.recommendedSellingPrice.textContent = "-";
        fields.koreanCostTHB.textContent = "-";
        fields.tierInfo.textContent = "-";
        fields.shippingMethodInfo.textContent = "-";
        fields.estimatedItemWeightKg.textContent = "-";
        fields.returnShippingRatePerKg.textContent = "-";
        fields.estimatedReturnShippingTHB.textContent = "-";
        fields.priceBeforeRounding.textContent = "-";
        fields.estimatedProfit.textContent = "-";
        fields.estimatedMarginPercent.textContent = "-";
        fields.warningBox.classList.add("d-none");
        fields.highRiskBadge.classList.add("d-none");
    }

    function showApiError(data) {
        const errors = data.errors ? Object.values(data.errors).join(" ") : data.message;
        fields.apiError.textContent = errors || "ข้อมูลไม่ถูกต้อง";
        fields.apiError.classList.remove("d-none");
    }

    function valueOf(id) {
        return document.getElementById(id).value.trim();
    }

    function numberValueOf(id) {
        return document.getElementById(id).value;
    }
});
