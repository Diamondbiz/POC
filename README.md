End-to-end UI automation tests for the **HOT Android TV** app, driven directly through
ADB (no Appium, no Selenium). The framework validates each screen with a mix of
**UI XML dumps** and **pixel-level image comparison** against reference baselines.

**Maintainer:** [@Diamondbiz](https://github.com/Diamondbiz)

---

## Overview

This project drives a physical Android TV device over the network to verify the HOT
app's user journey and each individual screen. It is deliberately not built on top
of Appium/Selenium — instead it talks to the device via the `adb` command-line tool,
using `uiautomator dump` for UI hierarchy and `screencap` for screenshots.

The framework is **layered**, so adding a new screen is a small, isolated task
(see [Adding a New Screen](#adding-a-new-screen)).

---

## Prerequisites

- **macOS** — the framework currently uses macOS-specific absolute paths.
- **JDK 17** (verified: OpenJDK 17 via Homebrew).
- **Maven** 3.9+.
- **ADB** installed and on `PATH` (part of Android Platform Tools).
- **ImageMagick** installed — `magick` (v7) or `convert` (v6) must be on `PATH`.
- A physical Android TV device reachable over the network.

Verify the environment:

```bash
adb devices                 # should list your device
magick --version            # or: convert --version
java -version               # should show 17.x
mvn -version

Project Structure
src/main/java/AndroidTV/V3/
├── Models/         Domain models (Router, RouterType)
├── config/         TestConfig (constants + paths), RouterConfig (SSID → phone map)
├── core/           DeviceController (all ADB I/O), XmlParser, ScreenState, KeyCodes
├── flows/          LoginFlow, TestFlow, Preconditions (state enforcers)
├── pages/          LoginPage, OtpPage, LiveMosaicPage
├── profiles/       ScreenProfile + per-screen profile data classes
├── services/       LoginService, OtpService, SsidService, KeypadStateService
├── utils/          TestLogger, ImageComparator, CropUtil, ArtifactReporter, AssertionRunner
└── validators/     ScreenValidator, ScreenAssertionResult

src/test/java/AndroidTV/V3/Tests/User/
├── FullLoginTest.java                End-to-end journey
└── ScreensAssertion/
    ├── LoginScreenAssertionTest.java Per-screen assertion
    ├── OtpScreenAssertionTest.java   Per-screen assertion
    └── LiveMosaicAssertionTest.java  Per-screen assertion

Available Tests

Test	                         Purpose
FullLoginTest                  End-to-end: cold start → logout (pm clear) → login screen → phone → OTP → Live Mosaic.
                               Combines all three per-screen assertions in one run with no repeated navigation.


LoginScreenAssertionTest       Standalone: assert the Login screen. Assumes no prior state — uses Preconditions to get there.

OtpScreenAssertionTest         Standalone: drive the login flow to reach the OTP screen, then assert it.

LiveMosaicAssertionTest        Standalone: drive login + OTP to reach Live Mosaic, then assert it.


Each test is standalone — it enforces its own preconditions, so it can run
individually or in any order. Each test is also chainable — running them one
after another reuses the state the previous test left on the device.

How to Run
Compile (test classes are not compiled by mvn compile — always run test-compile first):

cd /Users/Johnny/IdeaProjects/POC
mvn clean test-compile

Run a specific test (replace <Class> with the fully qualified class name):
mvn exec:java -Dexec.mainClass=<Class> -Dexec.classpathScope=test

Examples:
# Full login journey
mvn exec:java -Dexec.mainClass=AndroidTV.V3.Tests.User.FullLoginTest -Dexec.classpathScope=test

# Login screen only
mvn exec:java -Dexec.mainClass=AndroidTV.V3.Tests.User.ScreensAssertion.LoginScreenAssertionTest -Dexec.classpathScope=test

# OTP screen only
mvn exec:java -Dexec.mainClass=AndroidTV.V3.Tests.User.ScreensAssertion.OtpScreenAssertionTest -Dexec.classpathScope=test

# Live Mosaic only
mvn exec:java -Dexec.mainClass=AndroidTV.V3.Tests.User.ScreensAssertion.LiveMosaicAssertionTest -Dexec.classpathScope=test


Note: recompile after every code change. mvn exec:java does not
recompile — it runs whatever is currently in target/classes. If you edited
a .java file, run mvn clean test-compile first, or chain the commands:
mvn clean test-compile exec:java -Dexec.mainClass=<Class> -Dexec.classpathScope=test

Tests currently use a public static void main(...) entry point, not TestNG/JUnit.
A TestNG migration is planned.

How the Framework Works
The framework is layered, and every layer has one job:

Layer                                              	Role
Detectors (ScreenState):              Read-only. Answer "where is the device right now?" Never change state.

Preconditions (Preconditions):        Enforce that the device reaches a required state. Uses detectors to check first, then acts only if needed.

Profiles (LoginScreenProfile, ...)   Pure data — the expected elements and crop coordinates for one screen.

Validator (ScreenValidator):          One generic class that asserts any profile.

Runner (AssertionRunner).            Bundles validator + JSON log + console links + screenshot/crop folder handling.

The layers only depend downward: tests call the runner, the runner calls the
validator, the validator reads profiles and calls core. Nothing in core/ or
profiles/ knows about the tests.

Keypad State Detection
The HOT keypad can be in any of 12 states — digits 0–9, the Back button, and
the Next button. When the tests need to enter digits, they:

Detect the currently selected key using two independent signals:

XML — the selected key is wrapped in an extra View node in the hierarchy.

Image — a crop of each key is compared to reference images under
Screens/Expected/Keypad References/Key selected/.

Reconcile the two signals (image is primary, XML is secondary).

Navigate by DPAD from the detected position to each target digit.

When the detected key doesn't match the app's fresh default (0), the framework
prints a warning. This makes unexpected starting states visible in the log — useful
for debugging app focus behavior.

Configuration
Two files hold all configuration:


config/TestConfig.java
The single source of truth for:

DEVICE_UDID — network address of the target device (default 192.168.1.165:5555).

Folder paths — CURRENT_SCREEN_DIR, FAIL_DIR, PASS_DIR, EXPECTED_DIR, XML_DIR, LOGS_DIR.

Timeouts — app load, OTP load, screen marker wait, foreground wait.

Test data — REGULAR_PHONE_NUMBER, REGULAR_OTP.

Image comparison — SIMILARITY_THRESHOLD (95.0), PIXEL_TOLERANCE (10).

config/RouterConfig.java
Maps Wi-Fi SSID → expected phone number. Add new routers here. The SsidService
reads the current SSID at test start and picks the right phone number automatically.
If the SSID isn't in the map, it falls back to TestConfig.REGULAR_PHONE_NUMBER
and logs a warning.

Artifacts & Where They Land
Each test run writes to timestamped folders:

Folder                      Contents                                       Tracked in Git?

Screens/Expected/           Reference baselines and crop images            ✅ Yes

Screens/Current screen/.    Screenshots and crops from the current run.    ❌ No

Screens/Fail/               Failed crops and diagnostic screenshots        ❌ No

logs/                       JSON test logs (one per assertion)             ❌ No

xml/                        uiautomator XML dumps                          ❌ No

test-logs/                  Plain text logger output                       ❌ No

Every file the framework produces is printed in the console as a clickable
file:// link plus an open "<path>" command for terminal use.

Adding a New Screen
Adding a new screen assertion is a three-step process. No existing file changes.

Step 1 — Write a profile
Create profiles/XScreenProfile.java. It is pure data:

public class XScreenProfile {
    public static ScreenProfile get() {
        return ScreenProfile.builder("XScreen")
                .marker(ElementType.RESOURCE_ID, "some_unique_id")
                .element("elementName",   ElementType.RESOURCE_ID, "res_id")
                .element("anotherText",   ElementType.TEXT_PRESENT_ANYWHERE, "visible text")
                .crop("crop_name", new int[]{x1, y1, x2, y2}, "/full/path/to/baseline.png")
                .build();
    }
}

Element types:

RESOURCE_ID — matches resource-id="..." in the XML.

TEXT — matches text="..." exactly.

TEXT_PRESENT_ANYWHERE — matches the string anywhere in the XML.

Crops are optional. If the screen has no stable visual baseline, skip them.

Step 2 — Write a test
Create Tests/User/ScreensAssertion/XScreenAssertionTest.java. Copy the structure
of an existing test (e.g. OtpScreenAssertionTest), change the target profile,
and adjust the precondition (ensureOnLoginScreen, ensureOnOtpScreen,
ensureLoggedIn, or ensureOnScreen).

Step 3 — Run it
mvn clean test-compile
mvn exec:java -Dexec.mainClass=AndroidTV.V3.Tests.User.ScreensAssertion.XScreenAssertionTest -Dexec.classpathScope=test


That's it. The generic ScreenValidator handles element checks, crops, screenshots,
JSON logs, and console output. No changes to core/, utils/, or any other test.


Recent Major Changes
Class naming convention: removed all ADB prefixes. 
Acronyms in class names are lowercase (Otp, Ssid, Wifi). Test classes end in Test.
FullLoginTest added — the complete journey from cold start to Live Mosaic.
Keypad state detection added to both LoginScreenAssertionTest and OtpScreenAssertionTest — prints the current keypad state and warns if it isn't the fresh default (0).
Dead code removed: unused ADBBasePage, ADBLiveMosaicService, three per-screen validators, six obsolete test classes, two obsolete TestNG XML suites.
SsidService added — reads the current Wi-Fi SSID and resolves the correct phone number from RouterConfig.


Known Limitations
Absolute paths are hardcoded to /Users/Johnny/IdeaProjects/POC. Other machines require editing TestConfig.PROJECT_ROOT.
Logout uses pm clear in FullLoginTest. This wipes all app data (not just the session). The UI sign-out path (side menu → Settings) is planned as a separate test.
Tests use main(), not TestNG/JUnit. Migration to TestNG is planned for parallel and multi-device execution.
Image comparison is pixel-sensitive — crops and baselines must match dimensions exactly. See Screens/Expected/ for current references.
Single device only. The framework currently targets one DEVICE_UDID at a time.

---

## Then Commit and Push

```bash
cd /Users/Johnny/IdeaProjects/POC
git add README.md
git commit -m "Add README describing framework, tests, and how to add a new screen"
git push
