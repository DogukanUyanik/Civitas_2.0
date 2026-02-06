// --- TAB SWITCHING ---
function switchTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));

    // Show selected
    document.getElementById('tab-' + tabName).classList.add('active');

    // Highlight button (find button that calls this function with same arg)
    const buttons = document.querySelectorAll('.tab-btn');
    if (tabName === 'income') buttons[0].classList.add('active');
    else buttons[1].classList.add('active');
}

// --- FILE UPLOAD & SCANNING ---
function handleFileUpload(inputElement) {
    const file = inputElement.files[0];
    if (!file) return;

    // Show Loading Spinner
    document.getElementById('loadingOverlay').style.display = 'flex';

    // Prepare Data
    const formData = new FormData();
    formData.append('file', file);

    // Get CSRF Token
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Call Backend API
    fetch('/api/invoices/scan', {
        method: 'POST',
        headers: {
            [csrfHeader]: csrfToken
        },
        body: formData
    })
        .then(response => {
            if (!response.ok) throw new Error("Scan failed");
            return response.json();
        })
        .then(data => {
            // Hide Loading
            document.getElementById('loadingOverlay').style.display = 'none';

            // Open Modal with Data
            showReviewModal(data);
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('loadingOverlay').style.display = 'none';
            alert('Failed to scan invoice. Please try again.');
        });

    // Reset input so same file can be selected again
    inputElement.value = '';
}

// --- REVIEW MODAL LOGIC ---
function showReviewModal(data) {
    const modal = document.getElementById('reviewModal');

    // 1. Set PDF Preview
    // We access the file via the temp path or the newly saved ID
    // Note: In real app, make sure this path is accessible via browser
    const viewerUrl = '/uploads/invoices/' + data.filename;
    document.getElementById('pdfPreview').src = viewerUrl;

    // 2. Pre-fill Form Fields (Smart Guessing)
    document.getElementById('hiddenFileUrl').value = data.filename;
    document.getElementById('hiddenOriginalName').value = data.filename; // or original name

    // Guess Date
    if (data.guessedDate) {
        document.getElementById('inputDate').value = data.guessedDate;
    } else {
        document.getElementById('inputDate').valueAsDate = new Date();
    }

    // Guess Amount
    if (data.guessedAmount) {
        document.getElementById('inputAmount').value = data.guessedAmount;
    }

    // Guess Counterparty
    if (data.guessedCounterparty && data.guessedCounterparty !== 'Unknown') {
        document.getElementById('inputCounterparty').value = data.guessedCounterparty;
    }

    if(data.guessedType){
        document.getElementById('inputInvoiceType').value = data.guessedType;    }

    // 3. Show Modal
    modal.style.display = 'block';
}

function closeModal() {
    document.getElementById('reviewModal').style.display = 'none';
    document.getElementById('pdfPreview').src = ''; // Clear iframe
}

// --- SAVE CONFIRMED INVOICE ---
function saveInvoice() {
    // 1. Gather Data
    const invoiceData = {
        type: document.getElementById('inputInvoiceType').value,
        counterparty: document.getElementById('inputCounterparty').value,
        invoiceDate: document.getElementById('inputDate').value,
        totalAmount: document.getElementById('inputAmount').value,
        category: document.getElementById('inputCategory').value,
        fileUrl: document.getElementById('hiddenFileUrl').value,
        originalFilename: document.getElementById('hiddenOriginalName').value
    };

    // 2. Validation
    if (!invoiceData.totalAmount || !invoiceData.invoiceDate) {
        alert("Please fill in Amount and Date");
        return;
    }

    // 3. Send to Backend
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch('/api/invoices/confirm', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(invoiceData)
    })
        .then(response => {
            if (response.ok) {
                closeModal();
                // Refresh page to see new entry
                window.location.reload();
            } else {
                alert("Error saving invoice");
            }
        })
        .catch(error => console.error('Error:', error));
}