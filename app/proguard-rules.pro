# Room, Media3, Coil and DataStore ship their own consumer rules.
-keepattributes *Annotation*, Signature, InnerClasses
-keep class com.glad.app.widget.** { *; }
-keep class com.glad.app.music.PlaybackService { *; }
