---
docid: sample-code
title: Sample code
layout: docs
permalink: /docs/sample-code.html
---

*Note: the samples are licensed for non-commercial or evaluation purposes only, not the MIT license used for Fresco itself.*

Fresco's GitHub repository contains several samples to demonstrate how to use Fresco in your apps.

The samples are available in source form only. Follow the [build instructions](building-from-source.html) to set up your dev environment to build and run them.

### The Showcase app

The [Showcase App](https://github.com/facebook/fresco/blob/main/samples/showcase) demonstrates various features and allows to customize parameters to show their effect.
It includes samples for Drawee and for the image pipeline. Furthermore, it showcases how to use both built-in and custom image formats.

### The zoomable library

The [zoomable library](https://github.com/facebook/fresco/blob/main/samples/zoomable) features a `ZoomableDraweeView` class that supports gestures such as pinch-to-zoom and panning of a Drawee image.

### The comparison app

The comparison app lets the user do a proper, apples-to-apples comparison of Fresco with [Picasso](http://square.github.io/picasso), [Universal Image Loader](https://github.com/nostra13/Android-Universal-Image-Loader), [Volley](https://developer.android.com/training/volley/index.html)'s image loader, and [Glide](https://github.com/bumptech/glide).

Fresco allows you to also compare its performance with OkHttp as its network layer. You can also see the performance of Drawee running over Volley instead of Fresco's image pipeline.

The app offers you a choice of images from your local camera or from the Internet. The network images come from [Imgur](http://imgur.com).

You can build, install, and run a controlled test of any combination of loaders using the [run_comparison.py](https://github.com/facebook/fresco/blob/main/run_comparison.py) script. The following command will run them all on a connected ARM v7 device:

```./run_comparison.py -c armeabi-v7a```

### The round app

The round app shows the same image scaled in several different ways, with and without a circle applied.
