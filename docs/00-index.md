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
  compile 'com.facebook.fresco:fresco:0.2.0+'
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

We have placed a zip file containing Fresco and its transitive dependencies in our GitHub releases page. 

To install it into your Eclipse or Ant project:

1. [Download](https://github.com/facebook/fresco/releases/v0.2.0/fresco-libs.zip) the file.
2. Copy the file into the `libs` directory of your project.
3. Unzip it.




