---
id: closeable-references
title: 닫을 수 있는 참조
layout: docs-kr
permalink: /docs-kr/closeable-references.html
prev: datasources-datasubscribers.html
next: using-other-network-layers.html
---

**이 페이지는 고급 개발용입니다.**

대부분의 앱은 [Drawees](using-drawees-xml.html)를 사용하고 close 코드를 넣을 필요가 없습니다.

자바 언어는 쓸모없는 자원을 회수하고 대부분의 개발자는 객체를 싫든 좋든(willy-nilly) 만들고 관리해 결국엔 메모리에서 사라지도록 권한을 부여합니다.

안드로이드 5.0의 향상까지, 이것은 비트맵에 대해 좋은 생각은 아니었습니다. 이것은 큰 모바일 단말의 메모리를 공유합니다. 메모리 내부의 비트맵은 가비지 컬렉터를 더 자주 실행하도록 만들며, 이미지가 많이 사용되는 앱을 느려지게 하거나 좋지 않은 품질로(janky) 만듭니다.

비트맵은 자바 개발자가 C++과 [Boost](http://www.boost.org/doc/libs/1_57_0/libs/smart_ptr/smart_ptr.htm)같은 똑똑한 라이브러리를 그리워지게 만드는 것 중 하나입니다.

Fresco의 솔류션은 [CloseableReference](../javadoc/reference/com/facebook/common/references/CloseableReference.html) 클래스에서 볼 수 있습니다. 올바르게 사용하려면 아래 규칙을 따르세요 :

#### 1. 호출자는 레퍼런스를 소유합니다.

여기에 레퍼런스가 있습니다. 그리고 호출자에게 값을 전달하였기 때문에 호출자가 레퍼런스를 갖고 있습니다.

```java
CloseableReference<Val> foo() {
  Val val;
  return CloseableReference.of(val);
}
```

#### 2. 소유자는 스코프를 빠져나가기 전 레퍼런스를 무조건 close해야 합니다.

레퍼런스를 만들었지만 호출자에게 전달하지 않았으므로 close해야만 합니다.

```java
void gee() {
  CloseableReference<Val> ref = foo();
  try {
    haa(ref);
  } finally {
    ref.close();
  }
}
```

`finally` 블록은 거의 항상 이것을 수행하는 최고의 방법입니다.

#### 3. 어떤 것은 레퍼런스를 close하면 안됩니다.

아래는 인자를 통해 레퍼런스를 받았습니다. 이 호출자는 여전히 이 소유자에게 있으므로 close할 의무가 없습니다.

```java
void haa(CloseableReference<?> ref) {
  Log.println("Haa: " + ref.get());
}
```

If we called `.close()` here by mistake, then if the caller tried to call `.get()`, an `IllegalStateException` would be thrown.
만약 `.close()`를 실수로 호출하고 `.get()`을 수행하면 `IllegalStateException`이 발생할 것입니다.

#### 4. 항상 할당하기 전 레퍼런스를 복사하기

만약 레퍼런스를 유지할 필요가 있다면 복사하면 됩니다.

클래스에서 사용할 때 :

```java
class MyClass {
  CloseableReference<Val> myValRef;

  void mmm(CloseableReference<Val> ref) {
    myValRef = ref.clone();
  };
  // 복사본을 만들었으므로 안전하게 close할 수 있음.

  void close() {
    CloseableReference.closeSafely(myValRef);
  }
}
// MyClass는 close를 호출 해야만 함!
```

만약 내부 클래스에서 사용할 때:

```java
void haa(CloseableReference<?> ref) {
  final CloseableReference<?> refClone = ref.clone();
  executor.submit(new Runnable() {
    public void run() {
      try {
        Log.println("Haa Async: " + refClone.get());
      } finally {
        refClone.close();
      }
    }
  });
    // 복사본을 만들었으므로 안전하게 close할 수 있음.
}```
