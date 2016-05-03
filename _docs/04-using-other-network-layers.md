---
docid: using-other-network-layers
title: Using Other Network Layers
layout: docs
permalink: /docs/using-other-network-layers.html
prev: shared-transitions.html
next: using-other-image-loaders.html
---

By default, the image pipeline uses the [HttpURLConnection](https://developer.android.com/training/basics/network-ops/connecting.html) networking library bundled with Android. Apps may have their own network layer they may wish to use instead.

### Using OkHttp

[OkHttp](http://square.github.io/okhttp) is a popular open-source networking library. The image pipeline has a backend that uses OkHttp instead of the Android default.

####  OkHttp in Gradle

In order to use it, the `dependencies` section of your `build.gradle` file needs to be changed. Along with the Gradle dependencies given on the [download](index.html) page, add **just one** of these:

For OkHttp2:

```groovy
dependencies {
  // your project's other dependencies
  compile "com.facebook.fresco:imagepipeline-okhttp:{{site.current_version}}+"
}
```

For OkHttp3:

```groovy
dependencies {
  // your project's other dependencies
  compile "com.facebook.fresco:imagepipeline-okhttp3:{{site.current_version}}+"
}
```

#### OkHttp in Eclipse

Eclipse users should depend on **either** the `imagepipeline-okhttp` and `imagepipeline-okhttp3` directories in the `frescolib` tree as described in the [Eclipse instructions](index.html#eclipse-adt).

#### Configuring the image pipeline with OkHttp

You must also configure the image pipeline a little differently. Instead of using `ImagePipelineConfig.newBuilder`, use `OkHttpImagePipelineConfigFactory` instead:

```java
Context context;
OkHttpClient okHttpClient; // build on your own
ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
    .newBuilder(context, okHttpClient)
    . // other setters
    . // setNetworkFetcher is already called for you
    .build();
Fresco.initialize(context, config);
```

### Handling sessions and cookies correctly

The `OkHttpClient` you pass to Fresco in the above step should be set up with interceptors needed to handle authentications to your servers. See [this bug](https://github.com/facebook/fresco/issues/385) and the solutions outlined there for some problems that have occurred with cookies.

### Using your own network fetcher (optional)

For complete control on how the networking layer should behave, you can provide one for your app. You must subclass [NetworkFetcher](../javadoc/reference/com/facebook/imagepipeline/producers/NetworkFetcher.html), which controls communications to the network. You can also optionally subclass [FetchState](../javadoc/reference/com/facebook/imagepipeline/producers/FetchState.html), which is a data structure for request-specific information.

Our implementation for `OkHttp 3` can be used as an example. See [its source code](https://github.com/facebook/fresco/blob/master/imagepipeline-backends/imagepipeline-okhttp3/src/main/java/com/facebook/imagepipeline/backends/okhttp3/OkHttpNetworkFetcher.java).

You must pass your network producer to the image pipeline when [configuring it](configuring-image-pipeline.html):

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setNetworkFetcher(myNetworkFetcher);
  . // other setters
  .build();
Fresco.initialize(context, config);
```
