# 🎮 Games Spring - Game Store API

A comprehensive Spring Boot 3 application demonstrating enterprise-level patterns and best practices for building a game store platform with e-commerce functionality.

## 🎯 About
This is a self-initiated project aimed at exploring and applying key patterns and best practices provided by Spring Boot 3. The application simulates a game store where users can browse games, manage their cart, purchase games using a wallet system, and track their purchase history. Developers can manage their games, and admins have oversight over the entire system.

### User Features
- 🔐 User authentication with JWT tokens (access & refresh tokens)
- 🛒 Shopping cart management (add, remove, view, checkout, clear)
- 💰 Wallet system (deposit, withdraw, balance management)
- 🎮 Browse and search games by title, author, price range, and category
- 📜 Purchase history tracking
- 💳 Transaction history for all balance operations

### Developer Features
- ➕ Create and manage games
- 📊 View sales statistics and revenue
- 🏷️ Manage game categories
- 📈 Track game modification history
- 🔍 Audit trail for all game changes

### Admin Features
- 👥 View user purchase histories
- 📊 Access game purchase statistics
- 🔍 Monitor developer activities
- 💼 User balance management
- 📋 Complete system oversight

## 🛠️ Tech Stack

### Core Framework
- **Java 17+**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Spring Security**
- **PostgreSQL**

### Security
- **JWT (JSON Web Tokens)** - Authentication & Authorization
- **BCrypt** - Password encryption
- **Role-based Access Control** - USER, DEVELOPER, ADMIN roles

### Data & Mapping
- **MapStruct** - DTO-Entity mapping
- **Hibernate** - ORM
- **Jakarta Validation** - Input validation

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking dependencies
- **AssertJ** - Fluent assertions
- **@WebMvcTest** - Controller layer testing
- **@DataJpaTest** - Repository layer testing

### Build Tool
- **Maven** - Dependency management

### Key Design Patterns
- **Repository Pattern** - Data access abstraction
- **DTO Pattern** - Separation of internal and external models
- **Builder Pattern** - Object construction
- **Dependency Injection** - Loose coupling
- **AOP (Aspect-Oriented Programming)** - Cross-cutting concerns

## 📡 API Endpoints

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | User login |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | User logout |

### Games (`/api/v1/games`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Get all games | Public |
| GET | `/{id}` | Get game by ID | Public |
| GET | `/title/{title}` | Get game by title | Public |
| GET | `/search/title?title={title}` | Search by title | Public |
| GET | `/search/author?author={author}` | Search by author | Public |
| GET | `/filter/price?min={min}&max={max}` | Filter by price | Public |
| GET | `/sorted?ascending={true/false}` | Sort by price | Public |
| GET | `/category/{categoryId}` | Get by category | Public |
| POST | `/` | Create game | Developer |
| PUT | `/{id}` | Update game | Developer |
| DELETE | `/{id}` | Delete game | Developer |

### Categories (`/api/v1/categories`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/` | Get all categories | Public |
| GET | `/{id}` | Get category by ID | Public |
| POST | `/` | Create category | Developer |
| PUT | `/{id}` | Update category | Developer |
| DELETE | `/{id}` | Delete category | Developer |

### Cart (`/api/v1/cart`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/add` | Add game to cart | User |
| GET | `/` | View cart | User |
| DELETE | `/remove/{gameId}` | Remove from cart | User |
| POST | `/checkout` | Checkout cart | User |
| DELETE | `/clear` | Clear cart | User |
| GET | `/can-checkout` | Validate cart | User |

### Purchase (`/api/v1/purchase`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/game/{gameId}` | Purchase single game | User |
| POST | `/games` | Purchase multiple games | User |
| GET | `/history` | Get purchase history | User |
| GET | `/history/paged` | Paginated history | User |
| GET | `/admin/user/{userId}/history` | User's purchases | Admin |
| GET | `/admin/game/{gameId}/purchases` | Game purchases | Admin |
| GET | `/developer/sales` | Developer sales | Developer |
| GET | `/developer/revenue` | Total revenue | Developer |

### Balance (`/api/v1/balance`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/my` | Get my balance | User |
| POST | `/create` | Create balance | User |
| DELETE | `/delete` | Delete balance | User |
| POST | `/deposit` | Deposit funds | User |
| POST | `/withdraw` | Withdraw funds | User |
| GET | `/user/{userId}` | Get user balance | Admin |

### Transactions (`/api/v1/balance/transactions`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/me` | My transactions | User |
| GET | `/user/{userId}` | User transactions | Admin |

### Game History (`/api/v1/history`)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/game/{gameId}` | Game history | Admin |
| GET | `/developer/{developerId}` | Developer activity | Admin |
| GET | `/developer/{developerId}/history` | Developer history | Admin |
| GET | `/my/game/{gameId}` | My game history | Developer |
| GET | `/my/activity` | My activity | Developer |
| GET | `/my/history` | My history | Developer |

### Authentication Flow
1. User registers/logs in with credentials
2. Server validates and returns JWT access token + refresh token
3. Client includes access token in `Authorization: Bearer <token>` header
4. Server validates token on each protected endpoint
5. When access token expires, client uses refresh token to get new tokens

### Token Management
- Access tokens expire after 24 hours
- Refresh tokens expire after 7 days
- Logged out tokens are blacklisted

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/example/Games/
│   │   ├── cart/                    # Shopping cart module
│   │   ├── category/                # Category management
│   │   ├── config/                  # Configuration classes
│   │   │   ├── common/             # Common DTOs and mappers
│   │   │   ├── exception/          # Exception handling
│   │   │   ├── logging/            # Request logging
│   │   │   └── security/           # Security configuration
│   │   ├── game/                    # Game management
│   │   ├── gameHistory/             # Audit trail
│   │   ├── purchase/                # Purchase system
│   │   └── user/                    # User management
│   │       ├── auth/               # Authentication
│   │       ├── balance/            # Wallet system
│   │       └── role/               # Role management
│   └── resources/
│       └── application.properties   # Configuration
└── test/                           # Test files mirror main structure
```

### Standardized Error Response
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2025-10-01T12:00:00"
}
```

## 📊 Database Schema

### Key Entities
- **User** - User accounts and authentication
- **Role** - User roles (USER, DEVELOPER, ADMIN)
- **Game** - Game catalog
- **Category** - Game categories
- **CartItem** - Shopping cart items
- **PurchaseHistory** - Purchase records
- **Balance** - User wallet
- **BalanceTransaction** - Transaction history
- **GameHistory** - Audit trail for game changes

### Relationships
- User ↔ Role (Many-to-Many)
- User ↔ CartItem (One-to-Many)
- User ↔ PurchaseHistory (One-to-Many)
- User ↔ Balance (One-to-One)
- User ↔ Game (One-to-Many) - Developer relationship
- Game ↔ Category (Many-to-One)
- Balance ↔ BalanceTransaction (One-to-Many)
- Game ↔ GameHistory (One-to-Many)

## 👨‍💻 Author
**Kalkazbek Alikhan**

**Project Status:** 🚧 Active Development
Last Updated: October 2025
