async function initMod0() {
    console.log("Initializing Login Module (Mod4)...");

    // --- DOM Elements ---
    const loginFormFrag = document.getElementById('login-form-frag');
    const usernameInputFrag = document.getElementById('username-frag');
    const passwordInputFrag = document.getElementById('password-frag');
    const loginErrorFrag = document.getElementById('login-error-frag');

    // --- Event Listeners ---
    if (loginFormFrag) {
        loginFormFrag.addEventListener('submit', handleLogin);
    }

    function handleLogin(event) {
        event.preventDefault(); // Prevent default form submission

        const username = usernameInputFrag.value.trim();
        const password = passwordInputFrag.value.trim();

        // Replace this with your actual authentication logic (e.g., an API call)
        if (username === 'admin' && password === 'password') {
            console.log("Login successful!");
            // In a real application, you would likely redirect or update the UI
        } else {
            loginErrorFrag.style.display = 'block';
        }
    }
}

window.initMod0 = initMod0;