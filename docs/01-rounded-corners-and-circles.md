---
id: rounded-corners-and-circles
title: Rounded Corners and Circles
layout: docs
permalink: /docs/rounded-corners-and-circles.html
prev: scaling.html
next: using-controllerbuilder.html
---

Not every image is a rectangle. Apps frequently need images that appear with softer, rounded corners, or as circles. Drawee supports a variety of scenarios, all without the memory overhead of copying bitmaps.

### What

Images can be rounded in two shapes:

1. As a circle - set `roundAsCircle` to true.
2. As a rectangle, but with rounded corners. Set `roundedCornerRadius` to some value.

Rectangles support having each of the four corners have a different radius, but this must be specified in Java code rather than XML.

### How

Images can be rounded with two different methods:

1. Uses a shader to actually draw the rounded corners. This works only on the actual image and the [ placeholder](drawee-components.html). Other components, like failure and retry images, are not rounded. This is the default.
2. Draws rounded corners by overlaying a solid color, specified by the caller. The Drawee's background should be static and of the same solid color. Use `roundWithOverlayColor` in XML, or `setOverlayColor` in code, for this effect.

### In XML

The `SimpleDraweeView` class will forward several attributes over to `RoundingParams`:

```xml
<com.facebook.drawee.view.SimpleDraweeView
   ...
   fresco:roundedCornerRadius="5dp"
   fresco:roundBottomLeft="false"
   fresco:roundBottomRight="false"
   fresco:roundWithOverlayColor="@color/blue"
   fresco:roundingBorderWidth="1dp"
   fresco:roundingBorderColor="@color/red"
```

### In code

When [constructing a hierarchy](using-drawees-code.html), you can pass an instance of [RoundingParams](../javadoc/reference/com/facebook/drawee/generic/RoundingParams.html) to your `GenericDraweeHierarchyBuilder:`

```java
RoundingParams roundingParams = RoundingParams.fromCornersRadius(7f);
roundingParams.setOverlayColor(R.color.green);
// alternatively use fromCornersRadii or asCircle
genericDraweeHierarchyBuilder
    .setRoundingParams(roundingParams);
```

You can also change most of the rounding parameters on the fly:

```java
RoundingParams roundingParams = 
    mSimpleDraweeView.getHierarchy().getRoundingParams();
roundingParams.setBorder(R.color.red, 1.0);
roundingParams.setRoundAsCircle(true);
mSimpleDraweeView.getHierarchy().setRoundingParams(roundingParams);
```

The one exception to this is that the `RoundingMethod` cannot be changed when changing dynamically. Attempting to do so will throw an `IllegalStateException.`
