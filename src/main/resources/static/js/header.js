document.addEventListener("DOMContentLoaded", function () {
    console.log("🚀 App loaded. Initializing Notifications...");

    const bell = document.getElementById("notificationBell");
    const dropdown = document.getElementById("notificationDropdown");

    // Debugging check
    if (!bell) console.warn("⚠️ Warning: Could not find element with id 'notificationBell'");
    if (!dropdown) console.warn("⚠️ Warning: Could not find element with id 'notificationDropdown'");

    // 1. Fetch unread count immediately
    fetchUnreadCount();

    // 2. Add Click Event to Bell
    if (bell) {
        bell.addEventListener("click", function (e) {
            console.log("🔔 Bell clicked!");
            e.stopPropagation(); // Stop click from closing the menu immediately
            toggleNotifications();
        });
    }

    // 3. Close dropdown when clicking outside
    document.addEventListener("click", function (e) {
        if (dropdown && dropdown.style.display === 'block') {
            if (!bell.contains(e.target) && !dropdown.contains(e.target)) {
                console.log("🔒 Closing dropdown (clicked outside)");
                dropdown.style.display = 'none';
            }
        }
    });
});

function toggleNotifications() {
    const dropdown = document.getElementById('notificationDropdown');
    if (!dropdown) return;

    // Toggle logic
    const isHidden = dropdown.style.display === 'none' || dropdown.style.display === '';

    if (isHidden) {
        console.log("📂 Opening dropdown...");
        dropdown.style.display = 'block';
        loadRecentNotifications();
    } else {
        console.log("qw Closing dropdown...");
        dropdown.style.display = 'none';
    }
}

function fetchUnreadCount() {
    console.log("📡 Fetching unread count...");
    fetch('/api/notifications/unread-count')
        .then(response => {
            if (!response.ok) throw new Error("Network response was not ok");
            return response.json();
        })
        .then(count => {
            console.log("🔢 Unread count received:", count);
            const badge = document.getElementById('notificationBadge');
            if (badge) {
                if (count > 0) {
                    badge.innerText = count > 99 ? '99+' : count;
                    badge.style.display = 'flex';
                } else {
                    badge.style.display = 'none';
                }
            }
        })
        .catch(err => console.error("❌ Error fetching count:", err));
}

function loadRecentNotifications() {
    const list = document.getElementById('notificationList');
    if (!list) return;

    list.innerHTML = '<li style="text-align:center; padding:15px; color:#9ca3af;">Loading...</li>';

    fetch('/api/notifications/recent')
        .then(response => response.json())
        .then(data => {
            console.log("📨 Notifications received:", data);
            list.innerHTML = '';

            if (data.length === 0) {
                list.innerHTML = '<li style="text-align:center; padding:15px; color:#9ca3af;">No new notifications</li>';
                return;
            }

            data.forEach(note => {
                const li = document.createElement("li");

                // Style unread items
                if (note.status === 'UNREAD') {
                    li.style.backgroundColor = '#f0fdf4';
                    li.style.borderLeft = '3px solid #10b981';
                }

                const timeStr = new Date(note.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

                li.innerHTML = `
                    <div style="font-weight: 600; font-size: 13px; color: #111;">${note.title}</div>
                    <div style="font-size: 12px; color: #555; margin-top:2px;">${note.message}</div>
                    <div style="font-size: 10px; color: #999; margin-top:4px;">${timeStr}</div>
                `;

                if(note.url) {
                    li.style.cursor = "pointer";
                    li.onclick = (e) => {
                        e.stopPropagation(); // Prevent closing dropdown immediately
                        // ✅ Use markAndRedirect so clicking dropdown ALSO marks as read
                        markAndRedirect(note.id, note.url);
                    };
                }
                list.appendChild(li);
            });
        })
        .catch(err => console.error("❌ Error loading list:", err));
}

/**
 * Marks a notification as read via API, then redirects the user.
 * @param {number} notificationId - The ID of the notification
 * @param {string} url - The URL to redirect to (optional)
 */
function markAndRedirect(notificationId, url) {
    // 1. Get CSRF Token from Meta Tags (Spring Security)
    const csrfTokenMeta = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

    if (!csrfTokenMeta || !csrfHeaderMeta) {
        console.error("❌ CSRF Meta tags not found! Cannot send POST request.");
        // Fallback: just redirect if we can't mark read
        if (url && url !== '#' && url !== 'null') window.location.href = url;
        return;
    }

    const csrfToken = csrfTokenMeta.getAttribute('content');
    const csrfHeader = csrfHeaderMeta.getAttribute('content');

    // 2. Send API Request
    fetch(`/api/notifications/${notificationId}/read`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken // 👈 Critical for Spring Security
        }
    })
        .then(response => {
            if (response.ok) {
                console.log(`Notification ${notificationId} marked as read.`);

                // 3. Update Badge immediately (UX improvement)
                const badge = document.getElementById("notificationBadge");
                if (badge && badge.innerText) {
                    let count = parseInt(badge.innerText);
                    if (count > 1) badge.innerText = count - 1;
                    else badge.style.display = 'none';
                }
            }
        })
        .catch(err => console.error("Error marking read:", err))
        .finally(() => {
            // 4. Redirect regardless of success/fail
            if (url && url !== '#' && url !== 'null') {
                window.location.href = url;
            }
        });
}