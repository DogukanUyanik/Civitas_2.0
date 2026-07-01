// --- BULK IMPORT ---

function handleBulkImport(input) {
    const file = input.files[0];
    if (!file) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    const formData = new FormData();
    formData.append('file', file);

    document.getElementById('importLoadingOverlay').style.display = 'flex';

    fetch('/api/members/import', {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken },
        body: formData
    })
        .then(res => {
            if (!res.ok) return res.text().then(t => { throw new Error(t || 'Import failed'); });
            return res.json();
        })
        .then(result => {
            document.getElementById('importLoadingOverlay').style.display = 'none';
            showImportResult(result);
        })
        .catch(err => {
            document.getElementById('importLoadingOverlay').style.display = 'none';
            alert('Import failed: ' + err.message);
        });

    // Reset so the same file can be re-selected after fixing errors
    input.value = '';
}

function showImportResult(result) {
    const summary = document.getElementById('importSummary');
    summary.innerHTML =
        '<p><i class="fas fa-check-circle" style="color:#10b981"></i> <strong>' + result.successCount + '</strong> members imported successfully.</p>' +
        '<p><i class="fas fa-skip-forward" style="color:#f59e0b"></i> <strong>' + result.skippedCount + '</strong> rows skipped.</p>';

    const errorsDiv = document.getElementById('importErrors');
    if (result.errors && result.errors.length > 0) {
        errorsDiv.innerHTML =
            '<p class="import-errors-heading"><strong>Row errors:</strong></p>' +
            '<ul class="import-errors-list">' +
            result.errors.map(e => '<li>' + e + '</li>').join('') +
            '</ul>';
    } else {
        errorsDiv.innerHTML = '';
    }

    document.getElementById('importResultModal').style.display = 'flex';
}

function closeImportModal() {
    document.getElementById('importResultModal').style.display = 'none';
}
