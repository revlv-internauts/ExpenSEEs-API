document.addEventListener('DOMContentLoaded', () => {
    const app = document.getElementById('app');
    const loginScreen = document.getElementById('login-screen');
    const forgotPasswordScreen = document.getElementById('forgot-password-screen');
    const dashboardScreen = document.getElementById('dashboard-screen');
    const loginForm = document.getElementById('login-form');
    const forgotForm = document.getElementById('forgot-form');
    const loginEmail = document.getElementById('login-email');
    const loginPassword = document.getElementById('login-password');
    const loginError = document.getElementById('login-error');
    const forgotEmail = document.getElementById('forgot-email');
    const otpInput = document.getElementById('otp-input');
    const newPassword = document.getElementById('new-password');
    const confirmPassword = document.getElementById('confirm-password');
    const forgotError = document.getElementById('forgot-error');
    const jwtTokenInput = document.getElementById('jwt-token');
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebar-toggle');
    const expensesBtn = document.getElementById('expenses-btn');
    const budgetBtn = document.getElementById('budget-btn');
    const logoutBtn = document.getElementById('logout-btn');
    const profileName = document.getElementById('profile-name');
    const profileEmail = document.getElementById('profile-email');
    const totalAmount = document.getElementById('total-amount');
    const topExpensesList = document.getElementById('top-expenses-list');
    const expenseListItems = document.getElementById('expense-list-items');
    const expenseChartCanvas = document.getElementById('expense-chart').getContext('2d');
    const addExpenseBtn = document.getElementById('add-expense-btn');
    const addExpenseModal = document.getElementById('add-expense-modal');
    const addExpenseForm = document.getElementById('add-expense-form');
    const expenseModalTitle = document.getElementById('expense-modal-title');
    const expenseIdInput = document.getElementById('expense-id');
    const closeModalBtn = document.getElementById('close-modal-btn');
    const sendOtpBtn = document.getElementById('send-otp-btn');
    const verifyOtpBtn = document.getElementById('verify-otp-btn');
    const resetPasswordBtn = document.getElementById('reset-password-btn');
    const takePhotoBtn = document.getElementById('take-photo-btn');
    const pickGalleryBtn = document.getElementById('pick-gallery-btn');
    const receiptPhoto = document.getElementById('receipt-photo');
    const receiptGallery = document.getElementById('receipt-gallery');
    const imagePreview = document.getElementById('image-preview');
    const expenseDetailsModal = document.getElementById('expense-details-modal');
    const detailCategory = document.getElementById('detail-category');
    const detailAmount = document.getElementById('detail-amount');
    const detailDate = document.getElementById('detail-date');
    const detailRemarks = document.getElementById('detail-remarks');
    const detailImage = document.getElementById('detail-image');
    const editExpenseBtn = document.getElementById('edit-expense-btn');
    const deleteExpenseBtn = document.getElementById('delete-expense-btn');
    const closeDetailsModalBtn = document.getElementById('close-details-modal-btn');
    const resetPasswordBtnSidebar = document.getElementById('reset-password-btn-sidebar');
    const resetPasswordModal = document.getElementById('reset-password-modal');
    const resetPasswordForm = document.getElementById('reset-password-form');
    const closeResetModalBtn = document.getElementById('close-reset-modal-btn');
    const resetError = document.getElementById('reset-error');
    let isAdmin = false;
    let expenses = [];
    let chart;
    let otp;

    const transitionScreen = (from, to) => {
        from.classList.add('fade-out');
        setTimeout(() => {
            from.classList.remove('active', 'fade-out');
            to.classList.add('active', 'fade-in');
            setTimeout(() => to.classList.remove('fade-in'), 300);
        }, 300);
    };

    const login = async () => {
        const credentials = {
            usernameOrEmail: loginEmail.value.trim(),
            password: loginPassword.value.trim()
        };
        try {
            const response = await fetch('http://localhost:8080/api/auth/sign-in', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(credentials)
            });
            const data = await response.json();
            if (response.ok) {
                jwtTokenInput.value = data.jwt;
                profileName.textContent = data.username;
                profileEmail.textContent = data.email;
                isAdmin = data.username === 'admin';
                budgetBtn.style.display = isAdmin ? 'block' : 'none';
                loginError.textContent = '';
                transitionScreen(loginScreen, dashboardScreen);
                loadDashboardData();
            } else {
                loginError.textContent = data.message || 'Login failed';
            }
        } catch (error) {
            loginError.textContent = 'Error connecting to server';
        }
    };

    const sendOtp = async () => {
        try {
            const response = await fetch(`http://localhost:8080/forgotPassword/verifyMail/${forgotEmail.value}`, {
                method: 'POST'
            });
            const text = await response.text();
            if (response.ok) {
                forgotError.textContent = text;
                otpInput.style.display = 'block';
                verifyOtpBtn.style.display = 'block';
                sendOtpBtn.style.display = 'none';
            } else {
                forgotError.textContent = text;
            }
        } catch (error) {
            forgotError.textContent = 'Error sending OTP';
        }
    };

    const verifyOtp = async () => {
        try {
            const response = await fetch(`http://localhost:8080/forgotPassword/verifyOtp/${otpInput.value}/${forgotEmail.value}`, {
                method: 'POST'
            });
            const data = await response.json();
            if (response.ok && data.status === 'success') {
                forgotError.textContent = data.message;
                newPassword.style.display = 'block';
                confirmPassword.style.display = 'block';
                resetPasswordBtn.style.display = 'block';
                verifyOtpBtn.style.display = 'none';
                otp = otpInput.value;
            } else {
                forgotError.textContent = data.message || 'Invalid OTP';
            }
        } catch (error) {
            forgotError.textContent = 'Error verifying OTP';
        }
    };

    const resetPassword = async () => {
        if (newPassword.value !== confirmPassword.value) {
            forgotError.textContent = 'Passwords do not match';
            return;
        }
        try {
            const response = await fetch(`http://localhost:8080/forgotPassword/changePassword/${forgotEmail.value}?otp=${otp}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password: newPassword.value, repeatPassword: confirmPassword.value })
            });
            const text = await response.text();
            if (response.ok) {
                forgotError.textContent = text;
                transitionScreen(forgotPasswordScreen, loginScreen);
            } else {
                forgotError.textContent = text;
            }
        } catch (error) {
            forgotError.textContent = 'Error resetting password';
        }
    };

    const loadDashboardData = async () => {
        const token = jwtTokenInput.value;
        try {
            const [expensesResponse, totalResponse, distributionResponse, topResponse, recentResponse] = await Promise.all([
                fetch('http://localhost:8080/api/expenses', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('http://localhost:8080/api/expenses/total-amount', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('http://localhost:8080/api/expenses/distribution', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('http://localhost:8080/api/expenses/top', { headers: { 'Authorization': `Bearer ${token}` } }),
                fetch('http://localhost:8080/api/expenses/recent?limit=10', { headers: { 'Authorization': `Bearer ${token}` } })
            ]);

            expenses = await expensesResponse.json();
            const total = await totalResponse.json();
            const distribution = await distributionResponse.json();
            const topExpenses = await topResponse.json();
            const recentExpenses = await recentResponse.json();

            totalAmount.textContent = `Php ${total.toFixed(2)}`;
            topExpensesList.innerHTML = topExpenses.map((exp, index) => {
                const colors = ['#FF4500', '#00CED1', '#FFD700', '#ADFF2F', '#FF69B4', '#FFA500'];
                const lightColors = ['#FF8C69', '#87CEEB', '#FFFACD', '#D4E4A2', '#FFB6C1', '#FFDAB9']; // Lighter versions
                const darkColors = ['#A52A2A', '#008B8B', '#B8860B', '#6B8E23', '#8A2BE2', '#CD853F']; // Darker versions
                const colorIndex = index % colors.length;
                return `<li data-id="${exp.id}" style="background-color: ${lightColors[colorIndex]}; border: 2px solid ${darkColors[colorIndex]}; padding: 0.5rem; border-radius: 4px; color: #333;">${exp.category}: Php ${exp.amount.toFixed(2)}</li>`;
            }).join('');
            expenseListItems.innerHTML = recentExpenses.map((exp, index) => {
                const colors = ['#FF4500', '#00CED1', '#FFD700', '#ADFF2F', '#FF69B4', '#FFA500'];
                const lightColors = ['#FF8C69', '#87CEEB', '#FFFACD', '#D4E4A2', '#FFB6C1', '#FFDAB9']; // Lighter versions
                const darkColors = ['#A52A2A', '#008B8B', '#B8860B', '#6B8E23', '#8A2BE2', '#CD853F']; // Darker versions
                const colorIndex = index % colors.length;
                return `<li data-id="${exp.id}" style="background-color: ${lightColors[colorIndex]}; border: 2px solid ${darkColors[colorIndex]}; padding: 0.5rem; border-radius: 4px; color: #333;">${exp.category}: Php ${exp.amount.toFixed(2)} (${exp.dateOfTransaction})</li>`;
            }).join('');

            if (chart) chart.destroy();
            chart = new Chart(expenseChartCanvas, {
                type: 'pie',
                data: {
                    labels: Object.keys(distribution),
                    datasets: [{
                        data: Object.values(distribution),
                        backgroundColor: ['#FF4500', '#00CED1', '#FFD700', '#ADFF2F', '#FF69B4', '#FFA500'],
                        borderWidth: 1
                    }]
                },
                options: { responsive: true, plugins: { legend: { position: 'top' }, title: { display: true, text: 'Expense Distribution' } } }
            });
        } catch (error) {
            loginError.textContent = 'Failed to load data';
        }
    };

    const addExpense = async (e) => {
        e.preventDefault();
        const token = jwtTokenInput.value;
        const expenseId = expenseIdInput.value;
        const expense = {
            category: document.getElementById('expense-category').value,
            amount: parseFloat(document.getElementById('expense-amount').value),
            dateOfTransaction: document.getElementById('expense-date').value,
            remarks: document.getElementById('expense-remarks').value
        };

        const imageFile = receiptPhoto.files[0] || receiptGallery.files[0];
        if (imageFile) {
            const formData = new FormData();
            formData.append('expense', JSON.stringify(expense));
            formData.append('receiptImage', imageFile);

            try {
                const response = await fetch('http://localhost:8080/api/expenses/upload-image', {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}` },
                    body: formData
                });
                if (response.ok) {
                    closeModal();
                    loadDashboardData();
                } else {
                    const error = await response.text();
                    alert(`Failed to add expense: ${error}`);
                }
            } catch (error) {
                alert('Error uploading image: ' + error.message);
            }
        } else {
            const url = expenseId ? `http://localhost:8080/api/expenses/${expenseId}` : 'http://localhost:8080/api/expenses';
            const method = expenseId ? 'PUT' : 'POST';
            try {
                const response = await fetch(url, {
                    method: method,
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                    body: JSON.stringify(expense)
                });
                if (response.ok) {
                    closeModal();
                    loadDashboardData();
                    alert(expenseId ? 'Expense updated successfully' : 'Expense added successfully');
                } else {
                    alert(expenseId ? 'Failed to update expense' : 'Failed to add expense');
                }
            } catch (error) {
                alert('Error: ' + error.message);
            }
        }
    };

    const showExpenseDetails = async (expenseId) => {
        const token = jwtTokenInput.value;
        try {
            const response = await fetch(`http://localhost:8080/api/expenses/${expenseId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const expense = await response.json();
                detailCategory.textContent = expense.category || 'N/A';
                detailAmount.textContent = `Php ${expense.amount.toFixed(2)}`;
                detailDate.textContent = expense.dateOfTransaction || 'N/A';
                detailRemarks.textContent = expense.comments || 'None';
                if (expense.imagePath) {
                    detailImage.innerHTML = `<img src="http://localhost:8080/api/expenses/image/${expense.imagePath.split('/').pop()}" alt="Receipt" class="preview-image">`;
                } else {
                    detailImage.innerHTML = 'No image';
                }
                expenseDetailsModal.classList.add('active');
                setTimeout(() => expenseDetailsModal.classList.add('show'), 10);
                editExpenseBtn.onclick = () => openExpenseModal(expense);
                deleteExpenseBtn.onclick = () => deleteExpense(expenseId);
            } else {
                const error = await response.text();
                alert(`Failed to load expense: ${error}`);
            }
        } catch (error) {
            alert('Error: ' + error.message);
        }
    };

    const openExpenseModal = (expense = null) => {
        expenseModalTitle.textContent = expense ? 'Edit Expense' : 'Add New Expense';
        expenseIdInput.value = expense ? expense.id : '';
        document.getElementById('expense-category').value = expense ? expense.category : '';
        document.getElementById('expense-amount').value = expense ? expense.amount : '';
        document.getElementById('expense-date').value = expense ? expense.dateOfTransaction : '';
        document.getElementById('expense-remarks').value = expense ? expense.comments || '' : '';
        imagePreview.innerHTML = expense && expense.imagePath ? `<img src="http://localhost:8080/api/expenses/image/${expense.imagePath.split('/').pop()}" alt="Receipt Preview" class="preview-image">` : '';
        receiptPhoto.value = '';
        receiptGallery.value = '';
        addExpenseModal.classList.add('active');
        setTimeout(() => addExpenseModal.classList.add('show'), 10);
    };

    const deleteExpense = async (expenseId) => {
        if (confirm('Are you sure you want to delete this expense?')) {
            const token = jwtTokenInput.value;
            try {
                const response = await fetch(`http://localhost:8080/api/expenses/${expenseId}`, {
                    method: 'DELETE',
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                const text = await response.text();
                if (response.ok) {
                    closeDetailsModal();
                    loadDashboardData();
                    alert(text);
                } else {
                    alert(text || 'Failed to delete expense');
                }
            } catch (error) {
                alert('Error: ' + error.message);
            }
        }
    };

    const closeModal = () => {
        addExpenseModal.classList.remove('show');
        setTimeout(() => addExpenseModal.classList.remove('active'), 300);
        receiptPhoto.value = '';
        receiptGallery.value = '';
        imagePreview.innerHTML = '';
        expenseIdInput.value = '';
        addExpenseForm.reset();
        expenseModalTitle.textContent = 'Add New Expense';
    };

    const closeDetailsModal = () => {
        expenseDetailsModal.classList.remove('show');
        setTimeout(() => expenseDetailsModal.classList.remove('active'), 300);
    };

    const toggleSidebar = () => {
        sidebar.classList.toggle('open');
        sidebar.style.transition = 'transform 0.3s ease';
    };

    const navigateToExpenses = () => alert(isAdmin ? 'Admin Expenses View' : 'User Expenses View');
    const navigateToBudget = () => alert(isAdmin ? 'Admin Budget Management' : 'User Budget Request');

    const logout = () => {
        jwtTokenInput.value = '';
        transitionScreen(dashboardScreen, loginScreen);
        if (chart) chart.destroy();
    };

    const openResetPasswordModal = () => {
        resetPasswordModal.classList.add('active');
        setTimeout(() => resetPasswordModal.classList.add('show'), 10);
    };

    const closeResetPasswordModal = () => {
        resetPasswordModal.classList.remove('show');
        setTimeout(() => resetPasswordModal.classList.remove('active'), 300);
        resetError.textContent = '';
    };

    const resetPasswordFromSidebar = async () => {
        const token = jwtTokenInput.value;
        const email = profileEmail.textContent;
        const newPassword = document.getElementById('reset-new-password').value;
        const confirmPassword = document.getElementById('reset-confirm-password').value;

        if (newPassword !== confirmPassword) {
            resetError.textContent = 'Passwords do not match';
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/api/users/reset-password?email=${encodeURIComponent(email)}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ password: newPassword, repeatPassword: confirmPassword })
            });
            const text = await response.text();
            if (response.ok) {
                resetError.textContent = text;
                closeResetPasswordModal();
            } else {
                resetError.textContent = text || 'Failed to reset password';
            }
        } catch (error) {
            resetError.textContent = 'Error: ' + error.message;
        }
    };

    const triggerFileInput = (input) => input.click();

    const previewImage = () => {
        const file = receiptPhoto.files[0] || receiptGallery.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                imagePreview.innerHTML = `<img src="${e.target.result}" alt="Receipt Preview" class="preview-image">`;
            };
            reader.onerror = () => alert('Error reading image file');
            reader.readAsDataURL(file);
        }
    };

    takePhotoBtn.addEventListener('click', () => triggerFileInput(receiptPhoto));
    pickGalleryBtn.addEventListener('click', () => triggerFileInput(receiptGallery));
    receiptPhoto.addEventListener('change', previewImage);
    receiptGallery.addEventListener('change', previewImage);

    loginForm.addEventListener('submit', (e) => { e.preventDefault(); login(); });
    forgotForm.addEventListener('submit', (e) => { e.preventDefault(); sendOtp(); });
    sendOtpBtn.addEventListener('click', sendOtp);
    verifyOtpBtn.addEventListener('click', verifyOtp);
    resetPasswordBtn.addEventListener('click', resetPassword);
    sidebarToggle.addEventListener('click', toggleSidebar);
    expensesBtn.addEventListener('click', navigateToExpenses);
    budgetBtn.addEventListener('click', navigateToBudget);
    logoutBtn.addEventListener('click', logout);
    addExpenseBtn.addEventListener('click', () => openExpenseModal());
    addExpenseForm.addEventListener('submit', addExpense);
    closeModalBtn.addEventListener('click', closeModal);
    closeDetailsModalBtn.addEventListener('click', closeDetailsModal);
    resetPasswordBtnSidebar.addEventListener('click', openResetPasswordModal);
    resetPasswordForm.addEventListener('submit', (e) => { e.preventDefault(); resetPasswordFromSidebar(); });
    closeResetModalBtn.addEventListener('click', closeResetPasswordModal);

    expenseListItems.addEventListener('click', (e) => {
        const li = e.target.closest('li');
        if (li) {
            const expenseId = li.dataset.id;
            showExpenseDetails(expenseId);
        }
    });

    topExpensesList.addEventListener('click', (e) => {
        const li = e.target.closest('li');
        if (li) {
            const expenseId = li.dataset.id;
            showExpenseDetails(expenseId);
        }
    });

    window.addEventListener('hashchange', () => {
        const hash = window.location.hash;
        if (hash === '#forgot-password') transitionScreen(loginScreen, forgotPasswordScreen);
        else if (hash === '#login') transitionScreen(forgotPasswordScreen, loginScreen);
    });
});