---
id: image-requests
title: 이미지 리퀘스트
layout: docs-kr
permalink: /docs-kr/image-requests.html
prev: modifying-image.html
next: writing-custom-views.html
---
URI만 있는 `ImageRequest`가 필요하다면, `ImageRequest.fromURI`의 헬퍼메소드를 이용할 수 있습니다. 일반적인 경우 [여러개의 이미지를 요청](requesting-multiple-images.html)할 수 있습니다.

단순히 URI 요청하는 것 외에 더 많은게 필요하다면 `ImageRequestBuilder`로 이미지 파이프라인을 이용하세요.

```java
Uri uri;

ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
    .setBackgroundColor(Color.GREEN)
    .build();

ImageRequest request = ImageRequestBuilder
    .newBuilderWithSource(uri)
    .setAutoRotateEnabled(true)
    .setLocalThumbnailPreviewsEnabled(true)
    .setLowestPermittedRequestLevel(RequestLevel.FULL_FETCH)
    .setProgressiveRenderingEnabled(false)
    .setResizeOptions(new ResizeOptions(width, height))
    .build();
```

#### ImageRequest 필드

- `uri` - 필수 필드입니다. [지원하는 URIs](supported-uris.html) 참조하세요
- `autoRotateEnabled` - [자동 회전하기](resizing--rotating.html#rotate)을 결정.
- `progressiveEnabled` - [진보적인 불러오기](progressive-jpegs.html)을 결정.
- `postprocessor` - 디코딩된 이미지를 [후처리(postprocess)](modifying-image.html) 하는 컴퍼넌트.
- `resizeOptions` - 원하는 가로와 세로 길이.주의해서 사용하세요.[리사이징](resizing-rotating.html) 참조.

#### 제일 낮은 수준으로 허가된 요청 레벨

이미지 파이프라인은 이미지를 찾을 때 [정의된 순서](intro-image-pipeline.html) 를 따릅니다.

1. 비트맵 캐시를 체크합니다. 빠르며 발견되면 돌려줍니다.
2. 인코드된 메모리 캐시를 체크합니다. 발견되면 이미지를 디코드하고 돌려줍니다.
3. 디스크(로컬 스토리지) 캐시를 체크합니다. 발견하면 디스크에서 가져와 디코드하고 돌려줍니다.
4. 네트워크나 로컬의 원본을 찾습니다. 가져오거나 리사이즈, 회전하고 디코드해 돌려줍니다. 네트워크 이미지의 경우 아주 느릴 수 있습니다.

The `setLowestPermittedRequestLevel` 속성은 어느 수준까지 이 파이프라인이 비트맵을 찾을 것인지 조절할 수 있습니다. 아래는 쓸 수 있는 값들 입니다.

- `BITMAP_MEMORY_CACHE`
- `ENCODED_MEMORY_CACHE`
- `DISK_CACHE`
- `FULL_FETCH`

이미지가 빠르게, 혹은 상대적으로 빨리 필요하거나 그렇지 않을 때 유용합니다.
