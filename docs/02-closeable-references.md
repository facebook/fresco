---
id: closeable-references
title: Closeable References
layout: docs
permalink: /docs/closeable-references.html
prev: datasources-datasubscribers.html
next: using-other-network-layers.html
---

**This page is intended for advanced usage only.** 

Most apps should use [Drawees](using-drawees-xml.html) and not worry about closing.

The Java language is garbage-collected and most developers are used to creating objects willy-nilly and taking it for granted they will eventually disappear from memory.

Until Android 5.0's improvements, this was not at all a good idea for Bitmaps. They take up a large share of the memory of a mobile device. Their existence in memory would make the garbage collector run more frequently, making image-heavy apps slow and janky.

Bitmaps were the one thing that makes Java developers miss C++ and its many smart pointer libraries, such as [Boost](http://www.boost.org/doc/libs/1_57_0/libs/smart_ptr/smart_ptr.htm). 

Fresco's solution is found in the [CloseableReference](../javadoc/reference/com/facebook/common/references/CloseableReference.html) class. In order to use it correctly, you must follow the rules below:

#### 1. The caller owns the reference.

Here, we create a reference, but since we're passing it to a caller, the caller takes it:

```java
CloseableReference<Val> foo() {
  Val val;
  return CloseableReference.of(val);
}
```

#### 2. The owner must close the reference before leaving scope.

Here we create a reference, but are not passing it to a caller. So we must close it:

```java
void gee() {
  CloseableReference<Val> ref = foo();
  try {
    haa(ref);
  } finally {
    ref.close();
  }
}
```

The `finally` block is almost always the best way to do this.

#### 3. Something other than the owner should *not* close the reference.

Here, we are receiving the reference via argument. The caller is still the owner, so we are not supposed to close it.

```java
void haa(CloseableReference<?> ref) {
  Log.println("Haa: " + ref.get());
}
```

If we called `.close()` here by mistake, then if the caller tried to call `.get()`, an `IllegalStateException` would be thrown.

#### 4. Always clone the reference before assigning.

If we need to hold onto the reference, we need to clone it. 

If using it in a class:

```java
class MyClass {
  CloseableReference<Val> myValRef;

  void mmm(CloseableReference<Val> ref) {
    myValRef = ref.clone();
  };
  // caller can now safely close its copy as we made our own clone.
  
  void close() {
    CloseableReference.closeSafely(myValRef);
  }
}
// Now the caller of MyClass must close it!
```

If using it in an inner class:

```java
void haa(CloseableReference<?> ref) {
  final CloseableReference<?> refClone = ref.clone();
  executor.submit(new Runnable() {
    public void run() {
      try {
        Log.println("Haa Async: " + refClone.get());
      } finally {
        refClone.close();
      }
    }
  });
  // caller can now safely close its copy as we made our own clone.
}
