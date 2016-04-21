---
docid: progressive-jpegs
title: Progressive JPEGs
layout: docs
permalink: /docs/progressive-jpegs.html
prev: using-controllerbuilder.html
next: animations.html
---

Fresco supports the streaming of progressive JPEG images over the network.

Scans of the image will be shown in the view as you download them. Users will see the quality of the image start out low and gradually become clearer.

This is only supported for network images. Local images are decoded at once, so no need for progressiveness. Also, keep in mind that not all JPEG images are encoded in progressive format, and for those that are not, it is not possible to display them progressively.

#### Building the image request

Currently, you must explicitly request progressive rendering while building the image request:

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setProgressiveRenderingEnabled(true)
    .build();
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```

We hope to add support for using progressive images with `setImageURI` in a future release.
