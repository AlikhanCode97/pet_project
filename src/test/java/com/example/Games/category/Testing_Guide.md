# Category Package Testing Guide

## Overview

This guide explains the comprehensive test suite we've created for the Category package in your Games Spring Boot application. We've covered all major testing scenarios using JUnit 5, Mockito, and Spring Boot Test.

## Test Structure

### 1. CategoryTest.java - Entity Unit Tests
**Location**: `src/test/java/com/example/Games/category/CategoryTest.java`
**Purpose**: Tests the Category entity's business logic and methods

**What it tests**:
- Category creation with different constructors
- `updateName()` method with validation
- `hasGames()` method logic
- `getGameCount()` method
- `isNameEqual()` case-insensitive comparison
- Equality and hashCode behavior

**Key Features**:
- Uses `@Nested` classes for organized test groups
- Tests edge cases like null values, empty strings, long names
- Validates business rules (name length, trimming whitespace)

### 2. CategoryRepositoryTest.java - Repository Integration Tests
**Location**: `src/test/java/com/example/Games/category/CategoryRepositoryTest.java`
**Purpose**: Tests database operations and JPA repository methods

**What it tests**:
- CRUD operations (Create, Read, Update, Delete)
- Custom query method `findByName()`
- Database constraints (unique name constraint)
- Edge cases (null values, non-existent IDs)

**Key Features**:
- Uses `@DataJpaTest` for focused repository testing
- `TestEntityManager` for precise database control
- H2 in-memory database for fast, isolated tests
- Tests actual SQL constraints and database behavior

### 3. CategoryServiceTest.java - Service Unit Tests with Mocks
**Location**: `src/test/java/com/example/Games/category/CategoryServiceTest.java`
**Purpose**: Tests business logic in isolation using mocked dependencies

**What it tests**:
- All service methods: create, read, update, delete
- Exception handling scenarios
- Business rule validation
- Interaction with repository and mapper

**Key Features**:
- Uses `@ExtendWith(MockitoExtension.class)` for mock support
- `@Mock` annotations for dependencies
- `@InjectMocks` for the service under test
- Verifies method calls and interactions with `verify()`
- Tests both happy path and error scenarios

### 4. CategoryControllerTest.java - REST API Integration Tests
**Location**: `src/test/java/com/example/Games/category/CategoryControllerTest.java`
**Purpose**: Tests REST endpoints, security, and HTTP behavior

**What it tests**:
- All HTTP endpoints (GET, POST, PUT, DELETE)
- Security authorization (`@PreAuthorize` annotations)
- Request/response validation
- Error handling and HTTP status codes
- CSRF protection
- Content type validation

**Key Features**:
- Uses `@WebMvcTest` for web layer testing
- `MockMvc` for simulating HTTP requests
- `@WithMockUser` for security testing
- Tests authentication and authorization
- Validates JSON serialization/deserialization

## Testing Tools Explained

### JUnit 5
```java
@Test                    // Marks a test method
@DisplayName("...")      // Descriptive test names
@BeforeEach             // Setup method run before each test
@Nested                 // Organize related tests in inner classes
```

### Mockito
```java
@Mock                   // Creates a mock object
@InjectMocks           // Injects mocks into the tested object
when().thenReturn()    // Define mock behavior
verify()               // Verify method calls
```

### AssertJ
```java
assertThat(result)
    .isEqualTo(expected)
    .isNotNull()
    .hasSize(2)
    .extracting(Category::getName)
    .containsExactlyInAnyOrder("Action", "RPG");
```

### Spring Boot Test Annotations
```java
@DataJpaTest           // Repository layer testing
@WebMvcTest           // Web layer testing
@MockBean             // Mock Spring beans
@WithMockUser         // Security testing
```

## Running the Tests

### IDE (IntelliJ IDEA / Eclipse)
1. Right-click on the test class
2. Select "Run Tests"
3. View results in the test runner panel

### Maven Command Line
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CategoryTest

# Run tests with coverage
mvn test jacoco:report
```

### Gradle Command Line
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests CategoryTest

# Run tests with coverage
./gradlew test jacocoTestReport
```

## Test Configuration

### application-test.properties
**Location**: `src/test/resources/application-test.properties`
**Purpose**: Test-specific configuration

**Key Settings**:
- H2 in-memory database for isolation
- Debug logging for troubleshooting
- Test-specific JWT settings
- Security configuration for testing

## Test Categories and Patterns

### 1. Unit Tests
- **Fast**: No external dependencies
- **Isolated**: Mock all dependencies
- **Focused**: Test single units of code
- **Examples**: CategoryTest, CategoryServiceTest

### 2. Integration Tests
- **Realistic**: Use real components
- **Database**: Test actual database operations
- **HTTP**: Test real HTTP requests
- **Examples**: CategoryRepositoryTest, CategoryControllerTest

### 3. Test Organization Patterns

#### Given-When-Then Structure
```java
@Test
void shouldCreateCategorySuccessfully() {
    // Given - Setup test data
    CategoryRequest request = new CategoryRequest("Action");
    
    // When - Execute the action
    CategoryResponse result = categoryService.createCategory(request);
    
    // Then - Verify the outcome
    assertThat(result.name()).isEqualTo("Action");
}
```

#### Nested Test Classes
```java
@Nested
@DisplayName("createCategory() Tests")
class CreateCategoryTests {
    // All tests related to creating categories
}
```

## Test Data Management

### Test Builders
```java
Category category = Category.builder()
    .id(1L)
    .name("Action")
    .games(new ArrayList<>())
    .build();
```

### Test Fixtures in @BeforeEach
```java
@BeforeEach
void setUp() {
    // Create common test data used across multiple tests
    categoryRequest = new CategoryRequest("Action");
    categoryResponse = new CategoryResponse(1L, "Action", 0, now, now);
}
```

## Common Testing Scenarios Covered

### Happy Path Tests
- Successful CRUD operations
- Valid input handling
- Expected business logic flow

### Error Handling Tests
- Invalid input validation
- Business rule violations
- Exception scenarios
- Database constraint violations

### Edge Cases
- Null values
- Empty collections
- Boundary conditions (max length strings)
- Concurrent access scenarios

### Security Tests
- Authentication requirements
- Authorization rules
- CSRF protection
- Input sanitization

## Best Practices Demonstrated

### 1. Descriptive Test Names
```java
@DisplayName("Should throw exception when category name exceeds 100 characters")
void shouldThrowExceptionWhenCategoryNameExceeds100Characters() {
    // Test implementation
}
```

### 2. Single Responsibility
Each test method tests exactly one behavior or scenario.

### 3. Isolation
Tests don't depend on each other and can run in any order.

### 4. Fast Execution
Tests use mocks and in-memory databases for speed.

### 5. Comprehensive Coverage
- All public methods tested
- All branches and conditions covered
- Error scenarios included

## Next Steps

### 1. Run the Tests
Execute all tests to ensure they pass in your environment.

### 2. Add Test Coverage Reporting
Consider adding JaCoCo for coverage metrics:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

### 3. Continuous Integration
Integrate tests into your CI/CD pipeline.

### 4. Expand to Other Packages
Apply similar testing patterns to other packages like Game, User, etc.

### 5. Performance Testing
Consider adding performance tests for critical operations.

## Troubleshooting Common Issues

### Test Dependencies
Ensure you have these dependencies in your `pom.xml` or `build.gradle`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### Database Issues
If repository tests fail, check:
- H2 dependency is available
- Test configuration is correct
- Entity relationships are properly mapped

### Security Issues
If controller tests fail:
- Verify security configuration
- Check `@WithMockUser` annotations
- Ensure CSRF tokens are included where needed

This comprehensive test suite provides a solid foundation for maintaining code quality and catching regressions early in your development process.
