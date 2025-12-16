document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, initializing sidebar...');

    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebar');
    const mainContents = document.querySelectorAll('.main-content, .members-container, .events-container, .transactions-container, .notifications-container');
    const header = document.querySelector('.app-header');

    if (!sidebar) {
        console.error('Sidebar element not found!');
        return;
    }

    if (!toggleBtn) {
        console.error('Toggle button not found!');
        return;
    }

    // Apply saved state on page load
    const savedState = localStorage.getItem('sidebarCollapsed');

    if (savedState === 'true') {
        sidebar.classList.add('collapsed');
        mainContents.forEach(mc => mc.classList.add('sidebar-collapsed'));
        if (header) {
            header.classList.add('sidebar-collapsed');
        }
    }

    // Desktop toggle
    function toggleDesktop() {
        sidebar.classList.toggle('collapsed');
        mainContents.forEach(mc => mc.classList.toggle('sidebar-collapsed'));

        if (header) {
            header.classList.toggle('sidebar-collapsed');
        }

        const isCollapsed = sidebar.classList.contains('collapsed');
        localStorage.setItem('sidebarCollapsed', isCollapsed);
    }

    // Mobile toggle
    function toggleMobile() {
        sidebar.classList.toggle('mobile-open');
    }

    // Toggle button click
    toggleBtn.addEventListener('click', function (e) {
        e.preventDefault();

        const isMobile = window.innerWidth <= 768;

        if (isMobile) {
            e.stopPropagation();
            toggleMobile();
        } else {
            toggleDesktop();
        }

        // Button animation
        toggleBtn.style.transform = 'scale(0.9)';
        setTimeout(() => {
            toggleBtn.style.transform = 'scale(1)';
        }, 150);
    });

    // Close mobile sidebar when clicking outside
    document.addEventListener('click', function (e) {
        const isMobile = window.innerWidth <= 768;
        if (isMobile && sidebar.classList.contains('mobile-open')) {
            if (!sidebar.contains(e.target) && e.target !== toggleBtn) {
                sidebar.classList.remove('mobile-open');
            }
        }
    });

    // Ctrl+B shortcut for desktop toggle
    document.addEventListener('keydown', function (e) {
        if (e.ctrlKey && e.key.toLowerCase() === 'b' && window.innerWidth > 768) {
            e.preventDefault();
            toggleDesktop();
        }
    });

    // Test function - you can call this from browser console
    window.testSidebarToggle = function() {
        toggleDesktop();
    };
});