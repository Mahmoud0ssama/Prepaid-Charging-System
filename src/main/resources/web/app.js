document.addEventListener('DOMContentLoaded', () => {
    // Global State
    let allUsers = [];
    let allCdrs = [];

    // DOM Elements
    const usersTableBody = document.getElementById('users-table-body');
    const cdrsTableBody = document.getElementById('cdrs-table-body');
    
    // Stats elements
    const lblTotalUsers = document.getElementById('lbl-total-users');
    const lblTotalBalance = document.getElementById('lbl-total-balance');
    const lblTotalCalls = document.getElementById('lbl-total-calls');
    
    // Search element
    const searchMsisdn = document.getElementById('search-msisdn');
    
    // Modals & Forms
    const modalAddUser = document.getElementById('modal-add-user');
    const btnOpenAddModal = document.getElementById('btn-open-add-modal');
    const btnCloseAdd = document.getElementById('btn-close-add');
    const btnCancelAdd = document.getElementById('btn-cancel-add');
    const formAddUser = document.getElementById('form-add-user');
    
    const modalEditUser = document.getElementById('modal-edit-user');
    const btnCloseEdit = document.getElementById('btn-close-edit');
    const btnCancelEdit = document.getElementById('btn-cancel-edit');
    const formEditUser = document.getElementById('form-edit-user');
    
    const btnRefreshCdrs = document.getElementById('btn-refresh-cdrs');

    // Setup Toast Notification System
    const toastContainer = document.getElementById('toast-container');
    function showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        const icon = document.createElement('i');
        icon.className = type === 'success' ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-exclamation';
        
        const text = document.createElement('span');
        text.textContent = message;
        
        toast.appendChild(icon);
        toast.appendChild(text);
        toastContainer.appendChild(toast);
        
        // Force reflow
        toast.offsetHeight;
        
        toast.classList.add('show');
        
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    // Modal Control Functions
    function openModal(modal) {
        modal.classList.add('active');
    }
    
    function closeModal(modal) {
        modal.classList.remove('active');
    }

    // Event Listeners for Modals
    btnOpenAddModal.addEventListener('click', () => {
        formAddUser.reset();
        openModal(modalAddUser);
    });
    btnCloseAdd.addEventListener('click', () => closeModal(modalAddUser));
    btnCancelAdd.addEventListener('click', () => closeModal(modalAddUser));
    
    btnCloseEdit.addEventListener('click', () => closeModal(modalEditUser));
    btnCancelEdit.addEventListener('click', () => closeModal(modalEditUser));

    // Close modals on clicking outside the container
    window.addEventListener('click', (e) => {
        if (e.target === modalAddUser) closeModal(modalAddUser);
        if (e.target === modalEditUser) closeModal(modalEditUser);
    });

    // API Calls
    async function fetchUsers() {
        try {
            const response = await fetch('/api/users');
            if (!response.ok) throw new Error('Failed to load subscribers');
            allUsers = await response.json();
            renderUsers(allUsers);
            updateStats();
        } catch (error) {
            showToast(error.message, 'error');
            usersTableBody.innerHTML = `<tr><td colspan="4" class="table-empty">${error.message}</td></tr>`;
        }
    }

    async function fetchCdrs() {
        try {
            const response = await fetch('/api/cdrs');
            if (!response.ok) throw new Error('Failed to load call logs');
            allCdrs = await response.json();
            renderCdrs(allCdrs);
            updateStats();
        } catch (error) {
            cdrsTableBody.innerHTML = `<tr><td colspan="5" class="table-empty">${error.message}</td></tr>`;
        }
    }

    // Rendering functions
    function renderUsers(users) {
        if (users.length === 0) {
            usersTableBody.innerHTML = '<tr><td colspan="4" class="table-empty"><i class="fa-solid fa-user-slash"></i> No subscribers found</td></tr>';
            return;
        }

        usersTableBody.innerHTML = users.map(user => {
            const lowBalance = parseFloat(user.balance) <= 5.0 ? 'low' : '';
            return `
                <tr id="user-row-${user.id}">
                    <td><strong>#${user.id}</strong></td>
                    <td><i class="fa-solid fa-hashtag text-secondary icon-margin"></i>${user.msisdn}</td>
                    <td>
                        <span class="balance-badge ${lowBalance}">
                            ${parseFloat(user.balance).toFixed(2)} L.E.
                        </span>
                    </td>
                    <td class="text-right">
                        <div class="action-buttons-group">
                            <button class="btn btn-icon btn-edit-outline" onclick="window.editUser(${user.id}, '${user.msisdn}', ${user.balance})" title="Recharge / Edit">
                                <i class="fa-solid fa-bolt"></i>
                            </button>
                            <button class="btn btn-icon btn-danger-outline" onclick="window.deleteUser(${user.id}, '${user.msisdn}')" title="Delete">
                                <i class="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    }

    function renderCdrs(cdrs) {
        if (cdrs.length === 0) {
            cdrsTableBody.innerHTML = '<tr><td colspan="5" class="table-empty"><i class="fa-solid fa-phone-slash"></i> No call logs found</td></tr>';
            return;
        }

        cdrsTableBody.innerHTML = cdrs.map(cdr => {
            const isSuccess = cdr.callResult.toLowerCase().includes('normal') || cdr.callResult.toLowerCase().includes('clearing');
            const statusClass = isSuccess ? 'success' : 'failed';
            const statusIcon = isSuccess ? '<i class="fa-solid fa-circle-check"></i>' : '<i class="fa-solid fa-circle-xmark"></i>';
            return `
                <tr>
                    <td><strong>${cdr.msisdn}</strong></td>
                    <td>${cdr.duration} min</td>
                    <td><span class="balance-value">${parseFloat(cdr.callCost).toFixed(2)} L.E.</span></td>
                    <td>
                        <span class="status-badge ${statusClass}">
                            ${statusIcon} ${cdr.callResult}
                        </span>
                    </td>
                    <td><span class="text-secondary">${parseFloat(cdr.balanceAfterCall).toFixed(2)} L.E.</span></td>
                </tr>
            `;
        }).join('');
    }

    function updateStats() {
        lblTotalUsers.textContent = allUsers.length;
        
        const sumBalance = allUsers.reduce((sum, user) => sum + parseFloat(user.balance), 0);
        lblTotalBalance.textContent = `${sumBalance.toFixed(2)} L.E.`;
        
        lblTotalCalls.textContent = allCdrs.length;
    }

    // Local filter/search
    searchMsisdn.addEventListener('input', (e) => {
        const query = e.target.value.trim().toLowerCase();
        if (!query) {
            renderUsers(allUsers);
            return;
        }
        const filtered = allUsers.filter(u => u.msisdn.toLowerCase().includes(query));
        renderUsers(filtered);
    });

    // Refresh CDRs manually
    btnRefreshCdrs.addEventListener('click', () => {
        fetchCdrs();
        showToast('Call history logs updated.', 'success');
    });

    // Form Submissions
    formAddUser.addEventListener('submit', async (e) => {
        e.preventDefault();
        const msisdn = document.getElementById('add-msisdn').value.trim();
        const balance = parseFloat(document.getElementById('add-balance').value);

        try {
            const response = await fetch('/api/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ msisdn, balance })
            });

            const resData = await response.json();
            if (!response.ok) throw new Error(resData.error || 'Failed to add user');

            showToast('Subscriber added successfully!', 'success');
            closeModal(modalAddUser);
            fetchUsers();
        } catch (error) {
            showToast(error.message, 'error');
        }
    });

    formEditUser.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = parseInt(document.getElementById('edit-id').value);
        const msisdn = document.getElementById('edit-msisdn').value.trim();
        const balance = parseFloat(document.getElementById('edit-balance').value);

        try {
            const response = await fetch('/api/users', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id, msisdn, balance })
            });

            const resData = await response.json();
            if (!response.ok) throw new Error(resData.error || 'Failed to update subscriber');

            showToast('Subscriber updated successfully!', 'success');
            closeModal(modalEditUser);
            fetchUsers();
        } catch (error) {
            showToast(error.message, 'error');
        }
    });

    // Expose functions globally for table onclick handlers
    window.editUser = function(id, msisdn, balance) {
        document.getElementById('edit-id').value = id;
        document.getElementById('edit-msisdn').value = msisdn;
        document.getElementById('edit-balance').value = balance;
        openModal(modalEditUser);
    };

    window.deleteUser = async function(id, msisdn) {
        if (!confirm(`Are you sure you want to delete subscriber ${msisdn}?`)) return;

        try {
            const response = await fetch(`/api/users?id=${id}`, {
                method: 'DELETE'
            });

            const resData = await response.json();
            if (!response.ok) throw new Error(resData.error || 'Failed to delete user');

            showToast(`Subscriber ${msisdn} deleted successfully`, 'success');
            fetchUsers();
        } catch (error) {
            showToast(error.message, 'error');
        }
    };

    // Initial Fetch & Auto polling every 5 seconds for real-time MSC charging visualization
    fetchUsers();
    fetchCdrs();
    setInterval(() => {
        fetchUsers();
        fetchCdrs();
    }, 5000);
});
