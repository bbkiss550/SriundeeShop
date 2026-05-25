(function() {
    const initialized = new WeakSet();
    const nativeValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value");
    const thaiMonths = [
        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    ];
    const thaiWeekdays = ["อา", "จ", "อ", "พ", "พฤ", "ศ", "ส"];
    let activePicker = null;

    document.addEventListener("DOMContentLoaded", function() {
        injectDatePickerStyles();
        initDateDisplayInputs(document);
        observeDateInputs();
    });

    window.refreshDateDisplayInputs = function(root) {
        initDateDisplayInputs(root || document);
    };

    function initDateDisplayInputs(root) {
        root.querySelectorAll("input[type='date']:not([data-native-date])").forEach(enhanceDateInput);
    }

    function enhanceDateInput(input) {
        if (initialized.has(input)) {
            return;
        }
        initialized.add(input);

        const display = document.createElement("input");
        display.type = "text";
        display.className = (input.className || "form-control") + " thai-date-display";
        display.placeholder = "วว/มม/ปปปป";
        display.autocomplete = "off";
        display.inputMode = "numeric";
        display.dataset.dateDisplayFor = input.id || "";
        display.required = input.required;
        display.disabled = input.disabled;
        display.readOnly = input.readOnly;
        display.value = toDisplayDate(nativeValue.get.call(input));

        input.dataset.nativeDate = "true";
        input.type = "hidden";
        input.after(display);

        try {
            Object.defineProperty(input, "value", {
                configurable: true,
                get: function() {
                    return nativeValue.get.call(input);
                },
                set: function(value) {
                    nativeValue.set.call(input, value || "");
                    display.value = toDisplayDate(value || "");
                }
            });
        } catch (error) {
            // Keep the visible field synced through events when the browser rejects redefining value.
        }

        display.addEventListener("input", function() {
            display.value = normalizeDisplayTyping(display.value);
            nativeValue.set.call(input, toNativeDate(display.value));
            input.dispatchEvent(new Event("input", { bubbles: true }));
        });

        display.addEventListener("change", function() {
            const nativeDate = toNativeDate(display.value);
            display.value = toDisplayDate(nativeDate);
            nativeValue.set.call(input, nativeDate);
            input.dispatchEvent(new Event("change", { bubbles: true }));
        });

        display.addEventListener("focus", function() {
            openDatePicker(input, display);
        });

        display.addEventListener("mousedown", function(event) {
            if (event.button === 0) {
                openDatePicker(input, display);
            }
        });

        display.addEventListener("click", function() {
            openDatePicker(input, display);
        });

        display.addEventListener("keydown", function(event) {
            if (event.key === "Escape") {
                closeDatePicker();
            }
        });
    }

    function observeDateInputs() {
        if (!window.MutationObserver) {
            return;
        }
        const observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType !== 1) {
                        return;
                    }
                    if (node.matches && node.matches("input[type='date']:not([data-native-date])")) {
                        enhanceDateInput(node);
                    }
                    if (node.querySelectorAll) {
                        initDateDisplayInputs(node);
                    }
                });
            });
        });
        observer.observe(document.body, { childList: true, subtree: true });
    }

    function openDatePicker(input, display) {
        if (display.disabled || display.readOnly) {
            return;
        }

        if (activePicker && activePicker.state.display === display) {
            positionDatePicker();
            return;
        }

        closeDatePicker();

        const selected = parseNativeDate(nativeValue.get.call(input)) || parseDisplayDate(display.value) || new Date();
        const state = {
            input: input,
            display: display,
            year: selected.getFullYear(),
            month: selected.getMonth()
        };

        const picker = document.createElement("div");
        picker.className = "thai-datepicker";
        picker.addEventListener("mousedown", function(event) {
            event.preventDefault();
        });
        document.body.appendChild(picker);
        activePicker = { element: picker, state: state };

        renderDatePicker();
        positionDatePicker();

        setTimeout(function() {
            document.addEventListener("mousedown", handleOutsidePickerClick, true);
            window.addEventListener("resize", positionDatePicker);
            window.addEventListener("scroll", positionDatePicker, true);
        }, 0);
    }

    function renderDatePicker() {
        if (!activePicker) {
            return;
        }

        const picker = activePicker.element;
        const state = activePicker.state;
        const selectedValue = nativeValue.get.call(state.input);
        const selected = parseNativeDate(selectedValue);
        const today = new Date();
        const firstOfMonth = new Date(state.year, state.month, 1);
        const gridStart = new Date(state.year, state.month, 1 - firstOfMonth.getDay());

        picker.innerHTML = "";

        const header = document.createElement("div");
        header.className = "thai-datepicker-header";
        header.appendChild(createNavButton("‹", function() {
            moveMonth(-1);
        }));

        const title = document.createElement("div");
        title.className = "thai-datepicker-title";
        title.textContent = thaiMonths[state.month] + " " + state.year;
        header.appendChild(title);

        header.appendChild(createNavButton("›", function() {
            moveMonth(1);
        }));
        picker.appendChild(header);

        const weekdays = document.createElement("div");
        weekdays.className = "thai-datepicker-weekdays";
        thaiWeekdays.forEach(function(day) {
            const item = document.createElement("div");
            item.textContent = day;
            weekdays.appendChild(item);
        });
        picker.appendChild(weekdays);

        const days = document.createElement("div");
        days.className = "thai-datepicker-days";
        for (let i = 0; i < 42; i += 1) {
            const date = new Date(gridStart);
            date.setDate(gridStart.getDate() + i);

            const button = document.createElement("button");
            button.type = "button";
            button.className = "thai-datepicker-day";
            button.textContent = String(date.getDate());

            if (date.getMonth() !== state.month) {
                button.classList.add("is-outside-month");
            }
            if (isSameDate(date, today)) {
                button.classList.add("is-today");
            }
            if (selected && isSameDate(date, selected)) {
                button.classList.add("is-selected");
            }

            button.addEventListener("click", function() {
                const value = toNativeFromDate(date);
                nativeValue.set.call(state.input, value);
                state.display.value = toDisplayDate(value);
                state.input.dispatchEvent(new Event("input", { bubbles: true }));
                state.input.dispatchEvent(new Event("change", { bubbles: true }));
                closeDatePicker();
            });
            days.appendChild(button);
        }
        picker.appendChild(days);

        const footer = document.createElement("div");
        footer.className = "thai-datepicker-footer";
        const todayButton = document.createElement("button");
        todayButton.type = "button";
        todayButton.textContent = "วันนี้";
        todayButton.addEventListener("click", function() {
            const value = toNativeFromDate(new Date());
            nativeValue.set.call(state.input, value);
            state.display.value = toDisplayDate(value);
            state.input.dispatchEvent(new Event("input", { bubbles: true }));
            state.input.dispatchEvent(new Event("change", { bubbles: true }));
            closeDatePicker();
        });
        footer.appendChild(todayButton);
        picker.appendChild(footer);
    }

    function createNavButton(text, onClick) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "thai-datepicker-nav";
        button.textContent = text;
        button.addEventListener("click", onClick);
        return button;
    }

    function moveMonth(offset) {
        if (!activePicker) {
            return;
        }
        const state = activePicker.state;
        const next = new Date(state.year, state.month + offset, 1);
        state.year = next.getFullYear();
        state.month = next.getMonth();
        renderDatePicker();
        positionDatePicker();
    }

    function positionDatePicker() {
        if (!activePicker) {
            return;
        }
        const picker = activePicker.element;
        const display = activePicker.state.display;
        const rect = display.getBoundingClientRect();
        const gap = 6;
        const pickerWidth = Math.max(286, rect.width);
        const left = Math.min(
            window.scrollX + rect.left,
            window.scrollX + window.innerWidth - pickerWidth - 12
        );
        let top = window.scrollY + rect.bottom + gap;

        picker.style.width = pickerWidth + "px";
        picker.style.left = Math.max(12, left) + "px";
        picker.style.top = top + "px";

        const pickerRect = picker.getBoundingClientRect();
        if (pickerRect.bottom > window.innerHeight - 12 && rect.top > pickerRect.height) {
            top = window.scrollY + rect.top - pickerRect.height - gap;
            picker.style.top = top + "px";
        }
    }

    function handleOutsidePickerClick(event) {
        if (!activePicker) {
            return;
        }
        if (activePicker.element.contains(event.target) || activePicker.state.display === event.target) {
            return;
        }
        closeDatePicker();
    }

    function closeDatePicker() {
        if (!activePicker) {
            return;
        }
        document.removeEventListener("mousedown", handleOutsidePickerClick, true);
        window.removeEventListener("resize", positionDatePicker);
        window.removeEventListener("scroll", positionDatePicker, true);
        activePicker.element.remove();
        activePicker = null;
    }

    function normalizeDisplayTyping(value) {
        const digits = String(value || "").replace(/\D/g, "").slice(0, 8);
        if (digits.length <= 2) {
            return digits;
        }
        if (digits.length <= 4) {
            return digits.slice(0, 2) + "/" + digits.slice(2);
        }
        return digits.slice(0, 2) + "/" + digits.slice(2, 4) + "/" + digits.slice(4);
    }

    function toDisplayDate(value) {
        const text = String(value || "").trim();
        const match = text.match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (!match) {
            return text;
        }
        return match[3] + "/" + match[2] + "/" + match[1];
    }

    function toNativeDate(value) {
        const date = parseDisplayDate(value) || parseNativeDate(value);
        return date ? toNativeFromDate(date) : "";
    }

    function parseDisplayDate(value) {
        const text = String(value || "").trim();
        const match = text.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
        if (!match) {
            return null;
        }
        return createValidDate(Number(match[3]), Number(match[2]) - 1, Number(match[1]));
    }

    function parseNativeDate(value) {
        const text = String(value || "").trim();
        const match = text.match(/^(\d{4})-(\d{2})-(\d{2})$/);
        if (!match) {
            return null;
        }
        return createValidDate(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    }

    function createValidDate(year, month, day) {
        const date = new Date(year, month, day);
        if (date.getFullYear() !== year || date.getMonth() !== month || date.getDate() !== day) {
            return null;
        }
        return date;
    }

    function toNativeFromDate(date) {
        return [
            date.getFullYear(),
            String(date.getMonth() + 1).padStart(2, "0"),
            String(date.getDate()).padStart(2, "0")
        ].join("-");
    }

    function isSameDate(left, right) {
        return left.getFullYear() === right.getFullYear()
            && left.getMonth() === right.getMonth()
            && left.getDate() === right.getDate();
    }

    function injectDatePickerStyles() {
        if (document.getElementById("thai-datepicker-styles")) {
            return;
        }
        const style = document.createElement("style");
        style.id = "thai-datepicker-styles";
        style.textContent = `
            .thai-date-display {
                cursor: pointer;
                background-image: linear-gradient(45deg, transparent, transparent), url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='%23dbeafe' viewBox='0 0 16 16'%3E%3Cpath d='M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5zM1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1z'/%3E%3C/svg%3E");
                background-repeat: no-repeat;
                background-position: right .75rem center;
                background-size: 16px 16px;
                padding-right: 2.35rem !important;
            }

            html[data-theme="light"] .thai-date-display {
                background-image: linear-gradient(45deg, transparent, transparent), url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='%231f2937' viewBox='0 0 16 16'%3E%3Cpath d='M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5zM1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4H1z'/%3E%3C/svg%3E");
            }

            .thai-datepicker {
                position: absolute;
                z-index: 5000;
                padding: .75rem;
                color: #e5edf7;
                background: #111827;
                border: 1px solid #334155;
                border-radius: 8px;
                box-shadow: 0 18px 44px rgba(0,0,0,.35);
                user-select: none;
            }

            html[data-theme="light"] .thai-datepicker {
                color: #1f2937;
                background: #ffffff;
                border-color: #cbd5e1;
                box-shadow: 0 16px 36px rgba(15,23,42,.18);
            }

            .thai-datepicker-header,
            .thai-datepicker-weekdays,
            .thai-datepicker-days {
                display: grid;
                grid-template-columns: repeat(7, 1fr);
                gap: .25rem;
            }

            .thai-datepicker-header {
                grid-template-columns: 36px 1fr 36px;
                align-items: center;
                margin-bottom: .55rem;
            }

            .thai-datepicker-title {
                text-align: center;
                font-weight: 800;
            }

            .thai-datepicker-nav,
            .thai-datepicker-day,
            .thai-datepicker-footer button {
                border: 0;
                border-radius: 6px;
                color: inherit;
                background: transparent;
            }

            .thai-datepicker-nav {
                width: 36px;
                height: 32px;
                font-size: 1.25rem;
                line-height: 1;
            }

            .thai-datepicker-nav:hover,
            .thai-datepicker-day:hover,
            .thai-datepicker-footer button:hover {
                background: #263449;
            }

            html[data-theme="light"] .thai-datepicker-nav:hover,
            html[data-theme="light"] .thai-datepicker-day:hover,
            html[data-theme="light"] .thai-datepicker-footer button:hover {
                background: #e8eef8;
            }

            .thai-datepicker-weekdays {
                margin-bottom: .25rem;
                color: #93c5fd;
                font-size: .8rem;
                font-weight: 800;
                text-align: center;
            }

            .thai-datepicker-weekdays > div:first-child {
                color: #f87171;
            }

            .thai-datepicker-day {
                min-height: 34px;
                font-weight: 700;
            }

            .thai-datepicker-day:nth-child(7n + 1) {
                color: #f87171;
            }

            .thai-datepicker-day.is-outside-month {
                color: #64748b;
            }

            .thai-datepicker-day.is-today {
                outline: 1px solid #60a5fa;
            }

            .thai-datepicker-day.is-selected {
                color: #ffffff;
                background: #435ebe;
            }

            .thai-datepicker-footer {
                display: flex;
                justify-content: flex-end;
                margin-top: .55rem;
                padding-top: .55rem;
                border-top: 1px solid #334155;
            }

            html[data-theme="light"] .thai-datepicker-footer {
                border-top-color: #e2e8f0;
            }

            .thai-datepicker-footer button {
                padding: .3rem .65rem;
                color: #93c5fd;
                font-weight: 800;
            }
        `;
        document.head.appendChild(style);
    }
})();
