let users = [];
let expenses = [];
let budgetRequests = [];
let token = null;

async function login() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  try {
    console.log("Attempting login with:", { username, password });
    const response = await fetch("http://localhost:8080/api/auth/sign-in", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ usernameOrEmail: username, password })
    });
    console.log("Response status:", response.status, "OK:", response.ok);
    const data = await response.json();
    console.log("Response data:", data);
    if (response.ok) {
      token = data.access_token;
      if (!token) throw new Error("Token not found in response");
      document.getElementById("login-screen").style.display = "none";
      document.getElementById("dashboard").style.display = "flex";
      updateDashboard();
    } else {
      alert(data.error || `Login failed with status ${response.status}`);
    }
  } catch (error) {
    console.error("Login error:", error.message, error.stack);
    alert("Network error: " + error.message);
  }
}

function logout() {
  token = null;
  document.getElementById("dashboard").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
}

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

function openUserModal() {
  document.getElementById("userModal").style.display = "flex";
}
function closeUserModal() {
  document.getElementById("userModal").style.display = "none";
}

function togglePassword(id, icon) {
  const input = document.getElementById(id);
  input.type = input.type === "password" ? "text" : "password";
}

async function createUser() {
  const username = document.getElementById("newUsername").value;
  const email = document.getElementById("newEmail").value;
  const password = document.getElementById("newPassword").value;
  if (!username || !email || !password) return alert("Fill all fields");

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
      alert(data.error || "Failed to create user");
    }
  } catch (error) {
    alert("Network error");
  }
}

async function deleteUser(index) {
  try {
    const user = users[index];
    const response = await fetch(`http://localhost:8080/api/admin/users/${user.userId}`, {
      method: "DELETE",
      headers: { "Authorization": `Bearer ${token}` }
    });
    const data = await response.json();
    if (response.ok) {
      users.splice(index, 1);
      updateUserTable();
      updateDashboard();
    } else {
      alert(data.error || "Failed to delete user");
    }
  } catch (error) {
    alert("Network error");
  }
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
    const data = await response.json();
    if (response.ok) {
      budgetRequests[index].status = status;
      updateBudgetTable();
    } else {
      alert(data.error || "Failed to update status");
    }
  } catch (error) {
    alert("Network error");
  }
}

async function updateDashboard() {
  if (!token) return;

  try {
    // Fetch users
    const usersResponse = await fetch("http://localhost:8080/api/admin/users", {
      headers: { "Authorization": `Bearer ${token}` }
    });
    console.log("Users response status:", usersResponse.status, "OK:", usersResponse.ok);
    const usersData = await usersResponse.json();
    console.log("Users data:", usersData);
    if (usersResponse.ok) {
      users = usersData;
    } else {
      alert(usersData.error || "Failed to load users");
    }

    // Fetch expenses
    const expensesResponse = await fetch("http://localhost:8080/api/expenses", {
      headers: { "Authorization": `Bearer ${token}` }
    });
    const expensesData = await expensesResponse.json();
    if (expensesResponse.ok) {
      expenses = expensesData;
    } else {
      alert(expensesData.error || "Failed to load expenses");
    }

    // Fetch budgets
    const budgetsResponse = await fetch("http://localhost:8080/api/budgets", {
      headers: { "Authorization": `Bearer ${token}` }
    });
    const budgetsData = await budgetsResponse.json();
    if (budgetsResponse.ok) {
      budgetRequests = budgetsData.map(b => ({
        budgetId: b.budgetId,
        username: b.user?.username || "Unknown",
        amount: b.total || 0,
        status: b.status || "PENDING"
      })) || [];
    } else {
      alert(budgetsData.error || "Failed to load budgets");
    }

    // Update dashboard
    document.querySelector(".balance-card h1").textContent = `₱${expenses.reduce((acc, e) => acc + (e.amount || 0), 0)}`;
    document.querySelectorAll(".balance-card h1")[1].textContent = users.length || 0;
    updateChart();
    updateUserTable();
    updateBudgetTable();
    updateExpenseTable();
  } catch (error) {
    console.error("Dashboard update error:", error.message, error.stack);
    alert("Network error: " + error.message);
  }
}

function updateUserTable() {
  const tbody = document.getElementById("users-table");
  tbody.innerHTML = "";
  users.forEach((user, index) => {
    const row = `<tr><td>${user.userId || ''}</td><td>${user.username || ''}</td><td>${user.email || ''}</td><td>********</td>
      <td><button onclick="deleteUser(${index})">Delete</button></td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateBudgetTable() {
  const tbody = document.getElementById("budget-table");
  tbody.innerHTML = "";
  budgetRequests.forEach((req, index) => {
    const row = `<tr><td>${req.budgetId || ''}</td><td>${req.username || ''}</td><td>₱${req.amount || 0}</td><td>${req.status || ''}</td>
      <td><button onclick="updateRequest(${index}, 'APPROVED')">Approve</button>
      <button onclick="updateRequest(${index}, 'DENIED')">Deny</button></td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateExpenseTable() {
  const tbody = document.getElementById("expenses-table");
  tbody.innerHTML = "";
  expenses.forEach(exp => {
    const row = `<tr><td>${exp.expenseId || ''}</td><td>${exp.user?.userId || ''}</td><td>${exp.category || ''}</td><td>₱${exp.amount || 0}</td><td>${exp.dateOfTransaction || ''}</td><td>${exp.remarks || ''}</td></tr>`;
    tbody.innerHTML += row;
  });
}

let pieChart;
function updateChart() {
  const ctx = document.getElementById("expenseChart").getContext("2d");
  const categories = [...new Set(expenses.map(e => e.category))];
  const amounts = categories.map(cat => expenses.filter(e => e.category === cat).reduce((acc, e) => acc + (e.amount || 0), 0));

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