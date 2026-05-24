(function() {
    const lockState = new WeakMap();
    let recentAction = null;
    let recentActionTimer = null;

    const actionPatterns = [
        "save",
        "บันทึก",
        "แก้ไข",
        "รับเงิน"
    ];

    function isWritableRequest(input, init) {
        const method = String((init && init.method) || (input && input.method) || "GET").toUpperCase();
        return ["POST", "PUT", "PATCH", "DELETE"].includes(method);
    }

    function actionText(element) {
        if (!element) {
            return "";
        }
        return [
            element.id,
            element.name,
            element.className,
            element.getAttribute("onclick"),
            element.getAttribute("title"),
            element.getAttribute("aria-label"),
            element.textContent
        ].filter(Boolean).join(" ").toLowerCase();
    }

    function isSaveAction(element) {
        const text = actionText(element);
        return actionPatterns.some(pattern => text.includes(pattern.toLowerCase()));
    }

    function setElementBusy(element, busy) {
        if (!element) {
            return;
        }

        if (busy) {
            const count = (lockState.get(element) || 0) + 1;
            lockState.set(element, count);
            element.dataset.submitGuardBusy = "true";

            if (element.tagName === "BUTTON" || element.tagName === "INPUT") {
                element.disabled = true;
            } else {
                element.classList.add("disabled");
                element.setAttribute("aria-disabled", "true");
                element.style.pointerEvents = "none";
            }
            return;
        }

        const count = Math.max((lockState.get(element) || 1) - 1, 0);
        if (count > 0) {
            lockState.set(element, count);
            return;
        }

        lockState.delete(element);
        delete element.dataset.submitGuardBusy;

        if (element.tagName === "BUTTON" || element.tagName === "INPUT") {
            element.disabled = false;
        } else {
            element.classList.remove("disabled");
            element.removeAttribute("aria-disabled");
            element.style.pointerEvents = "";
        }
    }

    function rememberAction(element) {
        if (!element || !isSaveAction(element)) {
            return;
        }

        recentAction = element;
        setElementBusy(element, true);

        window.clearTimeout(recentActionTimer);
        recentActionTimer = window.setTimeout(function() {
            if (recentAction === element) {
                recentAction = null;
            }
            setElementBusy(element, false);
        }, 2500);
    }

    function extendCurrentLock() {
        const element = recentAction || document.activeElement;
        if (!element) {
            return null;
        }

        if (element.dataset && element.dataset.submitGuardBusy === "true") {
            window.clearTimeout(recentActionTimer);
            recentAction = null;
            return element;
        }

        if (isSaveAction(element)) {
            setElementBusy(element, true);
            return element;
        }

        return null;
    }

    document.addEventListener("click", function(event) {
        const target = event.target.closest("button, input[type='button'], input[type='submit'], a.btn");
        rememberAction(target);
    }, true);

    document.addEventListener("submit", function(event) {
        const submitter = event.submitter || event.target.querySelector("button[type='submit'], input[type='submit']");
        rememberAction(submitter);
    }, true);

    if (window.fetch) {
        const nativeFetch = window.fetch.bind(window);
        window.fetch = function(input, init) {
            const lockedElement = isWritableRequest(input, init) ? extendCurrentLock() : null;

            return nativeFetch(input, init).finally(function() {
                if (lockedElement) {
                    setElementBusy(lockedElement, false);
                }
            });
        };
    }
})();
