# Fresco Core Library - Domain Knowledge

**Team**: MF Client - QoE
**Oncall**: fresco
**Last Updated**: 2026-03-16

---

## Team Conventions

### Language Preference
- **New code**: Kotlin
- **Tests**: Kotlin (strongly preferred)
- **Java**: Only for JNI interfaces or legacy compatibility

### Naming Conventions
- **Interfaces**: No prefix/suffix (e.g., `ImagePipeline`, `FrescoController2`)
- **Implementations**: Descriptive suffix (e.g., `ImagePipelineImpl`, `KFrescoController`)
- **Constants**: `SCREAMING_SNAKE_CASE`
- **Producers**: Suffix with `Producer` (e.g., `DiskCacheReadProducer`)

### Configuration Pattern
All configuration should use MobileConfig for A/B testing:
```kotlin
// Pattern: MC-backed config via FrescoVitoConfig
interface FrescoVitoConfig {
    val useNewFeature: Boolean
        get() = false  // Safe default; app-specific impl reads MC
}
```

---

## Critical Code Areas

### High-Risk Areas (Extra Review Care)

| Area | Why Critical | What to Watch |
|------|--------------|---------------|
| `imagepipeline/memory/` | OOM risk | Memory limits, pool sizing |
| `imagepipeline/producers/` | Data integrity | Cache consistency, error handling |
| `native-imagetranscoder/` | Security | Buffer bounds, input validation |
| `animated-*/` | Performance | Frame scheduling, memory per frame |
| `vito/core-impl/` | User-facing | Lifecycle, threading |

### Frequently Changed Files
Based on recent diff history, these files change often and need careful review:
- `KFrescoController.kt` - Main controller logic
- `BasePool.kt` - Memory pool management
- `BufferFrameLoader.kt` - Animation frame loading

---

## Implicit Contracts

### 1. CloseableReference Ownership
When a method accepts a `CloseableReference`, the convention determines who closes it:
- **Consumer pattern**: Receiver clones if needed, caller closes original
- **Transfer pattern**: Caller transfers ownership, receiver closes

Always document which pattern a method uses. See https://fburl.com/closeable-references for the full guide.

### 2. Producer Chain Order
Producers MUST be chained in this order (outermost to innermost):
1. Bitmap memory cache
2. Thread handoff
3. Encoded memory cache
4. Disk cache
5. Network/Local fetch

Reordering can cause cache misses or data inconsistency.

### 3. CallerContext Requirements
Every image request should have a CallerContext that includes:
- Calling class name (for attribution)

Missing CallerContext breaks analytics and debugging.

---

## Testing Patterns

### Test Framework Setup
```kotlin
@RunWith(WithTestDefaultsRunner::class)
class MyTest {
    // Robolectric for Android framework mocking
}
```

### Common Test Utilities
- `imagepipeline-test/` - Test doubles and utilities (e.g., `MockBitmapFactory`)
- Use Mockito to mock `Producer<T>` interfaces for testing producer chains

### What to Test
1. **Happy path**: Normal operation
2. **Edge cases**: Zero dimensions, null inputs, empty data
3. **Error paths**: Network failures, decode failures
4. **Threading**: Ensure correct thread for callbacks
5. **Memory**: Verify CloseableReferences are closed

---

## Performance Considerations

### Memory Budget
- Bitmap memory cache: ~25% of heap
- Encoded memory cache: ~10% of heap
- Small disk cache: 10MB (thumbnails)
- Main disk cache: 40-100MB (full images)

### Critical Performance Paths
1. **Memory cache hit**: Must be <1ms, runs on main thread
2. **Decode**: Should be <50ms for typical images
3. **Network fetch**: Depends on network, but pipeline overhead <10ms

### Performance Anti-patterns
- Decoding on main thread
- Synchronous disk access
- Creating new Bitmap objects instead of reusing pools
- Not using downsampling for thumbnails

---

## Related Documentation

### Internal Wikis
- Fresco Architecture: https://www.internalfb.com/wiki/Fresco/
- Vito Migration Guide: https://www.internalfb.com/wiki/Fresco/Vito/
- Image Performance: https://www.internalfb.com/wiki/Media_Foundation/Image_Performance/

### External (OSS)
- Fresco Documentation: https://frescolib.org/docs/
- GitHub: https://github.com/facebook/fresco

---

## Common Debugging

### Debug Overlays
Enable image debug overlay to see:
- Image URI
- Dimensions (requested vs actual)
- Cache source
- Load time

### Logging
- Enable Fresco logging via `FLog.setMinimumLoggingLevel()`
- QPL markers for performance analysis
- Use CallerContext for request attribution

### Common Issues

| Symptom | Likely Cause | Investigation |
|---------|--------------|---------------|
| Images not loading | Cache key mismatch | Check prefetch vs fetch keys |
| OOM crashes | Memory leak or no limits | Check CloseableReference cleanup |
| Slow loading | Cache misses | Verify cache configuration |
| ANRs | Main thread blocking | Check for sync operations |
| Flickering | Controller/view mismatch | Check lifecycle handling |

---

## MobileConfig Flags (Key Experiments)

| Flag | Purpose |
|------|---------|
| `android_fresco_vito.*` | Vito-specific experiments |
| `android_image_pipeline.*` | Pipeline behavior changes |
| `android_fresco_animated.*` | Animation-related experiments |

Before adding new MC flags, check existing ones to avoid duplication.

---

## Contact & Escalation

- **Oncall**: fresco
- **Team**: MF Client - QoE
- **Slack**: #fresco-support

For urgent production issues affecting image loading, page the oncall immediately.
