// analytics_script.js

async function initMod6() { // Assuming this is your entry point for this module
    // Ensure Chart.js, Tailwind (if via JS), and Font Awesome are loaded before this.
    // E.g., by placing their <script> and <link> tags before this script in your main HTML,
    // or by dynamically loading them and awaiting their load.
    // Load Tailwind CSS
    const tailwindScript = document.createElement('script');
    tailwindScript.src = 'https://cdn.tailwindcss.com';
    document.head.appendChild(tailwindScript);
    // Load Chart.js
    const chartScript = document.createElement('script');
    chartScript.src = 'https://cdn.jsdelivr.net/npm/chart.js';
    document.head.appendChild(chartScript);
    // Load Font Awesome CSS
    const fontAwesomeLink = document.createElement('link');
    fontAwesomeLink.rel = 'stylesheet';
    fontAwesomeLink.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css';
    document.head.appendChild(fontAwesomeLink);

    // Chart instances
    let ordersTrendChartInstance = null; // Updated name
    let orderStatusChartInstance = null;
    let topSellingProductsChartInstance = null;
    // Removed revenueByCategoryChartInstance, userRolesChartInstance

    const formatCurrency = (amount, fallback = '$0.00') => {
        if (typeof amount !== 'number' || isNaN(amount)) return fallback;
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
    };
    const formatNumber = (num, fallback = '0') => {
        if (typeof num !== 'number' || isNaN(num)) return fallback;
        return num.toLocaleString('en-US');
    };

    // formatPercentage is no longer needed as profit margin is removed

    const setText = (id, value, formatter) => {
        const el = document.getElementById(id);
        if (el) {
            // Handle cases where value might be "N/A" or not a number if backend sends it
            if (value === "N/A" || (typeof value !== 'number' && !parseFloat(value))) {
                 el.textContent = value;
            } else {
                 el.textContent = formatter ? formatter(value) : value;
            }
        }
    };

    async function fetchAnalyticsData() {
        try {
            // Replace with your actual API call mechanism
            // const response = await fetch('/analytics/summary').then(res => res.json());
            const response = await callApi("GET","analytics/summary"); // Using your provided callApi
            console.log('Analytics Data Received:', response);
            if (response) {
                updateDashboard(response);
            } else {
                console.error("No data received from analytics summary API.");
                // You might want to display an error on the dashboard here
            }
        } catch (error) {
            console.error("Failed to fetch analytics data:", error);
            // Display a more user-friendly error on the dashboard
            const header = document.querySelector('header p');
            if (header) header.textContent = "Could not load analytics data. Please try again later.";
        }
    }

    function updateDashboard(data) {
        // Core Metrics - Earnings
        setText('metric-total-earnings-overall', data.total_earnings_overall, formatCurrency);
        setText('metric-total-earnings-this-month', data.total_earnings_this_month, formatCurrency);
        setText('metric-total-earnings-last-month', data.total_earnings_last_month, formatCurrency);

        // Core Metrics - Orders
        setText('metric-total-orders-overall', data.total_orders_overall, formatNumber);
        setText('metric-total-orders-this-month', data.total_orders_this_month, formatNumber);
        setText('metric-total-orders-last-month', data.total_orders_last_month, formatNumber);


        // Low Stock
        setText('metric-low-stock-products', data.low_stock_products_count, formatNumber);
        const lowStockThresholdEl = document.getElementById('metric-low-stock-threshold');
        if (lowStockThresholdEl) {
            lowStockThresholdEl.textContent = `(Threshold: <${data.low_stock_threshold || 5})`;
        }
         renderLowStockProductsList(data.low_stock_products_list, data.low_stock_threshold);


        // Charts
        // renderOrdersTrendChart(data.orders_trend_12_months); // Updated function call
        renderOrderStatusChart(data.order_status_distribution);
        renderTopSellingProductsChart(data.top_selling_products_revenue); // Updated key name


        // Lists
        renderTopOrderTakingUsersList(data.top_order_taking_users);

        // Removed calls for removed charts and tables
    }

    // Updated function name and logic for 12 Months Orders Trend
    function renderOrdersTrendChart(ordersData) {
        const ctx = document.getElementById('ordersTrend12MonthsChart')?.getContext('2d');
        const noteEl = document.getElementById('ordersTrend12MonthsNote'); // Updated note element ID
        if (!ctx) return;
        if (noteEl) noteEl.textContent = ordersData?.note || '';

        if (ordersTrendChartInstance) ordersTrendChartInstance.destroy();
        ordersTrendChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: ordersData?.labels?.length ? ordersData.labels : [],
                datasets: [{
                    label: 'Orders Count', // Updated label
                    data: ordersData?.data?.length ? ordersData.data : [],
                    borderColor: 'rgb(59, 130, 246)', backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.3, fill: true, pointBackgroundColor: 'rgb(59, 130, 246)',
                    pointBorderColor: '#fff', pointHoverRadius: 7, pointHoverBackgroundColor: 'rgb(59, 130, 246)',
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                scales: { y: { beginAtZero: true, ticks: { callback: value => formatNumber(value) } } }, // Format Y-axis as numbers
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: context => `Orders: ${formatNumber(context.raw)}` } } // Updated tooltip
                }
            }
        });
    }

    function renderOrderStatusChart(statusData) {
        const ctx = document.getElementById('orderStatusChart')?.getContext('2d');
        const noteEl = document.getElementById('orderStatusNote');
        if (!ctx) return;

        const hasData = statusData && Object.keys(statusData).length > 0;
        if (noteEl) noteEl.textContent = hasData ? (statusData.note || '') : "No order status data available.";


        if (orderStatusChartInstance) orderStatusChartInstance.destroy();
        const labels = hasData ? Object.keys(statusData) : ['No Data'];
        const dataPoints = hasData ? Object.values(statusData) : [1];
        const backgroundColors = labels.length > 1 ? [
            'rgba(239, 68, 68, 0.7)', 'rgba(245, 158, 11, 0.7)', 'rgba(59, 130, 246, 0.7)',
            'rgba(16, 185, 129, 0.7)', 'rgba(107, 114, 128, 0.7)', 'rgba(139, 92, 246, 0.7)'
        ].slice(0, labels.length) : ['rgba(200, 200, 200, 0.7)'];
        orderStatusChartInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels.map(label => label.replace("ORDER_", "").replace(/_/g, " ").toLowerCase().replace(/\b\w/g, l => l.toUpperCase())),
                datasets: [{
                    label: 'Order Status', data: dataPoints,
                    backgroundColor: backgroundColors, borderColor: '#fff', hoverOffset: 8
                }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: {boxWidth: 12, padding: 15} } }
            }
        });
    }

    // Modified function for Top 10 Selling Products by Revenue
    function renderTopSellingProductsChart(productsData) {
        const ctx = document.getElementById('topSellingProductsChart')?.getContext('2d');
        const noteEl = document.getElementById('topSellingProductsNote');
        if (!ctx) return;

        const hasRealData = productsData?.length && !productsData[0].note;
        if (noteEl) noteEl.textContent = hasRealData ? '' : (productsData?.[0]?.note || "No top selling products data.");

        if (topSellingProductsChartInstance) topSellingProductsChartInstance.destroy();

        const labels = hasRealData ? productsData.map(p => p.name) : ['No Products'];
        const dataPoints = hasRealData ? productsData.map(p => p.revenue) : [0]; // Use revenue data
        const bgColors = [
            'rgba(139, 92, 246, 0.7)', 'rgba(236, 72, 153, 0.7)',
            'rgba(249, 115, 22, 0.7)', 'rgba(22, 163, 74, 0.7)', 'rgba(6, 182, 212, 0.7)',
            'rgba(59, 130, 246, 0.7)', 'rgba(245, 158, 11, 0.7)', 'rgba(239, 68, 68, 0.7)',
            'rgba(34, 197, 94, 0.7)', 'rgba(217, 70, 239, 0.7)' // Added more colors for top 10
        ].slice(0, labels.length);

        topSellingProductsChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{ label: 'Revenue', data: dataPoints, backgroundColor: bgColors, borderWidth: 1 }] //# Updated label
            },
            options: {
                indexAxis: 'y', responsive: true, maintainAspectRatio: false,
                scales: { x: { beginAtZero: true, ticks: { callback: value => formatCurrency(value) } } }, //# Format X-axis as currency
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: context => `${context.label}: ${formatCurrency(context.raw)}` } } //# Updated tooltip
                }
            }
        });
    }

    // # New function to render Top 5 Order Taking Users List
    function renderTopOrderTakingUsersList(usersData) {
        const listEl = document.getElementById('topOrderTakingUsersList');
        const noteEl = document.getElementById('topOrderTakingUsersNote');
        if (!listEl) return;

        listEl.innerHTML = ''; //# Clear existing list items

        const hasRealData = usersData?.length && !usersData[0].note;
         if (noteEl) noteEl.textContent = hasRealData ? '' : (usersData?.[0]?.note || "No top order taking users data.");

        if (hasRealData) {
            usersData.forEach(user => {
                const li = document.createElement('li');
                li.className = "text-gray-700 py-1"; //# Simple styling
                li.textContent = `${user.username}: ${formatCurrency(user.total_value)}`;
                listEl.appendChild(li);
            });
        } else {
             const li = document.createElement('li');
             li.className = "text-gray-500 py-1";
             li.textContent = usersData?.[0]?.note || "No top order taking users data.";
             listEl.appendChild(li);
        }
    }

    //  # New function to render Low Stock Products List
    function renderLowStockProductsList(productsData, threshold) {
        const listEl = document.getElementById('lowStockProductsList');
        const noteEl = document.getElementById('lowStockProductsNote');
         const thresholdTextEl = document.getElementById('lowStockThresholdText');

        if (!listEl) return;

         if (thresholdTextEl) {
             thresholdTextEl.textContent = threshold || 5;
         }

        listEl.innerHTML = '';// # Clear existing list items

        const hasRealData = productsData?.length && !productsData[0].note;
         if (noteEl) noteEl.textContent = hasRealData ? '' : (productsData?.[0]?.note || "No low stock products data.");


        if (hasRealData) {
            productsData.forEach(product => {
                const div = document.createElement('div');
                div.className = "low-stock-item text-gray-700 flex justify-between items-center";// # Add styling
                div.innerHTML = `
                    <span>${product.name}</span>
                    <span class="font-semibold">${product.stock} units</span>
                `;
                //  # Add click event listener to show details (placeholder)
                //  div.addEventListener('click', () => {
                //      alert(`Details for Product ID: ${product.id}, Name: ${product.name}, Stock: ${product.stock}\n(Details view needs to be implemented)`);
                //  });
                listEl.appendChild(div);
            });
        } else {
             const div = document.createElement('div');
             div.className = "low-stock-item text-gray-500";
             div.textContent = productsData?.[0]?.note || "No low stock products to display.";
             listEl.appendChild(div);
        }
    }


    // # Removed renderRevenueByCategoryChart, renderUserRolesChart, renderRecentOrdersTable functions


    // # Initial data fetch
    fetchAnalyticsData();
    // # setInterval(fetchAnalyticsData, 30000); # Optional: Refresh data periodically
}

// # Ensure this script is called after the DOM is fully loaded,
// # or if you have a main app structure, integrate initMod6() call there.
// # For example, if this is the only script for the page:
// # if (document.readyState === 'loading') {
// # document.addEventListener('DOMContentLoaded', initMod6);
// # } else {
// # initMod6();
// # }
// # Assuming your existing setup calls initMod6 appropriately.
window.initMod6 = initMod6;// # Make it globally available if called from HTML onclick or similar