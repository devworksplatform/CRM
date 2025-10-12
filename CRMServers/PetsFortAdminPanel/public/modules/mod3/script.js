// modules/mod3/script.js

// Assume necessary helper functions (StorageHelper, callApi, showToast, escapeHtml, generateId,
// isValidHttpUrl, uploadBlobToFirebase, showLoading, hideLoading) are defined elsewhere
// and are accessible in this scope.
// Assume libraries like feather-icons, cropperjs, compressorjs are included in your HTML.

function onInputChange(el, cb) {
    el.addEventListener('input', () => cb(el.value));
    const d = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
    Object.defineProperty(el, 'value', {
      get() { return d.get.call(this); },
      set(v) {
        d.set.call(this, v);
        cb(v);
        el.dispatchEvent(new Event('input', { bubbles: true }));
      }
    });
}
  

  
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
    const productHsnInput = document.getElementById('product-hsn');
    const productCidInput = document.getElementById('product-cid');
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

    onInputChange(costMrpInput, val => {
        costRateInput.value = val
    });
      


    // Import Elements
    const importBlock = document.getElementById('importBlock'); // Assuming this exists for context
    const fileInputCsv = document.getElementById('fileInputCsv');

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
             const imgHtml = firstImage ? `<img src="${escapeHtml(firstImage)}" loading="lazy" alt="${escapeHtml(prod.product_name)}" class="table-img-preview" onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';"> <span style="display:none;"><i data-feather='package'></i></span>` : `<span><i data-feather='package'></i></span>`;

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
                 <button type="button" class="remove-img-btn" data-index="${index}"></button>
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
        saveButton.innerHTML = '<i data-feather="save"></i> Save Product'; // Default text
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
        productHsnInput.value = product.product_hsn || '';
        productCidInput.value = product.product_cid || '';
        costRateInput.value = product.cost_rate;
        costMrpInput.value = product.cost_mrp;
        costGstInput.value = product.cost_gst || 0;
        costDisInput.value = product.cost_dis || 0;
        stockInput.value = product.stock;

        // Populate subcategories
        const checkedSubIds = product.cat_sub ? product.cat_sub.filter(id => id) : []; // Handle empty string/null
         // Manually trigger the change event on the category select to populate subcategories first
         productCategorySelect.dispatchEvent(new Event('change'));
         // Then, check the appropriate subcategories
         setTimeout(() => {
             populateSubcategoryCheckboxes(product.cat_id, checkedSubIds);
             // Re-check checkboxes after repopulating, in case category change cleared them
             checkedSubIds.forEach(subId => {
                 const checkbox = productSubcategoriesContainer.querySelector(`input[value="${subId}"]`);
                 if (checkbox) {
                     checkbox.checked = true;
                 }
             });
         }, 50); // Small delay


        // Populate images
        currentImageUrls = Array.isArray(product.product_img) ? [...product.product_img] : [];
        renderImagePreviews();

        formTitle.textContent = 'Edit Product';
        saveButton.innerHTML = '<i data-feather="check-circle"></i> Update Product';
        cancelButton.style.display = 'inline-block';
        feather.replace();
        productForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }


    function addImageUploaded(url) {
        newImageUrlInput.value = url;
        addImage();
    }

    window.addImageUploaded = addImageUploaded; // Make globally accessible for the image picker

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


    // --- Core Save/Update Logic (Extracted for use by form submit AND import) ---
    // This function reads data FROM the form and performs the save/update action.
    async function processProductSaveFromForm() {
        const editId = productIdInput.value;
        const isUpdating = Boolean(editId);

        // Get selected subcategory IDs from the current form state
        const selectedSubcategoryCheckboxes = productSubcategoriesContainer.querySelectorAll('input[name="product-subcategories"]:checked');
        const subcategoryIds = Array.from(selectedSubcategoryCheckboxes).map(cb => cb.value);

        const product = {
            // Use existing product_id if editing, generate a new one otherwise
            product_id: editId || `ProductID${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
            product_name: productNameInput.value.trim(),
            product_desc: productDescInput.value.trim(),
            product_hsn: productHsnInput.value.trim(),
            product_cid: productCidInput.value.trim(),
            
            product_img: [...currentImageUrls], // Use images currently in the form/preview
            cat_id: productCategorySelect.value,
            cat_sub: subcategoryIds.join(','), // Store as comma-separated string for API/storage format
            cost_rate: parseFloat(costRateInput.value) || 0,
            cost_mrp: parseFloat(costMrpInput.value) || 0,
            cost_gst: parseFloat(costGstInput.value) || 0,
            cost_dis: parseFloat(costDisInput.value) || 0,
            stock: parseInt(stockInput.value, 10) || 0,
            // Assign a unique local storage ID if it's a new product
            id: editId ? getProducts().find(p=>p.product_id === editId)?.id : generateId(),
        };

         // Validation
         if (!product.product_name || !product.cat_id || product.cost_rate < 0 || product.cost_mrp < 0) {
             showToast('Validation Failed: Name, Category, Rate, and MRP are required.', 'error');
             return false; // Indicate failure
         }
           if (product.cost_rate > product.cost_mrp) {
              showToast('Validation Failed: Rate cannot be greater than MRP.', 'error');
              return false;
           }
           if (product.cost_gst < 0 || product.cost_dis < 0 || product.stock < 0) {
                showToast('Validation Failed: GST, Discount, and Stock cannot be negative.', 'error');
                return false;
           }

        let products = getProducts();

        try {
            if (isUpdating) {
                await callApi("PUT", "products/" + editId, product);
                 // Update local storage list - convert cat_sub back to array
                 product.cat_sub = product.cat_sub.split(',').filter(Boolean);
                products = products.map(p => (p.product_id === editId ? product : p));
                saveProducts(products);
                // showToast(`Product "${product.product_name}" updated.`, 'success'); // Toast handled by caller or specific logic
                return true; // Indicate success
            } else {
                 // Optional: Check for duplicate product name before adding
                 const nameLower = product.product_name.toLowerCase();
                 if (products.some(p => p.product_name.toLowerCase() === nameLower)) {
                    // For manual form submit, this is an error.
                    // For CSV import, this might be handled differently by the loop.
                    // Let's let the loop decide how to handle duplicates reported by this function.
                    showToast(`Product name "${product.product_name}" already exists.`, 'warning'); // Warning, not necessarily failure to proceed in import loop
                    return false; // Indicate skipping due to duplicate
                 }

                await callApi("POST", "products/", product);
                // Add to local storage list - convert cat_sub back to array
                product.cat_sub = product.cat_sub.split(',').filter(Boolean);
                products.push(product);
                saveProducts(products);
                // showToast(`Product "${product.product_name}" added.`, 'success'); // Toast handled by caller or specific logic
                return true; // Indicate success
            }
        } catch (error) {
            console.error('API or Save Error:', error);
            showToast(`Failed to save product "${product.product_name}": ${error.message || 'API error'}`, 'error');
            return false; // Indicate failure
        }
    }


    // --- Event Handler for standard Form Submission ---
    // This is for manual Add/Edit, not the CSV import loop
    async function handleFormSubmit(event) {
        event.preventDefault();
        // Call the core saving logic
        const success = await processProductSaveFromForm();
        if (success) {
             const productName = productNameInput.value.trim(); // Get name again for toast
             const editId = productIdInput.value; // Check if it was an update
             const action = editId ? 'updated' : 'added';
             showToast(`Product "${productName}" ${action}.`, 'success');
             handleFilterAndSearch(); // Re-render table based on current filters
             resetForm(); // Clear form after successful manual save
        } else {
             // processProductSaveFromForm already showed specific validation/API error toast
        }
    }

    function handleEditProduct(productId) { populateForm(productId); }

    async function handleDeleteProduct(productId, productName) {
         // Check if product is in any orders before deleting
         const orders = StorageHelper.load('orders', []); // Assuming 'orders' key
         const isInOrder = orders.some(order => order.items && order.items[productId]);
         if (isInOrder) {
            showToast(`Cannot delete product "${productName}" as it exists in orders.`, 'error'); return;
         }

        if (!confirm(`Delete product "${productName}"?`)) return;
        let products = getProducts();

        try {
            await callApi("DELETE", "products/" + productId);

            products = products.filter(p => p.product_id !== productId);
            saveProducts(products);

            handleFilterAndSearch(); // Re-render table
            resetForm(); // In case it was being edited
            showToast(`Product "${productName}" deleted.`, 'info');

        } catch (error) {
            console.error('API or Delete Error:', error);
            showToast(`Failed to delete product "${productName}": ${error.message || 'API error'}`, 'error');
        }
    }

     function handleCategoryChange() {
         const selectedCategoryId = productCategorySelect.value;
          // Get currently checked subcategories *before* repopulating
          // This is important if the user changes category *while editing* a product
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
                (p.product_desc && p.product_desc.toLowerCase().includes(searchTerm)) || 
                (p.product_hsn && p.product_hsn.toLowerCase().includes(searchTerm))|| 
                (p.product_cid && p.product_cid.toLowerCase().includes(searchTerm))
            );
        }

        renderProductTable(products);
    }


    // --- Helper Functions for CSV Import Workflow ---

    // Function to find category ID by name (case-insensitive)
    function getCategoryIdByName(categoryName, categories) {
        if (!categoryName) return null;
        const trimmedName = categoryName.trim().toLowerCase();
        const foundCategory = categories.find(cat => cat.name.toLowerCase() === trimmedName);
        if (!foundCategory) {
            console.warn(`Category "${categoryName}" not found.`);
        }
        return foundCategory ? foundCategory.id : null;
    }

    // Function to find subcategory IDs by names within a category (case-insensitive)
    function getSubcategoryIdsByNames(categoryId, subcategoryNames, allSubcategories) {
        if (!categoryId || !Array.isArray(subcategoryNames) || subcategoryNames.length === 0) return [];
        const relevantSubcategories = allSubcategories.filter(sub => sub.parentId === categoryId);
        const foundSubIds = [];
        subcategoryNames.forEach(csvSubName => {
            const trimmedCsvSubName = csvSubName.trim().toLowerCase();
            if (trimmedCsvSubName) { // Avoid empty strings
                const foundSub = relevantSubcategories.find(sub => sub.name.toLowerCase() === trimmedCsvSubName);
                if (foundSub) {
                    foundSubIds.push(foundSub.id);
                } else {
                     console.warn(`Subcategory "${csvSubName}" not found for category ID "${categoryId}".`);
                     // Optionally show a toast or mark this product for review during import
                }
            }
        });
        return foundSubIds;
    }


    // Prefills the product form with data from a single CSV row object
    function prefillFormFromCsvRow(csvRowData, categories, allSubcategories) {
        // Ensure form is reset before prefilling
        resetForm(); // Clears previous data, images, etc.

        // Map CSV headers to form fields
        // CSV headers: Name,Description,Category,Sub Categories List,Stocks,Mrp,Rate,Gst,Discount
        // Form field IDs: product-name, product-desc, product-category, etc.

        productNameInput.value = csvRowData["Name"] ? csvRowData["Name"].trim() : '';
        productDescInput.value = csvRowData["Description"] ? csvRowData["Description"].trim() : '';
        // productHsnInput.value = csvRowData["Hsn"] ? csvRowData["Hsn"].trim() : '';
        // productCidInput.value = csvRowData["Cid"] ? csvRowData["Cid"].trim() : '';
        

        // Handle Category lookup and selection
        const categoryName = csvRowData["Category"] ? csvRowData["Category"].trim() : '';
        const categoryId = getCategoryIdByName(categoryName, categories);
        productCategorySelect.value = categoryId || ''; // Set the dropdown value

        // Manually trigger the change event on the category select
        // so populateSubcategoryCheckboxes runs for the prefilled category.
        // Use a slight delay to ensure the DOM update happens first before checking subcats.
        setTimeout(() => {
            const changeEvent = new Event('change');
            productCategorySelect.dispatchEvent(changeEvent);

            // After subcategories are populated, select the correct checkboxes
            const subcategoryNamesFromCsv = Array.isArray(csvRowData["Sub Categories List"]) ? csvRowData["Sub Categories List"] : (csvRowData["Sub Categories List"] ? csvRowData["Sub Categories List"].split(';').map(s => s.trim()) : []);
            if (categoryId && subcategoryNamesFromCsv.length > 0) {
                 const subcategoryIdsToCheck = getSubcategoryIdsByNames(categoryId, subcategoryNamesFromCsv, allSubcategories);
                 subcategoryIdsToCheck.forEach(subId => {
                     const checkbox = productSubcategoriesContainer.querySelector(`input[value="${subId}"]`);
                     if (checkbox) {
                         checkbox.checked = true;
                     }
                 });
            } else if (!categoryId && subcategoryNamesFromCsv.length > 0) {
                console.warn(`Skipping subcategory prefill for product "${csvRowData["Name"]}" because category "${categoryName}" was not found.`);
            }

        }, 50); // Short delay to allow DOM update

        // Handle numeric fields (parseCSV should handle initial conversion, but ensure input is number)
        costRateInput.value = parseFloat(csvRowData["Rate"]) || 0;
        costMrpInput.value = parseFloat(csvRowData["Mrp"]) || 0;
        costGstInput.value = parseFloat(csvRowData["Gst"]) || 0;
        costDisInput.value = parseFloat(csvRowData["Discount"]) || 0;
        stockInput.value = parseInt(csvRowData["Stocks"], 10) || 0;

        // CSV sample doesn't have images. currentImageUrls is reset by resetForm().
        // User can manually add images if needed after prefilling using existing imagePicker/addImage functionality.
        renderImagePreviews(); // Ensure preview section is correctly rendered (empty initially)


        // Show the cancel button specifically during the import flow
        cancelButton.style.display = 'inline-block';

        // Note: Form title and save button text are updated in the loop itself
        // to show current item number.

        feather.replace(); // Replace icons in the updated form section
        productForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }


    // Waits for the user to click Save or Cancel on the prefilled form during import
    function waitForUserActionForProduct() {
        return new Promise((resolve) => {
            // Define handlers
            const saveHandler = (event) => {
                event.preventDefault(); // Prevent default form submission
                removeListeners();
                resolve('save');
            };

            const cancelHandler = (event) => {
                event.preventDefault(); // Prevent default button action
                removeListeners();
                resolve('cancel');
            };

            // Function to remove the listeners
            const removeListeners = () => {
                saveButton.removeEventListener('click', saveHandler);
                cancelButton.removeEventListener('click', cancelHandler);
            };

            // Attach listeners
            saveButton.addEventListener('click', saveHandler);
            cancelButton.addEventListener('click', cancelHandler);
        });
    }


    // Function to loop through the CSV data one by one, allowing user interaction
    async function loopTheDataToSaveWisely(data) {
        console.log(`Starting step-by-step import of ${data.length} products...`);
        showToast(`Starting import of ${data.length} products...`, 'info', 3000);

        // Get categories and subcategories once before the loop for lookups
        const categories = getCategories();
        const allSubcategories = getSubcategories();

        // Store original form button text/state to restore later
        const originalSaveButtonHtml = saveButton.innerHTML;
        const originalCancelButtonDisplay = cancelButton.style.display;
        const originalFormTitle = formTitle.textContent;

        let successfullyAddedCount = 0;
        let skippedCount = 0;
        let failedCount = 0;
        let duplicateCount = 0;


        // Loop through each item using a standard for loop
        for (let i = 0; i < data.length; i++) {
            const csvRowData = data[i];
            const productName = csvRowData["Name"] || `Item ${i + 1}`;

            console.log(`Processing item ${i + 1} of ${data.length}: "${productName}"`);

            // 1. Prefill the form with data from the current CSV row
            prefillFormFromCsvRow(csvRowData, categories, allSubcategories);
            formTitle.textContent = `Review/Add Product from CSV (${i + 1} of ${data.length}): "${escapeHtml(productName)}"`;
            // showToast(`Reviewing: "${escapeHtml(productName)}"`, 'info', 1500); // Short toast per item

            // 2. Wait for user action on THIS specific product form
            const action = await waitForUserActionForProduct(); // This pauses the loop

            // 3. Process user action
            if (action === 'save') {
                console.log(`User chose to SAVE "${productName}"`);
                // Gather data from the form and attempt to save/add it
                // processProductSaveFromForm reads the CURRENT state of the form
                const success = await processProductSaveFromForm(); // This handles validation, API, storage

                if (success === true) {
                    successfullyAddedCount++;
                    showToast(`Added: "${productName}"`, 'success', 2000);
                    // Update the table display after successful save
                     handleFilterAndSearch(); // Call this to refresh the table
                } else if (success === false) {
                     // processProductSaveFromForm already showed an error/warning toast (e.g., validation, API error, duplicate)
                     // We need to distinguish between hard failures and duplicates if needed for counts
                     const products = getProducts(); // Reload to check if it was added anyway (e.g. duplicate warning but saved something else)
                     const isDuplicate = products.some(p => p.product_name.toLowerCase() === productName.toLowerCase());

                     if(isDuplicate) {
                         duplicateCount++;
                         // Toast already shown by processProductSaveFromForm
                     } else {
                        failedCount++;
                         // Toast already shown by processProductSaveFromForm
                     }
                }


            } else if (action === 'cancel') {
                console.log(`User chose to SKIP "${productName}"`);
                skippedCount++;
                showToast(`Skipped: "${productName}"`, 'warning', 1500);
                // The form will be reset at the start of the next loop iteration anyway.
            } else {
                 console.error("Unknown action received:", action);
                 failedCount++;
                 showToast(`Unknown action for "${productName}". Skipping.`, 'error', 2000);
            }

            // Optional: Add a small delay between items to avoid overwhelming the user/UI
            // await new Promise(resolve => setTimeout(resolve, 50)); // Pause for 50ms
        }

        // --- Import Loop Finished ---
        console.log("Step-by-step import finished.");
        const finalMessage = `Import finished! Added: ${successfullyAddedCount}, Skipped: ${skippedCount}, Failed: ${failedCount}, Duplicates: ${duplicateCount}.`;
        showToast(finalMessage, 'info', 7000); // Keep final message longer

        // Restore form state and UI elements
        resetForm(); // Ensure form is clear and in default state at the end
        saveButton.innerHTML = originalSaveButtonHtml; // Restore default button text
        cancelButton.style.display = originalCancelButtonDisplay; // Restore cancel button display
        formTitle.textContent = originalFormTitle; // Restore default title
        handleFilterAndSearch(); // Final table refresh just in case

    }


    // --- CSV File Processing (Existing code) ---
    async function onCsvFileImported(file) {
        const reader = new FileReader();
        reader.onload = function (e) {
            const text = e.target.result;
            const data = parseCSV(text); // data is the array of objects
            // displayData(data); // Your existing console log - useful for debugging
            loopTheDataToSaveWisely(data); // <<< Call the new step-by-step function
        };
        reader.onerror = function (e) {
             console.error("Error reading file:", e);
             showToast("Error reading CSV file.", 'error');
        }
        reader.readAsText(file);
    }

    function parseCSV(text) {
        // Note: This simple parser assumes no commas or newlines within fields.
        // For more robust parsing, consider a library like PapaParse.
        const lines = text.trim().split('\n');
        if (lines.length === 0) return [];

        const headers = lines[0].split(',').map(header => header.trim());
        // Basic check to ensure headers are valid
        if (!headers.includes("Name") || !headers.includes("Category") || !headers.includes("Rate") || !headers.includes("Mrp") || !headers.includes("Stocks")) {
            showToast("CSV headers are missing required columns (Name, Category, Rate, Mrp, Stocks).", 'error');
            return []; // Return empty array if headers are bad
        }

        return lines.slice(1).map(line => {
          const values = line.split(',');
          const obj = {};

          headers.forEach((header, index) => {
            const key = header; // Use the already trimmed header key
            let value = values[index]?.trim();

            // Special case: convert "Sub Categories List" into array
            if (key === "Sub Categories List") {
              obj[key] = value ? value.split(';').map(item => item.trim()).filter(item => item) : []; // Split, trim, and filter out empty strings
            }
            // Convert known numeric fields to numbers
            else if (["Stocks", "Mrp", "Rate", "Gst", "Discount"].includes(key)) {
              obj[key] = parseFloat(value);
              if (isNaN(obj[key])) {
                  console.warn(`Invalid number format for ${key}: "${value}". Setting to 0.`);
                  obj[key] = 0; // Default to 0 if parsing fails
              }
            } else {
              obj[key] = value;
            }
          });

          return obj;
        }).filter(obj => obj["Name"]); // Filter out rows with no Name
    }

    function displayData(data) {
        console.clear(); // optional: clears the console before logging
        console.log("Parsed CSV Data:");
        data.forEach((row, index) => {
          console.log(`Item ${index + 1}:`, row);
        });
    }

    // --- Image Picker/Upload Logic (Existing code) ---

    async function imageSelected(imageBlob) {
        console.log('Image selected and compressed. Blob object:', imageBlob);
        showLoading(); // Assuming this shows a global loading indicator
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0]; // "YYYY-MM-DD"

        // Generate a simple random string (e.g., 8 characters)
        const randomStr = Math.random().toString(36).substring(2, 10); // "fj3kd9wq"

        // Combine into a unique path (adjust path as needed)
        const path = `images/products/product_${dateStr}_${Date.now()}_${randomStr}.jpeg`; // Use .jpeg extension for compressed images

        try {
             const url = await uploadBlobToFirebase(imageBlob, path); // Assuming this uploads and returns URL
             window.addImageUploaded(url); // Call the function to add URL to form/previews
             showToast("Image uploaded successfully!", "success");
        } catch (error) {
             console.error("Image upload failed:", error);
             showToast("Image upload failed. Please try again.", "error");
        } finally {
             hideLoading(); // Assuming this hides the loading indicator
        }
    }


    async function imagePicker() {
        const fileInput = document.getElementById('fileInput'); // Assuming this is the hidden file input
        const cropModal = document.getElementById('cropModal'); // Assuming you have a modal for cropping
        const cropImage = document.getElementById('cropImage'); // Image element inside modal
        const cancelBtn = document.getElementById('cancelBtn'); // Cancel button in modal
        const cropBtn = document.getElementById('cropBtn'); // Crop button in modal
        const preview = document.getElementById('preview'); // Element to show final preview (optional)
        let cropper = null;

        // Controller variable for image quality (0 to 1)
        const imageQuality = 0.9; // 70% quality

        // Ensure fileInput exists
        if (!fileInput) {
            console.error("Image file input (id='fileInput') not found.");
            return;
        }

        // Listener is added once in initMod3
        fileInput.addEventListener('change', function(e) { // Use function() for older browsers or if 'this' is needed
            const file = e.target.files[0];
            if (!file) return;

            // Basic file type validation
            if (!file.type.startsWith('image/')) {
                 showToast("Please select an image file.", "warning");
                 fileInput.value = ''; // Clear the input
                 return;
            }

            console.log('Original Image File:', file.name);
            console.log('Original Image Size:', (file.size / 1024).toFixed(2), 'KB', (file.size / (1024 * 1024)).toFixed(2), 'MB');

            const url = URL.createObjectURL(file);
            if (cropImage) {
                cropImage.src = url;
                if (cropModal) cropModal.classList.remove('hidden'); // Show modal

                cropImage.onload = () => {
                    console.log('Original Image Resolution:', cropImage.naturalWidth, 'x', cropImage.naturalHeight);
                    if (cropper) cropper.destroy(); // Destroy previous instance
                     if (cropImage) { // Check if cropImage exists
                         cropper = new Cropper(cropImage, {
                             aspectRatio: NaN,
                             viewMode: 1, // 0: no restrictions, 1: restrict crop box to canvas, 2: restrict canvas to container, 3: restrict container to canvas
                             background: false,
                             modal: true,
                             guides: false,
                             movable: true,
                             zoomable: true,
                             responsive: true,
                             autoCropArea: 1, // 80% of the image
                             // Add other Cropper.js options as needed
                         });
                     } else {
                          console.error("Crop image element not found.");
                          if (cropModal) cropModal.classList.add('hidden');
                          URL.revokeObjectURL(url);
                          fileInput.value = '';
                     }
                };

                // Handle error loading image
                 cropImage.onerror = () => {
                    cropImage.src = '';
                    fileInput.value = '';
                 };

            } else {
                 console.error("Crop image element (id='cropImage') not found.");
                 URL.revokeObjectURL(url);
                 fileInput.value = '';
                 showToast("Error initializing image crop.", "error");
            }
        });

        // Ensure modal elements exist before adding listeners
        if (cancelBtn && cropBtn && cropModal && cropImage) {
            cancelBtn.addEventListener('click', () => {
                cropModal.classList.add('hidden');
                if (cropper) cropper.destroy();
                if (cropImage.src) URL.revokeObjectURL(cropImage.src); // Clean up object URL
                cropImage.src = '';
                fileInput.value = ''; // Clear the file input
            });

            cropBtn.addEventListener('click', () => {
                if (!cropper) return;

                // Get cropped canvas at target size (e.g., 300x300)
                const canvas = cropper.getCroppedCanvas({ width: 800, height: 800 });
                console.log('Cropped Image Resolution (Target):', canvas.width, 'x', canvas.height);

                // Convert canvas to blob (original quality, then compress)
                // Using 'image/jpeg' and a default quality (0.9 here) before Compressor.js
                // The actual compression quality is controlled by Compressor.js
                canvas.toBlob(blob => {
                    if (!blob) {
                        console.error("Error creating blob from canvas.");
                         showToast("Error processing image.", "error");
                         // Close modal and clean up
                         cropModal.classList.add('hidden');
                         if (cropper) cropper.destroy();
                         if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                         cropImage.src = '';
                         fileInput.value = '';
                        return;
                    }
                    console.log('Cropped Image Size (Before Compression):', (blob.size / 1024).toFixed(2), 'KB', (blob.size / (1024 * 1024)).toFixed(2), 'MB');

                    // Compress the blob using Compressor.js
                    new Compressor(blob, {
                        quality: imageQuality, // Use the controller variable here (e.g., 0.7)
                        maxWidth: 800, // Ensure final image is not larger than this
                        maxHeight: 800,
                        convertSize: 0, // Always convert, helps ensure consistent format/quality
                        success(compressedBlob) {
                            console.log('Compressed Image Size:', (compressedBlob.size / 1024).toFixed(2), 'KB', (compressedBlob.size / (1024 * 1024)).toFixed(2), 'MB');
                            // Create URL for preview (optional)
                            // const url = URL.createObjectURL(compressedBlob);
                            // if(preview) { // Optional preview update
                            //     preview.innerHTML = '';
                            //     const imgEl = document.createElement('img');
                            //     imgEl.src = url;
                            //     imgEl.className = 'rounded-lg shadow-md';
                            //     preview.appendChild(imgEl);
                            // }

                            // *** Call the imageSelected function with the compressed blob ***
                            imageSelected(compressedBlob); // This handles the upload

                            // Clean up
                            cropModal.classList.add('hidden');
                            if (cropper) cropper.destroy();
                            if (cropImage.src) URL.revokeObjectURL(cropImage.src); // Clean up object URL
                            cropImage.src = '';
                            fileInput.value = ''; // Clear the file input

                        },
                        error(err) {
                            console.error('Compression error:', err.message);
                            showToast(`Image compression failed: ${err.message}`, 'error');
                             // Clean up
                            cropModal.classList.add('hidden');
                            if (cropper) cropper.destroy();
                            if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                            cropImage.src = '';
                            fileInput.value = '';
                        },
                    });
                }, 'image/jpeg', 0.9); // Format and initial quality for canvas.toBlob
            });
        } else {
            console.error("Image crop modal or button elements not found.");
            // The file input listener is still active, but modal won't show.
            // Consider disabling the 'Add Image' button if modal elements are missing.
        }
    }


    // --- Attach Event Listeners ---
    productForm.addEventListener('submit', handleFormSubmit); // Listener for standard form submit
    // The cancel button listener is handled dynamically by waitForUserActionForProduct during import
    // and is attached here for manual editing cancellation
     cancelButton.addEventListener('click', resetForm); // Listener for manual cancel

    addImageBtn.addEventListener('click', () => {
        addImage();
    }); // Assuming addImageBtn triggers the file input


    productCategorySelect.addEventListener('change', handleCategoryChange); // For populating subcats
    searchInput.addEventListener('input', handleFilterAndSearch);
    categoryFilterSelect.addEventListener('change', handleFilterAndSearch);

    // CSV File Input Listener
    fileInputCsv.addEventListener('change', function(event) {
        const file = event.target.files[0];
        if (!file) return;
        // Basic file type check
        if (!file.name.endsWith('.csv')) {
             showToast("Please select a CSV file.", "warning");
             fileInputCsv.value = ''; // Clear the input
             return;
        }
        onCsvFileImported(file);
        fileInputCsv.value = ''; // Clear the input after selection
    });


    // --- Initial Load ---
    populateCategoryDropdowns();

    // Load initial product data from API and save to local storage
     showLoading("Loading products..."); // Show loading indicator
     try {
         const productData = await callApi('POST', 'products/query', {
             filters: [],
             limit: 10000, // Adjust limit as needed
             offset: 0,
             order_by: "product_name",
             order_direction: "ASC"
         });

         const productList = Array.isArray(productData) ? productData.map(p => ({
             id: p.id, // Assuming this is the local storage UUID-like ID
             product_id: p.product_id, // Assuming this is a potentially external/API ID
             product_name: p.product_name,
             product_desc: p.product_desc,
             product_hsn: p.product_hsn,
             product_cid: p.product_cid,
             product_img: Array.isArray(p.product_img) ? p.product_img : (p.product_img ? [p.product_img] : []), // Ensure it's an array
             cat_id: p.cat_id,
             cat_sub: p.cat_sub ? p.cat_sub.split(',').filter(Boolean) : [], // Convert comma-separated string to array
             cost_rate: parseFloat(p.cost_rate) || 0,
             cost_mrp: parseFloat(p.cost_mrp) || 0,
             cost_gst: parseFloat(p.cost_gst) || 0,
             cost_dis: parseFloat(p.cost_dis) || 0,
             stock: parseInt(p.stock, 10) || 0
         })) : [];

         saveProducts(productList); // Save loaded products to local storage
         renderProductTable(); // Load and display products from local storage
          showToast(`Loaded ${productList.length} products.`, 'info', 3000);

     } catch (error) {
         console.error("Error loading initial product data:", error);
         showToast("Failed to load initial product data.", 'error');
         renderProductTable([]); // Render empty table on failure
         saveProducts([]); // Clear local storage if load failed? Or keep old data? Keep old data might be safer.
         // If load failed, render from potentially old local storage data?
         // const productsFromStorage = getProducts();
         // renderProductTable(productsFromStorage);
         // showToast(`Failed to load fresh data. Showing ${productsFromStorage.length} products from last session.`, 'warning', 5000);
     } finally {
         hideLoading(); // Hide loading indicator
     }

    // Ensure subcategory checkboxes area is initially empty/prompted
    populateSubcategoryCheckboxes(null);

    // Initialize image picker logic
    imagePicker(); // Setup the event listeners for the image input/modal
}

// Make initMod3 available globally or export it if using modules
window.initMod3 = initMod3;