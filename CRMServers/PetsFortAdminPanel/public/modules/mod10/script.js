async function initMod10() {
    const lockedMessage = document.getElementById("server-config-locked");
    const form = document.getElementById("server-config-form");
    const refreshButton = document.getElementById("server-config-refresh");
    const baseUrlInput = document.getElementById("server-config-base-url");
    const terminalWsUrlInput = document.getElementById("server-config-terminal-ws-url");
    const terminalPreview = document.getElementById("server-config-terminal-preview");
    const updatedAtLabel = document.getElementById("server-config-updated-at");
    const updatedByLabel = document.getElementById("server-config-updated-by");

    if (!window.serverConfigService?.canEdit()) {
        if (lockedMessage) lockedMessage.style.display = "";
        if (form) form.style.display = "none";
        return;
    }

    function buildPreview(baseUrl, overrideValue) {
        const normalizedBase = (baseUrl || "").trim();
        if (overrideValue && overrideValue.trim()) {
            return overrideValue.trim();
        }

        try {
            const parsed = new URL(normalizedBase);
            const protocol = parsed.protocol === "https:" ? "wss:" : "ws:";
            return `${protocol}//${parsed.hostname}:8000/ws/terminal`;
        } catch (error) {
            return "Invalid backend URL";
        }
    }

    function fillForm(config) {
        baseUrlInput.value = config.targetUrl || "";
        const autoValue = buildPreview(config.targetUrl, "");
        terminalWsUrlInput.value = config.terminalWsUrl && config.terminalWsUrl !== autoValue ? config.terminalWsUrl : "";
        terminalPreview.textContent = `Effective terminal websocket URL: ${buildPreview(baseUrlInput.value, terminalWsUrlInput.value)}`;
        updatedAtLabel.textContent = `Last updated: ${config.updatedAt ? new Date(config.updatedAt).toLocaleString() : "Never"}`;
        updatedByLabel.textContent = `Updated by: ${config.updatedByEmail || "-"}`;
    }

    async function refreshConfig(forceRefresh = false) {
        showLoading();
        try {
            const config = await window.serverConfigService.load(forceRefresh);
            fillForm(config);
            feather.replace();
        } catch (error) {
            console.error("Failed to refresh server config", error);
            showDialog("err", "OK", "Failed to load server configuration.", function () {});
        } finally {
            hideLoading();
        }
    }

    refreshButton?.addEventListener("click", () => refreshConfig(true));

    baseUrlInput?.addEventListener("input", () => {
        terminalPreview.textContent = `Effective terminal websocket URL: ${buildPreview(baseUrlInput.value, terminalWsUrlInput.value)}`;
    });

    terminalWsUrlInput?.addEventListener("input", () => {
        terminalPreview.textContent = `Effective terminal websocket URL: ${buildPreview(baseUrlInput.value, terminalWsUrlInput.value)}`;
    });

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        showLoading();

        try {
            const config = await window.serverConfigService.save({
                baseUrl: baseUrlInput.value.trim(),
                terminalWsUrl: terminalWsUrlInput.value.trim()
            });

            fillForm(config);
            showDialog("info", "OK", "Server configuration updated successfully.", function () {});
        } catch (error) {
            console.error("Failed to save server config", error);
            showDialog("err", "OK", error.message || "Failed to save configuration.", function () {});
        } finally {
            hideLoading();
        }
    });

    await refreshConfig(false);
}

window.initMod10 = initMod10;
