---
id: configure-image-pipeline
title: 이미지 파이프라인 구성하기
layout: docs-kr
permalink: /docs-kr/configure-image-pipeline.html
prev: intro-image-pipeline.html
next: caching.html
---

대부분의 앱은 Fresco를 간단한 명령으로 초기화 할 수 있습니다.

```java
Fresco.initialize(context);
```

더 향상된 커스텀을 필요로 하는 앱들은 [ImagePipelineConfig](../javadoc/reference/com/facebook/imagepipeline/core/ImagePipelineConfig.html)클래스를 사용할 수 있습니다.

여기에 최고의 예제가 있습니다. 극소수의 앱만 이러한 설정이 필요하겠지만 그들을 위해 레퍼런스를 만들었습니다.

```java
ImagePipelineConfig config = ImagePipelineConfig.newBuilder()
    .setBitmapMemoryCacheParamsSupplier(bitmapCacheParamsSupplier)
    .setCacheKeyFactory(cacheKeyFactory)
    .setEncodedMemoryCacheParamsSupplier(encodedCacheParamsSupplier)
    .setExecutorSupplier(executorSupplier)
    .setImageCacheStatsTracker(imageCacheStatsTracker)
    .setMainDiskCacheConfig(mainDiskCacheConfig)
    .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
    .setNetworkFetchProducer(networkFetchProducer)
    .setPoolFactory(poolFactory)
    .setProgressiveJpegConfig(progressiveJpegConfig)
    .setRequestListeners(requestListeners)
    .setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
    .build();
Fresco.initialize(context, config);
```

`ImagePipelineConfig` 객체를 `Fresco.initialize`에 꼭 전달하세요. 그렇지 않으면 Fresco는 당신이 만든 것 대신 기본 설정을 사용할겁니다.

### 공급자(Supplier) 이해하기

몇몇 설정 빌더 메소드는 자기 자체 인스턴스보다 [Supplier](../javadoc/reference/com/facebook/common/internal/Supplier.html) 인스턴스를 사용합니다.

이는 만들기 좀 복잡해 보이지만, 당신의 앱이 실행 중 일때 동작방식을 바꿀 수 있도록 합니다. 메모리캐시는 Supplier를 5분 마다 체크합니다.

파라미터를 직접 바꿀 필요가 없다면 매 시간마다 같은 객체를 돌려주는 Supplier를 사용하세요

```java
Supplier<X> xSupplier = new Supplier<X>() {
  public X get() {
    return new X(xparam1, xparam2...);
  }
);
//이미지 파이프라인이 만들어질 때
.setXSupplier(xSupplier);
```

### 쓰레드 풀

기본적으로 이미지 파이프라인은 3가지 쓰레드 풀을 사용합니다:

1. 네트워크 파일을 내려받을 3개의 쓰레드
2. 모든 디스크 동작을 위한 두개의 쓰레드 - 로컬파일 읽기, 디스크캐시
3. CPU기반 작업- 디코드, 모양 변환, 백그라운드 작업, 후처리 작업등 -을 위한 두개의 쓰레드

[네트워크 레이어 설정](using-other-network-layers.html)으로 네트워크 동작을 커스텀할 수 있습니다.

다른 동작을 바꾸려면 [ExecutorSupplier](../javadoc/reference/com/facebook/imagepipeline/core/ExecutorSupplier.html)인스턴스를 전달 하세요.

### 메모리캐시 구성하기

비트맵 캐시와 인코드된 메모리 캐시는 [MemoryCacheParams](../javadoc/reference/com/facebook/imagepipeline/cache/MemoryCacheParams.html#MemoryCacheParams\(int, int, int, int, int\)) 객체의 공급자(Supplier)로 설정될 수 있습니다.

### 디스크 캐시 구성하기

[DiskCacheConfig](../javadoc/reference/com/facebook/cache/disk/DiskCacheConfig.Builder.html) 객체를 만들기 위해 빌더패턴을 사용할 수 있습니다.


```java
DiskCacheConfig diskCacheConfig = DiskCacheConfig.newBuilder()
   .set....
   .set....
   .build()

//ImagePipelineConfig 만들 때
.setMainDiskCacheConfig(diskCacheConfig)
```

### 캐시 상태 유지하기

캐시 히트율을 계속 추적하고 싶다면 [ImageCacheStatsTracker](../javadoc/reference/com/facebook/imagepipeline/cache/ImageCacheStatsTracker.html) 를 이용하세요. 이것은 모든 캐시 이벤트의 콜백을 제공하고 당신은 통계를 내는데 사용할 수 있습니다.
