# Room entities are accessed via reflection-free generated code; keep model
# classes only where serialization names matter (backup JSON is hand-written,
# key-stable, so no keep rules are required for it).
-dontwarn org.slf4j.**

# Keep the BroadcastReceivers referenced from the manifest.
-keep class com.mtss.alcoholtracker.notifications.** { *; }
