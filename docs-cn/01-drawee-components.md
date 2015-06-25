---
id: drawee-components
title: Drawee的各种效果配置
layout: docs-cn
permalink: /docs-cn/drawee-components.html
prev: using-drawees-code.html
next: scaling.html
---

## 内容导航

* [定义](#Definitions)
* [设置要加载的图片](#Actual)
* [占位图](#Placeholder)
* [加载失败时的占位图](#Failure)
* [点击重新加载](#Retry)
* [显示一个进度条](#ProgressBar)
* [Backgrounds](#Backgrounds)
* [Overlays](#Overlays)
* [Pressed State Overlay](#PressedStateOverlay)

## <a name='Definitions'></a>定义

本页说明如何设置实现不同的图片呈现效果。

除了要加载的图片，其他各个设置都可以在xml中指定。在xml中指定的时候，可以是drawable/下的资源，也可以颜色。

在Java 代码中也可以指定。如果需要 [通过程序设定](using-drawees-code.html) 的话会接触到这个类: [GenericDraweeHierarchyBuilder](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchyBuilder.html)

通过代码设置是，设置的值可以是资源id，也可以是Drawable的子类。

创建完[GenericDraweeHierarchy](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchy.html)之后，也可以通过该类的相关方法，重新设置一些效果。

大多数的用户呈现不同效果的drawables都是可以[缩放的](scaling.html).

## <a name='Actual'></a>设置要加载的图

除了需要加载的图片是真正必须的，其他的都是可选的。如前所述，图片可以来自多个地方。

所需加载的图片实际是DraweeController的一个属性，而不是DraweeHierarchy的属性。

可使用`setImageURI`方法或者[通过设置DraweeController](using-controllerbuilder.html) 来进行设置。

对于要加载的图片，除了可以设置缩放类型外，DraweeHierarchy 还公开出一些其他方法用来控制显示效果:

* focus point (居中焦点, 用于[focusCrop缩放模式](scaling.html#FocusCrop))
* color filter

默认的缩放类型是: `centerCrop`

## <a name="Placeholder"></a>占位图(Placeholder)

在调用`setController` 或者 `setImageURI` 之后，占位图开始显示，直到图片加载完成。

对于渐进式格式的JPEG图片，占位图会显示直到满足已加载的图片解析度到达设定值。

XML 中属性值: `placeholderImage`  
Hierarchy builder中的方法: `setPlaceholderImage`  
Hierarchy method: `setPlaceholderImage`  
默认值: a transparent [ColorDrawable](http://developer.android.com/reference/android/graphics/drawable/ColorDrawable.html)  
默认缩放类型: `centerInside`  

## <a name='Failure' ></a>设置加载失败占位图

如果URI是无效的，或者下载过程中网络不可用，将会导致加载失败。当加载图片出错时，你可以设置一个出错提示图片。

XML 中属性值: `failureImage`  
Hierarchy builder中的方法: `setFailureImage`  
默认值: The placeholder image  
默认缩放类型: `centerInside`

## <a name='Retry'></a>点击重新加载图

在加载失败时，可以设置点击重新加载。这时提供一个图片，加载失败时，会显示这个图片（而不是失败提示图片），提示用户点击重试。

在[ControllerBuilder](using-controllerbuilder.html) 中如下设置:

```java
.setTapToRetryEnabled(true)
```

加载失败时，image pipeline 会重试四次；如果还是加载失败，则显示加载失败提示图片。

XML 中属性值: `retryImage`  
Hierarchy builder中的方法: `setRetryImage`  
默认值: The placeholder image
默认缩放类型: `centerInside`

## <a name="ProgressBar"></a>显示一个进度条

设置一个进度条图片，提示用户正在加载。目前，进度条仅仅是提示正在loading，和加载进度无关。

XML 中属性值: `progressBarImage`  
Hierarchy builder中的方法: `setProgressBarImage`  
默认值: None
默认缩放类型: `centerInside`

## <a name='Backgrounds'></a>背景

背景图会最先绘制，在XML中只可以指定一个背景图，但是在JAVA代码中，可以指定多个背景图。

当指定一个背景图列表的时候，列表中的第一项会被首先绘制，绘制在最下层，然后依次往上绘制。

背景图片不支持缩放类型，会被强制到`Drawee`尺寸大小。

XML 中属性值: `backgroundImage`  
Hierarchy builder中的方法: `setBackground,` `setBackgrounds`
默认值: None
默认缩放类型: N/A

## <a name='Overlays'></a>设置叠加图(Overlay)

叠加图会最后被绘制。

和背景图一样，XML中只可以指定一个，如果想指定多个，可以通过JAVA代码实现。

当指定的叠加图是一个列表的时候，列表第一个元素会被先绘制，最后一个元素最后被绘制到最上层。

同样的，不支持各种缩放类型。

XML 中属性值: `overlayImage`  
Hierarchy builder中的方法: `setOverlay,` `setOverlays`
默认值: None
默认缩放类型: N/A

## <a name="PressedStateOverlay"></a>设置按压状态下的叠加图

同样不支持缩放，用户按压DraweeView时呈现。

XML 中属性值: `pressedStateOverlayImage`  
Hierarchy builder中的方法: `setPressedStateOverlay`
默认值: None
默认缩放类型: N/A
