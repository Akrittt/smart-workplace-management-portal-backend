# 🏢 Smart Workplace Management Portal - Backend

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![Java](https://img.shields.io/badge/Java-20-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17.4-blue)
![AI Powered](https://img.shields.io/badge/AI-Groq%20Llama-purple)

A comprehensive RESTful API backend for workplace management, featuring AI-powered assistance, leave management, complaint handling, and role-based access control.

## 🌟 Features

### Core Features
- 🔐 **JWT Authentication & Authorization** - Secure token-based authentication
- 👥 **Role-Based Access Control** - EMPLOYEE, MANAGER, ADMIN roles
- 📅 **Leave Management System** - Request, approve, reject, and track leaves
- 📝 **Complaint Management** - File, assign, and resolve workplace complaints
- 🤖 **AI Chatbot Assistant** - Groq-powered conversational AI
- 📊 **Admin Dashboard** - User management, analytics, and system settings
- 🔍 **Comprehensive Search & Filtering** - Advanced query capabilities
- 📈 **Analytics & Reporting** - System usage statistics and insights

### Technical Features
- RESTful API design with OpenAPI documentation
- PostgreSQL database with JPA/Hibernate ORM
- Spring Security with JWT tokens
- Input validation and error handling
- Comprehensive logging
- CORS configuration for frontend integration

## 🛠️ Tech Stack

### Backend Framework
- **Spring Boot** 3.5.6
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database operations
- **Spring Web** - REST API

### Database
- **PostgreSQL** 17.4
- **HikariCP** - Connection pooling
- **Hibernate** - ORM

### AI Integration
- **Groq AI API** - Llama 3.3 70B model
- **OkHttp** - HTTP client for API calls

### Security
- **JWT (JSON Web Tokens)** - io.jsonwebtoken
- **BCrypt** - Password hashing
- **Spring Security** - Authorization framework

### Build Tools
- **Maven** - Dependency management
- **Lombok** - Reduce boilerplate code

## 📋 Prerequisites

Before running this application, ensure you have:

- **Java 20** or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **PostgreSQL 15+** ([Download](https://www.postgresql.org/download/))
- **Groq API Key** - Free from [console.groq.com](https://console.groq.com/keys)

  
## 📁 Project Structure

```
src/
├── main/
│ ├── java/com/example/Smart/Workplace/Management/Portal/
│ │ ├── controller/ # REST API Controllers
│ │ │ ├── AuthController.java
│ │ │ ├── LeaveController.java
│ │ │ ├── ComplaintController.java
│ │ │ ├── AdminController.java
│ │ │ └── AIAssistantController.java
│ │ ├── service/ # Business Logic
│ │ │ ├── AuthService.java
│ │ │ ├── LeaveService.java
│ │ │ ├── ComplaintService.java
│ │ │ ├── AIAssistantService.java
│ │ │ └── GroqAIService.java
│ │ ├── repository/ # Database Access
│ │ │ ├── UserRepository.java
│ │ │ ├── LeaveRequestRepository.java
│ │ │ ├── ComplaintRepository.java
│ │ │ └── ChatMessageRepository.java
│ │ ├── model/ # Entity Classes
│ │ │ ├── User.java
│ │ │ ├── LeaveRequest.java
│ │ │ ├── Complaint.java
│ │ │ └── ChatMessage.java
│ │ ├── dto/ # Data Transfer Objects
│ │ │ ├── AuthResponse.java
│ │ │ ├── LeaveRequestDto.java
│ │ │ └── ComplaintDto.java
│ │ └── security/ # Security Configuration
│ │ ├── SecurityConfig.java
│ │ ├── JwtAuthFilter.java
│ │ ├── JwtService.java
│ │ └── UserDetailsServiceImpl.java
│ └── resources/
│ └── application.properties
└── test/ # Unit Tests

```


## 🔌 API Endpoints

### Authentication
```
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login user | No |
```
### Leave Management
```
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/leave/submit` | Submit leave request | Yes |
| GET | `/api/leave/my-requests` | Get user's leaves | Yes |
| GET | `/api/leave/all` | Get all leaves (Manager/Admin) | Yes |
| PUT | `/api/leave/{id}/approve` | Approve leave | Manager/Admin |
| PUT | `/api/leave/{id}/reject` | Reject leave | Manager/Admin |
```
### Complaint Management
```
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/complaints` | File complaint | Yes |
| GET | `/api/complaints/my` | Get user's complaints | Yes |
| GET | `/api/complaints/all` | Get all complaints | Manager/Admin |
| PUT | `/api/complaints/{id}/assign/{staffId}` | Assign complaint | Manager/Admin |
| PUT | `/api/complaints/{id}` | Update complaint status | Yes |

### AI Assistant

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/ai-assistant/chat` | Chat with AI | Yes |
| GET | `/api/ai-assistant/history` | Get chat history | Yes |
```
### Admin Panel
```
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/admin/users` | Get all users | Admin |
| PUT | `/api/admin/users/{id}/toggle-active` | Toggle user status | Admin |
| PUT | `/api/admin/users/{id}/role` | Update user role | Admin |
| GET | `/api/admin/dashboard` | Get dashboard stats | Admin |
| GET | `/api/admin/analytics/leaves` | Leave analytics | Admin |
```
## 🔑 Environment Variables


## 🔒 Security Best Practices

- ✅ Passwords are hashed using BCrypt
- ✅ JWT tokens expire after 24 hours
- ✅ CORS configured for specific origins
- ✅ Input validation on all endpoints
- ✅ SQL injection prevention via JPA
- ✅ Rate limiting (configure as needed)








