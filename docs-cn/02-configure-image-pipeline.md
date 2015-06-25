---
id: configure-image-pipeline
title: 配置Image Pipeline
layout: docs-cn
permalink: /docs-cn/configure-image-pipeline.html
prev: intro-image-pipeline.html
next: caching.html
---

对于大多数的应用，Fresco的初始化，只需要以下一句代码:

```java
Fresco.initialize(context);
```

对于那些需要更多进一步配置的应用，我们提供了[ImagePipelineConfig](../javadoc/reference/com/facebook/imagepipeline/core/ImagePipelineConfig.html)。

以下是一个示例配置，列出了所有可配置的选项。几乎没有应用是需要以下这所有的配置的，列出来仅仅是为了作为参考。


```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
    .setBitmapMemoryCacheParamsSupplier(bitmapCacheParamsSupplier)
    .setCacheKeyFactory(cacheKeyFactory)
    .setEncodedMemoryCacheParamsSupplier(encodedCacheParamsSupplier)
    .setExecutorSupplier(executorSupplier)
    .setImageCacheStatsTracker(imageCacheStatsTracker)
    .setMainDiskCacheConfig(mainDiskCacheConfig)
    .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
    .setNetworkFetchProducer(networkFetchProducer)
    .setPoolFactory(poolFactory)
    .setProgressiveJpegConfig(progressiveJpegConfig)
    .setRequestListeners(requestListeners)
    .setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
    .build();
Fresco.initialize(context, config);
```

请记得将配置好的`ImagePipelineConfig` 传递给 `Fresco.initialize!` 否则仍旧是默认配置。

### 关于Supplier

许多配置的Builder都接受一个[Supplier](../javadoc/reference/com/facebook/common/internal/Supplier.html) 类型的参数而不是一个配置的实例。

创建时也许有一些麻烦，但这带来更多的利好：这允许在运行时改变创建行为。以内存缓存为例，每隔5分钟就可检查一下Supplier，根据实际情况返回不同类型。

如果你需要动态改变参数，那就是用Supplier每次都返回同一个对象。

```java
Supplier<X> xSupplier = new Supplier<X>() {
  public X get() {
    return new X(xparam1, xparam2...);
  }
);
// when creating image pipeline
.setXSupplier(xSupplier);
```

### 线程池

Image pipeline 默认有3个线程池:

1. 3个线程用于网络下载
2. 两个线程用于磁盘操作: 本地文件的读取，磁盘缓存操作。
3. 两个线程用于CPU相关的操作: 解码，转换，以及后处理等后台操作。

对于网络下载，你可以定制网络层的操作，具体参考:[自定义网络层加载](using-other-network-layers.html).

对于其他操作，如果要改变他们的行为，传入一个[ExecutorSupplier](../javadoc/reference/com/facebook/imagepipeline/core/ExecutorSupplier.html)即可。

### 内存缓存的配置

内存缓存和未解码的内存缓存的配置由一个Supplier控制，这个Supplier返回一个[MemoryCacheParams](../javadoc/reference/com/facebook/imagepipeline/cache/MemoryCacheParams.html#MemoryCacheParams\(int, int, int, int, int\)) 对象用于内存状态控制。

### 配置磁盘缓存

你可使用Builder模式创建一个 [DiskCacheConfig](../javadoc/reference/com/facebook/cache/disk/DiskCacheConfig.Builder.html):

```java
DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder()
   .set....
   .set....
   .build()

// when building ImagePipelineConfig
.setMainDiskCacheConfig(diskCacheConfig)
```

### 缓存统计

如果你想统计缓存的命中率，你可以实现[ImageCacheStatsTracker](../javadoc/reference/com/facebook/imagepipeline/cache/ImageCacheStatsTracker.html), 在这个类中，每个缓存时间都有回调通知，基于这些事件，可以实现缓存的计数和统计。
