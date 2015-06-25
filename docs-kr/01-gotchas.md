---
id: gotchas
title: 갓챠
layout: docs-kr
permalink: /docs-kr/gotchas.html
prev: writing-custom-views.html
next: intro-image-pipeline.html
---

#### 하위 캐스팅하지 마세요

환상적인 방법으로 Fresco 클래스를 사용한 객체를 다른 객체로 하위 캐스팅해 사용하고 싶을 수도 있습니다.
기껏해야, 이 방법은 취약한 코드를 만들거나 내지는 다음 릴리즈 때 오류가 날 수 있습니다. 최악의 경우 알아채기 힘든 버그를 만들어 낼 수도 있어요.

#### getTopLevelDrawable을 사용하지 마세요

`DraweeHierarchy.getTopLevelDrawable()`은 DraweeView에 의해서만 사용되야 합니다. 클라이언트 코드는 거의 쓸일이 없어요.

유일한 예외는 [커스텀 뷰](writing-custom-views.html)입니다. 하지만 이것도, top-level drawable은 하위 캐스팅할 수 없습니다. 나중에 우린 실제 drawable의 형식을 바꿔버릴 수도 있습니다.

#### DraweeHierarchies 를 재사용하지 마세요.

다른 뷰에서 같은 인자로 ```DraweeView.setHierarchy```를 절대 호출하지 마세요. 상속받은 속성은 Drawable에서 만들어지고 안드로이드의 Drawable은 다수의 뷰에서 공유되지 못합니다.

#### Drawable에 여러 DraweeHierarchy 를 사용하지 마세요.

이ㄷ 또한 위와 같은 이유입니다. Drawable은 여러 뷰에서 공유될 수 없습니다.

한 resourceID를 여러번 상속받거나 여러개의 뷰에서 사용하는 것은 당신 맘입니다. 안드로이드는 각각의 뷰에 각각의 Drawable 인스턴스를 만들 것입니다.

#### DraweeView 에 직접 이미지를 놓지 마세요.

현재 ```DraweeView``` 는 안드로이드 이미지뷰의 섭클래스 입니다. 이것은 이미지를 넣기 위한 여러가지 메소드(setImageBitmap, setImageDrawable등)가 있습니다.

이미지를 직접 설정해 버리면 ```DraweeHierarchy```를 쓸 수 없게 됩니다. 그리고 이미지 파이프라인에서 아무 결과도 돌려받을 수 없습니다.

#### DraweeView에서 ImageView의 속성이나 메소드를 쓰지 마세요.

[뷰](http://developer.android.com/reference/android/view/View.html)에 없는 이미지뷰의 모든 XML속성이나 메소드는 DraweeView에서 동작하지 않습니다.
`scaleType`, `src` 같은 경우죠. 이것들은 사용하지 마세요. DraweeView는 이 문서에서 설명한 각기 대응되는 것들을 갖고 있습니다. 다른 ImageView속성과 메소드는 곧 제거될 것 입니다. 그러니 사용하지 마세요.
