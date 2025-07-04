// === Global Variables ===
let users = [];
let expenses = [];
let budgetRequests = [];

// === Login ===
function login() {
  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  if (username === "admin" && password === "admin") {
    document.getElementById("login-screen").style.display = "none";
    document.getElementById("dashboard").style.display = "flex";
    updateDashboard();
  } else {
    alert("Invalid credentials");
  }
}

function logout() {
  document.getElementById("dashboard").style.display = "none";
  document.getElementById("login-screen").style.display = "flex";
}

// === Tabs ===
function showTab(tabId) {
  const tabs = document.querySelectorAll(".tab");
  tabs.forEach(tab => tab.classList.remove("active"));
  document.getElementById(tabId).classList.add("active");
}

// === Sidebar Toggle ===
function toggleSidebar() {
  const sidebar = document.getElementById("sidebar");
  const content = document.getElementById("content");
  sidebar.classList.toggle("active");
  content.classList.toggle("content-shift");
}

// === User Modal ===
function openUserModal() {
  document.getElementById("userModal").style.display = "flex";
}
function closeUserModal() {
  document.getElementById("userModal").style.display = "none";
}

// === Password Visibility ===
function togglePassword(id, icon) {
  const input = document.getElementById(id);
  input.type = input.type === "password" ? "text" : "password";
}

// === Create User ===
function createUser() {
  const username = document.getElementById("newUsername").value;
  const email = document.getElementById("newEmail").value;
  const password = document.getElementById("newPassword").value;
  const id = users.length + 1;

  if (!username || !email || !password) return alert("Fill all fields");

  users.push({ id, username, email, password });
  closeUserModal();
  updateUserTable();
  updateDashboard();
}

function deleteUser(index) {
  users.splice(index, 1);
  updateUserTable();
  updateDashboard();
}

// === Update Tables ===
function updateUserTable() {
  const tbody = document.getElementById("users-table");
  tbody.innerHTML = "";
  users.forEach((user, index) => {
    const row = `<tr><td>${user.id}</td><td>${user.username}</td><td>${user.email}</td><td>${user.password}</td>
    <td><button onclick="deleteUser(${index})">Delete</button></td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateBudgetTable() {
  const tbody = document.getElementById("budget-table");
  tbody.innerHTML = "";
  budgetRequests.forEach((req, index) => {
    const row = `<tr><td>${req.id}</td><td>${req.username}</td><td>₱${req.amount}</td><td>${req.status}</td>
      <td><button onclick="updateRequest(${index}, 'APPROVED')">Approve</button>
      <button onclick="updateRequest(${index}, 'DENIED')">Deny</button></td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateRequest(index, status) {
  budgetRequests[index].status = status;
  updateBudgetTable();
}

function updateExpenseTable() {
  const tbody = document.getElementById("expenses-table");
  tbody.innerHTML = "";
  expenses.forEach(exp => {
    const row = `<tr><td>${exp.id}</td><td>${exp.userId}</td><td>${exp.category}</td><td>₱${exp.amount}</td><td>${exp.date}</td><td>${exp.remarks}</td></tr>`;
    tbody.innerHTML += row;
  });
}

function updateDashboard() {
  document.querySelector(".balance-card h1").textContent = `₱${expenses.reduce((acc,e)=>acc+e.amount,0)}`;
  document.querySelectorAll(".balance-card h1")[1].textContent = users.length;
  updateChart();
  updateUserTable();
  updateBudgetTable();
  updateExpenseTable();
}

// === Chart.js Pie Chart ===
let pieChart;
function updateChart() {
  const ctx = document.getElementById("expenseChart").getContext("2d");
  const categories = [...new Set(expenses.map(e => e.category))];
  const amounts = categories.map(cat => expenses.filter(e => e.category === cat).reduce((acc, e) => acc + e.amount, 0));

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
        legend: {
          position: 'bottom'
        }
      }
    }
  });
}
