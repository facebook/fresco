---
id: progressive-jpegs
title: 渐进式JPEG图
layout: docs-cn
permalink: /docs-cn/progressive-jpegs.html
prev: using-controllerbuilder.html
next: animations.html
---

*注意: 本页提及的API仅是初步设计，后续可能变动*

Fresco 支持渐进式的网络JPEG图。在开始加载之后，图会从模糊到清晰渐渐呈现。

你可以设置一个清晰度标准，在未达到这个清晰度之前，会一直显示占位图。

渐进式JPEG图仅仅支持网络图。

#### 初始化

[配置Image pipeline时](configure-image-pipeline.html) 需要传递一个 [ProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/ProgressiveJpegConfig.html). 的实例。

这个实例需要完成两个事情:
1.  返回下一个需要解码的扫描次数
2.  确定多少个扫描次数之后的图片才能开始显示。

下面的实例中，为了实现节省CPU，并不是每个扫描都进行解码。

注意:

* 每次解码完之后，调用`getNextScanNumberToDecode`, 等待扫描值大于返回值，才有可能进行解码。

假设，随着下载的进行，下载完的扫描序列如下: `1, 4, 5, 10`。那么：

1.  首次调用`getNextScanNumberToDecode`返回为2， 因为初始时，解码的扫描数为0。
2.  那么1将不会解码，下载完成4个扫描时，解码一次。下个解码为扫描数为6
3.  5不会解码，10才会解码

```java
ProgressiveJpegConfig pjpegConfig = new ProgressiveJpegConfig() {
  @Override
  public int getNextScanNumberToDecode(int scanNumber) {
    return scanNumber + 2;
  }

  public QualityInfo getQualityInfo(int scanNumber) {
    boolean isGoodEnough = (scanNumber >= 5);
    return ImmutableQualityInfo.of(scanNumber, isGoodEnough, false);
  }
}

ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
    .setProgressiveJpegConfig(pjpeg)
    .build();
```

除了自己实现ProgressiveJpegConfig， 也可以直接使用[SimpleProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/SimpleProgressiveJpegConfig.html).

#### At Request Time

目前，我们必须显式地在加载时，允许渐进式JPEG图片加载。

```java
Uri uri;
ImageRequest request = ImageRequestBuilder
    .newBuilderWithSource(uri)
    .setProgressiveRenderingEnabled(true)
    .build();
PipelineDraweeController controller = Fresco.newControllerBuilder()
    .setImageRequest(requests)
    .setOldController(mSimpleDraweeView.getController())
    .build();

mSimpleDraweeView.setController(controller);
```

我们希望在后续的版本中，在`setImageURI`方法中可以直接支持渐进式图片加载。
