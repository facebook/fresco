---
id: wrap-content
title: Wrap_content
layout: docs
permalink: /docs/wrap-content.html
prev: gotchas.html
next: using-other-network-layers.html
---

### Why `wrap_content` is not supported

*Why can't I use Android's `wrap_content` attribute on a DraweeView?* we are often asked.

The reason is that Drawees can't support the [getIntrinsicHeight](http://developer.android.com/reference/android/graphics/drawable/Drawable.html#getIntrinsicHeight()) and getIntrinsicWidth methods. These always return -1.

And the reason for that is that a Drawee shows more than one thing. It may show a placeholder, and then fade in to an actual image. It may even have more than one actual images, one low-resolution, the other high-resolution. If these are not the same size, and they usually are not, then the concept of an "intrinsic" size is meaningless.

We could have returned the size of the placeholder until the image has finished loading, and then swap to the actual image's size. If we did that, though, the image would not appear correctly - it would be scaled or cropped to the placeholder's size. The only way to prevent that would be to force an Android layout pass when the image loads. Not only will that hurt your app's scroll perf, but it will be jarring for your users, who will suddenly see your app change on screen.

For this reason, you have to use an actual size or `match_parent` to lay out a DraweeView.

If your images are coming from a server, it may be possible to ask that server for the image's size, before you download it. This should be a faster request. Then use [setLayoutParams](http://developer.android.com/reference/android/view/View.html#setLayoutParams(android.view.ViewGroup.LayoutParams)) to dynamically size your view.
