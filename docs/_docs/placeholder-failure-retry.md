---
docid: placeholder-failure-retry
title: Placeholder, failure and retry images
layout: docs
permalink: /docs/placeholder-failure-retry.html
---

When you're loading network images things can go wrong, take a long time, or some images might not even be available at all. We've seen how to display [progress bars](progress-bars.html). On this page, we look at the other things that a `SimpleDraweeView` can display while the actual image is not available (yet, or at all). Note that all of these can have different [scale types](scaletypes.html), which you can customize.

### Placeholder Image

The placeholder image is displayed from before you've set a URI or a controller until it has finished loading (successfully or not).

#### XML

```xml
<com.facebook.drawee.view.SimpleDraweeView
  android:id="@+id/my_image_view"
  android:layout_width="20dp"
  android:layout_height="20dp"
  fresco:placeholderImage="@drawable/my_placeholder_drawable"
  />
```

#### Code

```java
mSimpleDraweeView.getHierarchy().setPlaceholderImage(placeholderImage);
```

### Failure Image

The failure image is displayed when a request has completed in error, either network-related (404, timeout) or image data-related (malformed image, unsupported format).

#### XML

```xml
<com.facebook.drawee.view.SimpleDraweeView
  android:id="@+id/my_image_view"
  android:layout_width="20dp"
  android:layout_height="20dp"
  fresco:failureImage="@drawable/my_failure_drawable"
  />
```

#### Code

```java
mSimpleDraweeView.getHierarchy().setFailureImage(failureImage);
```

### Retry Image

The retry image appears instead of the failure image. When the user taps on it, the request is retried up to four times, before the failure image is displayed. In order for the retry image to work, you need to enable support for it in your controller, which means setting up your image request like so:

```java
mSimpleDraweeView.setController(
    Fresco.newDraweeControllerBuilder()
        .setTapToRetryEnabled(true)
        .setUri(uri)
        .build());
```

#### XML

```xml
<com.facebook.drawee.view.SimpleDraweeView
  android:id="@+id/my_image_view"
  android:layout_width="20dp"
  android:layout_height="20dp"
  fresco:failureImage="@drawable/my_failure_drawable"
  />
```

#### Code

```java
simpleDraweeView.getHierarchy().setRetryImage(retryImage);
```

### Further Reading

Placeholder, failure and retry images are drawee *branches*. There are others than what is presented on this page, though these are the most commonly used ones. To read about all of the branches and how they work, check out [drawee branches](drawee-branches.html).

### Example

The Fresco showcase app has a [DraweeHierarchyFragment](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeHierarchyFragment.java) that demonstrates using placeholder, failure and retry images.

![Showcase app with placeholder, failure and retry images](/static/images/docs/01-placeholder-sample.png)
