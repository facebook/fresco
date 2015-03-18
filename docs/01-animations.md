---
id: animations
title: Animated Images
layout: docs
permalink: /docs/animations.html
prev: progressive-jpegs.html
next: requesting-multiple-images.html
---

Fresco supports animated GIF and WebP images.

We support WebP animations, even in the extended WebP format, on versions of Android going back to 2.3, even those that don't have built-in native support.

### Playing animations automatically

If you want your animated image to start playing automatically when it comes on-screen, and stop when it goes off, just say so in your [image request](image-requests.html):

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setAutoPlayAnimation(true)
    . // other setters
    .build();
    
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    . // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

### Playing animations manually

You may prefer to directly control the animation in your own code. In that case you'll need to listen for when the image has loaded, so it's even possible to do that. 

```java
ControllerListener controllerListener = new BaseControllerListener() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable ImageInfo imageInfo,
        @Nullable Animatable anim) {
    if (anim != null) {
      // app-specific logic to enable animation starting
    }
};

Uri uri;
PipelineDraweeController controller = Fresco.newControllerBuilder()
    .setControllerListener(controllerListener)
    .setUri(uri);
    // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

The controller exposes an instance of the [Animatable](http://developer.android.com/reference/android/graphics/drawable/Animatable.html) interface. If non-null, you can drive your animation with it:

```java
Animatable animation = mSimpleDraweeView.getController().getAnimatable();
if (animation != null) {
  animation.start();
  // later
  animation.stop();
}
```

