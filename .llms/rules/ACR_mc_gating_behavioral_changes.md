---
oncalls: ['fresco']
description: >
  Flag behavioral changes to shared Fresco/Vito infrastructure without
  MobileConfig gating — all behavioral changes must be rollback-safe
  (oprisnik recurring review pattern)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
apply_to_content: 'fun |override |if |return |when '
---

# Behavioral Changes Must Be MobileConfig-Gated

**Severity: HIGH** — Ungated changes to shared image loading infrastructure cannot be rolled back without a new release

## What to Look For

- Changes to image loading behavior (decode, fetch, cache, render)
- Modified return values or control flow in producer chain
- New code paths in `KFrescoController`, `FrescoController2Impl`, or `ImagePipeline`
- Changed cache eviction logic or memory pool behavior

## When to Flag

- Behavioral change in shared code without `config.useNewBehavior` / MC flag guard
- New code path that is always-on with no way to disable
- Modified producer behavior without experiment gating

## Do NOT Flag

- Bug fixes that restore correct behavior (no gating needed)
- Test-only changes
- Documentation or comment changes
- Changes already inside an existing MC flag guard

## Examples

```kotlin
// BAD — Always-on behavioral change, can't roll back
fun fetch(request: ImageRequest): DataSource<CloseableReference<CloseableImage>> {
    return newFetchImplementation(request)  // ❌ No way to revert without code push
}

// GOOD — Gated with MC flag, can be disabled server-side
fun fetch(request: ImageRequest): DataSource<CloseableReference<CloseableImage>> {
    return if (config.useNewFetchImplementation) {
        newFetchImplementation(request)  // ✅ Can be disabled via MC
    } else {
        existingFetchImplementation(request)
    }
}
```

## Evidence

- Recurring review pattern from oprisnik across 20+ fresco diffs (Dec 2025 - Mar 2026)
- D96566942: MC-gated hot path optimization
- D90468548: MC-gated postprocessor prefetch behavior
