# CompressUtils
安卓压缩jpeg库：libjpeg-turbo2.1.3

## Dependency
#### Add this in your root build.gradle file (not your module build.gradle file):
```xml
allprojects {
	repositories {
        maven { url "https://jitpack.io" }
    }
}
```
#### Then, add the library to your module build.gradle
```xml
dependencies {
    implementation 'com.github.pglve:CompressUtils:0.1.5'
}
```
