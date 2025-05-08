

async function initMod5() {
    console.log("Initializing Product Management Module (Mod5)...");

    // --- State ---
    let currentImageUrls = []; 

    const newImageUrlInput = document.getElementById("product-new-image-url");
    const addImageBtn = document.getElementById('product-add-image-btn');
    const imagePreviewList = document.getElementById('product-image-preview-list');
    const noImagesText = document.getElementById('no-images-text');

    async function imageSelected(imageBlob) {
        console.log('Image selected and compressed. Blob object:', imageBlob);
        showLoading(); // Assuming this shows a global loading indicator
        const now = new Date();
        const dateStr = now.toISOString().split('T')[0]; // "YYYY-MM-DD"
    
        // Generate a simple random string (e.g., 8 characters)
        const randomStr = Math.random().toString(36).substring(2, 10); // "fj3kd9wq"
    
        // Combine into a unique path (adjust path as needed)
        const path = `images/banners/banner_${dateStr}_${Date.now()}_${randomStr}.jpeg`; // Use .jpeg extension for compressed images
    
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

    
    // async function removeImage(index) {
    //     await deleteFileFromFirebase(currentImageUrls[index]);
    //     currentImageUrls.splice(index, 1);
    //     renderImagePreviews();
    //     showToast('Image URL removed.', 'info');
    // }

    async function removeImage(index) {
        showLoading();
        const imageUrl = currentImageUrls[index];  // Get the image URL
        const imageKey = Object.keys(data)[index];  // Get the unique key for the image in Firebase
    
        try {
            // Delete the image URL from Firebase Realtime Database using the key
            await database.ref('datas/announcement/all/' + imageKey).remove();
            console.log(`Image URL removed from Firebase: ${imageUrl}`);
            

            // Remove from the current image URLs array
            currentImageUrls.splice(index, 1);
                        
        } catch (error) {
            console.error("Failed to remove image URL from Firebase:", error);
            showToast('Failed to remove image URL. Please try again.', 'error');
        }

        try{
            await deleteFileFromFirebase(imageUrl);
        } catch(e){

        }
        hideLoading();
        
        showToast('Image URL removed.', 'info');
        window.location.reload();
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
            <a href="${escapeHtml(url)}" target="_blank">
                <img src="${escapeHtml(url)}" class="img-preview" alt="Preview ${index+1}" 
                     style="width: 100%; height: auto;" onerror="this.style.display='none'; this.nextElementSibling.style.display='block';">
            </a>
            <span style="display: none; font-size: 0.7em; color: var(--text-muted);">Invalid URL or image</span>
            <button type="button" class="remove-img-btn" data-index="${index}"></button>
        `;
        
            item.querySelector('.remove-img-btn').onclick = () => removeImage(index);
            imagePreviewList.appendChild(item);
        });
    }
    
    const snapshot = await database.ref('datas/announcement/all').once('value');
    const data = snapshot.val();

    if (data) {
        const values = Object.values(data);
        for (let i = 0; i < values.length; i++) {
            const url = values[i];
            currentImageUrls.push(url);
        }
        renderImagePreviews();
    }

    function addImageUploaded(url) {
        newImageUrlInput.value = url;
        addImage();
    }

    window.addImage = addImage;
    window.addImageUploaded = addImageUploaded; // Make globally accessible for the image picker

    async function addImage() {
        const url = newImageUrlInput.value.trim();
        console.log(url)
        if (!url) {
             showToast('Please enter an image URL.', 'error'); return;
        }
        if (!isValidHttpUrl(url)) {
             showToast('Invalid URL format. Please enter a valid HTTP/HTTPS URL.', 'error'); return;
        }
        if (currentImageUrls.includes(url)) {
             showToast('This image URL has already been added.', 'error'); return;
        }

        showLoading();

        const currentTimeMs = Date.now();

        await database.ref('datas/announcement/all/'+currentTimeMs).set(url);
        currentImageUrls.push(url);
        newImageUrlInput.value = ''; // Clear input field
        hideLoading();
        showToast('Image URL added.', 'success');
        window.location.reload();
    }
    
    async function imagePicker() {
        const fileInput = document.getElementById('fileInput');
        const cropModal = document.getElementById('cropModal');
        const cropImage = document.getElementById('cropImage');
        const cancelBtn = document.getElementById('cancelBtn');
        const cropBtn = document.getElementById('cropBtn');
        const preview = document.getElementById('preview');
        let cropper = null;
    
        const imageQuality = 1; // High quality, no compression loss
    
        if (!fileInput) {
            console.error("Image file input (id='fileInput') not found.");
            return;
        }
    
        fileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (!file) return;
    
            if (!file.type.startsWith('image/')) {
                showToast("Please select an image file.", "warning");
                fileInput.value = '';
                return;
            }
    
            const url = URL.createObjectURL(file);
            if (cropImage) {
                cropImage.src = url;
                if (cropModal) cropModal.classList.remove('hidden');
    
                cropImage.onload = () => {
                    if (cropper) cropper.destroy();
                    cropper = new Cropper(cropImage, {
                        aspectRatio: NaN,
                        viewMode: 1,
                        background: false,
                        modal: true,
                        guides: false,
                        movable: true,
                        zoomable: true,
                        responsive: true,
                        autoCropArea: 1,
                    });
                };
    
                cropImage.onerror = () => {
                    cropImage.src = '';
                    fileInput.value = '';
                };
            } else {
                console.error("Crop image element not found.");
                URL.revokeObjectURL(url);
                fileInput.value = '';
                showToast("Error initializing image crop.", "error");
            }
        });
    
        if (cancelBtn && cropBtn && cropModal && cropImage) {
            cancelBtn.addEventListener('click', () => {
                cropModal.classList.add('hidden');
                if (cropper) cropper.destroy();
                if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                cropImage.src = '';
                fileInput.value = '';
            });
    
            cropBtn.addEventListener('click', () => {
                if (!cropper) return;
    
                // Get the cropped canvas with the desired dimensions
                const canvas = cropper.getCroppedCanvas({ width: cropImage.naturalWidth, height: cropImage.naturalHeight });
                console.log('Cropped Image Resolution (Target):', canvas.width, 'x', canvas.height);
    
                // Convert canvas to blob (original quality, then compress)
                canvas.toBlob(blob => {
                    if (!blob) {
                        console.error("Error creating blob from canvas.");
                        showToast("Error processing image.", "error");
                        cropModal.classList.add('hidden');
                        if (cropper) cropper.destroy();
                        if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                        cropImage.src = '';
                        fileInput.value = '';
                        return;
                    }
    
                    console.log('Cropped Image Size (Before Compression):', (blob.size / 1024).toFixed(2), 'KB', (blob.size / (1024 * 1024)).toFixed(2), 'MB');
    
                    new Compressor(blob, {
                        quality: imageQuality, // No loss of quality
                        maxWidth: cropImage.naturalWidth, // Retain original width
                        maxHeight: cropImage.naturalHeight, // Retain original height
                        convertSize: 0, // Always convert
                        success(compressedBlob) {
                            console.log('Compressed Image Size:', (compressedBlob.size / 1024).toFixed(2), 'KB', (compressedBlob.size / (1024 * 1024)).toFixed(2), 'MB');
                            imageSelected(compressedBlob); // Handle the upload
    
                            cropModal.classList.add('hidden');
                            if (cropper) cropper.destroy();
                            if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                            cropImage.src = '';
                            fileInput.value = '';
                        },
                        error(err) {
                            console.error('Compression error:', err.message);
                            showToast(`Image compression failed: ${err.message}`, 'error');
                            cropModal.classList.add('hidden');
                            if (cropper) cropper.destroy();
                            if (cropImage.src) URL.revokeObjectURL(cropImage.src);
                            cropImage.src = '';
                            fileInput.value = '';
                        },
                    });
                }, 'image/jpeg', 0.2); // Using high-quality JPEG compression
            });
        } else {
            console.error("Image crop modal or button elements not found.");
        }
    }
    

    imagePicker();
}

// Make initMod3 available globally or export it if using modules
window.initMod5 = initMod5;