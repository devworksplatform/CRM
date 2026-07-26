async function initMod10() {
    const lockedMessage = document.getElementById("server-config-locked");
    const form = document.getElementById("server-config-form");
    const refreshButton = document.getElementById("server-config-refresh");
    const serverIdInput = document.getElementById("server-config-server-id");
    const timeoutInput = document.getElementById("server-config-timeout");
    const updatedAtLabel = document.getElementById("server-config-updated-at");
    const updatedByLabel = document.getElementById("server-config-updated-by");

    if (!window.serverConfigService?.canEdit()) {
        if (lockedMessage) lockedMessage.style.display = "";
        if (form) form.style.display = "none";
        return;
    }

    function fillForm(config) {
        serverIdInput.value = config.serverId || "";
        timeoutInput.value = config.timeoutMs || 45000;
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

    form?.addEventListener("submit", async (event) => {
        event.preventDefault();
        showLoading();

        try {
            const config = await window.serverConfigService.save({
                serverId: serverIdInput.value.trim(),
                timeoutMs: Number(timeoutInput.value)
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
