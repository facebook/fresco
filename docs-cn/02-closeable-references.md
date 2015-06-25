---
id: closeable-references
title: 可关闭的引用
layout: docs-cn
permalink: /docs-cn/closeable-references.html
prev: datasources-datasubscribers.html
next: using-other-network-layers.html
---

**本页内容仅为高级使用作参考**

大部分的应用，直接使用[Drawees](using-drawees-xml.html)就好了，不用考虑关闭的事情了。

Java带有垃圾收集功能，许多开发者习惯于不自觉地创建一大堆乱七八糟的对象，并且想当然地认为他们会从内存中想当然地消失。

在5.0系统之前，这样的做法对于操作Bitmap是极其糟糕的。Bitmap占用了大量的内存，大量的内存申请和释放引发频繁的GC，使得界面卡顿不已。

Bitmap 是Java中为数不多的能让Java开发者想念或者羡慕C++以及C++众多的指针库，比如[Boost](http://www.boost.org/doc/libs/1_57_0/libs/smart_ptr/smart_ptr.htm) 的东西。

Fresco的解决方案是: [可关闭的引用(CloseableReference)](../javadoc/reference/com/facebook/common/references/CloseableReference.html)

为了正确地使用它，请按以下步骤进行操作:

#### 1. 调用者拥有这个引用

我们创建一个引用，但我们传递给了一个调用者，调用者将持有这个引用。

```java
CloseableReference<Val> foo() {
  Val val;
  return CloseableReference.of(val);
}
```

#### 2. 持有者在离开作用域之前，需要关闭引用

创建了一个引用，但是没有传递给其他调用者，在结束时，需要关闭。

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

`finally` 中最适合做此类事情了。

#### 3. 除了引用的持有者，闲杂人等**不得**关闭引用

作为一个参数传递，调用者持有这个引用，在下面的函数体中，不能关闭引用。

```java
void haa(CloseableReference<?> ref) {
  Log.println("Haa: " + ref.get());
}
```

如果调用了 `.close()`, 调用者尝试调用 `.get()`时，会抛出`IllegalStateException`

#### 4. 在赋值给变量前，先进行clone

在类中使用:

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
// MyClass的调用者需要关闭myValRef
```

在内部中使用:

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
  // 当前函数域内可安全关闭，闭包内为已经clone过的引用。
}
