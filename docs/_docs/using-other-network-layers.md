---
docid: using-other-network-layers
title: Using Other Network Layers
layout: docs
permalink: /docs/using-other-network-layers.html
---

By default, the image pipeline uses the [HttpURLConnection](https://developer.android.com/training/basics/network-ops/connecting.html) which is included in the Android framework. However, if needed by the app a custom network layer can be used. Fresco already contains one alternative network layer that is based on OkHttp.

### Using OkHttp

[OkHttp](http://square.github.io/okhttp) is a popular open-source networking library.

### 1. Gradle setup

In order to use it, the `dependencies` section of your `build.gradle` file needs to be changed. Along with the Gradle dependencies given on the [Getting started](index.html) page, add **just one** of these:

For OkHttp2:

```groovy
dependencies {
  // your project's other dependencies
  implementation "com.facebook.fresco:imagepipeline-okhttp:{{site.current_version}}"
}
```

For OkHttp3:

```groovy
dependencies {
  // your project's other dependencies
  implementation "com.facebook.fresco:imagepipeline-okhttp3:{{site.current_version}}"
}
```

#### 2. Configuring the image pipeline to use OkHttp

You must also configure the image pipeline. Instead of using `ImagePipelineConfig.newBuilder`, use `OkHttpImagePipelineConfigFactory`:

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

For a more detailed example of this, see how this if configured in the [Fresco showcase app](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/ShowcaseApplication.java).

### Handling sessions and cookies correctly

The `OkHttpClient` you pass to Fresco in the above step should be set up with interceptors needed to handle authentications to your servers. See [this bug](https://github.com/facebook/fresco/issues/385) and the solutions outlined there for some problems that have occurred with cookies.

### Using your own network fetcher (optional)

For complete control on how the networking layer should behave, you can provide one for your app. You must subclass [NetworkFetcher](../javadoc/reference/com/facebook/imagepipeline/producers/NetworkFetcher.html), which controls communications to the network. You can also optionally subclass [FetchState](../javadoc/reference/com/facebook/imagepipeline/producers/FetchState.html), which is a data structure for request-specific information.

Our implementation for `OkHttp 3` can be used as an example. See [its source code](https://github.com/facebook/fresco/blob/master/imagepipeline-backends/imagepipeline-okhttp3/src/main/java/com/facebook/imagepipeline/backends/okhttp3/OkHttpNetworkFetcher.java).

You must pass your network producer to the image pipeline when [configuring it](configure-image-pipeline.html):

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setNetworkFetcher(myNetworkFetcher);
  . // other setters
  .build();
Fresco.initialize(context, config);
```
