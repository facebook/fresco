---
id: using-drawees-xml
title: XML의 Drawees 사용하기
layout: docs-kr
permalink: /docs-kr/using-drawees-xml.html
prev: concepts.html
next: using-drawees-code.html
---

Drawees는 custom 할 수 있는 많은 속성을 갖고 있습니다. 제일 좋은 방법은 XML을 이용하는 것 입니다.

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="20dp"
    fresco:fadeDuration="300"
    fresco:actualImageScaleType="focusCrop"
    fresco:placeholderImage="@color/wait_color"
    fresco:placeholderImageScaleType="fitCenter"
    fresco:failureImage="@drawable/error"
    fresco:failureImageScaleType="centerInside"
    fresco:retryImage="@drawable/retrying"
    fresco:retryImageScaleType="centerCrop"
    fresco:progressBarImage="@drawable/progress_bar"
    fresco:progressBarImageScaleType="centerInside"
    fresco:progressBarAutoRotateInterval="1000"
    fresco:backgroundImage="@color/blue"
    fresco:overlayImage="@drawable/watermark"
    fresco:pressedStateOverlayImage="@color/red"
    fresco:roundAsCircle="false"
    fresco:roundedCornerRadius="1dp"
    fresco:roundTopLeft="true"
    fresco:roundTopRight="false"
    fresco:roundBottomLeft="false"
    fresco:roundBottomRight="true"
    fresco:roundWithOverlayColor="@color/corner_color"
    fresco:roundingBorderWidth="2dp"
    fresco:roundingBorderColor="@color/border_color"
  />
```

#### 높이(Height)는 필수입니다.

'android:layout_width'와 'android:layout_height'는 **반드시 선언**해야 합니다. 이 2가지 속성이 없다면 Drawee는 이미지를 제대로 표시 못합니다.

#### wrap_content

Drawees 는 'layout_width' 와 'layout_height' 속성에 'wrap_content' 값을 지원하지 않습니다. 컨텐츠의 크기가 바뀌는 이유 때문인데요. 다운받은 이미지의 크기가 대기이미지(placeholder)때문에 달라질 수 - 이미지 표시 실패, 재시도 같은 여러가지 이유때문에- 있기 때문입니다.
'wrap_content' 를 사용하는 것은 이미지가 로드되었을 때 Android가 다른 레이아웃에 이미지 전달해 사용자의 눈에 들어오기 전에 이상한 효과를 만들며 레이아웃이 바뀌도록 강제합니다.(수정필요)

#### 길이 비율로 고정시키기

`wrap_content` 를 사용할 때입니다.
넓이와 높이를 4:3 비율로 설정하고 싶다면 아래와 같이 해보세요

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="20dp"
    android:layout_height="wrap_content"
    fresco:viewAspectRatio="1.33f"
    <!-- 다른 속성들-->
```

혹은 Java에서도 설정할 수 있습니다.

```java
mSimpleDraweeView.setAspectRatio(1.33f);
```
