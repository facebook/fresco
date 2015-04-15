---
id: index
title: Adding Fresco to your Project
layout: docs
permalink: /docs/index.html
next: getting-started.html
---

Here's how to add Fresco to your Android project.

### Android Studio or Gradle

Edit your `build.gradle` file. You must add the following line to the `dependencies` section:


```groovy
dependencies {
  // your app's other dependencies
  compile 'com.facebook.fresco:fresco:{{site.current_version}}+'
}
```

### Maven

Add the following to the `<dependencies>` section of your `pom.xml` file:


```xml
<dependency>
  <groupId>com.facebook.fresco</groupId>
  <artifactId>fresco</artifactId>
  <version>LATEST</version>
</dependency>
```

### Eclipse ADT / Ant

Unfortunately Eclipse does not yet support the AAR file format Fresco uses. We are still looking for a workaround.


