---
id: writing-custom-views
title: Writing Custom Views
layout: docs
permalink: /docs/writing-custom-views.html
prev: image-requests.html
next: gotchas.html
---

### DraweeHolders

There will always be times when `DraweeViews` won't fit your needs. You may need to show additional content inside the same view as your image. You might to show multiple images inside a single view.

We provide two alternate classes you can use to host your Drawee:

* `DraweeHolder` for a single image
* `MultiDraweeHolder` for multiple images

### Responsibilities of custom views

Android lays out View objects, and only they get told of system events. `DraweeViews` handle these events and use them to manage memory effectively. When using the holders, you must implement some of this functionality yourself.

#### Handling attach/detach events

**Your app may leak memory if this steps are not followed.**

There is no point in images staying in memory when Android is no longer displaying the view - it may have scrolled off-screen, or otherwise not be drawing. Drawees listen for detaches and release memory when they occur. They will automatically restore the image when it comes back on-screen.

All this is automatic in a `DraweeView,` but won't happen in a custom view unless you handle four system events. These must be passed to the `DraweeHolder`. Here's how:

```java
DraweeHolder mDraweeHolder;

@Override
public void onDetachedFromWindow() {
  super.onDetachedToWindow();
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
Drawable drawable = mDraweeHolder.getHierarchy().getTopLevelDrawable();
drawable.setBounds(...);
```
or the Drawee won't appear at all.

* Do not downcast this Drawable.
* Do not translate it.

#### Other responsibilities

* Override `verifyDrawable:`

```java
@Override
protected boolean verifyDrawable(Drawable who) {
  if (who == mDraweeHolder.getHierarchy().getTopLevelDrawable()) {
    return true;
  }
  // other logic for other Drawables in your view, if any
}
```

* Make sure `invalidateDrawable` invalidates the region occupied by your Drawee.


### Constructing a DraweeHolder

This should be done carefully.

#### Arranging your Constructors
 
We recommend the following pattern for constructors:

* Override all three of the three View constructors.
* Each constructor calls its superclass counterpart and then a private `init` method.
* All of your initialization happens in `init.`
 
That is, do not use the `this` operator to call one constructor from another. 

This approach guarantees that the correct initialization is called no matter what constructor is used. It is in the `init` method that your holder is created.

#### Creating the Holder

If possible, always create Drawees when your view gets created. Creating a hierarchy is not cheap so it's best to do it only once.

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

Instead of using a `DraweeHolder`, use a `MultiDraweeHolder`. There are `add`, `remove`, and `clear` methods for dealing with Drawees:

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


