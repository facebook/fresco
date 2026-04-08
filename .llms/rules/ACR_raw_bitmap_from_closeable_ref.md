---
oncalls: ['fresco']
description: >
  Flag raw Bitmap references extracted from CloseableReference and stored
  in fields or passed across async boundaries — causes recycled bitmap crashes
  (D97310040, D92782210, D89773566 pattern)
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$|java/com/facebook/fresco/.*\.(kt|java)$|java/com/instagram/fresco/.*\.(kt|java)$'
apply_to_content: 'getUnderlyingBitmap|\.get\(\)|CloseableReference|CloseableBitmap'
---

# Raw Bitmap from CloseableReference Must Not Escape Scope

**Severity: CRITICAL** — #1 Fresco crash pattern: recycled bitmap use-after-free

For the full CloseableReference ownership and lifecycle rules, see https://fburl.com/closeable-references.

## What to Look For

- `closeableRef.get()` result stored in a class field (not a local variable)
- `closeableImage.getUnderlyingBitmap()` result passed to another component
- `BaseBitmapDataSubscriber.onNewResultImpl(bitmap)` — bitmap escaping method scope
- Raw `Bitmap` held without a corresponding `CloseableReference` keeping it alive

## When to Flag

- `val/var bitmap: Bitmap = closeableRef.get()` as a class field
- Passing `bitmap` from a `DataSubscriber` callback to another thread or component
- Storing `getUnderlyingBitmap()` result without calling `bitmap.copy()` first

## Do NOT Flag

- Local variables used within the same method scope and not stored
- Code that calls `CloseableReference.clone()` (proper ownership transfer)
- Code that calls `bitmap.copy(config, true)` before storing

## Examples

```kotlin
// BAD — Raw bitmap stored as field; Fresco may recycle it at any time
class ImageHolder {
    var bitmap: Bitmap? = null  // ❌ Dangling reference after CloseableRef closes

    fun onImageReady(ref: CloseableReference<CloseableImage>) {
        bitmap = (ref.get() as CloseableBitmap).underlyingBitmap
    }
}

// GOOD — Hold the CloseableReference itself
class ImageHolder {
    var imageRef: CloseableReference<CloseableImage>? = null  // ✅ Keeps bitmap alive

    fun onImageReady(ref: CloseableReference<CloseableImage>) {
        CloseableReference.closeSafely(imageRef)  // Close previous
        imageRef = ref.clone()  // Own a copy
    }

    fun release() {
        CloseableReference.closeSafely(imageRef)
        imageRef = null
    }
}
```

```kotlin
// BAD — Bitmap from DataSubscriber escaping callback scope
override fun onNewResultImpl(bitmap: Bitmap?) {
    subscriber.onBitmapLoaded(bitmap)  // ❌ Bitmap only valid during this call!
}

// GOOD — Copy before passing out
override fun onNewResultImpl(bitmap: Bitmap?) {
    val copy = bitmap?.copy(bitmap.config, true)  // ✅ Independent copy
    subscriber.onBitmapLoaded(copy)
}
```

## Evidence

- **D97310040**: `FrescoDataSourceSubscriber` passed Fresco-managed bitmap to UI thread without copy — recycled bitmap crash
- **D92782210**: `BaseBitmapDataSubscriber` bitmap escaped to `IconCompat.createWithAdaptiveBitmap()` — "Can't copy a recycled bitmap"
- **D89773566/D89773500**: WhatsApp held raw Bitmap instead of CloseableReference — GL crash on recycled bitmap
- **D89199711**: Cache eviction called `recycle()` on Fresco-owned bitmap
