---
id: concepts
title: 컨셉
layout: docs-kr
permalink: /docs-kr/concepts.html
prev: index.html
next: supported-uris.html
---
## Drawees

Drawees는 이미지가 구현되는 공간입니다. Model-View-Controller 프레임워크같이 3가지 컴포넌트로 구성되어 있습니다.

### DraweeView

Android의 [View](http://developer.android.com/reference/android/view/View.html)클래스를 상속받은 클래스입니다.
대부분의 앱은 SimpleDraweeView 클래스를 사용하면 됩니다. XML이나 Java코드에 추가하세요. [시작](index.html)페이지에 설명한 것 처럼, setImageURI메소드로 URI를 지정하고 로드하면 됩니다.
XML에서 [커스터마이징 할 수도 있어요](using-drawees-xml.html).

### DraweeHierarchy

컨텐츠를 실제로 그려주는 Android 의 [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html) 객체의 상속 판입니다. MVC패턴의 Model입니다.
이미지를 [Java에서 커스텀하여 표현하고 싶을 때,](using-drawees-code.html) 살펴보세요.

### DraweeController

DraweeControllere는 이미지를 불러오는 기본적인 기능들- Fresco 의 이미지 파이프라인이나 그 외에 것- 을 담당합니다.
URI 하나 말고 더 많은 것을 전달해, 이미지를 화면에 표시하고 싶을 때 다루게 될 클래스입니다.

### DraweeControllerBuilder

DraweeControllers 는 한번 생성되면 바꿀 수 없습니다(immutable). [Builder패턴](using-controllerbuilder.html)으로 만들어졌습니다.

### Listeners

builder로 만들어진 것 중 하나입니다. [Listener](listening-download-events.html) 는 이미지를 모두 혹은 부분적으로 받아졌을 때 실행됩니다.

### 이미지 파이프라인

내부적으로 Fresco의 이미지 파이프라인은 이미지 처리작업 과정을 다룹니다. 네트워크, 내부파일, 컨텐트프로비더, 내부 리소스로부터 이미지를 추출합니다. 내부저장소에서 압축된 이미지를 계속 캐싱하고, 두 번째로 메모리에 압축을 푼 이미지를 캐싱합니다.

이미지 파이프라인은 Java 힙에서 이미지를 떨어트리기 위해 *pinned purgeables*라는 특별한 기술을 사용합니다. 호출자는 이미지 작업이 완료되면 `close` 사용을 필요로 합니다.

`SimpleDraweeView`는 이를 자동으로 관리하기 때문에 보통 처음 사용하기 좋습니다. 아주 적은 앱만이 이미지 파이프라인 직접 사용을 필요로 합니다.
