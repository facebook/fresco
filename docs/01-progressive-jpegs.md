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

This is only supported for URIs found over the network.

The placeholder is shown until enough scans have arrived to meet the *good enough* threshold. This is configurable.

#### Initialization

When you [configure](configure-image-pipeline.html) the image pipeline, you must pass in an instance of [ProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/ProgressiveJpegConfig.html).

This does two things. You should define one method that, given a scan number, returns the number of the next one to decode. The other decides what scan number is 'good enough' to display on screen.

This example will decode no more than every other scan of the image, using less CPU than decoding every scan.
Note: scan with the bigger number than nextScanNumberToDecode might be decoded. In this eample, if the `getNextScanNumberToDecode` method gets called after the following scans were fetched: `1`, `4`, `5`, `10`, only scans `4` and `10` are going to be decoded. Since no scans were decoded at the beginning, the first scan that is allowed to be decoded is `0 + 2 = 2`. Scan number `1` doesn't meet the criteria and only when the next scan greater than or equal to `2` arrives (in this case `4`), the first decode will happen. Similarly, the scan number 5 gets ignored, and the first scan greater than or equal to `4 + 2 = 6` is going to be decoded next (in this case `10`).

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

Instead of implementing this interface yourself, you can also instantiate the [SimpleProgressiveJpegConfig](../javadoc/reference/com/facebook/imagepipeline/decoder/SimpleProgressiveJpegConfig.html) class.

#### At Request Time

Currently, you must explicitly request progressive rendering for each image:

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

We hope to add support for using progressive images with `setImageURI` in a future release.
