---
id: listening-download-events
title: 다운로드 이벤트 리스닝
layout: docs-kr
permalink: /docs-kr/listening-download-events.html
prev: requesting-multiple-images.html
next: resizing-rotating.html
---

#### 동기

당신의 앱은 이미지 불러오기가 완료되때 어떤 액션 - 뷰를 보이게 한다던가 캡션을 보이게 한다던가 -을 하도록 원할 수 있습니다. 네트워크가 연결되지 않을 때 에러메시지를 보여주는 것 처럼 어떤 것을 원할 수도 있습니다.

물론 이미지를 불러오는 것은 비동기 작업입니다. 그래서 DraweeController에 게시해 놓은 몇가지 이벤트를 잡을 수 있는 방법이 필요합니다. 컨트롤러 리스너가 이것의 작동원리입니다.

*주의: 이것은 이미지 그 자체를 수정하지 못합니다. 이미지를 수정하려면 [Postprocessor](modifying-image.html)를 사용하세요.*

#### 사용법

리스너를 사용하려면 `ControllerListener` 인터페이스 인스턴스만 있으면 됩니다. 하위 클래스인 `BaseControllerLIstener`를 추천합니다.

```java
ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable ImageInfo imageInfo,
        @Nullable Animatable anim) {
      if (imageInfo == null) {
        return;
      }
      QualityInfo qualityInfo = imageInfo.getQualityInfo();
      FLog.d("Final image received! " +
          "Size %d x %d",
          "Quality level %d, good enough: %s, full quality: %s",
          imageInfo.getWidth(),
          imageInfo.getHeight(),
          qualityInfo.getQuality(),
          qualityInfo.isOfGoodEnoughQuality(),
          qualityInfo.isOfFullQuality());
    }

    @Override
    public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
      FLog.d("Intermediate image received");
    }

    @Override
    public void onFailure(String id, Throwable throwable) {
      FLog.e(getClass(), throwable, "Error loading %s", id)
    }
};

Uri uri;
DraweeController controller = Fresco.newControllerBuilder()
    .setControllerListener(controllerListener)
    .setUri(uri);
    // other setters
    .build();
mSimpleDraweeView.setController(controller);
```

`onFinalImageSet` 이나 `onFailure`에서 모든 이미지불러오기가 호출됩니다.

만약 [진보된 이미지](progressive-jpegs.html)가 사용가능 하고 그 이미지가 지원된다면 각각의 디코딩되는 스캔에 대해 `onIntermediateImageSet`이 호출됩니다. 디코딩 된 것은 [설정](progressive-jpegs.html)에서 결정됩니다.
