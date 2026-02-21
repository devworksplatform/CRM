// modules/mod9/script.js — GST Billing Module (Tally-like) — Full Rewrite

async function initMod9() {
    // ==================== STATE ====================
    const cache = {};
    let currentVoucherType = 'credit'; // 'credit' | 'debit'
    let currentGstReport = 'gstr1';
    let salesData = null;
    let chartInstances = {};
    let allUsers = null;

    // ==================== FORMATTING HELPERS ====================
    const fmt = (n) => {
        const num = parseFloat(n) || 0;
        const sign = num < 0 ? '-' : '';
        const abs = Math.abs(num).toFixed(2);
        const [intPart, dec] = abs.split('.');
        // Indian comma grouping: last 3 digits, then every 2
        let result = '';
        const len = intPart.length;
        if (len <= 3) { result = intPart; }
        else {
            result = intPart.slice(-3);
            let remaining = intPart.slice(0, -3);
            while (remaining.length > 2) {
                result = remaining.slice(-2) + ',' + result;
                remaining = remaining.slice(0, -2);
            }
            if (remaining) result = remaining + ',' + result;
        }
        return sign + '₹' + result + '.' + dec;
    };
    const fmtNum = (n) => {
        const num = parseFloat(n) || 0;
        return num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    };
    const fmtDate = (d) => d ? dayjs(d).format('DD-MMM-YYYY') : '—';
    const fmtDateShort = (d) => d ? dayjs(d).format('DD/MM/YY') : '—';
    const fmtPct = (n) => (parseFloat(n) || 0).toFixed(2) + '%';

    const statusBadge = (s) => {
        const map = {
            ORDER_PLACED: ['Placed','badge-blue'], ORDER_CONFIRMED: ['Confirmed','badge-green'],
            ORDER_SHIPPED: ['Shipped','badge-orange'], ORDER_DELIVERED: ['Delivered','badge-green'],
            ORDER_CANCELLED: ['Cancelled','badge-red'], Issued: ['Issued','badge-purple']
        };
        const [label, cls] = map[s] || [s || 'Unknown', 'badge-gray'];
        return `<span class="badge ${cls}">${escapeHtml(label)}</span>`;
    };

    // ==================== FY HELPERS ====================
    const getFYOptions = () => {
        const now = new Date();
        const cy = now.getFullYear();
        const options = [];
        for (let y = cy - 3; y <= cy + 1; y++) {
            const label = `${y}-${(y + 1).toString().slice(2)}`;
            options.push({ value: label, label: `FY ${label}` });
        }
        return options;
    };
    const populateFYSelect = (selId) => {
        const sel = document.getElementById(selId);
        if (!sel || sel.options.length > 1) return;
        const opts = getFYOptions();
        opts.forEach(o => { const op = document.createElement('option'); op.value = o.value; op.textContent = o.label; sel.appendChild(op); });
    };
    const setDefaultDates = (fromId, toId) => {
        const now = new Date();
        const y = now.getFullYear();
        const m = now.getMonth();
        const fyStart = m >= 3 ? `${y}-04-01` : `${y - 1}-04-01`;
        const today = now.toISOString().slice(0, 10);
        const f = document.getElementById(fromId); if (f && !f.value) f.value = fyStart;
        const t = document.getElementById(toId); if (t && !t.value) t.value = today;
    };

    // ==================== USER HELPERS ====================
    const loadUsers = async () => {
        if (allUsers) return allUsers;
        try { allUsers = await callApi('GET', 'userdata'); return allUsers; }
        catch { allUsers = []; return []; }
    };
    const populatePartySelect = async () => {
        const sel = document.getElementById('pl-party');
        if (!sel || sel.options.length > 1) return;
        const users = await loadUsers();
        (users || []).forEach(u => {
            const o = document.createElement('option');
            o.value = u.uid; o.textContent = `${u.name || u.uid}${u.gstin ? ' (' + u.gstin + ')' : ''}`;
            sel.appendChild(o);
        });
    };

    // ==================== TAB MANAGEMENT ====================
    const switchTab = (tabName) => {
        document.querySelectorAll('#gst-tab-bar .gst-tab').forEach(b => b.classList.toggle('active', b.dataset.tab === tabName));
        document.querySelectorAll('.mod9-container .gst-section').forEach(s => s.classList.toggle('active', s.id === `sec-${tabName}`));
        // Auto-load on first visit
        if (tabName === 'dashboard' && !cache.dashboard) loadDashboard();
        if (tabName === 'sales-register' && !cache.salesRegister) { setDefaultDates('sr-from','sr-to'); loadSalesRegister(); }
        if (tabName === 'stock-summary' && !cache.stock) loadStockSummary();
        if (tabName === 'outstanding' && !cache.outstanding) loadOutstanding();
        if (tabName === 'vouchers' && !cache['voucher_' + currentVoucherType]) loadVouchers();
        if (tabName === 'gst-reports' && !cache['gst_' + currentGstReport]) loadGstReport();
        if (tabName === 'profit-loss' && !cache.pl) loadProfitLoss();
    };

    // ==================== 1. DASHBOARD ====================
    const loadDashboard = async () => {
        showLoading();
        try {
            const fy = document.getElementById('dash-fy')?.value || '';
            const q = fy ? `?fy=${fy}` : '';
            const [data, extras] = await Promise.all([
                callApi('GET', `gst/dashboard${q}`),
                callApi('GET', `gst/dashboard-extras${q}`)
            ]);
            cache.dashboard = data;
            renderDashboard(data, extras);
        } catch (e) { showToast('Dashboard load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderDashboard = (d, ex) => {
        const m = document.getElementById('dash-metrics');
        m.innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Total Revenue</div><div class="metric-value">${fmt(d.total_invoiced)}</div><div class="metric-sub">${d.invoice_count} invoices</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Taxable Value</div><div class="metric-value">${fmt(d.total_taxable)}</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">CGST Collected</div><div class="metric-value">${fmt(d.total_cgst)}</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">SGST Collected</div><div class="metric-value">${fmt(d.total_sgst)}</div></div>
            <div class="metric-card accent-red"><div class="metric-label">Net Tax Liability</div><div class="metric-value">${fmt(d.net_tax_liability)}</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Credit Notes</div><div class="metric-value">${fmt(d.credit_note_total)}</div><div class="metric-sub">${d.credit_note_count} notes</div></div>
            <div class="metric-card accent-cyan"><div class="metric-label">Debit Notes</div><div class="metric-value">${fmt(d.debit_note_total)}</div><div class="metric-sub">${d.debit_note_count} notes</div></div>
            <div class="metric-card accent-pink"><div class="metric-label">Total Tax</div><div class="metric-value">${fmt(d.total_tax)}</div></div>
        `;
        // Monthly revenue chart
        const monthKeys = Object.keys(d.monthly || {}).sort();
        renderChart('chart-monthly-revenue', 'bar', {
            labels: monthKeys.map(k => dayjs(k + '-01').format('MMM YY')),
            datasets: [{
                label: 'Revenue', data: monthKeys.map(k => d.monthly[k].total),
                backgroundColor: '#3b82f6', borderRadius: 4
            }, {
                label: 'Tax', data: monthKeys.map(k => (d.monthly[k].cgst || 0) + (d.monthly[k].sgst || 0)),
                backgroundColor: '#f59e0b', borderRadius: 4
            }]
        });
        // Top customers chart
        if (ex.top_customers?.length) {
            renderChart('chart-top-customers', 'bar', {
                labels: ex.top_customers.map(c => c.name || 'Unknown'),
                datasets: [{ label: 'Revenue', data: ex.top_customers.map(c => c.value),
                    backgroundColor: ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#ec4899','#14b8a6','#6366f1','#f97316'], borderRadius: 4 }]
            }, { indexAxis: 'y' });
        }
        // Status distribution
        if (ex.status_distribution) {
            const labels = Object.keys(ex.status_distribution);
            const values = labels.map(l => ex.status_distribution[l].count);
            renderChart('chart-status-dist', 'doughnut', {
                labels: labels.map(l => l.replace('ORDER_', '')),
                datasets: [{ data: values, backgroundColor: ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4'] }]
            });
        }
        // Recent activity
        const rl = document.getElementById('dash-recent-activity');
        if (ex.recent_activity?.length) {
            rl.innerHTML = ex.recent_activity.map(r => `
                <div class="recent-item">
                    <div class="ri-left"><span class="ri-party">${escapeHtml(r.party || 'Unknown')}</span><span class="ri-date">${fmtDate(r.date)} · #${r.order_id}</span></div>
                    <div><span class="ri-amount">${fmt(r.total)}</span> ${statusBadge(r.status)}</div>
                </div>`).join('');
        } else { rl.innerHTML = '<div class="empty-state">No recent activity</div>'; }
    };

    const renderChart = (canvasId, type, data, extraOpts = {}) => {
        if (chartInstances[canvasId]) chartInstances[canvasId].destroy();
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        chartInstances[canvasId] = new Chart(ctx, {
            type, data,
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: type === 'doughnut' ? 'right' : 'top', labels: { font: { size: 11 } } } },
                scales: type === 'doughnut' ? {} : { y: { beginAtZero: true, ticks: { font: { size: 11 } } }, x: { ticks: { font: { size: 11 } } } },
                ...extraOpts
            }
        });
    };

    // ==================== 2. DAY BOOK ====================
    const loadDayBook = async () => {
        const from = document.getElementById('db-from')?.value;
        const to = document.getElementById('db-to')?.value;
        if (!from || !to) { showToast('Select date range', 'error'); return; }
        showLoading();
        try {
            const data = await callApi('GET', `gst/day-book?from_date=${from}&to_date=${to}`);
            cache.dayBook = data;
            renderDayBook(data);
        } catch (e) { showToast('Day Book load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderDayBook = (data) => {
        const s = data.summary || {};
        document.getElementById('db-summary').innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Total Entries</div><div class="metric-value">${s.total_entries || 0}</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Sales</div><div class="metric-value">${fmt(s.total_sales)}</div><div class="metric-sub">${s.sales_count} invoices</div></div>
            <div class="metric-card accent-red"><div class="metric-label">Credit Notes</div><div class="metric-value">${fmt(s.total_cn)}</div><div class="metric-sub">${s.cn_count} notes</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">Debit Notes</div><div class="metric-value">${fmt(s.total_dn)}</div><div class="metric-sub">${s.dn_count} notes</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Net Amount</div><div class="metric-value">${fmt(s.net_amount)}</div></div>
        `;
        const tbody = document.querySelector('#db-table tbody');
        const entries = data.entries || [];
        let ttax = 0, ttaxable = 0, tamt = 0;
        tbody.innerHTML = entries.length ? entries.map(e => {
            ttaxable += e.taxable || 0; ttax += e.tax || 0; tamt += e.amount || 0;
            const typeClass = e.type === 'Sales' ? 'badge-blue' : e.type === 'Credit Note' ? 'badge-red' : 'badge-orange';
            return `<tr class="clickable" onclick="window._gst.viewInvoice('${e.order_id}')">
                <td>${fmtDate(e.date)}</td><td><span class="badge ${typeClass}">${e.type}</span></td>
                <td>${escapeHtml(e.voucher_no)}</td><td>${escapeHtml(e.party)}</td>
                <td class="num">${fmtNum(e.taxable)}</td><td class="num">${fmtNum(e.tax)}</td>
                <td class="num"><strong>${fmtNum(e.amount)}</strong></td><td>${statusBadge(e.status)}</td></tr>`;
        }).join('') : '<tr><td colspan="8" class="empty-state">No vouchers found for this period</td></tr>';
        document.getElementById('db-ft-taxable').textContent = fmtNum(ttaxable);
        document.getElementById('db-ft-tax').textContent = fmtNum(ttax);
        document.getElementById('db-ft-amount').textContent = fmtNum(tamt);
    };

    // ==================== 3. SALES REGISTER ====================
    const loadSalesRegister = async () => {
        const from = document.getElementById('sr-from')?.value;
        const to = document.getElementById('sr-to')?.value;
        if (!from || !to) { showToast('Select date range', 'error'); return; }
        showLoading();
        try {
            const raw = await callApi('GET', `gst/sales-register?from_date=${from}&to_date=${to}`);
            const data = { invoices: Array.isArray(raw) ? raw : (raw.invoices || []) };
            salesData = data; cache.salesRegister = data;
            renderSalesRegister(data);
        } catch (e) { showToast('Sales Register load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderSalesRegister = (data) => {
        const invoices = data.invoices || [];
        // Summary
        const totalAmt = invoices.reduce((s, i) => s + (i.total || 0), 0);
        const totalTaxable = invoices.reduce((s, i) => s + (i.taxable_value || 0), 0);
        const totalCgst = invoices.reduce((s, i) => s + (i.cgst || 0), 0);
        const totalSgst = invoices.reduce((s, i) => s + (i.sgst || 0), 0);
        const delivered = invoices.filter(i => i.order_status === 'ORDER_DELIVERED').length;
        document.getElementById('sr-summary').innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Invoices</div><div class="metric-value">${invoices.length}</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Total Revenue</div><div class="metric-value">${fmt(totalAmt)}</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">Total Tax</div><div class="metric-value">${fmt(totalCgst + totalSgst)}</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Delivered</div><div class="metric-value">${delivered}</div></div>
        `;
        const search = (document.getElementById('sr-search')?.value || '').toLowerCase();
        const filtered = search ? invoices.filter(i =>
            (i.invoice_no || '').toLowerCase().includes(search) ||
            (i.party_name || '').toLowerCase().includes(search) ||
            (i.gstin || '').toLowerCase().includes(search)
        ) : invoices;

        const tbody = document.querySelector('#sr-table tbody');
        let ftTax = 0, ftCgst = 0, ftSgst = 0, ftTotal = 0;
        tbody.innerHTML = filtered.length ? filtered.map(i => {
            ftTax += i.taxable_value || 0; ftCgst += i.cgst || 0; ftSgst += i.sgst || 0; ftTotal += i.total || 0;
            return `<tr class="clickable" onclick="window._gst.viewInvoice('${i.order_id}')">
                <td><strong>${escapeHtml(i.invoice_no || `INV-${i.order_id}`)}</strong></td>
                <td>${fmtDate(i.date)}</td><td>${escapeHtml(i.party_name || '—')}</td>
                <td>${escapeHtml(i.gstin || '—')}</td>
                <td class="num">${fmtNum(i.taxable_value)}</td><td class="num">${fmtNum(i.cgst)}</td>
                <td class="num">${fmtNum(i.sgst)}</td><td class="num"><strong>${fmtNum(i.total)}</strong></td>
                <td>${statusBadge(i.order_status)}</td>
                <td><button class="btn-sm btn-outline" onclick="event.stopPropagation();window._gst.viewInvoice('${i.order_id}')"><i data-feather="eye" class="btn-icon"></i></button></td></tr>`;
        }).join('') : '<tr><td colspan="10" class="empty-state">No invoices found</td></tr>';
        document.getElementById('sr-ft-taxable').textContent = fmtNum(ftTax);
        document.getElementById('sr-ft-cgst').textContent = fmtNum(ftCgst);
        document.getElementById('sr-ft-sgst').textContent = fmtNum(ftSgst);
        document.getElementById('sr-ft-total').textContent = fmtNum(ftTotal);
        feather.replace();
    };

    // ==================== 4. PROFIT & LOSS ====================
    const loadProfitLoss = async () => {
        showLoading();
        try {
            const fy = document.getElementById('pl-fy')?.value || '';
            const q = fy ? `?fy=${fy}` : '';
            const data = await callApi('GET', `gst/profit-loss${q}`);
            cache.pl = data;
            renderProfitLoss(data);
        } catch (e) { showToast('P&L load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderProfitLoss = (d) => {
        const r = d.revenue || {};
        const p = d.profitability || {};
        const t = d.tax_summary || {};
        const o = d.orders || {};
        document.getElementById('pl-period').textContent = `For Financial Year ${d.fy || 'Current'}`;
        const tbody = document.querySelector('#pl-table tbody');
        tbody.innerHTML = `
            <tr class="pl-header-row"><td colspan="2">REVENUE</td></tr>
            <tr class="pl-indent"><td>Gross Sales (MRP Value)</td><td class="num">${fmt(r.gross_sales_mrp)}</td></tr>
            <tr class="pl-indent"><td>Less: Discount Given</td><td class="num" style="color:#ef4444">(${fmt(r.discount_given)})</td></tr>
            <tr class="pl-subtotal"><td>Net Sales (Taxable Value)</td><td class="num">${fmt(r.net_sales_taxable)}</td></tr>
            <tr class="pl-indent"><td>Add: CGST Collected</td><td class="num">${fmt(r.cgst_collected)}</td></tr>
            <tr class="pl-indent"><td>Add: SGST Collected</td><td class="num">${fmt(r.sgst_collected)}</td></tr>
            <tr class="pl-subtotal"><td>Total Revenue (incl. Tax)</td><td class="num">${fmt(r.total_revenue)}</td></tr>
            <tr class="pl-indent"><td>Less: Credit Note Adjustments</td><td class="num" style="color:#ef4444">(${fmt(r.credit_note_adj)})</td></tr>
            <tr class="pl-indent"><td>Add: Debit Note Adjustments</td><td class="num">${fmt(r.debit_note_adj)}</td></tr>
            <tr class="pl-total"><td>NET REVENUE</td><td class="num">${fmt(r.net_revenue)}</td></tr>
            <tr><td colspan="2" style="padding:0.5rem"></td></tr>
            <tr class="pl-header-row"><td colspan="2">PROFITABILITY ANALYSIS</td></tr>
            <tr class="pl-indent"><td>Gross MRP Value of Goods Sold</td><td class="num">${fmt(p.gross_mrp_value)}</td></tr>
            <tr class="pl-indent"><td>Effective Selling Price</td><td class="num">${fmt(p.effective_selling_price)}</td></tr>
            <tr class="pl-indent"><td>Margin on MRP</td><td class="num">${fmt(p.margin_on_mrp)}</td></tr>
            <tr class="pl-subtotal"><td>Margin Percentage</td><td class="num">${fmtPct(p.margin_percentage)}</td></tr>
            <tr><td colspan="2" style="padding:0.5rem"></td></tr>
            <tr class="pl-header-row"><td colspan="2">TAX SUMMARY</td></tr>
            <tr class="pl-indent"><td>Output CGST</td><td class="num">${fmt(t.output_cgst)}</td></tr>
            <tr class="pl-indent"><td>Output SGST</td><td class="num">${fmt(t.output_sgst)}</td></tr>
            <tr class="pl-indent"><td>Total Output Tax</td><td class="num">${fmt(t.total_output_tax)}</td></tr>
            <tr class="pl-indent"><td>Less: Credit Note Tax Adj.</td><td class="num" style="color:#ef4444">(${fmt(t.cn_tax_adj)})</td></tr>
            <tr class="pl-indent"><td>Add: Debit Note Tax Adj.</td><td class="num">${fmt(t.dn_tax_adj)}</td></tr>
            <tr class="pl-total"><td>NET TAX PAYABLE</td><td class="num">${fmt(t.net_tax_payable)}</td></tr>
        `;
        // Orders summary
        document.getElementById('pl-orders-summary').innerHTML = `
            <div class="pl-os-row"><span class="pl-os-label">Total Orders</span><span class="pl-os-value">${o.total_orders}</span></div>
            <div class="pl-os-row"><span class="pl-os-label">Active Orders</span><span class="pl-os-value">${o.active_orders}</span></div>
            <div class="pl-os-row"><span class="pl-os-label">Cancelled Orders</span><span class="pl-os-value" style="color:#ef4444">${o.cancelled_orders}</span></div>
            <div class="pl-os-row"><span class="pl-os-label">Cancelled Value</span><span class="pl-os-value" style="color:#ef4444">${fmt(o.cancelled_value)}</span></div>
        `;
        // Monthly chart
        const monthly = d.monthly || {};
        const months = Object.keys(monthly).sort();
        renderChart('chart-pl-monthly', 'bar', {
            labels: months.map(m => dayjs(m + '-01').format('MMM YY')),
            datasets: [
                { label: 'Revenue', data: months.map(m => monthly[m].revenue), backgroundColor: '#3b82f6', borderRadius: 4 },
                { label: 'Tax', data: months.map(m => monthly[m].tax), backgroundColor: '#f59e0b', borderRadius: 4 },
                { label: 'Discount', data: months.map(m => monthly[m].discount), backgroundColor: '#ef4444', borderRadius: 4 }
            ]
        });
    };

    // ==================== 5. STOCK SUMMARY ====================
    const loadStockSummary = async () => {
        showLoading();
        try {
            const data = await callApi('GET', 'gst/stock-summary');
            cache.stock = data;
            renderStockSummary(data);
        } catch (e) { showToast('Stock load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderStockSummary = (data) => {
        const s = data.summary || {};
        document.getElementById('stock-summary-cards').innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Total Products</div><div class="metric-value">${s.total_products}</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Total Items</div><div class="metric-value">${s.total_items?.toLocaleString('en-IN')}</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Stock Value (Rate)</div><div class="metric-value">${fmt(s.total_stock_value)}</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">Stock Value (MRP)</div><div class="metric-value">${fmt(s.total_mrp_value)}</div></div>
            <div class="metric-card accent-red"><div class="metric-label">Out of Stock</div><div class="metric-value">${s.out_of_stock}</div></div>
            <div class="metric-card accent-cyan"><div class="metric-label">Low Stock (&lt;5)</div><div class="metric-value">${s.low_stock}</div></div>
        `;
        // Populate category filter
        const catFilter = document.getElementById('stock-cat-filter');
        if (catFilter && catFilter.options.length <= 1) {
            const cats = [...new Set((data.products || []).map(p => p.category))].sort();
            cats.forEach(c => { const o = document.createElement('option'); o.value = c; o.textContent = c; catFilter.appendChild(o); });
        }
        const search = (document.getElementById('stock-search')?.value || '').toLowerCase();
        const catVal = document.getElementById('stock-cat-filter')?.value || '';
        let products = data.products || [];
        if (search) products = products.filter(p => (p.name || '').toLowerCase().includes(search) || (p.hsn || '').includes(search));
        if (catVal) products = products.filter(p => p.category === catVal);

        const tbody = document.querySelector('#stock-table tbody');
        let totalQty = 0, totalVal = 0;
        tbody.innerHTML = products.length ? products.map(p => {
            totalQty += p.stock; totalVal += p.stock_value;
            const alert = p.stock === 0 ? '<span class="stock-alert out">OUT</span>'
                : p.stock < 5 ? '<span class="stock-alert low">LOW</span>'
                : '<span class="stock-alert ok">OK</span>';
            return `<tr><td><strong>${escapeHtml(p.name)}</strong></td><td>${escapeHtml(p.hsn || '—')}</td>
                <td>${escapeHtml(p.category)}</td><td class="num">${fmtNum(p.mrp)}</td><td class="num">${fmtNum(p.rate)}</td>
                <td>${p.gst_pct}%</td><td class="num"><strong>${p.stock}</strong></td>
                <td class="num">${fmtNum(p.stock_value)}</td><td>${alert}</td></tr>`;
        }).join('') : '<tr><td colspan="9" class="empty-state">No products found</td></tr>';
        document.getElementById('stock-ft-qty').textContent = totalQty.toLocaleString('en-IN');
        document.getElementById('stock-ft-val').textContent = fmtNum(totalVal);
    };

    // ==================== 6. OUTSTANDING ====================
    const loadOutstanding = async () => {
        showLoading();
        try {
            const data = await callApi('GET', 'gst/outstanding');
            cache.outstanding = data;
            renderOutstanding(data);
        } catch (e) { showToast('Outstanding load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderOutstanding = (data) => {
        const s = data.summary || {};
        document.getElementById('out-summary-cards').innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Total Parties</div><div class="metric-value">${s.total_parties}</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Total Credits</div><div class="metric-value">${fmt(s.total_credits)}</div></div>
            <div class="metric-card accent-red"><div class="metric-label">Outstanding Dues</div><div class="metric-value">${fmt(s.total_outstanding)}</div></div>
            <div class="metric-card accent-orange"><div class="metric-label">Parties with Credit</div><div class="metric-value">${s.parties_with_credit}</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Parties with Dues</div><div class="metric-value">${s.parties_with_dues}</div></div>
        `;
        const search = (document.getElementById('out-search')?.value || '').toLowerCase();
        const filter = document.getElementById('out-filter')?.value || 'all';
        let parties = data.parties || [];
        if (search) parties = parties.filter(p => (p.name || '').toLowerCase().includes(search) || (p.gstin || '').includes(search));
        if (filter === 'credit') parties = parties.filter(p => p.credits > 0);
        if (filter === 'dues') parties = parties.filter(p => p.credits < 0);

        const tbody = document.querySelector('#out-table tbody');
        tbody.innerHTML = parties.length ? parties.map(p => {
            const creditClass = p.credits > 0 ? 'color:#10b981' : p.credits < 0 ? 'color:#ef4444' : '';
            return `<tr><td><strong>${escapeHtml(p.name || p.uid)}</strong></td><td>${escapeHtml(p.gstin || '—')}</td>
                <td>${escapeHtml(p.contact || '—')}</td><td class="num">${fmtNum(p.total_purchased)}</td>
                <td class="num" style="${creditClass}"><strong>${fmtNum(p.credits)}</strong></td>
                <td class="num">${p.total_orders}</td><td>${fmtDate(p.last_order)}</td>
                <td><button class="btn-sm btn-outline" onclick="window._gst.viewPartyLedger('${p.uid}')"><i data-feather="book-open" class="btn-icon"></i>Ledger</button></td></tr>`;
        }).join('') : '<tr><td colspan="8" class="empty-state">No parties found</td></tr>';
        feather.replace();
    };

    const viewPartyLedger = (uid) => {
        document.getElementById('pl-party').value = uid;
        switchTab('party-ledger');
        loadPartyLedger();
    };

    // ==================== 7. VOUCHERS (CN/DN) ====================
    const switchVoucher = (type) => {
        currentVoucherType = type;
        document.querySelectorAll('[data-voucher]').forEach(b => b.classList.toggle('active', b.dataset.voucher === type));
        document.getElementById('voucher-form-title').textContent = type === 'credit' ? 'Create Credit Note' : 'Create Debit Note';
        if (!cache['voucher_' + type]) loadVouchers();
        else renderVouchers(cache['voucher_' + type]);
    };

    const loadVouchers = async () => {
        showLoading();
        try {
            const endpoint = currentVoucherType === 'credit' ? 'gst/credit-notes' : 'gst/debit-notes';
            const raw = await callApi('GET', endpoint);
            const key = currentVoucherType === 'credit' ? 'credit_notes' : 'debit_notes';
            const data = Array.isArray(raw) ? { [key]: raw } : raw;
            cache['voucher_' + currentVoucherType] = data;
            renderVouchers(data);
        } catch (e) { showToast('Load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderVouchers = (data) => {
        const notes = (currentVoucherType === 'credit' ? data.credit_notes : data.debit_notes) || [];
        const prefix = currentVoucherType === 'credit' ? 'cn' : 'dn';
        const tbody = document.querySelector('#voucher-table tbody');
        let fSub = 0, fCgst = 0, fSgst = 0, fTotal = 0;
        tbody.innerHTML = notes.length ? notes.map(n => {
            const id = n[prefix + '_id'];
            fSub += n.subtotal || 0; fCgst += n.cgst_total || 0; fSgst += n.sgst_total || 0; fTotal += n.total || 0;
            return `<tr><td><strong>${escapeHtml(n[prefix + '_number'] || '')}</strong></td>
                <td>${fmtDate(n.created_at)}</td><td>${escapeHtml(n.original_invoice || '')}</td>
                <td>${escapeHtml(n.user_name || n.user_id || '')}</td><td>${escapeHtml(n.reason || '')}</td>
                <td class="num">${fmtNum(n.subtotal)}</td><td class="num">${fmtNum(n.cgst_total)}</td>
                <td class="num">${fmtNum(n.sgst_total)}</td><td class="num"><strong>${fmtNum(n.total)}</strong></td>
                <td><button class="btn-sm btn-danger" onclick="window._gst.deleteVoucher('${id}')"><i data-feather="trash-2" class="btn-icon"></i></button></td></tr>`;
        }).join('') : `<tr><td colspan="10" class="empty-state">No ${currentVoucherType} notes found</td></tr>`;
        document.getElementById('vt-ft-sub').textContent = fmtNum(fSub);
        document.getElementById('vt-ft-cgst').textContent = fmtNum(fCgst);
        document.getElementById('vt-ft-sgst').textContent = fmtNum(fSgst);
        document.getElementById('vt-ft-total').textContent = fmtNum(fTotal);
        feather.replace();
    };

    const toggleVoucherForm = () => {
        const form = document.getElementById('voucher-form');
        form.style.display = form.style.display === 'none' ? 'block' : 'none';
        if (form.style.display === 'block' && document.getElementById('v-items-container').children.length === 0) addVoucherItem();
    };

    const addVoucherItem = () => {
        const container = document.getElementById('v-items-container');
        const idx = container.children.length;
        const row = document.createElement('div');
        row.className = 'item-row';
        row.innerHTML = `
            <div class="form-group"><label>Description</label><input type="text" class="vi-desc" placeholder="Item name"></div>
            <div class="form-group"><label>HSN</label><input type="text" class="vi-hsn" placeholder="HSN"></div>
            <div class="form-group"><label>Qty</label><input type="number" class="vi-qty" value="1" min="1" onchange="window._gst.calcVoucherTotals()"></div>
            <div class="form-group"><label>Rate (₹)</label><input type="number" class="vi-rate" value="0" step="0.01" onchange="window._gst.calcVoucherTotals()"></div>
            <div class="form-group"><label>GST %</label><input type="number" class="vi-gst" value="18" step="0.01" onchange="window._gst.calcVoucherTotals()"></div>
            <div class="form-group"><label>Amount</label><input type="text" class="vi-amount" readonly></div>
            <button class="btn-sm btn-danger" onclick="this.parentElement.remove();window._gst.calcVoucherTotals()" style="margin-bottom:4px"><i data-feather="x" class="btn-icon"></i></button>
        `;
        container.appendChild(row);
        feather.replace();
        calcVoucherTotals();
    };

    const calcVoucherTotals = () => {
        let subtotal = 0, cgst = 0, sgst = 0;
        document.querySelectorAll('#v-items-container .item-row').forEach(row => {
            const qty = parseFloat(row.querySelector('.vi-qty')?.value) || 0;
            const rate = parseFloat(row.querySelector('.vi-rate')?.value) || 0;
            const gstPct = parseFloat(row.querySelector('.vi-gst')?.value) || 0;
            const amt = qty * rate;
            const c = amt * (gstPct / 2) / 100;
            const s = c;
            subtotal += amt; cgst += c; sgst += s;
            const amtInput = row.querySelector('.vi-amount');
            if (amtInput) amtInput.value = fmtNum(amt + c + s);
        });
        document.getElementById('v-subtotal').textContent = fmt(subtotal);
        document.getElementById('v-cgst').textContent = fmt(cgst);
        document.getElementById('v-sgst').textContent = fmt(sgst);
        document.getElementById('v-total').textContent = fmt(subtotal + cgst + sgst);
    };

    const submitVoucher = async () => {
        const number = document.getElementById('v-number')?.value?.trim();
        const originalInvoice = document.getElementById('v-original-invoice')?.value?.trim();
        const userId = document.getElementById('v-user-id')?.value?.trim();
        const userName = document.getElementById('v-user-name')?.value?.trim();
        const userGstin = document.getElementById('v-user-gstin')?.value?.trim();
        const reason = document.getElementById('v-reason')?.value?.trim();
        const notes = document.getElementById('v-notes')?.value?.trim();
        if (!originalInvoice || !userId || !reason) {
            showToast('Fill all required fields (marked with *)', 'error'); return;
        }
        const items = [];
        let subtotal = 0, cgstTotal = 0, sgstTotal = 0;
        document.querySelectorAll('#v-items-container .item-row').forEach(row => {
            const desc = row.querySelector('.vi-desc')?.value?.trim() || '';
            const hsn = row.querySelector('.vi-hsn')?.value?.trim() || '';
            const qty = parseFloat(row.querySelector('.vi-qty')?.value) || 0;
            const rate = parseFloat(row.querySelector('.vi-rate')?.value) || 0;
            const gstPct = parseFloat(row.querySelector('.vi-gst')?.value) || 0;
            const amt = qty * rate;
            const c = amt * (gstPct / 2) / 100; const s = c;
            subtotal += amt; cgstTotal += c; sgstTotal += s;
            items.push({ description: desc, hsn, qty, rate, gst_rate: gstPct, amount: Math.round(amt * 100) / 100 });
        });
        if (items.length === 0) { showToast('Add at least one item', 'error'); return; }
        const prefix = currentVoucherType === 'credit' ? 'cn' : 'dn';
        const body = {};
        body[prefix + '_number'] = number;
        body.original_invoice = originalInvoice;
        body.user_id = userId;
        body.user_name = userName;
        body.user_gstin = userGstin;
        body.reason = reason;
        body.items = items;
        body.subtotal = Math.round(subtotal * 100) / 100;
        body.cgst_total = Math.round(cgstTotal * 100) / 100;
        body.sgst_total = Math.round(sgstTotal * 100) / 100;
        body.total = Math.round((subtotal + cgstTotal + sgstTotal) * 100) / 100;
        body.notes = notes;

        showLoading();
        try {
            const endpoint = currentVoucherType === 'credit' ? 'gst/credit-notes' : 'gst/debit-notes';
            await callApi('POST', endpoint, body);
            showToast(`${currentVoucherType === 'credit' ? 'Credit' : 'Debit'} note created`, 'success');
            toggleVoucherForm();
            // Reset form
            ['v-number','v-original-invoice','v-user-id','v-user-name','v-user-gstin','v-reason','v-notes'].forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
            document.getElementById('v-items-container').innerHTML = '';
            // Invalidate caches
            cache['voucher_' + currentVoucherType] = null;
            cache.dashboard = null; cache.pl = null;
            cache['gst_gstr1'] = null; cache['gst_gstr3b'] = null;
            loadVouchers();
        } catch (e) { showToast('Submit failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const deleteVoucher = async (id) => {
        showDialog('info', 'Delete', 'Cancel', 'Are you sure you want to delete this voucher?', async (action) => {
            if (action !== 'Delete') return;
            showLoading();
            try {
                const endpoint = currentVoucherType === 'credit' ? `gst/credit-notes/${id}` : `gst/debit-notes/${id}`;
                await callApi('DELETE', endpoint);
                showToast('Voucher deleted', 'success');
                cache['voucher_' + currentVoucherType] = null;
                cache.dashboard = null; cache.pl = null;
                cache['gst_gstr1'] = null; cache['gst_gstr3b'] = null;
                loadVouchers();
            } catch (e) { showToast('Delete failed: ' + e.message, 'error'); }
            finally { hideLoading(); }
        });
    };

    // ==================== 8. GST REPORTS ====================
    const switchGstReport = (report) => {
        currentGstReport = report;
        document.querySelectorAll('[data-gst-sub]').forEach(b => b.classList.toggle('active', b.dataset.gstSub === report));
        document.querySelectorAll('.gst-sub-section').forEach(s => s.classList.toggle('active', s.id === `sub-${report}`));
        if (!cache['gst_' + report]) loadGstReport();
    };

    const loadGstReport = async () => {
        if (currentGstReport === 'taxledger') { loadTaxLedger(); return; }
        if (cache['gst_' + currentGstReport]) return;
        showLoading();
        try {
            if (currentGstReport === 'gstr1' || currentGstReport === 'gstr3b' || currentGstReport === 'hsn') {
                if (!salesData) {
                    const now = new Date();
                    const fyFrom = (now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1) + '-04-01';
                    const fyTo = now.toISOString().slice(0, 10);
                    const raw = await callApi('GET', `gst/sales-register?from_date=${fyFrom}&to_date=${fyTo}`);
                    salesData = { invoices: Array.isArray(raw) ? raw : (raw.invoices || []) };
                }
                let cnData = cache['voucher_credit'];
                if (!cnData) {
                    const cnRaw = await callApi('GET', 'gst/credit-notes');
                    cnData = Array.isArray(cnRaw) ? { credit_notes: cnRaw } : cnRaw;
                    cache['voucher_credit'] = cnData;
                }
                let dnData = cache['voucher_debit'];
                if (!dnData) {
                    const dnRaw = await callApi('GET', 'gst/debit-notes');
                    dnData = Array.isArray(dnRaw) ? { debit_notes: dnRaw } : dnRaw;
                    cache['voucher_debit'] = dnData;
                }
                if (currentGstReport === 'gstr1') renderGSTR1(salesData, cnData, dnData);
                if (currentGstReport === 'gstr3b') renderGSTR3B(salesData, cnData, dnData);
                if (currentGstReport === 'hsn') renderHSN(salesData);
                cache['gst_' + currentGstReport] = true;
            }
        } catch (e) { showToast('GST report load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    // --- GSTR-1 ---
    const renderGSTR1 = (sales, cnData, dnData) => {
        const invoices = sales.invoices || [];
        const b2b = invoices.filter(i => i.gstin);
        const b2c = invoices.filter(i => !i.gstin);
        const creditNotes = cnData.credit_notes || [];
        const debitNotes = dnData.debit_notes || [];

        document.querySelector('#gstr1-b2b tbody').innerHTML = b2b.length ? b2b.map(i =>
            `<tr><td>${escapeHtml(i.gstin)}</td><td>${escapeHtml(i.party_name || '—')}</td>
            <td>${escapeHtml(i.invoice_no || '')}</td><td>${fmtDate(i.date)}</td>
            <td class="num">${fmtNum(i.taxable_value)}</td><td class="num">${fmtNum(i.cgst)}</td>
            <td class="num">${fmtNum(i.sgst)}</td><td class="num">${fmtNum(i.total)}</td></tr>`
        ).join('') : '<tr><td colspan="8" class="empty-state">No B2B invoices</td></tr>';

        document.querySelector('#gstr1-b2c tbody').innerHTML = b2c.length ? b2c.map(i =>
            `<tr><td>${escapeHtml(i.invoice_no || '')}</td><td>${fmtDate(i.date)}</td>
            <td>${escapeHtml(i.party_name || '—')}</td>
            <td class="num">${fmtNum(i.taxable_value)}</td><td class="num">${fmtNum(i.cgst)}</td>
            <td class="num">${fmtNum(i.sgst)}</td><td class="num">${fmtNum(i.total)}</td></tr>`
        ).join('') : '<tr><td colspan="7" class="empty-state">No B2C invoices</td></tr>';

        document.querySelector('#gstr1-cn tbody').innerHTML = creditNotes.length ? creditNotes.map(n =>
            `<tr><td>${escapeHtml(n.cn_number || '')}</td><td>${fmtDate(n.created_at)}</td>
            <td>${escapeHtml(n.original_invoice || '')}</td><td>${escapeHtml(n.user_name || '')}</td>
            <td class="num">${fmtNum(n.subtotal)}</td><td class="num">${fmtNum((n.cgst_total||0)+(n.sgst_total||0))}</td>
            <td class="num">${fmtNum(n.total)}</td></tr>`
        ).join('') : '<tr><td colspan="7" class="empty-state">No credit notes</td></tr>';

        document.querySelector('#gstr1-dn tbody').innerHTML = debitNotes.length ? debitNotes.map(n =>
            `<tr><td>${escapeHtml(n.dn_number || '')}</td><td>${fmtDate(n.created_at)}</td>
            <td>${escapeHtml(n.original_invoice || '')}</td><td>${escapeHtml(n.user_name || '')}</td>
            <td class="num">${fmtNum(n.subtotal)}</td><td class="num">${fmtNum((n.cgst_total||0)+(n.sgst_total||0))}</td>
            <td class="num">${fmtNum(n.total)}</td></tr>`
        ).join('') : '<tr><td colspan="7" class="empty-state">No debit notes</td></tr>';
    };

    // --- GSTR-3B ---
    const renderGSTR3B = (sales, cnData, dnData) => {
        const invoices = sales.invoices || [];
        const tTaxable = invoices.reduce((s, i) => s + (i.taxable_value || 0), 0);
        const tCgst = invoices.reduce((s, i) => s + (i.cgst || 0), 0);
        const tSgst = invoices.reduce((s, i) => s + (i.sgst || 0), 0);
        const creditNotes = cnData.credit_notes || [];
        const debitNotes = dnData.debit_notes || [];
        const cnSub = creditNotes.reduce((s, n) => s + (n.subtotal || 0), 0);
        const cnCgst = creditNotes.reduce((s, n) => s + (n.cgst_total || 0), 0);
        const cnSgst = creditNotes.reduce((s, n) => s + (n.sgst_total || 0), 0);
        const dnSub = debitNotes.reduce((s, n) => s + (n.subtotal || 0), 0);
        const dnCgst = debitNotes.reduce((s, n) => s + (n.cgst_total || 0), 0);
        const dnSgst = debitNotes.reduce((s, n) => s + (n.sgst_total || 0), 0);

        const tbody = document.querySelector('#gstr3b-table tbody');
        tbody.innerHTML = `
            <tr><td><strong>3.1 — Outward Supplies (Taxable)</strong></td><td class="num">${fmtNum(tTaxable)}</td><td class="num">0.00</td><td class="num">${fmtNum(tCgst)}</td><td class="num">${fmtNum(tSgst)}</td><td class="num">0.00</td></tr>
            <tr><td>&nbsp;&nbsp;&nbsp;(a) Outward taxable supplies (excl. zero/nil/exempt)</td><td class="num">${fmtNum(tTaxable)}</td><td class="num">0.00</td><td class="num">${fmtNum(tCgst)}</td><td class="num">${fmtNum(tSgst)}</td><td class="num">0.00</td></tr>
            <tr><td>&nbsp;&nbsp;&nbsp;(b) Outward taxable supplies (zero rated)</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td></tr>
            <tr><td>&nbsp;&nbsp;&nbsp;(c) Exempt and Nil rated</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td></tr>
            <tr><td><strong>4 — Eligible ITC</strong></td><td class="num">—</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td><td class="num">0.00</td></tr>
            <tr><td><strong>5 — Tax Payable & Paid</strong></td><td class="num"></td><td class="num">0.00</td><td class="num">${fmtNum(tCgst - cnCgst + dnCgst)}</td><td class="num">${fmtNum(tSgst - cnSgst + dnSgst)}</td><td class="num">0.00</td></tr>
            <tr style="background:#eff6ff"><td><strong>Credit Note Adjustments</strong></td><td class="num">${fmtNum(cnSub)}</td><td class="num">0.00</td><td class="num">${fmtNum(cnCgst)}</td><td class="num">${fmtNum(cnSgst)}</td><td class="num">0.00</td></tr>
            <tr style="background:#fff7ed"><td><strong>Debit Note Adjustments</strong></td><td class="num">${fmtNum(dnSub)}</td><td class="num">0.00</td><td class="num">${fmtNum(dnCgst)}</td><td class="num">${fmtNum(dnSgst)}</td><td class="num">0.00</td></tr>
            <tr style="background:#f0fdf4;font-weight:700"><td>NET TAX LIABILITY</td><td class="num">${fmtNum(tTaxable - cnSub + dnSub)}</td><td class="num">0.00</td><td class="num">${fmtNum(tCgst - cnCgst + dnCgst)}</td><td class="num">${fmtNum(tSgst - cnSgst + dnSgst)}</td><td class="num">0.00</td></tr>
        `;
    };

    // --- HSN Summary ---
    const renderHSN = (sales) => {
        const invoices = sales.invoices || [];
        const hsnMap = {};
        invoices.forEach(inv => {
            (inv.gst_details || []).forEach(g => {
                const hsn = g.hsnSac || 'N/A';
                if (!hsnMap[hsn]) hsnMap[hsn] = { taxable: 0, cgst: 0, sgst: 0, totalTax: 0, invoiceVal: 0 };
                hsnMap[hsn].taxable += parseFloat(g.taxableValue || 0);
                hsnMap[hsn].cgst += parseFloat(g.cgstAmount || 0);
                hsnMap[hsn].sgst += parseFloat(g.sgstUtgstAmount || 0);
                hsnMap[hsn].totalTax += parseFloat(g.totalTaxAmount || 0);
                hsnMap[hsn].invoiceVal += parseFloat(g.taxableValue || 0) + parseFloat(g.totalTaxAmount || 0);
            });
        });
        const tbody = document.querySelector('#hsn-table tbody');
        const entries = Object.entries(hsnMap).sort((a, b) => b[1].taxable - a[1].taxable);
        tbody.innerHTML = entries.length ? entries.map(([hsn, v]) =>
            `<tr><td><strong>${escapeHtml(hsn)}</strong></td><td class="num">${fmtNum(v.taxable)}</td>
            <td class="num">${fmtNum(v.cgst)}</td><td class="num">${fmtNum(v.sgst)}</td>
            <td class="num">${fmtNum(v.totalTax)}</td><td class="num">${fmtNum(v.invoiceVal)}</td></tr>`
        ).join('') : '<tr><td colspan="6" class="empty-state">No HSN data</td></tr>';
    };

    // --- Tax Ledger ---
    const loadTaxLedger = async () => {
        const from = document.getElementById('tl-from')?.value;
        const to = document.getElementById('tl-to')?.value;
        if (!from || !to) { showToast('Select date range for Tax Ledger', 'error'); return; }
        showLoading();
        try {
            const data = await callApi('GET', `gst/tax-ledger?from_date=${from}&to_date=${to}`);
            cache['gst_taxledger'] = data;
            renderTaxLedger(data);
        } catch (e) { showToast('Tax Ledger load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderTaxLedger = (data) => {
        // Rate summary
        const rateEntries = Object.entries(data.rate_summary || {}).sort((a, b) => a[1].rate - b[1].rate);
        document.querySelector('#tl-rate-table tbody').innerHTML = rateEntries.length ? rateEntries.map(([k, v]) =>
            `<tr><td><strong>${k}</strong></td><td class="num">${v.invoices}</td>
            <td class="num">${fmtNum(v.taxable)}</td><td class="num">${fmtNum(v.cgst)}</td>
            <td class="num">${fmtNum(v.sgst)}</td><td class="num"><strong>${fmtNum(v.total_tax)}</strong></td></tr>`
        ).join('') + `<tr style="background:#f0fdf4;font-weight:700"><td>TOTAL</td><td class="num">${rateEntries.reduce((s,e)=>s+e[1].invoices,0)}</td>
            <td class="num">${fmtNum(data.totals?.total_taxable)}</td><td class="num">${fmtNum(rateEntries.reduce((s,e)=>s+e[1].cgst,0))}</td>
            <td class="num">${fmtNum(rateEntries.reduce((s,e)=>s+e[1].sgst,0))}</td><td class="num">${fmtNum(data.totals?.total_tax)}</td></tr>`
            : '<tr><td colspan="6" class="empty-state">No tax data</td></tr>';
        // Detail entries
        const entries = data.entries || [];
        document.querySelector('#tl-detail-table tbody').innerHTML = entries.length ? entries.map(e =>
            `<tr><td>${fmtDate(e.date)}</td><td>${escapeHtml(e.invoice)}</td><td>${escapeHtml(e.party)}</td>
            <td>${escapeHtml(e.hsn)}</td><td>${e.rate}%</td><td class="num">${fmtNum(e.taxable)}</td>
            <td class="num">${fmtNum(e.cgst)}</td><td class="num">${fmtNum(e.sgst)}</td>
            <td class="num">${fmtNum(e.total_tax)}</td></tr>`
        ).join('') : '<tr><td colspan="9" class="empty-state">No entries</td></tr>';
    };

    // ==================== 9. PARTY LEDGER ====================
    const loadPartyLedger = async () => {
        const uid = document.getElementById('pl-party')?.value;
        if (!uid) { showToast('Select a party', 'error'); return; }
        const from = document.getElementById('pl-from')?.value;
        const to = document.getElementById('pl-to')?.value;
        let q = `?user_id=${uid}`;
        if (from) q += `&from_date=${from}`;
        if (to) q += `&to_date=${to}`;
        showLoading();
        try {
            const data = await callApi('GET', `gst/party-ledger${q}`);
            renderPartyLedger(data);
        } catch (e) { showToast('Party Ledger load failed: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const renderPartyLedger = (data) => {
        const entries = data.entries || [];
        const totalDebit = data.total_debit || 0;
        const totalCredit = data.total_credit || 0;
        const netBal = data.net_balance || 0;

        document.getElementById('pl-summary-cards').innerHTML = `
            <div class="metric-card accent-blue"><div class="metric-label">Total Debit</div><div class="metric-value">${fmt(totalDebit)}</div></div>
            <div class="metric-card accent-green"><div class="metric-label">Total Credit</div><div class="metric-value">${fmt(totalCredit)}</div></div>
            <div class="metric-card ${netBal >= 0 ? 'accent-green' : 'accent-red'}"><div class="metric-label">Net Balance</div><div class="metric-value">${fmt(netBal)}</div></div>
            <div class="metric-card accent-purple"><div class="metric-label">Transactions</div><div class="metric-value">${entries.length}</div></div>
        `;
        const tbody = document.querySelector('#pl-table2 tbody');
        let runBal = 0;
        tbody.innerHTML = entries.length ? entries.map(e => {
            const debit = e.debit || 0;
            const credit = e.credit || 0;
            runBal += debit - credit;
            const orderId = (e.voucher_no || '').replace(/^INV-/, '') || '';
            return `<tr class="clickable" onclick="window._gst.viewInvoice('${orderId}')">
                <td>${fmtDate(e.date)}</td><td>${escapeHtml(e.type)}</td><td>${escapeHtml(e.voucher_no)}</td>
                <td class="num">${debit ? fmtNum(debit) : '—'}</td><td class="num">${credit ? fmtNum(credit) : '—'}</td>
                <td class="num" style="${runBal >= 0 ? 'color:#10b981' : 'color:#ef4444'}"><strong>${fmtNum(runBal)}</strong></td></tr>`;
        }).join('') : '<tr><td colspan="6" class="empty-state">No transactions found</td></tr>';
        document.getElementById('pl-ft-debit').textContent = fmtNum(totalDebit);
        document.getElementById('pl-ft-credit').textContent = fmtNum(totalCredit);
        document.getElementById('pl-ft-bal').textContent = fmtNum(netBal);
    };

    // ==================== INVOICE VIEWER ====================
    const viewInvoice = async (orderId) => {
        if (!orderId) return;
        showLoading();
        try {
            const data = await callApi('GET', `bills/${orderId}`);
            const bill = data.bill || data;
            if (!bill || !bill.company) { showToast('Invoice data not found', 'error'); return; }
            const body = document.getElementById('invoice-modal-body');
            const items = bill.items || [];
            const totals = bill.totals || {};
            const gstDets = bill.gstDetails || [];
            body.innerHTML = `
                <div class="inv-company">
                    <h2>${escapeHtml(bill.company?.name || '')}</h2>
                    <p>${escapeHtml(bill.company?.address || '')}</p>
                    <p>GSTIN: ${escapeHtml(bill.company?.gstNo || '')} | Email: ${escapeHtml(bill.company?.email || '')}</p>
                </div>
                <div style="text-align:center;font-weight:700;margin:0.5rem 0;font-size:14px">TAX INVOICE</div>
                <div class="inv-details-grid">
                    <div class="detail-block">
                        <h4>Invoice Details</h4>
                        <p><strong>Invoice No:</strong> ${escapeHtml(bill.details?.invoiceNo || '')}</p>
                        <p><strong>Date:</strong> ${escapeHtml(bill.details?.dated || '')}</p>
                        <p><strong>Delivery Note:</strong> ${escapeHtml(bill.details?.deliveryNote || '')}</p>
                    </div>
                    <div class="detail-block">
                        <h4>Buyer</h4>
                        <p><strong>${escapeHtml(bill.buyer?.name || '')}</strong></p>
                        <p>${escapeHtml(bill.buyer?.address || '')}</p>
                        <p>GSTIN: ${escapeHtml(bill.buyer?.gstin || 'N/A')} | Ph: ${escapeHtml(bill.buyer?.contactNo || '')}</p>
                    </div>
                </div>
                ${bill.consignee?.name ? `<div class="detail-block" style="margin-bottom:1rem"><h4>Consignee</h4><p>${escapeHtml(bill.consignee.name)} — ${escapeHtml(bill.consignee.address || '')}</p></div>` : ''}
                <table class="inv-items-table">
                    <thead><tr><th>#</th><th>Description</th><th>HSN/SAC</th><th>Qty</th><th>MRP</th><th>Disc</th><th>Rate</th><th>Amount</th></tr></thead>
                    <tbody>${items.map(it => `<tr><td>${it.sNo}</td><td>${escapeHtml(it.description || '')}</td>
                        <td>${escapeHtml(it.hsnSac || '')}</td><td>${it.quantityBilled}</td>
                        <td style="text-align:right">${fmtNum(it.mrp)}</td><td>${it.discount}</td>
                        <td style="text-align:right">${fmtNum(it.rate)}</td><td style="text-align:right">${fmtNum(it.amount)}</td></tr>`).join('')}
                    </tbody>
                    <tfoot>
                        <tr><td colspan="7" style="text-align:right">Sub Total</td><td style="text-align:right">${fmtNum(totals.subTotal)}</td></tr>
                        <tr><td colspan="7" style="text-align:right">CGST</td><td style="text-align:right">${fmtNum(totals.cgstAmount)}</td></tr>
                        <tr><td colspan="7" style="text-align:right">SGST</td><td style="text-align:right">${fmtNum(totals.sgstAmount)}</td></tr>
                        ${totals.specialDiscount ? `<tr><td colspan="7" style="text-align:right">Special Discount</td><td style="text-align:right">${fmtNum(totals.specialDiscount)}</td></tr>` : ''}
                        ${totals.roundOff ? `<tr><td colspan="7" style="text-align:right">Round Off</td><td style="text-align:right">${fmtNum(totals.roundOff)}</td></tr>` : ''}
                        <tr style="font-size:14px"><td colspan="7" style="text-align:right"><strong>GRAND TOTAL</strong></td><td style="text-align:right"><strong>${fmtNum(totals.total)}</strong></td></tr>
                    </tfoot>
                </table>
                ${gstDets.length ? `<table class="inv-gst-table">
                    <thead><tr><th>HSN/SAC</th><th>Taxable Value</th><th>CGST Rate</th><th>CGST Amt</th><th>SGST Rate</th><th>SGST Amt</th><th>Total Tax</th></tr></thead>
                    <tbody>${gstDets.map(g => `<tr><td>${escapeHtml(g.hsnSac || '')}</td>
                        <td style="text-align:right">${fmtNum(g.taxableValue)}</td><td>${g.cgstRate}</td>
                        <td style="text-align:right">${fmtNum(g.cgstAmount)}</td><td>${g.sgstUtgstRate}</td>
                        <td style="text-align:right">${fmtNum(g.sgstUtgstAmount)}</td>
                        <td style="text-align:right">${fmtNum(g.totalTaxAmount)}</td></tr>`).join('')}</tbody></table>` : ''}
                ${bill.amountsInWords ? `<div class="inv-amounts-words">
                    <p><strong>Amount Chargeable (in words):</strong> ${escapeHtml(bill.amountsInWords.amountChargeable || '')}</p>
                    <p><strong>Tax Amount (in words):</strong> ${escapeHtml(bill.amountsInWords.taxAmount || '')}</p></div>` : ''}
            `;
            document.getElementById('invoice-modal-overlay').classList.add('active');
        } catch (e) { showToast('Could not load invoice: ' + e.message, 'error'); }
        finally { hideLoading(); }
    };

    const closeInvoice = () => { document.getElementById('invoice-modal-overlay').classList.remove('active'); };
    const printInvoice = () => { window.print(); };
    const printSection = (id) => {
        const el = document.getElementById(id);
        if (!el) return;
        const w = window.open('', '_blank');
        w.document.write(`<html><head><title>Print</title><style>
            body{font-family:system-ui,sans-serif;padding:2rem;font-size:13px}
            table{width:100%;border-collapse:collapse;margin:1rem 0}
            th,td{padding:6px 8px;border:1px solid #ddd;text-align:left}
            th{background:#f1f5f9;font-weight:600}
            .num{text-align:right}
            .pl-header-row td{background:#f8fafc;font-weight:700}
            .pl-subtotal td{background:#eff6ff;font-weight:600}
            .pl-total td{background:#dbeafe;font-weight:700}
            .pl-indent td:first-child{padding-left:2rem}
        </style></head><body>${el.innerHTML}</body></html>`);
        w.document.close(); w.print();
    };

    // ==================== CSV EXPORT ====================
    const exportCSV = (type) => {
        let rows = [];
        let filename = type;
        if (type === 'day-book' && cache.dayBook) {
            rows = [['Date','Type','Voucher No','Party','Taxable','Tax','Amount','Status']];
            (cache.dayBook.entries || []).forEach(e => rows.push([fmtDate(e.date), e.type, e.voucher_no, e.party, e.taxable, e.tax, e.amount, e.status]));
        } else if (type === 'sales-register' && salesData) {
            rows = [['Invoice #','Date','Party','GSTIN','Taxable','CGST','SGST','Total','Status']];
            (salesData.invoices || []).forEach(i => rows.push([i.invoice_no, fmtDate(i.date), i.party_name, i.gstin, i.taxable_value, i.cgst, i.sgst, i.total, i.order_status]));
        } else if (type === 'stock' && cache.stock) {
            rows = [['Product','HSN','Category','MRP','Rate','GST%','Stock','Stock Value']];
            (cache.stock.products || []).forEach(p => rows.push([p.name, p.hsn, p.category, p.mrp, p.rate, p.gst_pct, p.stock, p.stock_value]));
        } else if (type === 'outstanding' && cache.outstanding) {
            rows = [['Party','GSTIN','Contact','Total Purchased','Credits','Orders','Last Order']];
            (cache.outstanding.parties || []).forEach(p => rows.push([p.name, p.gstin, p.contact, p.total_purchased, p.credits, p.total_orders, fmtDate(p.last_order)]));
        } else if (type === 'vouchers') {
            const data = cache['voucher_' + currentVoucherType];
            if (!data) { showToast('No data to export', 'error'); return; }
            const notes = (currentVoucherType === 'credit' ? data.credit_notes : data.debit_notes) || [];
            const pfx = currentVoucherType === 'credit' ? 'cn' : 'dn';
            rows = [['Note #','Date','Original Invoice','Party','Reason','Subtotal','CGST','SGST','Total']];
            notes.forEach(n => rows.push([n[pfx + '_number'], fmtDate(n.created_at), n.original_invoice, n.user_name, n.reason, n.subtotal, n.cgst_total, n.sgst_total, n.total]));
            filename = currentVoucherType + '-notes';
        } else if (type === 'party-ledger') {
            const table = document.querySelector('#pl-table2 tbody');
            if (!table || !table.rows.length) { showToast('No data to export', 'error'); return; }
            rows = [['Date','Voucher Type','Voucher #','Debit','Credit','Balance']];
            Array.from(table.rows).forEach(tr => { if (tr.cells.length >= 6) rows.push(Array.from(tr.cells).map(c => c.textContent.trim())); });
        } else if (type === 'gst-report') {
            // Export current visible GST report
            const activeSection = document.querySelector('.gst-sub-section.active');
            if (!activeSection) { showToast('No data to export', 'error'); return; }
            const tables = activeSection.querySelectorAll('table');
            tables.forEach(table => {
                Array.from(table.rows).forEach(tr => rows.push(Array.from(tr.cells).map(c => c.textContent.trim())));
                rows.push([]);
            });
            filename = 'gst-' + currentGstReport;
        } else {
            showToast('No data to export. Load the data first.', 'error'); return;
        }
        if (rows.length <= 1) { showToast('No data to export', 'error'); return; }
        const csv = rows.map(r => r.map(c => `"${String(c || '').replace(/"/g, '""')}"`).join(',')).join('\n');
        const blob = new Blob(['\xEF\xBB\xBF' + csv], { type: 'text/csv;charset=utf-8' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob); a.download = `${filename}_${dayjs().format('YYYYMMDD')}.csv`;
        a.click(); URL.revokeObjectURL(a.href);
        showToast('CSV exported', 'success');
    };

    // ==================== EVENT LISTENERS ====================
    // Tab clicks
    document.querySelectorAll('#gst-tab-bar .gst-tab').forEach(btn => {
        btn.addEventListener('click', () => switchTab(btn.dataset.tab));
    });

    // Search debounce
    let srTimer;
    document.getElementById('sr-search')?.addEventListener('input', () => {
        clearTimeout(srTimer);
        srTimer = setTimeout(() => { if (salesData) renderSalesRegister(salesData); }, 300);
    });
    let stockTimer;
    document.getElementById('stock-search')?.addEventListener('input', () => {
        clearTimeout(stockTimer);
        stockTimer = setTimeout(() => { if (cache.stock) renderStockSummary(cache.stock); }, 300);
    });
    document.getElementById('stock-cat-filter')?.addEventListener('change', () => { if (cache.stock) renderStockSummary(cache.stock); });
    let outTimer;
    document.getElementById('out-search')?.addEventListener('input', () => {
        clearTimeout(outTimer);
        outTimer = setTimeout(() => { if (cache.outstanding) renderOutstanding(cache.outstanding); }, 300);
    });
    document.getElementById('out-filter')?.addEventListener('change', () => { if (cache.outstanding) renderOutstanding(cache.outstanding); });

    // ESC key to close modal
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeInvoice();
    });
    // Click overlay to close
    document.getElementById('invoice-modal-overlay')?.addEventListener('click', (e) => {
        if (e.target === e.currentTarget) closeInvoice();
    });

    // ==================== EXPOSE API ====================
    window._gst = {
        loadDashboard, loadDayBook, loadSalesRegister, loadProfitLoss,
        loadStockSummary, loadOutstanding, loadVouchers, loadPartyLedger,
        loadTaxLedger, loadGstReport,
        switchVoucher, switchGstReport, toggleVoucherForm,
        addVoucherItem, calcVoucherTotals, submitVoucher, deleteVoucher,
        viewInvoice, closeInvoice, printInvoice, printSection,
        exportCSV, viewPartyLedger
    };

    // ==================== INITIALIZE ====================
    populateFYSelect('dash-fy');
    populateFYSelect('pl-fy');
    setDefaultDates('sr-from', 'sr-to');
    setDefaultDates('db-from', 'db-to');
    setDefaultDates('pl-from', 'pl-to');
    setDefaultDates('tl-from', 'tl-to');
    await populatePartySelect();
    feather.replace();
    loadDashboard();
}
