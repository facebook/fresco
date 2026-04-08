---
oncalls: ['fresco']
description: >
  Flag internal Meta imports (com.meta.*) in OSS-exported Fresco code —
  libraries/fresco/ is synced to GitHub and must not use internal packages
  (D97540365 revert)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
apply_to_content: 'import com\.meta\.'
---

# No Internal Imports in OSS-Exported Fresco Code

**Severity: HIGH** — libraries/fresco/ is open-source and synced to GitHub

## What to Look For

- `import com.meta.*` in any file under `libraries/fresco/`
- Automated codemod migrations that replace `com.facebook.*` with `com.meta.*`
- New dependencies on internal-only packages
- New BUCK dependencies added without a corresponding entry in `build.gradle` (breaks the OSS Gradle build)

## When to Flag

- Any `import com.meta.*` statement in `libraries/fresco/`
- Any new BUCK dependency on a `com.meta.*` target from `libraries/fresco/`

## Do NOT Flag

- `import com.meta.*` in FB-internal code (`java/com/facebook/fresco/`, `java/com/meta/images/`)
- Test-only files that are not exported to OSS

## Examples

```kotlin
// BAD — Internal import in OSS code (breaks GitHub sync)
import com.meta.dumpapp.internal.DumperPlugin  // ❌ Not available in OSS

// GOOD — Use original OSS-compatible import
import com.facebook.stetho.dumpapp.DumperPlugin  // ✅ Available in OSS
```

## Evidence

- **D97540365**: Automated Stetho→Dumpapp migration broke OSS Fresco samples, required manual revert
