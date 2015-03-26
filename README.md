# Fresco 

Fresco is a powerful system for displaying images in Android applications.

Fresco takes care of image loading and display, so you don't have to. It will load images from the network, local storage, or local resources, and display a placeholder until the image has arrived. It has two levels of cache; one in memory and another in internal storage.

In Android 4.x and lower, Fresco puts images in a special region of Android memory. This lets your application run faster - and suffer the dreaded `OutOfMemoryError` much less often.

Fresco also supports:

* streaming of progressive JPEGs
* display of animated GIFs and WebPs
* extensive customization of image loading and display
* and many more!

Find out more at our [website](http://frescolib.org/index.html).

## Requirements

Fresco can be included in any Android application. 

Fresco supports Android 2.3 (Gingerbread) and later. 

## Using Fresco in your application

If you are building with Gradle, simply add the following line to the `dependencies` section of your `build.gradle` file:

```groovy
compile 'com.facebook.fresco:fresco:0.1.0+'
```

See our [download](http://frescolib.org/docs/download-fresco.html) page for other options.

## Building Fresco from source

Install the Android [SDK](https://developer.android.com/sdk/index.html#Other) if you haven't already. Then run the Android SDK Manager and install the Android Support Library and Android Support Repository.

Download the Android [NDK](https://developer.android.com/tools/sdk/ndk/index.html). Then add the directory containing it to your PATH environment variable.

Then just do

```sh
git clone https://github.com/facebook/fresco.git
cd fresco
./gradlew build
```

## Join the Fresco community

Please use our [issues page](https://github.com/facebook/fresco/issues) to let us know of any problems.

For pull requests, please see the [CONTRIBUTING](https://github.com/facebook/fresco/blob/master/CONTRIBUTING.md) file for information on how to help out.

## License
Fresco is [BSD-licensed](https://github.com/facebook/fresco/blob/master/LICENSE). We also provide an additional [patent grant](https://github.com/facebook/fresco/blob/master/PATENTS).
