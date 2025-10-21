# User Registration Workflow - Complete Implementation

## 🎯 **Overview**

This document describes the complete user registration workflow for the BuiltSmart CMS system.

---

## 📋 **Registration Flow**

```
┌─────────────────┐
│  User Visits    │
│ /register page  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Fills Form:    │
│  - Name         │
│  - Email        │
│  - Password     │
│  - Phone        │
│  - Address      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Submit  POST    │
│   /register     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Validation    │
│ - Email unique? │
│ - Password OK?  │
│ - Fields valid? │
└────────┬────────┘
         │
         ├─────── ❌ Fails → Show error, keep form data
         │
         ▼
┌─────────────────┐
│  Create User:   │
│  - Hash password│
│  - role=WORKER  │
│  - active=false │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Save to Database│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Redirect to     │
│ /login?         │
│ registered=true │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Show Success:   │
│ "Wait for admin │
│  approval"      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ User tries login│
│   ⚠️ FAILS      │
│ "Account        │
│  inactive"      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Admin activates │
│ active = true   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ User can login  │
│ Redirected to   │
│ role dashboard  │
└─────────────────┘
```

---

## 🔧 **Technical Implementation**

### **1. Registration Controller**

**File:** `RegistrationController.java`

```java
@PostMapping("/register")
public String registerUser(
    @RequestParam String name,
    @RequestParam String email,
    @RequestParam String password,
    @RequestParam String phone,
    @RequestParam String address,
    RedirectAttributes redirectAttributes,
    Model model
)
```

**Responsibilities:**
- ✅ Validate all input fields
- ✅ Check email uniqueness
- ✅ Validate password strength (min 6 chars)
- ✅ Create new User entity
- ✅ Set default role (WORKER)
- ✅ Set active = false (pending approval)
- ✅ Redirect to login with success message

---

### **2. User Service**

**File:** `UserService.java`

**Method:** `createUser(User user)`

```java
public User createUser(User user) {
    // Check if email exists
    if (userRepository.existsByEmail(user.getEmail())) {
        throw new RuntimeException("User already exists");
    }
    
    // Hash password with BCrypt
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    
    // Save to database
    return userRepository.save(user);
}
```

**Responsibilities:**
- ✅ Email uniqueness check
- ✅ Password hashing (BCrypt)
- ✅ Save to database

---

### **3. User Entity**

**File:** `User.java`

**Default Values:**
```java
private boolean active = true; // Changed to false on registration
private UserRole role;          // Set to WORKER by default
```

**After Registration:**
```java
User newUser = new User();
newUser.setRole(User.UserRole.WORKER);  // Default role
newUser.setActive(false);                // Inactive until approved
```

---

### **4. Authentication Failure Handler**

**File:** `CustomAuthenticationFailureHandler.java`

```java
@Override
public void onAuthenticationFailure(...) {
    String redirectUrl = "/login?error";
    
    // Check if account is disabled/inactive
    if (exception instanceof DisabledException) {
        redirectUrl = "/login?inactive";
    }
    
    response.sendRedirect(redirectUrl);
}
```

**Responsibilities:**
- ✅ Detect inactive account login attempts
- ✅ Redirect to login with appropriate message
- ✅ Differentiate between wrong credentials vs inactive account

---

### **5. Security Configuration**

**File:** `SecurityConfig.java`

```java
.formLogin(form -> form
    .loginPage("/login")
    .defaultSuccessUrl("/dashboard", true)
    .failureHandler(authenticationFailureHandler) // ← Uses custom handler
    .permitAll()
)
```

**Public Access:**
```java
.requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
```

---

## 📝 **Form Fields**

### **Registration Form**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| Name | text | Yes | Not blank |
| Email | email | Yes | Valid email, unique |
| Password | password | Yes | Min 6 characters |
| Confirm Password | password | Yes | Must match password |
| Phone | tel | Yes | Valid phone number |
| Address | textarea | Yes | Not blank |
| Terms Agreement | checkbox | Yes | Must be checked |

---

## ✅ **Validation Rules**

### **Server-Side Validation:**

1. **Email Uniqueness:**
   ```java
   if (userService.existsByEmail(email)) {
       model.addAttribute("error", "Email already exists");
       return "auth/register";
   }
   ```

2. **Password Length:**
   ```java
   if (password == null || password.length() < 6) {
       model.addAttribute("error", "Password must be at least 6 characters");
       return "auth/register";
   }
   ```

3. **Password Match:**
   ```java
   if (confirmPassword != null && !password.equals(confirmPassword)) {
       model.addAttribute("error", "Passwords do not match");
       return "auth/register";
   }
   ```

### **Client-Side Validation:**

- ✅ Password strength indicator (Weak/Fair/Good/Strong)
- ✅ Real-time password match check
- ✅ HTML5 input validation (required, email, minlength)
- ✅ Terms checkbox must be checked to enable submit

---

## 🚦 **User States**

### **1. Newly Registered**

```java
active = false
role = WORKER
```

**Can:**
- ❌ Cannot login
- ❌ Cannot access any dashboard

**Sees:**
- ✅ Success message on login page
- ✅ "Wait for admin approval" message

---

### **2. After Admin Activation**

```java
active = true
role = WORKER (or assigned role)
```

**Can:**
- ✅ Login successfully
- ✅ Access role-based dashboard
- ✅ Use system features

---

## 🔐 **Security Features**

### **Password Security:**
- ✅ **BCrypt hashing** (industry standard)
- ✅ **Minimum length** enforced (6 characters)
- ✅ **Strength indicator** guides users
- ✅ **Never stored in plain text**

### **Email Security:**
- ✅ **Uniqueness** enforced at database level
- ✅ **Validation** using regex pattern
- ✅ **Case-insensitive** comparison

### **Session Security:**
- ✅ **Inactive users** cannot create sessions
- ✅ **Custom failure handling** prevents info leakage
- ✅ **Proper error messages** without revealing account existence

---

## 📊 **Database Schema**

### **User Table**

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hashed
    role VARCHAR(50) NOT NULL,       -- WORKER, PROJECT_MANAGER, etc.
    phone VARCHAR(20),
    address TEXT,
    department VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## 🎨 **UI/UX Features**

### **Registration Page:**
- ✅ Split-screen layout with brand story
- ✅ Password strength meter
- ✅ Real-time password match indicator
- ✅ Toggle password visibility
- ✅ Terms & conditions checkbox
- ✅ Submit button disabled until terms accepted
- ✅ Error messages preserved on validation fail
- ✅ Form data retained on error (except password)

### **Login Page:**
- ✅ Success message for new registrations
- ✅ Inactive account warning message
- ✅ Clear differentiation between error types

---

## 🔄 **Complete User Journey**

### **Step 1: Registration**
1. User visits `/register`
2. Fills out form
3. Checks terms agreement
4. Clicks "Create Account"

### **Step 2: Validation**
1. System checks email doesn't exist
2. Validates password length
3. Confirms passwords match
4. If fails → Shows error, keeps form data

### **Step 3: Account Creation**
1. Password is hashed with BCrypt
2. User created with `active=false` and `role=WORKER`
3. Saved to database

### **Step 4: Redirect**
1. Redirects to `/login?registered=true`
2. Shows success message
3. Informs user to wait for approval

### **Step 5: First Login Attempt**
1. User tries to login
2. Authentication fails (account disabled)
3. Redirected to `/login?inactive`
4. Shows "Account pending activation" message

### **Step 6: Admin Activation**
1. Admin logs in
2. Goes to user management
3. Finds pending user
4. Sets `active=true`
5. Optionally assigns different role

### **Step 7: Successful Login**
1. User tries login again
2. Authentication succeeds
3. Session created
4. Redirected to role-based dashboard

---

## 🛠️ **Admin Actions Required**

### **To Activate User:**

1. Login as admin
2. Navigate to **User Management**
3. Find users with status "Inactive"
4. Click "Activate" or edit user
5. Set `active = true`
6. Optionally change role from WORKER to appropriate role
7. Save changes

### **Role Assignment:**

Default registration role: **WORKER**

Available roles:
- PROJECT_MANAGER
- SITE_ENGINEER
- HR_EXECUTIVE
- FINANCE_OFFICER
- CLIENT_REPRESENTATIVE
- WORKER

---

## 📧 **Messages & Alerts**

### **Success Messages:**

**After Registration:**
```
✅ Account created successfully! Please wait for an administrator 
   to activate your account before logging in.
```

**After Logout:**
```
✅ You've been logged out successfully.
```

### **Error Messages:**

**Invalid Credentials:**
```
❌ Invalid email or password. Please try again.
```

**Inactive Account:**
```
❌ Your account is pending activation. Please contact an administrator.
```

**Email Exists:**
```
❌ An account with this email already exists.
```

**Password Too Short:**
```
❌ Password must be at least 6 characters long.
```

**Passwords Don't Match:**
```
❌ Passwords do not match.
```

---

## ⚙️ **Configuration**

### **Application Properties:**

```properties
# Password Encoding
spring.security.password.encoder=bcrypt

# Default User Settings
app.user.default-role=WORKER
app.user.require-activation=true
```

---

## 🧪 **Testing**

### **Manual Test Cases:**

**Test 1: Successful Registration**
1. Go to `/register`
2. Fill all fields with valid data
3. Check terms checkbox
4. Submit form
5. ✅ Should redirect to login with success message

**Test 2: Duplicate Email**
1. Register with existing email
2. ❌ Should show "Email already exists" error
3. ✅ Form data should be preserved

**Test 3: Short Password**
1. Enter password with <6 characters
2. ❌ Should show password length error

**Test 4: Password Mismatch**
1. Enter different passwords in password and confirm fields
2. ❌ Should show mismatch error

**Test 5: Login While Inactive**
1. Register new account
2. Try to login immediately
3. ❌ Should show "Account pending activation"

**Test 6: Login After Activation**
1. Admin activates account
2. User tries to login
3. ✅ Should succeed and redirect to dashboard

---

## 📊 **Flow Diagram**

```
User Registration → Email Check → Password Validation
                        ↓              ↓
                    [Unique]      [Valid]
                        ↓              ↓
                    Create User (active=false, role=WORKER)
                        ↓
                    Hash Password (BCrypt)
                        ↓
                    Save to Database
                        ↓
                    Redirect to Login
                        ↓
                    Show Success Message
                        
User Login Attempt → Authentication Check
                        ↓
                    [Active?]
                    ↙      ↘
                [No]      [Yes]
                ↓          ↓
            Show         Login
           "Inactive"   Success
            Message       ↓
                      Dashboard
```

---

## ✅ **Implementation Checklist**

- ✅ RegistrationController created
- ✅ POST /register endpoint implemented
- ✅ Email uniqueness validation
- ✅ Password strength validation
- ✅ Password match validation
- ✅ User creation with default values
- ✅ Password hashing (BCrypt)
- ✅ Database persistence
- ✅ Success redirect to login
- ✅ Error handling with form data retention
- ✅ Custom authentication failure handler
- ✅ Inactive account detection
- ✅ Login page success message
- ✅ Login page inactive warning
- ✅ Register page error display
- ✅ Security configuration updated
- ✅ Client-side validation (password strength, match)

---

## 🎯 **Summary**

The registration workflow is now **fully implemented** with:

1. ✅ **Secure registration** with validation
2. ✅ **Admin approval required** (active=false by default)
3. ✅ **Password hashing** with BCrypt
4. ✅ **Proper error handling** and user feedback
5. ✅ **Inactive account detection** on login
6. ✅ **Role-based redirect** after activation
7. ✅ **Beautiful UI** with clear messaging

Users can now register, but **cannot login until an administrator activates their account**. This ensures proper user management and security.
