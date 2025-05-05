async function generateInvoice(invoiceData) {

    // (Paste the entire JavaScript code from the previous response here)
  function generateSummaryRows(rowsData, colCountEmpty) {
    const tableBody = document.createElement('tbody');
  
    rowsData.forEach(rowData => {
      const row = document.createElement('tr');
      if (rowData.isBlank) {
        const cell = document.createElement('td');
        cell.setAttribute('colspan', rowData.colspan);
        row.appendChild(cell);
      } else {
        const emptyCell = document.createElement('td');
        row.appendChild(emptyCell);
  
        const labelCell = document.createElement('td');
        labelCell.textContent = rowData.label;
        row.appendChild(labelCell);
  
        for (let i = 0; i < colCountEmpty; i++) {
          const emptyCell = document.createElement('td');
          row.appendChild(emptyCell);
        }
  
        const valueCell = document.createElement('td');
        if (rowData.isBold) {
          const boldElement = document.createElement('b');
          boldElement.textContent = rowData.value;
          valueCell.appendChild(boldElement);
        } else {
          valueCell.textContent = rowData.value;
        }
        row.appendChild(valueCell);
      }
      tableBody.appendChild(row);
    });
  
    return tableBody;
  }
  
  function generateSummaryRows2(rowsData,cgst, sgst) {
    const tableBody = document.createElement('tbody');
  
    rowsData.forEach(rowData => {
      const row = document.createElement('tr');
      if (rowData.isBlank) {
        const cell = document.createElement('td');
        cell.setAttribute('colspan', rowData.colspan);
        row.appendChild(cell);
      } else {
        const labelCell = document.createElement('td');
        labelCell.textContent = rowData.label;
        row.appendChild(labelCell);
  
        for (let i = 0; i < 2; i++) {
          const emptyCell = document.createElement('td');
          row.appendChild(emptyCell);
        }
  
        const emptyCell1 = document.createElement('td');
        emptyCell1.innerHTML = cgst;
        row.appendChild(emptyCell1);
  
        for (let i = 0; i < 1; i++) {
            const emptyCell = document.createElement('td');
            row.appendChild(emptyCell);
        }
  
        const emptyCell2 = document.createElement('td');
        emptyCell2.innerHTML = sgst
        row.appendChild(emptyCell2);
  
        const valueCell = document.createElement('td');
        if (rowData.isBold) {
          const boldElement = document.createElement('b');
          boldElement.textContent = rowData.value;
          valueCell.appendChild(boldElement);
        } else {
          valueCell.textContent = rowData.value;
        }
        row.appendChild(valueCell);
      }
      tableBody.appendChild(row);
    });
  
    return tableBody;
  }
  
  
      try {
          const response = await fetch('modules/mod4/bill.html');
          if (!response.ok) {
              throw new Error(`HTTP error! status: ${response.status}`);
          }
          const htmlTemplate = await response.text();
  
          const parser = new DOMParser();
          const doc = parser.parseFromString(htmlTemplate, 'text/html');
  
          // Company Information
          const addressElement = doc.getElementById('Address');
          if (addressElement) addressElement.textContent = invoiceData.company.address;
          const gstNoElement = doc.getElementById('GstNo');
          if (gstNoElement) gstNoElement.textContent = invoiceData.company.gstNo;
          const emailElement = doc.getElementById('Email');
          if (emailElement) emailElement.textContent = invoiceData.company.email;
  
  
          // Delivery Note Details
          const invoiceNoElement = doc.getElementById('InvoiceNo');
          if (invoiceNoElement) invoiceNoElement.textContent = invoiceData.details.invoiceNo;
          const datedElement = doc.getElementById('Dated');
          if (datedElement) datedElement.textContent = invoiceData.details.dated;
          const deliveryNoteElement = doc.getElementById('DeliveryNote');
          if (deliveryNoteElement) deliveryNoteElement.textContent = invoiceData.details.deliveryNote;
          const refNoDateElement = doc.getElementById('RefNoDate');
          if (refNoDateElement) refNoDateElement.textContent = invoiceData.details.refNoDate;
          const otherRefElement = doc.getElementById('OtherRef');
          if (otherRefElement) otherRefElement.textContent = invoiceData.details.otherRef;
          const checkedByElement = doc.getElementById('CheckedBy');
          if (checkedByElement) checkedByElement.textContent = invoiceData.details.checkedBy;
  
  
          // Consignee and Buyer Details
          const cAddress1Element = doc.getElementById('Caddress1');
          if (cAddress1Element) cAddress1Element.textContent = invoiceData.consignee.address;
          const cContact1Element = doc.getElementById('Ccontact1');
          if (cContact1Element) cContact1Element.textContent = invoiceData.consignee.contactNo;
          const cAddress2Element = doc.getElementById('Caddress2');
          if (cAddress2Element) cAddress2Element.textContent = invoiceData.buyer.address;
          const cContact2Element = doc.getElementById('Ccontact2');
          if (cContact2Element) cContact2Element.textContent = invoiceData.buyer.contactNo;
  
  
          // Item Details
          const productTableBody = doc.getElementById('productTableEditable');
          if (productTableBody) {
               // Clear existing placeholder rows
               productTableBody.innerHTML = '';
               invoiceData.items.forEach(item => {
                 const row = doc.createElement('tr');
                 row.innerHTML = `
                   <td>${item.sNo}</td>
                   <td>${item.description}</td>
                   <td>${item.hsnSac}</td>
                   <td>${item.partNo}</td>
                   <td>${item.quantityShipped}</td>
                   <td>${item.quantityBilled}</td>
                   <td>${item.rate.toFixed(2)}</td>
                   <td>${item.discount}</td>
                   <td>${item.amount.toFixed(2)}</td>
                 `;
                 productTableBody.appendChild(row);
               });
          }
  
          // Example of how to use the function with parameter values:
          const subTotalValue = invoiceData.totals.subTotal;
          const cgstValue = invoiceData.totals.cgstAmount;
          const sgstValue = invoiceData.totals.sgstAmount;
          const discountValue = invoiceData.totals.specialDiscount;
          const roundOffValue = invoiceData.totals.roundOff;
          const totalValue = invoiceData.totals.total;
          
          
          const rowsData = [
              { colspan: 9, isBlank: true },
              { label: 'Sub Total', value: subTotalValue.toFixed(2) },
              { label: 'Output CGST', value: cgstValue.toFixed(2) },
              { label: 'Output SGST', value: sgstValue.toFixed(2) },
              { label: 'Special Discount', value: `(-)${discountValue.toFixed(2)}`, isBold: true },
              { label: 'Round Off', value: `(-)${roundOffValue.toFixed(2)}`, isBold: true },
              { label: 'Total', value: totalValue.toFixed(2) },
          ];
          
          const summaryRows = generateSummaryRows(rowsData, 6);
          productTableBody.appendChild(summaryRows);
  
  
          // GST Details
          const gstTableBody = doc.getElementById('gstTableEditable');
          if (gstTableBody) {
              // Clear existing placeholder rows
              gstTableBody.innerHTML = '';
              invoiceData.gstDetails.forEach(gst => {
                  const row = doc.createElement('tr');
                  row.innerHTML = `
                      <td>${gst.hsnSac}</td>
                      <td>${gst.taxableValue.toFixed(2)}</td>
                      <td>${gst.cgstRate}</td>
                      <td>${gst.cgstAmount.toFixed(2)}</td>
                      <td>${gst.sgstUtgstRate}</td>
                      <td>${gst.sgstUtgstAmount.toFixed(2)}</td>
                      <td>${gst.totalTaxAmount.toFixed(2)}</td>
                  `;
                  gstTableBody.appendChild(row);
              });
          }
  
          const rowsData2 = [
              { colspan: 8, isBlank: true },
              { label: 'Total', value: (cgstValue + sgstValue).toFixed(2) },
          ];
  
          const summaryRows2 = generateSummaryRows2(rowsData2, invoiceData.totals.cgstAmount.toFixed(2), invoiceData.totals.sgstAmount.toFixed(2));
          gstTableBody.appendChild(summaryRows2);
  
          const taxAmountInWordsElement = doc.getElementById('taxAmountInWords');
          if (taxAmountInWordsElement && invoiceData.amountsInWords) {
              taxAmountInWordsElement.textContent = invoiceData.amountsInWords.taxAmount;
          }
  
          const amountChargeableInWords = doc.getElementById('AmountChargeableInWords');
          if (amountChargeableInWords && invoiceData.amountsInWords) {
              amountChargeableInWords.textContent = invoiceData.amountsInWords.amountChargeable;
          }
  
          return doc.documentElement.outerHTML;
      } catch (error) {
          console.error('Error generating invoice:', error);
          return null;
      }
}



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
        var bill = await callApi("GET", "bills/"+orderId)

    
        const generatedHtml = await generateInvoice(bill);
        if (generatedHtml) {
            // Open in a new tab
            const newWindow = window.open('', '_blank');
            if (newWindow) {
                // Write the generated HTML to the new window
                newWindow.document.write(generatedHtml);
                newWindow.document.close(); // Important to signal the end of the document
    
                // Wait for the content to load before printing
                newWindow.onload = function() {
                    // Trigger the print dialog
                    newWindow.print();
                };
            } else {
                alert('Could not open new window. Please allow pop-ups for this site.');
            }
        } else {
            console.error('Failed to generate invoice.');
        }
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
        user_id: o.user_id,
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