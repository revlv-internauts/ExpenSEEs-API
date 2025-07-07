let users = [];
let expenses = [];
let budgetRequests = [];
let token = null;
let selectedUserIndex = null;

// =================== AUTH ===================

async function login() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  try {
    const response = await fetch("http://localhost:8080/api/auth/sign-in", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ usernameOrEmail: username, password })
    });

    const data = await response.json();
    if (response.ok && data.access_token) {
      token = data.access_token;
      document.getElementById("login-screen").style.display = "none";
      document.getElementById("dashboard").style.display = "flex";
      updateDashboard();
    } else {
      alert(data.error || "Login failed");
    }
  } catch (error) {
    alert("Network error: " + error.message);
  }
}

function logout() {
  token = null;
  document.getElementById("dashboard").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
}

// =================== UI CONTROL ===================

function showTab(tabId) {
  const tabs = document.querySelectorAll(".tab");
  tabs.forEach(tab => tab.classList.remove("active"));
  document.getElementById(tabId).classList.add("active");
}

function toggleSidebar() {
  const sidebar = document.getElementById("sidebar");
  const content = document.getElementById("content");
  sidebar.classList.toggle("active");
  content.classList.toggle("content-shift");
}

function togglePassword(id, icon) {
  const input = document.getElementById(id);
  input.type = input.type === "password" ? "text" : "password";
}

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
    const usersRes = await fetch("http://localhost:8080/api/admin/users", {
      headers: { Authorization: `Bearer ${token}` }
    });
    const usersData = await usersRes.json();
    if (usersRes.ok) users = usersData;

    // EXPENSES
    const expensesRes = await fetch("http://localhost:8080/api/expenses/all", {
      headers: { Authorization: `Bearer ${token}` }
    });
    const expensesData = await expensesRes.json();
    if (expensesRes.ok) expenses = expensesData;

    // BUDGET REQUESTS
    const budgetRes = await fetch("http://localhost:8080/api/budgets", {
      headers: { Authorization: `Bearer ${token}` }
    });
    const budgetData = await budgetRes.json();
    if (budgetRes.ok) {
      budgetRequests = budgetData.map(b => ({
        budgetId: b.budgetId,
        username: b.user?.username || "Unknown",
        amount: b.total || 0,
        status: b.status || "PENDING"
      }));
    }

    // UPDATE UI
    document.querySelector(".balance-card h1").textContent = `₱${expenses.reduce((sum, e) => sum + (e.amount || 0), 0)}`;
    document.querySelectorAll(".balance-card h1")[1].textContent = users.filter(u => u.username?.toLowerCase() !== "admin").length;
    updateChart();
    updateUserTable();
    updateBudgetTable();
    updateExpenseTable();
  } catch (error) {
    alert("Dashboard error: " + error.message);
  }
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
    const response = await fetch("http://localhost:8080/api/admin/users", {
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
    } else {
      const msg = data?.error || "Failed to create user";
      if (msg.includes("already exists")) {
        errorEl.textContent = "Username or email already exists.";
      } else {
        errorEl.textContent = msg;
      }
      errorEl.style.display = "block";
    }
  } catch (error) {
    errorEl.textContent = "Network error. Please try again.";
    errorEl.style.display = "block";
  }
}

async function deleteUser(index) {
  try {
    const user = users[index];
    const response = await fetch(`http://localhost:8080/api/admin/users/${user.userId}`, {
      method: "DELETE",
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (response.ok) {
      users.splice(index, 1);
      updateUserTable();
      updateDashboard();
    } else {
      const data = await response.json();
      alert(data.error || "Delete failed");
    }
  } catch {
    alert("Delete error");
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

  const popup = document.createElement("div");
  popup.className = "user-popup";
  popup.innerHTML = `
    <div class="popup-card">
      <h3>User Details</h3>
      <p><strong>ID:</strong> ${user.userId}</p>
      <p><strong>Username:</strong> ${user.username}</p>
      <p><strong>Email:</strong> ${user.email}</p>
      <div class="popup-actions" id="userPopupActions-${index}">
        <button>Delete</button>
        <button>Close</button>
      </div>
    </div>
  `;
  document.body.appendChild(popup);
  popup.onclick = e => { if (e.target === popup) closePopup(popup); };

  // Add event listeners after the popup is added to the DOM
  const actionsDiv = document.getElementById(`userPopupActions-${index}`);
  actionsDiv.querySelector("button:nth-child(1)").addEventListener("click", () => confirmDeleteUser(popup));
  actionsDiv.querySelector("button:nth-child(2)").addEventListener("click", () => closePopup(popup));
}

function confirmDeleteUser(popup) {
  const confirmPopup = document.createElement("div");
  confirmPopup.className = "user-popup";
  confirmPopup.innerHTML = `
    <div class="popup-card">
      <h3>Confirm Deletion</h3>
      <p>Are you sure you want to delete this user?</p>
      <div class="popup-actions" id="confirmPopupActions">
        <button style="background-color: red; color: white;">Yes, Delete</button>
        <button>Cancel</button>
      </div>
    </div>
  `;
  document.body.appendChild(confirmPopup);
  confirmPopup.onclick = e => { if (e.target === confirmPopup) closePopup(confirmPopup); };

  // Add event listeners after the popup is added to the DOM
  const actionsDiv = document.getElementById("confirmPopupActions");
  actionsDiv.querySelector("button:nth-child(1)").addEventListener("click", () => {
    const filtered = users.filter(u => u.username?.toLowerCase() !== "admin");
    const actualIndex = users.findIndex(u => u.userId === filtered[selectedUserIndex].userId);
    deleteUser(actualIndex);
    closePopup(confirmPopup);
  });
  actionsDiv.querySelector("button:nth-child(2)").addEventListener("click", () => closePopup(confirmPopup));
}

function closePopup(popupElement = null) {
    if (popupElement) {
        popupElement.remove();
    } else {
        document.querySelectorAll(".user-popup").forEach(el => el.remove());
        document.getElementById("expenseImagePopup").style.display = "none";
    }
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
      <td><button onclick="showUserDetails(${users.findIndex(u => u.userId === user.userId)})">View</button></td>
    `;
    tbody.appendChild(row);
  });

  document.getElementById("no-users-message").style.display = filtered.length === 0 ? "block" : "none";
}

// =================== OTHER TABLES ===================

function updateBudgetTable() {
  const tbody = document.getElementById("budget-table");
  tbody.innerHTML = "";
  budgetRequests.forEach((req, index) => {
    const row = `<tr><td>${req.budgetId}</td><td>${req.username}</td><td>₱${req.amount}</td><td>${req.status}</td>
      <td><button onclick="updateRequest(${index}, 'APPROVED')">Approve</button>
      <button onclick="updateRequest(${index}, 'DENIED')">Deny</button></td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateExpenseTable() {
    const tbody = document.getElementById("expenses-table");
    tbody.innerHTML = "";

    const sortedExpenses = getSortedExpenses();
    const sortValue = document.getElementById("sortExpense")?.value || "date";

    if (sortValue === "amount") {
        // Display amount sort as a flat list (highest to lowest)
        sortedExpenses.forEach(exp => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${exp.user.username || 'Unknown'}</td>
                <td>${exp.category || ''}</td>
                <td>₱${exp.amount || 0}</td>
                <td>${exp.dateOfTransaction || ''}</td>
                <td>${exp.remarks || ''}</td>
                <td><button onclick="showExpenseImage(${exp.expenseId})">View</button></td>
            `;
            tbody.appendChild(row);
        });
    } else {
        // Function to group expenses based on sort value (for date, user, category)
        const groupExpenses = (expenses, key) => {
            const groups = {};
            expenses.forEach(exp => {
                let groupKey;
                switch (key) {
                    case "date":
                        groupKey = exp.dateOfTransaction ? new Date(exp.dateOfTransaction).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'No Date';
                        break;
                    case "user":
                        groupKey = exp.user?.username || 'Unknown';
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

        // Group expenses based on the sort value
        const groupedExpenses = groupExpenses(sortedExpenses, sortValue);

        // Iterate over grouped items
        Object.keys(groupedExpenses).sort().forEach((groupKey, index) => {
            // Add group header
            const headerRow = document.createElement("tr");
            headerRow.innerHTML = `<td colspan="6" style="background: linear-gradient(90deg, #ffe6e6, #ffffff); font-weight: bold; padding: 0.5rem; border-top: 1px solid #ffd1c5;">${groupKey}</td>`;
            tbody.appendChild(headerRow);

            // Add expense rows for this group
            groupedExpenses[groupKey].forEach(exp => {
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td>${exp.user.username || 'Unknown'}</td>
                    <td>${exp.category || ''}</td>
                    <td>₱${exp.amount || 0}</td>
                    <td>${exp.dateOfTransaction || ''}</td>
                    <td>${exp.remarks || ''}</td>
                    <td><button onclick="showExpenseImage(${exp.expenseId})">View</button></td>
                `;
                tbody.appendChild(row);
            });

            // Add separator after each group except the last one
            if (index < Object.keys(groupedExpenses).length - 1) {
                const separatorRow = document.createElement("tr");
                separatorRow.innerHTML = `<td colspan="6" style="height: 0.5rem; background: linear-gradient(to right, #ffd1c5, #ffffff); border-bottom: 1px solid #ffd1c5;"></td>`;
                tbody.appendChild(separatorRow);
            }
        });
    }
}

function getSortedExpenses() {
  const sortValue = document.getElementById("sortExpense")?.value || "date";
  return [...expenses].sort((a, b) => {
    switch (sortValue) {
      case "amount":
        return (b.amount || 0) - (a.amount || 0); // Highest to lowest
      case "category":
        return (a.category || '').localeCompare(b.category || '');
      case "user":
        return (a.user?.username || '').localeCompare(b.user?.username || '');
      default: // date
        return new Date(b.dateOfTransaction) - new Date(a.dateOfTransaction); // Latest to oldest
    }
  });
}

function showExpenseImage(expenseId) {
    const popup = document.getElementById("expenseImagePopup");
    const img = document.getElementById("popup-expense-img");
    if (expenseId) {
        console.log("Fetching image for expense ID:", expenseId, "with token:", token);
        fetch(`http://localhost:8080/api/expenses/${expenseId}/images?index=0`, {
            headers: { "Authorization": `Bearer ${token}` }
        })
        .then(response => {
            if (!response.ok) throw new Error(`Image not found: ${response.status} ${response.statusText}`);
            return response.blob();
        })
        .then(blob => {
            img.src = URL.createObjectURL(blob);
            popup.style.display = "flex";
        })
        .catch(error => {
            console.error("Error loading image for expense ID " + expenseId + ":", error);
            alert("Failed to load image for expense ID " + expenseId + ": " + error.message);
        });
    } else {
        img.src = '';
        popup.style.display = "flex";
    }
}

function closePopup(popupElement = null) {
    if (popupElement) {
        popupElement.remove();
    } else {
        document.querySelectorAll(".user-popup").forEach(el => el.remove());
        document.getElementById("expenseImagePopup").style.display = "none";
    }
    selectedUserIndex = null;
}

async function updateRequest(index, status) {
  try {
    const budget = budgetRequests[index];
    const response = await fetch(`http://localhost:8080/api/budgets/${budget.budgetId}/status`, {
      method: "PUT",
      headers: {
        "Content-Type": "text/plain",
        "Authorization": `Bearer ${token}`
      },
      body: status
    });
    if (response.ok) {
      budgetRequests[index].status = status;
      updateBudgetTable();
    } else {
      const data = await response.json();
      alert(data.error || "Status update failed");
    }
  } catch {
    alert("Update failed");
  }
}

// =================== CHART ===================

let pieChart;
function updateChart() {
  const ctx = document.getElementById("expenseChart").getContext("2d");
  const categories = [...new Set(expenses.map(e => e.category))];
  const amounts = categories.map(cat => expenses.filter(e => e.category === cat).reduce((sum, e) => sum + e.amount, 0));

  if (pieChart) pieChart.destroy();
  pieChart = new Chart(ctx, {
    type: 'pie',
    data: {
      labels: categories,
      datasets: [{
        data: amounts,
        backgroundColor: ['#111', '#888', '#ccc', '#f46354']
      }]
    },
    options: {
      responsive: true,
      plugins: {
        legend: { position: 'bottom' }
      }
    }
  });
}