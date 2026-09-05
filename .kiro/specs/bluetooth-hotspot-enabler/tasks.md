# Implementation Plan: Bluetooth Hotspot Enabler

## Overview

Implement a single-Activity Android app in Kotlin that enables the WiFi hotspot automatically when the phone connects to a hardcoded Bluetooth device ("CHEVROLET6572"). Hotspot control is via Shizuku only. The background service, boot receiver, preferences, and all helper classes are built incrementally, each wired into the previous step.

## Tasks

- [x] 1. Project scaffolding and manifest
  - Create a new Android project with `minSdk 31`, `targetSdk 36`, Kotlin DSL Gradle build files
  - Add Shizuku dependencies (`dev.rikka.shizuku:api:13.1.5`, `dev.rikka.shizuku:provider:13.1.5`) and Kotest + JUnit 5 + MockK test dependencies to `build.gradle.kts`
  - Write `AndroidManifest.xml` with all required permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH` (maxSdkVersion 30), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `moe.shizuku.manager.permission.API_V23`), declare `BluetoothMonitorService` with `foregroundServiceType="connectedDevice"`, declare `BootReceiver` for `ACTION_BOOT_COMPLETED`, and declare `MainActivity`
  - _Requirements: 2.1, 4.1, 4.2, 5.1_

- [x] 2. Core data types and interfaces
  - [x] 2.1 Define shared interfaces and data classes
    - Create `AutomationPreferences` interface and `AutomationPrefsException` class in `data/AutomationPreferences.kt`
    - Create `HotspotController` interface in `hotspot/HotspotController.kt`
    - Create `ShizukuStatusChecker` interface in `shizuku/ShizukuStatusChecker.kt`
    - Create `PermissionHelper` interface in `permissions/PermissionHelper.kt`
    - Create `NotificationHelper` interface in `notifications/NotificationHelper.kt`
    - Create `ServiceNotificationContent` data class and `ServiceActions` object in `service/ServiceActions.kt`
    - _Requirements: 1.2, 1.3, 1.7, 3.1, 4.3, 5.1, 5.2_

- [x] 3. AutomationPreferences implementation
  - [x] 3.1 Implement `AutomationPreferencesImpl`
    - Write `AutomationPreferencesImpl` backed by `SharedPreferences` with key `automation_enabled` defaulting to `false`
    - Wrap read failures with a `false` return; wrap write failures by throwing `AutomationPrefsException`
    - _Requirements: 1.2, 1.3, 1.4, 1.6, 1.7_

  - [x] 3.2 Write property test for automation state persistence round-trip
    - **Property 1: Automation state persistence round-trip**
    - Use `Arb.boolean()`, write to an in-memory `AutomationPreferences` fake, read back, assert equal
    - Tag: `Feature: bluetooth-hotspot-enabler, Property 1: Persistence round-trip`
    - Minimum 100 iterations
    - **Validates: Requirements 1.4**

  - [x] 3.3 Write unit tests for `AutomationPreferencesImpl`
    - Test default value returns `false` when no key is stored
    - Test write failure throws `AutomationPrefsException`
    - Test read failure returns `false`
    - _Requirements: 1.4, 1.6, 1.7_

- [x] 4. NotificationHelper implementation
  - [x] 4.1 Implement `NotificationHelperImpl`
    - Create `CHANNEL_SERVICE` (low importance) and `CHANNEL_ERRORS` (default importance) notification channels, register them in the constructor
    - Implement `buildServiceNotification(isMonitoring: Boolean)` returning a `Notification` containing the app name and either "Monitoring" or "Stopped"
    - Implement `postErrorNotification(title, body)` posting to `CHANNEL_ERRORS`
    - _Requirements: 3.4, 5.1, 5.2_

  - [x] 4.2 Write property test for notification status correctness
    - **Property 5: Notification status correctness**
    - Use `Arb.boolean()` → `buildServiceNotification(isMonitoring)` → assert notification extras contain app name; assert status text is `"Monitoring"` when `true`, `"Stopped"` when `false`
    - Tag: `Feature: bluetooth-hotspot-enabler, Property 5: Notification status correctness`
    - Minimum 100 iterations
    - **Validates: Requirements 5.2**

  - [x] 4.3 Write unit tests for `NotificationHelperImpl`
    - Test both channels are created with correct importance levels
    - Test `postErrorNotification` posts to `CHANNEL_ERRORS`
    - _Requirements: 3.4, 5.2_

- [x] 5. PermissionHelper implementation
  - [x] 5.1 Implement `PermissionHelperImpl`
    - Implement `requiredBluetoothPermission()` returning `BLUETOOTH_CONNECT` on API 31+, `BLUETOOTH` otherwise
    - Implement `hasAllPermissions(context)` checking all required runtime permissions
    - Implement `buildDenialMessage(permission)` returning a string containing: (a) the permission name, (b) a functionality description, (c) the word "Settings" or a settings URI
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 5.2 Write property test for permission denial message completeness
    - **Property 4: Permission denial message completeness**
    - Use `Arb.element(BLUETOOTH_CONNECT, BLUETOOTH)` → `buildDenialMessage(permission)` → assert result contains permission name, non-empty description, and "Settings"
    - Tag: `Feature: bluetooth-hotspot-enabler, Property 4: Permission denial message completeness`
    - Minimum 100 iterations
    - **Validates: Requirements 4.3**

  - [x] 5.3 Write unit tests for `PermissionHelperImpl`
    - Test `requiredBluetoothPermission()` returns correct value for API 31+ and below
    - Test `buildDenialMessage` for permanently denied case includes settings navigation hint
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 6. ShizukuStatusChecker implementation
  - [x] 6.1 Implement `ShizukuStatusCheckerImpl`
    - Implement `isInstalled()` by querying `PackageManager` for `moe.shizuku.privileged.api`
    - Implement `isRunning()` by calling `Shizuku.pingBinder()`
    - Implement `hasPermission()` by calling `Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED`
    - Implement `requestPermission(requestCode)` by calling `Shizuku.requestPermission(requestCode)`
    - _Requirements: 3.3_

  - [x] 6.2 Write unit tests for `ShizukuStatusCheckerImpl`
    - Test `isInstalled()` returns `false` when package is absent (mock `PackageManager`)
    - Test `isInstalled()` returns `true` when package is present
    - Test `isRunning()` returns correct value based on `Shizuku.pingBinder()` result
    - Test `hasPermission()` maps `PERMISSION_GRANTED` / `PERMISSION_DENIED` correctly
    - _Requirements: 3.3_

- [x] 7. HotspotController implementation
  - [x] 7.1 Implement `HotspotControllerImpl`
    - Implement `isHotspotEnabled()` using `WifiManager.isWifiApEnabled()`
    - Implement `enableHotspotIfNeeded()` with three guard checks in order: `isHotspotEnabled()` (no-op if true), `Shizuku.pingBinder()` (post `CHANNEL_ERRORS` notification if false), `Shizuku.checkSelfPermission()` (post `CHANNEL_ERRORS` notification if denied)
    - On guards passing, call `TetheringManager.startTethering(TETHERING_WIFI, ...)` via Shizuku binder; handle `onTetheringFailed(error)` by posting a `CHANNEL_ERRORS` notification with the error code
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 2.7_

  - [x] 7.2 Write unit tests for `HotspotControllerImpl`
    - Test `enableHotspotIfNeeded()` is a no-op when `isHotspotEnabled()` returns `true`
    - Test posts Shizuku-not-active notification when `pingBinder()` fails
    - Test posts failure notification when `onTetheringFailed()` fires with an error code
    - _Requirements: 3.2, 3.3, 3.4_

- [x] 8. Checkpoint — core helpers complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. BluetoothConnectionReceiver
  - [x] 9.1 Implement `BluetoothConnectionReceiver`
    - Listen for `BluetoothDevice.ACTION_ACL_CONNECTED` in `onReceive`
    - Extract device name from `BluetoothDevice` extra
    - If name equals `"CHEVROLET6572"` AND `AutomationPreferences.isAutomationEnabled()` is `true`, call `HotspotController.enableHotspotIfNeeded()`; otherwise take no action
    - _Requirements: 2.3, 2.4, 2.5_

  - [x] 9.2 Write property test — non-target device name triggers no action
    - **Property 2: Non-target device name triggers no action**
    - Use `Arb.string()` filtered to exclude `"CHEVROLET6572"`, automation active, call receiver's `onReceive` with a mock intent → verify `enableHotspotIfNeeded()` never called (MockK `verify(exactly = 0)`)
    - Tag: `Feature: bluetooth-hotspot-enabler, Property 2: Non-target device name triggers no action`
    - Minimum 100 iterations
    - **Validates: Requirements 2.4**

  - [x] 9.3 Write property test — inactive automation suppresses all events
    - **Property 3: Inactive automation suppresses all events**
    - Use `Arb.string()` (all device names including `"CHEVROLET6572"`), automation inactive → verify `enableHotspotIfNeeded()` never called
    - Tag: `Feature: bluetooth-hotspot-enabler, Property 3: Inactive automation suppresses all events`
    - Minimum 100 iterations
    - **Validates: Requirements 2.5**

  - [x] 9.4 Write unit tests for `BluetoothConnectionReceiver`
    - Test calls `enableHotspotIfNeeded()` exactly once when name is `"CHEVROLET6572"` and automation is active
    - Test takes no action when name is `"CHEVROLET6572"` but automation is inactive
    - Test takes no action when name does not match and automation is active
    - _Requirements: 2.3, 2.4, 2.5_

- [x] 10. BluetoothMonitorService
  - [x] 10.1 Implement `BluetoothMonitorService`
    - Extend `Service`, declare `START_STICKY` return from `onStartCommand`
    - In `onStartCommand`: call `startForeground()` with `NotificationHelper.buildServiceNotification(true)`, then dynamically register `BluetoothConnectionReceiver` with an `IntentFilter` for `BluetoothDevice.ACTION_ACL_CONNECTED`
    - In `onDestroy`: unregister `BluetoothConnectionReceiver`; post `NotificationHelper.buildServiceNotification(false)` (or update the notification) to show "Stopped"
    - _Requirements: 2.1, 2.2, 5.1, 5.2, 5.3, 5.4_

  - [x] 10.2 Write integration tests for service lifecycle
    - Test foreground service persists after `Activity.finish()`
    - Test service restarts within 10 seconds after process kill (START_STICKY)
    - Test toggle on starts service within 2 seconds; toggle off stops it
    - _Requirements: 2.2, 5.3, 5.4_

- [~] 11. BootReceiver
  - [x] 11.1 Implement `BootReceiver`
    - In `onReceive` for `ACTION_BOOT_COMPLETED`: call `AutomationPreferences.isAutomationEnabled()`; if `true`, call `context.startForegroundService(Intent(context, BluetoothMonitorService::class.java))`
    - _Requirements: 2.6, 5.5_

  - [x] 11.2 Write unit tests for `BootReceiver`
    - Test starts `BluetoothMonitorService` when automation was active (mock `AutomationPreferences`)
    - Test does NOT start service when automation was inactive
    - _Requirements: 2.6, 5.5_

  - [~] 11.3 Write integration test for boot receiver
    - Send `ACTION_BOOT_COMPLETED` broadcast and assert service starts within 30 seconds
    - _Requirements: 2.6, 5.5_

- [~] 12. Checkpoint — service layer complete
  - Ensure all tests pass, ask the user if questions arise.

- [~] 13. MainActivity
  - [x] 13.1 Implement `MainActivity` layout
    - Create `activity_main.xml` with a `SwitchMaterial` (`AutomationToggle`) and a dismissible Shizuku status `MaterialBanner` (or `TextView` banner)
    - _Requirements: 1.1_

  - [x] 13.2 Implement `MainActivity` logic
    - In `onResume`: read toggle state from `AutomationPreferences`; check Shizuku status via `ShizukuStatusChecker` and update the status banner accordingly (not installed → Play Store link; installed but not running → launch Shizuku app); disable `AutomationToggle` when Shizuku is not active
    - On toggle → on: verify `ShizukuStatusChecker.isRunning()` and `hasPermission()`; if not, show guidance and revert toggle; if yes, persist state and call `startForegroundService(BluetoothMonitorService)`
    - On toggle → off: persist state and call `stopService(BluetoothMonitorService)`
    - Catch `AutomationPrefsException` and display an inline error message
    - Catch `SecurityException` from `startForegroundService` and display an inline error message
    - Check and request Bluetooth and `POST_NOTIFICATIONS` runtime permissions via `PermissionHelper`; handle denial and permanent denial per requirements 4.3 and 4.4
    - Handle `Requirement 4.5`: show explanation before requesting `WRITE_SETTINGS` (note: Shizuku removes this need, so display a note that WRITE_SETTINGS is not required)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 4.1, 4.2, 4.3, 4.4_

  - [~] 13.3 Write unit tests for `MainActivity`
    - Test toggle defaults to off on first launch (no persisted state)
    - Test inline error shown on prefs write failure
    - Test toggle on/off never calls `HotspotController` directly
    - Test toggle is disabled when `ShizukuStatusChecker.isRunning()` returns `false`
    - _Requirements: 1.1, 1.5, 1.6, 1.7_

  - [~] 13.4 Write Espresso integration tests for `MainActivity`
    - Test toggle on starts `BluetoothMonitorService`; toggle off stops it
    - Test Shizuku status banner is shown when Shizuku is not active
    - _Requirements: 1.1, 1.2, 1.3, 2.2_

- [ ] 14. End-to-end integration test
  - [~] 14.1 Write end-to-end Espresso/instrumented test
    - With Shizuku active on the test device, send a fake `ACTION_ACL_CONNECTED` broadcast with device name `"CHEVROLET6572"` and assert that `TetheringManager.startTethering()` is invoked (via mock or observable side-effect)
    - _Requirements: 2.3, 3.1_

- [~] 15. Final checkpoint — all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical milestones
- Property tests validate universal correctness properties (minimum 100 iterations each)
- Unit tests validate specific examples and edge cases using JUnit 5 + MockK
- Integration/Espresso tests validate lifecycle and end-to-end behavior on device
- Shizuku does not survive device reboot on non-rooted devices — this is a platform limitation, not a bug; the test for requirement 2.6 should account for manual Shizuku re-activation
- `minSdk` is set to 31 to align with the `BLUETOOTH_CONNECT` requirement; the `android:maxSdkVersion="30"` attribute on the legacy `BLUETOOTH` permission handles pre-31 gracefully in the manifest

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1"] },
    { "id": 1, "tasks": ["3.1", "4.1", "5.1", "6.1"] },
    { "id": 2, "tasks": ["3.2", "3.3", "4.2", "4.3", "5.2", "5.3", "6.2", "7.1"] },
    { "id": 3, "tasks": ["7.2", "9.1"] },
    { "id": 4, "tasks": ["9.2", "9.3", "9.4", "10.1"] },
    { "id": 5, "tasks": ["10.2", "11.1", "13.1"] },
    { "id": 6, "tasks": ["11.2", "11.3", "13.2"] },
    { "id": 7, "tasks": ["13.3", "13.4", "14.1"] }
  ]
}
```
