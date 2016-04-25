---
layout: home
title: Fresco | An image management library.
id: home
---

<div class="gridBlock">
<div class="featureBlock twoByGridBlock" markdown="1">
### Image Pipeline

Fresco's image pipeline will load images from the network, local storage, or local resources. To save data and CPU, it has three levels of cache; two in memory and another in internal storage.
</div>
<div class="featureBlock twoByGridBlock" markdown="1">
### Drawees

Fresco's `Drawee` shows a placeholder for you until the image has loaded and then automatically shows the image when it arrives. When the image goes off-screen, it automatically releases its memory.
</div>
</div>

## Features

<div class="gridBlock">
<div class="featureBlock twoByGridBlock" markdown="1">
### Memory

A decompressed image - an Android `Bitmap` - takes up a lot of memory. This leads to more frequent runs of the Java garbage collector. This slows apps down. The problem is especially bad without the improvements to the garbage collector made in Android 5.0.

On Android 4.x and lower, Fresco puts images in a special region of Android memory. It also makes sure that images are automatically released from memory when they're no longer shown on screen. This lets your application run faster - and suffer fewer crashes.

Apps using Fresco can run even on low-end devices without having to constantly struggle to keep their image memory footprint under control.

</div>
<div class="featureBlock twoByGridBlock" markdown="1">
### Loading

Fresco's image pipeline lets you customize the load in a variety of ways:

* Specify several different uris for an image, and choose the one already in cache for display
* Show a low-resolution image first and swap to a higher-res one when it arrives
* Send events back into your app when the image arrives
* If the image has an EXIF thumbnail, show it first until the full image loads (local images only)
* Resize or rotate the image
* Modify the downloaded image in-place
* Decode WebP images, even on older versions of Android that don't fully support them

</div>
<div class="featureBlock twoByGridBlock" markdown="1">
### Drawing

Fresco uses `Drawees` for display. These offer a number of useful features:

* Scale the image to a custom focus point, instead of the center
* Show the image with rounded corners, or a circle
* Let users tap the placeholder to retry load of the image, if the network load failed
* Show custom backgrounds, overlays, or progress bars on the image
* Show a custom overlay when the user presses the image

</div>
<div class="featureBlock twoByGridBlock" markdown="1">
### Streaming

Progressive JPEG images have been on the Web for years. These let a low-resolution scan of the image download first, then gradually improve the quality as more of the image downloads. This is a lifesaver for users on slow networks.

Android's own imaging libraries don't support streaming. Fresco does. Just specify a URI, and your app will automatically update its display as more data arrives.

</div>
<div class="featureBlock twoByGridBlock" markdown="1">
### Animations

Animated GIFs and WebPs can be challenging for apps. Each frame is a large Bitmap, and each animation is a series of frames. Fresco takes care of loading and disposing of frames and managing their memory.
</div>
</div>
