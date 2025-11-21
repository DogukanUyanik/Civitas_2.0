function toggleNotifications() {
    const dropdown = document.getElementById('notificationDropdown');
    if (!dropdown) return;

    const isVisible = dropdown.style.display === 'block';
    dropdown.style.display = isVisible ? 'none' : 'block';
}

// Optional: close dropdown if clicking outside
document.addEventListener('click', function(e) {
    const bell = document.querySelector('.notification-bell');
    const dropdown = document.getElementById('notificationDropdown');

    if (!bell || !dropdown) return;

    if (!bell.contains(e.target)) {
        dropdown.style.display = 'none';
    }
});
