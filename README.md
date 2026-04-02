# Group A — Exam Module API

Base URL: `http://localhost:8080/api/exams`

All endpoints require a valid JWT token:
```
Authorization: Bearer <token>
```

---

## Exam status lifecycle

```
DRAFT → PUBLISHED → CLOSED
  ↑________↓
 (unpublish)
```

---

## Endpoints

### Create an exam
```
POST /api/exams
```

**Request body:**
```json
{
  "courseId": "uuid-of-course",
  "title": "Database CAT 1",
  "description": "Optional description",
  "visibilityMode": "IMMEDIATE",
  "timeLimitMins": 60,
  "maxAttempts": 1,
  "seedSql": "CREATE TABLE students (id INT PRIMARY KEY, name VARCHAR(100), grade INT); INSERT INTO students VALUES (1, 'Alice', 85);"
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `courseId` | string | yes | UUID from Group F |
| `title` | string | yes | |
| `description` | string | no | |
| `visibilityMode` | string | yes | `IMMEDIATE`, `END_OF_EXAM`, `NEVER` |
| `timeLimitMins` | integer | no | `null` = no time limit |
| `maxAttempts` | integer | no | defaults to `1` |
| `seedSql` | string | yes | SQL for sandbox seeding — Group D uses this |

**Response `200 OK`:**
```json
{
  "id": "36b28504-1902-4ac4-89a1-1271f0ead90e",
  "courseId": "uuid-of-course",
  "title": "Database CAT 1",
  "description": "Optional description",
  "status": "DRAFT",
  "visibilityMode": "IMMEDIATE",
  "timeLimitMins": 60,
  "maxAttempts": 1,
  "seedSql": "CREATE TABLE...",
  "createdAt": "2026-04-01T11:16:10.448",
  "publishedAt": null
}
```

---

### Get exam by ID
```
GET /api/exams/{examId}
```

**Response `200 OK`:** full exam object

---

### Get exams by course
```
GET /api/exams/course/{courseId}
```

**Response `200 OK`:** array of exam objects for that course

---

### Get all published exams
```
GET /api/exams/published
```

**Response `200 OK`:** array of exams where `status = PUBLISHED`

---

### Update an exam
```
PUT /api/exams/{examId}
```

> Only allowed when `status = DRAFT`

**Request body** (all fields optional):
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "visibilityMode": "END_OF_EXAM",
  "timeLimitMins": 90,
  "maxAttempts": 2,
  "seedSql": "CREATE TABLE..."
}
```

**Response `200 OK`:** updated exam object

**Error if not DRAFT:**
```json
{ "message": "Only DRAFT exams can be edited" }
```

---

### Publish an exam
```
PATCH /api/exams/{examId}/publish
```

Validation before publish:
- Status must be `DRAFT`
- `seedSql` must not be empty
- `visibilityMode` must be set

**Response `200 OK`:**
```json
{
  "status": "PUBLISHED",
  "publishedAt": "2026-04-01T11:20:00.000",
  ...
}
```

**Error if validation fails:**
```json
{ "message": "Only DRAFT exams can be published" }
```

---

### Unpublish an exam
```
PATCH /api/exams/{examId}/unpublish
```

> Moves `PUBLISHED` back to `DRAFT`

**Response `200 OK`:**
```json
{
  "status": "DRAFT",
  "publishedAt": null,
  ...
}
```

**Error if not PUBLISHED:**
```json
{ "message": "Only PUBLISHED exams can be unpublished" }
```

---

### Close an exam
```
PATCH /api/exams/{examId}/close
```

> Moves `PUBLISHED` to `CLOSED`. Cannot be reversed.

**Response `200 OK`:**
```json
{
  "status": "CLOSED",
  ...
}
```

**Error if not PUBLISHED:**
```json
{ "message": "Only PUBLISHED exams can be closed" }
```

---

### Delete an exam
```
DELETE /api/exams/{examId}
```

> Only `DRAFT` exams can be deleted

**Response `204 No Content`**

**Error if not DRAFT:**
```json
{ "message": "Only DRAFT exams can be deleted" }
```

---

## Visibility mode reference

| Value | Meaning |
|---|---|
| `IMMEDIATE` | Student sees results right after submitting |
| `END_OF_EXAM` | Student sees results only after exam is closed |
| `NEVER` | Results never shown to student |

---

## Error reference

| Status | Meaning |
|---|---|
| `200 OK` | Success |
| `204 No Content` | Delete succeeded |
| `401 Unauthorized` | Missing or invalid JWT token |
| `500` | Business rule violation — check `message` field |

---

# Group D — Sandbox Environment Module

Welcome to the Sandbox Environment Module documentation for the QueryMe backend project. This module, developed by Group D, provides a secure and isolated database environment for students to execute SQL queries during exams. As a fellow developer in our university class project, this guide will help you understand and integrate with our internal API.

## Overview / Architecture

The Sandbox Environment Module is a core component of the QueryMe monolithic Spring Boot application, designed to dynamically provision, secure, and manage isolated PostgreSQL schemas for individual student exam sessions. Its primary purpose is to ensure data isolation and security by preventing students from accessing or modifying the main application data stored in the public schema.

### Security Model
- **Isolation**: Each student gets a unique schema (e.g., `exam_123_student_456`) and a dedicated database user with randomly generated credentials.
- **Blast Radius Containment**: Students are explicitly revoked access to the public schema and granted full CRUD permissions only on their assigned schema.
- **Automated Cleanup**: Expired sandboxes are automatically torn down to free up server resources and maintain a clean database state.

### Key Processes
1. **Provisioning**: Upon exam start, a new schema and user are created using `JdbcTemplate` for direct SQL execution.
2. **Security Enforcement**: SQL commands revoke public schema access and grant schema-specific privileges.
3. **Registry Tracking**: Active sandboxes are tracked via a JPA entity for monitoring and cleanup.
4. **Teardown**: Manual or scheduled removal of schemas and users when exams conclude or expire.

## Key Components

The module consists of the following main classes:

- **`SandboxService`**: The primary interface for interacting with the sandbox functionality. It defines methods for provisioning, retrieving connection details, and tearing down sandboxes.
- **`SandboxRegistry`**: A JPA entity mapped to the `sandbox_registry` table, used to persist metadata about active sandboxes, including schema names, user details, expiration times, and status.
- **`SandboxCleanupScheduler`**: A Spring `@Scheduled` component that runs every 5 minutes to identify and clean up expired sandboxes, ensuring efficient resource management.

## Configuration Requirements

To enable the Sandbox Environment Module, ensure the following configurations are in place:

- **JPA DDL Auto-Update**: Set `spring.jpa.hibernate.ddl-auto=update` in your `application.yml` to allow Hibernate to create and update the `sandbox_registry` table automatically.
- **Scheduling Enablement**: Add `@EnableScheduling` to your main Spring Boot application class (e.g., `QueryMeBackendApplication`) to activate the background cleanup scheduler.
- **Database User Privileges**: The main database user (configured via `DB_USER`) must have `CREATEROLE` privileges in PostgreSQL to create and manage temporary database users for sandboxes.

Example application.yml snippet:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Example main application class:
```java
@SpringBootApplication
@EnableScheduling
public class QueryMeBackendApplication {
    // ...
}
```

## Integration Guide for Group G

As Group G (Query Engine), you can integrate with the Sandbox Environment Module by autowiring the `SandboxService` interface into your Spring `@Service` classes. This allows you to provision isolated database environments for query execution and securely manage their lifecycles.

Below is a sample integration example:

```java
package com.year2.queryme.queryengine; // Replace with your actual package

import com.year2.queryme.sandbox.dto.SandboxConnectionInfo;
import com.year2.queryme.sandbox.service.SandboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QueryEngineService {

    @Autowired
    private SandboxService sandboxService;

    public void executeStudentQuery(UUID examId, UUID studentId, String seedSql, String studentQuery) {
        // Step 1: Provision a new sandbox for the student
        String schemaName = sandboxService.provisionSandbox(examId, studentId, seedSql);
        System.out.println("Provisioned sandbox with schema: " + schemaName);

        // Step 2: Retrieve connection details for database access
        SandboxConnectionInfo connectionInfo = sandboxService.getSandboxConnectionDetails(examId, studentId);
        String dbUsername = connectionInfo.dbUsername();
        String dbPassword = connectionInfo.dbPassword(); // Assuming password is included
        String schema = connectionInfo.schemaName();

        // Step 3: Use the connection details to execute the student's query
        // (Implement your query execution logic here using JdbcTemplate or a DataSource)
        // For example:
        // DataSource dataSource = createDataSource(dbUsername, dbPassword, schema);
        // executeQuery(dataSource, studentQuery);

        // Step 4: After query execution, teardown the sandbox
        sandboxService.teardownSandbox(examId, studentId);
        System.out.println("Teardown completed for exam: " + examId + ", student: " + studentId);
    }

    // Additional helper methods for DataSource creation and query execution...
}
```

### Method Details
- **`provisionSandbox(UUID examId, UUID studentId, String seedSql)`**: Provisions a new sandbox and returns the schema name. The `seedSql` parameter allows initializing the schema with exam-specific data.
- **`getSandboxConnectionDetails(UUID examId, UUID studentId)`**: Retrieves connection information (schema name, username, password) for an active sandbox.
- **`teardownSandbox(UUID examId, UUID studentId)`**: Permanently removes the sandbox, dropping the schema and associated user.

Ensure that your service handles exceptions appropriately, as provisioning or teardown operations may fail due to database constraints or permissions.

If you have any questions or need further assistance, feel free to reach out to Group D!

