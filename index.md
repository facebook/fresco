---
layout: default
title: Fresco | An image management library.
id: home
hero: true
---

# About Fresco

Fresco is a powerful system for displaying images in Android applications. It takes care of image loading and display so you don't have to. 

Fresco's *image pipeline* will load images from the network, local storage, or local resources. To save data and CPU, it has three levels of cache; two in memory and another in internal storage.

Fresco's *Drawees* show a placeholder for you until the image has loaded and automatically show to the image when it arrives. When the image goes off-screen, it automatically releases its memory.

Fresco supports Android 2.3 (Gingerbread) and later.

# Features

### Memory

A decompressed image - an Android `Bitmap` - takes up a lot of memory. This leads to more frequent runs of the Java garbage collector. This slows apps down. The problem is especially bad without the improvements to the garbage collector made in Android 5.0.

On Android 4.x and lower, Fresco puts images in a special region of Android memory. It also makes sure that images are automatically released from memory when they're no longer shown on screen. This lets your application run faster - and suffer fewer crashes.

Apps using Fresco can run even on low-end devices without having to constantly struggle to keep their image memory footprint under control.

### Streaming

Progressive JPEG images have been on the Web for years. These let a low-resolution scan of the image download first, then gradually improve the quality as more of the image downloads. This is a lifesaver for users on slow networks.

Android's own imaging libraries don't support streaming. Fresco does. Just specify a URI, and your app will automatically update its display as more data arrives.

### Animations

Animated GIFs and WebPs can be challenging for apps. Each frame is a large Bitmap, and each animation is a series of frames. Fresco takes care of loading and disposing of frames and managing their memory.

### Drawing

Fresco uses `Drawees` for display. These offer a number of useful features:
    
* Scale the image to a custom focus point, instead of the center
* Show the image with rounded corners, or a circle
* Let users tap the placeholder to retry load of the image, if the network load failed
* Show custom backgrounds, overlays, or progress bars on the image
* Show a custom overlay when the user presses the image

### Loading 

Fresco's image pipeline lets you customize the load in a variety of ways:
   
* Specify several different uris for an image, and choose the one already in cache for display
* Show a low-resolution image first and swap to a higher-res one when it arrives
* Send events back into your app when the image arrives
* If the image has an EXIF thumbnail, show it first until the full image loads (local images only)
* Resize or rotate the image 
* Modify the downloaded image in-place
* Decode WebP images, even on older versions of Android that don't fully support them

# Who Uses Fresco?
* [Wikipedia](https://play.google.com/store/apps/details?id=org.wikipedia)
* [Best Apps Market](https://play.google.com/store/apps/details?id=com.bestappsmarket.android.bestapps)
* [Redfin](https://play.google.com/store/apps/details?id=com.redfin.android) - See their [blog post](https://www.redfin.com/devblog/2015/10/using-fresco-to-load-images-efficiently-on-android.html) about Fresco!
* [Memrise](https://play.google.com/store/apps/details?id=com.memrise.android.memrisecompanion)
* [local.ch](https://play.google.com/store/apps/details?id=ch.local.android)
* [Mappy](https://play.google.com/store/apps/details?id=com.mappy.app)
* [YOP](https://play.google.com/store/apps/details?id=com.yopapp.yop)
* [ChatGame](https://play.google.com/store/apps/details?id=me.chatgame.mobilecg)
* [Bobble - Chat Stickers Stories](https://play.google.com/store/apps/details?id=com.touchtalent.bobbleapp)
* [ShareTheMeal](https://play.google.com/store/apps/details?id=org.sharethemeal.app)
* [NativeScript](https://www.nativescript.org/) and [apps using NativeScript](https://www.nativescript.org/showcases)
* [TouTiao](https://play.google.com/store/apps/details?id=com.ss.android.article.news)
* [Playbuzz](https://play.google.com/store/apps/details?id=com.playbuzz.android.app)
* [Camerite](https://play.google.com/store/apps/details?id=com.camerite)
* [nice](https://play.google.com/store/apps/details?id=com.nice.main)
* [JokeEssay](https://play.google.com/store/apps/details?id=com.ss.android.essay.joke)
* [Phonotto](https://play.google.com/store/apps/details?id=com.duckma.phonotto)
* [Bakar](https://play.google.com/store/apps/details?id=com.bakar)
* [LaMaille](https://play.google.com/store/apps/details?id=net.opalesurfcasting.lamaille)
* [Poke](https://play.google.com/store/apps/details?id=com.netpub.poke)
* [SamePinch](https://play.google.com/store/apps/details?id=co.samepinch.android.app)
* [ChacuaTool](https://play.google.com/store/apps/details?id=com.dak42.chacuatool)
* [React Native](https://facebook.github.io/react-native/) and [apps using React Native](https://facebook.github.io/react-native/showcase.html)
* [Facebook](https://play.google.com/store/apps/details?id=com.facebook.katana)
* [Messenger](https://play.google.com/store/apps/details?id=com.facebook.orca)
* [Moments](https://play.google.com/store/apps/details?id=com.facebook.moments)
* [Facebook Pages Manager](https://play.google.com/store/apps/details?id=com.facebook.pages.app)
* [Facebook Groups](https://play.google.com/store/apps/details?id=com.facebook.groups)  
* [Facebook Ads Manager](https://play.google.com/store/apps/details?id=com.facebook.adsmanager)

*Does your app use Fresco? Add it to this list with a [pull request](https://github.com/facebook/fresco/edit/gh-pages/index.md)!*
        
# More information

* Our [blog post](https://code.facebook.com/posts/366199913563917) announcing Fresco and explaining the technical design
* Our [talk](https://developers.facebooklive.com/videos/542/move-fast-ensuring-mobile-performance-without-breaking-things) at the F8 conference (begins at 25:00, or -14:35 remaining)
* [Download](docs/index.html) Fresco
* Our [documentation](docs/getting-started.html)
* Our source code on [GitHub](https://github.com/facebook/fresco)
