# Fresco

<img alt="Fresco Logo" align="right" src="docs/static/sample-images/fresco_logo.svg" width="15%" />

[![Build Status](https://circleci.com/gh/facebook/fresco.svg?style=shield)](https://circleci.com/gh/facebook/fresco)

Fresco is a powerful system for displaying images in Android applications.

Fresco takes care of image loading and display, so you don't have to. It will load images from the network, local storage, or local resources, and display a placeholder until the image has arrived. It has two levels of cache; one in memory and another in internal storage.

In Android 4.x and lower, Fresco puts images in a special region of Android memory. This lets your application run faster - and suffer the dreaded `OutOfMemoryError` much less often.

Fresco also supports:

* streaming of progressive JPEGs
* display of animated GIFs and WebPs
* extensive customization of image loading and display
* and much more!

Find out more at our [website](http://frescolib.org/index.html).

## Requirements

Fresco can be included in any Android application.

Fresco supports Android 2.3 (Gingerbread) and later.

## Using Fresco in your application

If you are building with Gradle, simply add the following line to the `dependencies` section of your `build.gradle` file:

```groovy
implementation 'com.facebook.fresco:fresco:2.3.0'
```

For full details, visit the documentation on our web site, available in English, Chinese, and Korean:

<a href="http://frescolib.org/docs/index.html"><img src="http://frescolib.org/static/GetStarted-en.png" width="150" height="42"/></a>

<a href="http://fresco-cn.org/docs/index.html"><img src="http://frescolib.org/static/GetStarted-zh.png" width="104" height="42"/></a>

<a href="http://fresco.recrack.com/docs/index.html"><img src="http://frescolib.org/static/GetStarted-ko.png" width="104" height="42"/></a>

## Join the Fresco community

Please use our [issues page](https://github.com/facebook/fresco/issues) to let us know of any problems.

For pull requests, please see the [CONTRIBUTING](https://github.com/facebook/fresco/blob/master/CONTRIBUTING.md) file for information on how to help out. See our [documentation](http://frescolib.org/docs/building-from-source.html) for information on how to build from source.


## License
Fresco is [MIT-licensed](https://github.com/facebook/fresco/blob/master/LICENSE).
