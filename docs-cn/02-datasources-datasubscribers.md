---
id: datasources-datasubscribers
title: 数据源和数据订阅者
layout: docs-cn
permalink: /docs-cn/datasources-datasubscribers.html
prev: using-image-pipeline.html
next: closeable-references.html
---

[数据源](../javadoc/reference/com/facebook/datasource/DataSource.html) 和 [Future](http://developer.android.com/reference/java/util/concurrent/Future.html), 有些相似，都是异步计算的结果。

不同点在于，数据源对于一个调用会返回一系列结果，Future只返回一个。

提交一个Image request之后，Image
pipeline返回一个数据源。从中获取数据需要使用[数据订阅者(DataSubscriber)](../javadoc/reference/com/facebook/datasource/DataSubscriber.html).

### 当你仅仅需要Bitmap

如果你请求Image pipeline仅仅是为了获取一个 [Bitmap](http://developer.android.com/reference/android/graphics/Bitmap.html), 对象。你可以利用简单易用的[BaseBitmapDataSubscriber](../javadoc/reference/com/facebook/imagepipeline/datasource/BaseBitmapDataSubscriber):

```java
dataSource.subscribe(new BaseBitmapDataSubscriber() {
    @Override
    public void onNewResultImpl(@Nullable Bitmap bitmap) {
	   // You can use the bitmap in only limited ways
      // No need to do any cleanup.
    }

    @Override
    public void onFailureImpl(DataSource dataSource) {
      // No cleanup required here.
    }
  });
```

看起来很简单，对吧。下面是一些小警告:

千万 **不要** 把bitmap复制给`onNewResultImpl`函数范围之外的任何变量。订阅者执行完操作之后，image
pipeline
会回收这个bitmap，释放内存。在这个函数范围内再次使用这个Bitmap对象进行绘制将会导致`IllegalStateException`。

### 通用的解决方案

如果你就是想维持对这个Bitmap对象的引用，你不能维持纯Bitmap对象的引用，可以利用[可关闭的引用(closeable
references)](closeable-references.html) 和 [BaseDataSubscriber](../javadoc/reference/com/facebook/datasource/BaseDataSubscriber.html):

```java
DataSubscriber dataSubscriber =
    new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
  @Override
  public void onNewResultImpl(
      DataSource<CloseableReference<CloseableImage>> dataSource) {

    if (!dataSource.isFinished()) {
      FLog.v("Not yet finished - this is just another progressive scan.");
    }  

    CloseableReference<CloseableImage> imageReference = dataSource.getResult();
    if (imageReference != null) {
      try {
        CloseableImage image = imageReference.get();
        // do something with the image
      } finally {
        imageReference.close();
      }
    }
  }
  @Override
  public void onFailureImpl(DataSource dataSource) {
    Throwable throwable = dataSource.getFailureCause();
    // handle failure
  }
};

dataSource.subscribe(dataSubscriber, executor);
```

这样，只要遵守[可关闭的引用使用规则](closeable-references.html)，你就可以把这个`CloseableReference`复制给其他变量了。
