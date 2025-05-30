// modules/mod1/script.js
async function initMod1() {
    console.log("Initializing User Management Module (Mod1)...");

    // DOM Elements
    const userForm = document.getElementById('user-form');
    const userTableBody = document.querySelector('#user-table tbody');
    const searchInput = document.getElementById('user-search');
    const userIdInput = document.getElementById('user-id');
    const userUIdInput = document.getElementById('user-uid');
    const usernameInput = document.getElementById('user-username');
    const userContactInput = document.getElementById('user-contact');
    const userGstinInput = document.getElementById('user-gstin');
    
    const emailInput = document.getElementById('user-email');
    const roleInput = document.getElementById('user-role');
    const addressInput = document.getElementById('user-address');
    const passwordInput = document.getElementById('user-password');
    const creditsInput = document.getElementById('user-credits');
    const isBlockedCheckbox = document.getElementById('user-isblocked');
    const formTitle = document.getElementById('user-form-title');
    const saveButton = document.getElementById('user-save-btn');
    const cancelButton = document.getElementById('user-cancel-btn');
    const noUsersMessage = document.getElementById('no-users-message'); // Reference paragraph element
    const expiryInput = document.getElementById('user-credits-expiry');

    usernameInput.addEventListener('input', () => {
        
        const isUpdating = Boolean(userIdInput.value);

        if (!isUpdating) {
            // Remove non-alphanumeric characters and trim spaces
            const cleanedUsername = usernameInput.value.replace(/[^a-zA-Z0-9]/g, '').trim();
            emailInput.value = cleanedUsername + '@petsfort.in';
        }
    });
    
    // const today = new Date().toISOString().split('T')[0];
    // expiryInput.value = today;
    // expiryInput.min = today;

    const expiryPicker = flatpickr("#user-credits-expiry", {
        dateFormat: "d-m-Y", // dd-mm-yyyy
        minDate: new Date().fp_incr(1), // Tomorrow
        defaultDate: new Date().fp_incr(1), // Default value: tomorrow
    });


    function setExpiryDate(days) {
        const futureDate = new Date();
        futureDate.setDate(futureDate.getDate() + days);
    
        expiryPicker.setDate(futureDate, true); // true to trigger the change event
    }

    window.setExpiryDate = setExpiryDate
    

    // console.log(expiryInput.min)

    const STORAGE_KEY = 'users';

    // --- Data Functions ---
    const getUsers = () => StorageHelper.load(STORAGE_KEY, []);
    const saveUsers = (users) => StorageHelper.save(STORAGE_KEY, users);

    // --- Rendering ---
    function renderUserTable(filteredUsers = null) {
        const users = filteredUsers !== null ? filteredUsers : getUsers();
        userTableBody.innerHTML = ''; // Clear existing rows
        noUsersMessage.style.display = users.length === 0 ? 'block' : 'none'; // Show/hide message

        if (users.length === 0 && filteredUsers === null) { // Only show placeholder if no users *at all*
             userTableBody.innerHTML = `<tr><td colspan="5" class="no-data-message">No users created yet.</td></tr>`;
             return;
        }
         if (users.length === 0 && filteredUsers !== null) { // Show different message if filtered results empty
             userTableBody.innerHTML = `<tr><td colspan="5" class="no-data-message">No users match your search.</td></tr>`;
             return;
        }

        users.forEach(user => {
            const row = userTableBody.insertRow();
            row.dataset.userId = user.id; // Use dataset for clarity

            row.innerHTML = `
                <td>${escapeHtml(user.username)}</td>
                <td>${escapeHtml(user.email)}</td>
                <td class="text-right">${escapeHtml(user.credits)}</td>
                <td><span class="status-${user.isblocked ? 'blocked' : 'active'}">${user.isblocked ? 'Blocked' : 'Active'}</span></td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-outline btn-edit"><i data-feather="edit-2"></i> Edit</button>
                    <button class="btn btn-sm btn-danger btn-delete"><i data-feather="trash-2"></i> Delete</button>
                </td>
            `;
            row.querySelector('.btn-edit').onclick = () => handleEditUser(user.id);
            row.querySelector('.btn-delete').onclick = () => handleDeleteUser(user.id, user.username);
        });
        feather.replace();
    }

    // --- Form Handling ---
    function resetForm() {
        userForm.reset();
        userIdInput.value = '';
        formTitle.textContent = 'Add New User';
        saveButton.innerHTML = '<i data-feather="save"></i> Save User';
        cancelButton.style.display = 'none';
        usernameInput.disabled = false;
        feather.replace();
    }

    function populateForm(userId) {
        const user = getUsers().find(u => u.id === userId);
        if (!user) return;

        console.log(user)
        userIdInput.value = user.id;
        userUIdInput.value = user.uid;
        usernameInput.value = user.username;
        userContactInput.value = user.contact;
        userGstinInput.value = user.gstin;
        emailInput.value = user.email;
        roleInput.value = user.role;
        addressInput.value = user.address;
        creditsInput.value = user.credits;
        // expiryInput.value = user.creditse;        
        expiryPicker.setDate(parseDDMMYYYY(user.creditse), true); 
        isBlockedCheckbox.checked = user.isblocked;

        formTitle.textContent = 'Edit User';
        saveButton.innerHTML = '<i data-feather="check-circle"></i> Update User';
        cancelButton.style.display = 'inline-block';
        feather.replace();
        userForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // --- Event Handlers ---
    function handleFormSubmit(event) {
        event.preventDefault();
        let passwordValue = passwordInput.value;
        const userId = userIdInput.value;
        const userUid = userUIdInput.value;
        const isUpdating = Boolean(userId);

        
        const user = {
            id: userId || generateId(),
            uid: userUid,
            username: usernameInput.value.trim(),
            contact: userContactInput.value.trim(),
            gstin: userGstinInput.value.trim(),
            email: emailInput.value.trim(),
            role: roleInput.value,
            address: addressInput.value.trim(),
            credits: parseInt(creditsInput.value, 10) || 0,
            creditse: expiryInput.value,
            isblocked: isBlockedCheckbox.checked,
        };

        if (!user.username || !user.email) {
            showToast('Username and Email are required.', 'error'); return;
        }

        // Basic email format check
        if (!/^\S+@\S+\.\S+$/.test(user.email)) {
            showToast('Please enter a valid email address.', 'error'); return;
        }

        if (!user.role) {
            showToast('User Role are required.', 'error'); return;
        }

        if (user.role == '1' && !user.address) {
            showToast('User Address are required.', 'error'); return;
        }


        if (isUpdating) {
            if (passwordValue.length > 0) {
                // Check for minimum 6 characters
                if (passwordValue.length < 6) {
                    showToast('Password must be at least 6 characters long.','error'); return false;
                }

                // // Basic rules regex: at least one uppercase letter, one lowercase letter, and one digit
                // const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
                // if (!passwordRegex.test(passwordValue)) {
                //     showToast('Password must contain at least one uppercase letter, one lowercase letter, and one digit.','error'); return;
                // }
            } else {
                //pass
                passwordValue = "";
            }
        } else {
            // Check for minimum 6 characters
            if (passwordValue.length < 6) {
                showToast('Password must be at least 6 characters long.','error'); return false;
            }

            // // Basic rules regex: at least one uppercase letter, one lowercase letter, and one digit
            // const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
            // if (!passwordRegex.test(passwordValue)) {
            //     showToast('Password must contain at least one uppercase letter, one lowercase letter, and one digit.','error'); return;
            // }

        }

        let users = getUsers();
        if (isUpdating) {
            originalUser = users.find(u => u.id === userId || u.uid === userUid);
            if (originalUser) {
                // if (usernameInput.value.trim() !== originalUser.username.trim()) {
                //     showToast('Username cannot be modified during update.', 'error');return;
                // }
                if (emailInput.value.trim().toLowerCase() !== originalUser.email.trim().toLowerCase()) {
                    showToast('Email cannot be modified during update.', 'error');
                    return;
                }
            } else {
                showToast('Original user data not found for update.', 'error');
                return;
            }
        }

        
        const usernameLower = user.username.toLowerCase();
        const emailLower = user.email.toLowerCase();

        // Check for duplicates (excluding self during update)
        if (users.some(u => u.id !== userId && u.username.toLowerCase() === usernameLower)) {
            showToast(`Username "${user.username}" already exists.`, 'error'); return;
        }
        if (users.some(u => u.id !== userId && u.email.toLowerCase() === emailLower)) {
            showToast(`Email "${user.email}" already exists.`, 'error'); return;
        }

        if (isUpdating) {
            showToast(`Updating User Account... "${user.email}".`, 'success');

            console.log("userdata/"+user.uid)
            callApi("PUT","userdata/"+user.uid,{
                name: user.username,
                contact: user.contact,
                gstin: user.gstin,
                email: user.email,
                role: user.role,
                address: user.address,
                credits: user.credits,
                creditse: user.creditse,
                isblocked: user.isblocked ? 1 : 0,
                pwd: passwordValue
            }).then(v => {
                if (v !==null && v !== undefined && v.uid !== null && v.uid !== undefined && v.uid !== "") {
                    users = users.map(u => (u.id === userId ? user : u));
                    saveUsers(users);
                    handleSearch(); // Re-render based on current search term
                    resetForm();
                } else {
                    showToast(`Failed to Update User Account "${user.email}".`, 'error');
                }
            })
        } else {

            showToast(`Creating User Account... "${user.email}".`, 'success');

            callApi("POST","userdata",{
                id: user.id,
                name: user.username,
                contact: user.contact,
                gstin: user.gstin,
                email: user.email,
                role: user.role,
                address: user.address,
                credits: user.credits,
                creditse: user.creditse,
                isblocked: user.isblocked ? 1 : 0,
                pwd: passwordValue
            }).then(v => {
                if (v !==null && v !== undefined && v.uid !== null && v.uid !== undefined && v.uid !== "") {
                    user.id = v.uid
                    users.push(user);
                    saveUsers(users);
                    handleSearch(); // Re-render based on current search term
                    resetForm();
                } else {
                    showToast(`Failed to Create User Account "${user.email}".`, 'error');
                }
            })
        }
    }

    function handleEditUser(userId) { populateForm(userId); }

    function handleDeleteUser(userId, username) {
        if (!confirm(`Delete user "${username}"? This cannot be undone.`)) return;
        let users = getUsers();
        // TODO: Add check here if user has associated orders in Module 4 before deleting?
        // For now, just delete the user.

        const user = users.find(u => u.id === userId);
        users = users.filter(u => u.id !== userId);

        callApi("DELETE","userdata/"+user.uid)

        saveUsers(users);
        handleSearch(); // Re-render based on current search term
        resetForm();
        showToast(`User "${username}" deleted.`, 'info');
    }

    function handleSearch() {
        const searchTerm = searchInput.value.toLowerCase().trim();
        const users = getUsers();
        if (!searchTerm) {
            renderUserTable(users);
        } else {
            const filteredUsers = users.filter(user =>
                user.username.toLowerCase().includes(searchTerm) ||
                user.email.toLowerCase().includes(searchTerm)
            );
            renderUserTable(filteredUsers);
        }
    }

    // --- Attach Event Listeners ---
    userForm.addEventListener('submit', handleFormSubmit);
    cancelButton.addEventListener('click', resetForm);
    searchInput.addEventListener('input', handleSearch);

    // --- Initial Load ---


    const userdata = await callApi('GET', 'userdata');
    const userdata_list = Array.isArray(userdata) ? userdata.map(u => ({
        uid: u.uid,
        id: u.id,
        username: u.name,
        contact: u.contact,
        gstin: u.gstin,
        email: u.email,
        role: u.role,
        address: u.address,
        credits: u.credits,
        creditse: u.creditse,
        isblocked: u.isblocked === 1
    })) : [];

    saveUsers(userdata_list);
    renderUserTable();
}
// Ensure it's callable from index.html
window.initMod1 = initMod1;