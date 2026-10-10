# Room, Compose, WorkManager and Firebase ship their own consumer rules.
# Keep Firestore document mapping safe if you later switch to toObject():
-keepclassmembers class com.fairwayfit.app.data.db.** { *; }
