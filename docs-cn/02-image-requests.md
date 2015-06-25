---
id: image-requests
title: (图片请求)Image Requests
layout: docs-cn
permalink: /docs-cn/image-requests.html
prev: modifying-image.html
next: writing-custom-views.html
---

如果你需要的`ImageRequest`仅仅是一个URI，那么`ImageRequest.fromURI`就足够了，在[多图请求及图片复用](requesting-multiple-images.html)中，有这样的用法。

否则，你需要`ImageRequestBuilder`来做更多的事情。

```java
Uri uri;

ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
    .setBackgroundColor(Color.GREEN)
    .build();

ImageRequest request = ImageRequestBuilder
    .newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .setLocalThumbnailPreviewsEnabled(true)
    .setLowestPermittedRequestLevel(RequestLevel.FULL_FETCH)
    .setProgressiveRenderingEnabled(false)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
```

#### ImageRequest 的属性和成员

- `uri` - 唯一的必选的成员. 参考 [支持的URIs](supported-uris.html)
- `autoRotateEnabled` - 是否支持[自动旋转](resizing--rotating.html#rotate).
- `progressiveEnabled` - 是否支持[渐进式加载](progressive-jpegs.html).
- `postprocessor` - [后处理器(postprocess)](modifying-image.html).
- `resizeOptions` - 图片缩放选项，用前请先阅读[缩放和旋转](resizing-rotating.html).

#### 最低请求级别

Image pipeline 加载图片时有一套明确的[请求流程](intro-image-pipeline.html)

1. 检查内存缓存，有如，立刻返回。这个操作是实时的。
2. 检查未解码的图片缓存，如有，解码并返回。
3. 检查磁盘缓存，如果有加载，解码，返回。
4. 下载或者加载本地文件。调整大小和旋转（如有），解码并返回。对于网络图来说，这一套流程下来是最耗时的。

`setLowestPermittedRequestLevel`允许设置一个最低请求级别，请求级别和上面对应地有以下几个取值:

- `BITMAP_MEMORY_CACHE`
- `ENCODED_MEMORY_CACHE`
- `DISK_CACHE`
- `FULL_FETCH`

如果你需要立即取到一个图片，或者在相对比较短时间内取到图片，否则就不显示的情况下，这非常有用。
