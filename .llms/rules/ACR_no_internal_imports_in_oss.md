---
oncalls: ['fresco']
description: >
  Guard the OSS Gradle build — libraries/fresco/ is synced to GitHub.
  Flag internal imports, unavailable dependency versions, missing Gradle
  test deps, and internal-only API usage from codemods.
apply_to_regex: 'libraries/fresco/.*\.(kt|java|gradle)$'
---

# OSS Build Compatibility for Fresco

**Severity: HIGH** — libraries/fresco/ is open-source and synced to GitHub

Fresco has two build systems: internal Buck and OSS Gradle. Changes that work internally often break the OSS Gradle build (`ci/build-and-test.sh`). This rule covers the common breakage patterns.

## 1. No Internal Imports

- `import com.meta.*` in any file under `libraries/fresco/`
- Internal Litho APIs not available in the OSS Litho release (e.g., `PrimitiveImage` from `com.facebook.litho.widget`)

### Evidence

- **D97540365**: Stetho→Dumpapp migration broke OSS samples
- **D102613081**: AI codemod migrated `Image.create(c)...build()` to `PrimitiveImage(...)` which does not exist in OSS Litho 0.50.1

## 2. Dependency Version Bumps Must Exist on Maven Central

Before bumping a dependency version in `buildSrc/dependencies.kt`, verify the artifact actually exists on Maven Central. Several libraries used by Fresco are archived and have no new releases.

### Known Archived Libraries (do NOT bump)

- **Stetho**: Last release 1.6.0 (archived ~2020)
- **Volley**: Last release 1.2.1

### AndroidX Version Constraints

AndroidX library upgrades can require a higher `compileSdkVersion`. For example, `androidx.core:core:1.15.0` requires `compileSdk 35` but Fresco uses 34. Check the AAR metadata for `minCompileSdk` requirements before bumping.

### OkHttp 3 vs 4

The `imagepipeline-okhttp3` module uses OkHttp 3 Java method-call syntax (`response.body()`, `okHttpClient.dispatcher().executorService()`). OkHttp 4 is a Kotlin rewrite where these became properties (`response.body`, `okHttpClient.dispatcher.executorService`). Do not bump to OkHttp 4.x without updating all call sites.

### Evidence

- **D91469563**: Bumped Stetho to 1.6.1 (does not exist), Volley to 1.2.2 (does not exist), OkHttp to 4.11.0 (API incompatible), AndroidX Core to 1.15.0 (requires compileSdk 35)

## 3. New BUCK Test Dependencies Need Gradle Equivalents

When adding a test file or test dependency via Buck, also add the corresponding `testImplementation` in the module's `build.gradle`. Common missing deps:

- `TestDeps.junit` — needed for `@Test`, `assertEquals`
- `TestDeps.assertjCore` — needed for `assertThat`
- `TestDeps.robolectric` — needed for `@RunWith(RobolectricTestRunner::class)`
- `Deps.inferAnnotation` — needed if test files use `@Nullsafe`
- `TestDeps.mockitoKotlin3` — needed for `mock<T>()`, `whenever`, `verify`

### Evidence

- `ui-common`, `vito:view`, `native-filters`, `imagepipeline` all had tests added internally without Gradle dependency updates

## 4. Mockito-Kotlin Version Compatibility

The OSS build uses mockito-kotlin **3.1.0**. The following APIs are NOT available until 4.x:

- `org.mockito.kotlin.verifyNoInteractions` — use `org.mockito.Mockito.verifyNoInteractions` instead
- `Mockito.mock<T>()` (reified, no class param) — use `mock<T>()` from mockito-kotlin or `Mockito.mock(T::class.java)`

### Evidence

- **D99865098**: Migrated to `org.mockito.kotlin.verifyNoInteractions` which does not exist in 3.1.0

## Do NOT Flag

- `import com.meta.*` in FB-internal code (`java/com/facebook/fresco/`, `java/com/meta/images/`)
- Test-only files that are not exported to OSS
- Modifications to existing Java files using existing internal patterns
