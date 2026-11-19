# R8 rules for Coffee Music Player. Most libraries (Media3, Room, OkHttp, Retrofit,
# kotlinx.serialization, WorkManager, Coil, Koin) ship their own consumer rules.

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# jaudiotagger looks up some classes reflectively and references desktop Java APIs
# (AWT / ImageIO / Swing) that don't exist on Android.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**

# Jsoup's optional regex engine.
-dontwarn com.google.re2j.**
