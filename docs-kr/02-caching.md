---
id: caching
title: 캐싱
layout: docs-kr
permalink: /docs-kr/caching.html
prev: configure-image-pipeline.html
next: using-image-pipeline.html
---

###  세가지 캐시

#### 1. 비트맵 캐시

비트맵 캐시는 안드로이드 `Bitmap`객체 입니다. 이것은 [후처리(postprocessing)](modifying-image.html)나 화면에 표시하기 위해 완전히 디코드되어 준비된 이미지입니다.

안드로이드 4.x나 더 낮은 버전에선, 비트맵 캐시는 자바 힙이 아닌 *ashmem* 힙 영역에 있습니다. 이는 이미지는 추가적인 가비지 컬렉터 수행을 강제하지 않아 당신의 앱을 느리게 하는 요인이 될 수 있습니다.

안드로이드 5.0은 이전 버전에 비해 엄청나게 향상된 메모리 관리기법을 가지고 있습니다. 그래서 자바 힙에 비트맵 캐시를 더 안전하게 남겨놀 수 있습니다.

앱이 백그라운드에서 돌 때, 비트맵 캐시는 비워집니다.

#### 2. 인코드된 메모리 캐시

이 메모리 캐시는 원본의 압축된 형식으로 저장합니다. 이미지는 화면에 표시되기 전에 이 캐시에서 디코드되어 가져옵니다.

[리사이징, 회전](resizing-rotating.html) 이나 [트랜스코딩(지원되지 않는 형식을 변환하는 프로세스)](#webp) 같은 다른 가져오기 형식으로 요청될 땐 디코드하기 전 수행합니다.

이 캐시는 앱이 백그라운드로 가면 비워집니다.

#### 3. 디스크 캐시

물론 폰엔 디스크가 없습니다. 하지만 *로컬 저장 캐시*를 계속 언급하는 것도 따분한 일입니다.

인코드된 메모리캐시 같이 디코드 되거나 가끔 화면에 표시하기 전 변환되어야 하는 압축된 이미지를 제공합니다.

다른 것 과는 달리 이 캐시는 앱이 백그라운드 상태거나, 종료, 장치가 꺼졌을 때 없어지지 않습니다. 물론, 사용자는 안드로이드 설정메뉴에서 이것을 없앨 수 있습니다.

### 몇개의 디스크 캐시를 써야 하나?

대부분의 앱은 디스크 캐시가 한개만 필요합니다. 하지만 어떤 조건에선 더 작은 이미지를 분리된 캐시에서 계속 유지하거나, 큰 이미지에 의해 너무 빨리 없어지는 것을 막고 싶을 수 있습니다.

[이미지 파이프 라인을 설정](configure-image-pipeline.html)할 때, 다음의 두 메소드`setMainDiskCacheConfig`, `setSmallImageDiskCacheConfig`를 사용하세요.

*작은 이미지*는 어떻게 정의할까요? [이미지 리퀘스트를 만들 때](image-requests.html)나 [ImageType](../javadoc/reference/com/facebook/imagepipeline/request/ImageRequest.ImageType.html)을 설정하면 됩니다:

```java
ImageRequest request = ImageRequest.newBuilderWithSourceUri(uri)
    .setImageType(ImageType.SMALL)
```

하나의 캐시만 필요하다면, 단순히 `setSmallImageDiskCacheConfig`를 호출하면 됩니다. 이미지 파이프라인은 `ImageType`을 무시한 채 같은 캐시만 쓸 것입니다.

### 캐시 제거

[이미지 파이프라인을 설정할 때](configure-image-pipeline.html) 각 캐시의 최대 갯수를 고를 수 있습니다. 그 외에도 최대 갯수보다 적게 쓰고싶을 때도 있을 것입니다. 예를 들면 당신의 앱이 더 많은 공간과 복잡한 데이터 종류를 캐시하고 싶을 수 있습니다. 아니면 용량이 가득 찼는지 저장소를 체크하고 싶을 수 있습니다.

Fresco 캐시는 [DiskTrimmable](../javadoc/reference/com/facebook/common/disk/DiskTrimmable.html)나 [MemoryTrimmable](../javadoc/reference/com/facebook/common/memory/MemoryTrimmable.html)인터페이스를 구현합니다. 이것들은 앱에 긴급하게 캐시를 정리하라고 전달 할 수 있습니다.

당신의 앱은 이 [DiskTrimmableRegistry](../javadoc/reference/com/facebook/common/disk/DiskTrimmableRegistry.html)와 [MemoryTrimmableRegistry](../javadoc/reference/com/facebook/common/memory/MemoryTrimmableRegistry.html) 인터페이스를 구현한 객체로 파이프라인을 설정할 수 있습니다.

이 객체들은 제거가능하도록 유지되어야 합니다. 이것들은 메모리나 디스크 공간이 반드시 유지되도록 앱의 상세한 로직을 사용해야만 합니다. 그러면 이것들은 이 제거가능한 객체들에 제거명령을 통보 할 수 있습니다.
