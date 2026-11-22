# RUNVANTA Release ProGuard / R8 Rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve Room database classes and entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }

# Preserve Google Mobile Ads & UMP SDK callbacks
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }

# Preserve OpenGL ES renderer entry points
-keepclassmembers class com.example.game3d.** { *; }
