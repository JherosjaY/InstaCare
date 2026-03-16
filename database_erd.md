# InstaCare Local Database ERD

This document provides a visual representation and detailed schema of the local Room database used in the InstaCare application.

## Entity-Relationship Diagram

```mermaid
erDiagram
    USERS {
        int uid PK
        string username
        string fullName
        string email
        string password
        string phone
        boolean isVerified
        boolean isSuspended
    }

    ACTIVITY_LOGS {
        int id PK
        string type
        string description
        string userEmail
        string category
        long timestamp
    }

    EMERGENCY_CONTACTS {
        int id PK
        string name
        string phoneNumber
        string relation
        string username
    }

    ENDORSEMENTS {
        int id PK
        string username
        string patientName
        string address
        string purpose
        string hospitalName
        string reason
        string status
        string adminRemarks
        long createdAt
        long updatedAt
    }

    GUIDES {
        string id PK
        string title
        string category
        string description
        string difficulty
        string duration
        int views
        string videoUrl
        string stepsJson
        boolean isBookmarked
    }

    HOSPITALS {
        string id PK
        string name
        string distance
        string address
        string phone
        boolean isOpen
        string type
        string servicesJson
        double latitude
        double longitude
        string imageUrl
        string capacityStatus
    }

    NOTIFICATIONS {
        int id PK
        string title
        string message
        long timestamp
        boolean isRead
    }

    USERS ||--o{ ACTIVITY_LOGS : "logs activity for"
    USERS ||--o{ EMERGENCY_CONTACTS : "has"
    USERS ||--o{ ENDORSEMENTS : "requests"
    HOSPITALS ||--o{ ENDORSEMENTS : "receives referral"
```

## Context on User Roles (Admin / Barangay / User)

Based on the actual code in `LoginActivity.java`, the application supports 3 roles, although they are currently handled as follows:

- **Admin**: Built-in hardcoded account (`username: admin`). Has access to `AdminDashboardActivity`.
- **Barangay**: Built-in hardcoded account (`username: barangay`). Has access to `BarangayDashboardActivity`.
- **User**: Dynamically stored in the `users` table. These are the accounts created via `RegisterActivity`.

> [!NOTE]
> Currently, the `users` table does not have a `role` column because the Admin and Barangay accounts are "built-in". If you plan to allow registration for multiple roles in the future, we can add a `role` column to the `User.java` entity.

## Detailed Table Descriptions

### 1. USERS (`users`)
Stores user account information including credentials and status.

### 2. ACTIVITY_LOGS (`activity_logs`)
Tracks user actions such as logins, profile updates, and SOS alerts. Linked to users via `userEmail`.

### 3. EMERGENCY_CONTACTS (`emergency_contacts`)
Stores per-user emergency contacts. Linked to users via `username`.

### 4. ENDORSEMENTS (`endorsements`)
Referrals or medical assistance requests created by users for specific hospitals.
- **Relationships**: Linked to `USERS` via `username` and `HOSPITALS` via `hospitalName`.

### 5. GUIDES (`guides`)
First aid instruction data (Bleeding, Cardiac, Burns, etc.). Seeded from `AppDatabase`.

### 6. HOSPITALS (`hospitals`)
Information about local hospitals and clinics, including capacity status and location. Seeded from `AppDatabase`.

### 7. NOTIFICATIONS (`notifications`)
Stores alerts and system notifications for the local user.
