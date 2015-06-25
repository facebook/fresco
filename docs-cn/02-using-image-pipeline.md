---
id: using-image-pipeline
title: 直接使用Image Pipeline
layout: docs-cn
permalink: /docs-cn/using-image-pipeline.html
prev: caching.html
next: datasources-datasubscribers.html
---

本页介绍Image pipeline的高级用法，大部分的应用使用[Drawees](using-drawees-xml.html) 和image pipeline打交道就好了。

直接使用Image pipeline是较为有挑战的事情，这意味着要维护图片的内存使用。Drawees
会根据各种情况确定图片是否需要在内存缓存中，在需要时加载，在不需要时移除。直接使用的话，你需要自己完成这些逻辑。

Image pipeline返回的是一个[CloseableReference](closeable-references.html)对象。在这些对象不需要时，Drawees会调用`.close()`方法。如果你的应用不使用Drawees，那你需要自己完成这个事情。

Java的GC机制会在Bitmap不使用时，清理掉Bitmap。但要GC时总是太迟了，另外GC是很昂贵的开销。GC大对象也会带来性能问题，尤其是在5.0以下系统。

#### 调用 pipeline

首先[创建一个image request](image-requests.html). 然后传递给 `ImagePipeline:`

```java
ImagePipeline imagePipeline = Fresco.getImagePipeline();
DataSource<CloseableReference<CloseableImage>>
    dataSource = imagePipeline.fetchDecodedImage(imageRequest);
```

关于如果接收数据，请参考[数据源](datasources-datasubscribers.html) 章节。

#### 忽略解码

如果你不保持图片原始格式，不执行解码，使用`fetchEncodedImage`即可:

```java
DataSource<CloseableReference<PooledByteBuffer>>
    dataSource = imagePipeline.fetchEncodedImage(imageRequest);
```

#### 从Bitmap缓存中立刻取到结果

不像其他缓存，如果图片在内存缓存中有的话，可以在UI线程立刻拿到结果。

```java
DataSource<CloseableReference<CloseableImage>> dataSource =
    mImagePipeline.fetchImageFromBitmapCache(imageRequest);
CloseableReference<CloseableImage> imageReference;
try {
  imageReference = dataSource.getResult();
  if (imageReference != null) {
    CloseableImage image = imageReference.get();
    // do something with the image
  }
} finally {
  dataSource.close();
  CloseableReference.closeSafely(imageReference);
}
```

千万 **不要** 省略掉 `finally` 中的代码!

#### 预加载图片

预加载图片可减少用户等待的时间，如果预加载的图片用户没有真正呈现给用户，那么就浪费了用户的流量，电量，内存等资源了。大多数应用，并不需要预加载。


Image pipeline 提供两种预加载方式。

预加载到文件缓存:

```java
imagePipeline.prefetchToDiskCache(imageRequest);
```

预加载到内存缓存:

```java
imagePipeline.prefetchToBitmapCache(imageRequest);
```
