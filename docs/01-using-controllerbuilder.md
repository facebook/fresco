---
id: using-controllerbuilder
title: Using the ControllerBuilder
layout: docs
permalink: /docs/using-controllerbuilder.html
prev: rounded-corners-and-circles.html
next: progressive-jpegs.html
---

`SimpleDraweeView` has two methods for specifying an image. The easy way is to just call `setImageURI.` 

If you want more control over how the Drawee displays your image, you can use a [DraweeController](concepts.html). This page explains how to build and use one.

### Building a DraweeController

Then pass the image request to a [PipelineDraweeControllerBuilder](../javadoc/reference/com/facebook/drawee/backends/pipeline/PipelineDraweeControllerBuilder.html). You then specify additional options for the controller:

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

You should always call `setOldController` when building a new controller. This prevents an unneeded memory allocation.

More details:

* [Controller Listeners](listening-download-events.html)

### <a name="ImageRequest"></a>Customizing the ImageRequest

For still more advanced usage, you might need to send an [ImageRequest](../javadoc/reference/com/facebook/imagepipeline/request/ImageRequest.html) to the pipeline, instead of merely a URI. An example of this is using a [postprocessor](modifying-image.html).

```java
Uri uri;
Postprocessor myPostprocessor = new Postprocessor() { ... }
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setPostprocessor(myPostprocessor)
    .build();
    
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    // other setters as you need
    .build();
```

More details:

* [Postprocessors](modifying-image.html)
* [Requesting Multiple Images](requesting-multiple-images.html)
* [Resizing and Rotating](resizing-rotating.html)
