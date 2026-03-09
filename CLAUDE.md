# Whydah-UserIdentityBackend

## Purpose
The identity storage backend for Whydah. Stores user identities and their relationships to roles, applications, and organizations. Provides the persistence layer that SecurityTokenService and UserAdminService depend on for identity data.

## Tech Stack
- Language: Java 11+
- Framework: Jersey 2.x, Jetty 9.x, Spring 5.x
- Build: Maven
- Key dependencies: Whydah-Admin-SDK, Lucene (search), Jersey, Spring

## Architecture
Standalone microservice that manages the persistent storage of user identities. Uses Lucene for user search capabilities. Exposes REST APIs for identity CRUD operations. Optionally requires SecurityTokenService for authorization of incoming requests. The foundational data store that underpins the entire Whydah identity system.

## Key Entry Points
- REST API for identity operations
- `/health` - Health check
- `start_service.sh` - Service startup
- Lucene-based user search index

## Development
```bash
# Build
mvn clean install

# Run
java -jar target/UserIdentityBackend-*.jar
```

## Domain Context
Whydah IAM identity persistence. The authoritative store for user identities, credentials, and access relationships. All authentication and user management operations ultimately read from or write to this backend.
