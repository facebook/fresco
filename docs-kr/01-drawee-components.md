---
id: drawee-components
title: Drawee 컴퍼넌트
layout: docs-kr
permalink: /docs-kr/drawee-components.html
prev: using-drawees-code.html
next: progress-bars.html
---

## 컨텐츠

* [Definitions](#Definitions)
* [Actual](#Actual)
* [Placeholder](#Placeholder)
* [Failure](#Failure)
* [Retry](#Retry)
* [Progress Bar](#ProgressBar)
* [Backgrounds](#Backgrounds)
* [Overlays](#Overlays)
* [Pressed State Overlay](#PressedStateOverlay)

## Definitions 정의

이 페이지는 Drawee 에 표시될 수 있는 컴퍼넌트(소스의 이미지 브랜치에 있는)와 어떻게 설정하는지에 대해 기술합니다.

실제 이미지를 제외한 모든 이미지는 XML 속성에서 설정할 수 있습니다. 이 XML의 값은 Android drawable 이나 color 리소스입니다.

이 값들을 [코드에서 작업하기](using-drawees-code.html) 를 원한다면 [GenericDraweeHierarchyBuilder](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchyBuilder.html) 클래스 메소드를 이용할 수 있습니다. 코드에서는 값들 역시 리소스에서 가져오거나 [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html) 의 하위 클래스를 커스텀하여 사용할 수 있습니다.

심지어 몇몇 아이템은 상속받은 속성이 생성된 후에도 바꿀 수 있습니다. [GenericDraweeHierarchy](../javadoc/reference/com/facebook/drawee/generic/GenericDraweeHierarchy.html) 클래스를 살펴보세요.
몇몇 drawable은 [크기를 변환](scaling.html) 할 수 있습니다.

## Actual

_actual_ 이미지는 인터넷이나 로컬, 리소스, 컨텐트프로비더의 이미지를 가리키는 URI를 이용해 기술되는 타겟입니다.

이것은 상속받은 것이 아닌 controller 의 속성입니다. 그러므로 다른 Drawee 컴퍼넌트에서 사용되는 메소드로 설정할 수 없습니다.

대신에 `setImageURI` 메소드를 이용하거나 [컨트롤러 설정](using-controllerbuilder.html) 코드를 이용할 수 있습니다.

또 스케일 타잎도 있습니다. 이 상속받은 속성은 다른 actual 이미지를 위한 메소드를 보여줍니다.

* 포커스 포인트(scale 타잎의 [focusCrop](scaling.html#FocusCrop))
* 컬러 필터

기본 스케일 타잎은 `centerCrop` 입니다.

## Placeholder - 대기이미지

_placeholder_는 Drawee가 화면에 처음 나타났을 때 보여집니다. `setController`나 `setImageURI`를 호출 해 이미지 가져오기 시작한 후, 가져오기가 완료될 때 까지 placeholder 는 보여집니다.

JPEG 를 가져오는 경우, 대기이미지는 이미지 품질이 당신의 앱에서 설정된 값 혹은 기본 기준에 도달할때까지 대기합니다.

XML attribute: `placeholderImage`  
Hierarchy 빌더 메소드: `setPlaceholderImage`  
Hierarchy 메소드: `setPlaceholderImage`  
기본 값 : 투명한 [ColorDrawable](http://developer.android.com/reference/android/graphics/drawable/ColorDrawable.html)  
기본 스케일 타잎 : `centerInside`

## Failure - 실패

The _failure_ image appears if there is an error loading your image. The most common cause of this is an invalid URI, or lack of connection to the network.
_실패_ 이미지는 이미지 가져오기 실패했을 경우 나타납니다.

XML 속성 : `failureImage`  
Hierarchy 빌더 메소드: `setFailureImage`  
기본 값 : placeholder 이미지  
기본 스케일 타입: `centerInside`  

## Retry - 재시도

_재시도_이미지는 tap-to-retry 기능을 활성화 했을 때 실패이미지 대신에 표시됩니다.

이를 위해 [당신만의 Controller 빌드하기 build your own Controller](using-controllerbuilder.html) 를 해야합니다. 그리고 아래의 코드를 추가하세요.
```java
.setTapToRetryEnabled(true)
```

이 이미지 파이프라인은 유저가 탭하면 이미지 다시 가져오기를 시도합니다. 실패이미지가 보이는 대신 최대 4번까지 시도할 수 있습니다.

XML 속성 : `retryImage`  
Hierarchy 빌더 메소드 : `setRetryImage`  
기본 값 :  placeholder 이미지  
기본 스케일 타입 : `centerInside`  

## <a name="ProgressBar"></a>Progress Bar 프로그레스바

_프로그레스바_ 이미지는 마지막 이미지가 표시될 때 Drawee 위에 덮여 보여집니다.

자세한 것은 [프로그레스바](progress-bars.html)를 보세요

XML 속성: `progressBarImage`  
Hierarchy 빌더 메소드 : `setProgressBarImage`  
기본 값: None  
기본 스케일 타입 : `centerInside`

## Backgrounds - 배경

_배경_ drawable 은 상속받은 것들 보다 먼저 처음으로그려집니다.

XML 에선 오직 하나의 속성에서만 지정할 수 있지만, 코드에선 더 지정할 수 있습니다. 이 경우 가장 처음에 지정한 이미지가 먼저 그려집니다.

배경 이미지는 스케일 타입을 지원하지 않으며, Drawee의 크기만큼 커집니다.

XML 속성: `backgroundImage`  
Hierarchy 빌더 메소드: `setBackground,` `setBackgrounds`  
기본 값: None  
기본 스케일 타입 : N/A

## Overlays - 오버레이

_Overlay_ drawable 은 상송받은 속성의 위에 덮여 마지막으로 그려집니다.

XML 에선 오직 하나의 속성에서만 지정할 수 있지만, 코드에선 더 지정할 수 있습니다. 이 경우 가장 처음에 지정한 이미지가 먼저 그려집니다.

오버레이 이미지는 스케일 타입을 지원하지 않고 Drawee 크기만큼 커집니다.

XML 속성: `overlayImage`  
Hierarchy 빌더 메소드 : `setOverlay,` `setOverlays`  
기본 값: None  
기본 스케일 타입 : N/A

## <a name="PressedStateOverlay"></a>Pressed State Overlay - 눌려진 Overlay

_눌려진 오버레이_는 사용자가 Drawee를 눌렀을 때 나타나는 특별한 오버레이입니다. 예를 들어, 만약 이 Drawee가 버튼이라면 이 오버레이는 눌렸을 때 색깔이 바뀌는 버튼을 포함할 수 있습니다.

눌려진 상태의 오버레이는 스케일 타입을 지원하지 않습니다.

XML 속성 : `pressedStateOverlayImage`  
Hierarchy 빌더 메소드: `setPressedStateOverlay`  
기본 값: None  
기본 스케일 타입 : N/A
