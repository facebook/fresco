---
docid: closeable-references
title: Closeable References
layout: docs
permalink: /docs/closeable-references.html
prev: datasources-datasubscribers.html
next: webp-support.html
---

**This page is intended for advanced usage only.**

Most apps should use [Drawees](using-drawees-xml.html) and not worry about closing.

The Java language is garbage-collected and most developers are used to creating objects willy-nilly and taking it for granted they will eventually disappear from memory.

Until Android 5.0's improvements, this was not at all a good idea for Bitmaps. They take up a large share of the memory of a mobile device. Their existence in memory would make the garbage collector run more frequently, making image-heavy apps slow and janky.

Bitmaps were the one thing that makes Java developers miss C++ and its many smart pointer libraries, such as [Boost](http://www.boost.org/doc/libs/1_57_0/libs/smart_ptr/smart_ptr.htm).

Fresco's solution is found in the [CloseableReference](../javadoc/reference/com/facebook/common/references/CloseableReference.html) class. In order to use it correctly, you must follow the rules below:

#### 1. The caller owns the reference.

Here, we create a reference, but since we're passing it to the caller, the caller takes the ownership:

```java
CloseableReference<Val> foo() {
  Val val;
  // We are returning the reference from this method,
  // so whoever is calling this method is the owner
  // of the reference and is in charge of closing it.
  return CloseableReference.of(val);
}
```

#### 2. The owner must close the reference before leaving scope.

Here we create a reference, but are not passing it to a caller. So we must close it:

```java
void gee() {
  // We are the caller of `foo` and so
  // we own the returned reference.
  CloseableReference<Val> ref = foo();
  try {
    // `haa` is a callee and not a caller, and so
    // it is NOT the owner of this reference, and
    // it must NOT close it.
    haa(ref);
  } finally {
    // We are not returning the reference to the
    // caller of this method, so we are still the owner,
    // and must close it before leaving the scope.
    ref.close();
  }
}
```

The `finally` block is almost always the best way to do this.

#### 3. **Never** close the value.

`CloseableReference` wraps a shared resource which gets released when there are no more active references pointing to it. Tracking of active references is done automatically by an internal reference counter. When the reference count drops to 0, `CloseableReference` will release the underlying resource. The very purpose of `CloseableReference` is to manage the underlying resource so that you don't have to. That said, you are responsible for closing the `CloseableReference` if you own it, but **not** the value it points to! If you explicitly close the underlying value, you will erroneously invalidate all the other active references pointing to that same resource.

```java
  CloseableReference<Val> ref = foo();

  Val val = ref.get();
  // do something with val
  // ...
  
  // Do NOT close the value!
  //// val.close();

  // DO close the reference instead.
  ref.close();
```

#### 4. Something other than the owner should *not* close the reference.

Here, we are receiving the reference via argument. The caller is still the owner, so we are not supposed to close it.

```java
void haa(CloseableReference<?> ref) {
  // We are callee, and not a caller, and so
  // we must NOT close the reference.
  // We are guaranteed that the reference won't
  // become invalid for the duration of this call.
  Log.println("Haa: " + ref.get());
}
```

If we called `.close()` here by mistake, then if the caller tried to call `.get()`, an `IllegalStateException` would be thrown.

#### 5. Callee should always clone the reference before assigning.

If we need to hold onto the reference, we need to clone it.

If using it in a class:

```java
class MyClass {
  CloseableReference<Val> myValRef;

  void mmm(CloseableReference<Val> ref) {
    // Some caller called this method. Caller owns the original
    // reference and if we want to have our own copy, we must clone it.
    myValRef = ref.clone();
  };
  // caller can now safely close its copy as we made our own clone.

  void close() {
    // We are in charge of closing our copy, of course.
    CloseableReference.closeSafely(myValRef);
  }
}
// Now the caller of MyClass must close it!
```

If using it in an inner class:

```java
void haa(CloseableReference<?> ref) {
  // Here we make our own copy of the original reference,
  // so that we can guarantee its validity when the executor
  // executes our runnable in the future.
  final CloseableReference<?> refClone = ref.clone();
  executor.submit(new Runnable() {
    public void run() {
      try {
        Log.println("Haa Async: " + refClone.get());
      } finally {
        // We need to close our copy once we are done with it.
        refClone.close();
      }
    }
  });
  // caller can now safely close its copy as we made our own clone.
}
```
