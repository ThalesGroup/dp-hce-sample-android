TODO: Place in this folder the TSHPaySDK binaries from the distribution package and potentially update the dependency definition in ../build.gradle

For example:

``` groovy
    debugImplementation (files("libs/TSHPaySDK-dev-6.10.0.rc01.aar"))
    releaseImplementation (files("libs/TSHPaySDK-release-6.10.0.rc01.aar"))

```