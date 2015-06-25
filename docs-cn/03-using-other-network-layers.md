---
id: using-other-network-layers
title: 自定义网络加载
layout: docs-cn
permalink: /docs-cn/using-other-network-layers.html
prev: closeable-references.html
next: using-other-image-loaders.html
---

Image pipeline 默认使用[HttpURLConnection](https://developer.android.com/training/basics/network-ops/connecting.html)。应用可以根据自己需求使用不同的网络库。

### OkHttp

[OkHttp](http://square.github.io/okhttp) 是一个流行的开源网络请求库。Image
pipeline有一个使用OkHttp替换掉了Android默认的网络请求的补充。

如果需要使用OkHttp,
不要使用这个[下载](download-fresco.html)页面的gradle依赖配置，应该使用下面的依赖配置

```groovy
dependencies {
  // your project's other dependencies
  compile "com.facebook.fresco:fresco:{{site.current_version}}+"
  compile 'com.facebook.fresco:imagepipeline-okhttp:{{site.current_version}}+'
}
```

配置Image
pipeline这时也有一些不同，不再使用`ImagePipelineConfig.newBuilder`,而是使用`OkHttpImagePipelineConfigFactory`:

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

### 使用自定的网络层

For complete control on how the networking layer should behave, you can provide one for your app. You must subclass
为了完全控制网络层的行为，你可以自定义网络层。继承[NetworkFetchProducer](../javadoc/reference/com/facebook/imagepipeline/producers/NetworkFetchProducer.html), 这个类包含了网络通信。

你也可以选择性地继承[NfpRequestState](../javadoc/reference/com/facebook/imagepipeline/producers/NfpRequestState.html), 这个类是请求时的数据结构描述。

默认的 `HttpURLConnection` 可以作为一个参考. 源码在这 [its source code](https://github.com/facebook/fresco/blob/master/imagepipeline/src/main/java/com/facebook/imagepipeline/producers/HttpUrlConnectionNetworkFetcher.java).

在[配置Image pipeline](configuring-image-pipeline.html)时，把producer传递给Image pipeline。

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setNetworkFetchProducer(myNetworkFetchProducer);
  . // other setters
  .build();
Fresco.initialize(context, config);
```
