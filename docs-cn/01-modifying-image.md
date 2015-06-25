---
id: modifying-image
title: 修改图片
layout: docs-cn
permalink: /docs-cn/modifying-image.html
prev: resizing-rotating.html
next: image-requests.html
---

有时，我们想对从服务器下载，或者本地的图片做些修改，比如在某个坐标统一加个网格什么的。这时使用[后处理器(Postprocessor)](../javadoc/reference/com/facebook/imagepipeline/request/Postprocessor.html)便可达到目的。

#### 例子:

给图片加个网格:

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

PipelineDraweeController controller = (PipelineDraweeController)
    Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    // other setters as you need
    .build();
mSimpleDraweeView.setController(controller);
```

#### 注意点

图片在进入后处理器(postprocessor)的图片是原图的一个完整拷贝，原来的图片不受修改的影响。在5.0以前的机器上，拷贝后的图片也在native内存中。

在开始一个图片显示时，即使是反复显示同一个图片，在每次进行显示时，都需要指定后处理器。

对于同一个图片，每次显示，可以使用不同的后处理器。

#### Repeated Postprocessors

如果想对同一个图片进行多次后处理，那么继承[BaseRepeatedPostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BaseRepatedPostprocessor.html)即可。该类有一个`update`方法，需要执行后处理时，调用该方法即可。

下面的例子展示了在运行时，后处理改变图片网格的颜色:

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

// setPostprocessor as in above example

// 改变颜色
meshPostprocessor.setColor(Color.RED);
meshPostprocessor.setColor(Color.BLUE);
```

每个image request, 仍旧只有一个`Postprocessor`，但是这个后处理器是状态相关了。
