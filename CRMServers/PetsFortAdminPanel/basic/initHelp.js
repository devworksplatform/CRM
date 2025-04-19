// basic/initHelp.js

// --- LocalStorage Helpers ---
const StorageHelper = {
    save: (key, data) => {
        try {
            localStorage.setItem(key, JSON.stringify(data));
        } catch (e) {
            console.error(`Error saving data to localStorage for key "${key}":`, e);
            showToast(`Failed to save data. Storage might be full.`, 'error');
        }
    },
    load: (key, defaultValue = null) => {
        try {
            const data = localStorage.getItem(key);
            if (data === null) {
                return defaultValue;
            }
            // Handle potential empty string before parsing
            return data ? JSON.parse(data) : defaultValue;
        } catch (e) {
            console.error(`Error loading or parsing data from localStorage for key "${key}":`, e);
            return defaultValue;
        } 
    },
    remove: (key) => {
        localStorage.removeItem(key);
    },
    clearAll: () => {
        console.warn("Clearing specific app keys from localStorage.");
        StorageHelper.remove('users');
        StorageHelper.remove('categories');
        StorageHelper.remove('subcategories');
        StorageHelper.remove('products');
        StorageHelper.remove('orders');
        // Add any other keys your app uses
         showToast('Application data cleared from local storage.', 'info');
    }
};

// --- UI Helpers ---
function showToast(message, type = 'info', duration = 3000) {
    const toastElement = document.getElementById('toast-message');
    if (!toastElement) {
        console.error('Toast element not found!');
        return;
    }
    toastElement.textContent = message;
    toastElement.className = 'toast'; // Reset classes
    toastElement.classList.add(type);
    toastElement.classList.add('show');
    setTimeout(() => {
        toastElement.classList.remove('show');
    }, duration);
}

function generateId() {
    // Simple ID generator (timestamp + random string)
    return Date.now().toString(36) + Math.random().toString(36).substring(2, 7);
}

function escapeHtml(unsafe) {
    if (unsafe === null || typeof unsafe === 'undefined') return '';
    return String(unsafe)
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

// --- Image Handling (Basic URL Validation Example) ---
function isValidHttpUrl(string) {
  let url;
  try {
    url = new URL(string);
  } catch (_) {
    return false;
  }
  return url.protocol === "http:" || url.protocol === "https:";
}


// --- Initialization ---
console.log("initHelp.js loaded. Storage and UI helpers ready.");
// Note: Feather Icons and Day.js are initialized in index.html's inline script
// for better control over timing relative to DOM content loading.


async function callApi(method, url, body = null) {
    const options = {
        method: method.toUpperCase(),
        headers: {
        'Content-Type': 'application/json'
        }
    };

    if (body && ['POST', 'PUT'].includes(options.method)) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, options);

        if (!response.ok) {
        throw new Error(`HTTP ${response.status} - ${response.statusText}`);
        }

        return await response.json();
    } catch (err) {
        console.error('API call error:', err.message);
        throw err;
    }
}
  