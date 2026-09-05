# Requirements Document

## Introduction

The Bluetooth Hotspot Enabler is a simple Android application that monitors Bluetooth connection events and automatically enables the device's WiFi hotspot when the phone connects to the Bluetooth device named "CHEVROLET6572". The app provides a single-screen interface with a toggle to activate or deactivate this automation without affecting the current state of the hotspot itself. The target device is hardcoded and requires no user configuration.

## Glossary

- **App**: The Bluetooth Hotspot Enabler Android application.
- **Automation**: The background behavior that enables the WiFi hotspot upon a Bluetooth connection event.
- **Target_Device**: The hardcoded Bluetooth device with the name "CHEVROLET6572" whose connection triggers the hotspot automation.
- **Hotspot**: The Android device's built-in WiFi tethering / mobile hotspot feature.
- **Automation_Toggle**: The UI control that enables or disables the Automation without affecting the Hotspot state.
- **BT_Monitor**: The background service responsible for listening to Bluetooth connection events.
- **Persistence_Store**: The local storage mechanism used to save user preferences across app restarts.

---

## Requirements

### Requirement 1: Automation Toggle

**User Story:** As a user, I want a toggle on the main screen to enable or disable the automation, so that I can turn the automatic hotspot behavior on or off without opening system settings.

#### Acceptance Criteria

1. THE App SHALL display a single-screen interface containing an Automation_Toggle that is always visible.
2. WHEN the user turns the Automation_Toggle on, THE App SHALL activate the Automation and persist this state to the Persistence_Store.
3. WHEN the user turns the Automation_Toggle off, THE App SHALL deactivate the Automation and persist this state to the Persistence_Store.
4. WHEN the App is launched AND a valid persisted state exists in the Persistence_Store, THE App SHALL restore the Automation_Toggle state from the Persistence_Store.
5. THE Automation_Toggle SHALL NOT directly enable or disable the Hotspot.
6. WHEN the App is launched for the first time AND no persisted state exists, THE App SHALL default the Automation_Toggle to the off position.
7. IF a Persistence_Store read or write operation fails, THEN THE App SHALL default to the off state and display an inline error message indicating that settings could not be saved.

---

### Requirement 2: Bluetooth Connection Monitoring

**User Story:** As a user, I want the app to monitor Bluetooth connections in the background, so that the hotspot is enabled automatically when I get in my car without requiring me to open the app.

#### Acceptance Criteria

1. WHILE the Automation is active, THE BT_Monitor SHALL listen for Bluetooth device connection events in the background.
2. WHEN the Automation_Toggle is turned on, THE App SHALL start the BT_Monitor within 2 seconds without requiring user intervention.
3. WHEN a Bluetooth device connects AND the connected device's name matches "CHEVROLET6572", THE BT_Monitor SHALL request the system to enable the Hotspot.
4. WHEN a Bluetooth device connects AND the connected device's name does not match "CHEVROLET6572", THE BT_Monitor SHALL take no action.
5. WHILE the Automation is inactive, THE BT_Monitor SHALL NOT respond to Bluetooth connection events.
6. WHEN the Android system restarts AND the Automation was active at the time of the last shutdown, THE BT_Monitor SHALL resume listening for Bluetooth events within 30 seconds of the boot completing.
7. IF the system rejects the Hotspot enable request, THEN THE BT_Monitor SHALL display a notification informing the user that the hotspot could not be enabled and SHALL log the failure reason.

---

### Requirement 3: Hotspot Activation

**User Story:** As a user, I want the app to enable the WiFi hotspot when my car's Bluetooth connects, so that devices in my car automatically have internet access.

#### Acceptance Criteria

1. WHEN the BT_Monitor determines that the Target_Device has connected, THE App SHALL attempt to enable the Hotspot using the Android system API.
2. IF the Hotspot is already enabled when the Target_Device connects, THEN THE App SHALL take no action.
3. IF the App lacks the required system permissions to enable the Hotspot, THEN THE App SHALL display a notification informing the user that the permission is missing, SHALL skip the hotspot activation attempt, and SHALL not retry until the permission is granted.
4. IF the Android system API call to enable the Hotspot returns a failure result, THEN THE App SHALL display a notification to the user indicating that the hotspot could not be enabled and SHALL include the failure reason where available.

---

### Requirement 4: Permissions Handling

**User Story:** As a user, I want the app to request only the permissions it needs and explain why, so that I understand what access the app requires.

#### Acceptance Criteria

1. WHEN running on Android 12 (API 31) or above, THE App SHALL request the BLUETOOTH_CONNECT permission at runtime before accessing Bluetooth connection events.
2. WHEN running on Android versions below API 31, THE App SHALL request the BLUETOOTH permission to access Bluetooth functionality.
3. WHEN the user denies a required permission, THE App SHALL display a message that includes: (a) the name of the denied permission, (b) the specific functionality it enables, and (c) instructions for navigating to system settings to grant it manually.
4. IF a required permission is permanently denied (denied at least twice or the user selected "Don't ask again" such that the system dialog no longer appears), THEN THE App SHALL display a message directing the user to Android system settings to grant the permission manually and SHALL NOT attempt to re-request the permission via a system dialog.
5. WHEN the App first attempts to enable the Hotspot AND the WRITE_SETTINGS permission has not been granted, THE App SHALL display an explanation of why WRITE_SETTINGS is required before presenting the system permission request to the user.

---

### Requirement 5: Background Service Lifecycle

**User Story:** As a user, I want the automation to remain active even when the app is closed or the phone restarts, so that the feature works reliably without manual intervention.

#### Acceptance Criteria

1. WHILE the Automation is active, THE BT_Monitor SHALL run as an Android foreground service with a persistent notification.
2. THE persistent notification SHALL display: (a) the name of the app, and (b) a monitoring status indicator that reflects either "Monitoring" or "Stopped".
3. WHEN the user closes the App, THE BT_Monitor SHALL continue running as a foreground service as long as the Automation is active.
4. WHEN the Android system kills the BT_Monitor process AND the Automation was active at the time of termination, THE App SHALL restart the BT_Monitor within 10 seconds via the service's onStartCommand restart strategy.
5. WHEN the device boots AND the Automation was active at the time of the last shutdown, THE BT_Monitor SHALL start automatically within 30 seconds of boot completion without requiring user interaction.
