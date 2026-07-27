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
    const statusLabel = document.getElementById("backup-status");
    const actionButtons = [
        refreshButton, createButton, deleteSelectedButton, resetButton, deleteOldButton,
        selectAllCheckbox
    ];

    let backups = [];
    let selectedIds = new Set();
    let busy = false;

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

    function formatBytes(value) {
        const bytes = Number(value) || 0;
        if (bytes < 1024) return `${bytes} B`;
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    async function backupApi(method, url, body = null) {
        return await callApi(method, url, body);
    }

    function setBusy(nextBusy, message = "") {
        busy = nextBusy;
        document.querySelector(".backup-page")?.setAttribute("aria-busy", String(nextBusy));
        actionButtons.forEach((button) => {
            if (button) button.disabled = nextBusy;
        });
        tableBody.querySelectorAll("button, input").forEach((control) => {
            control.disabled = nextBusy;
        });
        if (!nextBusy) updateSelectionState();
        if (statusLabel && message) statusLabel.textContent = message;
    }

    function updateSelectionState() {
        const selectedCount = selectedIds.size;
        deleteSelectedButton.disabled = busy || selectedCount === 0;
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
                <td data-label="Select">
                    <input type="checkbox" class="backup-row-check" data-id="${escapeHtml(backup.id)}" ${isChecked ? "checked" : ""} aria-label="Select ${escapeHtml(backup.id)}">
                </td>
                <td data-label="Backup">
                    <div class="backup-id">
                        <span>${escapeHtml(backup.id)}</span>
                        ${backup.is_latest ? '<span class="backup-badge">Latest</span>' : ""}
                    </div>
                </td>
                <td data-label="Created">${escapeHtml(formatDate(backup.created_at))}</td>
                <td data-label="Contents">
                    <span class="backup-detail">${Number(backup.table_count) || 0} tables · ${Number(backup.record_count) || 0} records · ${escapeHtml(formatBytes(backup.size_bytes))}</span>
                </td>
                <td data-label="Path" class="backup-path">${escapeHtml(backup.path || `tables/${backup.id}`)}</td>
                <td data-label="Actions">
                    <div class="backup-row-actions">
                    <button class="btn btn-warn backup-restore-one" data-id="${escapeHtml(backup.id)}" aria-label="Restore backup ${escapeHtml(backup.id)}">
                        <i data-feather="upload-cloud"></i> Restore
                    </button>
                    <button class="btn btn-error backup-delete-one" data-id="${escapeHtml(backup.id)}" aria-label="Delete backup ${escapeHtml(backup.id)}">
                        <i data-feather="trash-2"></i> Delete
                    </button>
                    </div>
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
        tableBody.querySelectorAll(".backup-restore-one").forEach((button) => {
            button.addEventListener("click", () => confirmRestore(button.dataset.id));
        });

        updateSelectionState();
        feather.replace();
    }

    async function refreshBackups(manageBusy = true) {
        if (manageBusy) {
            setBusy(true, "Loading backups…");
            showLoading();
        }
        try {
            const data = await backupApi("GET", "backups/list");
            backups = Array.isArray(data.backups) ? data.backups : [];
            selectedIds = new Set([...selectedIds].filter((id) => backups.some((backup) => backup.id === id)));
            renderBackups();
            if (statusLabel) statusLabel.textContent = `Loaded ${backups.length} backup${backups.length === 1 ? "" : "s"}.`;
        } catch (error) {
            console.error("Failed to load backups", error);
            showDialog("err", "OK", error.message || "Failed to load backups.", function () {});
        } finally {
            if (manageBusy) {
                setBusy(false);
                hideLoading();
            }
        }
    }

    async function runBackupAction(message, action) {
        if (busy) return;
        setBusy(true, message);
        showLoading();
        try {
            await action();
            await refreshBackups(false);
        } catch (error) {
            console.error("Backup action failed", error);
            showDialog("err", "OK", error.message || "Backup action failed.", function () {});
        } finally {
            setBusy(false);
            hideLoading();
        }
    }

    function confirmDeleteOne(id) {
        showDialog("info", "Delete", "Cancel", `Delete backup "${id}"?`, async (action) => {
            if (action !== "Delete") return;
            await runBackupAction(`Deleting backup ${id}…`, async () => {
                await backupApi("DELETE", `backups/${encodeURIComponent(id)}`);
                selectedIds.delete(id);
                showToast("Backup deleted.", "info");
            });
        });
    }

    function confirmRestore(id) {
        showDialog(
            "info",
            "Restore Live Database",
            "Cancel",
            `Restore backup "${id}"? Current live data will be replaced. A safety backup will be created first.`,
            async (action) => {
                if (action !== "Restore Live Database") return;
                await runBackupAction(`Restoring backup ${id}…`, async () => {
                    const result = await backupApi("POST", `backups/${encodeURIComponent(id)}/restore`, {});
                    selectedIds.clear();
                    const safetyId = result.safety_backup?.id;
                    showToast(
                        safetyId ? `Restore complete. Safety backup: ${safetyId}` : "Restore complete.",
                        "info",
                        5000
                    );
                });
            }
        );
    }

    refreshButton?.addEventListener("click", refreshBackups);

    createButton?.addEventListener("click", async () => {
        await runBackupAction("Creating a full database backup…", async () => {
            await backupApi("POST", "backups/create");
            showToast("Backup created.", "info");
        });
    });

    deleteSelectedButton?.addEventListener("click", () => {
        const ids = [...selectedIds];
        if (!ids.length) return;

        showDialog("info", "Delete", "Cancel", `Delete ${ids.length} selected backup${ids.length === 1 ? "" : "s"}?`, async (action) => {
            if (action !== "Delete") return;
            await runBackupAction(`Deleting ${ids.length} selected backups…`, async () => {
                await backupApi("POST", "backups/delete-selected", { ids });
                selectedIds.clear();
                showToast("Selected backups deleted.", "info");
            });
        });
    });

    resetButton?.addEventListener("click", () => {
        showDialog("info", "Delete All", "Cancel", "Delete every backup and create a fresh backup as of now?", async (action) => {
            if (action !== "Delete All") return;
            await runBackupAction("Creating a fresh backup and replacing backup history…", async () => {
                await backupApi("POST", "backups/reset-current");
                selectedIds.clear();
                showToast("Backups reset and fresh backup created.", "info");
            });
        });
    });

    deleteOldButton?.addEventListener("click", () => {
        showDialog("info", "Delete", "Cancel", "Delete dated backups older than 1 week? The latest alias is kept.", async (action) => {
            if (action !== "Delete") return;
            await runBackupAction("Deleting backups older than one week…", async () => {
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
