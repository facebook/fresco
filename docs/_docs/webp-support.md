---
docid: webp-support
title: WebP Images
layout: docs
permalink: /docs/webp-support.html
---

[WebP](https://en.wikipedia.org/wiki/WebP) is an image format that supports lossy and lossless compressions. Furthermore, it allows for transparency and animations.

### Support on Android

Android added WebP support in version 4.0 and improved it in 4.2.1:

* 4.0+ (Ice Cream Sandwich) have basic webp support
* 4.2.1+ (Jelly Beam MR1) have support for transparency and lossless WebP

By adding the Fresco webpsupport module, apps can display all kinds of WebP images on all versions of Android:

|  Configuration         | Basic WebP  | Lossless or Transparent WebP | Animated WebP  |
|---                     |:-:          |:-:                           |:-:             |
| OS < 4.0               |             |                              |                |
| OS >= 4.0              | ✓           |                              |                |
| OS >= 4.2.1            | ✓           | ✓                            |                |
| Any OS + webpsupport   | ✓           | ✓                            |                |
| Any OS + animated-webp | ✓           | (✓ if webpsupport or OS >= 4.2.1)           |  ✓             |

### Adding Support for Static WebP images on Older Versions

The only thing you need to do is add the `webpsupport` library to your dependencies. This adds support for all types of non-animated WebP images. E.g. you can use it to display transparent WebP images on Gingerbread.

```groovy
dependencies {
  // ... your app's other dependencies
  implementation 'com.facebook.fresco:webpsupport:{{site.current_version}}'
}
```

### Animated WebP

In order to display animated WebP images, you have to add the following dependencies:

```groovy
dependencies {
  // ... your app's other dependencies
  implementation 'com.facebook.fresco:animated-webp:{{site.current_version}}'
  implementation 'com.facebook.fresco:webpsupport:{{site.current_version}}'
}
```

You can then load the animated WebP images like any other URI. In order to auto-start the animation, you can set `setAutoPlayAnimations(true)` on the `DraweeController`:

```java
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri("http://example.org/somefolder/animated.webp")
    .setAutoPlayAnimations(true)
    .build();
mSimpleDraweeView.setController(controller);
```

### Full Sample

For the full sample see the `ImageFormatWebpFragment` in the showcase app: [ImageFormatWebpFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/imageformat/webp/ImageFormatWebpFragment.java)

![Showcase app with a notification](/static/images/docs/03-webp-support-sample.png)
