---
id: using-other-image-loaders
title: 다른 이미지 로더 사용하기
layout: docs-kr
permalink: /docs-kr/using-other-image-loaders.html
prev: using-other-network-layers.html
---

Drawee is not tied to a particular image loading mechanism and can be used with other image loaders.
Drawee는 특정한 이미지 로딩 메카니즘과 밀접하지 않고, 다른 이미지 로더들과 함께 사용이 가능합니다.

However, some of its features are only available on the Fresco image pipeline. Any feature in the preceding pages that required using an [ImageRequest](image-requests.html) or [configuration](configure-image-pipeline.html) may not work with a different loader.
그러나, 어떤 특징들은 오직 Fresco의 이미지 파이프라인에서만 가능합니다. [ImageRequest](image-requests.html) 나 [구성하기](configure-image-pipeline.html)를 사용한 이전 페이지의 모든 특징들은 다른 로더와 사용할 떄 작동하지 않을 수 있습니다.

### Volley ImageLoader와 Drawee 사용하기
Drawee 를 Volley 이미지 로더와 함께 사용하는 방법

We have an backend for Drawee that allows Volley's [ImageLoader](https://developer.android.com/training/volley/request.html) to be used instead of Fresco's image pipeline.
Fresco의 이미지 파이프 라인 대신 Volley의 [ImageLoader](https://developer.android.com/training/volley/request.html)를 Drawee를 위한 백엔드로 대신 사용가능합니다.

Volley ImageLoader를 많이 사용했을 때 이용할 것을 추천합니다.

사용하는 방법은 프로젝트의 `build.gradle` 파일에서 `dependencies` 섹션을 수정해야 합니다. [다운로드](download-fresco.html) 페이지에서 제공되어지는 Gradle dependencies를 사용하면 **안됩니다.**
대신에 이것을 사용하세요:

```groovy
dependencies {
  // your project's other dependencies
  compile: "com.facebook.fresco:drawee-volley:{{site.current_version}}+"
}
```

#### Volley 이미지로더 초기화

`Fresco.initialize` 를 호출하면 안됩니다. 반드시 Volley의 이미지 파이프라인을 사용해야 합니다.

```java
Context context;
ImageLoader imageLoader; // build yourself
VolleyDraweeControllerBuilderSupplier mControllerBuilderSupplier
    = new VolleyDraweeControllerBuilderSupplier(context, imageLoader);
SimpleDraweeView.initialize(mControllerBuilderSupplier);
```

`VolleyDraweeControllerBuilderSupplier`가 스코프를 벗어나면 안됩니다. `SimpleDraweeView.setImageURI.` 를 사용하지 않는다면, 항상 빌드 컨트롤러를 사용해야 합니다.

#### Volley ImageLoader와 함께 DraweeControllers 사용하기
DraweeControllers 를 Volley 이미지로더와 함께 사용하는 방법

`Fresco.newControllerBuilder` 대신 호출하는 방법,

```java
VolleyController controller = mControllerBuilderSupplier
    .newControllerBuilder()
    . // setters
    .build();
mSimpleDraweeView.setController(controller);
```

### 다른 이미지로더와 Drawee 함께 사용하기
Drawee를 다른 이미지 로더와 함께 사용하는 방법

다른 Drawee 백엔드들은 아직 준비되지 않았지만, [Volley 예제](https://github.com/facebook/fresco/tree/master/drawee-backends/drawee-volley/src/main/java/com/facebook/drawee/backends/volley) 모델을 참고하면 가능할 수도 있습니다.
