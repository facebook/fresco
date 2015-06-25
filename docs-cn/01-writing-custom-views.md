---
id: writing-custom-views
title: 自定义View
layout: docs-cn
permalink: /docs-cn/writing-custom-views.html
prev: image-requests.html
next: gotchas.html
---

### DraweeHolders

总有一些时候，`DraweeViews`是满足不了需求的，在展示图片的时候，我们还需要展示一些其他的内容，或者支持一些其他的操作。在同一个View里，我们可能会想显示一张或者多张图。

在自定义View中，Fresco 提供了两个类来负责图片的展现:

* `DraweeHolder` 单图情况下用。
* `MultiDraweeHolder` 多图情况下用。

### 自定义View需要完成的事情

Android 呈现View对象，只有View对象才能得到一些系统事件的通知。`DraweeViews`
处理这些事件通知，高效地管理内存。使用`DraweeHolder`时，你需要自己实现这几个方法。

#### 处理 attach/detach 事件

**如果没按照以下步骤实现的话，很可能会引起内存泄露**

当图片不再在View上显示时，比如滑动时View滑动到屏幕外，或者不再绘制，图片就不应该再存在在内存中。Drawees 监听这些事情，并负责释放内存。当图片又需要显示时，重新加载。

这些在`DraweeView`中是自动的，但是在自定义View中，需要我们自己去操作，如下:

```java
DraweeHolder mDraweeHolder;

@Override
public void onDetachedFromWindow() {
  super.onDetachedFromWindow();
  mDraweeHolder.onDetach();
}

@Override
public void onStartTemporaryDetach() {
  super.onStartTemporaryDetach();
  mDraweeHolder.onDetach();
}

@Override
public void onAttachedToWindow() {
  super.onAttachedToWindow();
  mDraweeHolder.onAttach();
}

@Override
public void onFinishTemporaryDetach() {
  super.onFinishTemporaryDetach();
  mDraweeHolder.onAttach();
}
```

#### 处理触摸事件

如果你启用了[点击重新加载](drawee-components.html#Retry)，在自定义View中，需要这样:

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
  return mDraweeHolder.onTouchEvent(event) || super.onTouchEvent(event);
}
```

#### 自定义onDraw

```java
Drawable drawable = mDraweeHolder.getHierarchy().getTopLevelDrawable();
drawable.setBounds(...);
```

否则图片将不会出现

* 不要向下转换这个Drawable
* 不要变换这个Drawable

#### 其他应该做的

* 重写 `verifyDrawable:`

```java
@Override
protected boolean verifyDrawable(Drawable who) {
  if (who == mDraweeHolder.getHierarchy().getTopLevelDrawable()) {
    return true;
  }
  // 对其他Drawable的验证逻辑
}
```

* 确保`invalidateDrawable` 处理了图片占用的那块区域。

### 创建 DraweeHolder

这同样需要非常小心和细致

#### 构造函数

我们推荐如下实现构造函数:

* 重写3个构造函数
* 在每个构造函数中调用同等签名的父类构造函数，和一个私有的`init`方法。
* 在`init`方法中执行初始化操作。

即，不要在构造函数中用`this`来调用另外一个构造。

这样可以保证，不管调用哪个构造，都可以正确地执行初始化流程。然后在`init`方法中创建holder。

#### 创建 Holder

如果有可能，只在View创建时，创建Drawees。创建DraweeHierarchy开销较大，最好只做一次。

```java
class CustomView extends View {
  DraweeHolder<GenericDraweeHierarchy> mDraweeHolder;

  // constructors following above pattern

  private void init() {
    GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources());
      .set...
      .set...
      .build();
    mDraweeHolder = DraweeHolder.create(hierarchy, context);
  }
}
```

#### 设置要显示的图片

使用[controller builder](using-controllerbuilder.html)创建DraweeController，然后调用holder的`setController`方法，而不是设置给自定义View。

```java
DraweeController controller = Fresco.newControllerBuilder()
    .setUri(uri)
    .setOldController(mDraweeHolder.getController())
    .build();
mDraweeHolder.setController(controller);
```

### MultiDraweeHolder

和`DraweeHolder`相比，`MultiDraweeHolder`有 `add`, `remove`, `clear`
等方法可以操作Drawees。如下:

```java
MultiDraweeHolder<GenericDraweeHierarchy> mMultiDraweeHolder;

private void init() {
  GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources());
    .set...
    .build();
  mMultiDraweeHolder = new MultiDraweeHolder<GenericDraweeHierarchy>();
  mMultiDraweeHolder.add(new DraweeHolder<GenericDraweeHierarchy>(hierarchy, context));
  // repeat for more hierarchies
}
```

同样，也需要处理系统事件，设置声音等等，就想处理单个`DraweeHolder`那样。
