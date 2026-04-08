---
oncalls: ['fresco']
description: >
  Flag new Java files and non-idiomatic Kotlin patterns; prefer Kotlin for all
  new code (reviewer feedback from D92851254, D89896459)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
apply_to_content: 'class |interface |object |checkNotNull'
---

# Prefer Kotlin for New Code

**Severity: MEDIUM** — Team standard from D92851254, D89896459 reviewer feedback

## What to Look For

- New Java files being added (not modifications to existing)
- New test classes written in Java instead of Kotlin
- Kotlin code using `!!` (non-null assertion) operator
- Kotlin code using `checkNotNull()` instead of `?.let`

## When to Flag

- **New Java file**: "Why not Kotlin?" — new code should be Kotlin
- **`!!` operator**: Prefer safe calls (`?.`) or explicit null handling
- **`checkNotNull(x).doSomething()`**: Prefer `x?.let { it.doSomething() }`

## Do NOT Flag

- Modifications to existing Java files (incremental migration OK)
- Java files that are part of OSS Fresco public API
- `!!` in test assertions where crash is intentional

## Examples

```kotlin
// BAD — Non-null assertion can crash
val result = maybeNull!!.process()  // ❌ Crashes if null

// GOOD — Safe call with let
maybeNull?.let { result = it.process() }  // ✅ Safe

// GOOD — Elvis operator for default
val result = maybeNull?.process() ?: defaultValue  // ✅ Safe
```

## Recommendation

1. **New files**: Always create in Kotlin
2. **Null handling**: Use `?.let`, `?:`, `?.` instead of `!!` or `checkNotNull()`
3. **Tests**: Especially prioritize Kotlin for test code

## Evidence

- **D92851254**: Reviewer asked "Why not Kotlin?" for new Java test class
- **D89896459**: PostprocessorProducer converted to idiomatic Kotlin

