---
id: modifying-image
title: Modifying the Image
layout: docs
permalink: /docs/modifying-image.html
prev: resizing-rotating.html
next: image-requests.html
---

#### Motivation

Sometimes the image downloaded from the server, or fetched from local storage, is not exactly what you want to display on the screen. If you want to apply custom code to the image in-place, use a [Postprocessor](../javadoc/reference/com/facebook/imagepipeline/request/Postprocessor.html).

#### Example

The following example applies a red mesh to the image:

```java
Uri uri;
Postprocessor redMeshPostprocessor = new Postprocessor() { 
  @Override
  public String getName() {
    return "redMeshPostprocessor";
  }
  
  @Override
  public void process(Bitmap bitmap) {
    for (int x = 0; x < bitmap.getWidth(); x+=2) {
      for (int y = 0; y < bitmap.getHeight(); y+=2) {
        bitmap.setPixel(x, y, Color.RED);
      }
    }
  }
}

ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setPostprocessor(redMeshPostprocessor)
    .build();
    
PipelineDraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getOldController())
    // other setters as you need
    .build();
mSimpleDraweeView.setController(controller);
```

#### Things to Know

The image is copied before it enters your postprocessor. The copy of the image in cache is *not* affected by any changes you make in your postprocessor. On Android 4.x and lower, the copy is stored outside the Java heap, just as the original image was.

If you show the same image repeatedly, you must specify the postprocessor each time it is requested. You are free to use different postprocessors on different requests for the same image.

#### Repeated Postprocessors

What if you want to post-process the same image more than once? No problem at all. Just subclass [BaseRepeatedPostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BaseRepatedPostprocessor.html). This class has a method `update` which can be invoked at any time to run the postprocessor again.

The example below allows you to change the color of the mesh at any time.

```java
public class MeshPostprocessor extends BaseRepeatedPostprocessor { 
  private int mColor = Color.TRANSPARENT;

  public void setColor(int color) {
    mColor = color;
    update();
  }
  
  @Override
  public String getName() {
    return "meshPostprocessor";
  }
  
  @Override
  public void process(Bitmap bitmap) {
    for (int x = 0; x < bitmap.getWidth(); x+=2) {
      for (int y = 0; y < bitmap.getHeight(); y+=2) {
        bitmap.setPixel(x, y, mColor);
      }
    }
  }
}
MeshPostprocessor meshPostprocessor = new MeshPostprocessor();

/// setPostprocessor as in above example

meshPostprocessor.setColor(Color.RED);
meshPostprocessor.setColor(Color.BLUE);
```

You should have still have one `Postprocessor` instance per image request, as internally the class is stateful.
