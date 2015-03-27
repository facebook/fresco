---
id: using-drawees-code
title: Using Drawees in Code
layout: docs
permalink: /docs/using-drawees-code.html
prev: using-drawees-xml.html
next: drawee-components.html
---

### Change the image

The easy to way is to call 

```java
mSimpleDraweeView.setImageURI(uri);
```

For more advanced requirements, use a [controller builder](using-controllerbuilder.html).

### Customizing the hierarchy

For most apps, specify the parameters of their hierarchy in [XML](using-drawees-xml) provides all the customization they need. In some cases, though, you may need to go further.

We create an instance of the builder and then set it to the view:

```java
List<Drawable> backgroundsList;
List<Drawable> overlaysList;
GenericDraweeHierarchyBuilder builder =
    new GenericDraweeHierarchyBuilder(getResources());
GenericDraweeHierarchy hierarchy = builder
    .setFadeDuration(300)
    .setPlaceholderImage(new MyCustomDrawable())
    .setBackgrounds(backgroundList)
    .setOverlays(overlaysList)
    .build();
mSimpleDraweeView.setHierarchy(hierarchy);
```

Do **not** call `setHierarchy` more than once on the same view, even if the view is recycled. The hierarchy is expensive to create and is intended to be used more than once. Use `setController` or `setImageURI` to change the image shown in it.

### Modifying the hierarchy in-place

Some attributes of the hierarchy can be changed while the application is running. 

You would first need to get it from the View:

```java
GenericDraweeHierarchy hierarchy = mSimpleDraweeView.getHierarchy();
```

#### Change the placeholder
Then you could modify the placeholder, either with a resource id:

```java
hierarchy.setPlaceholderImage(R.drawable.placeholderId);
```

 or a full-fledged [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html):

```java
Drawable drawable; 
// create your drawable
hierarchy.setPlaceholderImage(drawable);
```

#### Change the image display

You can change the [scale type](scaling.html):

```java
hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);
```

If you have chosen scale type `focusCrop,` you'll need to set a focus point:

```java
hierarchy.setActualImageFocusPoint(point);
```

You can add a color filter to the image:

```java
ColorFilter filter;
// create your filter
hierarchy.setActualImageColorFilter(filter);
```

#### Rounding

All of the [rounding related params](rounded-corners-and-circles.html), except the rounding method, can be modified. You get a `RoundingParams` object from the hierarchy, modify it, and set it back again:

```java
RoundingParams roundingParams = hierarchy.getRoundingParams();
roundingParams.setCornersRadius(10);
hierarchy.setRoundingParams(roundingParams);
```
