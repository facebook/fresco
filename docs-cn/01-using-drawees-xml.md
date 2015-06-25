---
id: using-drawees-xml
title: 在XML中使用Drawees
layout: docs-cn
permalink: /docs-cn/using-drawees-xml.html
prev: supported-uris.html
next: using-drawees-code.html
---

Drawees 具有极大的可定制性。

下面的例子给出了可以配置的各种选项：

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

#### 必须设置layout_width和layout_height

如果没有在XML中声明这两个属性，将无法正确加载图像。

#### wrap_content

*Drawees 不支持 `wrap_content` 属性。*

所下载的图像可能和占位图尺寸不一致，如果设置出错图或者重试图的话，这些图的尺寸也可能和所下载的图尺寸不一致。

如果大小不一致，图像下载完之后，假设如果是`wrap_content`，View将会重新layout，改变大小和位置。这将会导致界面跳跃。

#### 固定宽高比

只有希望显示的固定宽高比时，可以使用`wrap_content`。

如果希望显示的图片保持一定宽高比例，如果 4:3，则在XML中:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="wrap_content"
    <!-- other attributes -->
```

然后在代码中指定显示比例：


```java
mSimpleDraweeView.setAspectRatio(1.33f);
```
