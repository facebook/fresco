---
id: caching
title: 缓存
layout: docs-cn
permalink: /docs-cn/caching.html
prev: configure-image-pipeline.html
next: using-image-pipeline.html
---

###  三级缓存

#### 1. Bitmap缓存

Bitmap缓存存储`Bitmap`对象，这些Bitmap对象可以立刻用来显示或者用于[后处理](modifying-images.html)

在5.0以下系统，Bitmap缓存位于ashmem，这样Bitmap对象的创建和释放将不会引发GC，更少的GC会使你的APP运行得更加流畅。

5.0及其以上系统，相比之下，内存管理有了很大改进，所以Bitmap缓存直接位于Java的heap上。

当应用在后台运行是，该内存会被清空。

#### 2. 未解码图片的内存缓存

这个缓存存储的是原始压缩格式的图片。从该缓存取到的图片在使用之前，需要先进行解码。

如果有调整大小，旋转，或者WebP编码转换工作需要完成，这些工作会在解码之前进行。

APP在后台时，这个缓存同样会被清空。

#### 3. 文件缓存

和未解码的内存缓存相似，文件缓存存储的是未解码的原始压缩格式的图片，在使用之前同样需要经过解码等处理。

和内存缓存不一样，APP在后台时，内容是不会被清空的。即使关机也不会。用户可以随时用系统的设置菜单中进行清空缓存操作。

### 用一个文件还是两个文件缓存?

如果要使用2个缓存，在[配置image pipeline](configure-image-pipeline.html) 时调用 `setMainDiskCacheConfig` 和 `setSmallImageDiskCacheConfig`  方法即可。

大部分的应用有一个文件缓存就够了，但是在一些情况下，你可能需要两个缓存。比如你也许想把小文件放在一个缓存中，大文件放在另外一个文件中，这样小文件就不会因大文件的频繁变动而被从缓存中移除。

至于什么是小文件，这个由应用来区分，在[创建image request](image-requests.html), 设置 [ImageType](../javadoc/reference/com/facebook/imagepipeline/request/ImageRequest.ImageType.html) 即可:

```java
ImageRequest request = ImageRequest.newBuilderWithSourceUri(uri)
    .setImageType(ImageType.SMALL)
```

如果你仅仅需要一个缓存，那么不调用`setSmallImageDiskCacheConfig`即可。Image pipeline 默认会使用同一个缓存，同时`ImageType`也会被忽略。

### 内存用量的缩减

在 [配置Image pipeline](configure-image-pipeline.html) 时，我们可以指定每个缓存最大的内存用量。但是有时我们可能会想缩小内存用量。比如应用中有其他数据需要占用内存，不得不把图片缓存清除或者减小
或者我们想检查看看手机是否已经内存不够了。

Fresco的缓存实现了[DiskTrimmable](../javadoc/reference/com/facebook/common/disk/DiskTrimmable.html) 或者 [MemoryTrimmable](../javadoc/reference/com/facebook/common/memory/MemoryTrimmable.html) 接口。这两个接口负责从各自的缓存中移除内容。

在应用中，可以给Image pipeline配置上实现了[DiskTrimmableRegistry](../javadoc/reference/com/facebook/common/disk/DiskTrimmableRegistry.html) 和 [MemoryTrimmableRegistry](../javadoc/reference/com/facebook/common/memory/MemoryTrimmableRegistry.html) 接口的对象。

实现了这两个接口的对象保持着一个列表，列表中的各个元素在内存不够时，缩减各自的内存用量。
