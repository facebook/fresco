---
id: resizing-rotating
title: 缩放和旋转图片
layout: docs-cn
permalink: /docs-cn/resizing-rotating.html
prev: listening-download-events.html
next: modifying-image.html
---

使用这个功能需要直接[创建 image request](using-controllerbuilder.html#ImageRequest)。

## 缩放图片

### 什么时候该修改图片尺寸

一般地，当所要显示的图片和显示区域大小不一致时，会按以下方式进行处理。

1. 从服务器下载小一些的图片
2. 显示时缩放图片
3. 调整图片尺寸大小

对于一个图片，如果服务器支持不同尺寸的缩略图，那么每次下载都选择尺寸最匹配的图片，这个不仅节省数据流量也节约本地储存和CPU。

如果服务器不支持，或者处理本地图片的话，第二个选择是[使用缩放类型](scaling.html)。缩放是用Androi内置的功能使图像和显示边界相符。在4.0之后，支持硬件加速。这在大部分情况下是最快，同时也是最高效的显示一张和显示边界大小相符的图片的方式。首先指定`layout_width`和`layout_width`为指定值，然后指定[缩放类型](scaling.html)

但当所要显示的图片比显示区域大许多的时候，不推荐这样做，缩放过程会导致大量的内存消耗。

这时，需要改变图片尺寸。

### 修改图片尺寸

调整大小并不是修改原来的文件，而是在解码之前，在native内存中修改。

这个缩放方法，比Android内置的缩放范围更大。Android相机生成的照片一般尺寸都很大，需要调整大小之后才能被显示。

目前，仅仅支持JPEG格式的图片，同时，大部分的Android系统相机图片都是JPEG的。

如果要修改图片尺寸，创建`ImageRequest`时，提供一个 [ResizeOptions](../javadoc/reference/com/facebook/imagepipeline/common/ResizeOptions.html):

```java
Uri uri = "file:///mnt/sdcard/MyApp/myfile.jpg";
int width = 50, height = 50;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
PipelineDraweeController controller = Fresco.newDraweeControllerBuilder()
    .setOldController(mDraweeView.getController())
    .setImageRequest(request)
    .build();
mSimpleDraweeView.setController(controller);
```

## <a name="rotate"></a>自动旋转

如果看到的图片是侧着的，用户是难受的。许多设备会在JPEG文件的metadata中记录下照片的方向。如果你想图片呈现的方向和设备屏幕的方向一致，你可以简单地这样做到:

```java
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .build();
// as above
```
