---
id: progressive-jpegs
title: 진보된 JPEGs
layout: docs-kr
permalink: /docs-kr/progressive-jpegs.html
prev: using-controllerbuilder.html
next: animations.html
---
Q. progressive image 란 개선된 이미지(jpg, png)타입인가 아니면 개선된 이미지 처리 방식인가?

Fresco 는 개선된 JPEG이미지 스트리밍을 지원합니다.

이미지를 보려면 다운로드 받은 후 볼 수 있습니다. 사용자는 이미지의 품질이 낮음에서 시작해 점점 높아짐을 볼 수 있습니다.

이것은 오직 네트워크 이미지에만 지원됩니다. 로컬 이미지는 한번에 복호화됨으로 더 할 것이 없습니다. 또한 모든 JPEG이미지가 개선된 형식의 보여줄 수 있는 방식으로 인코딩되어있진 않습니다. 이런 것들은 미리 화면에 표시할 수 없음을 명심하세요.

#### 이미지 요청 빌드하기

당신은 이미지요청을 빌드하는 동시에 진보적인 렌더링 방법을 명시적으로 요청해야만 합니다.

```java
Uri uri;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setProgressiveRenderingEnabled(true)
    .build();
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    .build();
mSimpleDraweeView.setController(controller);
```
우린 `setImageURI` 로 개선된 이미지를 이용한 지원을 추가할 예정입니다.
