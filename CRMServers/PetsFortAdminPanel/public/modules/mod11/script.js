async function initMod11() {
    const lockedMessage = document.getElementById("backup-locked");
    const content = document.getElementById("backup-content");
    const refreshButton = document.getElementById("backup-refresh");
    const createButton = document.getElementById("backup-create");
    const deleteSelectedButton = document.getElementById("backup-delete-selected");
    const resetButton = document.getElementById("backup-reset-current");
    const deleteOldButton = document.getElementById("backup-delete-old");
    const selectAllCheckbox = document.getElementById("backup-select-all");
    const tableBody = document.getElementById("backup-table-body");
    const emptyMessage = document.getElementById("backup-empty");
    const countLabel = document.getElementById("backup-count");
    const lastLabel = document.getElementById("backup-last");

    let backups = [];
    let selectedIds = new Set();

    if (!window.serverConfigService?.canEdit()) {
        if (lockedMessage) lockedMessage.style.display = "";
        if (content) content.style.display = "none";
        return;
    }

    function formatDate(value) {
        if (!value) return "-";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;
        return date.toLocaleString();
    }

    function getLastBackup() {
        return backups.find((item) => !item.is_latest && item.created_at) || backups.find((item) => item.is_latest);
    }

    function waitForAuthUser(timeoutMs = 5000) {
        if (auth.currentUser) {
            return Promise.resolve(auth.currentUser);
        }

        return new Promise((resolve) => {
            let settled = false;
            let timer = null;
            let unsubscribe = null;
            unsubscribe = auth.onAuthStateChanged((user) => {
                if (settled) return;
                settled = true;
                if (timer) clearTimeout(timer);
                if (unsubscribe) unsubscribe();
                resolve(user);
            });

            timer = setTimeout(() => {
                if (settled) return;
                settled = true;
                if (unsubscribe) unsubscribe();
                resolve(auth.currentUser);
            }, timeoutMs);
        });
    }

    async function backupApi(method, url, body = null) {
        const user = await waitForAuthUser();
        if (!user) {
            throw new Error("Please login again.");
        }

        const token = await user.getIdToken();
        const options = {
            method: method.toUpperCase(),
            headers: {
                "Authorization": `Bearer ${token}`,
                "Content-Type": "application/json"
            }
        };

        if (body && ["POST", "PUT"].includes(options.method)) {
            options.body = JSON.stringify(body);
        }

        const response = await fetch(SERVER_URL + url, options);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `HTTP ${response.status} - ${response.statusText}`);
        }

        return await response.json();
    }

    function updateSelectionState() {
        const selectedCount = selectedIds.size;
        deleteSelectedButton.disabled = selectedCount === 0;
        deleteSelectedButton.innerHTML = `<i data-feather="trash-2"></i> Delete Selected${selectedCount ? ` (${selectedCount})` : ""}`;
        if (selectAllCheckbox) {
            selectAllCheckbox.checked = backups.length > 0 && selectedCount === backups.length;
            selectAllCheckbox.indeterminate = selectedCount > 0 && selectedCount < backups.length;
        }
        feather.replace();
    }

    function renderBackups() {
        tableBody.innerHTML = "";

        if (!backups.length) {
            emptyMessage.style.display = "";
        } else {
            emptyMessage.style.display = "none";
        }

        backups.forEach((backup) => {
            const row = document.createElement("tr");
            const isChecked = selectedIds.has(backup.id);
            row.innerHTML = `
                <td>
                    <input type="checkbox" class="backup-row-check" data-id="${escapeHtml(backup.id)}" ${isChecked ? "checked" : ""} aria-label="Select ${escapeHtml(backup.id)}">
                </td>
                <td>
                    <div class="backup-id">
                        <span>${escapeHtml(backup.id)}</span>
                        ${backup.is_latest ? '<span class="backup-badge">Latest</span>' : ""}
                    </div>
                </td>
                <td>${escapeHtml(formatDate(backup.created_at))}</td>
                <td class="backup-path">${escapeHtml(backup.path || `tables/${backup.id}`)}</td>
                <td>
                    <button class="btn btn-error backup-delete-one" data-id="${escapeHtml(backup.id)}">
                        <i data-feather="trash-2"></i> Delete
                    </button>
                </td>
            `;
            tableBody.appendChild(row);
        });

        const backupCount = backups.length;
        const lastBackup = getLastBackup();
        countLabel.textContent = `${backupCount} backup${backupCount === 1 ? "" : "s"}`;
        lastLabel.textContent = `Last backup: ${lastBackup ? `${lastBackup.id} (${formatDate(lastBackup.created_at)})` : "-"}`;

        tableBody.querySelectorAll(".backup-row-check").forEach((checkbox) => {
            checkbox.addEventListener("change", (event) => {
                const id = event.target.dataset.id;
                if (event.target.checked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.delete(id);
                }
                updateSelectionState();
            });
        });

        tableBody.querySelectorAll(".backup-delete-one").forEach((button) => {
            button.addEventListener("click", () => confirmDeleteOne(button.dataset.id));
        });

        updateSelectionState();
        feather.replace();
    }

    async function refreshBackups() {
        showLoading();
        try {
            const data = await backupApi("GET", "backups/list");
            backups = Array.isArray(data.backups) ? data.backups : [];
            selectedIds = new Set([...selectedIds].filter((id) => backups.some((backup) => backup.id === id)));
            renderBackups();
        } catch (error) {
            console.error("Failed to load backups", error);
            showDialog("err", "OK", "Failed to load backups.", function () {});
        } finally {
            hideLoading();
        }
    }

    async function runBackupAction(action) {
        showLoading();
        try {
            await action();
            await refreshBackups();
        } catch (error) {
            console.error("Backup action failed", error);
            showDialog("err", "OK", error.message || "Backup action failed.", function () {});
        } finally {
            hideLoading();
        }
    }

    function confirmDeleteOne(id) {
        showDialog("info", "Delete", "Cancel", `Delete backup "${id}"?`, async (action) => {
            if (action !== "Delete") return;
            await runBackupAction(async () => {
                await backupApi("DELETE", `backups/${encodeURIComponent(id)}`);
                selectedIds.delete(id);
                showToast("Backup deleted.", "info");
            });
        });
    }

    refreshButton?.addEventListener("click", refreshBackups);

    createButton?.addEventListener("click", async () => {
        await runBackupAction(async () => {
            await backupApi("POST", "backups/create");
            showToast("Backup created.", "info");
        });
    });

    deleteSelectedButton?.addEventListener("click", () => {
        const ids = [...selectedIds];
        if (!ids.length) return;

        showDialog("info", "Delete", "Cancel", `Delete ${ids.length} selected backup${ids.length === 1 ? "" : "s"}?`, async (action) => {
            if (action !== "Delete") return;
            await runBackupAction(async () => {
                await backupApi("POST", "backups/delete-selected", { ids });
                selectedIds.clear();
                showToast("Selected backups deleted.", "info");
            });
        });
    });

    resetButton?.addEventListener("click", () => {
        showDialog("info", "Delete All", "Cancel", "Delete every backup and create a fresh backup as of now?", async (action) => {
            if (action !== "Delete All") return;
            await runBackupAction(async () => {
                await backupApi("POST", "backups/reset-current");
                selectedIds.clear();
                showToast("Backups reset and fresh backup created.", "info");
            });
        });
    });

    deleteOldButton?.addEventListener("click", () => {
        showDialog("info", "Delete", "Cancel", "Delete dated backups older than 1 week? The latest alias is kept.", async (action) => {
            if (action !== "Delete") return;
            await runBackupAction(async () => {
                await backupApi("DELETE", "backups/older-than-days/7");
                selectedIds.clear();
                showToast("Old backups deleted.", "info");
            });
        });
    });

    selectAllCheckbox?.addEventListener("change", (event) => {
        if (event.target.checked) {
            selectedIds = new Set(backups.map((backup) => backup.id));
        } else {
            selectedIds.clear();
        }
        renderBackups();
    });

    await refreshBackups();
}

window.initMod11 = initMod11;
