const SERVER_URL = "http://152.42.192.226:8080"; // Change to "http://152.42.192.226:8080" for server testing
                                            // Change to "http://localhost:8080" for local testing
let users = [];
let expenses = [];
let budgetRequests = [];
let liquidations = [];
let token = null;
let selectedUserIndex = null;
let userDetailsPopup = null;
let selectedBudgetIndex = null;
let selectedLiquidationIndex = null;
let currentExpenseImages = [];
let currentImageIndex = 0;
let zoomLevel = 1;
let panX = 0, panY = 0;
let isDragging = false;
let startX = 0, startY = 0;
let categoryChart, userChart, monthlySpendingChart, budgetStatusChart;
let currentUser = null;
let currentUserId = null;

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
                    userId: data.user_id
                };
                currentUserId = data.user_id;
                document.getElementById("login-screen").style.display = "none";
                document.getElementById("dashboard").style.display = "flex";
                updateDashboard();
                loadProfilePicture();
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
    if (monthlySpendingChart) monthlySpendingChart.destroy();
    if (budgetStatusChart) budgetStatusChart.destroy();
    document.getElementById("admin-profile-picture").src = "images/default-profile.png";
}

// =================== UI CONTROL ===================
function showTab(tabId) {
    const tabs = document.querySelectorAll(".tab");
    tabs.forEach(tab => tab.classList.remove("active"));
    document.getElementById(tabId).classList.add("active");
    if (tabId === 'profile' && currentUser) {
        showProfile();
        document.getElementById('admin-reset-password-form').reset();
        document.getElementById('admin-reset-error').style.display = 'none';
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
        loadProfilePicture();
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
            document.getElementById("admin-profile-picture").src = "images/default-profile.png";
        }
    } catch (error) {
        console.error("Error loading profile picture:", error);
        document.getElementById("admin-profile-picture").src = "images/default-profile.png";
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
            await loadProfilePicture();
            fileInput.value = "";
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

        // LIQUIDATIONS
        const liquidationsRes = await fetch(`${SERVER_URL}/api/liquidation`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        const liquidationsData = await liquidationsRes.json();
        if (liquidationsRes.ok) {
            liquidations = (liquidationsData.budgets || []).map(l => ({
                liquidationId: l.liquidationId,
                username: l.username || "Unknown",
                dateOfTransaction: l.dateOfTransaction || new Date().toISOString(),
                budgetName: l.budgetName || "No Name",
                amount: l.amount || 0,
                totalSpent: l.totalSpent || 0,
                remainingBalance: l.remainingBalance || 0,
                status: l.status || "PENDING",
                remarks: l.remarks || "",
                expenses: l.expenses || []
            }));
        }

        // UPDATE UI
        const balanceCards = document.querySelectorAll(".balance-card h1");
        balanceCards[0].textContent = users.filter(u => u.username?.toLowerCase() !== "admin").length;
        balanceCards[1].textContent = `₱${(budgetRequests.filter(b => b.status === "RELEASED").reduce((sum, b) => sum + (b.amount || 0), 0)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        balanceCards[2].textContent = `₱${(liquidations.filter(l => l.status === "LIQUIDATED").reduce((sum, l) => sum + (l.totalSpent || 0), 0)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        balanceCards[3].textContent = `₱${(liquidations.filter(l => l.status === "LIQUIDATED").reduce((sum, l) => sum + ((l.amount || 0) - (l.totalSpent || 0)), 0)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
        updateCharts();
        updateUserTable();
        updateBudgetTable();
        updateExpenseTable();
        updateLiquidationTable();
        showProfile();
    } catch (error) {
        showToast("Dashboard error: " + error.message);
    }
}

// =================== CHARTS ===================
function updateCharts() {
    // Category Chart
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

    // User Chart
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

    // Monthly Spending Trend Chart
    const monthlyTotals = expenses.reduce((acc, exp) => {
        const date = new Date(exp.dateOfTransaction);
        const monthYear = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`;
        acc[monthYear] = (acc[monthYear] || 0) + (exp.amount || 0);
        return acc;
    }, {});
    const today = new Date();
    const months = [];
    for (let i = 11; i >= 0; i--) {
        const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
        const monthYear = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`;
        months.push(monthYear);
    }
    const monthlyLabels = months.map(my => {
        const [year, month] = my.split('-');
        return new Date(year, month - 1).toLocaleString('en-US', { month: 'short', year: 'numeric' });
    });
    const monthlyData = months.map(my => monthlyTotals[my] || 0);

    if (monthlySpendingChart) monthlySpendingChart.destroy();
    monthlySpendingChart = new Chart(document.getElementById("monthlySpendingChart"), {
        type: "line",
        data: {
            labels: monthlyLabels,
            datasets: [{
                label: "Monthly Expenses",
                data: monthlyData,
                borderColor: "rgba(229, 115, 115, 1)",
                backgroundColor: "rgba(229, 115, 115, 0.2)",
                fill: true,
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, title: { display: true, text: "Amount (₱)" } },
                x: { title: { display: true, text: "Month" } }
            },
            plugins: { legend: { display: false } }
        }
    });

    // Budget Status Distribution Chart
    const statusCounts = budgetRequests.reduce((acc, req) => {
        const status = req.status || "PENDING";
        acc[status] = (acc[status] || 0) + 1;
        return acc;
    }, {});
    const statusLabels = ["PENDING", "RELEASED", "DENIED"];
    const statusData = statusLabels.map(status => statusCounts[status] || 0);
    const statusColors = ["#f0ad4e", "#5cb85c", "#d9534f"];

    if (budgetStatusChart) budgetStatusChart.destroy();
    budgetStatusChart = new Chart(document.getElementById("budgetStatusChart"), {
        type: "pie",
        data: {
            labels: statusLabels,
            datasets: [{
                data: statusData,
                backgroundColor: statusColors,
                borderColor: "#fff",
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom' }
            }
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

async function resetUserPassword(user, popup) {
    const newPassword = document.getElementById('user-new-password').value;
    const confirmPassword = document.getElementById('user-confirm-password').value;
    const errorElement = document.getElementById('user-reset-error');
    errorElement.style.display = 'none';
    errorElement.textContent = '';

    if (!newPassword || !confirmPassword) {
        errorElement.textContent = 'All fields are required';
        errorElement.style.display = 'block';
        return;
    }

    if (newPassword !== confirmPassword) {
        errorElement.textContent = 'Passwords do not match';
        errorElement.style.display = 'block';
        return;
    }

    try {
        const response = await fetch(`${SERVER_URL}/api/forgotPassword/reset-password?email=${encodeURIComponent(user.email)}`, {
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
            popup.remove();
            userDetailsPopup = null;
            selectedUserIndex = null;
        } else {
            errorElement.textContent = result.error || 'Failed to reset password';
            errorElement.style.display = 'block';
        }
    } catch (error) {
        errorElement.textContent = 'Network error: ' + error.message;
        errorElement.style.display = 'block';
    }
}

async function loadUserProfilePicture(userId) {
    try {
        const response = await fetch(`${SERVER_URL}/api/users/${userId}/profile-picture`, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (response.ok) {
            const blob = await response.blob();
            return URL.createObjectURL(blob);
        }
        return "images/default-profile.png";
    } catch (error) {
        console.error("Error loading user profile picture:", error);
        return "images/default-profile.png";
    }
}

async function updateUserTable() {
    const tbody = document.getElementById("users-table");
    tbody.innerHTML = "";
    const filteredUsers = users.filter(u => u.username?.toLowerCase() !== "admin");

    for (let index = 0; index < filteredUsers.length; index++) {
        const user = filteredUsers[index];
        const profilePicSrc = await loadUserProfilePicture(user.userId);
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><img src="${profilePicSrc}" class="user-profile-pic" alt="${user.username}'s Profile Picture">${user.username}</td>
            <td>${user.email}</td>
            <td><button onclick="showUserDetails(${index})">View</button></td>
        `;
        tbody.appendChild(row);
    }

    document.getElementById("no-users-message").style.display = filteredUsers.length === 0 ? "block" : "none";
}

function showUserDetails(index) {
    selectedUserIndex = index;
    const user = users.filter(u => u.username?.toLowerCase() !== "admin")[index];

    userDetailsPopup = document.createElement("div");
    userDetailsPopup.className = "user-popup";
    userDetailsPopup.innerHTML = `
        <div class="modal-content popup-card">
            <span class="close-btn" onclick="closePopup()" style="float: right; font-size: 1.5rem; cursor: pointer; color: #333;">×</span>
            <h3>User Details</h3>
            <p><strong>ID:</strong> ${user.userId}</p>
            <p><strong>Username:</strong> ${user.username}</p>
            <p><strong>Email:</strong> ${user.email}</p>
            <h4>Reset Password</h4>
            <form id="user-reset-password-form" class="profile-form">
                <div class="form-group password-wrapper">
                    <label for="user-new-password">New Password</label>
                    <input type="password" id="user-new-password" required>
                    <span class="toggle-password" onclick="togglePassword('user-new-password', this)" aria-label="Toggle password visibility">
                        <i class="fa-solid fa-eye-slash"></i>
                    </span>
                </div>
                <div class="form-group password-wrapper">
                    <label for="user-confirm-password">Confirm New Password</label>
                    <input type="password" id="user-confirm-password" required>
                    <span class="toggle-password" onclick="togglePassword('user-confirm-password', this)" aria-label="Toggle password visibility">
                        <i class="fa-solid fa-eye-slash"></i>
                    </span>
                </div>
                <p id="user-reset-error" class="error-text" style="display: none;"></p>
                <button type="button" onclick="resetUserPassword(users.filter(u => u.username?.toLowerCase() !== 'admin')[${index}], this.closest('.user-popup'))" style="background: linear-gradient(135deg, #5cb85c, #4cae4c); color: white;">Reset Password</button>
            </form>
            <div class="popup-actions">
                <button onclick="confirmDeleteUser()" style="background: linear-gradient(135deg, #d9534f, #c9302c); color: white;">Delete</button>
                <button onclick="closePopup()" style="background: linear-gradient(135deg, #ccc, #aaa); color: black;">Close</button>
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
        currentExpenseImages = [];
        currentImageIndex = 0;
        zoomLevel = 1;
        panX = 0;
        panY = 0;
    }
    userDetailsPopup = null;
    selectedUserIndex = null;
}

async function filterUsers() {
    const value = document.getElementById("searchInput").value.toLowerCase();
    const tbody = document.getElementById("users-table");
    tbody.innerHTML = "";

    const filtered = users.filter(u =>
        u.username?.toLowerCase().includes(value) ||
        u.email?.toLowerCase().includes(value)
    ).filter(u => u.username?.toLowerCase() !== "admin");

    for (let index = 0; index < filtered.length; index++) {
        const user = filtered[index];
        const profilePicSrc = await loadUserProfilePicture(user.userId);
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><img src="${profilePicSrc}" class="user-profile-pic" alt="${user.username}'s Profile Picture">${user.username}</td>
            <td>${user.email}</td>
            <td><button onclick="showUserDetails(${index})">View</button></td>
        `;
        tbody.appendChild(row);
    }

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
            body: JSON.stringify({ status })
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
                <td><button onclick="showExpenseImage(${exp.expenseId}, null, null)">View</button></td>
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
                    <td><button onclick="showExpenseImage(${exp.expenseId}, null, null)">View</button></td>
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

function showExpenseImage(expenseId, liquidationId = null, imageIndex = 0) {
    const popup = document.getElementById("expenseImagePopup");
    const img = document.getElementById("popup-expense-img");
    zoomLevel = 1;
    panX = 0;
    panY = 0;
    img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
    img.style.width = 'auto';
    img.style.height = 'auto';

    // Update popup to include navigation buttons
    const imageControls = popup.querySelector('div[style*="margin-top: 10px"]');
    imageControls.innerHTML = `
        <button onclick="zoomImage('in')" style="margin-right: 10px; padding: 5px 10px; border: none; border-radius: 5px; background: linear-gradient(135deg, #5cb85c, #4cae4c); color: white; cursor: pointer;">
            <i class="fas fa-search-plus"></i> Zoom In
        </button>
        <button onclick="zoomImage('out')" style="margin-right: 10px; padding: 5px 10px; border: none; border-radius: 5px; background: linear-gradient(135deg, #d9534f, #c9302c); color: white; cursor: pointer;">
            <i class="fas fa-search-minus"></i> Zoom Out
        </button>
        <button onclick="showPreviousImage()" style="margin-right: 10px; padding: 5px 10px; border: none; border-radius: 5px; background: linear-gradient(135deg, #333, #555); color: white; cursor: pointer; display: ${liquidationId ? 'inline-block' : 'none'};">
            <i class="fas fa-arrow-left"></i> Previous
        </button>
        <button onclick="showNextImage()" style="margin-right: 10px; padding: 5px 10px; border: none; border-radius: 5px; background: linear-gradient(135deg, #333, #555); color: white; cursor: pointer; display: ${liquidationId ? 'inline-block' : 'none'};">
            <i class="fas fa-arrow-right"></i> Next
        </button>
        <button onclick="downloadImage()" style="padding: 5px 10px; border: none; border-radius: 5px; background: linear-gradient(135deg, #333, #555); color: white; cursor: pointer;">
            <i class="fas fa-download"></i> Download
        </button>
    `;

    if (liquidationId) {
        const liquidation = liquidations.find(l => l.liquidationId === liquidationId);
        const expense = liquidation?.expenses.find(e => e.expenseId === expenseId);
        currentExpenseImages = expense?.imagePaths || [];
        currentImageIndex = imageIndex;

        if (currentExpenseImages.length > 0 && currentImageIndex >= 0 && currentImageIndex < currentExpenseImages.length) {
            fetch(`${SERVER_URL}/api/liquidation/${liquidationId}/images?index=${currentImageIndex}`, {
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
                img.src = '';
                popup.style.display = "flex";
            });
        } else {
            img.src = '';
            popup.style.display = "flex";
            showToast("No images available for this expense");
        }
    } else {
        currentExpenseImages = [];
        currentImageIndex = 0;
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
            img.src = '';
            popup.style.display = "flex";
        });
    }

    img.addEventListener('mousedown', startDragging);
    img.addEventListener('mousemove', drag);
    img.addEventListener('mouseup', stopDragging);
    img.addEventListener('mouseleave', stopDragging);
}

function showPreviousImage() {
    if (currentImageIndex > 0) {
        currentImageIndex--;
        const liquidation = liquidations[selectedLiquidationIndex];
        const expense = liquidation?.expenses.find(e => e.imagePaths[currentImageIndex]);
        showExpenseImage(expense?.expenseId, liquidation?.liquidationId, currentImageIndex);
    }
}

function showNextImage() {
    if (currentImageIndex < currentExpenseImages.length - 1) {
        currentImageIndex++;
        const liquidation = liquidations[selectedLiquidationIndex];
        const expense = liquidation?.expenses.find(e => e.imagePaths[currentImageIndex]);
        showExpenseImage(expense?.expenseId, liquidation?.liquidationId, currentImageIndex);
    }
}

function zoomImage(direction) {
    const img = document.getElementById("popup-expense-img");
    if (direction === 'in' && zoomLevel < 3) {
        zoomLevel += 0.2;
    } else if (direction === 'out' && zoomLevel > 0.5) {
        zoomLevel -= 0.2;
    }
    img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
}

function handleKeyZoom(event) {
    if (event.key === '+' || event.key === '=') {
        zoomImage('in');
    } else if (event.key === '-' || event.key === '_') {
        zoomImage('out');
    }
}

function startDragging(event) {
    if (zoomLevel <= 1) return;
    isDragging = true;
    startX = event.clientX - panX;
    startY = event.clientY - panY;
    document.getElementById("popup-expense-img").style.cursor = 'grabbing';
}

function drag(event) {
    if (!isDragging) return;
    event.preventDefault();
    panX = event.clientX - startX;
    panY = event.clientY - startY;
    const img = document.getElementById("popup-expense-img");
    img.style.transform = `scale(${zoomLevel}) translate(${panX}px, ${panY}px)`;
}

function stopDragging() {
    isDragging = false;
    document.getElementById("popup-expense-img").style.cursor = 'grab';
}

async function downloadImage() {
    const img = document.getElementById("popup-expense-img");
    if (!img.src) {
        showToast("No image available to download");
        return;
    }

    try {
        let url;
        if (currentExpenseImages.length > 0) {
            const liquidation = liquidations[selectedLiquidationIndex];
            url = `${SERVER_URL}/api/liquidation/${liquidation.liquidationId}/images?index=${currentImageIndex}`;
        } else {
            const expenseId = expenses.find(exp => exp.imagePaths?.includes(img.src.split('/').pop()))?.expenseId;
            url = `${SERVER_URL}/api/expenses/${expenseId}/images?index=0`;
        }

        const response = await fetch(url, {
            headers: { "Authorization": `Bearer ${token}` }
        });
        if (!response.ok) throw new Error("Failed to fetch image");

        const blob = await response.blob();
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `expense-image-${Date.now()}.jpg`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
        showToast("Image downloaded successfully");
    } catch (error) {
        showToast("Failed to download image: " + error.message);
    }
}

// =================== LIQUIDATION FUNCTIONS ===================
function updateLiquidationTable() {
    const tbody = document.getElementById("liquidation-table");
    tbody.innerHTML = "";

    const sortValue = document.getElementById("sortLiquidation")?.value || "username";
    const searchValue = document.getElementById("searchLiquidation")?.value.toLowerCase() || "";

    let filteredLiquidations = liquidations;
    if (searchValue) {
        filteredLiquidations = liquidations.filter(l =>
            (l.username?.toLowerCase().includes(searchValue) || '') ||
            (l.budgetName?.toLowerCase().includes(searchValue) || '') ||
            (l.amount?.toString().includes(searchValue) || '') ||
            (l.totalSpent?.toString().includes(searchValue) || '') ||
            (l.remainingBalance?.toString().includes(searchValue) || '') ||
            (l.status?.toLowerCase().includes(searchValue) || '') ||
            (l.remarks?.toLowerCase().includes(searchValue) || '')
        );
    }

    if (sortValue === "amount") {
        const sortedLiquidations = [...filteredLiquidations].sort((a, b) => (b.amount || 0) - (a.amount || 0));
        sortedLiquidations.forEach((l, index) => {
            const row = document.createElement("tr");
            const statusClass = l.status === "PENDING" ? "badge-pending" :
                              l.status === "LIQUIDATED" ? "badge-liquidated" :
                              "badge-denied";
            row.innerHTML = `
                <td>${l.username}</td>
                <td>${new Date(l.dateOfTransaction).toLocaleDateString()}</td>
                <td>${l.budgetName}</td>
                <td>₱${(l.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                <td>₱${(l.totalSpent || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                <td>₱${(l.remainingBalance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                <td><span class="status-badge ${statusClass}">${l.status}</span></td>
                <td>${l.remarks || ''}</td>
                <td><button onclick="showLiquidationDetails(${liquidations.findIndex(li => li.liquidationId === l.liquidationId)})">View</button></td>
            `;
            tbody.appendChild(row);
        });
    } else {
        const groupLiquidations = (liquidations, key) => {
            const groups = {};
            liquidations.forEach(l => {
                let groupKey;
                switch (key) {
                    case "username": groupKey = l.username || 'Unknown'; break;
                    case "date": groupKey = l.dateOfTransaction ? new Date(l.dateOfTransaction).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'No Date'; break;
                    case "status": groupKey = l.status || 'PENDING'; break;
                }
                if (!groups[groupKey]) groups[groupKey] = [];
                groups[groupKey].push(l);
            });
            return groups;
        };

        const groupedLiquidations = groupLiquidations(filteredLiquidations, sortValue);

        Object.keys(groupedLiquidations).sort().forEach((groupKey, index) => {
            const headerRow = document.createElement("tr");
            headerRow.innerHTML = `<td colspan="9" style="background: linear-gradient(135deg, #fff5f5, #f5f5f5); font-weight: bold; padding: 0.5rem; border-top: 1px solid #ffb3b3;">${groupKey}</td>`;
            tbody.appendChild(headerRow);

            groupedLiquidations[groupKey].forEach(l => {
                const row = document.createElement("tr");
                const statusClass = l.status === "PENDING" ? "badge-pending" :
                                  l.status === "LIQUIDATED" ? "badge-liquidated" :
                                  "badge-denied";
                row.innerHTML = `
                    <td>${l.username}</td>
                    <td>${new Date(l.dateOfTransaction).toLocaleDateString()}</td>
                    <td>${l.budgetName}</td>
                    <td>₱${(l.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td>₱${(l.totalSpent || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td>₱${(l.remainingBalance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    <td><span class="status-badge ${statusClass}">${l.status}</span></td>
                    <td>${l.remarks || ''}</td>
                    <td><button onclick="showLiquidationDetails(${liquidations.findIndex(li => li.liquidationId === l.liquidationId)})">View</button></td>
                `;
                tbody.appendChild(row);
            });

            if (index < Object.keys(groupedLiquidations).length - 1) {
                const separatorRow = document.createElement("tr");
                separatorRow.innerHTML = `<td colspan="9" style="height: 0.5rem; background: linear-gradient(to right, #ffb3b3, #f5f5f5); border-bottom: 1px solid #ffb3b3;"></td>`;
                tbody.appendChild(separatorRow);
            }
        });
    }

    document.getElementById("no-liquidations-message").style.display = filteredLiquidations.length === 0 ? "block" : "none";
}

function filterLiquidations() {
    updateLiquidationTable();
}

function showLiquidationDetails(index) {
    if (index === null || index < 0 || index >= liquidations.length) return;
    selectedLiquidationIndex = index;
    const liquidation = liquidations[index];
    const detailsDiv = document.getElementById("liquidationDetails");
    const actionsDiv = document.getElementById("liquidationActions");
    const isPending = liquidation.status === "PENDING";

    detailsDiv.innerHTML = `
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Username:</span> <span style="word-break: break-word;">${liquidation.username || 'Unknown'}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Date:</span> <span style="word-break: break-word;">${new Date(liquidation.dateOfTransaction).toLocaleDateString()}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Request Name:</span> <span style="word-break: break-word;">${liquidation.budgetName || 'No Name'}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Amount:</span> <span style="word-break: break-word;">₱${(liquidation.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Total Spent:</span> <span style="word-break: break-word;">₱${(liquidation.totalSpent || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Remaining Balance:</span> <span style="word-break: break-word;">₱${(liquidation.remainingBalance || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Status:</span> <span class="status-badge ${liquidation.status === "PENDING" ? "badge-pending" : liquidation.status === "LIQUIDATED" ? "badge-liquidated" : "badge-denied"}" style="word-break: break-word;">${liquidation.status || 'PENDING'}</span>
        </div>
        <div>
            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Remarks:</span> <span style="word-break: break-word;">${liquidation.remarks || 'None'}</span>
        </div>
        <h4 style="margin: 1rem 0;">Associated Expenses:</h4>
        ${liquidation.expenses && liquidation.expenses.length > 0 ? `
            <ul style="padding-left: 20px; margin: 0;">
                ${liquidation.expenses.map(exp => `
                    <li style="margin-bottom: 1rem;">
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Category:</span> <span style="word-break: break-word;">${exp.category || 'N/A'}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Amount:</span> <span style="word-break: break-word;">₱${(exp.amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Date:</span> <span style="word-break: break-word;">${exp.dateOfTransaction ? new Date(exp.dateOfTransaction).toLocaleDateString() : 'N/A'}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Remarks:</span> <span style="word-break: break-word;">${exp.remarks || 'None'}</span>
                        </div>
                        <div>
                            <span style="font-weight: bold; min-width: 120px; display: inline-block;">Receipt:</span>
                            <button onclick="showExpenseImage(${exp.expenseId}, ${liquidation.liquidationId}, 0)">View</button>
                        </div>
                    </li>
                `).join('')}
            </ul>
        ` : '<p style="margin: 1rem 0;">No expenses associated with this liquidation yet.</p>'}
    `;

    actionsDiv.innerHTML = isPending ? `
        <button onclick="liquidateBudget()" style="background: linear-gradient(135deg, #5cb85c, #4cae4c); color: white;">Liquidate</button>
        <button onclick="denyLiquidation()" style="background: linear-gradient(135deg, #d9534f, #c9302c); color: white;">Deny</button>
    ` : ``;

    document.getElementById("liquidationPopup").style.display = "flex";
}

function closeLiquidationPopup() {
    document.getElementById("liquidationPopup").style.display = "none";
    selectedLiquidationIndex = null;
}

async function liquidateBudget() {
    if (selectedLiquidationIndex === null) return;
    await updateLiquidationStatus("LIQUIDATED");
}

async function denyLiquidation() {
    if (selectedLiquidationIndex === null) return;
    await updateLiquidationStatus("DENIED");
}

async function updateLiquidationStatus(status) {
    try {
        const liquidation = liquidations[selectedLiquidationIndex];
        const response = await fetch(`${SERVER_URL}/api/liquidation/${liquidation.liquidationId}/status`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${token}`
            },
            body: JSON.stringify({ status })
        });
        const data = await response.json();
        if (response.ok) {
            liquidations[selectedLiquidationIndex].status = status;
            updateLiquidationTable();
            closeLiquidationPopup();
            updateDashboard(); // Update dashboard to reflect new liquidation status
            showToast(`Liquidation ${status.toLowerCase()} successfully`);
        } else {
            showToast(data.error || "Status update failed");
        }
    } catch (error) {
        showToast("Status update error: " + error.message);
    }
}
