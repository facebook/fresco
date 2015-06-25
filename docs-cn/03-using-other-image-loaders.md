---
id: using-other-image-loaders
title: 使用其他的Image Loader
layout: docs-cn
permalink: /docs-cn/using-other-image-loaders.html
prev: using-other-network-layers.html
---

Drawee is not tied to a particular image loading mechanism and can be used with other image loaders.
Drawee 并不是吊死在特定的一种图片加载机制上，它同样适用于其他image loader。

不过有一些特性，只有Fresco image pipeline才有。前面的提到的需要使用[ImageRequest]和[配置image
pipeline]的特性，使用其他image loader时都有可能不起作用。

### Drawee 和 Volley ImageLoader配合使用

我们有一个Drawee使用Volley的 [ImageLoader](https://developer.android.com/training/volley/request.html)的补充实现。

我们仅仅对那些已经深度使用Volley ImageLoader的应用推荐这个组合。

同样地，如要使用，使用下面的依赖，而不是[下载](download-fresco.html)页面给出的依赖:

```groovy
dependencies {
  // your project's other dependencies
  compile: "com.facebook.fresco:drawee-volley:{{site.current_version}}+"
}
```

#### 初始化Volley ImageLoader

这时，不需要再调用`Fresco.initialize`了，需要的是初始化Volley。

```java
Context context;
ImageLoader imageLoader; // build yourself
VolleyDraweeControllerBuilderSupplier mControllerBuilderSupplier
    = new VolleyDraweeControllerBuilderSupplier(context, imageLoader);
SimpleDraweeView.initialize(mControllerBuilderSupplier);
```

不要让 `VolleyDraweeControllerBuilderSupplier`离开作用域，你需要它来创建DraweeController，除非你只使用`SimpleDraweeView.setImageURI`。

#### DraweeControllers 和 Volley ImageLoader 配合使用

不是调用`Fresco.newControllerBuilder`, 而是:

```java
VolleyController controller = mControllerBuilderSupplier
    .newControllerBuilder()
    . // setters
    .build();
mSimpleDraweeView.setController(controller);
```

### Drawee 和其他Image Loader 配合使用

依照[源码](https://github.com/facebook/fresco/tree/master/drawee-backends/drawee-volley/src/main/java/com/facebook/drawee/backends/volley) 作为例子，其他Image Loader也是可以和Drawee配合使用的，但是没有我们还没有Drawee和其他Image loader的配合使用的补充实现。
