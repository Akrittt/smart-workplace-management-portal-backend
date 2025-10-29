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

## 🚀 Getting Started

### 1. Clone the Repository

