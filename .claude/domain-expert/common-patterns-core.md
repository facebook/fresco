<!-- analysis-metadata: { "date": "2026-03-25", "time_window": "30d", "max_diffs": 50, "diffs_analyzed": 7 } -->
# Fresco Core Library - Core Patterns

**Team**: MF Client - QoE
**Oncall**: fresco
**Last Updated**: 2026-03-25

---

## 1. Dimension Validation: The Zero-Dimension Trap

### The Problem
Image processing code often assumes dimensions are positive, but frames can be loaded before layout is complete, resulting in zero width/height.

### Why It Causes Problems
- Division by zero in scaling calculations
- Infinite loops when iterating through frames
- Crashes in native code that doesn't handle edge cases

### How to Think About It
When you see code that:
- Calculates aspect ratios
- Iterates through frames
- Passes dimensions to native code

Ask yourself: *"What happens if width or height is 0?"*

```kotlin
// ❌ Dangerous: Assumes positive dimensions
val aspectRatio = width.toFloat() / height.toFloat()

// ✅ Safe: Guard against zero
if (width <= 0 || height <= 0) {
    return findNearestValidFrame()  // Fallback strategy
}
val aspectRatio = width.toFloat() / height.toFloat()
```

### Real Bug Examples
- D92552368: Skip frame loading when dimensions are zero
- D92552358: Handle zero dimensions in BufferFrameLoader.getFrame

---

## 2. Thread Context: The Layout Thread Deadlock

### The Problem
Fresco code may execute on various threads including Litho layout threads, which must never block.

### Why It Causes Problems
- ANRs when sync operations block the main thread
- Deadlocks when layout threads wait for resources
- UI jank when background work runs on foreground threads

### How to Think About It
When reviewing code in:
- Prefetch paths
- `onBoundsDefined` callbacks
- Any code called during image preparation

Ask yourself: *"Could this block? What thread am I on?"*

```kotlin
// ❌ Dangerous: Sync network call in layout path
fun shouldFetch(): Boolean {
    return networkService.checkAvailability()  // Blocks!
}

// ✅ Safe: Check thread context
fun shouldRunImmediately(): Boolean {
    val threadName = Thread.currentThread().name
    if (threadName.contains("ComponentLayoutThread") ||
        threadName.startsWith("litho_")) {
        return false  // Defer to background
    }
    return true
}
```

### Real Bug Examples
- D91274295: Fix ANR from synchronous network fetch on Litho layout threads

---

## 3. CloseableReference: The Memory Leak Pattern

### The Problem
`CloseableReference` manages native memory through reference counting. Missing close() calls leak memory.

### Why It Causes Problems
- Native memory isn't tracked by GC
- Leaks accumulate causing OOM
- Difficult to debug - no stack traces

### How to Think About It
Every `CloseableReference.of()` or `.clone()` MUST have a corresponding `.close()`. In Kotlin, prefer the `use()` extension; in Java, use `try-finally`.

```kotlin
// ❌ Dangerous: Reference not closed on error path
fun processImage(ref: CloseableReference<Bitmap>) {
    val cloned = ref.clone()
    doWork(cloned)  // What if this throws?
    cloned.close()
}

// ✅ Preferred (Kotlin): use() extension handles close automatically
fun processImage(ref: CloseableReference<Bitmap>) {
    ref.clone().use { cloned ->
        doWork(cloned)
    }
}

// ✅ Also safe (Java fallback): try-finally
fun processImage(ref: CloseableReference<Bitmap>) {
    val cloned = ref.clone()
    try {
        doWork(cloned)
    } finally {
        cloned.close()
    }
}
```

---

## 4. Cache Key Consistency: The Prefetch Mismatch

### The Problem
If prefetch and fetch use different cache keys (e.g., one includes postprocessor, one doesn't), prefetched data won't be found.

### Why It Causes Problems
- Wasted bandwidth from re-fetching
- Increased latency despite prefetching
- Confusing debugging - "why isn't my prefetch working?"

### How to Think About It
When modifying cache key generation or prefetch logic, trace both paths and ask: *"Will the fetch find what prefetch stored?"*

```kotlin
// ❌ Inconsistent: Prefetch excludes postprocessor
fun getPrefetchCacheKey(request: ImageRequest): CacheKey {
    return CacheKeyFactory.create(request.sourceUri)  // Missing postprocessor!
}

fun getFetchCacheKey(request: ImageRequest): CacheKey {
    return CacheKeyFactory.create(request.sourceUri, request.postprocessor)
}

// ✅ Consistent: Both include all relevant parameters
fun getCacheKey(request: ImageRequest): CacheKey {
    return CacheKeyFactory.create(
        request.sourceUri,
        request.postprocessor,
        request.resizeOptions
    )
}
```

### Real Bug Examples
- D90468548: Fix postprocessor not applied during decoded image prefetch

---

## 5. DataSource Extras: The Memory Cache Blind Spot

### The Problem
`datasourceExtras` is null for memory cache hits. Using it alone misses cached image metadata.

### Why It Causes Problems
- Logging/analytics miss memory cache hits
- Feature flags based on extras don't work for cached images
- Inconsistent behavior between cache hit and miss paths

### How to Think About It
When accessing image metadata via extras, always provide a fallback:

```kotlin
// ❌ Incomplete: Misses memory cache hits and keys not in datasourceExtras
val encodedSize = imagePerfData.datasourceExtras?.get("encoded_size")

// ✅ Complete: Check datasourceExtras first, fall back to imageExtras
val encodedSize = imagePerfData.datasourceExtras?.get("encoded_size")
    ?: imagePerfData.imageExtras?.get("encoded_size")
```

---

## Review Checklist (Core)

- [ ] **Dimensions**: What happens if width or height is 0?
- [ ] **Threads**: Could this block? What thread is this on?
- [ ] **Memory**: Are all CloseableReferences closed in all paths?
- [ ] **Cache Keys**: Will fetch find what prefetch stored?
- [ ] **Extras**: Am I handling both cache hit and miss paths?
