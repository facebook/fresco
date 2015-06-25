---
id: rounded-corners-and-circles
title: 圆角和圆圈
layout: docs-cn
permalink: /docs-cn/rounded-corners-and-circles.html
prev: scaling.html
next: using-controllerbuilder.html
---

Drawee 轻松支持圆角显示，并且显示圆角时，并不复制和修改Bitmap对象，那样太耗费内存。

### 圆角

圆角实际有2中呈现方式:

1. 圆圈 - 设置`roundAsCircle`为true
2. 圆角 - 设置`roundedCornerRadius`

设置圆角时，支持4个角不同的半径。XML中无法配置，但可在Java代码中配置。

### 设置圆角

可使用以下两种方式:

1. 默认使用一个shader绘制圆角，但是仅仅占位图所要显示的图有圆角效果。失败示意图和重下载示意图无圆角效果。
2. 叠加一个`solid color`来绘制圆角。但是背景需要固定成指定的颜色。
    在XML中指定 `roundWithOverlayColor`, 或者通过调用`setOverlayColor`来完成此设定。

### XML中配置

`SimpleDraweeView` 支持如下几种圆角配置:

```xml
<com.facebook.drawee.view.SimpleDraweeView
   ...
   fresco:roundedCornerRadius="5dp"
   fresco:roundBottomLeft="false"
   fresco:roundBottomRight="false"
   fresco:roundWithOverlayColor="@color/blue"
   fresco:roundingBorderWidth="1dp"
   fresco:roundingBorderColor="@color/red"
```

### 代码中配置

在创建 [DraweeHierarchy](using-drawees-code.html) 时，可以给`GenericDraweeHierarchyBuilder`指定一个[RoundingParams](../javadoc/reference/com/facebook/drawee/generic/RoundingParams.html) 用来绘制圆角效果。


```java
RoundingParams roundingParams = RoundingParams.fromCornersRadius(7f);
roundingParams.setOverlayColor(R.color.green);
// 或用 fromCornersRadii 以及 asCircle 方法
genericDraweeHierarchyBuilder
    .setRoundingParams(roundingParams);
```

你也可以在运行时，改变圆角效果

```java
RoundingParams roundingParams =
    mSimpleDraweeView.getHierarchy().getRoundingParams();
roundingParams.setBorder(R.color.red, 1.0);
roundingParams.setRoundAsCircle(true);
mSimpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
```

> 在运行时，不能改变呈现方式: 原本是圆角，不能改为圆圈。
>
