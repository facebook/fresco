# Contributing to Fresco

We want to make contributing to this project as easy and transparent as
possible.

## Security bugs

Facebook has a [bounty program](https://www.facebook.com/whitehat/) for the safe disclosure of security bugs. In those cases, please go through the process outlined on that page and do not file a GitHub issue.

## Pull Requests

We welcome pull requests.

1. Fork the repo and create your branch from `master`. 
2. If you've added code that should be tested, add tests.
3. If you've changed APIs, update the documentation. 
4. Make sure the test suite passes.
5. Make sure your code passes lint.
6. If you haven't already, complete the [Contributor License Agreement](https://code.facebook.com/cla) ("CLA").

## Getting started

In Android Studio, choose `File > Open..`. and select the `fresco` folder.

### Specify a path to the NDK

Fresco uses native code for a few features. To build Fresco you'll need to specify the path to the NDK.

In Android Studio, go to `File > Project Structure` and in the dialog set the `Android NDK location`. Android Studio stores the NDK location in to your `local.properties` file.

### Run a sample app

Select the **Showcase** app and click run:

![Running a sample Fresco app](https://cloud.githubusercontent.com/assets/346214/24415877/d48d894c-13da-11e7-8601-09627661de67.png)

You can use the drawer to select one of the demos:

<img width="364" alt="Fresco showcase app" src="https://cloud.githubusercontent.com/assets/346214/24416135/a9a4a07a-13db-11e7-9d19-25ae9cbc83d3.png">

Now you can change any code in Fresco and see the changes in the app.

Have fun hacking on Fresco! 😎

## Testing your changes

You can check your code compiles using:

```
cd fresco
./gradlew assembleDebug
```

You can run tests locally using:

```
cd fresco
./gradlew test
```

Circle CI will run the same tests and report on your pull request.

## Contributor License Agreement ("CLA")

In order to accept your pull request, we need you to submit a CLA. You only need
to do this once to work on any of Facebook's open source projects.

Complete your CLA here: <https://code.facebook.com/cla>.

## Our Development Process

Each pull request is first submitted into Facebook's internal repositories by a
Facebook team member. Once the commit has successfully passed Facebook's internal
test suite, it will be exported back out from Facebook's repository. We endeavour
to do this as soon as possible for all commits.

## Coding Style  

* 2 spaces for indentation rather than tabs
* 100 character line length
* Although officially archived, we still follow the practice of Oracle's 
[Coding Conventions for the Java Programming Language](http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html).

## License

By contributing to Fresco, you agree that your contributions will be licensed
under its MIT license.
