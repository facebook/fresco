---
id: using-controllerbuilder
title: ControllerBuilder
layout: docs-cn
permalink: /docs-cn/using-controllerbuilder.html
prev: rounded-corners-and-circles.html
next: progressive-jpegs.html
---

`SimpleDraweeView` 有两个方法可以设置所要加载显示图片，简单的方法就是`setImageURI`。

如果你需要对加载显示的图片做更多的控制和定制，那就需要用到[DraweeController](concepts.html#DraweeController)，本页说明如何使用。

### DraweeController

首先，创建一个DraweeController, 然后传递图片加载请求给[PipelineDraweeControllerBuilder](../javadoc/reference/com/facebook/drawee/backends/pipeline/PipelineDraweeControllerBuilder.html).

随后，你可以控制controller的其他选项了:

```java
ControllerListener listener = new BaseControllerListener() {...}

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setTapToRetryEnabled(true)
    .setOldController(mSimpleDraweeView.getController())
    .setControllerListener(listener)
    .build();

mSimpleDraweeView.setController(controller);
```

在指定一个新的controller的时候，使用`setOldController`，这可节省不必要的内存分配。

更多细节:

* [Controller Listeners](listening-download-events.html)

### <a name="ImageRequest"></a>自定义图片加载请求

在更进一步的用法中，你需要给Image pipeline 发送一个ImageRequest。下面是一个图片加载后，[使用后处理器(postprocessor)](modifying-image.html) 进行图片后处理的例子.


```java
Uri uri;
Postprocessor myPostprocessor = new Postprocessor() { ... }
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setPostprocessor(myPostprocessor)
    .build();

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    // 其他设置
    .build();
```

更多细节:

* [后处理器(Postprocessors)](modifying-image.html)
* [加载多个图片](requesting-multiple-images.html)
* [缩放和旋转图片](resizing-rotating.html)
