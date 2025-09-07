
    document.addEventListener('DOMContentLoaded', function() {
    console.log('Script loaded'); // Debug log

    const table = document.getElementById('transactionsTable');
    const tbody = document.getElementById('transactionsBody');
    const dateFilter = document.getElementById('dateFilter');
    const statusFilter = document.getElementById('statusFilter');
    const resetBtn = document.getElementById('resetFilters');
    const noDataRow = document.querySelector('.no-data-row');

    console.log('Elements found:', { table, tbody, dateFilter, statusFilter, resetBtn, noDataRow }); // Debug log

    let currentSort = { column: 'date', direction: 'desc' };

    // Check if elements exist before proceeding
    if (!table || !tbody) {
    console.error('Table elements not found');
    return;
}

    // Initialize - sort by date descending by default
    setTimeout(() => {
    sortTable('date', 'desc');
}, 100);

    // Add click listeners to sortable headers
    document.querySelectorAll('.sortable').forEach(header => {
    header.addEventListener('click', function() {
    console.log('Header clicked:', this.dataset.column); // Debug log
    const column = this.dataset.column;
    let direction = 'asc';

    if (currentSort.column === column && currentSort.direction === 'asc') {
    direction = 'desc';
}

    sortTable(column, direction);
});
});

    // Add filter listeners
    if (dateFilter) {
    dateFilter.addEventListener('change', function() {
    console.log('Date filter changed:', this.value); // Debug log
    applyFilters();
});
}

    if (statusFilter) {
    statusFilter.addEventListener('change', function() {
    console.log('Status filter changed:', this.value); // Debug log
    applyFilters();
});
}

    if (resetBtn) {
    resetBtn.addEventListener('click', function() {
    console.log('Reset button clicked'); // Debug log
    resetFilters();
});
}

    function sortTable(column, direction) {
    console.log('Sorting by:', column, direction); // Debug log

    const rows = Array.from(tbody.querySelectorAll('tr:not(.no-data-row)'));
    console.log('Found rows:', rows.length); // Debug log

    if (rows.length === 0) {
    console.log('No rows to sort');
    return;
}

    rows.sort((a, b) => {
    let aValue, bValue;

    switch(column) {
    case 'date':
    // Get date from the actual cell text
    const aDateText = a.querySelector('.date-cell')?.textContent || '';
    const bDateText = b.querySelector('.date-cell')?.textContent || '';

    // Parse date in format dd/MM/yyyy HH:mm
    aValue = parseDateFromText(aDateText);
    bValue = parseDateFromText(bDateText);
    break;

    case 'amount':
    const aAmountText = a.querySelector('.amount-cell')?.textContent || '0';
    const bAmountText = b.querySelector('.amount-cell')?.textContent || '0';
    aValue = parseFloat(aAmountText.replace(/[^\d.-]/g, '')) || 0;
    bValue = parseFloat(bAmountText.replace(/[^\d.-]/g, '')) || 0;
    break;

    case 'type':
    aValue = (a.querySelector('.type-cell')?.textContent || '').toLowerCase().trim();
    bValue = (b.querySelector('.type-cell')?.textContent || '').toLowerCase().trim();
    break;

    case 'currency':
    aValue = (a.querySelector('.currency-cell')?.textContent || '').toLowerCase().trim();
    bValue = (b.querySelector('.currency-cell')?.textContent || '').toLowerCase().trim();
    break;

    case 'status':
    aValue = (a.querySelector('.status-cell .status-pill')?.textContent || '').toLowerCase().trim();
    bValue = (b.querySelector('.status-cell .status-pill')?.textContent || '').toLowerCase().trim();
    break;

    default:
    return 0;
}

    console.log('Comparing:', aValue, 'vs', bValue); // Debug log

    if (aValue < bValue) return direction === 'asc' ? -1 : 1;
    if (aValue > bValue) return direction === 'asc' ? 1 : -1;
    return 0;
});

    // Update UI
    updateSortUI(column, direction);
    currentSort = { column, direction };

    // Re-append sorted rows
    rows.forEach(row => tbody.appendChild(row));
    if (noDataRow) {
    tbody.appendChild(noDataRow); // Keep no-data row at the end
}

    console.log('Sort completed'); // Debug log
}

    function parseDateFromText(dateText) {
    if (!dateText) return new Date(0);

    try {
    // Parse format: dd/MM/yyyy HH:mm
    const parts = dateText.split(' ');
    const datePart = parts[0]; // dd/MM/yyyy
    const timePart = parts[1] || '00:00'; // HH:mm

    const dateComponents = datePart.split('/');
    const timeComponents = timePart.split(':');

    if (dateComponents.length === 3 && timeComponents.length === 2) {
    const day = parseInt(dateComponents[0]);
    const month = parseInt(dateComponents[1]) - 1; // Month is 0-indexed
    const year = parseInt(dateComponents[2]);
    const hour = parseInt(timeComponents[0]);
    const minute = parseInt(timeComponents[1]);

    return new Date(year, month, day, hour, minute);
}
} catch (e) {
    console.error('Error parsing date:', dateText, e);
}

    return new Date(0);
}

    function updateSortUI(column, direction) {
    // Remove all existing sort classes
    document.querySelectorAll('.sortable').forEach(th => {
    th.classList.remove('sorted-asc', 'sorted-desc');
});

    // Add appropriate class to current sorted column
    const currentHeader = document.querySelector(`[data-column="${column}"]`);
    if (currentHeader) {
    currentHeader.classList.add(direction === 'asc' ? 'sorted-asc' : 'sorted-desc');
}
}

    function applyFilters() {
    if (!dateFilter || !statusFilter) return;

    const dateValue = dateFilter.value;
    const statusValue = statusFilter.value;
    const rows = tbody.querySelectorAll('tr:not(.no-data-row)');
    let visibleCount = 0;

    console.log('Applying filters - Date:', dateValue, 'Status:', statusValue); // Debug log

    rows.forEach(row => {
    let showRow = true;

    // Date filter
    if (dateValue !== 'all') {
    const dateText = row.querySelector('.date-cell')?.textContent || '';
    const rowDate = parseDateFromText(dateText);
    const now = new Date();
    let cutoffDate;

    switch(dateValue) {
    case 'today':
    cutoffDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    break;
    case 'week':
    cutoffDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
    break;
    case 'month':
    cutoffDate = new Date(now.getFullYear(), now.getMonth() - 1, now.getDate());
    break;
    case 'quarter':
    cutoffDate = new Date(now.getFullYear(), now.getMonth() - 3, now.getDate());
    break;
    case 'year':
    cutoffDate = new Date(now.getFullYear() - 1, now.getMonth(), now.getDate());
    break;
}

    if (cutoffDate && rowDate < cutoffDate) {
    showRow = false;
}
}

    // Status filter
    if (statusValue !== 'all') {
    const rowStatus = row.querySelector('.status-cell .status-pill')?.textContent?.trim() || '';
    if (rowStatus.toUpperCase() !== statusValue.toUpperCase()) {
    showRow = false;
}
}

    if (showRow) {
    row.classList.remove('hidden');
    visibleCount++;
} else {
    row.classList.add('hidden');
}
});

    console.log('Visible rows after filtering:', visibleCount); // Debug log

    // Show/hide no data message
    if (noDataRow) {
    if (visibleCount === 0) {
    noDataRow.classList.add('show');
} else {
    noDataRow.classList.remove('show');
}
}
}

    function resetFilters() {
    if (dateFilter) dateFilter.value = 'all';
    if (statusFilter) statusFilter.value = 'all';
    applyFilters();
    sortTable('date', 'desc'); // Reset to default sort
}





        const modal = document.getElementById('paymentModal');
        const btn = document.getElementById('sendPaymentBtn');
        const cancelBtn = document.getElementById('cancelPayment');

        btn.addEventListener('click', () => {
            modal.style.display = 'flex';
        });

        cancelBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        window.addEventListener('click', (e) => {
            if (e.target === modal) modal.style.display = 'none';
        });



        const sendPaymentBtn = document.getElementById('sendPaymentBtn');
        const paymentModal = document.getElementById('paymentModal');
        const cancelPaymentBtn = document.getElementById('cancelPayment');
        const paymentForm = document.getElementById('paymentForm');
        const paymentResult = document.getElementById('paymentResult');
        const paymentLink = document.getElementById('paymentLink');
        const closeResultBtn = document.getElementById('closeResult');

        // Show modal
        sendPaymentBtn.addEventListener('click', () => {
            paymentModal.classList.remove('hidden');
        });

        // Hide modal
        cancelPaymentBtn.addEventListener('click', () => {
            paymentModal.classList.add('hidden');
            paymentForm.reset();
        });

        // Close payment result
        closeResultBtn.addEventListener('click', () => {
            paymentResult.classList.add('hidden');
            paymentForm.reset();
        });

        // Submit form
        paymentForm.addEventListener('submit', (e) => {
            e.preventDefault();

            const memberId = parseInt(sendPaymentBtn.dataset.memberId, 10);
            const amount = parseFloat(document.getElementById('amount').value.trim());
            const paymentType = document.getElementById('paymentType').value.trim();
            const currency = document.getElementById('currency').value.trim(); // get value from select box
            const note = document.getElementById('note').value.trim();

            if (!memberId || isNaN(amount) || amount <= 0 || !paymentType) {
                alert('Please fill in all required fields correctly.');
                return;
            }

            // CSRF headers
            const csrfToken = document.querySelector('meta[name="_csrf"]').content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

            const params = new URLSearchParams();
            params.append('memberId', memberId);
            params.append('amount', amount);
            params.append('paymentType', paymentType);
            params.append('currency', currency); // <-- add this
            if (note) params.append('note', note);

            fetch('/api/transactions/send-payment', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [csrfHeader]: csrfToken
                },
                body: params.toString()
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        paymentModal.classList.add('hidden');
                        paymentResult.classList.remove('hidden');
                        paymentLink.href = data.paymentLink;
                        paymentLink.textContent = data.paymentLink;
                    } else {
                        alert('Error: ' + data.message);
                    }
                })
                .catch(err => {
                    console.error('Fetch error:', err);
                    alert('Something went wrong!');
                });
        });

    });
