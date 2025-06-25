# Blog Application - JWT Authentication with RBAC

This Spring Boot 3.5.3 application implements JWT-based authentication using RSA PEM keys and role-based access control (RBAC) for GraphQL endpoints.

## Features

- JWT Authentication using RSA PEM keys
- Role-based access control (USER, MODERATOR, ADMIN)
- GraphQL API with secure endpoints
- Token extraction from Authorization header
- Clean architecture implementation

## Authentication Setup

### JWT Configuration
- Access tokens: 1 hour expiration
- Refresh tokens: 7 days expiration
- HMAC SHA256 signing using secret keys

## GraphQL Endpoints

### Public Endpoints
- `hello` - No authentication required
- `posts` - View published posts
- `post(slug)` - View individual post

### Authenticated Endpoints (Requires Bearer token)
- `simple` - Requires USER role or higher
- `myPosts` - User's own posts
- `createPost` - Create new post
- `publishPost` - Publish own post
- `deletePost` - Delete own post (or any post for MODERATOR/ADMIN)

### Admin/Moderator Only Endpoints
- `getUser(id)` - Requires MODERATOR or ADMIN
- `archivePost` - Requires MODERATOR or ADMIN

## Usage Examples

### 1. Register New User (Start Here)
```graphql
mutation {
  register(input: {
    username: "admin"
    email: "admin@blog.com"
    password: "admin123"
    displayName: "Administrator"
  }) {
    accessToken
    refreshToken
    user {
      id
      username
      email
      displayName
      role
    }
  }
}
```

### 2. Login
```graphql
mutation {
  login(input: {
    usernameOrEmail: "admin@blog.com"
    password: "admin123"
  }) {
    accessToken
    refreshToken
    user {
      id
      username
      email
      displayName
      role
    }
  }
}
```

### 3. Register New User
```graphql
mutation {
  register(input: {
    username: "newuser"
    email: "newuser@example.com"
    password: "password123"
    displayName: "New User"
  }) {
    accessToken
    refreshToken
    user {
      id
      username
      email
      displayName
      role
    }
  }
}
```

### 4. Create Post (Authenticated)
```graphql
# Headers: { "Authorization": "Bearer YOUR_ACCESS_TOKEN" }
mutation {
  createPost(input: {
    title: "My First Post"
    content: "This is the content of my first post..."
    excerpt: "A short excerpt"
  }) {
    id
    title
    slug
    status
    author {
      username
      displayName
    }
  }
}
```

### 5. Get My Posts (Authenticated)
```graphql
# Headers: { "Authorization": "Bearer YOUR_ACCESS_TOKEN" }
query {
  myPosts(page: 0, size: 10) {
    id
    title
    slug
    status
    createdAt
  }
}
```

### 6. Archive Post (MODERATOR/ADMIN only)
```graphql
# Headers: { "Authorization": "Bearer MODERATOR_OR_ADMIN_TOKEN" }
mutation {
  archivePost(id: "post-uuid") {
    id
    title
    status
  }
}
```

## Authorization Header Format
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Role-Based Access Control

### USER Role
- Can create, view, edit, and delete their own posts
- Can view published posts from others
- Can access basic authenticated endpoints

### MODERATOR Role
- All USER permissions
- Can delete any post
- Can archive any post
- Can view user details

### ADMIN Role
- All MODERATOR permissions
- Full system access
- Can manage users and system settings

## Testing Different Roles

Since all users are created with USER role by default, to test MODERATOR and ADMIN roles:

1. **Create a user** via the `register` mutation
2. **Manually update the user's role** in the database:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE username = 'your-username';
   -- or
   UPDATE users SET role = 'MODERATOR' WHERE username = 'your-username';
   ```
3. **Login again** to get a new token with the updated role

Alternatively, you can modify the `AuthServiceImpl.register()` method temporarily to assign different roles during registration.

## Development

### Database Setup
1. Start PostgreSQL using Docker Compose: `docker-compose up -d`
2. The application will auto-create tables using the DDL script
3. Create users by registering through GraphQL mutations

### Running the Application
```bash
./mvnw spring-boot:run
```

### GraphQL Playground
Visit `http://localhost:8080/graphiql` to access the GraphQL playground.

## Security Features

- JWT tokens are signed with HMAC SHA256 using secret keys
- Tokens are stateless and contain user claims
- Authorization is handled through Spring Security @PreAuthorize annotations
- Automatic token validation on each request
- Role-based method security

## Error Handling

The application includes proper GraphQL error handling for:
- Authentication errors
- Authorization errors
- Validation errors
- Business logic errors
