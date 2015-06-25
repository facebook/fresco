---
id: scaling
title: 스케일링
layout: docs-kr
permalink: /docs-kr/scaling.html
prev: progress-bars.html
next: rounded-corners-and-circles.html
---

여러 타입의 스케일을 Drawee의 [여러가지 drawables](drawee-components.html)에 각각 적용할 수 있습니다.

### 여러가지 스케일 타입

| 스케일 타입 | 설명|
| --------- | ----------- |
| center | 스케일없이 뷰의 중심에 이미지 놓음|
| centerCrop |이미지를 스케일하여 부모 뷰의 높이나 넓이 한 쪽 크기보다 크거나 같도록 맞춥니다.<br> 넓이나 높이 중 하나가 정확하게 맞춰집니다.<br>이미지는 부모 뷰의 중심에 놓임.|
| [focusCrop](#focusCrop) |centerCrop과 비슷합니다. 하지만 중심이 호출자가 지정한 좌표를 중심으로 합니다|
| centerInside |  이미지를 축소하여 전체가 부모 뷰의 안에 들어오도록 합니다. <br>'fitCenter' 처럼 확대되지 않는 것이 다릅니다.<br> 비율은 유지됩니다.<br>이미지는 부모 뷰의 중심에 놓입니다. |
| fitCenter | 이미지를 스케일하여 전체가 부모 뷰의 내부에 맞게 그립니다. <br> 넓이, 높이 중 하나가 정확하게 맞춰 집니다.<br> 이미지는 부모 뷰의 중심에 높입니다.|
| fitStart | 이미지를 스케일하여 부모 뷰의 내부에 딱 맞도록 합니다. <br>높이나 넓이 중 하나는 정확하게 일치합니다. <br>비율은 유지됩니다. <br>이미지는 왼쪽 위 가장자리에 놓입니다.
| fitEnd |이미지를 스케일하여 부모 뷰의 내부에 딱 맞도록 합니다. <br>높이나 넓이 중 하나는 정확하게 일치합니다. <br>비율은 유지됩니다<br> 이미지는 부모 뷰의 오른쪽 아래 가장자리에 놓입니다.
| fitXY | 높이와 넓이를 독립적으로 스케일합니다. 이미지는 부모 뷰의 내부에 꽉 찹니다.  <br>비율은 유지되지 않습니다.
| [none](#none) |Android의 타일모드를 위해 사용됩니다.|

이것들은 Android [ImageView](http://developer.android.com/reference/android/widget/ImageView.ScaleType.html) 클래스와 거의 비슷합니다.

하나 지원하지 않는 타입은 `matrix` 입니다. 대신 Fresco는 `focusCrop`을 제공하지요. 보통 더 나은 성능을 제공합니다.

### 사용하는 방법

Actual, placeholder, 재시도, 실패 이미지는 [XML 에서 설정하기](using-drawees-xml.html)에서 `fresco:actualImageScaleType` 같은 속성을 사용해 설정할 수 있습니다. 또한 [코드에서 설정하기](using-drawees-code.html) 에서도 [GenericDraweeHierarchyBuilder](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchyBuilder.html) 를 이용할 수도 있습니다.

또 상속 속성을 만든 후에는 실제 이미지의 스케일 타입은  [GenericDraweeHierarchy](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchy.html)를 이용해 수정될 수 있습니다.

하지만 `android:scaleType` 속성과 `.setScaleType`은 **사용하지 마세요**. 이것들은 Drawees에선 쓸모 없습니다.

### focusCrop

`centerCrop` 스케일 타입을 제공하는 Android와 Fresco는 이미지의 비율을 보존하며 필요한 만큼 잘라 전체 뷰 영역을 채웁니다.

focusCrop은 아주 유용하지만 문제는 잘라내기가 항상 필요한 곳에서 일어나지 않는 것입니다. 예를 들어 당신이 누군가의 얼굴 이미지를 오른쪽 아래 구석으로 자르고 싶다면 `centerCrop`은 원했던 방식으로 동작하지 않습니다.

focus 위치를 잡아 어떤 부분이 뷰의 중심에 놓일 것인지 기술할 수 있습니다. 만약 이 focus 위치를 이미지의 위쪽 상단에 놓고 싶으면 다른 것 필요없이 (0.5f, 0f) 로 설정하면 됩니다. 이 위치는 가능한 많은 뷰의 중심을 부분을 보여줄 것입니다.

Focus 위치는 상대적 좌표 시스템으로 기술됩니다. 즉 (0f, 0f)는 왼쪽 상단 구석이고, (1f,1f)는 우측 하단 구석입니다. 상대적 좌표는 스케일불변이며 따라서 아주 유용합니다.

(0.5f, 0.5f)의 focus위치는 `centerCrop` 과 일치합니다.
focus 위치를 사용하려면 올바른 스케일 타입을 XML에 설정해야 합니다.
```xml
  fresco:actualImageScaleType="focusCrop"
```

Java코드에선 꼭 올바른 focus위치를 설정해야 합니다.

```java
PointF focusPoint;
// 포커스 포인트를 지정한다
mSimpleDraweeView
    .getHierarchy()
    .setActualImageFocusPoint(focusPoint);
```

### none

만약 Drawable을 Android의 tile 형식으로 사용하려면 `none` 스케일 타입을 사용하면 됩니다.
