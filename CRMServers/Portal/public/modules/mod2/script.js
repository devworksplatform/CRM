// modules/mod2/script.js
async function initMod2() {
    console.log("Initializing Category Management Module (Mod2)...");

    // --- DOM Elements ---
    // Categories
    const categoryForm = document.getElementById('category-form');
    const categoryTableBody = document.querySelector('#category-table tbody');
    const categoryIdInput = document.getElementById('category-id');
    const categoryNameInput = document.getElementById('category-name');
    const categoryImageInput = document.getElementById('category-image');
    const categoryFormTitle = document.getElementById('category-form-title');
    const categoryCancelBtn = document.getElementById('category-cancel-btn');
    const noCategoriesMessage = document.getElementById('no-categories-message');
    const parentCategorySelect = document.getElementById('parent-category-select');

    // Subcategories
    const subcategoryFormCard = document.getElementById('subcategory-form-card');
    const subcategoryForm = document.getElementById('subcategory-form');
    const subcategoryTableBody = document.querySelector('#subcategory-table tbody');
    const subcategoryIdInput = document.getElementById('subcategory-id');
    const subcategoryParentIdInput = document.getElementById('subcategory-parent-id');
    const subcategoryNameInput = document.getElementById('subcategory-name');
    const subcategoryImageInput = document.getElementById('subcategory-image');
    const subcategoryFormTitle = document.getElementById('subcategory-form-title');
    const subcategoryCancelBtn = document.getElementById('subcategory-cancel-btn');
    const noSubcategoriesMessage = document.getElementById('no-subcategories-message');

    // --- Storage Keys ---
    const CATEGORY_KEY = 'categories';
    const SUBCATEGORY_KEY = 'subcategories';

    // --- Data Functions ---
    const getCategories = () => StorageHelper.load(CATEGORY_KEY, []);
    const getSubcategories = () => StorageHelper.load(SUBCATEGORY_KEY, []);
    const saveCategories = (categories) => StorageHelper.save(CATEGORY_KEY, categories);
    const saveSubcategories = async (subcategories) => StorageHelper.save(SUBCATEGORY_KEY, subcategories);

    // --- State ---
    let selectedParentCategoryId = null; // Track which category is selected for subcategory view

    // --- Rendering ---
    function renderCategoryTable() {
        const categories = getCategories();
        categoryTableBody.innerHTML = '';
        noCategoriesMessage.style.display = categories.length === 0 ? 'block' : 'none';

        if (categories.length === 0) {
             categoryTableBody.innerHTML = `<tr><td colspan="3" class="no-data-message">No categories created yet.</td></tr>`;
        }

        categories.forEach(cat => {
            const row = categoryTableBody.insertRow();
            row.dataset.categoryId = cat.id;
            const imgHtml = cat.image ? `<img src="${escapeHtml(cat.image)}" alt="${escapeHtml(cat.name)}" class="table-img-preview" onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';" > <span style="display:none;"><i data-feather='image'></i></span>` : `<span><i data-feather='image'></i></span>`;

            row.innerHTML = `
                <td>${imgHtml}</td>
                <td>${escapeHtml(cat.name)}</td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-outline btn-edit-category"><i data-feather="edit-2"></i> Edit</button>
                    <button class="btn btn-sm btn-danger btn-delete-category"><i data-feather="trash-2"></i> Delete</button>
                </td>
            `;
            row.querySelector('.btn-edit-category').onclick = () => handleEditCategory(cat.id);
            row.querySelector('.btn-delete-category').onclick = () => handleDeleteCategory(cat.id, cat.name);
        });
        feather.replace();
    }

    function renderSubcategoryTable(parentId) {
        const allSubcategories = getSubcategories();
        const filteredSubs = parentId ? allSubcategories.filter(sub => sub.parentId === parentId) : [];
        subcategoryTableBody.innerHTML = '';
        noSubcategoriesMessage.style.display = (parentId && filteredSubs.length === 0) ? 'block' : 'none';

        if (!parentId) {
             subcategoryTableBody.innerHTML = `<tr><td colspan="3" class="no-data-message">Select a parent category above.</td></tr>`;
             return;
        }
         if (filteredSubs.length === 0) {
             subcategoryTableBody.innerHTML = `<tr><td colspan="3" class="no-data-message">No subcategories for this category.</td></tr>`;
        }


        filteredSubs.forEach(sub => {
            const row = subcategoryTableBody.insertRow();
            row.dataset.subcategoryId = sub.id;
             const imgHtml = sub.image ? `<img src="${escapeHtml(sub.image)}" alt="${escapeHtml(sub.name)}" class="table-img-preview" onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';" > <span style="display:none;"><i data-feather='image'></i></span>` : `<span><i data-feather='image'></i></span>`;

            row.innerHTML = `
                <td>${imgHtml}</td>
                <td>${escapeHtml(sub.name)}</td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-outline btn-edit-subcategory"><i data-feather="edit-2"></i> Edit</button>
                    <button class="btn btn-sm btn-danger btn-delete-subcategory"><i data-feather="trash-2"></i> Delete</button>
                </td>
            `;
             row.querySelector('.btn-edit-subcategory').onclick = () => handleEditSubcategory(sub.id);
             row.querySelector('.btn-delete-subcategory').onclick = () => handleDeleteSubcategory(sub.id, sub.name);
        });
        feather.replace();
    }

    function populateParentCategorySelect() {
        const categories = getCategories();
        parentCategorySelect.innerHTML = '<option value="">-- Select Category --</option>'; // Reset
        categories.forEach(cat => {
            const option = document.createElement('option');
            option.value = cat.id;
            option.textContent = cat.name;
            parentCategorySelect.appendChild(option);
        });
         // Restore selection if possible
        if (selectedParentCategoryId) {
             parentCategorySelect.value = selectedParentCategoryId;
        }
    }

    // --- Form Handling ---
    function resetCategoryForm() {
        categoryForm.reset();
        categoryIdInput.value = '';
        categoryFormTitle.textContent = 'Add New Category';
        categoryCancelBtn.style.display = 'none';
    }

    function populateCategoryForm(catId) {
        const category = getCategories().find(c => c.id === catId);
        if (!category) return;
        categoryIdInput.value = category.id;
        categoryNameInput.value = category.name;
        categoryImageInput.value = category.image || '';
        categoryFormTitle.textContent = 'Edit Category';
        categoryCancelBtn.style.display = 'inline-block';
        categoryForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

     function resetSubcategoryForm() {
        subcategoryForm.reset();
        subcategoryIdInput.value = '';
        // Keep parent ID selected: subcategoryParentIdInput.value = '';
        subcategoryFormTitle.textContent = 'Add Subcategory';
        subcategoryCancelBtn.style.display = 'none';
    }

     function populateSubcategoryForm(subId) {
        const subcategory = getSubcategories().find(s => s.id === subId);
        if (!subcategory) return;
        subcategoryIdInput.value = subcategory.id;
        subcategoryParentIdInput.value = subcategory.parentId; // Ensure parent ID is set
        subcategoryNameInput.value = subcategory.name;
        subcategoryImageInput.value = subcategory.image || '';
        subcategoryFormTitle.textContent = 'Edit Subcategory';
        subcategoryCancelBtn.style.display = 'inline-block';
        subcategoryFormCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }


    // --- Event Handlers ---
    function handleCategoryFormSubmit(event) {
        event.preventDefault();
        const catId = categoryIdInput.value;
        const category = {
            id: catId || generateId(),
            name: categoryNameInput.value.trim(),
            image: categoryImageInput.value.trim() || null // Store null if empty
        };

        if (!category.name) {
            showToast('Category Name is required.', 'error'); return;
        }
         if (category.image && !isValidHttpUrl(category.image)) {
             showToast('Please enter a valid URL for the image.', 'error'); return;
         }

        let categories = getCategories();
        const isUpdating = Boolean(catId);
        const nameLower = category.name.toLowerCase();

         // Check duplicate name (excluding self)
        if (categories.some(c => c.id !== catId && c.name.toLowerCase() === nameLower)) {
             showToast(`Category name "${category.name}" already exists.`, 'error'); return;
        }

        if (isUpdating) {
            categories = categories.map(c => (c.id === catId ? category : c));
            showToast(`Category "${category.name}" updated.`, 'success');
        } else {
            categories.push(category);
            showToast(`Category "${category.name}" added.`, 'success');
        }

        callApi('POST', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/categories', category);

        saveCategories(categories);
        renderCategoryTable();
        populateParentCategorySelect(); // Update dropdown
        resetCategoryForm();
    }

    function handleEditCategory(catId) { populateCategoryForm(catId); }

    function handleDeleteCategory(catId, catName) {
         // Check if category has subcategories or products before deleting
         const subcategories = getSubcategories();
         const products = StorageHelper.load('products', []); // Need products data
         if (subcategories.some(sub => sub.parentId === catId)) {
             showToast(`Cannot delete category "${catName}" as it has subcategories.`, 'error'); return;
         }
         if (products.some(p => p.cat_id === catId)) {
              showToast(`Cannot delete category "${catName}" as it has associated products.`, 'error'); return;
         }

        if (!confirm(`Delete category "${catName}"?`)) return;
        let categories = getCategories();
        categories = categories.filter(c => c.id !== catId);

        callApi('DELETE', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/categories/'+catId);

        saveCategories(categories);
        renderCategoryTable();
        populateParentCategorySelect(); // Update dropdown
         // If the deleted category was selected, clear the subcat view
        if (selectedParentCategoryId === catId) {
             selectedParentCategoryId = null;
             parentCategorySelect.value = "";
             renderSubcategoryTable(null);
             subcategoryFormCard.style.display = 'none';
        }
        resetCategoryForm(); // In case it was being edited
        showToast(`Category "${catName}" deleted.`, 'info');
    }

    function handleSubcategoryFormSubmit(event) {
        event.preventDefault();
        const subId = subcategoryIdInput.value;
        const parentId = subcategoryParentIdInput.value; // Get from hidden input

         if (!parentId) { // Should be set when form is shown
             showToast('Cannot save subcategory without a parent category selected.', 'error'); return;
         }

        const subcategory = {
            id: subId || generateId(),
            parentId: parentId,
            name: subcategoryNameInput.value.trim(),
            image: subcategoryImageInput.value.trim() || null
        };

        if (!subcategory.name) {
            showToast('Subcategory Name is required.', 'error'); return;
        }
         if (subcategory.image && !isValidHttpUrl(subcategory.image)) {
             showToast('Please enter a valid URL for the image.', 'error'); return;
         }


        let subcategories = getSubcategories();
        const isUpdating = Boolean(subId);
        const nameLower = subcategory.name.toLowerCase();

        // Check duplicate name *within the same parent category* (excluding self)
        if (subcategories.some(s => s.id !== subId && s.parentId === parentId && s.name.toLowerCase() === nameLower)) {
            showToast(`Subcategory name "${subcategory.name}" already exists in this category.`, 'error'); return;
        }


        if (isUpdating) {
            subcategories = subcategories.map(s => (s.id === subId ? subcategory : s));
            showToast(`Subcategory "${subcategory.name}" updated.`, 'success');
        } else {
            subcategories.push(subcategory);
             showToast(`Subcategory "${subcategory.name}" added.`, 'success');
        }



        callApi('POST', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/subcategories', {
            id: subcategory.id,
            parentid: subcategory.parentId,
            name: subcategory.name,
            image:subcategory.image
        });

        saveSubcategories(subcategories);
        renderSubcategoryTable(parentId); // Re-render for the current parent
        resetSubcategoryForm();
    }

     function handleEditSubcategory(subId) { populateSubcategoryForm(subId); }

     function handleDeleteSubcategory(subId, subName) {
        // Check if subcategory is used by products
         const products = StorageHelper.load('products', []);
         if (products.some(p => p.cat_sub && p.cat_sub.split(',').includes(subId))) {
             showToast(`Cannot delete subcategory "${subName}" as it's used by products.`, 'error'); return;
         }

        if (!confirm(`Delete subcategory "${subName}"?`)) return;
        let subcategories = getSubcategories();
        const subToDelete = subcategories.find(s => s.id === subId);
        subcategories = subcategories.filter(s => s.id !== subId);

        callApi('DELETE', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/categories'+subId);

        saveSubcategories(subcategories);
        renderSubcategoryTable(subToDelete ? subToDelete.parentId : selectedParentCategoryId); // Re-render
        resetSubcategoryForm(); // In case it was being edited
        showToast(`Subcategory "${subName}" deleted.`, 'info');
    }


    function handleParentCategoryChange(event) {
        selectedParentCategoryId = event.target.value;
         subcategoryParentIdInput.value = selectedParentCategoryId; // Update hidden field for form

        if (selectedParentCategoryId) {
            renderSubcategoryTable(selectedParentCategoryId);
            resetSubcategoryForm(); // Clear form when changing parent
            subcategoryFormCard.style.display = 'block'; // Show form
        } else {
            renderSubcategoryTable(null); // Clear table
            subcategoryFormCard.style.display = 'none'; // Hide form
        }
    }

    // --- Attach Event Listeners ---
    categoryForm.addEventListener('submit', handleCategoryFormSubmit);
    categoryCancelBtn.addEventListener('click', resetCategoryForm);
    subcategoryForm.addEventListener('submit', handleSubcategoryFormSubmit);
    subcategoryCancelBtn.addEventListener('click', resetSubcategoryForm);
    parentCategorySelect.addEventListener('change', handleParentCategoryChange);

    // --- Initial Load ---

    const category = await callApi('GET', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/categories');
    const category_list = Array.isArray(category) ? category.map(c => ({
        id: c.id,
        name: c.name,
        image: c.image
    })) : [];

    const sub_category = await callApi('GET', 'http://ec2-13-235-78-112.ap-south-1.compute.amazonaws.com:8000/subcategories');
    const sub_category_list = Array.isArray(sub_category) ? sub_category.map(c => ({
        id: c.id,
        parentId: c.parentid,
        name: c.name,
        image: c.image
    })) : [];    

    saveCategories(category_list)
    saveSubcategories(sub_category_list)

    renderCategoryTable();
    populateParentCategorySelect();
    renderSubcategoryTable(null); // Initially show empty/prompt state

     // Add some default data if storage is empty (for testing)
     if (getCategories().length === 0 && getSubcategories().length === 0) {
        // console.log("Adding default category data for testing...");
        // const defaultCatId = generateId();
        // saveCategories([{ id: defaultCatId, name: "Electronics", image: null }]);
        // saveSubcategories([
        //     { id: generateId(), parentId: defaultCatId, name: "Smartphones", image: null },
        //     { id: generateId(), parentId: defaultCatId, name: "Laptops", image: null }
        // ]);
        // renderCategoryTable();
        // populateParentCategorySelect();
    }
}
window.initMod2 = initMod2;