---
id: progress-bars
title: 프로그레스바
layout: docs-kr
permalink: /docs-kr/progress-bars.html
prev: drawee-components.html
next: scaling.html
---

프로그레스바를 설정하는 가장 쉬운 방법은 앱에서 [상속받은 속성 설정](using-drawees-code.html)할 때 [ProgressBarDrawable](../javadoc/reference/com/facebook/drawee/drawable/ProgressBarDrawable.html) 클래스를 사용하면 됩니다.

```java
.setProgressBarImage(new ProgressBarDrawable())
```

이것은 Drawee의 하단을 따라 어두운 파란색 네모 프로그레스바를 보여줍니다.

###당신만의 프로그레스바 정의하기

진행상태 표시자를 커스텀하고 싶다면 [Drawable.onLevelChange](http://developer.android.com/reference/android/graphics/drawable/Drawable.html#onLevelChange\(int\))를 오버라이딩 하면 됩니다. 로딩중일 때 정확하게 진행상태를 반영할 수 있도록 목적을 명확하게 하세요.

```java
class CustomProgressBar extends Drawable {
   @Override
   protected void onLevelChange(int level) {
     // 0-10,000까지의 스케일 레벨
     // 10,000은 완전히 다운 받음 의미

     // 프로그레스에 따른 drawable의 모양을
     // 바꾸는 로직
   }
}
```
