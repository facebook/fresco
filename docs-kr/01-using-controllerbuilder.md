---
id: using-controllerbuilder
title: ControllerBuilder 사용하기
layout: docs-kr
permalink: /docs-kr/using-controllerbuilder.html
prev: rounded-corners-and-circles.html
next: progressive-jpegs.html
---

`SimpleDraweeView` 는 이미지 처리를 위해 두 메소드를 제공합니다. 가장 쉬운 방법은 `setImageURI`를 호출하는 겁니다.

Drawee를 이용해 여러가지 방법으로 이미지 처리를 하고 싶다면, [DraweeController](concepts.html)를 사용할 수 있습니다. 이 페이지는 이 빌더를 어떻게 빌드하고 사용하는지 설명합니다.

### DraweeController 빌드하기

[PipelineDraweeControllerBuilder](../javadoc/reference/com/facebook/drawee/backends/pipeline/PipelineDraweeControllerBuilder.html)에 이미지 요청을 전달해봅시다. 몇가지 옵션을 이 컨트롤러에 추가할 수 있습니ㅏ.

```java
ControllerListener listener = new BaseControllerListener() {...}

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setTapToRetryEnabled(true)
    .setOldController(mSimpleDraweeView.getController())
    .setControllerListener(listener)
    .build();

mSimpleDraweeView.setController(controller);
```

새로운 컨트롤러를 빌드할 때, 항상 `setOldController`를 호출하세요.이것은 불필요한 메모리 할당을 방지합니다.

더 자세히 보기:

* [Controller Listeners](listening-download-events.html)

### <a name="ImageRequest"></a>이미지 요청 커스터마이징
여전히 몇가지 사용법이 더 있습니다. [ImageRequest](../javadoc/reference/com/facebook/imagepipeline/request/ImageRequest.html)를 단순히 URI대신에 파이프라인에 전달하는 것이 필요할 수도 있는데요. [postprocessor](modifying-image.html)에 예제가 있습니다.

```java
Uri uri;
Postprocessor myPostprocessor = new Postprocessor() { ... }
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setPostprocessor(myPostprocessor)
    .build();

DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    // other setters as you need
    .build();
```

더 자세히 보기:

* [Postprocessors](modifying-image.html)
* [여러 이미지 요청하기](requesting-multiple-images.html)
* [리사이징하거나 회전하기](resizing-rotating.html)
