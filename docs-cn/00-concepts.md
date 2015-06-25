---
id: concepts
title: 关键概念
layout: docs-cn
permalink: /docs-cn/concepts.html
prev: index.html
next: supported-uris.html
---

## Drawees

Drawees 负责图片的呈现，包含几个组件，有点像MVC模式。

### DraweeView

继承于 [View](http://developer.android.com/reference/android/view/View.html), 负责图片的显示。

一般情况下，使用`SimpleDraweeView` 即可. 简单的用法，在这个页面：[开始使用](index.html) 。

它支持很多自定义效果，参见这里: [自定义显示效果](using-drawees-xml.html).

### DraweeHierarchy

DraweeHierarchy 用于组织和维护最终绘制和呈现的[Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html)对象，相当于MVC中的M。

如果你想在Java代码中自定义图片的展示，可以通过这类实现，具体的请参考这里: [在Java代码中自定义显示效果](using-drawees-code.html)

### DraweeController

`DraweeController` 负责和 image loader 交互（默认是Fresco中 image pipeline），可以创建一个这个类的实例，来实现对所要显示的图片做更多的控制。

### DraweeControllerBuilder

`DraweeControllers` 由 `DraweeControllerBuilder` 采用 Builder 模式创建，创建之后，不可修改。具体参见:  [使用ControllerBuilder](using-controllerbuilder.html)。

### Listeners

使用 ControllerListener 的一个场景就是设置一个 [Listener](listening-download-events.html)监听图片的下载。

## Image Pipeline

Fresco 的 Image Pipeline 负责图片的获取和管理。图片可以来自远程服务器，本地文件，或者Content Provider，本地资源。压缩后的文件缓存在本地存储中，Bitmap数据缓存在内存中。

在5.0系统以下，Image Pipeline 使用`pinned purgeables*将Bitmap数据避开Java堆内存，存在ashmem中。这要求图片不使用时，要显式地释放内存。

`SimpleDraweeView` 自动处理了这个释放过程，所以没有特殊情况，尽量使用`SimpleDraweeView`，在特殊的场合，如果有需要，也可以直接控制Image Pipeline。
