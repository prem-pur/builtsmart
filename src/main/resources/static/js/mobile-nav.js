/**
 * Mobile Navigation Handler
 * Handles sidebar toggle on mobile devices
 */

document.addEventListener('DOMContentLoaded', function() {
    // Create mobile navigation button if it doesn't exist
    if (!document.querySelector('.mobile-nav-toggle')) {
        const toggleBtn = document.createElement('button');
        toggleBtn.className = 'mobile-nav-toggle';
        toggleBtn.innerHTML = '<i class="fas fa-bars"></i>';
        toggleBtn.setAttribute('aria-label', 'Toggle navigation');
        document.body.appendChild(toggleBtn);
        
        // Create overlay
        const overlay = document.createElement('div');
        overlay.className = 'mobile-nav-overlay';
        document.body.appendChild(overlay);
        
        // Get sidebar
        const sidebar = document.querySelector('.sidebar');
        
        if (sidebar) {
            // Toggle sidebar
            toggleBtn.addEventListener('click', function() {
                sidebar.classList.toggle('mobile-open');
                overlay.classList.toggle('active');
                
                // Change icon
                const icon = this.querySelector('i');
                if (sidebar.classList.contains('mobile-open')) {
                    icon.className = 'fas fa-times';
                } else {
                    icon.className = 'fas fa-bars';
                }
            });
            
            // Close on overlay click
            overlay.addEventListener('click', function() {
                sidebar.classList.remove('mobile-open');
                overlay.classList.remove('active');
                toggleBtn.querySelector('i').className = 'fas fa-bars';
            });
            
            // Close on link click (for mobile)
            const sidebarLinks = sidebar.querySelectorAll('a');
            sidebarLinks.forEach(link => {
                link.addEventListener('click', function() {
                    if (window.innerWidth <= 992) {
                        sidebar.classList.remove('mobile-open');
                        overlay.classList.remove('active');
                        toggleBtn.querySelector('i').className = 'fas fa-bars';
                    }
                });
            });
        }
    }
});

/**
 * Handle window resize
 */
window.addEventListener('resize', function() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.mobile-nav-overlay');
    const toggleBtn = document.querySelector('.mobile-nav-toggle');
    
    if (window.innerWidth > 992 && sidebar) {
        sidebar.classList.remove('mobile-open');
        if (overlay) overlay.classList.remove('active');
        if (toggleBtn) toggleBtn.querySelector('i').className = 'fas fa-bars';
    }
});
