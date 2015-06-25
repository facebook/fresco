---
id: gotchas
title: 一些陷阱
layout: docs-cn
permalink: /docs-cn/gotchas.html
prev: writing-custom-views.html
next: intro-image-pipeline.html
---

#### 不要向下转换

不要试图把Fresco返回的一些对象进行向下转化，这也许会带来一些对象操作上的便利，但是也许在后续的版本中，你会遇到一些因为向下转换特性丢失导致的难以处理的问题。

#### 不要使用getTopLevelDrawable

`DraweeHierarchy.getTopLevelDrawable()` **仅仅** 应该在DraweeViews中用，除了定义View中，其他应用代码建议连碰都不要碰这个。

在自定义View中，也千万不要将返回值向下转换，也许下个版本，我们会更改这个返回值类型。

#### 不要复用 DraweeHierarchies

永远不要吧`DraweeHierarchy` 通过 ```DraweeView.setHierarchy``` 设置给不同的View。DraweeHierarchy是由一系列Drawable组成的。在Android中, Drawable不能被多个View共享。

#### 不要在多个DraweeHierarchy中使用同一个Drawable

原因同上。当时可以使用不同的资源ID。Android实际会创建不同的Drawable。

#### 不要直接给 `DraweeView` 设置图片。

目前 ```DraweeView``` 直接继承于ImageView，因此它有 `setImageBitmap`,
`setImageDrawable`  等方法。

如果利用这些方法，直接设置一个图片。内部的```DraweeHierarchy```就会丢失，也就无法取到image
pipeline 的任何图像了。

#### 使用DraweeView时，请不要使用任何ImageView的属性

在后续的版本中，DraweeView会直接从View派生。任何属于ImageView但是不属于View的方法都会被移除。
