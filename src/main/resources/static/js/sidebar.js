document.addEventListener('DOMContentLoaded', function () {
    console.log('DOM loaded, initializing sidebar...');

    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('toggleSidebar');
    const mainContents = document.querySelectorAll('.main-content, .members-container');

    // Debug: Check if elements are found
    console.log('Sidebar element:', sidebar);
    console.log('Toggle button:', toggleBtn);
    console.log('Main content elements:', mainContents);

    if (!sidebar) {
        console.error('Sidebar element not found!');
        return;
    }

    if (!toggleBtn) {
        console.error('Toggle button not found!');
        return;
    }

    // Apply saved state
    const savedState = localStorage.getItem('sidebarCollapsed');
    console.log('Saved state:', savedState);

    if (savedState === 'true') {
        sidebar.classList.add('collapsed');
        mainContents.forEach(mc => mc.classList.add('sidebar-collapsed'));
        console.log('Applied saved collapsed state');
    }

    // Desktop toggle
    function toggleDesktop() {
        console.log('Toggling desktop sidebar...');
        sidebar.classList.toggle('collapsed');
        mainContents.forEach(mc => mc.classList.toggle('sidebar-collapsed'));

        const isCollapsed = sidebar.classList.contains('collapsed');
        localStorage.setItem('sidebarCollapsed', isCollapsed);
        console.log('Sidebar collapsed:', isCollapsed);
    }

    // Mobile toggle
    function toggleMobile() {
        console.log('Toggling mobile sidebar...');
        sidebar.classList.toggle('mobile-open');
        console.log('Mobile sidebar open:', sidebar.classList.contains('mobile-open'));
    }

    // Toggle button click
    toggleBtn.addEventListener('click', function (e) {
        console.log('Toggle button clicked!');
        e.preventDefault(); // Prevent any default behavior

        const isMobile = window.innerWidth <= 768;
        console.log('Is mobile:', isMobile, 'Window width:', window.innerWidth);

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
            console.log('Button animation complete');
        }, 150);
    });

    // Close mobile sidebar when clicking outside
    document.addEventListener('click', function (e) {
        const isMobile = window.innerWidth <= 768;
        if (isMobile && sidebar.classList.contains('mobile-open')) {
            if (!sidebar.contains(e.target) && e.target !== toggleBtn) {
                sidebar.classList.remove('mobile-open');
                console.log('Mobile sidebar closed by outside click');
            }
        }
    });

    // Ctrl+B shortcut for desktop toggle
    document.addEventListener('keydown', function (e) {
        if (e.ctrlKey && e.key.toLowerCase() === 'b' && window.innerWidth > 768) {
            e.preventDefault();
            console.log('Ctrl+B pressed, toggling sidebar');
            toggleDesktop();
        }
    });

    // Test function - you can call this from browser console
    window.testSidebarToggle = function() {
        console.log('Testing sidebar toggle...');
        toggleDesktop();
    };

    console.log('Sidebar initialization complete');
});