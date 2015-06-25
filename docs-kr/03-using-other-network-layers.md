---
id: using-other-network-layers
title: 다른 네트워크 레이어 사용하기
layout: docs-kr
permalink: /docs-kr/using-other-network-layers.html
prev: closeable-references.html
next: using-other-image-loaders.html
---

기본적으로 안드로이드에서 이미지 파이프 라인은 [HttpURLConnection](https://developer.android.com/training/basics/network-ops/connecting.html) 네트워크 라이브러리를 사용합니다. 앱은 이것 대신에 사용할 자신만의 네트워크 라이브러리를 사용하고 싶어할 수 있습니다.

### OkHttp 사용하기
OkHttp 사용법

[OkHttp](http://square.github.io/okhttp)는 많이 사용되는 오픈소스 네트워크 라이브러리 입니다. 이미지 파이프라인 백앤드는 안드로이드 기본 라이브러리 대신 OkHttp를 사용합니다.

#####  OkHttp in Gradle

사용하는 방법은 프로젝트의 `build.gradle` 파일에서 `dependencies` 섹션을 수정해야 합니다. [다운로드](index.html) 페이지에서 제공되어지는 Gradle dependencies를 사용하면 **안됩니다.**
대신에 이것을 사용하세요:

```groovy
dependencies {
  // 프로젝트의 다른 디펜던시
  compile "com.facebook.fresco:fresco:{{site.current_version}}+"
  compile "com.facebook.fresco:imagepipeline-okhttp:{{site.current_version}}+"
}
```

##### Eclipse에서 OkHttp 사용하기

Eclipse 사용자는 [이클립스 세팅하기](index.html#eclipse-adt)에서 소개한 것 처럼  `frescolib`폴더의 `fresco` 와 `imagepipeline-okhttp` 디렉토리 **둘 다** 의존해야 합니다.

##### 이미지 파이프라인을 OkHttp와 사용하는 방법

이미지 파이프라인을 조금 다르게 구성을 해야합니다. `ImagePipelineConfig.newBuilder` 대신 `OkHttpImagePipelineConfigFactory`를 사용합니다.

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

### 당신만의 Network fetcher 사용하는 방법(옵션)

당신의 앱에서 Network layer를 완벽하게 제어하는 방법을 제공할 수 있습니다. Network 통신을 제어하는 [NetworkFetcher](../javadoc/reference/com/facebook/imagepipeline/producers/NetworkFetcher.html)를 subclass하여 사용해야합니다. 또한 Data structure 구체적인 정보를 요청하기 위해서는 [FetchState](../javadoc/reference/com/facebook/imagepipeline/producers/FetchState.html)를 subclass하여 사용하면 됩니다.

`HttpURLConnection`의 기본 구현은 예제에 있습니다. [소스코드](https://github.com/facebook/fresco/blob/master/imagepipeline-backends/imagepipeline-okhttp/src/main/java/com/facebook/imagepipeline/backends/okhttp/OkHttpNetworkFetcher.java)를 보세요.

이미지 파이프라인을 [구성할 때](configuring-image-pipeline.html), 반드시 network producer를 이미지 파이프라인에 전달 해야 합니다.

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
  .setNetworkFetcher(myNetworkFetcher);
  . // other setters
  .build();
Fresco.initialize(context, config);
```
