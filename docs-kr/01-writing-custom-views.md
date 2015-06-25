---
id: writing-custom-views
title: 커스텀 뷰 사용하기
layout: docs-kr
permalink: /docs-kr/writing-custom-views.html
prev: image-requests.html
next: gotchas.html
---

### DraweeHolders

`DraweeViews`만 항상 필요한 건 아닙니다. 같은 뷰 내부에 여러 이미지를 보여 줘야하는 경우도 있을 수 있고, 다른 컨텐츠를 추가적으로 보여줘야 할 경우도 있을 것입니다.

이 때, Drawee를 사용한 두 가지의 방법이 있습니다.

* 이미지 한 장을 위한 `DraweeHolder`
* 여러 이미지를 위한 `MultiDraweeHolder`

### 커스텀 뷰의 응답성

안드로이드는 뷰 오브젝트를 시스템이벤트로만 다룹니다. `DraweeViews`는 이 이벤트를 다루면서 메모리를 효율적으로 관리합니다. 이 holder를 사용할 땐 몇가지 기능을 필수로 구현해야합니다.

#### attach/detach 이벤트 다루기

**주의 : 아래 사항을 숙지하지 않으면 메모리릭이 생길수도 있습니다.**
안드로이드에서 뷰가 화면에 더이상 나타나지 않을 때 - 리스트뷰에서 스크롤되어 화면에 나타나지 않을 때나 다른 방식으로 나타나지 않을 때 - 메모리에 있는 이미지를 다루는 방법은 없습니다. Drawee는 이런 현상들을 알아채서 메모리를 해제합니다. 이미지가 화면에 다시 나타나면 자동으로 불러옵니다.

이러한 작업은 `draweeView`에서 자동으로 수행됩니다. 하지만 커스텀뷰에선 다음 4가지 시스템 이벤트를 다루지 않으면 이러한 작업들이 수행되지 않습니다. `DraweeHolder`에 이 이벤트들을 꼭 전달하세요. 아래에 이 방법이 나와있습니다.

```java
DraweeHolder mDraweeHolder;

@Override
public void onDetachedFromWindow() {
  super.onDetachedFromWindow();
  mDraweeHolder.onDetach();
}

@Override
public void onStartTemporaryDetach() {
  super.onStartTemporaryDetach();
  mDraweeHolder.onDetach();
}

@Override
public void onAttachedToWindow() {
  super.onAttachedToWindow();
  mDraweeHolder.onAttach();
}

@Override
public void onFinishTemporaryDetach() {
  super.onFinishTemporaryDetach();
  mDraweeHolder.onAttach();
}
```

#### 터치 이벤트 다루기

Drawee에서 [탭해서 다시 가져오기](drawee-components.html#Retry)를 활성화 했다면, 사용자가 화면을 터치했다고 전달해줘야 합니다. 아래와 같이 해보세요.

```java
@Override
public boolean onTouchEvent(MotionEvent event) {
  return mDraweeHolder.onTouchEvent(event) || super.onTouchEvent(event);
}
```

#### onDraw 커스텀하기

다음을 꼭 호출 하세요.

```java
Drawable drawable = mDraweeHolder.getHierarchy().getTopLevelDrawable();
drawable.setBounds(...);
```
안하면 Drawee는 이미지를 나타내지 않을겁니다.

* Drawable은 하위 캐스팅 하지 마세요.
* 변환하지 마세요

#### 다른 응답성들

* `verifyDrawable` 오버라이딩 :

```java
@Override
protected boolean verifyDrawable(Drawable who) {
  if (who == mDraweeHolder.getHierarchy().getTopLevelDrawable()) {
    return true;
  }
  // 뷰에 사용할 다른 로직
}
```

* `invalidateDrawable` 는 Drawee에 점유된 지역을 무효화한다는 것을 명심하세요.


### DraweeHolder 만들기

주의해서 사용하세요.

#### 생성자 순서 정하기

생성자를 만들 때 다음 패턴을 추천합니다.

* View의 3가지 생성자 모두 오버라이딩.
* 각각의 생성자는 대응하는 수퍼클래스를 호출한 다음 private `init`메소드를 호출합니다.
* 초기화 과정은 `init`에서 일어납니다.

생성자를 `this` 연산자로 다른 생성자에서 호출하지 마세요.

어떤 생성자를 사용하던지간에 일단 호출만 하면 올바른 초기화를 보증합니다. 초기화는 holder가 만들어지는 `init`메소드에 있습니다.

#### Holder 만들기

가능하다면, view가 만들어 질 때 Drawee를 만드세요. 상속받는 것은 비용이 많이 들기 때문에 한 번만 하는게 좋습니다.

```java
class CustomView extends View {
  DraweeHolder<GenericDraweeHierarchy> mDraweeHolder;

  // 위의 패턴을 이용한 생성자

  private void init() {
    GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources());
      .set...
      .set...
      .build();
    mDraweeHolder = DraweeHolder.create(hierarchy, context);
  }
}
```

#### 이미지 세팅하기

뷰대신 홀더의 `setController`를 호출하여 [컨트롤러 빌더](using-controllerbuilder.html)를 사용하세요.

```java
DraweeController controller = Fresco.newControllerBuilder()
    .setUri(uri)
    .setOldController(mDraweeHolder.getController())
    .build();
mDraweeHolder.setController(controller);
```

### MultiDraweeHolder

`DraweeHolder`를 사용하는 대신 `MultiDraweeHolder`를 사용할 수도 있습니다. drawee를 다루는 `add`, `remove`, `clear`메소드가 있습니다.

```java
MultiDraweeHolder<GenericDraweeHierarchy> mMultiDraweeHolder;

private void init() {
  GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources());
    .set...
    .build();
  mMultiDraweeHolder = new MultiDraweeHolder<GenericDraweeHierarchy>();
  mMultiDraweeHolder.add(new DraweeHolder<GenericDraweeHierarchy>(hierarchy, context));
  // 다른 상속받은 속성 설정 반복
}
```

하나의 `DraweeHolder`의 시스템 이벤트, 경계설정, 그리고 모든 필수 속성을 반드시 오버라이드 해야합니다.
