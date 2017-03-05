---
docid: using-other-image-loaders
title: Using Other Image Loaders
layout: docs
permalink: /docs/using-other-image-loaders.html
prev: using-other-network-layers.html
next: proguard.html
---

Drawee is not tied to a particular image loading mechanism and can be used with other image loaders.

However, some of its features are only available on the Fresco image pipeline. Any feature in the preceding pages that required using an [ImageRequest](image-requests.html) or [configuration](configure-image-pipeline.html) may not work with a different loader.

### Using Drawee with Volley ImageLoader

We have an backend for Drawee that allows Volley's [ImageLoader](https://developer.android.com/training/volley/request.html) to be used instead of Fresco's image pipeline.

We only recommend this for apps that already have a significant investment in Volley ImageLoader.

In order to use it, the `dependencies` section of your `build.gradle` file needs to be changed. Do **not** use the Gradle dependencies given on the [download](download-fresco.html) page. Use this instead:

```groovy
dependencies {
  // your project's other dependencies
  compile: "com.facebook.fresco:drawee-volley:{{site.current_version}}"
}
```

#### Initializing with Volley ImageLoader

Do not call `Fresco.initialize`. You must do yourself for Volley what it does with the image pipeline:

```java
Context context;
ImageLoader imageLoader; // build yourself
VolleyDraweeControllerBuilderSupplier mControllerBuilderSupplier
    = new VolleyDraweeControllerBuilderSupplier(context, imageLoader);
SimpleDraweeView.initialize(mControllerBuilderSupplier);
```

Do not let the `VolleyDraweeControllerBuilderSupplier` out of scope; you need it to build controllers, unless you always use `SimpleDraweeView.setImageURI.`

#### Using DraweeControllers with Volley ImageLoader

Instead of calling `Fresco.newControllerBuilder`, call

```java
VolleyController controller = mControllerBuilderSupplier
    .newControllerBuilder()
    . // setters
    .build();
mSimpleDraweeView.setController(controller);
```

### Using Drawee with other image loaders

No other Drawee backends have been built yet, though it is possible to do so using the [Volley example](https://github.com/facebook/fresco/tree/master/drawee-backends/drawee-volley/src/main/java/com/facebook/drawee/backends/volley) as a model.
