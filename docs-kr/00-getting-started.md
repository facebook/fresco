---
id: getting-started
title: Fresco 시작하기
layout: docs-kr
permalink: /docs-kr/getting-started.html
prev: index.html
next: concepts.html
---

단순히 이미지를 다운받거나 대기이미지를 화면에 표시하고싶다면 [SimpleDraweeView](../javadoc/reference/com/facebook/drawee/view/SimpleDraweeView.html)를 사용하세요.

네트워크의 이미지를 받고 싶다면 유저에게 인터넷 접근권한이 필요합니다. 다음을 ```AndroidManifest.xml```에 추가하세요.

```xml
  <uses-permission android:name="android.permission.INTERNET"/>
```

당신의 앱에서 ```setContentView()```가 실행되기 전에 Fresco클래스를 초기화 해주세요.

```java
Fresco.initialize(context);
```

XML에서 custom namespace를 최상위 요소로 추가하세요.

```xml
<!-- 다른 적합한 엘리먼트도 넣으세요.-->
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:fresco="http://schemas.android.com/apk/res-auto"
    android:layout_height="match_parent"
    android:layout_width="match_parent">
```

그런 후에 ```SimpleDraweeView```를 레이아웃에 넣으세요.

```xml
<com.facebook.drawee.view.SimpleDraweeView
    android:id="@+id/my_image_view"
    android:layout_width="130dp"
    android:layout_height="130dp"
    fresco:placeholderImage="@drawable/my_drawable"
  />
```

아래의 코드로 이미지를 보여줄 수 있습니다.

```java
Uri uri = Uri.parse("http://frescolib.org/static/fresco-logo.png");
SimpleDraweeView draweeView = (SimpleDraweeView) findViewById(R.id.my_image_view);
draweeView.setImageURI(uri);
```

나머지는 Fresco가 알아서 처리합니다.

placeholder는 이미지가 보여지기 전 까지 보여집니다. 이미지는 다운로드, 캐시로 저장, 화면표시, 화면이 꺼지면 뷰의 메모리 해제할 수 있게 됩니다.
