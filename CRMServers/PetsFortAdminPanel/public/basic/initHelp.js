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

function parseDDMMYYYY(dateStr) {
    const [dd, mm, yyyy] = dateStr.split('-');
    return new Date(yyyy, mm - 1, dd);
}


// --- Initialization ---
console.log("initHelp.js loaded. Storage and UI helpers ready.");
// Note: Feather Icons and Day.js are initialized in index.html's inline script
// for better control over timing relative to DOM content loading.

    // --- Firebase Configuration ---
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
    apiKey: "AIzaSyA2pWmkmWSjD9cRbntYTXdZSglkHnNeZpk",
    authDomain: "pets-fort.firebaseapp.com",
    databaseURL: "https://pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "pets-fort",
    storageBucket: "pets-fort.firebasestorage.app",
    messagingSenderId: "268576564832",
    appId: "1:268576564832:web:5494647746c28f7b430505",
    measurementId: "G-D7MDH3KMVQ"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const storage = firebase.storage();

console.log("Firebase loaded and ready");

function isLoggedIn() {
    if(localStorage.getItem("userId") == null) {
        return false;
    } else return true;
}

async function handleLogout() {
    showLoading();
    try {
        await auth.signOut();
        localStorage.removeItem("userId");
        localStorage.removeItem("userdata");
        console.log("Logged out successfully!");
        hideLoading();
        // Reload the current module (assuming this initMod0 will be called again)
        window.loadModuleByName("Login");
    } catch (error) {
        console.error("Logout failed:", error);
        hideLoading();
        showDialog("err", "OK", "Logout failed!", function () { });
    }
}

function checkPermissionForAction(action, param1) {
    if(action == "loadModule") {
        const moduleName = param1;
        const userdata = JSON.parse(localStorage.getItem("userdata"));

        if(userdata == null) {
            if(moduleName == "Login") {
                return true;
            } else return false;
        }

        if(userdata.role == "4") return true;
        if(moduleName == "Login" || moduleName == "Orders") {
            return true;
        } else return false;
    } else if(action == "delete") {
      const type = param1;
      const userdata = JSON.parse(localStorage.getItem("userdata"));
      return userdata != null && userdata.role == "4";
    } else return false;
}

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
      // const response = await fetch("https://server.petsfort.in/"+url, options);
      const response = await fetch("https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com/"+url, options);

        if (!response.ok) {
        throw new Error(`HTTP ${response.status} - ${response.statusText}`);
        }

        return await response.json();
    } catch (err) {
        console.error('API call error:', err.message);
        throw err;
    }
}
  

async function uploadBlobToFirebase(blob, path) {
    const storageRef = storage.ref().child(path);

    try {
      const snapshot = await storageRef.put(blob);
      const downloadURL = await snapshot.ref.getDownloadURL();
      return downloadURL;
    } catch (error) {
      console.error('Upload failed:', error);
      throw error;
    }
  }



function showLoading() {
  // Remove existing if any
  const existingLoading = document.getElementById("enhanced-loading-screen");
  if (existingLoading) existingLoading.remove();

  // Create loading screen element
  const loadingDiv = document.createElement("div");
  loadingDiv.className = "enhanced-loading-screen";
  loadingDiv.id = "enhanced-loading-screen";
  loadingDiv.innerHTML = `
    <div class="enhanced-spinner-container">
      <div class="spinner-dot"></div>
      <div class="spinner-dot"></div>
      <div class="spinner-dot"></div>
      <div class="spinner-dot"></div>
    </div>`;
  document.body.appendChild(loadingDiv);
}

function hideLoading() {
  const loadingDiv = document.getElementById("enhanced-loading-screen");
  if (loadingDiv) {
    const spinnerContainer = loadingDiv.querySelector('.enhanced-spinner-container');

    // Animate spinner out first
    if (spinnerContainer) {
        spinnerContainer.style.animation = "loadingSpinnerDisappear 0.3s ease-in forwards";
    }

    // Animate backdrop fade out
    loadingDiv.style.animation = "loadingFadeOut 0.5s ease forwards";

    // Use durations from CSS
    const spinnerDuration = 300; // Match loadingSpinnerDisappear
    const backdropDuration = 500; // Match loadingFadeOut

    // Remove after the longer animation (backdrop) finishes
    setTimeout(() => {
      loadingDiv.remove();
    }, backdropDuration);
  }
}


// Basic SVG Icons (replace with more complex ones if needed)
const ICONS = {
    info: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>`,
    warn: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>`,
    err: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>` // Using same as info for demo, ideally use a cross mark
};

function showDialog(type, ...args) {
  // Remove existing dialog
  const existingBackdrop = document.getElementById("enhanced-dialog-backdrop");
  if (existingBackdrop) existingBackdrop.remove();

  const backdrop = document.createElement("div");
  backdrop.className = "enhanced-dialog-backdrop";
  backdrop.id = "enhanced-dialog-backdrop";

  const box = document.createElement("div");
  box.className = "enhanced-dialog-box";

  let title = "Dialog", buttonsHTML = "", message = "", callback, iconHTML = "";

  const iconClass = `${type}-icon`;
  iconHTML = `<div class="dialog-icon ${iconClass}">${ICONS[type] || ''}</div>`; // Add icon based on type

  if (type === "info") {
    const [btn1Text, btn2Text, infoText, cb] = args;
    title = "Information";
    message = infoText;
    callback = cb;
    // Using different button styles for primary/secondary actions
    buttonsHTML = `
      <button class="btn btn-secondary" onclick="handleEnhancedDialogClick('${btn2Text}')">${btn2Text}</button>
      <button class="btn btn-info" onclick="handleEnhancedDialogClick('${btn1Text}')">${btn1Text}</button>
    `;
  } else if (type === "warn") {
    const [btnText, warnText, cb] = args;
    title = "Warning";
    message = warnText;
    callback = cb;
    buttonsHTML = `<button class="btn btn-warn" onclick="handleEnhancedDialogClick('${btnText}')">${btnText}</button>`;
  } else if (type === "err") {
    const [btnText, errText, cb] = args;
    title = "Error";
    message = errText;
    callback = cb;
    buttonsHTML = `<button class="btn btn-error" onclick="handleEnhancedDialogClick('${btnText}')">${btnText}</button>`;
  } else {
      // Default/fallback case
      const [btnText, defaultText, cb] = args;
      title = "Message";
      message = defaultText;
      callback = cb;
      buttonsHTML = `<button class="btn btn-info" onclick="handleEnhancedDialogClick('${btnText}')">${btnText}</button>`;
  }

  box.innerHTML = `
    ${iconHTML}
    <h2>${title}</h2>
    <p>${message}</p>
    <div class="dialog-buttons">${buttonsHTML}</div>
  `;

  backdrop.appendChild(box);
  document.body.appendChild(backdrop);

  // Make callback globally accessible (simplified approach)
  window.handleEnhancedDialogClick = (btnText) => {
    const currentBackdrop = document.getElementById("enhanced-dialog-backdrop");
    const currentBox = currentBackdrop ? currentBackdrop.querySelector('.enhanced-dialog-box') : null;

    if (currentBackdrop && currentBox) {
         // Apply exit animations
        currentBox.style.animation = 'dialogBoxDisappear 0.3s ease-in forwards';
        currentBackdrop.style.animation = 'dialogBackdropFadeOut 0.4s ease forwards';

        // Remove after backdrop fade-out (longest animation)
        setTimeout(() => {
           currentBackdrop.remove();
        }, 400); // Match backdrop fade-out duration
    }

    // Execute the original callback after starting the animation
    if (callback) callback(btnText);

    // Clean up global function reference if desired
    // delete window.handleEnhancedDialogClick;
  };
}