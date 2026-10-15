/**
 * validation.js — Form validation utilities
 * Faculty Leave Management System
 *
 * Provides client-side validation for leave applications and admin forms.
 * The backend ALSO validates all data independently.
 */

const Validator = {
    /**
     * Validate a leave application form
     * @param {object} data - form data
     * @returns {{ valid: boolean, errors: object }}
     */
    validateLeaveApplication(data) {
        const errors = {};

        // Leave type
        if (!data.leave_type_id) {
            errors.leave_type_id = 'Please select a leave type.';
        }

        // Start date
        if (!data.start_date) {
            errors.start_date = 'Start date is required.';
        } else {
            const start = new Date(data.start_date);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            // Allow past dates only for sick leave
            if (start < today && data.leave_type_code !== 'SL') {
                errors.start_date = 'Start date cannot be in the past (except for Sick Leave).';
            }
        }

        // End date
        if (!data.end_date) {
            errors.end_date = 'End date is required.';
        } else if (data.start_date) {
            const start = new Date(data.start_date);
            const end = new Date(data.end_date);
            if (end < start) {
                errors.end_date = 'End date cannot be before start date.';
            }
            // Max leave duration check (configurable but default 60 days)
            const diffDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;
            if (diffDays > 180) {
                errors.end_date = 'Leave duration cannot exceed 180 calendar days.';
            }
        }

        // Reason
        if (!data.reason || data.reason.trim().length < 10) {
            errors.reason = 'Please provide a reason (at least 10 characters).';
        }
        if (data.reason && data.reason.length > 1000) {
            errors.reason = 'Reason must be less than 1000 characters.';
        }

        // Total days check
        if (data.total_days !== undefined && data.total_days <= 0) {
            errors.total_days = 'Leave duration must be at least 0.5 days.';
        }

        return {
            valid: Object.keys(errors).length === 0,
            errors
        };
    },

    /**
     * Validate user/faculty form
     * @param {object} data
     * @returns {{ valid: boolean, errors: object }}
     */
    validateUser(data) {
        const errors = {};

        if (!data.name || data.name.trim().length < 2) {
            errors.name = 'Name is required (at least 2 characters).';
        }

        if (!data.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
            errors.email = 'A valid email address is required.';
        }

        if (!data.department_id) {
            errors.department_id = 'Please select a department.';
        }

        if (!data.designation || data.designation.trim().length < 2) {
            errors.designation = 'Designation is required.';
        }

        if (!data.role || !['FACULTY', 'HOD', 'ADMIN'].includes(data.role)) {
            errors.role = 'Please select a valid role.';
        }

        return {
            valid: Object.keys(errors).length === 0,
            errors
        };
    },

    /**
     * Validate a leave type form
     * @param {object} data
     * @returns {{ valid: boolean, errors: object }}
     */
    validateLeaveType(data) {
        const errors = {};

        if (!data.name || data.name.trim().length < 2) {
            errors.name = 'Leave type name is required.';
        }

        if (!data.code || data.code.trim().length < 2) {
            errors.code = 'Leave type code is required (2+ characters).';
        }

        return {
            valid: Object.keys(errors).length === 0,
            errors
        };
    },

    /**
     * Validate a department form
     * @param {object} data
     * @returns {{ valid: boolean, errors: object }}
     */
    validateDepartment(data) {
        const errors = {};

        if (!data.name || data.name.trim().length < 2) {
            errors.name = 'Department name is required.';
        }

        if (!data.code || data.code.trim().length < 2) {
            errors.code = 'Department code is required (2+ characters).';
        }

        return {
            valid: Object.keys(errors).length === 0,
            errors
        };
    },

    /**
     * Validate approval action
     * @param {object} data
     * @returns {{ valid: boolean, errors: object }}
     */
    validateApproval(data) {
        const errors = {};

        if (!data.action || !['APPROVED', 'REJECTED', 'RETURNED'].includes(data.action)) {
            errors.action = 'Please select a valid action.';
        }

        if (data.action === 'REJECTED' && (!data.comments || data.comments.trim().length < 5)) {
            errors.comments = 'Please provide a reason for rejection (at least 5 characters).';
        }

        return {
            valid: Object.keys(errors).length === 0,
            errors
        };
    },

    /**
     * Show validation errors on a form
     * @param {object} errors - { fieldName: errorMessage }
     * @param {HTMLFormElement} form
     */
    showErrors(errors, form) {
        // Clear previous errors
        form.querySelectorAll('.form-error').forEach(el => el.remove());
        form.querySelectorAll('.error').forEach(el => el.classList.remove('error'));

        // Show new errors
        for (const [field, message] of Object.entries(errors)) {
            const input = form.querySelector(`[name="${field}"]`);
            if (input) {
                input.classList.add('error');
                const errorEl = document.createElement('div');
                errorEl.className = 'form-error';
                errorEl.innerHTML = `⚠ ${Utils.escapeHtml(message)}`;
                input.parentElement.appendChild(errorEl);
            }
        }
    },

    /**
     * Clear all validation errors from a form
     * @param {HTMLFormElement} form
     */
    clearErrors(form) {
        form.querySelectorAll('.form-error').forEach(el => el.remove());
        form.querySelectorAll('.error').forEach(el => el.classList.remove('error'));
    },

    /**
     * Validate a single field and show/clear error
     * @param {HTMLElement} input
     * @param {string} errorMessage - empty string if valid
     */
    validateField(input, errorMessage) {
        // Remove existing error
        const existingError = input.parentElement.querySelector('.form-error');
        if (existingError) existingError.remove();
        input.classList.remove('error');

        if (errorMessage) {
            input.classList.add('error');
            const errorEl = document.createElement('div');
            errorEl.className = 'form-error';
            errorEl.innerHTML = `⚠ ${Utils.escapeHtml(errorMessage)}`;
            input.parentElement.appendChild(errorEl);
        }
    }
};
