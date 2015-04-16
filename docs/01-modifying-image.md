---
id: modifying-image
title: Modifying the Image
layout: docs
permalink: /docs/modifying-image.html
prev: resizing-rotating.html
next: image-requests.html
---

#### Motivation

Sometimes the image downloaded from the server, or fetched from local storage, is not exactly what you want to display on the screen. If you want to apply custom code to the image in-place, use a Postprocessor. The best way to do this is to subclass [BasePostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BasePostprocessor.html).

#### Example

The following example applies a red mesh to the image:

```java
Uri uri;
Postprocessor redMeshPostprocessor = new BasePostprocessor() { 
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

Postprocessors are not currently supported for [animated](animations.html) images.

#### Copying the bitmap

There may be cases where an in-place post-process is not possible. If that is the case, `BasePostprocessor` has a second `process` method that takes two arguments. This example horizontally flips the bitmap:

```java
@Override
public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
  for (int x = 0; x < destBitmap.getWidth(); x++) {
    for (int y = 0; y < destBitmap.getHeight(); y++) {
      destBitmap.setPixel(destBitmap.getWidth() - x, y, sourceBitmap.getPixel(x, y));
    }
  }
}
```

The destination and source bitmaps are the same size. 

* Do **not** modify the source Bitmap. In future releases this will throw an exception.
* Do **not** keep a reference to either bitmap. Both have their memory managed by the image pipeline. The destBitmap will end up in your Drawee or DataSource normally.

#### Copying into a different size

If the postprocessed image needs to be a different size from the original, we have a third `process` method. Here, you can make use of our [PlatformBitmapFactory](../javadoc/reference/com/facebook/imagepipeline/bitmaps/PlatformBitmapFactory.html) class to safely create a bitmap, of a size you specify, off the Java heap.

This example samples every other pixel into a quarter-size bitmap:

```java
@Override
public CloseableReference<Bitmap> process(
    Bitmap sourceBitmap,
    PlatformBitmapFactory bitmapFactory) {
  CloseableReference<Bitmap> bitmapRef = bitmapFactory.createBitmap(
      sourceBitmap.getWidth() / 2,
      sourceBitmap.getHeight() / 2);
  try {
    Bitmap destBitmap = bitmapRef.get();
	 for (int x = 0; x < destBitmap.getWidth(); x+=2) {
	   for (int y = 0; y < destBitmap.getHeight(); y+=2) {
	     destBitmap.setPixel(sourceBitmap.getPixel(x, y));
	   }
	 }
	 return CloseableReference.cloneOrNull(bitmapRef);
  } finally {
    CloseableReference.closeSafely(bitmapRef);
  }	
}
```

You must follow the rules for [closeable references](closeable-references.html).

Do **not** use the Android `Bitmap.createBitmap` method, which creates a bitmap on the Java heap.

#### Which to override?

Do not override more than one of the three `process` methods. Doing so can produce unpredictable results.

#### Repeated Postprocessors

What if you want to post-process the same image more than once? No problem at all. Just subclass [BaseRepeatedPostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BaseRepeatedPostProcessor.html). This class has an `update` method which can be invoked at any time to run the postprocessor again.

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
