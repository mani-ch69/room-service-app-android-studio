# Walkthrough - Renaming Security Setting to App Security

I have renamed the "Security Setting" option to "App Security" in both the settings menu and the top navigation bar.

## Changes

### [SettingsScreen.kt](file:///C:/Users/USER/AndroidStudioProjects/roomservice/app/src/main/java/com/example/roomservice/ui/settings/SettingsScreen.kt)
- Renamed the title of the security card from "Security Setting" to **"App Security"**.

### [AdminMenuScreen.kt](file:///C:/Users/USER/AndroidStudioProjects/roomservice/app/src/main/java/com/example/roomservice/ui/waiter/AdminMenuScreen.kt)
- Updated the header title mapping so that when the user enters the security settings, the top bar now displays **"App Security"** instead of "Security Setting".

## Verification Summary
- **UI Consistency**: The name is now consistent across the selection card and the header title.
- **Code Health**: Static analysis confirmed that no new errors were introduced, although some existing warnings persist in the file.
