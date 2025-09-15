# Contributing to Aries Askar Java Wrapper

Thank you for your interest in contributing to the Aries Askar Java wrapper! This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites

- Java 8 or higher
- Maven 3.6+ or Gradle 6.0+
- Rust toolchain (for building the native library)
- Git

### Building the Project

1. Clone the repository:
```bash
git clone https://github.com/openwallet-foundation/askar.git
cd askar/wrappers/java
```

2. Build the native library (if not already built):
```bash
cd ../../
cargo build --release
cd wrappers/java
```

3. Build the Java wrapper:
```bash
# Using Maven
mvn clean compile

# Using Gradle
./gradlew build
```

### Running Tests

Make sure the native library is available in your library path before running tests:

```bash
# Using Maven
mvn test

# Using Gradle
./gradlew test
```

## Code Style

### Java Conventions

- Follow standard Java naming conventions
- Use 4 spaces for indentation
- Maximum line length of 120 characters
- Include Javadoc comments for all public APIs
- Use meaningful variable and method names

### Example:
```java
/**
 * Retrieves an entry from the store.
 * @param category The entry category
 * @param name The entry name
 * @param forUpdate Whether to lock the entry for update
 * @return The entry or null if not found
 * @throws AskarException If the operation fails
 */
public Entry fetch(String category, String name, boolean forUpdate) throws AskarException {
    // Implementation
}
```

## Testing Guidelines

### Test Categories

1. **Unit Tests**: Test individual classes and methods in isolation
2. **Integration Tests**: Test interactions between components
3. **Native Integration Tests**: Test integration with the native library

### Test Structure

- Place tests in `src/test/java`
- Use JUnit 5 for testing framework
- Use descriptive test method names
- Include both positive and negative test cases
- Clean up resources (use try-with-resources)

### Example Test:
```java
@Test
void testStoreOperations() throws AskarException {
    try (Store store = Store.provision(tempStoreUri, "raw", null, null, false)) {
        try (Session session = store.session().open()) {
            session.insert("category", "name", "value".getBytes(), null, null);

            Entry entry = session.fetch("category", "name", false);
            assertNotNull(entry);
            assertEquals("value", entry.getValueString());
        }
    }
}
```

## Documentation

### Javadoc Requirements

- All public classes, methods, and fields must have Javadoc
- Include `@param`, `@return`, and `@throws` tags as appropriate
- Provide usage examples for complex APIs
- Link to related classes using `{@link}`

### README Updates

- Update README.md for any new features
- Include code examples for new functionality
- Update version information when applicable

## Submitting Changes

### Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Update documentation
7. Commit with descriptive messages
8. Push to your fork
9. Create a pull request

### Commit Messages

Use clear and descriptive commit messages:

```
Add support for key exchange operations

- Implement ECDH key exchange in Key class
- Add key exchange tests
- Update documentation with examples
```

### Pull Request Description

Include in your PR description:
- What changes were made
- Why the changes were necessary
- How to test the changes
- Any breaking changes
- Related issues (if any)

## Issue Reporting

### Bug Reports

Include:
- Java version
- Operating system
- Askar version
- Steps to reproduce
- Expected vs actual behavior
- Stack trace (if applicable)

### Feature Requests

Include:
- Use case description
- Proposed API design
- Examples of how it would be used
- Any alternative solutions considered

## Native Library Integration

### FFI Guidelines

- Use JNA for native integration
- Define clear interfaces in `AskarLibrary`
- Handle memory management properly
- Use appropriate error handling
- Test on multiple platforms when possible

### Memory Management

- Use try-with-resources for auto-cleanup
- Free native resources explicitly when needed
- Implement proper finalizers as backup
- Use weak references where appropriate

## Security Considerations

- Handle sensitive data (keys, passwords) securely
- Zero out sensitive byte arrays when possible
- Avoid logging sensitive information
- Follow secure coding practices
- Report security issues privately

## Performance Guidelines

- Use async operations where provided by native library
- Minimize object allocations in hot paths
- Use appropriate data structures
- Profile performance-critical code
- Document performance characteristics

## Release Process

1. Update version numbers
2. Update CHANGELOG.md
3. Ensure all tests pass
4. Create release branch
5. Tag release
6. Build and test artifacts
7. Publish to Maven Central

## Getting Help

- Check existing issues and documentation first
- Join the Hyperledger Aries community channels
- Ask questions in GitHub discussions
- Attend community meetings

## Code of Conduct

This project follows the [Hyperledger Code of Conduct](https://wiki.hyperledger.org/display/HYP/Hyperledger+Code+of+Conduct).

## License

By contributing to this project, you agree that your contributions will be licensed under the same terms as the project (Apache 2.0 or MIT License).