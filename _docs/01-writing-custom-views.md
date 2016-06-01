---
docid: writing-custom-views
title: Writing Custom Views
layout: docs
permalink: /docs/writing-custom-views.html
prev: image-requests.html
next: intro-image-pipeline.html
---

### DraweeHolders

There will always be times when `DraweeViews` won't fit your needs. You may need to show additional content inside the same view as your image. You might need to show multiple images inside a single view.

We provide two alternative classes you can use to host your Drawee:

* `DraweeHolder` for a single image
* `MultiDraweeHolder` for multiple images

`DraweeHolder` is a class that holds one DraweeHierarchy and the associated DraweeController. It allows you to make use of all the functionality Drawee provides in your custom views and other places where you need a drawable instead of a view. To get the drawable, you just do `mDraweeHolder.getTopLevelDrawable()`. Keep in mind that Android drawables require a bit of housekeeping which we covered below.
`MultiDraweeHolder` is basically just an array of `DraweeHolder`s with some syntactic sugar added on top of it.

### Responsibilities of custom views

Android lays out View objects, and only they get notified of system events. `DraweeViews` handle these events and use them to manage memory effectively. When using the holders, you must implement some of this functionality yourself.

#### Handling attach/detach events

**Your app may leak memory, or the image may not be displayed at all, if these steps are not followed.**

There is no point in images staying in memory when Android is no longer displaying the view - it may have scrolled off-screen, or otherwise not be drawing. Drawees listen for detaches and release memory when they occur. They will automatically restore the image when it comes back on-screen.

All this is automatic in a `DraweeView,` but won't happen in a custom view unless you handle four system events. These must be passed to the `DraweeHolder`. Here's how:

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

It is important that `Holder` receives all the attach/detach events that the view itself receives. If the holder misses an attach event the image may not be displayed because Drawee will think that the view is not visible. Likewise, if the holder misses an detach event, the image may still remain in memory because Drawee will think that the view is still visible. Best way to ensure that is to create the holder from your view's constructor.

#### Handling touch events

If you have enabled [tap to retry](drawee-components.html#Retry) in your Drawee, it will not work unless you tell it that the user has touched the screen. Like this:

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
  return mDraweeHolder.onTouchEvent(event) || super.onTouchEvent(event);
}
```

#### Your custom onDraw

You must call

```java
Drawable drawable = mDraweeHolder.getTopLevelDrawable();
drawable.setBounds(...);
...
drawable.draw(canvas);
```
or the Drawee won't appear at all.

* Do not downcast this Drawable. The underlying implementation may change without any notice.
* Do not translate it. Just set the proper bounds.
* If you need to apply some canvas transformations, then make sure that you properly invalidate the area that the drawable occupies in the view. See below on how to do that.

#### Other responsibilities

* Set [Drawable.Callback] (http://developer.android.com/reference/android/graphics/drawable/Drawable.Callback.html)

```java
// When a holder is set to the view for the first time,
// don't forget to set the callback to its top-level drawable:
mDraweeHolder = ...
mDraweeHolder.getTopLevelDrawable().setCallback(this);

// In case the old holder is no longer needed,
// don't forget to clear the callback from its top-level drawable:
mDraweeHolder.getTopLevelDrawable().setCallback(null);
mDraweeHolder = ...
```

* Override `verifyDrawable:`

```java
@Override
protected boolean verifyDrawable(Drawable who) {
  if (who == mDraweeHolder.getTopLevelDrawable()) {
    return true;
  }
  // other logic for other Drawables in your view, if any
}
```

* Make sure `invalidateDrawable` invalidates the region occupied by your Drawee. If you apply some canvas transformations on the drawable before it gets drawn, then those transformations needs to be taken into account in invalidation. The simplest thing to do is what Android ImageView does in its [invalidateDrawable] (http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/4.4.4_r1/android/widget/ImageView.java#192) method. That is, to just invalidate the whole view when the drawable gets invalidated.


### Constructing the View and DraweeHolder

This should be done carefully. Se below.

#### Arranging your Constructors

We recommend the following pattern for constructors:

* Override all three of the three View constructors.
* Each constructor calls its superclass counterpart and then a private `init` method.
* All of your initialization happens in `init.`

That is, do not use the `this` to call one constructor from another. This is because Android View already calls one constructor from another, and it does so in an unintuitive way.

This approach guarantees that the correct initialization is called no matter what constructor is used. It is in the `init` method that your holder is created.

#### Creating the Holder

If possible, always create Drawees when your view gets created. Creating a hierarchy is not cheap so it's best to do it only once. More importantly, holder's lifecycle should be bound to the view's lifecycle for the reasons explained in the attach/detach section. Best way to ensure that is to create the holder when the view gets constructed as explained above.

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

#### Setting an image

Use a [controller builder](using-controllerbuilder.html), but call `setController` on the holder instead of a View:

```java
DraweeController controller = Fresco.newControllerBuilder()
    .setUri(uri)
    .setOldController(mDraweeHolder.getController())
    .build();
mDraweeHolder.setController(controller);
```

### MultiDraweeHolder

If you are dealing with multiple drawees in your custom view, `MultiDraweeHolder` might come handy. There are `add`, `remove`, and `clear` methods for dealing with DraweeHalders:

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

You must override system events, set bounds, and do all the same responsibilities as for a single `DraweeHolder.`


