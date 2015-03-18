---
id: using-other-network-layers
title: Using Other Network Layers
layout: docs
permalink: /docs/using-other-network-layers.html
prev: closeable-references.html
next: using-other-image-loaders.html
---

By default, the image pipeline uses the [HttpURLConnection](https://developer.android.com/training/basics/network-ops/connecting.html) networking library bundled with Android. Apps may have their own network layer they may wish to use instead.

### Using OkHttp

[OkHttp](http://square.github.io/okhttp) is a popular open-source networking library. The image pipeline has a backend that uses OkHttp instead of the Android default.

In order to use it, the `dependencies` section of your `build.gradle` file needs to be changed. Do **not** use the Gradle dependencies given on the [download](download-fresco.html) page. Use these instead:

```groovy
dependencies {
  // your project's other dependencies
  compile: "com.facebook.fresco:drawee:0.1.0+"
  compile: "com.facebook.fresco:imagepipeline-okhttp:0.1.0+"
}
```

You must also configure the image pipeline a little differently. Instead of using `ImagePipelineConfig.newBuilder`, use `OkHttpImagePipelineConfigFactory` instead:

```java
Context context;
OkHttpClient okHttpClient; // build on your own
ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
    .newBuilder(context, okHttpClient)
    . // other setters
    . // setNetworkFetchProducer is already called for you
    .build();
Fresco.initialize(context, config);
```    


### Using your own layer

For complete control on how the networking layer should behave, you can provide one for your app. You must subclass [NetworkFetchProducer](../javadoc/reference/com/facebook/imagepipeline/producers/NetworkFetchProducer.html), which controls communications to the network. You can also optionally subclass [NfpRequestState](../javadoc/reference/com/facebook/imagepipeline/producers/NfpRequestState.html), which is a data structure for request-specific information.

Our default implementation for `HttpURLConnection` can be used as an example. See [its source code](https://github.com/facebook/fresco/blob/master/imagepipeline/src/main/java/com/facebook/imagepipeline/producers/HttpURLConnectionNetworkFetchProducer.java).

You must pass your network producer to the image pipeline when [configuring it](configuring-image-pipeline.html):

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setNetworkFetchProducer(myNetworkFetchProducer);
  . // other setters
  .build();
Fresco.initialize(context, config);
```
