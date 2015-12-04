---
id: intrinsic-dimensions
title: Intrinsic Dimensions
layout: docs
permalink: /docs/intrinsic-dimensions.html
prev: writing-custom-views.html
next: intro-image-pipeline.html
---

### No intrinsic dimensions and no wrap-content

You can't call getIntrinsicHeight/Width on a Drawee.
Technically you can, but the result will always be -1.
Consequently, wrap-content attribute is not supported either.
Here's why.

When dealing with network images, immediate delivery can not be guaranteed.
In fact this is true even for the images that come from disk cache.
That means that for some brief period of time, before seeing the actual image,
placeholder gets displayed.
This causes intrinsic dimensions of the top-level drawable to change whenever actual image arrives.
Furthermore, things get even more complicated if we have to fade between placeholder and the actual image,
or if we have more than one actual image (e.g, low-res and then high-res).
Because of that, we made some design decisions that have important consequences:

#### Our top-level drawable always return -1 for its intrinsic width and height.

Rationale: Drawee already applies correct scale type scaling and hence there is no need for a view to do so.
Furthermore, ImageView can only apply one scale type whereas Drawee hierarchy can use separate scale types
for each image branch (placeholder, failure image, actual image, etc.).
Returning actual intrinsic dimensions to the view just puts as on risk of having sizing bugs.

#### We do not support `wrap_content` for `android:layout_width/height`.

Rationale: In cases where we actually do want to size view based on the image size,
we can do so in advance (before the actual image arrives) by storing image dimensions 
elsewhere (possibly getting it from the server) then programmatically setting LayoutParams
to the view. We can see that
`wrap_content` is not relevant (it gets overridden) in this case.

Another reason is that having a "jumpy" behavior (views get resized and re-laid out
when actual image arrives) is not a desirable behavior both from aesthetics and
performance point of view. Layouts are expensive and shuffling the whole scene when
image arrives looks ugly. Imagine how would feed look like if we don't size our image
views upfront, but just pop the image in once it arrives by moving the content before
it above and content after it below. That would be very disruptive if you are in the
middle of reading that content.

The one exception is when you call `setAspectRatio` on a Drawee.
