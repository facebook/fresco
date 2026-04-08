---
oncalls: ['fresco']
description: >
  Flag loop variables that are declared but not used in the loop body,
  especially when a parameter with similar meaning exists in scope (D95237240)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
apply_to_content: 'for\s*\(|while\s*\('
---

# Loop Variable Not Used in Loop Body

**Severity: HIGH** — D95237240 caused animation glitches in variable frame rate GIFs

## What to Look For

- `for` or `while` loops where the declared iterator variable is not referenced in the loop body
- Method parameters with similar semantic meaning to the loop variable (e.g., `frameNumber` vs `i`)
- Array/list indexing that uses a parameter instead of the loop iterator

## When to Flag

- Loop declares variable `i` or similar, but loop body uses outer scope variable instead
- Pattern: `for (i in 0 until n) { array[n] }` — uses `n` instead of `i`
- The loop would produce incorrect results (repeating same value N times)

## Do NOT Flag

- Loops where the iterator is intentionally unused (e.g., repeat N times)
- Loops with explicit `@Suppress("UNUSED_VARIABLE")` annotation
- Loops where the iterator is used in a nested scope

## Examples

```kotlin
// BAD — uses parameter frameNumber instead of loop variable i
for (i in 0 until frameNumber) {
    sum += frameDurations[frameNumber]  // ❌ Wrong: adds same duration N times
}

// GOOD — uses loop variable i correctly
for (i in 0 until frameNumber) {
    sum += frameDurations[i]  // ✅ Correct: sums durations 0 to frameNumber-1
}
```

## Recommendation

When writing loops that iterate through a range, always verify the loop body uses the iterator variable for indexing, not the range boundary parameter.

## Evidence

- **D95237240**: `getTargetRenderTimeMs()` in DropFramesFrameScheduler used `frameNumber` instead of `i`, causing incorrect animation timing for GIFs with variable frame durations
- **Impact**: `jumpToFrame()` calculated wrong loop counts, causing premature animation termination

