---
id: using-image-pipeline
title: 이미지 파이프라인 직접 사용하기
layout: docs-kr
permalink: /docs-kr/using-image-pipeline.html
prev: caching.html
next: datasources-datasubscribers.html
---
이 페이지는 고급 개발 용입니다. 대부분의 앱은 프레스코의 이미지 파이프라인과 상호작용 하려면 [Drawees](using-drawees-xml.html)를 사용해야 합니다.

직접 이 이미지 파이프라인을 이용하는 것은 메모리 사용 능력을 시험하는 것 입니다. Drawee는 자동으로 당신의 이미지가 메모리에 있어야 하는지 자동으로 추적합니다. 프레스코는 메모리의 이미지를 교환하고 화면에 표시되어야 할 때 곧 바로 불러옵니다. 만약 직접 이미지 파이프라인을 사용하고 있다면 당신의 앱은 이 로직을 반복할 수 밖에 없습니다.

이 이미지 파이프라인은 [CloseableReference](closeable-references.html)를 감싼 객체를 돌려줍니다. Drawee는 작업이 완료되면 이 객체의 `.close()`를 호출합니다. 만약 당신의 앱이 Drawee를 사용하지 않는 다면 이와 비슷한 작업을 해야만 합니다.

자바의 가비지 컬렉터는 비트맵 객체가 스코브를 벗어나면 이미지 메모리를 해제합니다. 하지만 매우 늦게 작동할 수도 있습니다. 가비지 컬렉션은 비용이 많이 나가고 큰 객체를 다룰 때 성능 이슈도 있습니다. 이것은 특히 안드로이드 4.x나 하위버전에서 분리된 비트맵 메모리 공간을 유지하지 않았을 때 발생합니다.

#### 파이프라인 호출하기

[이미지 리퀘스트 빌드하기](image-requests.html)는 필수입니다. 완료되면  `ImagePipeline`에 직접 이것을 전달 할 수 있습니다.

```java
ImagePipeline imagePipeline = Fresco.getImagePipeline();
DataSource<CloseableReference<CloseableImage>>
    dataSource = imagePipeline.fetchDecodedImage(imageRequest);
```
 어떻게 이미지 파이프라인에서 데이터를 받는지는 [DataSources](datasources-datasubscribers.html)를 보세요.

#### 디코드 건너 뛰기

이미지를 디코드하기 싫고 원본 압축 형식을 사용하고 싶다면 `fetchEncodedImage`를 사용하세요 :

```java
DataSource<CloseableReference<PooledByteBuffer>>
    dataSource = imagePipeline.fetchEncodedImage(imageRequest);
```

#### 빠르게 비트맵 캐시 인스턴스 가져오기

비트맵 캐시는 다른 것과는 다르게 UI 스레드에서 불러오기가 완료됩니다. 비트맵이 여깃다면 즉시 인스턴스를 갖고 올 수 있습니다.

```java
DataSource<CloseableReference<CloseableImage>> dataSource =
    imagePipeline.fetchImageFromBitmapCache(imageRequest);
try {
  CloseableReference<CloseableImage> imageReference = dataSource.getResult();
  if (imageReference != null) {
    try {
      CloseableImage image = imageReference.get();
      // do something with the image
    } finally {
      CloseableReference.closeSafely(imageReference);
    }
  }
} finally {
  dataSource.close();
}
```

`finally` 블록을 **건너 뛰면 안됩니다**.

#### [Prefetching](http://endic.naver.com/enkrEntry.nhn?sLn=kr&entryId=b6d41d5ce7d541b7a0975487276f6bab&query=prefetch)

이미지가 보여지기 전에 미리 판독하는 것은 가끔 사용자에게 더 적은 대기시간을 제공할 수 있습니다. 하지만 트레이드-오프를 생각해야 합니다. Prefetch는 사용자의 데이터를 취하고, CPU와 메모리를 공유합니다. 일반적으로 prefetching은 대부분의 앱에서는 추천되지 않습니다.

그럼에도 불구하고, 이미지 파이프라인은 디스크와 비트맵 캐시를 prefetch할 수 있습니다. 두개 다 네트워크 URI를 위해 더 많은 데이터를 이용합니다. 하지만 이 디스크 캐시는 디코드를 하지 않기 때문에 더 적은 CPU를 이용합니다.

디스크에서 prefetch하기 :

```java
imagePipeline.prefetchToDiskCache(imageRequest);
```

비트맵 캐시 Prefetch:

```java
imagePipeline.prefetchToBitmapCache(imageRequest);
```
