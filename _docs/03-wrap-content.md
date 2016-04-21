---
docid: wrap-content
title: Wrap_content
layout: docs
permalink: /docs/wrap-content.html
prev: gotchas.html
next: shared-transitions.html
---

### Why `wrap_content` is not supported

*Why can't I use Android's `wrap_content` attribute on a DraweeView?* we are often asked.

The reason is that Drawee always returns -1 for [getIntrinsicHeight](http://developer.android.com/reference/android/graphics/drawable/Drawable.html#getIntrinsicHeight()) and getIntrinsicWidth methods.

And the reason for that is that unlike a simple ImageView, Drawee may show more than one thing at the same time. For example, during the fade transition from the placeholder to the actual image, both images are visible. There may even be more than one actual image, one low-resolution, the other high-resolution. If all these images are not of exactly the same size, and they practically never are, then the concept of an "intrinsic" size cannot be well defined.

We could have returned the size of the placeholder until the image has finished loading, and then swap to the actual image's size. If we did that, though, the image would not appear correctly - it would be scaled or cropped to the placeholder's size. The only way to prevent that would be to force an Android layout pass when the image loads. Not only will that hurt your app's scroll perf, but it will be jarring for your users, who will suddenly see your app change on screen. Imagine if the user is reading a text article and all of a sudden the text jumps down because the image above it just loaded and caused everything to re-layout.

For this reason, you have to use an actual size or `match_parent` to lay out a DraweeView.

If your images are coming from a server, it may be possible to ask that server for the image dimensions, before you download it. This should be a faster request. Then use [setLayoutParams](http://developer.android.com/reference/android/view/View.html#setLayoutParams(android.view.ViewGroup.LayoutParams)) to dynamically size your view upfront.

If on the other hand your use case is a legitimate exception, you can actually resize Drawee view dynamically by using a controller listener as explained [here](http://stackoverflow.com/a/34075281/3027862). And remember, we intentionally removed this functionality because it is undesireable. [Ugly things should look ugly.](https://youtu.be/qCdpTji8nxo?t=890).
