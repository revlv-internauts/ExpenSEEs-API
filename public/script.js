const API_BASE_URL = 'http://localhost:8080/api';
let token = null;

document.addEventListener('DOMContentLoaded', () => {
    const loginSection = document.getElementById('login-section');
    const dashboard = document.getElementById('dashboard');
    const loginBtn = document.getElementById('login-btn');
    const loginError = document.getElementById('login-error');
    const logoutBtn = document.getElementById('logout-btn');
    const createUserBtn = document.getElementById('create-user-btn');
    const createUserModal = document.getElementById('create-user-modal');
    const cancelCreateUser = document.getElementById('cancel-create-user');
    const submitCreateUser = document.getElementById('submit-create-user');
    const createBudgetBtn = document.getElementById('create-budget-btn');
    const createBudgetModal = document.getElementById('create-budget-modal');
    const cancelCreateBudget = document.getElementById('cancel-create-budget');
    const submitCreateBudget = document.getElementById('submit-create-budget');
    const createExpenseBtn = document.getElementById('create-expense-btn');
    const createExpenseModal = document.getElementById('create-expense-modal');
    const cancelCreateExpense = document.getElementById('cancel-create-expense');
    const submitCreateExpense = document.getElementById('submit-create-expense');
    const createFundBtn = document.getElementById('create-fund-btn');
    const createFundModal = document.getElementById('create-fund-modal');
    const cancelCreateFund = document.getElementById('cancel-create-fund');
    const submitCreateFund = document.getElementById('submit-create-fund');
    const mobileMenuBtn = document.getElementById('mobile-menu-btn');
    const sidebar = document.getElementById('sidebar');
    const sections = {
        users: document.getElementById('users'),
        budgets: document.getElementById('budgets'),
        expenses: document.getElementById('expenses'),
        funds: document.getElementById('funds'),
        reports: document.getElementById('reports')
    };
    const dashboardTitle = document.getElementById('dashboard-title');
    const loginForm = document.getElementById('login-form');

    // Mobile menu toggle
    mobileMenuBtn.addEventListener('click', () => {
        sidebar.classList.toggle('hidden');
    });

    // Navigation
    document.querySelectorAll('#sidebar a').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const sectionId = e.target.getAttribute('href').substring(1);
            if (!token) {
                alert('Please log in first');
                return;
            }
            Object.values(sections).forEach(section => section.classList.add('section-hidden'));
            sections[sectionId].classList.remove('section-hidden');
            dashboardTitle.textContent = `${sectionId.charAt(0).toUpperCase() + sectionId.slice(1)} Management`;
            if (window.innerWidth < 768) sidebar.classList.add('hidden');
            loadSectionData(sectionId);
        });
    });

    // Login
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        loginBtn.innerHTML = '<div class="loading-spinner"></div>';
        loginError.classList.add('hidden');
        try {
            const response = await fetch(`${API_BASE_URL}/auth/sign-in`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Origin': 'http://localhost:8000' },
                body: JSON.stringify({ usernameOrEmail: username, password })
            });
            const data = await response.json();
            console.log('Login response:', data); // Debug log
            if (response.ok) {
                if (data.access_token) {
                    token = data.access_token;
                    localStorage.setItem('access_token', token);
                    loginSection.classList.add('hidden');
                    dashboard.classList.remove('hidden');
                    sections.users.classList.remove('section-hidden');
                    dashboardTitle.textContent = 'User Management';
                    await loadUsers();
                } else {
                    loginError.textContent = 'Invalid response format or missing token';
                    loginError.classList.remove('hidden');
                }
            } else {
                loginError.textContent = data.message || 'Login failed';
                loginError.classList.remove('hidden');
            }
        } catch (error) {
            loginError.textContent = `Network error: ${error.message}`;
            loginError.classList.remove('hidden');
        } finally {
            loginBtn.innerHTML = 'Login';
        }
    });

    // Logout
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('access_token');
        token = null;
        loginSection.classList.remove('hidden');
        dashboard.classList.add('hidden');
        Object.values(sections).forEach(section => section.classList.add('section-hidden'));
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
    });

    // User Management
    createUserBtn.addEventListener('click', () => {
        if (!token) {
            alert('Please log in first');
            return;
        }
        createUserModal.classList.remove('hidden');
    });
    cancelCreateUser.addEventListener('click', () => {
        createUserModal.classList.add('hidden');
        document.getElementById('new-username').value = '';
        document.getElementById('new-email').value = '';
        document.getElementById('new-password').value = '';
    });
    submitCreateUser.addEventListener('click', async () => {
        const username = document.getElementById('new-username').value;
        const email = document.getElementById('new-email').value;
        const password = document.getElementById('new-password').value;
        submitCreateUser.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/admin/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ username, email, password })
            });
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'User created successfully');
                createUserModal.classList.add('hidden');
                document.getElementById('new-username').value = '';
                document.getElementById('new-email').value = '';
                document.getElementById('new-password').value = '';
                loadUsers();
            } else {
                alert(data.error || 'Failed to create user');
            }
        } catch (error) {
            alert('Error creating user: ' + error.message);
        } finally {
            submitCreateUser.innerHTML = 'Create';
        }
    });

    // Budget Management
    createBudgetBtn.addEventListener('click', () => {
        if (!token) {
            alert('Please log in first');
            return;
        }
        createBudgetModal.classList.remove('hidden');
        document.getElementById('budget-expenses').innerHTML = '';
    });
    document.getElementById('add-expense-btn').addEventListener('click', () => {
        const expenseDiv = document.createElement('div');
        expenseDiv.className = 'space-y-2 mt-2';
        expenseDiv.innerHTML = `
            <input type="text" placeholder="Category" class="w-full p-2 border rounded expense-category" required>
            <input type="number" placeholder="Quantity" step="1" class="w-full p-2 border rounded expense-quantity" value="1" required>
            <input type="number" placeholder="Amount per Unit" step="0.01" class="w-full p-2 border rounded expense-amount" required>
            <input type="text" placeholder="Remarks" class="w-full p-2 border rounded expense-remarks">
        `;
        document.getElementById('budget-expenses').appendChild(expenseDiv);
    });
    cancelCreateBudget.addEventListener('click', () => {
        createBudgetModal.classList.add('hidden');
        document.getElementById('budget-name').value = '';
        document.getElementById('budget-status').value = 'PENDING';
        document.getElementById('budget-expenses').innerHTML = '';
    });
    submitCreateBudget.addEventListener('click', async () => {
        const name = document.getElementById('budget-name').value;
        const status = document.getElementById('budget-status').value;
        const expenses = Array.from(document.querySelectorAll('#budget-expenses > div')).map(div => ({
            category: div.querySelector('.expense-category').value,
            quantity: parseInt(div.querySelector('.expense-quantity').value),
            amountPerUnit: parseFloat(div.querySelector('.expense-amount').value),
            remarks: div.querySelector('.expense-remarks').value
        }));
        submitCreateBudget.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/budgets`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ name, status: status.toUpperCase(), expenses })
            });
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'Budget created successfully');
                createBudgetModal.classList.add('hidden');
                document.getElementById('budget-name').value = '';
                document.getElementById('budget-status').value = 'PENDING';
                document.getElementById('budget-expenses').innerHTML = '';
                loadBudgets();
            } else {
                alert(data.error || 'Failed to create budget');
            }
        } catch (error) {
            alert('Error creating budget: ' + error.message);
        } finally {
            submitCreateBudget.innerHTML = 'Create';
        }
    });

    // Expense Management
    createExpenseBtn.addEventListener('click', () => {
        if (!token) {
            alert('Please log in first');
            return;
        }
        createExpenseModal.classList.remove('hidden');
    });
    cancelCreateExpense.addEventListener('click', () => {
        createExpenseModal.classList.add('hidden');
        document.getElementById('expense-category').value = '';
        document.getElementById('expense-amount').value = '';
        document.getElementById('expense-date').value = '';
        document.getElementById('expense-remarks').value = '';
        document.getElementById('expense-file').value = '';
    });
    submitCreateExpense.addEventListener('click', async () => {
        const category = document.getElementById('expense-category').value;
        const amount = parseFloat(document.getElementById('expense-amount').value);
        const dateOfTransaction = document.getElementById('expense-date').value;
        const remarks = document.getElementById('expense-remarks').value;
        const fileInput = document.getElementById('expense-file');
        submitCreateExpense.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const formData = new FormData();
            formData.append('category', category);
            formData.append('amount', amount);
            formData.append('dateOfTransaction', dateOfTransaction);
            formData.append('remarks', remarks);
            if (fileInput.files.length > 0) {
                for (let file of fileInput.files) {
                    formData.append('files', file);
                }
            }
            const response = await fetch(`${API_BASE_URL}/expenses`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'Expense created successfully');
                createExpenseModal.classList.add('hidden');
                document.getElementById('expense-category').value = '';
                document.getElementById('expense-amount').value = '';
                document.getElementById('expense-date').value = '';
                document.getElementById('expense-remarks').value = '';
                document.getElementById('expense-file').value = '';
                loadExpenses();
            } else {
                alert(data.error || 'Failed to create expense');
            }
        } catch (error) {
            alert('Error creating expense: ' + error.message);
        } finally {
            submitCreateExpense.innerHTML = 'Create';
        }
    });

    // Fund Requests
    createFundBtn.addEventListener('click', () => {
        if (!token) {
            alert('Please log in first');
            return;
        }
        createFundModal.classList.remove('hidden');
        document.getElementById('fund-expenses').innerHTML = '';
    });
    document.getElementById('add-fund-expense-btn').addEventListener('click', () => {
        const expenseDiv = document.createElement('div');
        expenseDiv.className = 'space-y-2 mt-2';
        expenseDiv.innerHTML = `
            <input type="text" placeholder="Category" class="w-full p-2 border rounded fund-expense-category" required>
            <input type="number" placeholder="Quantity" step="1" class="w-full p-2 border rounded fund-expense-quantity" value="1" required>
            <input type="number" placeholder="Amount per Unit" step="0.01" class="w-full p-2 border rounded fund-expense-amount" required>
            <input type="text" placeholder="Remarks" class="w-full p-2 border rounded fund-expense-remarks">
        `;
        document.getElementById('fund-expenses').appendChild(expenseDiv);
    });
    cancelCreateFund.addEventListener('click', () => {
        createFundModal.classList.add('hidden');
        document.getElementById('fund-name').value = '';
        document.getElementById('fund-status').value = 'PENDING';
        document.getElementById('fund-expenses').innerHTML = '';
    });
    submitCreateFund.addEventListener('click', async () => {
        const name = document.getElementById('fund-name').value;
        const status = document.getElementById('fund-status').value;
        const expenses = Array.from(document.querySelectorAll('#fund-expenses > div')).map(div => ({
            category: div.querySelector('.fund-expense-category').value,
            quantity: parseInt(div.querySelector('.fund-expense-quantity').value),
            amountPerUnit: parseFloat(div.querySelector('.fund-expense-amount').value),
            remarks: div.querySelector('.fund-expense-remarks').value
        }));
        submitCreateFund.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/funds/request`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ name, status: status.toUpperCase(), expenses })
            });
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'Fund request submitted successfully');
                createFundModal.classList.add('hidden');
                document.getElementById('fund-name').value = '';
                document.getElementById('fund-status').value = 'PENDING';
                document.getElementById('fund-expenses').innerHTML = '';
                loadFunds();
            } else {
                alert(data.error || 'Failed to submit fund request');
            }
        } catch (error) {
            alert('Error submitting fund request: ' + error.message);
        } finally {
            submitCreateFund.innerHTML = 'Create';
        }
    });

    // Load Section Data
    async function loadSectionData(sectionId) {
        if (!token) return;
        switch (sectionId) {
            case 'users': loadUsers(); break;
            case 'budgets': loadBudgets(); break;
            case 'expenses': loadExpenses(); break;
            case 'funds': loadFunds(); break;
            case 'reports': loadReports(); break;
        }
    }

    // Load Users
    async function loadUsers() {
        const userList = document.getElementById('user-list');
        userList.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/admin/users`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await response.json();
            userList.innerHTML = '';
            if (data.data) {
                data.data.forEach(user => {
                    const userCard = document.createElement('div');
                    userCard.className = 'user-card bg-gray-100 p-4 rounded-lg shadow';
                    userCard.innerHTML = `
                        <div class="flex justify-between items-center">
                            <div>
                                <p class="font-bold">${user.username}</p>
                                <p>${user.email}</p>
                            </div>
                            <button class="delete-user bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700" data-id="${user.userId}">Delete</button>
                        </div>
                    `;
                    userList.appendChild(userCard);
                    userCard.querySelector('.delete-user').addEventListener('click', async (e) => {
                        const userId = e.target.dataset.id;
                        if (confirm('Are you sure you want to delete this user?')) {
                            try {
                                const response = await fetch(`${API_BASE_URL}/admin/users/${userId}`, {
                                    method: 'DELETE',
                                    headers: { 'Authorization': `Bearer ${token}` }
                                });
                                const data = await response.json();
                                if (response.ok) {
                                    alert(data.message || 'User deleted successfully');
                                    loadUsers();
                                } else {
                                    alert(data.error || 'Failed to delete user');
                                }
                            } catch (error) {
                                alert('Error deleting user: ' + error.message);
                            }
                        }
                    });
                });
            }
        } catch (error) {
            userList.innerHTML = '<p class="text-red-600">Error loading users</p>';
        }
    }

    // Load Budgets
    async function loadBudgets() {
        const budgetList = document.getElementById('budget-list');
        budgetList.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/budgets`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const budgets = await response.json();
            budgetList.innerHTML = '';
            budgets.forEach(budget => {
                const budgetCard = document.createElement('div');
                budgetCard.className = 'budget-card bg-gray-100 p-4 rounded-lg shadow';
                budgetCard.innerHTML = `
                    <div class="flex justify-between items-center">
                        <div>
                            <p class="font-bold">${budget.name}</p>
                            <p>Total: $${budget.total.toFixed(2)}</p>
                            <p>Status: ${budget.status}</p>
                        </div>
                        <select class="status-select bg-white border p-2 rounded" data-id="${budget.budgetId}">
                            <option value="PENDING" ${budget.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                            <option value="APPROVED" ${budget.status === 'APPROVED' ? 'selected' : ''}>Approved</option>
                            <option value="DENIED" ${budget.status === 'DENIED' ? 'selected' : ''}>Denied</option>
                        </select>
                    </div>
                `;
                budgetList.appendChild(budgetCard);
                budgetCard.querySelector('.status-select').addEventListener('change', async (e) => {
                    const budgetId = e.target.dataset.id;
                    const status = e.target.value;
                    try {
                        const response = await fetch(`${API_BASE_URL}/budgets/${budgetId}/status`, {
                            method: 'PUT',
                            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                            body: JSON.stringify(status)
                        });
                        const data = await response.json();
                        if (response.ok) {
                            alert(data.message || 'Budget status updated');
                            loadBudgets();
                        } else {
                            alert(data.error || 'Failed to update budget status');
                        }
                    } catch (error) {
                        alert('Error updating budget status: ' + error.message);
                    }
                });
            });
        } catch (error) {
            budgetList.innerHTML = '<p class="text-red-600">Error loading budgets</p>';
        }
    }

    // Load Expenses
    async function loadExpenses() {
        const expenseList = document.getElementById('expense-list');
        expenseList.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/expenses`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const expenses = await response.json();
            expenseList.innerHTML = '';
            expenses.forEach(expense => {
                const expenseCard = document.createElement('div');
                expenseCard.className = 'expense-card bg-gray-100 p-4 rounded-lg shadow';
                expenseCard.innerHTML = `
                    <div class="flex justify-between items-center">
                        <div>
                            <p class="font-bold">${expense.category}</p>
                            <p>Amount: $${expense.amount.toFixed(2)}</p>
                            <p>Date: ${expense.dateOfTransaction}</p>
                            <p>Remarks: ${expense.remarks || 'N/A'}</p>
                            ${expense.imagePaths && expense.imagePaths.length > 0 ? `<img src="${API_BASE_URL}/expenses/${expense.expenseId}/images?index=0" alt="Expense Image" class="mt-2 w-20 h-20 object-cover">` : ''}
                        </div>
                        <button class="delete-expense bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700" data-id="${expense.expenseId}">Delete</button>
                    </div>
                `;
                expenseList.appendChild(expenseCard);
                expenseCard.querySelector('.delete-expense').addEventListener('click', async (e) => {
                    const expenseId = e.target.dataset.id;
                    if (confirm('Are you sure you want to delete this expense?')) {
                        try {
                            const response = await fetch(`${API_BASE_URL}/expenses/${expenseId}`, {
                                method: 'DELETE',
                                headers: { 'Authorization': `Bearer ${token}` }
                            });
                            const data = await response.json();
                            if (response.ok) {
                                alert(data.message || 'Expense deleted successfully');
                                loadExpenses();
                            } else {
                                alert(data.error || 'Failed to delete expense');
                            }
                        } catch (error) {
                            alert('Error deleting expense: ' + error.message);
                        }
                    }
                });
            });
        } catch (error) {
            expenseList.innerHTML = '<p class="text-red-600">Error loading expenses</p>';
        }
    }

    // Load Funds
    async function loadFunds() {
        const fundList = document.getElementById('fund-list');
        fundList.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/budgets`, { // Using budgets as proxy for funds
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const funds = await response.json();
            fundList.innerHTML = '';
            funds.forEach(fund => {
                const fundCard = document.createElement('div');
                fundCard.className = 'fund-card bg-gray-100 p-4 rounded-lg shadow';
                fundCard.innerHTML = `
                    <div class="flex justify-between items-center">
                        <div>
                            <p class="font-bold">${fund.name}</p>
                            <p>Total: $${fund.total.toFixed(2)}</p>
                            <p>Status: ${fund.status}</p>
                        </div>
                    </div>
                `;
                fundList.appendChild(fundCard);
            });
        } catch (error) {
            fundList.innerHTML = '<p class="text-red-600">Error loading funds</p>';
        }
    }

    // Load Reports
    async function loadReports() {
        const reportContent = document.getElementById('report-content');
        reportContent.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const response = await fetch(`${API_BASE_URL}/reports/liquidation`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await response.json();
            reportContent.innerHTML = `
                <div class="report-card bg-gray-100 p-4 rounded-lg shadow">
                    <p class="font-bold">Total Expenses: $${data.totalExpenses.toFixed(2)}</p>
                </div>
                <div class="mt-4">
                    <h4 class="text-lg font-semibold">Budgets</h4>
                    ${data.budgets.map(budget => `
                        <div class="report-card bg-gray-100 p-4 rounded-lg shadow mt-2">
                            <p class="font-bold">${budget.name}</p>
                            <p>Total: $${budget.total.toFixed(2)}</p>
                            <p>Status: ${budget.status}</p>
                        </div>
                    `).join('')}
                </div>
                <div class="mt-4">
                    <h4 class="text-lg font-semibold">Expenses</h4>
                    ${data.expenses.map(expense => `
                        <div class="report-card bg-gray-100 p-4 rounded-lg shadow mt-2">
                            <p class="font-bold">${expense.category}</p>
                            <p>Amount: $${expense.amount.toFixed(2)}</p>
                            <p>Date: ${expense.dateOfTransaction}</p>
                            <p>Remarks: ${expense.remarks || 'N/A'}</p>
                        </div>
                    `).join('')}
                </div>
            `;
        } catch (error) {
            reportContent.innerHTML = '<p class="text-red-600">Error loading reports</p>';
        }
    }
});