#!/bin/bash

rm -rf $HOME/.gradle/caches
rm -rf .gradle
./gradlew clean allclean build uploadArchives
