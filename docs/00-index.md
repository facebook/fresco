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

Download the [zip file](https://github.com/facebook/fresco/releases/download/v0.3.0/fresco-v{{site.current_version}}.zip).

When you expand it, it will create a directory called 'frescolib', with the following directory structure:

    frescolib/
      drawee/
      fbcore/
      fresco/
      imagepipeline/

In your Eclipse workspace, import all four of these subdirectories as Eclipse projects. Use File, Import, Android, "Existing Android Code into Workspace".

Then in your app's own project, add a dependency on 'fresco'. Right-click on your project, choose Properties, then Android. Click Add and select fresco.


