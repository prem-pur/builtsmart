// BuildSmart CMS - Main JavaScript File

document.addEventListener('DOMContentLoaded', function() {
    
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
    
    // Auto-hide alerts after 5 seconds
    setTimeout(function() {
        var alerts = document.querySelectorAll('.alert');
        alerts.forEach(function(alert) {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        });
    }, 5000);
    
    // Form validation
    var forms = document.querySelectorAll('.needs-validation');
    Array.prototype.slice.call(forms).forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
    
    // Confirm delete actions
    var deleteButtons = document.querySelectorAll('.btn-delete');
    deleteButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            if (!confirm('Are you sure you want to delete this item?')) {
                e.preventDefault();
            }
        });
    });
    
    // Auto-save form data
    var autoSaveForms = document.querySelectorAll('.auto-save');
    autoSaveForms.forEach(function(form) {
        var inputs = form.querySelectorAll('input, textarea, select');
        inputs.forEach(function(input) {
            input.addEventListener('change', function() {
                saveFormData(form);
            });
        });
    });
    
    // Real-time search
    var searchInputs = document.querySelectorAll('.search-input');
    searchInputs.forEach(function(input) {
        input.addEventListener('input', debounce(function() {
            performSearch(this.value);
        }, 300));
    });
    
    // Chart initialization
    initializeCharts();
    
    // Notification system
    initializeNotifications();
});

// Utility Functions

function debounce(func, wait) {
    var timeout;
    return function executedFunction() {
        var later = function() {
            clearTimeout(timeout);
            func();
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

function saveFormData(form) {
    var formData = new FormData(form);
    var data = {};
    for (var [key, value] of formData.entries()) {
        data[key] = value;
    }
    localStorage.setItem('form_' + form.id, JSON.stringify(data));
}

function loadFormData(form) {
    var data = localStorage.getItem('form_' + form.id);
    if (data) {
        var formData = JSON.parse(data);
        for (var key in formData) {
            var input = form.querySelector('[name="' + key + '"]');
            if (input) {
                input.value = formData[key];
            }
        }
    }
}

function performSearch(query) {
    // Implement search functionality
    console.log('Searching for:', query);
}

function initializeCharts() {
    // Initialize any charts on the page
    var chartElements = document.querySelectorAll('[data-chart]');
    chartElements.forEach(function(element) {
        var chartType = element.getAttribute('data-chart');
        var chartData = element.getAttribute('data-chart-data');
        
        if (chartData) {
            try {
                var data = JSON.parse(chartData);
                createChart(element, chartType, data);
            } catch (e) {
                console.error('Error parsing chart data:', e);
            }
        }
    });
}

function createChart(element, type, data) {
    var ctx = element.getContext('2d');
    new Chart(ctx, {
        type: type,
        data: data,
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

function initializeNotifications() {
    // Check for new notifications
    setInterval(function() {
        checkForNotifications();
    }, 30000); // Check every 30 seconds
}

function checkForNotifications() {
    // Implement notification checking
    fetch('/api/notifications/unread')
        .then(response => response.json())
        .then(data => {
            if (data.length > 0) {
                showNotificationBadge(data.length);
            }
        })
        .catch(error => {
            console.error('Error checking notifications:', error);
        });
}

function showNotificationBadge(count) {
    var badge = document.querySelector('.notification-badge');
    if (badge) {
        badge.textContent = count;
        badge.style.display = 'block';
    }
}

// API Helper Functions

function apiCall(url, method, data) {
    return fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').getAttribute('content')
        },
        body: data ? JSON.stringify(data) : null
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    });
}

function showSuccessMessage(message) {
    showAlert('success', message);
}

function showErrorMessage(message) {
    showAlert('danger', message);
}

function showAlert(type, message) {
    var alertContainer = document.querySelector('.alert-container');
    if (!alertContainer) {
        alertContainer = document.createElement('div');
        alertContainer.className = 'alert-container';
        document.body.appendChild(alertContainer);
    }
    
    var alert = document.createElement('div');
    alert.className = 'alert alert-' + type + ' alert-dismissible fade show';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    alertContainer.appendChild(alert);
    
    // Auto-remove after 5 seconds
    setTimeout(function() {
        alert.remove();
    }, 5000);
}

// Export functions for global access
window.BuildSmartCMS = {
    apiCall: apiCall,
    showSuccessMessage: showSuccessMessage,
    showErrorMessage: showErrorMessage,
    showAlert: showAlert
}; 