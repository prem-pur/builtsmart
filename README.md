# BuildSmart Construction Management System (CMS)

A comprehensive web-based Construction Management System developed for BuildSmart Lanka Pvt Ltd using Spring Boot and modern web technologies.

##  System Overview

The BuildSmart CMS is designed to manage various aspects of construction operations including:

- **Project Management** - Create, track, and manage construction projects
- **Task Assignment** - Assign and monitor tasks for workers and engineers
- **Attendance Tracking** - Digital attendance system for workers
- **Site Reporting** - Daily logs and progress updates from site engineers
- **Financial Management** - Expense tracking, budget management, and invoicing
- **Client Communication** - Progress updates and document sharing with clients
- **Multi-role Access Control** - Role-based access for different stakeholders

##  User Roles & Personas

### 1. Project Manager (Mr. Nuwan Perera)
- Plans and oversees construction project lifecycles
- Assigns tasks to workers and engineers
- Tracks overall project progress
- Communicates with clients and engineers

### 2. Site Engineer (Ms. Shanika Jayawardena)
- Reports site conditions and updates
- Uploads daily progress logs and photos
- Flags issues or delays to project managers
- Documents construction phases

### 3. HR Executive (Mr. Chamika Silva)
- Maintains worker profiles
- Manages attendance and leave requests
- Handles staff recordkeeping

### 4. Finance Officer (Ms. Tharushi Fernando)
- Manages project budgets
- Logs and tracks daily/monthly expenses
- Generates invoices and reports
- Coordinates payments with managers and clients

### 5. Client Representative (Mr. Ramesh Dissanayake)
- Views regular progress updates
- Accesses financial documents (invoices, reports)
- Requests clarifications from project managers

### 6. Worker
- Views assigned daily tasks
- Marks attendance
- Requests leave

##  Technology Stack

### Backend
- **Spring Boot 3.5.4** - Main framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data access layer
- **MySQL** - Database
- **Thymeleaf** - Template engine
- **Lombok** - Code generation

### Frontend
- **Bootstrap 5.3.0** - CSS framework
- **Font Awesome 6.0.0** - Icons
- **Chart.js** - Data visualization
- **Vanilla JavaScript** - Client-side functionality

### Development Tools
- **Maven** - Build tool
- **IntelliJ IDEA** - IDE
- **Postman** - API testing
- **XAMPP** - Local development environment

##  Prerequisites

Before running the application, ensure you have:

1. **Java 24** or higher
2. **MySQL 8.0** or higher
3. **Maven 3.6** or higher
4. **Git** for version control

##  Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd builtsmart
```

### 2. Database Setup
1. Start MySQL server
2. Create database (optional - will be auto-created):
```sql
CREATE DATABASE builtsmart_cms;
```

### 3. Configure Application
Update `src/main/resources/application.properties` with your database credentials:
```properties
spring.datasource.username=root
spring.datasource.password=Umeshi@0512
```

### 4. Build and Run
```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run
```

### 5. Access the Application
Open your browser and navigate to:
```
http://localhost:8080
```

##  Demo Credentials

The system comes pre-loaded with sample users for testing:

| Role | Email | Password |
|------|-------|----------|
| Project Manager | nuwan@builtsmart.lk | manager123 |
| Site Engineer | shanika@builtsmart.lk | engineer123 |
| HR Executive | chamika@builtsmart.lk | hr123 |
| Finance Officer | tharushi@builtsmart.lk | finance123 |
| Client Representative | ramesh@client.com | client123 |
| Worker | worker@builtsmart.lk | worker123 |

##  System Architecture

### Database Schema
The system uses the following main entities:

- **Users** - User management with role-based access
- **Projects** - Construction project details
- **Tasks** - Task assignments and tracking
- **Attendance** - Worker attendance records
- **Leave Requests** - Leave management
- **Logs** - Site updates and progress reports
- **Expenses** - Financial tracking
- **Invoices** - Client billing

### Security Implementation
- **Spring Security** with role-based access control
- **BCrypt** password encoding
- **Session management** with CSRF protection
- **Role-based URL access** control

##  Key Features

### Project Manager Dashboard
- Overview cards showing project statistics
- Project progress charts
- Upcoming deadlines
- Recent site updates
- Issues and alerts

### Site Engineer Features
- Upload daily logs and photos
- Report issues and delays
- View assigned tasks
- Track project progress

### HR Management
- Digital attendance tracking
- Leave request management
- Worker profile management
- Attendance reports

### Financial Management
- Expense logging and tracking
- Budget management
- Invoice generation
- Financial reporting

### Client Portal
- Project progress viewing
- Document access
- Communication with project managers
- Invoice and report downloads

##  Development

### Project Structure
```
src/
├── main/
│   ├── java/com/example/builtsmart/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # Web controllers
│   │   ├── entity/          # JPA entities
│   │   ├── factory/         # Factory pattern implementations
│   │   ├── repository/      # Data access layer
│   │   └── service/         # Business logic
│   └── resources/
│       ├── static/          # CSS, JS, images
│       ├── templates/       # Thymeleaf templates
│       └── application.properties
└── test/                    # Unit tests
```

### Design Patterns

The system implements several design patterns for maintainability and scalability:

#### Factory Method Pattern (User Creation)
- **Location**: `src/main/java/com/example/builtsmart/factory/`
- **Purpose**: Creates users with role-specific defaults (department, active status, etc.)
- **Components**:
  - `UserFactory` interface - Defines factory contract
  - Concrete factories - One for each user role (ProjectManagerFactory, WorkerFactory, etc.)
  - `UserFactoryProvider` - Registry that provides appropriate factory based on role
- **Usage**:
  ```java
  User user = userService.createUserWithFactory(
      User.UserRole.PROJECT_MANAGER,
      name, email, password, phone, address
  );
  ```
- **Documentation**: See `FACTORY_PATTERN_DOCUMENTATION.md` for details

#### Other Patterns
- **MVC (Model-View-Controller)**: Separates presentation, business logic, and data
- **Repository Pattern**: Abstracts data access via Spring Data JPA
- **Service Layer Pattern**: Encapsulates business logic
- **Dependency Injection**: Spring IoC container manages dependencies

### Adding New Features
1. Create entity classes in `entity/` package
2. Add repository interfaces in `repository/` package
3. Implement business logic in `service/` package
4. Create controllers in `controller/` package

##  Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

##  Deployment

### Production Deployment
1. Update `application.properties` for production database
2. Set appropriate security configurations
3. Configure logging levels
4. Build the application:
```bash
mvn clean package
```

### Docker Deployment (Optional)
```dockerfile
FROM openjdk:24-jdk-slim
COPY target/builtsmart-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

##  Security Considerations

- All passwords are encrypted using BCrypt
- Role-based access control implemented
- CSRF protection enabled
- Input validation on all forms
- SQL injection prevention through JPA

##  API Documentation

The system provides REST APIs for:
- User management
- Project operations
- Task management
- Attendance tracking
- Financial operations

API endpoints are secured and require appropriate authentication.

##  Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

##  License

This project is developed for BuildSmart Lanka Pvt Ltd as a university-level project.

##  Support

For technical support or questions:
- Check the documentation
- Review the code comments
- Contact the development team

##  Version History

- **v1.0.0** - Initial release with basic functionality
- Core user management
- Project and task management
- Attendance tracking
- Basic financial features

---

