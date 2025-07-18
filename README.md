# ExpenSEEs-API

### Overview

ExpenSEEs-API is the backend for the ExpenSEEs expense tracking system, designed to support the Admin Dashboard and mobile application for efficient financial management. This API, developed over six weeks in 2025 by Al Francis B. Paz (Backend Developer) and Andrew Emmanuel A. Abarientos (Frontend Developer) during their internship at REVLV, powers user management, budget operations, expense tracking, and liquidation reports. As a milestone project for two 4th-year Computer Engineering students at Ateneo de Naga University, it provides a robust RESTful interface for seamless integration with the frontend.

Features





User Management: Create, retrieve, delete, and reset passwords for non-admin users.



Budget Management: Handle budget requests with approval, denial, and deletion capabilities.



Expense Tracking: Manage expenses with details like category, amount, date, and receipt images.



Liquidation Reports: Process liquidation reports with detailed expense breakdowns and receipt images.



User Authentication: Secure login, logout, token refresh, and password reset functionality using JWT.



Profile Management: Support uploading and retrieving user profile pictures.



Real-time Data: Provide REST API endpoints for dynamic data operations.



Security: Implement Spring Security with JWT-based authentication and role-based access control.

### Tech Stack for Backend:





Spring Boot: REST API framework



Java: Core programming language



PostgreSQL: Database for persistent storage



Spring Data JPA: Simplified database operations



Spring Security: Authentication and authorization



Utilities:





JWT: Token-based authentication



Maven: Dependency management



API Base URL: http://152.42.192.226:8080/



Language: Java



Tools:





IntelliJ IDEA: Development environment



Postman: API testing



Git: Version control



PostgreSQL: Database management

Requirements





PostgreSQL: Version 13 or higher for database storage



Postman: For testing API endpoints



IntelliJ IDEA: For developing and running the Spring Boot application (Community or Ultimate edition)



Java Development Kit (JDK): Version 17 or higher



Maven: For dependency management



Git: For cloning the repository



Server Access: For deployment (e.g., AWS, DigitalOcean, or local server with public IP)

### Installation

Clone the Repository





Clone the ExpenSEEs-API repository:

git clone git@github.com:revlv-internauts/ExpenSEEs-API.git

Set Up PostgreSQL Database





Install PostgreSQL if not already installed.



Create a database named expense_tracker:

CREATE DATABASE expense_tracker;



Configure the database connection in src/main/resources/application.properties:

spring.datasource.url=jdbc:postgresql://localhost:5432/expense_tracker
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update

Open in IntelliJ IDEA





Open IntelliJ IDEA and import the cloned ExpenSEEs-API project.



Ensure JDK 17 or higher is configured in the project settings.



Sync the project with Maven to resolve dependencies (click the Maven icon or run mvn install).

Run the Backend





Locate the main class ExpenSEEsApiApplication.java in src/main/java.



Run the application in IntelliJ IDEA (use the "Run" button or Shift+F10).



The backend will start on http://localhost:8080/ by default (configurable in application.properties).

Test API Endpoints





Open Postman to test the API.



Set the base URL to http://localhost:8080/ or the deployed server URL (http://152.42.192.226:8080/).



Test endpoints (see API Endpoints section) by sending requests with appropriate headers (e.g., Authorization: Bearer <token> for protected endpoints).



Obtain a JWT token via POST /api/auth/sign-in and include it in subsequent requests.

### Deploy the Backend





Package the application as a JAR file:

mvn clean package



Copy the generated JAR (e.g., target/expensees-api.jar) to the server.



Run the JAR on the server:

java -jar expensees-api.jar



Ensure the server is accessible at http://152.42.192.226:8080/ or update the frontend to point to the deployed URL.



Configure CORS in application.properties to allow requests from the frontend domain:

spring.web.cors.allowed-origins=http://your-frontend-domain

Usage





Authentication: Log in via POST /api/auth/sign-in to obtain a JWT token for accessing protected endpoints.



User Management: Use endpoints to create, retrieve, or delete users and reset passwords.



Budgets: Approve, deny, or delete budget requests via dedicated endpoints.



Expenses: Retrieve or delete expenses, including associated receipt images.



Liquidations: Manage liquidation reports with approval, denial, or deletion actions.



Profile: Upload or retrieve profile pictures for users.



Integration: Connect the API to the ExpenSEEs Admin Dashboard or mobile app by updating the frontend’s SERVER_URL.

### Project Structure





src/main/java/:





ExpenSEEsApiApplication.java: Main application entry point.



controllers/: REST controllers for authentication, users, budgets, expenses, and liquidations.



services/: Business logic for API operations.



repositories/: Spring Data JPA repositories for database access.



models/: Entity classes mapping to PostgreSQL tables.



src/main/resources/:





application.properties: Configuration for database, server port, and CORS.



pom.xml: Maven configuration for dependencies.

### API Endpoints

Authentication





POST /api/auth/sign-in: Authenticate admin or user and return a JWT token.



POST /api/auth/refresh-token: Refresh an expired JWT token.



POST /api/forgotPassword/reset-password: Reset a user’s password via email link.

Users





GET /api/users: Retrieve all non-admin users.



POST /api/users: Create a new user.



DELETE /api/users/{userId}: Delete a user.



POST /api/users/{userId}/reset-password: Reset a user’s password.

Budgets





GET /api/budgets: Retrieve all budget requests.



POST /api/budgets/{budgetId}/release: Approve a budget.



POST /api/budgets/{budgetId}/deny: Deny a budget.



DELETE /api/budgets/{budgetId}: Delete a budget.

Expenses





GET /api/expenses: Retrieve all expenses.



DELETE /api/expenses/{expenseId}: Delete an expense.

Liquidation Reports





GET /api/liquidation: Retrieve all liquidation reports.



POST /api/liquidation/{liquidationId}/approve: Approve a liquidation.



POST /api/liquidation/{liquidationId}/deny: Deny a liquidation.



DELETE /api/liquidation/{liquidationId}: Delete a liquidation.

Profile





POST /api/users/{userId}/profile-picture: Upload a user’s profile picture.



GET /api/users/{userId}/profile-picture: Retrieve a user’s profile picture.

Testing with Postman





### Setup:





Open Postman and set the base URL to http://localhost:8080/ or http://152.42.192.226:8080/.



Create requests for each endpoint listed above.



Authentication:





Send a POST /api/auth/sign-in request with credentials to obtain a JWT token.



Add the token to the Authorization header (Bearer <token>) for protected endpoints.



Testing Scenarios:





Test user creation and deletion.



Verify budget approval and denial workflows.



Check expense retrieval and deletion.



Validate liquidation report operations.



Test profile picture upload and retrieval.

Deployment Notes





### Backend Deployment:





Ensure the server has Java 17+ and PostgreSQL installed.



Update application.properties with the server’s PostgreSQL credentials.



Run the JAR file and verify accessibility at http://152.42.192.226:8080/.



Configure CORS to allow requests from the frontend domain.



### Frontend Integration:





Update the Admin Dashboard or mobile app’s SERVER_URL to match the deployed backend URL.



Test connectivity to ensure the frontend can access all API endpoints.

Contact

For inquiries, contact:





Al Francis B. Paz (Backend Developer): 09772153941



Andrew Emmanuel A. Abarientos (Frontend Developer): 09070357944

Thank you for using ExpenSEEs-API!