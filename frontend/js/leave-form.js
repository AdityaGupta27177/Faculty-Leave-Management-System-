/**
 * leave-form.js — Leave application form logic
 * Faculty Leave Management System
 */

let leaveTypes = [];
let holidays = [];
let balances = [];

document.addEventListener('DOMContentLoaded', async () => {
    const user = await requireAuth(['FACULTY', 'HOD', 'ADMIN']);
    if (!user) return;

    // Load leave types and holidays for duration calculation
    await Promise.all([
        loadLeaveTypes(),
        loadHolidays(),
        loadBalances()
    ]);

    // Set up event listeners
    setupFormListeners();

    // Check if editing a draft
    const params = new URLSearchParams(window.location.search);
    const editId = params.get('edit');
    if (editId) {
        await loadDraft(editId);
    }
});

async function loadLeaveTypes() {
    try {
        leaveTypes = await API.get('/api/leave-types');
        const select = document.getElementById('leaveType');
        leaveTypes.forEach(lt => {
            const option = document.createElement('option');
            option.value = lt.id;
            option.textContent = `${lt.name} (${lt.code})`;
            option.dataset.description = lt.description || '';
            option.dataset.requiresDocument = lt.requires_document || false;
            option.dataset.code = lt.code;
            select.appendChild(option);
        });
    } catch (e) {
        showToast('error', 'Error', 'Unable to load leave types.');
    }
}

async function loadHolidays() {
    try {
        holidays = await API.get('/api/holidays');
    } catch (e) {
        holidays = [];
    }
}

async function loadBalances() {
    try {
        balances = await API.get('/api/leave/balances');
    } catch (e) {
        balances = [];
    }
}

function setupFormListeners() {
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');
    const halfStart = document.getElementById('halfDayStart');
    const halfEnd = document.getElementById('halfDayEnd');
    const leaveType = document.getElementById('leaveType');
    const reason = document.getElementById('reason');

    // Calculate duration on date change
    [startDate, endDate, halfStart, halfEnd].forEach(el => {
        el.addEventListener('change', calculateDuration);
    });

    // Leave type change — show description and document requirement
    leaveType.addEventListener('change', () => {
        const selected = leaveType.options[leaveType.selectedIndex];
        const help = document.getElementById('leaveTypeHelp');
        const docRequired = document.getElementById('docRequired');

        help.textContent = selected.dataset.description || '';

        if (selected.dataset.requiresDocument === 'true') {
            docRequired.classList.remove('hidden');
        } else {
            docRequired.classList.add('hidden');
        }

        calculateDuration();
    });

    // Character count for reason
    reason.addEventListener('input', () => {
        document.getElementById('charCount').textContent = reason.value.length;
    });

    // Set minimum start date to today
    startDate.min = Utils.today();
}

function calculateDuration() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const halfStart = document.getElementById('halfDayStart').checked;
    const halfEnd = document.getElementById('halfDayEnd').checked;
    const display = document.getElementById('durationDisplay');
    const warning = document.getElementById('balanceWarning');

    if (!startDate || !endDate) {
        display.textContent = '— days';
        warning.style.display = 'none';
        return;
    }

    const days = Utils.calculateWorkingDays(startDate, endDate, halfStart, halfEnd, holidays);
    display.textContent = `${days} working day${days !== 1 ? 's' : ''}`;

    // Check balance
    const leaveTypeId = document.getElementById('leaveType').value;
    if (leaveTypeId && balances.length > 0) {
        const balance = balances.find(b => b.leave_type_id == leaveTypeId);
        if (balance) {
            const remaining = balance.total_entitled - balance.used - balance.pending;
            if (days > remaining) {
                warning.textContent = `⚠ Insufficient balance. Available: ${remaining} days.`;
                warning.style.display = 'block';
            } else {
                warning.style.display = 'none';
            }
        }
    }
}

async function loadDraft(id) {
    try {
        const app = await API.get(`/api/leave/${id}`);
        if (app.status !== 'DRAFT' && app.status !== 'RETURNED') {
            showToast('warning', 'Not Editable', 'This application cannot be edited.');
            return;
        }

        document.getElementById('leaveType').value = app.leave_type_id;
        document.getElementById('startDate').value = app.start_date;
        document.getElementById('endDate').value = app.end_date;
        document.getElementById('halfDayStart').checked = app.half_day_start;
        document.getElementById('halfDayEnd').checked = app.half_day_end;
        document.getElementById('reason').value = app.reason || '';
        document.getElementById('charCount').textContent = (app.reason || '').length;

        // Store the ID for updating
        document.getElementById('leaveForm').dataset.editId = id;

        calculateDuration();
    } catch (e) {
        showToast('error', 'Error', 'Unable to load draft.');
    }
}

async function submitLeave(status) {
    const form = document.getElementById('leaveForm');
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const halfStart = document.getElementById('halfDayStart').checked;
    const halfEnd = document.getElementById('halfDayEnd').checked;

    const data = {
        leave_type_id: parseInt(document.getElementById('leaveType').value),
        start_date: startDate,
        end_date: endDate,
        half_day_start: halfStart,
        half_day_end: halfEnd,
        total_days: Utils.calculateWorkingDays(startDate, endDate, halfStart, halfEnd, holidays),
        reason: document.getElementById('reason').value.trim(),
        status: status
    };

    // Find the leave type code for validation
    const lt = leaveTypes.find(t => t.id == data.leave_type_id);
    if (lt) data.leave_type_code = lt.code;

    // Client-side validation (only for submission, not drafts)
    if (status === 'SUBMITTED') {
        const validation = Validator.validateLeaveApplication(data);
        if (!validation.valid) {
            Validator.showErrors(validation.errors, form);
            showToast('warning', 'Validation Error', 'Please fix the highlighted fields.');
            return;
        }
    }

    // Confirm submission
    if (status === 'SUBMITTED') {
        const confirmed = await Utils.confirm(
            'Submit Application?',
            `You are about to submit a ${data.total_days}-day leave application. This will be sent to your approver for review.`,
            'Submit',
            'success'
        );
        if (!confirmed) return;
    }

    // Disable buttons
    const submitBtn = document.getElementById('submitBtn');
    const draftBtn = document.getElementById('saveDraftBtn');
    submitBtn.disabled = true;
    draftBtn.disabled = true;

    try {
        const editId = form.dataset.editId;
        let result;

        if (editId) {
            result = await API.put(`/api/leave/${editId}`, data);
        } else {
            result = await API.post('/api/leave/apply', data);
        }

        if (status === 'DRAFT') {
            showToast('success', 'Draft Saved', 'Your application has been saved as a draft.');
            // Set edit ID for future saves
            if (result.id) form.dataset.editId = result.id;
        } else {
            showToast('success', 'Application Submitted', 'Your leave application has been submitted for approval.');
            setTimeout(() => {
                window.location.href = 'my-leaves.html';
            }, 1500);
        }
    } catch (e) {
        showToast('error', 'Error', e.message || 'Unable to submit application.');
    } finally {
        submitBtn.disabled = false;
        draftBtn.disabled = false;
    }
}
