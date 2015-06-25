---
id: datasources-datasubscribers
title: 데이터소스와 데이터 구독자(DataSubscribers)
layout: docs-kr
permalink: /docs-kr/datasources-datasubscribers.html
prev: using-image-pipeline.html
next: closeable-references.html
---

비동기 연산의 결과인 [DataSource](../javadoc/reference/com/facebook/datasource/DataSource.html)는 자바의 [Future](http://developer.android.com/reference/java/util/concurrent/Future.html)와 같습니다. Future와 다른것은, DataSource는 하나의 명령에서 모든 결과 목록을 가져올 수 있지만 하나만 돌려줍니다.

이미지 요청 후에, 이미지 파이프라인은 데이터 소스를 돌려줍니다. 이를 얻으려면 [DataSubscriber](../javadoc/reference/com/facebook/datasource/DataSubscriber.html)가 필요합니다.

### 그냥 비트맵만 필요하다면...

이미지 파이프라인에 디코드된 이미지 - 안드로이드의 [Bitmap](http://developer.android.com/reference/android/graphics/Bitmap.html) - 를 요청하고 싶다면, [BaseBitmapDataSubscriber](../javadoc/reference/com/facebook/imagepipeline/datasource/BaseBitmapDataSubscriber)를 사용하세요.

```java
dataSource.subscribe(new BaseBitmapDataSubscriber() {
    @Override
    public void onNewResultImpl(@Nullable Bitmap bitmap) {
	  // 제한된 방법으로 이미지를 사용할 수 있습니다.
      // No cleanup required here.
    }

    @Override
    public void onFailureImpl(DataSource dataSource) {
      // No cleanup required here.
    }
  },
  executor);
```

사용 예제 입니다. 저작권보호 신청되있어요.

`onNewResultImpl`메소드의 스코프에 어떤 비트맵 변수도 할당하면 **안 됩니다**.
이유는 이 subscriber가 실행이 끝난 뒤 이미지 파이프라인은 비트맵을 재활용하고 메모리를 해제해버립니다. 만약 이후에 비트맵을 그리려고 한다면 앱은 곧 `IllegalStateException`을 발생 시킬겁니다.

안드로이드  [notification](https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#setLargeIcon\(android.graphics.Bitmap\))이나 [remote view](http://developer.android.com/reference/android/widget/RemoteViews.html#setImageViewBitmap\(int, android.graphics.Bitmap\))에 안전하게 비트맵을 전달할 수 있습니다. 안드로이드가 시스템 처리에 필요한 비트맵이 필요하다면, ashmem에 있는 비트맵데이터의 복사본(프레스코가 같은 힙에서 사용되는 것)을 만들 것입니다. 그래서 프레스코의 자동 정리 기법은 이슈없이 동작할 것입니다.


### 일반적인 목적의 솔루션

raw 비트맵을 사용해 비트맵을 유지할 수는 없습니다. [closeable references](closeable-references.html)나 [BaseDataSubscriber](../javadoc/reference/com/facebook/datasource/BaseDataSubscriber.html)을 사용해야만 합니다:

```java
DataSubscriber dataSubscriber =
    new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
  @Override
  public void onNewResultImpl(
      DataSource<CloseableReference<CloseableImage>> dataSource) {

    if (!dataSource.isFinished()) {
      FLog.v("Not yet finished - this is just another progressive scan.");
    }

    CloseableReference<CloseableImage> imageReference = dataSource.getResult();
    if (imageReference != null) {
      try {
        CloseableImage image = imageReference.get();
        // do something with the image
      } finally {
        imageReference.close();
      }
    }
  }
  @Override
  public void onFailureImpl(DataSource dataSource) {
    Throwable throwable = dataSource.getFailureCause();
    // handle failure
  }
};

dataSource.subscribe(dataSubscriber, executor);
```

위의 예제를 쓰고 싶지 않고 `CloseableReference`를 다른데서 할당하고 싶으면 할 수 있습니다. [규칙 따르기](closeable-references.html)를 확인하세요.
