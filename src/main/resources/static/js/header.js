document.addEventListener("DOMContentLoaded", function () {
    console.log("🚀 App loaded. Initializing Notifications...");

    const bell = document.getElementById("notificationBell");
    const dropdown = document.getElementById("notificationDropdown");

    // Debugging check
    if (!bell) console.error("❌ Error: Could not find element with id 'notificationBell'");
    if (!dropdown) console.error("❌ Error: Could not find element with id 'notificationDropdown'");

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

                // Style Unread
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
                        window.location.href = note.url;
                    };
                }
                list.appendChild(li);
            });
        })
        .catch(err => console.error("❌ Error loading list:", err));
}