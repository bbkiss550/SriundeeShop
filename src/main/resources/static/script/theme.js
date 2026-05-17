document.addEventListener("DOMContentLoaded", function() {
    loadSavedTheme();
    bindThemeToggle();
});

function loadSavedTheme() {
    fetch("/settings/theme")
        .then(response => response.json())
        .then(data => {
            applyTheme(data.theme || "light");
        })
        .catch(error => {
            console.error("Error loading theme:", error);
            applyTheme(localStorage.getItem("theme_mode") || "light");
        });
}

function bindThemeToggle() {
    const toggle = document.getElementById("themeToggle");
    if (!toggle) {
        return;
    }

    toggle.addEventListener("click", function() {
        const nextTheme = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
        applyTheme(nextTheme);
        saveTheme(nextTheme);
    });
}

function saveTheme(theme) {
    fetch("/settings/theme", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ theme: theme })
    }).catch(error => {
        console.error("Error saving theme:", error);
    });
}

function applyTheme(theme) {
    const normalizedTheme = theme === "dark" ? "dark" : "light";
    document.documentElement.dataset.theme = normalizedTheme;
    localStorage.setItem("theme_mode", normalizedTheme);
    updateThemeToggle(normalizedTheme);
}

function updateThemeToggle(theme) {
    const toggle = document.getElementById("themeToggle");
    const icon = document.getElementById("themeToggleIcon");
    const label = document.getElementById("themeToggleLabel");
    if (!toggle || !icon || !label) {
        return;
    }

    const isDark = theme === "dark";
    toggle.setAttribute("aria-pressed", isDark ? "true" : "false");
    icon.textContent = isDark ? "\u263E" : "\u2600";
    label.textContent = isDark ? "Dark" : "Light";
}
