---
id: resizing-rotating
title: 리사이징과 회전
layout: docs-kr
permalink: /docs-kr/resizing-rotating.html
prev: listening-download-events.html
next: modifying-image.html
---

이 특징은 [이미지 리퀘스트 만들기](using-controllerbuilder.html#ImageRequest)를 직접 필요로 합니다.

## 이미지 리사이징

### 용어: 리사이징 vs 스케일링

- **리사이징**은 소프트웨어에서 실행되는 파이프라인 동작입니다. 다른 사이즈의 완전히 새로운 비트맵을 돌려줍니다.
- **스케일링**은 보통 하드웨어 가속으로 캔버스에서 수행됩니다. 비트맵은 항상 같은 크기고 업스케일되거나 다운스케일되어 그려집니다.

### 리사이징할 것이냐, 스케일할 것이냐?

리사이징은 거의 필요하지 않습니다. 리사이징된 스케일링이 보통 선호됩니다.
다음은 리사이징의 몇가지 한계입니다.

  - 리사이징은 더 큰 이미지를 주지 못하도록 제한되어 있습니다. 더 작은 이미지를 만드는 것만 가능합니다.
  - 그 순간에는 JPEG이미지만 리사이즈될 수 있습니다.
  - 결과 이미지의 차원을 조절하는 단순한 방법이 있습니다. 이미지는 정확한 사이즈로 리사이즈할 수 없지만 한 리사이즈 요소를 줄일 수 있습니다. 이는 리사이즈된 이미지는 화면에 표시되기 전에 스케일될 필요가 있다는 것입니다.
  - 다음 리사이즈 요소가 지원됩니다: `N/8` (이 때 `1 <= N <=8`)
  - 리사이즈는 소프트웨어단에서 수행되며 하드웨어가속 방법의 스케일링보다 훨씬 느립니다.

반면에 스케일링은 이런 제한이 없습니다. 스케일링은 뷰 크기에 이미지를 맞추는 안드로이드 시스템만의 방법을 사용합니다. 안드로이드 4.0과 이후 버전에서 장치의 GPU를 이용한  하드웨어 가속방법이 적용됩니다. 대부분 원하는 크기의 이미지를 화면에 표시하는데 효율적인 방법입니다. 단점은 이미지가 뷰의 크기보다 훨씬 크다면 메모리가 낭비 되는 점입니다.

왜 리사이징을 사용해야 할까요? 트레이드 오프 때문입니다. 메모리를 절약하며 뷰크기보다 훨씬 큰 이미지를 표시할 필요가 있다면 항상 리사이즈를 이용해야 합니다. 한 적절한 예제로 8MP사진을 1280*720(대략 1MP) 뷰에 표시하고 싶을 때 입니다. 8MP이미지는 픽셀당 4byte인 ARGB 비트맵으로 디코드할 때 32MB의 메모리를 점유합니다. 만약 이 뷰의 크기에 맞게 리사이즈하면 4MB보다 적은 크기를 점유하게 됩니다.

리사이징을 생각 하기전에는, 이미지가 네트워크에서 온다면, 적절한 사이즈의 이미지를 요청할 것입니다. 더 작은 사진을 요청할 수 있다면, 서버의 8MB 고해상도 사진을 요청하면 안됩니다. 당신의 사용자는 싼 요금제를 사용할 수 있고 이것을 고려해야 합니다. 게다가 더 작은 이미지를 불러오는 것은 앱의 내부 저장소와 CPU 작동시간을 절약할 수 있습니다.

서버가 더 작은 이미지의 대체 URI를 제공하지 못한다거나, 로컬의 사진을 이용한다면 리사이징을 이용하세요. 이미지를 업스케일링하는 것을 포함한 다른 모든 경우엔 스케일링이 사용되야 합니다. 스케일하려면, 다른 Android 뷰와 같은 방식으로 간단하게 `SimpleDraweeView`의 `layout_width`와 `layout_height`를 설정한 후, [스케일 타입]을 지정하세요.

### 리사이징

리사이징은 원본 파일을 수정하지 않습니다. 리사이징은 디코드 되기 전 메모리의 이미지 크기를 변경할 뿐입니다.

안드로이드 시스템에선 더 큰 범위의 리사이징을 가능하도록 합니다. 어떤 경우 장치의 카메라로 촬영된 이미지는 종종 스케일하기엔 너무 큰 나머지 화면에 표시되기 전에 리사이즈 될 필요가 있습니다.

현재 JPEG 형식의 이미지만 리사이징을 지원하긴 하지만, 어쨋든 이 형식은 가장 널리 사용되는 이미지 형식이고 대부분의 카메라가 있는 안드로이드 장치가 JPEG형식으로 파일을 저장합니다.

`ImageRequest` 오브젝트를 만들 때, 리사이즈하기 위해 [ResizeOptions](../javadoc/reference/com/facebook/imagepipeline/common/ResizeOptions.html)을 전달하세요.

```java
Uri uri = "file:///mnt/sdcard/MyApp/myfile.jpg";
int width = 50, height = 50;
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
PipelineDraweeController controller = Fresco.newDraweeControllerBuilder()
    .setOldController(mDraweeView.getController())
    .setImageRequest(request)
    .build();
mSimpleDraweeView.setController(controller);
```

## <a name="rotate"></a>자동 회전

이미지를 옆으로 보는 건 아주 짜증나는 일입니다. 많은 장치들이 JPEG파일의 메타데이터에 방향을 저장합니다. 만약 이미지가 장치의 방향에 맞게 자동으로 회전되길 원한다면 이미지 요청할 때 다음과 같이 사용할 수 있습니다.

```java
ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .build();
// 위쪽의 코드
```
