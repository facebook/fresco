---
docid: using-drawees-code
title: Using Drawees in Code
layout: docs
permalink: /docs/using-drawees-code.html
prev: using-drawees-xml.html
next: drawee-branches.html
---

### Setting the actual image

The easy way is to call

```
mSimpleDraweeView.setImageURI(uri);
```

For more advanced requirements, use a [controller builder](using-controllerbuilder.html).

### Customizing the hierarchy

For most apps, specify the parameters of their hierarchy in [XML](using-drawees-xml.html) provides all the customization they need. In some cases, though, you may need to go further.

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

Calling `setHierarchy` more than once on the same view is usually unnecessary, even if the view is recycled. The hierarchy is expensive to create and in most cases you can just modify the existing hierarchy instead of creating a new one. Keep in mind that each instance of a `DraweeView` needs its own instance of a `DraweeHierarchy`. Same instance of a `DraweeHierarchy` is **not** to be used by multiple views.
To change the actual image displayed by the view, use `setController` or `setImageURI`.

### Modifying the hierarchy in-place

Some attributes of the hierarchy can be changed without having to build a new hierarchy.

You would first need to get it from the View:

```java
GenericDraweeHierarchy hierarchy = mSimpleDraweeView.getHierarchy();
```

<a name="change_placeholder"></a>

#### Changing the placeholder

Then you could modify the placeholder, either with a resource id:

```java
hierarchy.setPlaceholderImage(R.drawable.placeholderId);
```

 or a full-fledged [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html):

```java
Drawable placeholderImage = ...;
hierarchy.setPlaceholderImage(placeholderImage);
```

The other image branches (failure image, retry image and progress bar) can be modified in a similar way too.

```java
Drawable failureImage = ...;
hierarchy.setFailureImage(failureImage, ScaleType.CENTER);
```

#### Changing the actual image display

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

All of the [rounding related params](rounded-corners-and-circles.html) can be modified dynamically. You get a `RoundingParams` object from the hierarchy, modify it, and set it back again:

```java
RoundingParams roundingParams = hierarchy.getRoundingParams();
roundingParams.setCornersRadius(10);
hierarchy.setRoundingParams(roundingParams);
```
