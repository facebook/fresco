---
oncalls: ['fresco']
description: >
  Fresco team conventions, testing commands, debugging tips, critical code areas
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
---

# Fresco Core - Domain Knowledge

## Team Conventions

### Language Preference
- **New code**: Kotlin preferred
- **Existing Java files**: OK to continue in Java
- **Tests**: Kotlin with JUnit 5

### Commit Messages
```
[fresco][component] Brief description

Longer explanation if needed.

Differential Revision: D12345678
```

## Testing

### Run Unit Tests
```bash
buck2 test fbandroid//libraries/fresco/...:
```

### Run Specific Test
```bash
buck2 test fbandroid//libraries/fresco/vito/core:core-test
```

## Debugging

### Enable Fresco Logging
```kotlin
FLog.setMinimumLoggingLevel(FLog.VERBOSE)
```

### Memory Debugging
```kotlin
// Check for leaks
CloseableReference.setDisableCloseableReferencesForBitmaps(false)
```

## Critical Code Areas

These areas require extra review care:

1. **Native memory** (`memory-types/`, `nativecode/`)
2. **Cache eviction** (`imagepipeline/cache/`)
3. **Producer chain** (`imagepipeline/producers/`)
4. **Animation** (`animated-*/`)

## Common Mistakes to Avoid

1. Forgetting to close `CloseableReference`
2. Blocking on UI thread
3. Not handling null `ImageInfo`
4. Inconsistent cache keys between prefetch/fetch
5. Missing API level checks for Java 8 APIs
