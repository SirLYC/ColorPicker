# ColorPicker
[![](https://jitpack.io/v/SirLYC/ColorPicker.svg)](https://jitpack.io/#SirLYC/ColorPicker)

A simple customer view to choose color.

## Preview

![preview](imgs/video.gif)
> This view will save state when configuration changed, like activity orientation change.

## Usage
***Step 1.*** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

``` groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
````

**Step 2.** Add the dependency

``` groovy
dependencies {
        implementation 'com.github.SirLYC:ColorPicker:0.0.1'
}
```

You can find examples in `sample` module.

## License
[License.md](./LICENSE.MD)
