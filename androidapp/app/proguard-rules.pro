# Release shrinking (R8). Retrofit, OkHttp, kotlinx.serialization, Firebase and AndroidX ship their own
# consumer rules; views in layouts and fragments in nav_graph are kept by AAPT. Add rules here only for
# what those do not cover, with the reason.

# Crashlytics: readable file names and line numbers in de-obfuscated stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception
