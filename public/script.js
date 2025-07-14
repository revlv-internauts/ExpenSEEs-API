const SERVER_URL = "http://152.42.192.226:8080"; // Change to "http://152.42.192.226:8080" for server testing
                                            // Change to "http://localhost:8080" for local testing
let users = [];
let expenses = [];
let budgetRequests = [];
let notifications = [];
let token = null;
let selectedUserIndex = null;
let userDetailsPopup = null;
let selectedBudgetIndex = null;
let zoomLevel = 1;
let panX = 0, panY = 0;
let isDragging = false;
let startX = 0, startY = 0;
let categoryChart, userChart;
let currentUser = null;
let currentUserId = null; // To store the current user's ID

// =================== AUTH ===================
async function login() {
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;
    const errorElement = document.getElementById("login-error");
    errorElement.style.display = "none";
    errorElement.textContent = "";

    try {
        const response = await fetch(`${SERVER_URL}/api/auth/sign-in`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ usernameOrEmail: username, password })
        });

        const data = await response.json();
        if (response.ok && data.access_token) {
            if (username.toLowerCase() === "admin") {
                token = data.access_token;
                currentUser = {
                    username,
                    email: data.email || "admin@gmail.com",
                    userId: data.user_id // Use user_id (snake_case) to match backend response
                };
                currentUserId = data.user_id; // Store user_id
                document.getElementById("login-screen").style.display = "none";
                document.getElementById("dashboard").style.display = "flex";
                updateDashboard();
                loadProfilePicture(); // Load profile picture on login
            } else {
                errorElement.textContent = "Access denied. Only the admin can log in.";
                errorElement.style.display = "block";
            }
        } else {
            errorElement.textContent = data.error || "Login failed. Please check your credentials.";
            errorElement.style.display = "block";
        }
    } catch (error) {
        errorElement.textContent = "Network error: " + error.message;
        errorElement.style.display = "block";
    }
}

function logout() {
    token = null;
    currentUser = null;
    currentUserId = null;
    document.getElementById("dashboard").style.display = "none";
    document.getElementById("login-screen").style.display = "flex";
    if (categoryChart) categoryChart.destroy();
    if (userChart) userChart.destroy();
    document.getElementById("admin-profile-picture").src = "Uploads/profile-pictures/default-profile.jpg";
}

// =================== UI CONTROL ===================
function showTab(tabId) {
    const tabs = document.querySelectorAll(".tab");
    tabs.forEach(tab => tab.classList.remove("active"));
    document.getElementById(tabId).classList.add("active");
    if (tabId === 'profile' && currentUser) {
        showProfile();
        document.getElementById('admin-reset-password-form').reset();
        document.getElementById('user-reset-password-form').reset();
        document.getElementById('admin-reset-error').style.display = 'none';
        document.getElementById('user-reset-error').style.display = 'none';
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    const content = document.getElementById("content");
    sidebar.classList.toggle("active");
    content.classList.toggle("content-shift");
}

// =================== PROFILE ===================
function showProfile() {
    if (document.getElementById('profile').classList.contains('active') && currentUser) {
        document.getElementById('admin-username').textContent = currentUser.username || 'N/A';
        document.getElementById('admin-email').textContent = currentUser.email || 'N/A';
        loadProfilePicture(); // Load profile picture when showing profile tab
    }
}

async function loadProfilePicture() {
    if (!currentUserId || !token) return;

    try {
        const response = await fetch(`${SERVER_URL}/api/users/${currentUserId}/profile-picture`, {
            headers: { "Authorization": `Bearer ${token}` }
        });

        if (response.ok) {
            const blob = await response.blob();
            const img = document.getElementById("admin-profile-picture");
            img.src = URL.createObjectURL(blob);
        } else {
            // Fallback to default image if request fails
            document.getElementById("admin-profile-picture").src = "Uploads/profile-pictures/default-profile.jpg";
        }
    } catch (error) {
        console.error("Error loading profile picture:", error);
        document.getElementById("admin-profile-picture").src = "Uploads/profile-pictures/default-profile.jpg";
    }
}

async function uploadProfilePicture() {
    const fileInput = document.getElementById("profile-picture-input");
    const errorElement = document.getElementById("profile-picture-error");
    errorElement.style.display = "none";
    errorElement.textContent = "";

    if (!fileInput.files[0]) {
        errorElement.textContent = "Please select an image file";
        errorElement.style.display = "block";
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    try {
        const response = await fetch(`${SERVER_URL}/api/users/${currentUserId}/profile-picture`, {
            method: "POST",
            headers: { "Authorization": `Bearer ${token}` },
            body: formData
        });

        const data = await response.json();
        if (response.ok) {
            showToast(data.message || "Profile picture uploaded successfully");
            await loadProfilePicture(); // Refresh the picture
            fileInput.value = ""; // Clear the input
        } else {
            errorElement.textContent = data.error || "Failed to upload profile picture";
            errorElement.style.display = "block";
        }
    } catch (error) {
        errorElement.textContent = "Network error: " + error.message;
        errorElement.style.display = "block";
    }
}

document.getElementById("admin-reset-password-form").addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitButton = e.target.querySelector('.profile-button');
    const errorElement = document.getElementById('admin-reset-error');
    errorElement.style.display = 'none';
    errorElement.textContent = '';
    submitButton.disabled = true;

    const email = document.getElementById('admin-email').textContent;
    const currentPassword = document.getElementById('admin-current-password').value;
    const newPassword = document.getElementById('admin-new-password').value;
    const confirmPassword = document.getElementById('admin-confirm-password').value;

    if (!currentPassword || !newPassword || !confirmPassword) {
        errorElement.textContent = 'All fields are required';
        errorElement.style.display = 'block';
        submitButton.disabled = false;
        return;
    }

    if (newPassword !== confirmPassword) {
        errorElement.textContent = 'Passwords do not match';
        errorElement.style.display = 'block';
        submitButton.disabled = false;
        return;
    }

    try {
        const response = await fetch(`${SERVER_URL}/api/forgotPassword/reset-password?email=${encodeURIComponent(email)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                currentPassword,
                newPassword,
                repeatPassword: confirmPassword
            })
        });
        const result = await response.json();
        if (response.ok) {
            showToast(result.message || 'Password reset successfully');
            e.target.reset();
        } else {
            errorElement.textContent = result.error || 'Failed to reset password';
            errorElement.style.display = 'block';
        }
    } catch (error) {
        errorElement.textContent = 'Network error: ' + error.message;
        errorElement.style.display = 'block';
    } finally {
        submitButton.disabled = false;
    }
});

document.getElementById("user-reset-password-form").addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitButton = e.target.querySelector('.profile-button');
    const errorElement = document.getElementById('user-reset-error');
    errorElement.style.display = 'none';
    errorElement.textContent = '';
    submitButton.disabled = true;

    const email = document.getElementById('user-email').value;
    const newPassword = document.getElementById('user-new-password').value;
    const confirmPassword = document.getElementById('user-confirm-password').value;

    if (!email || !newPassword || !confirmPassword) {
        errorElement.textContent = 'All fields are required';
        errorElement.style.display = 'block';
        submitButton.disabled = false;
        return;
    }

    if (newPassword !== confirmPassword) {
        errorElement.textContent = 'Passwords do not match';
        errorElement.style.display = 'block';
        submitButton.disabled = false;
        return;
    }

    try {
        const response = await fetch(`${SERVER_URL}/api/forgotPassword/reset-password?email=${encodeURIComponent(email)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                newPassword,
                repeatPassword: confirmPassword
            })
        });
        const result = await response.json();
        if (response.ok) {
            showToast(result.message || 'Password reset successfully');
            e.target.reset();
        } else {
            errorElement.textContent = result.error || 'Failed to reset password';
            errorElement.style.display = 'block';
        }
    } catch (error) {
        errorElement.textContent = 'Network error: ' + error.message;
        errorElement.style.display = 'block';
    } finally {
        submitButton.disabled = false;
    }
});

// Add event listener for profile picture upload
document.getElementById("profile-picture-input").addEventListener("change", uploadProfilePicture);

// =================== USER MODAL ===================
function openUserModal() {
    document.getElementById("userModal").style.display = "flex";
}

function closeUserModal() {
    document.getElementById("userModal").style.display = "none";
    document.getElementById("newUsername").value = "";
    document.getElementById("newEmail").value = "";
    document.getElementById("newPassword").value = "";
    const errorEl = document.getElementById("addUserError");
    errorEl.textContent = "";
    errorEl.style.display = "none";
}

// =================== DASHBOARD UPDATE ===================
async function updateDashboard() {
    if (!token) return;

    try {
        // USERS
        const usersRes = await fetch(`${SERVER_URL}/api/admin/users`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const usersData = await usersRes.json();
        if (usersRes.ok) users = usersData;

        // EXPENSES
        const expensesRes = await fetch(`${SERVER_URL}/api/expenses/all`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const expensesData = await expensesRes.json();
        if (expensesRes.ok) expenses = expensesData;

        // BUDGET REQUESTS
        const budgetRes = await fetch(`${SERVER_URL}/api/budgets`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const budgetData = await budgetRes.json();
        if (budgetRes.ok) {
            budgetRequests = budgetData.map(b => ({
                budgetId: b.budgetId,
                username: b.username || "Unknown",
                name: b.name || "No Name",
                amount: b.total || 0,
                status: b.status || "PENDING",
                expenses: b.expenses || []
            }));
        }

        // NOTIFICATIONS
        const notificationsRes = await fetch(`${SERVER_URL}/api/admin/notifications`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const notificationsData = await notificationsRes.json();
        if (notificationsRes.ok) {
            notifications = notificationsData.map(n => ({
                userId: n.userId,
                username: n.username || "Unknown",
                action: n.action || "Unknown action",
                details: n.details || "",
                timestamp: n.timestamp || new Date().toISOString()
            }));
        }

        // UPDATE UI
        document.querySelector(".balance-card h1").textContent = `₱${(budgetRequests.filter(b => b.status === "RELEASED").reduce((sum, b) => sum + (b.amount || 0), 0)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        document.querySelectorAll(".balance-card h1")[1].textContent = users.filter(u => u.username?.toLowerCase() !== "admin").length;
        updateNotifications();
        updateCharts();
        updateUserTable();
        updateBudgetTable();
        updateExpenseTable();
        showProfile();
    } catch (error) {
        showToast("Dashboard error: " + error.message);
    }
}

// =================== NOTIFICATIONS ===================
function updateNotifications() {
    const notificationsList = document.getElementById("notifications-list");
    notificationsList.innerHTML = "";

    if (notifications.length === 0) {
        document.getElementById("no-notifications-message").style.display = "block";
        return;
    }

    document.getElementById("no-notifications-message").style.display = "none";

    notifications
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
        .slice(0, 5)
        .forEach(notification => {
            const div = document.createElement("div");
            div.className = "notification-item";
            div.innerHTML = `
                <p><strong>${notification.username}</strong> ${notification.action}</p>
                <p style="color: #555; font-size: 0.8rem;">${new Date(notification.timestamp).toLocaleString()}</p>
            `;
            notificationsList.appendChild(div);
        });
}

// =================== CHARTS ===================
function updateCharts() {
    // Top Categories
    const categoryTotals = expenses.reduce((acc, exp) => {
        const cat = exp.category || "Uncategorized";
        acc[cat] = (acc[cat] || 0) + (exp.amount || 0);
        return acc;
    }, {});
    const topCategories = Object.entries(categoryTotals)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5);
    const categoryLabels = topCategories.map(([cat]) => cat);
    const categoryData = topCategories.map(([_, amount]) => amount);

    if (categoryChart) categoryChart.destroy();
    categoryChart = new Chart(document.getElementById("categoryChart"), {
        type: "bar",
        data: {
            labels: categoryLabels,
            datasets: [{
                label: "Total Expenses",
                data: categoryData,
                backgroundColor: "rgba(229, 115, 115, 0.6)",
                borderColor: "rgba(229, 115, 115, 1)",
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, title: { display: true, text: "Amount (₱)" } }
            },
            plugins: { legend: { display: false } }
        }
    });

    // Top Users
    const userTotals = expenses.reduce((acc, exp) => {
        const user = exp.username || "Unknown";
        acc[user] = (acc[user] || 0) + (exp.amount || 0);
        return acc;
    }, {});
    const topUsers = Object.entries(userTotals)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5);
    const userLabels = topUsers.map(([user]) => user);
    const userData = topUsers.map(([_, amount]) => amount);

    if (userChart) userChart.destroy();
    userChart = new Chart(document.getElementById("userChart"), {
        type: "bar",
        data: {
            labels: userLabels,
            datasets: [{
                label: "Total Expenses",
                data: userData,
                backgroundColor: "rgba(255, 153, 153, 0.6)",
                borderColor: "rgba(255, 153, 153, 1)",
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, title: { display: true, text: "Amount (₱)" } }
            },
            plugins: { legend: { display: false } }
        }
    });
}

// =================== USER FUNCTIONS ===================
async function createUser() {
    const username = document.getElementById("newUsername").value.trim();
    const email = document.getElementById("newEmail").value.trim();
    const password = document.getElementById("newPassword").value;
    const errorEl = document.getElementById("addUserError");
    errorEl.style.display = "none";
    errorEl.textContent = "";

    if (!username || !email || !password) {
        errorEl.textContent = "Please fill all fields.";
        errorEl.style.display = "block";
        return;
    }

    try {
        const response = await fetch(`${SERVER_URL}/api/admin/users`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ username, email, password })
        });

        const data = await response.json();
        if (response.ok) {
            closeUserModal();
            updateDashboard();
            showToast("User created successfully");
        } else {
            errorEl.textContent = data.error || "Failed to create user";
            errorEl.style.display = "block";
        }
    } catch (error) {
        errorEl.textContent = "Network error: " + error.message;
        errorEl.style.display = "block";
    }
}

async function deleteUser(index) {
    try {
        const user = users[index];
        const response = await fetch(`${SERVER_URL}/api/admin/users/${user.userId}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` }
        });
        if (response.ok) {
            users.splice(index, 1);
            updateUserTable();
            updateDashboard();
            showToast("User deleted successfully");
        } else {
            showToast("Delete failed");
        }
    } catch {
        showToast("Delete error");
    }
}

function updateUserTable() {
    const tbody = document.getElementById("users-table");
    tbody.innerHTML = "";
    const filteredUsers = users.filter(u => u.username?.toLowerCase() !== "admin");

    filteredUsers.forEach((user, index) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${user.username}</td>
            <td>${user.email}</td>
            <td><button onclick="showUserDetails(${index})">View</button></td>
        `;
        tbody.appendChild(row);
    });

    document.getElementById("no-users-message").style.display = filteredUsers.length === 0 ? "block" : "none";
}

function showUserDetails(index) {
    selectedUserIndex = index;
    const user = users.filter(u => u.username?.toLowerCase() !== "admin")[index];

    userDetailsPopup = document.createElement("div");
    userDetailsPopup.className = "user-popup";
    userDetailsPopup.innerHTML = `
        <div class="popup-card">
            <h3>User Details</h3>
            <p><strong>ID:</strong> ${user.userId}</p>
            <p><strong>Username:</strong> ${user.username}</p>
            <p><strong>Email:</strong> ${user.email}</p>
            <div class="popup-actions">
                <button onclick="confirmDeleteUser()">Delete</button>
                <button onclick="closePopup()">Close</button>
            </div>
        </div>
    `;
    document.body.appendChild(userDetailsPopup);
}

function confirmDeleteUser() {
    const confirmPopup = document.getElementById("confirmModal");
    confirmPopup.style.display = "flex";
}

function cancelDeleteUser() {
    const confirmPopup = document.getElementById("confirmModal");
    confirmPopup.style.display = "none";
    if (userDetailsPopup) {
        userDetailsPopup.remove();
        userDetailsPopup = null;
    }
}

function confirmDeleteUser() {
    const confirmPopup = document.getElementById("confirmModal");
    if (selectedUserIndex !== null) {
        const filtered = users.filter(u => u.username?.toLowerCase() !== "admin");
        const actualIndex = users.findIndex(u => u.userId === filtered[selectedUserIndex].userId);
        deleteUser(actualIndex);
    }
    confirmPopup.style.display = "none";
    if (userDetailsPopup) {
        userDetailsPopup.remove();
        userDetailsPopup = null;
    }
}

function closePopup(popupElement = null) {
    if (popupElement) {
        popupElement.remove();
    } else {
        document.querySelectorAll(".user-popup").forEach(el => el.remove());
        document.getElementById("expenseImagePopup").style.display = "none";
        document.removeEventListener('keydown', handleKeyZoom);
        const img = document.getElementById("popup-expense-img");
        img.removeEventListener('mousedown', startDragging);
        img.removeEventListener('mousemove', drag);
        img.removeEventListener('mouseup', stopDragging);
        img.removeEventListener('mouseleave', stopDragging);
    }
    userDetailsPopup = null;
    selectedUserIndex = null;
}

function filterUsers() {
    const value = document.getElementById("searchInput").value.toLowerCase();
    const tbody = document.getElementById("users-table");
    tbody.innerHTML = "";

    const filtered = users.filter(u =>
        u.username?.toLowerCase().includes(value) ||
        u.email?.toLowerCase().includes(value)
    ).filter(u => u.username?.toLowerCase() !== "admin");

    filtered.forEach((user, index) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${user.username}</td>
            <td>${user.email}</td>
            <td><button onclick="showUserDetails(${index})">View</button></td>
        `;
        tbody.appendChild(row);
    });

    document.getElementById("no-users-message").style.display = filtered.length === 0 ? "block" : "none";
}

// =================== BUDGET FUNCTIONS ===================
function updateBudgetTable() {
    const tbody = document.getElementById("budget-table");
    tbody.innerHTML = "";

    const sortValue = document.getElementById("sortBudget")?.value || "username";
    const searchValue = document.getElementById("searchBudget")?.value.toLowerCase() || "";

    let filteredBudgets = budgetRequests;
    if (searchValue) {
        filteredBudgets = budgetRequests.filter(req =>
            (req.username?.toLowerCase().includes(searchValue) || '') ||
            (req.name?.toLowerCase().includes(searchValue) || '') ||
            (req.amount?.toString().includes(searchValue) || '') ||
            (req.status?.toLowerCase().includes(searchValue) || '')
        );
    }

    if (sortValue === "amount") {
        const sortedBudgets = [...filteredBudgets].sort((a, b) => (b.amount || 0) - (a.amount || 0));
        sortedBudgets.forEach((req, index) => {
            const row = document.createElement("tr");
            const statusClass = req.status === "PENDING" ? "badge-pending" :
                              req.status === "RELEASED" ? "badge-released" :
                              "badge-denied";
            row.innerHTML = `
                <td>${req.username}</td>
                <td>${req.name}</td>
                <td>₱${(req.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                <td><span class="status-badge ${statusClass}">${req.status}</span></td>
                <td><button onclick="showBudgetDetails(${budgetRequests.findIndex(b => b.budgetId === req.budgetId)})">View</button></td>
            `;
            tbody.appendChild(row);
        });
    } else {
        const groupBudgets = (budgets, key) => {
            const groups = {};
            budgets.forEach(req => {
                let groupKey;
                switch (key) {
                    case "username": groupKey = req.username || 'Unknown'; break;
                    case "name": groupKey = req.name || 'No Name'; break;
                    case "status": groupKey = req.status || 'PENDING'; break;
                }
                if (!groups[groupKey]) groups[groupKey] = [];
                groups[groupKey].push(req);
            });
            return groups;
        };

        const groupedBudgets = groupBudgets(filteredBudgets, sortValue);

        Object.keys(groupedBudgets).sort().forEach((groupKey, index) => {
            const headerRow = document.createElement("tr");
            headerRow.innerHTML = `<td colspan="5" style="background: linear-gradient(135deg, #fff5f5, #f5f5f5); font-weight: bold; padding: 0.5rem; border-top: 1px solid #ffb3b3;">${groupKey}</td>`;
            tbody.appendChild(headerRow);

            groupedBudgets[groupKey].forEach(req => {
                const row = document.createElement("tr");
                const statusClass = req.status === "PENDING" ? "badge-pending" :
                                  req.status === "RELEASED" ? "badge-released" :
                                  "badge-denied";
                row.innerHTML = `
                    <td>${req.username}</td>
                    <td>${req.name}</td>
                    <td>₱${(req.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td><span class="status-badge ${statusClass}">${req.status}</span></td>
                    <td><button onclick="showBudgetDetails(${budgetRequests.findIndex(b => b.budgetId === req.budgetId)})">View</button></td>
                `;
                tbody.appendChild(row);
            });

            if (index < Object.keys(groupedBudgets).length - 1) {
                const separatorRow = document.createElement("tr");
                separatorRow.innerHTML = `<td colspan="5" style="height: 0.5rem; background: linear-gradient(to right, #ffb3b3, #f5f5f5); border-bottom: 1px solid #ffb3b3;"></td>`;
                tbody.appendChild(separatorRow);
            }
        });
    }

    document.getElementById("no-budgets-message").style.display = filteredBudgets.length === 0 ? "block" : "none";
}

function filterBudgets() {
    updateBudgetTable();
}

function showBudgetDetails(index) {
    if (index === null || index < 0 || index >= budgetRequests.length) return;
    selectedBudgetIndex = index;
    const budget = budgetRequests[index];
    const expenseTotal = budget.expenses.reduce((sum, exp) => sum + (exp.quantity * exp.amountPerUnit || 0), 0);
    const detailsDiv = document.getElementById("budgetDetails");
    const actionsDiv = document.getElementById("budgetActions");
    const isPending = budget.status === "PENDING";

    detailsDiv.innerHTML = `
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Username:</span> <span style="word-break: break-word;">${budget.username || 'Unknown'}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Request Name:</span> <span style="word-break: break-word;">${budget.name || 'No Name'}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Total Amount:</span> <span style="word-break: break-word;">₱${(budget.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${expenseTotal !== (budget.amount || 0) && budget.expenses.length > 0 ? `(Calculated from items: ₱${expenseTotal.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})` : ''}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Status:</span> <span class="status-badge ${budget.status === "PENDING" ? "badge-pending" : budget.status === "RELEASED" ? "badge-released" : "badge-denied"}" style="word-break: break-word;">${budget.status || 'PENDING'}</span>
        </div>
        <h4 style="margin: 1rem 0;">Associated Expense Items:</h4>
        ${budget.expenses && budget.expenses.length > 0 ? `
            <ul style="padding-left: 20px; margin: 0;">
                ${budget.expenses.map(exp => `
                    <li style="margin-bottom: 1rem;">
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Category:</span> <span style="word-break: break-word;">${exp.category || 'N/A'}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Quantity:</span> <span style="word-break: break-word;">${exp.quantity || 0}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Amount per Unit:</span> <span style="word-break: break-word;">₱${(exp.amountPerUnit || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Subtotal:</span> <span style="word-break: break-word;">₱${(exp.quantity * (exp.amountPerUnit || 0)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Remarks:</span> <span style="word-break: break-word;">${exp.remarks || 'None'}</span>
                        </div>
                    </li>
                `).join('')}
            </ul>
        ` : '<p style="margin: 1rem 0;">No expense items associated with this budget request yet.</p>'}
    `;

    actionsDiv.innerHTML = isPending ? `
        <button onclick="releaseBudget()" style="background: linear-gradient(135deg, #5cb85c, #4cae4c); color: white;">Release</button>
        <button onclick="denyBudget()" style="background: linear-gradient(135deg, #d9534f, #c9302c); color: white;">Deny</button>
    ` : ``;

    document.getElementById("budgetPopup").style.display = "flex";
}

function closeBudgetPopup() {
    document.getElementById("budgetPopup").style.display = "none";
    selectedBudgetIndex = null;
}

async function releaseBudget() {
    if (selectedBudgetIndex === null) return;
    await updateBudgetStatus("RELEASED");
}

async function denyBudget() {
    if (selectedBudgetIndex === null) return;
    await updateBudgetStatus("DENIED");
}

async function updateBudgetStatus(status) {
    try {
        const budget = budgetRequests[selectedBudgetIndex];
        const response = await fetch(`${SERVER_URL}/api/budgets/${budget.budgetId}/status`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ status }) // Send as { "status": "RELEASED" }
        });
        const data = await response.json();
        if (response.ok) {
            budgetRequests[selectedBudgetIndex].status = status;
            updateBudgetTable();
            closeBudgetPopup();
            showToast(`Budget ${status.toLowerCase()} successfully`);
        } else {
            showToast(data.error || "Status update failed");
        }
    } catch (error) {
        showToast("Status update error: " + error.message);
    }
}

// =================== EXPENSE FUNCTIONS ===================
function updateExpenseTable() {
    const tbody = document.getElementById("expenses-table");
    tbody.innerHTML = "";

    const sortedExpenses = getSortedExpenses();
    const sortValue = document.getElementById("sortExpense")?.value || "date";
    const searchValue = document.getElementById("searchExpense")?.value.toLowerCase() || "";

    let filteredExpenses = sortedExpenses;
    if (searchValue) {
        filteredExpenses = sortedExpenses.filter(exp =>
            (exp.username?.toLowerCase().includes(searchValue) || '') ||
            (exp.category?.toLowerCase().includes(searchValue) || '') ||
            (exp.amount?.toString().includes(searchValue) || '') ||
            (exp.dateOfTransaction?.toLowerCase().includes(searchValue) || '') ||
            (exp.remarks?.toLowerCase().includes(searchValue) || '')
        );
    }

    if (sortValue === "amount") {
        filteredExpenses.forEach(exp => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${exp.username || 'Unknown'}</td>
                <td>${exp.category || ''}</td>
                <td>₱${(exp.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                <td>${exp.dateOfTransaction || ''}</td>
                <td>${exp.remarks || ''}</td>
                <td><button onclick="showExpenseImage(${exp.expenseId})">View</button></td>
            `;
            tbody.appendChild(row);
        });
    } else {
        const groupExpenses = (expenses, key) => {
            const groups = {};
            expenses.forEach(exp => {
                let groupKey;
                switch (key) {
                    case "date":
                        groupKey = exp.dateOfTransaction ? new Date(exp.dateOfTransaction).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'No Date';
                        break;
                    case "user":
                        groupKey = exp.username || 'Unknown';
                        break;
                    case "category":
                        groupKey = exp.category || 'Uncategorized';
                        break;
                }
                if (!groups[groupKey]) groups[groupKey] = [];
                groups[groupKey].push(exp);
            });
            return groups;
        };

        const groupedExpenses = groupExpenses(filteredExpenses, sortValue);

        Object.keys(groupedExpenses).sort().forEach((groupKey, index) => {
            const headerRow = document.createElement("tr");
            headerRow.innerHTML = `<td colspan="6" style="background: linear-gradient(135deg, #fff5f5, #f5f5f5); font-weight: bold; padding: 0.5rem; border-top: 1px solid #ffb3b3;">${groupKey}</td>`;
            tbody.appendChild(headerRow);

            groupedExpenses[groupKey].forEach(exp => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${exp.username || 'Unknown'}</td>
                    <td>${exp.category || ''}</td>
                    <td>₱${(exp.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td>${exp.dateOfTransaction || ''}</td>
                    <td>${exp.remarks || ''}</td>
                    <td><button onclick="showExpenseImage(${exp.expenseId})">View</button></td>
                `;
                tbody.appendChild(row);
            });

            if (index < Object.keys(groupedExpenses).length - 1) {
                const separatorRow = document.createElement("tr");
                separatorRow.innerHTML = `<td colspan="6" style="height: 0.5rem; background: linear-gradient(to right, #ffb3b3, #f5f5f5); border-bottom: 1px solid #ffb3b3;"></td>`;
                tbody.appendChild(separatorRow);
            }
        });
    }

    document.getElementById("no-expenses-message").style.display = filteredExpenses.length === 0 ? "block" : "none";
}

function filterExpenses() {
    updateExpenseTable();
}

function getSortedExpenses() {
    const sortValue = document.getElementById("sortExpense")?.value || "date";
    return [...expenses].sort((a, b) => {
        switch (sortValue) {
            case "amount":
                return (b.amount || 0) - (a.amount || 0);
            case "category":
                return (a.category || '').localeCompare(b.category || '');
            case "user":
                return (a.username || '').localeCompare(b.username || '');
            default:
                return new Date(b.dateOfTransaction) - new Date(a.dateOfTransaction);
        }
    });
}

function showExpenseImage(expenseId) {
    const popup = document.getElementById("expenseImagePopup");
    const img = document.getElementById("popup-expense-img");
    zoomLevel = 1;
    panX = 0; panY = 0;
    img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
    img.style.width = 'auto';
    img.style.height = 'auto';
    if (expenseId) {
        fetch(`${SERVER_URL}/api/expenses/${expenseId}/images?index=0`, {
            headers: { "Authorization": `Bearer ${token}` }
        })
        .then(response => {
            if (!response.ok) throw new Error(`Image not found: ${response.status} ${response.statusText}`);
            return response.blob();
        })
        .then(blob => {
            img.src = URL.createObjectURL(blob);
            popup.style.display = "flex";
            document.addEventListener('keydown', handleKeyZoom);
        })
        .catch(error => {
            showToast("Failed to load image: " + error.message);
        });
    } else {
        img.src = '';
        popup.style.display = "flex";
    }
    img.addEventListener('mousedown', startDragging);
    img.addEventListener('mousemove', drag);
    img.addEventListener('mouseup', stopDragging);
    img.addEventListener('mouseleave', stopDragging);
}

function zoomImage(direction) {
    const img = document.getElementById("popup-expense-img");
    let newZoom = zoomLevel + (direction === 'in' ? 0.1 : -0.1);
    newZoom = Math.max(0.5, Math.min(3, newZoom));
    if (newZoom !== zoomLevel) {
        zoomLevel = newZoom;
        const rect = img.getBoundingClientRect();
        const container = img.parentElement.getBoundingClientRect();
        const centerX = (container.width - rect.width) / 2 + panX;
        const centerY = (container.height - rect.height) / 2 + panY;
        const scaleFactor = newZoom / zoomLevel;
        panX = centerX * (1 - scaleFactor);
        panY = centerY * (1 - scaleFactor);
        img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
        if (zoomLevel <= 0.5 || zoomLevel >= 3) {
            img.style.opacity = '0.7';
            setTimeout(() => img.style.opacity = '1', 200);
        }
    }
}

function handleKeyZoom(event) {
    if (document.getElementById("expenseImagePopup").style.display === "flex") {
        if (event.key === '+') zoomImage('in');
        else if (event.key === '-') zoomImage('out');
    }
}

function startDragging(event) {
    if (zoomLevel > 1) {
        isDragging = true;
        const img = document.getElementById("popup-expense-img");
        img.style.cursor = 'grabbing';
        startX = event.clientX;
        startY = event.clientY;
    }
}

function drag(event) {
    if (isDragging && zoomLevel > 1) {
        const dx = event.clientX - startX;
        const dy = event.clientY - startY;
        panX += dx;
        panY += dy;
        startX = event.clientX;
        startY = event.clientY;
        const img = document.getElementById("popup-expense-img");
        img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
    }
}

function stopDragging() {
    if (isDragging) {
        isDragging = false;
        const img = document.getElementById("popup-expense-img");
        img.style.cursor = 'grab';
    }
}

function downloadImage() {
    const img = document.getElementById("popup-expense-img");
    if (img.src) {
        const link = document.createElement('a');
        link.href = img.src;
        link.download = `expense_receipt_${new Date().toISOString().slice(0, 10)}.png`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    } else {
        showToast("No image available to download.");
    }
}