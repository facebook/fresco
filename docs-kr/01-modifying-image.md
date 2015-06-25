---
id: modifying-image
title: 이미지 수정하기
layout: docs-kr
permalink: /docs-kr/modifying-image.html
prev: resizing-rotating.html
next: image-requests.html
---

#### 이유

가끔 서버에서 이미지를 다운받거나 로컬 저장소에서 가져온 이미지가 화면에 나타내고 싶은 이미지가 아닐 수도 있습니다. 만약 이미지에 커스텀 코드를 적용하고 싶다면 Postprocessor를 사용하세요. [BasePostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BasePostprocessor.html) 섭클래스를 사용하면 제일 좋습니다.

#### 예제

아래 예제는 빨간 그물을 이미지에 적용합니다.

```java
Uri uri;
Postprocessor redMeshPostprocessor = new BasePostprocessor() {
  @Override
  public String getName() {
    return "redMeshPostprocessor";
  }

  @Override
  public void process(Bitmap bitmap) {
    for (int x = 0; x < bitmap.getWidth(); x+=2) {
      for (int y = 0; y < bitmap.getHeight(); y+=2) {
        bitmap.setPixel(x, y, Color.RED);
      }
    }
  }
}

ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
    .setPostprocessor(redMeshPostprocessor)
    .build();

PipelineDraweeController controller = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setOldController(mSimpleDraweeView.getController())
    // 필요한 다른 설정
    .build();
mSimpleDraweeView.setController(controller);
```

#### 알아야 할 것

이미지는 postprocessor가 사용되기 전 복사됩니다. 캐시의 이미지지 복사본은 당신의  postprocessor로 바뀐 어떤 것도 *적용되지 않습니다*. 안드로이드 4.x 나 더 낮은 버전에서 복사본은 원본이미지처럼 Java 힙의 외부에 저장됩니다.

만약 같은 이미지를 반복적으로 보여준다면, 요청되었을 때마다 postprocessor를 명시해야합니다. 같은 이미지에 대해 다른 요청의 다른 postprocessor를 사용하는 것은 당신 마음입니다.

Postprocessor는 [움직이는 이미지](animations.html)엔 현재 지원되지 않습니다.

#### 비트맵 복사하기

post-process 내부에 몇몇 동작하지 않는 경우가 있을 수 있습니다. `BasePostprocessor`는 두가지 인자를 취하는 두번 째 `process`메소드가 있습니다. 이 예제는 비트맵을 수평으로 뒤집습니다.

```java
@Override
public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
  for (int x = 0; x < destBitmap.getWidth(); x++) {
    for (int y = 0; y < destBitmap.getHeight(); y++) {
      destBitmap.setPixel(x, y, sourceBitmap.getPixel(x, y));
    }
  }
}
```

원본과 수정본은 같은 크기입니다.

* 원본 비트맵을 **수정하지 마세요**. 추후에 예외를 던지도록 처리할거니까요.
* 원본, 수정본 중 어느 하나도 계속 참조하지 마세요. 둘 다 이미지 파이프라인에서 메모리가 관리됩니다. 수정된 비트맵은 Drawee나 DataSource에서 정상적으로 보여집니다 - The destBitmap will end up in your Drawee or DataSource normally.-.

#### 다른 크기로 복사하기

후처리된 이미지가 원본과 다른 크기가 되야 한다면, 세 번째 `process`메소드가 있습니다. [PlatformBitmapFactory](../javadoc/reference/com/facebook/imagepipeline/bitmaps/PlatformBitmapFactory.html) 클래스를 사용해 원하는 크기의 비트맵을 안전하게 만들고 자바 힙에서 떨어트리세요.

아래 예제는 이미지를 4/1 크기로 줄입니다.

```java
@Override
public CloseableReference<Bitmap> process(
    Bitmap sourceBitmap,
    PlatformBitmapFactory bitmapFactory) {
  CloseableReference<Bitmap> bitmapRef = bitmapFactory.createBitmap(
      sourceBitmap.getWidth() / 2,
      sourceBitmap.getHeight() / 2);
  try {
    Bitmap destBitmap = bitmapRef.get();
	 for (int x = 0; x < destBitmap.getWidth(); x+=2) {
	   for (int y = 0; y < destBitmap.getHeight(); y+=2) {
	     destBitmap.setPixel(sourceBitmap.getPixel(x, y));
	   }
	 }
	 return CloseableReference.cloneOrNull(bitmapRef);
  } finally {
    CloseableReference.closeSafely(bitmapRef);
  }
}
```

[closeable references](closeable-references.html)의 규칙은 지켜야 합니다.

자바 힙에 비트맵을 만드는 안드로이드의 `Bitmap.createBitmap`메소드는 쓰지 마세요.

#### 오버라이드 해야할 것은?

하나 이상 `process`메소드를 오버라이드하지 마세요. 예상치 못한 결과가 나올 수도 있습니다.

#### 반복하는 Postprocessor

한개 이상의 이미지를 후처리 하고 싶나요? 문제없어요. 그냥 [BaseRepeatedPostprocessor](../javadoc/reference/com/facebook/imagepipeline/request/BaseRepeatedPostProcessor.html)클래스를 상속받으세요. 이 클래스는 postprocessor를 실행할 때 계속 호출되는 `update`메소드를 가지고 있습니다.

아래 예제는 아무때나 그물의 색깔을 바꿉니다.

```java
public class MeshPostprocessor extends BaseRepeatedPostprocessor {
  private int mColor = Color.TRANSPARENT;

  public void setColor(int color) {
    mColor = color;
    update();
  }

  @Override
  public String getName() {
    return "meshPostprocessor";
  }

  @Override
  public void process(Bitmap bitmap) {
    for (int x = 0; x < bitmap.getWidth(); x+=2) {
      for (int y = 0; y < bitmap.getHeight(); y+=2) {
        bitmap.setPixel(x, y, mColor);
      }
    }
  }
}
MeshPostprocessor meshPostprocessor = new MeshPostprocessor();

/// 위의 예제처럼 setPostprocessor

meshPostprocessor.setColor(Color.RED);
meshPostprocessor.setColor(Color.BLUE);
```
이미지 리퀘스트 인스턴스 하나당 한개의 `Postprocessor`가 있어야 합니다. `Postprocessor`는 내부적으로 네트워크 연결상태를 추적할 수 있습니다.
