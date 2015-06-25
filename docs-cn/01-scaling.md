---
id: scaling
title: 缩放
layout: docs-cn
permalink: /docs-cn/scaling.html
prev: drawee-components.html
next: rounded-corners-and-circles.html
---
对于 Drawee 的[各种效果配置](drawee-components.html)，其中一些是支持缩放类型的。

### 可用的缩放类型

| 类型 | 描述 |
| --------- | ----------- |
| center | 居中，无缩放 |
| centerCrop | 保持宽高比缩小或放大，使得两边都大于或等于显示边界。居中显示。|
| [focusCrop](#focusCrop) | 同centerCrop, 但居中点不是中点，而是指定的某个点|
| centerInside | 使两边都在显示边界内，居中显示。<br/>如果图尺寸大于显示边界，则保持长宽比缩小图片。|
| fitCenter | 保持宽高比，缩小或者放大，使得图片完全显示在显示边界内。居中显示|
| fitStart | 同上。但不居中，和显示边界左上对齐|
| fitEnd | 同fitCenter， 但不居中，和显示边界右下对齐|
| fitXY | 不保存宽高比，填充满显示边界|
| [none](#none) | 如要使用tile mode显示, 需要设置为none|

这些缩放类型和Android [ImageView](http://developer.android.com/reference/android/widget/ImageView.ScaleType.html) 支持的缩放类型几乎一样.

唯一不支持的缩放类型是`matrix.` Fresco 提供了`focusCrop` 作为补充。通常这个缩放效果更佳。

### focusCrop

`centerCrop`缩放模式会保持长宽比，缩放图片，填充满显示边界，居中显示。这个缩放模式在通常情况下很有用。

但是对于人脸等图片时，一味地居中显示，这个模式可能会裁剪掉一些有用的信息。

以人脸图片为例，借助一些类库，我们可以识别出人脸所在位置。如果可以设置以人脸位置居中裁剪显示，那么效果会好很多。

Fresco的focusCrop缩放模式正是为此而设计。只要提供一个居中聚焦点，显示时就会**尽量**以此点为中心。

居中点是以相对方式给出的，比如(0.5f, 0.5f)就是居中显示，(0f, 0f)就是左上对齐显示。

如果要使用此缩放模式，首先指定缩放模式。在XML:

```xml
  fresco:actualImageScaleType="focusCrop"
```

在Java代码中

```java
PointF focusPoint;
// your app populates the focus point
mSimpleDraweeView
    .getHierarchy()
    .setActualImageFocusPoint(focusPoint);
```

### none

如果你要使用tile mode进行显示，那么需要将scale type 设置为none.
