async function initMod0() {
    console.log("Initializing Login Module (Mod0) with Firebase...");

    const loginFormFrag = document.getElementById('login-form-frag');
    const loginContainer = document.querySelector('.card-body'); // Assuming the form is inside this div

    // --- DOM Elements ---
    const usernameInputFrag = document.getElementById('username-frag');
    const passwordInputFrag = document.getElementById('password-frag');

    // Check if user data exists in local storage
    const storedUserData = localStorage.getItem("userdata");
    const userId = localStorage.getItem("userId");

    if (storedUserData && userId) {
        const userdata = JSON.parse(storedUserData);
        const name = userdata.name || 'N/A'; // Assuming email is stored in userdata
        const email = userdata.email || 'N/A'; // Assuming email is stored in userdata
        let roleText = '';
        if (userdata.role === '3') {
            roleText = 'Order Viewer';
        } else if (userdata.role === '4') {
            roleText = 'Admin';
        } else {
            roleText = 'Unknown Role';
        }

        // Hide the login form
        if (loginContainer) {
            loginContainer.innerHTML = `
                <div>
                    <p>Logged in as: <strong>${name}</strong> ->&nbsp;<strong>${email}</strong></p>
                    <p>Role: <strong>${roleText}</strong></p>
                    <p>User ID: <strong>${userId}</strong></p>
                    <button id="logout-button" class="btn btn-secondary">Logout</button>
                </div>
            `;

            const logoutButton = document.getElementById('logout-button');
            if (logoutButton) {
                logoutButton.addEventListener('click', handleLogout);
            }
        }
    } else {
        // --- Event Listeners ---
        if (loginFormFrag) {
            loginFormFrag.addEventListener('submit', handleLogin);
        }
    }

    async function handleLogin(event) {
        event.preventDefault();

        const email = usernameInputFrag.value.trim();
        const password = passwordInputFrag.value.trim();

        showLoading();

        try {
            const userCredential = await auth.signInWithEmailAndPassword(email, password);
            const userdata = await callApi("GET", "user/" + userCredential.user.uid);

            if (userdata.isblocked == 1 || (userdata.role != "3" && userdata.role != "4")) {
                handleLogout();
                hideLoading();
                console.log("Blocked User");

                showDialog("err", "OK", "You are not allowed to use this portal. this action is informed to our admin as well", function () {

                });

            } else {
                localStorage.setItem("userId", userCredential.user.uid);
                localStorage.setItem("userdata", JSON.stringify(userdata));
                console.log("Login successful!", userCredential.user);
                hideLoading();

                showDialog("info", "Ok", "Close", "Login Successfull", function (btn) {
                    if (btn == "Ok") {
                        loadModuleByName("Login");
                    }
                });
            }
        } catch (error) {
            hideLoading();
            console.error("Login failed:", error);
            showDialog("err", "Try Again", "Invalid Username or Password!", function () { });
        }
    }
}


window.initMod0 = initMod0;