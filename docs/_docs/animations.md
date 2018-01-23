---
docid: animations
title: Animated Images
layout: docs
permalink: /docs/animations.html
---

Fresco supports animated GIF and WebP images.

We support WebP animations, even in the extended WebP format, on versions of Android going back to 2.3, even those that don't have built-in native support.

For adding this optional modules in your build.gradle please visit [here](index.html):

### Playing animations automatically

If you want your animated image to start playing automatically when it comes on-screen, and stop when it goes off, just say so in your [image request](image-requests.html):

```java
Uri uri;
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setAutoPlayAnimations(true)
    . // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

### Playing animations manually

You may prefer to directly control the animation in your own code. In that case you'll need to listen for when the image has loaded, so it's even possible to do that.

```java
ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable ImageInfo imageInfo,
        @Nullable Animatable anim) {
        if (anim != null) {
          // app-specific logic to enable animation starting
          anim.start();
        }
    }
};

Uri uri;
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setControllerListener(controllerListener)
    // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

The controller exposes an instance of the [Animatable](http://developer.android.com/reference/android/graphics/drawable/Animatable.html) interface. If non-null, you can drive your animation with it:

```java
Animatable animatable = mSimpleDraweeView.getController().getAnimatable();
if (animatable != null) {
  animatable.start();
  // later
  animatable.stop();
}
```

### Limitations

Animations do not currently support [postprocessors](modifying-image.html).
