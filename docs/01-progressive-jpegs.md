---
id: progressive-jpegs
title: Progressive JPEGs
layout: docs
permalink: /docs/progressive-jpegs.html
prev: using-controllerbuilder.html
next: animations.html
---

*Note: the API in this page is still preliminary and subject to change.*

Fresco supports the streaming of progressive JPEG images over the network.

Scans of the image will be shown in the view as you download them. Users will see the quality of the image start out low and gradually become clearer.

This is only supported for the network images. Local images are decoded at once.

#### Initialization

When you [configure](configure-image-pipeline.html) the image pipeline, you must pass in an instance of [ProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/ProgressiveJpegConfig.html). We plan to remove this requirement.

This example will decode no more than every other scan of the image, using less CPU than decoding every scan.

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
    .setProgressiveJpegConfig(pjpegConfig)
    .build();
```

Instead of implementing this interface yourself, you can also instantiate the [SimpleProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/SimpleProgressiveJpegConfig.html) class.

#### At Request Time

Currently, you must explicitly request progressive rendering while building the image request:

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setProgressiveRenderingEnabled(true)
    .build();
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

We hope to add support for using progressive images with `setImageURI` in a future release.
