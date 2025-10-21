/**
 * Toast Notification System
 * Usage: showToast('Success!', 'Project created successfully', 'success')
 */

// Create toast container if it doesn't exist
function initToastContainer() {
    if (!document.querySelector('.toast-container')) {
        const container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
}

/**
 * Show a toast notification
 * @param {string} title - Toast title
 * @param {string} message - Toast message
 * @param {string} type - Toast type: 'success', 'error', 'warning', 'info'
 * @param {number} duration - Auto-dismiss duration in ms (0 = no auto-dismiss)
 */
function showToast(title, message, type = 'info', duration = 5000) {
    initToastContainer();
    
    const container = document.querySelector('.toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    // Icon based on type
    const icons = {
        success: 'fa-check',
        error: 'fa-times',
        warning: 'fa-exclamation',
        info: 'fa-info'
    };
    
    toast.innerHTML = `
        <div class="toast-icon">
            <i class="fas ${icons[type]}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            ${message ? `<div class="toast-message">${message}</div>` : ''}
        </div>
        <button class="toast-close" onclick="removeToast(this.parentElement)">
            <i class="fas fa-times"></i>
        </button>
        ${duration > 0 ? '<div class="toast-progress"></div>' : ''}
    `;
    
    container.appendChild(toast);
    
    // Auto-dismiss
    if (duration > 0) {
        setTimeout(() => removeToast(toast), duration);
    }
    
    return toast;
}

/**
 * Remove a toast
 */
function removeToast(toast) {
    toast.classList.add('removing');
    setTimeout(() => {
        toast.remove();
    }, 300);
}

/**
 * Convenience methods
 */
window.toast = {
    success: (title, message, duration) => showToast(title, message, 'success', duration),
    error: (title, message, duration) => showToast(title, message, 'error', duration),
    warning: (title, message, duration) => showToast(title, message, 'warning', duration),
    info: (title, message, duration) => showToast(title, message, 'info', duration)
};

// Initialize on page load
document.addEventListener('DOMContentLoaded', initToastContainer);

// Listen for custom events (for server-side messages)
document.addEventListener('show-toast', (e) => {
    const { title, message, type, duration } = e.detail;
    showToast(title, message, type, duration);
});
