---
id: listening-download-events
title: Listening to Download Events
layout: docs
permalink: /docs/listening-download-events.html
prev: requesting-multiple-images.html
next: resizing-rotating.html
---

#### Motivation

Your app may want to execute actions of its own when an image arrives - perhaps make another view visible, or show a caption. You may also want to do something in case of a network failure, like showing an error message to the user. 

Loading images is, of course, asynchronous. So you need some way of listening to events posted by the DraweeController. The mechanism for doing this is a controller listener.

*Note: this does not allow you to modify the image itself. To do that, use a [Postprocessor](modifying-image.html).*

#### Usage

To use it, you merely define an instance of the `ControllerListener` interface. We recommend subclassing `BaseControllerListener:`

```java
ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable ImageInfo imageInfo,
        @Nullable Animatable anim) {
      if (imageInfo == null) {
        return;
      }
      QualityInfo qualityInfo = imageInfo.getQualityInfo();
      FLog.d("Final image received! " + 
          "Size %d x %d",
          "Quality level %d, good enough: %s, full quality: %s",
          imageInfo.getWidth(),
          imageInfo.getHeight(),
          qualityInfo.getQuality(),
          qualityInfo.isOfGoodEnoughQuality(),
          qualityInfo.isOfFullQuality());
    }
     
    @Override 
    public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
      FLog.d("Intermediate image received");
    }

    @Override
    public void onFailure(String id, Throwable throwable) {
      FLog.e(getClass(), throwable, "Error loading %s", id)
    }
};

Uri uri;
DraweeController controller = Fresco.newControllerBuilder()
    .setControllerListener(controllerListener)
    .setUri(uri);
    // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

`onFinalImageSet` or `onFailure` is called for all image loads. 

If [progressive decoding](progressive-jpegs.html) is enabled, and the image supports it, `onIntermediateImageSet` is called in response to each scan that gets decoded. Which scans get decoded is determined by your [configuration](progressive-jpegs.html).
