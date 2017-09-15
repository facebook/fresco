---
docid: using-simpledraweeview
title: Using SimpleDraweeView
layout: docs
permalink: /docs/using-simpledraweeview.html
redirect_from:
  - /docs/using-drawees-xml.html
  - /docs/using-drawees-code.html
---

When using Fresco, you will use `SimpleDraweeView` to display images. These can be used in XML layouts. The simplest usage example of `SimpleDraweeView` is:

```xml
<com.facebook.drawee.view.SimpleDraweeView
  android:id="@+id/my_image_view"
  android:layout_width="20dp"
  android:layout_height="20dp"
  />
```

**NOTE:** `SimpleDraweeView` does not support `wrap_content` for `layout_width` or `layout_height` attributes. More information can be found [here](faq.html). The only exception to this is when you are setting an aspect ratio, like so:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="wrap_content"
    fresco:viewAspectRatio="1.33"
    />
```

### Loading an image

The easiest way to load an image into a `SimpleDraweeView` is to call `setImageURI`:

```java
mSimpleDraweeView.setImageURI(uri);
```

That's it, you are now displaying images with Fresco!

### Advanced XML attributes

`SimpleDraweeView`, despite its name, supports a great deal of customization through XML attributes. The example below presents all of them:

```xml
<com.facebook.drawee.view.SimpleDraweeView
  android:id="@+id/my_image_view"
  android:layout_width="20dp"
  android:layout_height="20dp"
  fresco:fadeDuration="300"
  fresco:actualImageScaleType="focusCrop"
  fresco:placeholderImage="@color/wait_color"
  fresco:placeholderImageScaleType="fitCenter"
  fresco:failureImage="@drawable/error"
  fresco:failureImageScaleType="centerInside"
  fresco:retryImage="@drawable/retrying"
  fresco:retryImageScaleType="centerCrop"
  fresco:progressBarImage="@drawable/progress_bar"
  fresco:progressBarImageScaleType="centerInside"
  fresco:progressBarAutoRotateInterval="1000"
  fresco:backgroundImage="@color/blue"
  fresco:overlayImage="@drawable/watermark"
  fresco:pressedStateOverlayImage="@color/red"
  fresco:roundAsCircle="false"
  fresco:roundedCornerRadius="1dp"
  fresco:roundTopLeft="true"
  fresco:roundTopRight="false"
  fresco:roundBottomLeft="false"
  fresco:roundBottomRight="true"
  fresco:roundTopStart="false"
  fresco:roundTopEnd="false"
  fresco:roundBottomStart="false"
  fresco:roundBottomEnd="false"
  fresco:roundWithOverlayColor="@color/corner_color"
  fresco:roundingBorderWidth="2dp"
  fresco:roundingBorderColor="@color/border_color"
  />
```

### Customizing from code

Although it's generally recommended to set these options in XML, all of the attributes above can also be set from code. In order to do this, you will need to create a `DraweeHierarchy` before setting the image URI:

```java
GenericDraweeHierarchy hierarchy =
    GenericDraweeHierarchyBuilder.newInstance(getResources())
        .setActualImageColorFilter(colorFilter)
        .setActualImageFocusPoint(focusPoint)
        .setActualImageScaleType(scaleType)
        .setBackground(background)
        .setDesiredAspectRatio(desiredAspectRatio)
        .setFadeDuration(fadeDuration)
        .setFailureImage(failureImage)
        .setFailureImageScaleType(scaleType)
        .setOverlays(overlays)
        .setPlaceholderImage(placeholderImage)
        .setPlaceholderImageScaleType(scaleType)
        .setPressedStateOverlay(overlay)
        .setProgressBarImage(progressBarImage)
        .setProgressBarImageScaleType(scaleType)
        .setRetryImage(retryImage)
        .setRetryImageScaleType(scaleType)
        .setRoundingParams(roundingParams)
        .build();
mSimpleDraweeView.setHierarchy(hierarchy);
mSimpleDraweeView.setImageURI(uri);
```

**NOTE:** some of these options can be set on an existing hierarchy without having to build a new one. To do this, simply get the hierarchy from a `SimpleDraweeView` and call any of the setter methods on it, e.g.:

```java
mSimpleDraweeView.getHierarchy().setPlaceHolderImage(placeholderImage);
```

### Full Sample

For a full sample see the `DraweeSimpleFragment` in the showcase app: [DraweeSimpleFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeSimpleFragment.java)

![Showcase app with a scale type example](/static/images/docs/01-using-simpledraweeview-sample.png)
