---
id: getting-started
title: 开始使用 Fresco
layout: docs-cn
permalink: /docs-cn/getting-started.html
prev: compile-in-android-studio.html
next: concepts.html
---

如果你仅仅是想简单下载一张网络图片，在下载完成之前，显示一张占位图，那么简单使用 [SimpleDraweeView](../javadoc/reference/com/facebook/drawee/view/SimpleDraweeView.html) 即可。

在Application 初始化时:

```java
Fresco.initialize(context);
```

在xml布局文件中, 加入命名空间:

```xml
<!-- 其他元素 -->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:fresco="http://schemas.android.com/apk/res-auto">
```

加入`SimpleDraweeView`:

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="20dp"
    fresco:placeholderImage="@drawable/my_drawable"
  />
```

开始加载图片

```java
Uri uri = Uri.parse("https://raw.githubusercontent.com/liaohuqiu/fresco-docs-cn/docs/static/fresco-logo.png");
draweeView.setImageURI(uri);
```

剩下的，Fresco会替你完成:

* 显示占位图直到加载完成；
* 下载图片；
* 缓存图片；
* 图片不再显示时，从内存中移除；

等等等等。
