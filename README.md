# Tinfoil for SkBank

This is an app wrapper for SkBank's mobile web site, for those of you who cannot use the native app.

Based on Daniel Velazco's excellent [Tinfoil for Facebook](https://github.com/velazcod/Tinfoil-Facebook).

## Building

* Install the [Android SDK](https://developer.android.com/sdk/installing/index.html).  Make sure that API 21, Build-tools 21.x and Android Support Repository are checked in the Android SDK Manager.
* Set ANDROID_HOME, e.g. ``export ANDROID_HOME=/Users/myusername/Android/sdk``
* Run ``./gradlew build``
* Install ``./app/build/outputs/apk/app-debug.apk`` to your phone
