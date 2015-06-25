---
id: animations
title: 움직이는 이미지
layout: docs-kr
permalink: /docs-kr/animations.html
prev: progressive-jpegs.html
next: requesting-multiple-images.html
---
Fresco는 움직이는 GIF와 WebP 이미지도 지원합니다.

OS지원 없이도 안드로이드 2.3 버전까지 WebP 애니메이션 - 확장된 WebP형식까지 - 을 지원합니다.

### 자동으로 애니메이션 시작하기

화면에 나타났을 때 자동으로 애니메이션이 시작되거나, 화면이 꺼졌을 때 멈추기 원한다면  [이미지 요청하기](image-requests.html)를 보세요.

```java
Uri uri;
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setAutoPlayAnimations(true)
    . // 다른 설정들
    .build();
mSimpleDraweeView.setController(controller);
```

### 수동으로 애니메이션 시작하기

애니메이션을 코드에서 직접 조종하는 것을 더 선호할 수 있습니다. 이 경우에 당신은 이미지 로딩이 완료되었다는 것을 확인해야할 필요가 있습니다. 그래서 이것도 가능하도록 해놓았습니다.

```java
ControllerListener controllerListener = new BaseControllerListener<ImageInfo>() {
    @Override
    public void onFinalImageSet(
        String id,
        @Nullable ImageInfo imageInfo,
        @Nullable Animatable anim) {
    if (anim != null) {
      // app-specific logic to enable animation starting
      anim.start();
    }
};

Uri uri;
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setUri(uri)
    .setControllerListener(controllerListener)
    // 다른 설정들
    .build();
mSimpleDraweeView.setController(controller);
```

컨트롤러는 [움직일 수 있는](http://developer.android.com/reference/android/graphics/drawable/Animatable.html) 인터페이스 인스턴스를 보여줍니다. null만 아니라면 애니메이션을 사용할 수 있습니다.

```java
Animatable animatable = mSimpleDraweeView.getController().getAnimatable();
if (animatable != null) {
  animatable.start();
  // later
  animatable.stop();
}
```

### 제한

애니메이션은 [postprocessors](modifying-image.html)에선 지원하지 않습니다.
