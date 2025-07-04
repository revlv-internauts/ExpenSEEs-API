const transactions = [
  { category: 'Food', date: '2025-06-28', amount: '₱ 500', userId: 1 },
  { category: 'Transportation', date: '2025-07-01', amount: '₱ 1,000', userId: 2 },
  { category: 'Parking', date: '2025-07-02', amount: '₱ 500', userId: 3 },
  { category: 'Utilities', date: '2025-07-03', amount: '₱ 1,000', userId: 4 },
  { category: 'Grocery', date: '2025-07-03', amount: '₱ 500', userId: 5 },
  { category: 'Other Expenses', date: '2025-07-04', amount: '₱ 1,000', userId: 6 }
];

function login() {
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value.trim();

  if (username && password) {
    // Hide login screen and show dashboard
    document.getElementById('loginScreen').style.display = 'none';
    document.getElementById('dashboard').style.display = 'flex';
    loadTransactions();
  } else {
    alert('Please enter both username and password');
  }
}

function loadTransactions() {
  const table = document.getElementById('transactionTable');
  transactions.forEach(tx => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>${tx.category}</td>
      <td>${tx.date}</td>
      <td>${tx.amount}</td>
      <td>${tx.userId}</td>
    `;
    table.appendChild(row);
  });
}

function logout() {
  document.getElementById('dashboard').style.display = 'none';
  document.getElementById('loginScreen').style.display = 'flex';
}
