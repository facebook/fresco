---
oncalls: ['fresco']
description: >
  Flag Java 8 Stream APIs and other methods requiring API 24+ when minSdkVersion
  is 21 (D93795294 build fix, related to SEV S311353)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
apply_to_content: '\.stream\(|\.parallelStream\(|computeIfAbsent|putIfAbsent|getOrDefault|Comparator\.(natural|reverse)Order\('
---

# API Level 24+ Methods Used with minSdkVersion 21

**Severity: HIGH** — D93795294 fixed build failure; related SEV S311353 was production crash

## What to Look For

- Java 8 Stream APIs: `.stream()`, `.parallelStream()`, `.collect()`
- Map methods: `computeIfAbsent()`, `putIfAbsent()`, `getOrDefault()`, `forEach()`
- Comparator methods: `Comparator.naturalOrder()`, `Comparator.reverseOrder()`
- `Stream.of()`, `Collectors.toList()`

## When to Flag

- Any of the above APIs used in code with `minSdkVersion < 24`
- Missing `@RequiresApi` or `@TargetApi` annotation with version guard
- No fallback implementation for API 21-23 devices

## Do NOT Flag

- Code inside `if (Build.VERSION.SDK_INT >= 24)` blocks
- Code with `@RequiresApi(24)` annotation AND version guard
- Kotlin stdlib equivalents (`.map {}`, `.filter {}`)
- Test code that only runs on API 24+ devices

## Examples

```kotlin
// BAD — parallelStream requires API 24+
items.parallelStream()
    .map { transform(it) }
    .collect(Collectors.toList())  // ❌ Crashes on API 21-23

// GOOD — Kotlin stdlib works on all API levels
items.map { transform(it) }  // ✅ Safe
```

```java
// BAD — naturalOrder requires API 24+
Queue<Long> heap = new PriorityQueue<>(Comparator.naturalOrder());  // ❌

// GOOD — Custom comparator works on all APIs
Queue<Long> heap = new PriorityQueue<>(capacity,
    (t1, t2) -> Long.compare(t1, t2));  // ✅ Safe
```

## Recommendation

For Fresco (minSdk 21):
1. **Never use** `.stream()` or `.parallelStream()`
2. **Never use** `Comparator.naturalOrder()` or `reverseOrder()`
3. **Always use** Kotlin stdlib (`map`, `filter`) or manual loops

## Evidence

- **D93795294**: `parallelStream()` in iterativeBlurBoxFilter.kt broke gradle build
- **SEV S311353**: Production crash from `Comparator.naturalOrder()` (API 24+)

