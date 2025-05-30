// modules/mod4/script.js
async function initMod4() {
    console.log("Initializing Order Management Module (Mod4)...");

    // --- DOM Elements ---
    const orderTableBody = document.querySelector('#order-table tbody');
    const orderIdSearchInput = document.getElementById('order-search-id');
    const userIdSearchInput = document.getElementById('order-search-user');
    const statusFilterSelect = document.getElementById('order-status-filter');
    const dateFilterStartInput = document.getElementById('order-date-filter-start');
    const dateFilterEndInput = document.getElementById('order-date-filter-end');
    const clearFiltersBtn = document.getElementById('order-clear-filters-btn');
    const noOrdersMessage = document.getElementById('no-orders-message');

    // --- Storage Key ---
    const ORDER_KEY = 'orders';
    const ORDER_STATUSES = [ // Define available statuses
        { value: "ORDER_PENDING", text: "Pending" },
        { value: "ORDER_IN_PROGRESS", text: "In Progress" },
        { value: "ORDER_DELIVERED", text: "Delivered" },
        { value: "ORDER_CANCELLED", text: "Cancelled" }
    ];

    // --- Data Functions ---
    const getOrders = () => StorageHelper.load(ORDER_KEY, []);
    const saveOrders = (orders) => StorageHelper.save(ORDER_KEY, orders);

    // --- Rendering ---
    function renderOrderTable(filteredOrders = null) {
        const orders = filteredOrders !== null ? filteredOrders : getOrders();
        orderTableBody.innerHTML = '';
        noOrdersMessage.style.display = orders.length === 0 ? 'block' : 'none';

         if (orders.length === 0 && filteredOrders === null) {
             orderTableBody.innerHTML = `<tr><td colspan="6" class="no-data-message">No orders placed yet.</td></tr>`; return;
        }
         if (orders.length === 0 && filteredOrders !== null) {
             orderTableBody.innerHTML = `<tr><td colspan="6" class="no-data-message">No orders match your filters.</td></tr>`; return;
        }

        // Sort orders by creation date descending (most recent first)
         orders.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

        orders.forEach(order => {
            const row = orderTableBody.insertRow();
            row.dataset.orderId = order.order_id;

            // Status dropdown
            let statusOptionsHtml = '';
            ORDER_STATUSES.forEach(status => {
                statusOptionsHtml += `<option value="${status.value}" ${order.order_status === status.value ? 'selected' : ''}>${status.text}</option>`;
            });
            const statusSelectHtml = `
                <select class="status-select" data-order-id="${order.order_id}">
                    ${statusOptionsHtml}
                </select>`;

            row.innerHTML = `
                <td>${escapeHtml(order.order_id)}</td>
                <td>${escapeHtml(order.user_id)}</td>
                <td>${dayjs(order.created_at).format('DD MMM YYYY, hh:mm A')}</td>
                <td class="text-right">₹${escapeHtml(order.total.toFixed(2))}</td>
                <td>${statusSelectHtml}</td>
                <td class="actions-cell">
                    <button class="btn btn-sm btn-outline btn-view-order"><i data-feather="eye"></i> View</button>
                    <button class="btn btn-sm btn-danger btn-delete-order"><i data-feather="trash-2"></i> Delete</button>
                </td>
            `;
            // Add event listeners for this row
             row.querySelector('.status-select').addEventListener('change', handleStatusChange);
             row.querySelector('.btn-view-order').onclick = () => handleViewOrder(order.order_id);
             row.querySelector('.btn-delete-order').onclick = () => handleDeleteOrder(order.order_id);
        });
        feather.replace(); // Apply icons
    }

     function renderOrderDetailsModal(orderId) {
        const order = getOrders().find(o => o.order_id === orderId);
        if (!order) {
            showToast(`Order ${orderId} not found.`, 'error'); return;
        }

        // Format items details
        let itemsHtml = '<p>No item details available.</p>';
        if (order.items_detail && order.items_detail.length > 0) {
            itemsHtml = `
            <table class="order-items-table">
                <thead><tr><th>Item</th><th>Qty</th><th>Rate (₹)</th><th>Total (₹)</th></tr></thead>
                <tbody>`;
            order.items_detail.forEach(item => {
                 const quantity = order.items[item.product_id]?.count || 0; // Get quantity from 'items' object
                 const itemTotal = quantity * item.cost_rate;
                 itemsHtml += `
                    <tr>
                        <td>
                            ${escapeHtml(item.product_name)}<br>
                            <small class="text-muted">ID: ${escapeHtml(item.product_id)}</small>
                        </td>
                        <td class="text-center">${quantity}</td>
                        <td class="text-right">₹${item.cost_rate.toFixed(2)}</td>
                        <td class="text-right">₹${itemTotal.toFixed(2)}</td>
                    </tr>
                `;
            });
            itemsHtml += `</tbody></table>`;
        }

        // Format totals
         const totalsHtml = `
            <div class="order-totals-summary">
                Subtotal: <strong class="text-right">₹${order.total_rate.toFixed(2)}</strong><br>
                GST (${((order.total_gst / order.total_rate) * 100).toFixed(1)}%): <strong class="text-right">₹${order.total_gst.toFixed(2)}</strong><br>
                Discount: <strong class="text-right">- ₹${order.total_discount.toFixed(2)}</strong><br>
                <hr>
                <strong>Total:</strong> <strong class="text-right">₹${order.total.toFixed(2)}</strong>
            </div>
        `;

        // Main modal content
        const modalBodyHtml = `
            <div class="order-details-grid">
                 <dl>
                    <dt>Order ID</dt><dd>${escapeHtml(order.order_id)}</dd>
                    <dt>User ID</dt><dd>${escapeHtml(order.user_id)}</dd>
                 </dl>
                 <dl>
                     <dt>Created At</dt><dd>${dayjs(order.created_at).format('DD MMM YYYY, hh:mm:ss A')}</dd>
                    <dt>Status</dt><dd>${escapeHtml(ORDER_STATUSES.find(s=>s.value === order.order_status)?.text || order.order_status)}</dd>
                 </dl>
             </div>

             <h4>Items</h4>
             ${itemsHtml}

             <h4 class="mt-2">Totals</h4>
             ${totalsHtml}
        `;

        // Use the global modal function from index.html
        window.openModal(`Order Details - ${order.order_id}`, modalBodyHtml);
    }


    // --- Event Handlers ---
    async function handleStatusChange(event) {
        const selectElement = event.target;
        const orderId = selectElement.dataset.orderId;
        const newStatus = selectElement.value;

        let orders = getOrders();
        const orderIndex = orders.findIndex(o => o.order_id === orderId);

        if (orderIndex === -1) {
             showToast(`Order ${orderId} not found for status update.`, 'error'); return;
        }

        // Optional: Add confirmation or logic checks before status change?
        // e.g., cannot revert from 'Delivered' easily.

        orders[orderIndex].order_status = newStatus;
        // Optional: Add an 'updated_at' timestamp if needed
        // orders[orderIndex].updated_at = new Date().toISOString();

        await callApi("PUT", "orders/"+orderId, {
            order_status: newStatus
        });

        saveOrders(orders);
        showToast(`Order ${orderId} status updated to ${ORDER_STATUSES.find(s=>s.value === newStatus)?.text}.`, 'success');
        // No need to re-render full table, just update was successful.
        // Re-rendering might lose scroll position etc.
        // handleFilterAndSearch(); // Uncomment if full re-render is desired
    }

    async function handleViewOrder(orderId) {
        // renderOrderDetailsModal(orderId);
        window.open(`https://pets-fort.web.app/bill.html?orderid=${orderId}`, '_blank');
    }

    async function handleDeleteOrder(orderId) {
        if(checkPermissionForAction("delete","order") == false) {
            showDialog("err", "OK", "You are not allowed to delete orders. due to Permission!!!", function () {
                
            });
            return;
        }
        if (!confirm(`Are you sure you want to delete order ${orderId}? This cannot be undone.`)) {
            return;
        }
        let orders = getOrders();
        orders = orders.filter(o => o.order_id !== orderId);

        await callApi("DELETE", "orders/"+orderId)
        
        saveOrders(orders);
        handleFilterAndSearch(); // Re-render the table
        showToast(`Order ${orderId} deleted successfully.`, 'info');
    }

    function handleFilterAndSearch() {
        const orderIdTerm = orderIdSearchInput.value.toLowerCase().trim();
        const userIdTerm = userIdSearchInput.value.toLowerCase().trim();
        const statusFilter = statusFilterSelect.value;
        const dateStart = dateFilterStartInput.value ? dayjs(dateFilterStartInput.value).startOf('day') : null;
        const dateEnd = dateFilterEndInput.value ? dayjs(dateFilterEndInput.value).endOf('day') : null;

        let orders = getOrders();

        // Apply filters
        if (orderIdTerm) {
            orders = orders.filter(o => o.order_id.toLowerCase().includes(orderIdTerm));
        }
        if (userIdTerm) {
            orders = orders.filter(o => o.user_id.toLowerCase().includes(userIdTerm));
        }
        if (statusFilter) {
            orders = orders.filter(o => o.order_status === statusFilter);
        }
         if (dateStart) {
            orders = orders.filter(o => dayjs(o.created_at).isAfter(dateStart) || dayjs(o.created_at).isSame(dateStart));
        }
         if (dateEnd) {
            orders = orders.filter(o => dayjs(o.created_at).isBefore(dateEnd) || dayjs(o.created_at).isSame(dateEnd));
        }

        renderOrderTable(orders);
    }

     function clearFilters() {
        orderIdSearchInput.value = '';
        userIdSearchInput.value = '';
        statusFilterSelect.value = '';
        dateFilterStartInput.value = '';
        dateFilterEndInput.value = '';
        handleFilterAndSearch(); // Apply cleared filters (show all)
         showToast('Filters cleared.', 'info');
    }

    // --- Attach Event Listeners ---
    orderIdSearchInput.addEventListener('input', handleFilterAndSearch);
    userIdSearchInput.addEventListener('input', handleFilterAndSearch);
    statusFilterSelect.addEventListener('change', handleFilterAndSearch);
    dateFilterStartInput.addEventListener('change', handleFilterAndSearch);
    dateFilterEndInput.addEventListener('change', handleFilterAndSearch);
    clearFiltersBtn.addEventListener('click', clearFilters);

    // --- Initial Load ---

    const orderData = await callApi('POST', 'orders/query', {
        filters: [],
        limit: 10000,
        offset: 0,
        order_by: "created_at",
        order_direction: "DESC"
    });

    const orderList = Array.isArray(orderData) ? orderData.map(o => ({
        order_id: o.order_id,
        user_id: o.user_name,
        items: o.items,
        items_detail: o.items_detail,
        order_status: o.order_status,
        total_rate: o.total_rate,
        total_gst: o.total_gst,
        total_discount: o.total_discount,
        total: o.total,
        created_at: o.created_at
    })) : [];

    saveOrders(orderList);
    renderOrderTable();

    // console.log(orderList);


     // Add default data if needed
     if (getOrders().length === 0 && StorageHelper.load('users',[]).length > 0 && StorageHelper.load('products',[]).length > 0) {
        //  console.log("Adding default order data for testing...");
        //  const sampleUser = StorageHelper.load('users',[])[0];
        //  const sampleProduct = StorageHelper.load('products',[])[0];

        //  saveOrders([{
        //     order_id: `ORD${Date.now()}`, user+_id: sampleUser.id,
        //     items: { [sampleProduct.product_id]: { count: 2 } },
        //     items_detail: [ JSON.parse(JSON.stringify(sampleProduct)) ], // Deep copy product detail
        //     order_status: "ORDER_PENDING",
        //     total_rate: sampleProduct.cost_rate * 2,
        //     total_gst: (sampleProduct.cost_rate * 2) * (sampleProduct.cost_gst / 100),
        //     total_discount: (sampleProduct.cost_rate * 2) * (sampleProduct.cost_dis / 100),
        //     total: (sampleProduct.cost_rate * 2) + ((sampleProduct.cost_rate * 2) * (sampleProduct.cost_gst / 100)) - ((sampleProduct.cost_rate * 2) * (sampleProduct.cost_dis / 100)),
        //     created_at: new Date().toISOString()
        //  }]);
        //  renderOrderTable();
     }

}
window.initMod4 = initMod4;