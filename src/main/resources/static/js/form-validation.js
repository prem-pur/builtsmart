/**
 * Form Validation & Enhancement System
 * Adds inline validation and better UX to forms
 */

// Initialize form validation
document.addEventListener('DOMContentLoaded', function() {
    const forms = document.querySelectorAll('form[data-validate]');
    
    forms.forEach(form => {
        // Add novalidate to use custom validation
        form.setAttribute('novalidate', 'true');
        
        // Add validation on submit
        form.addEventListener('submit', function(e) {
            if (!validateForm(this)) {
                e.preventDefault();
                toast.error('Validation Error', 'Please check the form for errors');
            } else {
                // Add loading state to submit button
                const submitBtn = this.querySelector('button[type="submit"]');
                if (submitBtn) {
                    submitBtn.classList.add('btn-loading');
                    submitBtn.disabled = true;
                }
            }
        });
        
        // Add real-time validation to inputs
        const inputs = form.querySelectorAll('input, textarea, select');
        inputs.forEach(input => {
            input.addEventListener('blur', function() {
                validateInput(this);
            });
            
            input.addEventListener('input', function() {
                // Clear error on input
                if (this.classList.contains('is-invalid')) {
                    this.classList.remove('is-invalid');
                    const errorMsg = this.parentElement.querySelector('.invalid-feedback');
                    if (errorMsg) errorMsg.remove();
                }
            });
        });
    });
});

/**
 * Validate entire form
 */
function validateForm(form) {
    let isValid = true;
    const inputs = form.querySelectorAll('input, textarea, select');
    
    inputs.forEach(input => {
        if (!validateInput(input)) {
            isValid = false;
        }
    });
    
    return isValid;
}

/**
 * Validate single input
 */
function validateInput(input) {
    const value = input.value.trim();
    const type = input.type;
    let isValid = true;
    let errorMessage = '';
    
    // Required field
    if (input.hasAttribute('required') && !value) {
        isValid = false;
        errorMessage = 'This field is required';
    }
    
    // Email validation
    if (type === 'email' && value) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            isValid = false;
            errorMessage = 'Please enter a valid email address';
        }
    }
    
    // Min length
    if (input.hasAttribute('minlength') && value) {
        const minLength = parseInt(input.getAttribute('minlength'));
        if (value.length < minLength) {
            isValid = false;
            errorMessage = `Minimum ${minLength} characters required`;
        }
    }
    
    // Max length
    if (input.hasAttribute('maxlength') && value) {
        const maxLength = parseInt(input.getAttribute('maxlength'));
        if (value.length > maxLength) {
            isValid = false;
            errorMessage = `Maximum ${maxLength} characters allowed`;
        }
    }
    
    // Number validation
    if (type === 'number' && value) {
        if (input.hasAttribute('min') && parseFloat(value) < parseFloat(input.getAttribute('min'))) {
            isValid = false;
            errorMessage = `Minimum value is ${input.getAttribute('min')}`;
        }
        if (input.hasAttribute('max') && parseFloat(value) > parseFloat(input.getAttribute('max'))) {
            isValid = false;
            errorMessage = `Maximum value is ${input.getAttribute('max')}`;
        }
    }
    
    // Update UI
    if (!isValid) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        
        // Remove existing error message
        const existingError = input.parentElement.querySelector('.invalid-feedback');
        if (existingError) existingError.remove();
        
        // Add error message
        const errorDiv = document.createElement('div');
        errorDiv.className = 'invalid-feedback d-block';
        errorDiv.textContent = errorMessage;
        input.parentElement.appendChild(errorDiv);
    } else if (value) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        const existingError = input.parentElement.querySelector('.invalid-feedback');
        if (existingError) existingError.remove();
    }
    
    return isValid;
}

/**
 * Add loading state to any button
 */
function setButtonLoading(button, isLoading) {
    if (isLoading) {
        button.classList.add('btn-loading');
        button.disabled = true;
        button.dataset.originalText = button.innerHTML;
    } else {
        button.classList.remove('btn-loading');
        button.disabled = false;
        if (button.dataset.originalText) {
            button.innerHTML = button.dataset.originalText;
        }
    }
}

// Make globally available
window.setButtonLoading = setButtonLoading;
window.validateForm = validateForm;
