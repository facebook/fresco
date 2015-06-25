---
id: requesting-multiple-images
title: 多图请求及图片复用
layout: docs-cn
permalink: /docs-cn/requesting-multiple-images.html
prev: animations.html
next: listening-download-events.html
---

多图请求需 [自定义ImageRequest](using-controllerbuilder.html).

### 先显示低分辨率的图，然后是高分辨率的图

如果你要显示一张高分辨率的图，但是这张图下载比较耗时。你可以在下载前，先提供一张很快能下载完的小缩略图。这比一直显示占位图，用户体验会好很多。

这时，你可以设置两个图片的URI，一个是低分辨率的缩略图，一个是高分辨率的图。

```java
Uri lowResUri, highResUri;
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setLowResImageRequest(ImageRequest.fromUri(lowResUri))
    .setImageRequest(ImageRequest.fromUri(highResUri))
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

### 缩略图预览

*本功能仅支持本地URI，并且是JPEG图片格式*

如果本地JPEG图，有EXIF的缩略图，image pipeline 会立刻返回一个缩略图。完整的清晰大图，在decode完之后再显示。

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setLocalThumbnailPreviewsEnabled(true)
    .build();

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```


### 本地图片复用

大部分的时候，一个图片可能会对应有多个URI，比如:

* 拍照上传。本地图片较大，上传的图片较小。上传完成之后的图片，有一个url，如果要加载这个url，可直接加载本地图片。
* 本地已经有600x600尺寸的大图了，需要显示100x100的小图

对于一个URI，image pipeline 会依次检查内存，磁盘，如果没有从网络下载。

而对于一个图片的多个URI，image pipeline 会先检查他们是否在内存中。如果没有任何一个是在内存中的，会检查是否在本地存储中。如果也没有，才会执行网络下载。

但凡有任何一个检查发现在内存或者在本地存储中，都会进行复用。列表顺序就是要显示的图片的优先顺序。

使用时，创建一个image request 列表，然后传给ControllerBuilder:

```java
Uri uri1, uri2;
ImageRequest request = ImageRequest.fromUri(uri1);
ImageRequest request2 = ImageRequest.fromUri(uri2);
ImageRequest[] requests = { request1, request2 };

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setFirstAvailableImageRequests(requests)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

### 自定义 `DataSource Supplier`
为了更好的灵活性，你可以在创建Drawee controller时自定义DataSource Supplier。你可以以`FirstAvailiableDataSourceSupplier`,`IncreasingQualityDataSourceSupplier`为例自己实现DataSource Supplier或者以`AbstractDraweeControllerBuilder`为例将多个DataSource Supplier根据需求组合在一起。
