// modules/mod3/script.js
async function initMod3() {
    console.log("Initializing Product Management Module (Mod3)...");

    // --- DOM Elements ---
    const productForm = document.getElementById('product-form');
    const productTableBody = document.querySelector('#product-table tbody');
    const searchInput = document.getElementById('product-search');
    const categoryFilterSelect = document.getElementById('product-category-filter');
    const noProductsMessage = document.getElementById('no-products-message');

    // Form fields
    const productIdInput = document.getElementById('product-edit-id');
    const productNameInput = document.getElementById('product-name');
    const productCategorySelect = document.getElementById('product-category');
    const productDescInput = document.getElementById('product-desc');
    const productSubcategoriesContainer = document.getElementById('product-subcategories-container');
    const costRateInput = document.getElementById('product-cost-rate');
    const costMrpInput = document.getElementById('product-cost-mrp');
    const costGstInput = document.getElementById('product-cost-gst');
    const costDisInput = document.getElementById('product-cost-dis');
    const stockInput = document.getElementById('product-stock');
    const newImageUrlInput = document.getElementById('product-new-image-url');
    const addImageBtn = document.getElementById('product-add-image-btn');
    const imagePreviewList = document.getElementById('product-image-preview-list');
    const noImagesText = document.getElementById('no-images-text');

    const formTitle = document.getElementById('product-form-title');
    const saveButton = document.getElementById('product-save-btn');
    const cancelButton = document.getElementById('product-cancel-btn');

    // --- Storage Keys ---
    const PRODUCT_KEY = 'products';
    const CATEGORY_KEY = 'categories';
    const SUBCATEGORY_KEY = 'subcategories';

    // --- Data Functions ---
    const getProducts = () => StorageHelper.load(PRODUCT_KEY, []);
    const saveProducts = (products) => StorageHelper.save(PRODUCT_KEY, products);
    const getCategories = () => StorageHelper.load(CATEGORY_KEY, []);
    const getSubcategories = () => StorageHelper.load(SUBCATEGORY_KEY, []);

    // --- State ---
    let currentImageUrls = []; // Store image URLs for the product being edited/added

    // --- Rendering ---
    function renderProductTable(filteredProducts = null) {
        const products = filteredProducts !== null ? filteredProducts : getProducts();
        const categories = getCategories(); // Get categories for display name
        productTableBody.innerHTML = '';
        noProductsMessage.style.display = products.length === 0 ? 'block' : 'none';

        if (products.length === 0 && filteredProducts === null) {
             productTableBody.innerHTML = `<tr><td colspan="7" class="no-data-message">No products added yet.</td></tr>`; return;
        }
         if (products.length === 0 && filteredProducts !== null) {
             productTableBody.innerHTML = `<tr><td colspan="7" class="no-data-message">No products match your filters.</td></tr>`; return;
        }

        products.forEach(prod => {
            const category = categories.find(c => c.id === prod.cat_id);
            const categoryName = category ? category.name : 'N/A';
            const firstImage = prod.product_img && prod.product_img.length > 0 ? prod.product_img[0] : null;
             const imgHtml = firstImage ? `<img src="${escapeHtml(firstImage)}" alt="${escapeHtml(prod.product_name)}" class="table-img-preview" onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';"> <span style="display:none;"><i data-feather='package'></i></span>` : `<span><i data-feather='package'></i></span>`;

            const row = productTableBody.insertRow();
            row.dataset.productId = prod.product_id; // Use product_id from data

            row.innerHTML = `
                <td>${imgHtml}</td>
                <td>${escapeHtml(prod.product_name)}</td>
                <td>${escapeHtml(categoryName)}</td>
                <td class="text-right">${escapeHtml(prod.stock)}</td>
                <td class="text-right">${escapeHtml(prod.cost_rate.toFixed(2))}</td>
                <td class="text-right">${escapeHtml(prod.cost_mrp.toFixed(2))}</td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-outline btn-edit-product"><i data-feather="edit-2"></i> Edit</button>
                    <button class="btn btn-sm btn-danger btn-delete-product"><i data-feather="trash-2"></i> Delete</button>
                </td>
            `;
            row.querySelector('.btn-edit-product').onclick = () => handleEditProduct(prod.product_id);
            row.querySelector('.btn-delete-product').onclick = () => handleDeleteProduct(prod.product_id, prod.product_name);
        });
        feather.replace();
    }

    function populateCategoryDropdowns() {
        const categories = getCategories();
        // Form dropdown
        productCategorySelect.innerHTML = '<option value="">-- Select Category --</option>';
        // Filter dropdown
        categoryFilterSelect.innerHTML = '<option value="">-- Filter by Category --</option>';

        categories.forEach(cat => {
            const option = document.createElement('option');
            option.value = cat.id;
            option.textContent = cat.name;
            productCategorySelect.appendChild(option.cloneNode(true)); // Clone for filter
            categoryFilterSelect.appendChild(option);
        });
    }

     function populateSubcategoryCheckboxes(selectedCategoryId, checkedSubIds = []) {
        productSubcategoriesContainer.innerHTML = ''; // Clear previous
        if (!selectedCategoryId) {
            productSubcategoriesContainer.innerHTML = '<p class="text-muted">Select a category first.</p>';
            return;
        }

        const allSubcategories = getSubcategories();
        const relevantSubcategories = allSubcategories.filter(sub => sub.parentId === selectedCategoryId);

        if (relevantSubcategories.length === 0) {
             productSubcategoriesContainer.innerHTML = '<p class="text-muted">No subcategories available for this category.</p>';
             return;
        }

        relevantSubcategories.forEach(sub => {
            const isChecked = checkedSubIds.includes(sub.id);
            const label = document.createElement('label');
            label.className = 'checkbox-label';
            label.innerHTML = `
                <input type="checkbox" name="product-subcategories" value="${sub.id}" ${isChecked ? 'checked' : ''}>
                ${escapeHtml(sub.name)}
            `;
            productSubcategoriesContainer.appendChild(label);
        });
    }

     function renderImagePreviews() {
        imagePreviewList.innerHTML = ''; // Clear existing
        if (currentImageUrls.length === 0) {
            noImagesText.style.display = 'block';
            return;
        }
         noImagesText.style.display = 'none';

        currentImageUrls.forEach((url, index) => {
            const item = document.createElement('div');
            item.className = 'img-preview-item';
            item.innerHTML = `
                <img src="${escapeHtml(url)}" class="img-preview" alt="Preview ${index+1}" onerror="this.style.display='none'; this.nextElementSibling.style.display='block';" >
                <span style="display: none; font-size: 0.7em; color: var(--text-muted);">Invalid URL or image</span>
                <button type="button" class="remove-img-btn" data-index="${index}">&times;</button>
            `;
            item.querySelector('.remove-img-btn').onclick = () => removeImage(index);
            imagePreviewList.appendChild(item);
        });
    }

    // --- Form Handling ---
    function resetForm() {
        productForm.reset();
        productIdInput.value = ''; // Clear hidden ID
        formTitle.textContent = 'Add New Product';
        saveButton.innerHTML = '<i data-feather="save"></i> Save Product';
        cancelButton.style.display = 'none';
        currentImageUrls = []; // Clear image array
        renderImagePreviews();
        productSubcategoriesContainer.innerHTML = '<p class="text-muted">Select a category first.</p>'; // Reset subcats
        feather.replace();
    }

    function populateForm(productId) {
        const product = getProducts().find(p => p.product_id === productId);
        if (!product) return;

        productIdInput.value = product.product_id;
        productNameInput.value = product.product_name;
        productCategorySelect.value = product.cat_id;
        productDescInput.value = product.product_desc || '';
        costRateInput.value = product.cost_rate;
        costMrpInput.value = product.cost_mrp;
        costGstInput.value = product.cost_gst || 0;
        costDisInput.value = product.cost_dis || 0;
        stockInput.value = product.stock;

        // Populate subcategories
        const checkedSubIds = product.cat_sub ? product.cat_sub.filter(id => id) : []; // Handle empty string/null
        populateSubcategoryCheckboxes(product.cat_id, checkedSubIds);

        // Populate images
        currentImageUrls = Array.isArray(product.product_img) ? [...product.product_img] : [];
        renderImagePreviews();

        formTitle.textContent = 'Edit Product';
        saveButton.innerHTML = '<i data-feather="check-circle"></i> Update Product';
        cancelButton.style.display = 'inline-block';
        feather.replace();
        productForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function addImage() {
        const url = newImageUrlInput.value.trim();
        if (!url) {
             showToast('Please enter an image URL.', 'warning'); return;
        }
        if (!isValidHttpUrl(url)) {
             showToast('Invalid URL format. Please enter a valid HTTP/HTTPS URL.', 'error'); return;
        }
        if (currentImageUrls.includes(url)) {
             showToast('This image URL has already been added.', 'warning'); return;
        }

        currentImageUrls.push(url);
        renderImagePreviews();
        newImageUrlInput.value = ''; // Clear input field
         showToast('Image URL added.', 'success');
    }

    function removeImage(index) {
        currentImageUrls.splice(index, 1);
        renderImagePreviews();
         showToast('Image URL removed.', 'info');
    }


    // --- Event Handlers ---
    function handleFormSubmit(event) {
        event.preventDefault();
        const editId = productIdInput.value; // Use product_id for editing check

        // Get selected subcategory IDs
        const selectedSubcategoryCheckboxes = productSubcategoriesContainer.querySelectorAll('input[name="product-subcategories"]:checked');
        const subcategoryIds = Array.from(selectedSubcategoryCheckboxes).map(cb => cb.value);

        const product = {
            product_id: editId || `ProductID${Date.now()}`, // Generate if new, keep if editing
            product_name: productNameInput.value.trim(),
            product_desc: productDescInput.value.trim(),
            product_img: [...currentImageUrls], // Copy current URLs
            cat_id: productCategorySelect.value,
            cat_sub: subcategoryIds.join(','), // Comma-separated string
            cost_rate: parseFloat(costRateInput.value) || 0,
            cost_mrp: parseFloat(costMrpInput.value) || 0,
            cost_gst: parseFloat(costGstInput.value) || 0,
            cost_dis: parseFloat(costDisInput.value) || 0,
            stock: parseInt(stockInput.value, 10) || 0,
            id: editId ? getProducts().find(p=>p.product_id === editId)?.id : generateId(), // Keep existing UUID-like id or generate new
             // Add created_at/updated_at timestamps? Maybe not needed for pure local storage version.
        };

        // Validation
        if (!product.product_name || !product.cat_id || product.cost_rate < 0 || product.cost_mrp < 0) {
            showToast('Please fill in Product Name, Category, Rate, and MRP.', 'error'); return;
        }
         if (product.cost_rate > product.cost_mrp) {
            showToast('Rate cannot be greater than MRP.', 'error'); return;
         }
         if (product.cost_gst < 0 || product.cost_dis < 0 || product.stock < 0) {
             showToast('GST, Discount, and Stock cannot be negative.', 'error'); return;
         }


        let products = getProducts();
        const isUpdating = Boolean(editId);

        if (isUpdating) {
            products = products.map(p => (p.product_id === editId ? product : p));
            showToast(`Product "${product.product_name}" updated.`, 'success');
        } else {
            // Optional: Check for duplicate product name before adding?
            const nameLower = product.product_name.toLowerCase();
            if (products.some(p => p.product_name.toLowerCase() === nameLower)) {
               showToast(`Product name "${product.product_name}" already exists.`, 'error'); return;
            }
            products.push(product);
             showToast(`Product "${product.product_name}" added.`, 'success');
        }


        callApi("POST","products/",product)
        saveProducts(products);
        handleFilterAndSearch(); // Re-render table based on current filters
        resetForm();
    }

    function handleEditProduct(productId) { populateForm(productId); }

    function handleDeleteProduct(productId, productName) {
         // Check if product is in any orders before deleting
         const orders = StorageHelper.load('orders', []);
         const isInOrder = orders.some(order => order.items && order.items[productId]);
         if (isInOrder) {
            showToast(`Cannot delete product "${productName}" as it exists in orders.`, 'error'); return;
         }

        if (!confirm(`Delete product "${productName}"?`)) return;
        let products = getProducts();
        products = products.filter(p => p.product_id !== productId);

        callApi("DELETE","products/"+productId)

        saveProducts(products);
        handleFilterAndSearch(); // Re-render table
        resetForm(); // In case it was being edited
        showToast(`Product "${productName}" deleted.`, 'info');
    }

     function handleCategoryChange() {
        const selectedCategoryId = productCategorySelect.value;
         // Get currently checked subcategories *before* repopulating
         const currentCheckedIds = Array.from(productSubcategoriesContainer.querySelectorAll('input[name="product-subcategories"]:checked')).map(cb => cb.value);
        populateSubcategoryCheckboxes(selectedCategoryId, currentCheckedIds); // Pass currently checked to preserve if applicable
    }

    function handleFilterAndSearch() {
        const searchTerm = searchInput.value.toLowerCase().trim();
        const categoryFilter = categoryFilterSelect.value;
        let products = getProducts();

        // Filter by Category
        if (categoryFilter) {
            products = products.filter(p => p.cat_id === categoryFilter);
        }

        // Filter by Search Term
        if (searchTerm) {
            products = products.filter(p =>
                p.product_name.toLowerCase().includes(searchTerm) ||
                (p.product_desc && p.product_desc.toLowerCase().includes(searchTerm))
            );
        }

        renderProductTable(products);
    }

    // --- Attach Event Listeners ---
    productForm.addEventListener('submit', handleFormSubmit);
    cancelButton.addEventListener('click', resetForm);
    addImageBtn.addEventListener('click', addImage);
    productCategorySelect.addEventListener('change', handleCategoryChange); // For populating subcats
    searchInput.addEventListener('input', handleFilterAndSearch);
    categoryFilterSelect.addEventListener('change', handleFilterAndSearch);


    // --- Initial Load ---
    populateCategoryDropdowns();
    
    
    const productData = await callApi('POST', 'products/query', {
        filters: [],
        limit: 10000,
        offset: 0,
        order_by: "product_name",
        order_direction: "ASC"
    });

    const productList = Array.isArray(productData) ? productData.map(p => ({
        id: p.id,
        product_id: p.product_id,
        product_name: p.product_name,
        product_desc: p.product_desc,
        product_img: p.product_img[0] || null,
        cat_id: p.cat_id,
        cat_sub: p.cat_sub.split(',').filter(Boolean),
        cost_rate: p.cost_rate,
        cost_mrp: p.cost_mrp,
        cost_gst: p.cost_gst,
        cost_dis: p.cost_dis,
        stock: p.stock
    })) : [];

    saveProducts(productList);
    renderProductTable(); // Load and display products
    // Ensure subcategory checkboxes are initially empty/prompted
    populateSubcategoryCheckboxes(null);
}
window.initMod3 = initMod3;