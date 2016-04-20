---
docid: using-drawees-xml
title: Using Drawees in XML
layout: docs
permalink: /docs/using-drawees-xml.html
---

Drawees have very extensive customization facilities.
The best way to customize your Drawee is to do so in the XML.

Here is an example that sets nearly all possible options:

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
  fresco:roundWithOverlayColor="@color/corner_color"
  fresco:roundingBorderWidth="2dp"
  fresco:roundingBorderColor="@color/border_color"
/>
```

#### Height and width mandatory

You **must** declare both `android:layout_width` and `android:layout_height`. Without both of these two, the view will not be able to lay the image out correctly.

#### wrap_content

Drawees do not support the `wrap_content` value for the `layout_width` and `layout_height` attributes.

The reason for this is that the content's size changes. The size of your downloaded image can be different from your placeholder - and the failure and retry images, if any, can be still different.

Use of `wrap_content` would force Android to do another layout pass when your image comes in - and for the layout to change before users' eyes, creating a jarring effect. More information can be found [here](wrap-content.html#_).

#### Fixing the aspect ratio

This is the one time you should use `wrap_content.`

You can force a DraweeView to be laid out in a particular aspect ratio. If you want a width:height ratio of 4:3, for instance, do this:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="wrap_content"
    fresco:viewAspectRatio="1.33"
    <!-- other attributes -->
```

You can also specify your aspect ratio in Java:

```java
mSimpleDraweeView.setAspectRatio(1.33f);
```
