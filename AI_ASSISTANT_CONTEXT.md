# Appium/Android Automation Project - AI Assistant Context

## Project Overview
This is a mobile UI automation project for testing the HOT Android application using Appium and TestNG.

## Technology Stack
- **Java Version**: 21
- **Build Tool**: Maven
- **Appium**: 9.2.0
- **Selenium**: 4.16.1
- **TestNG**: 7.8.0
- **Log4j**: 2.25.3
- **Jackson**: 2.15.2
- **Allure Reporting**: 2.29.1
- **AShot**: 1.5.4 (image comparison)

## Appium/Android Configuration

### Device Information
- **Device Name**: HOT Streamer V4
- **Device UDID**: 192.168.0.2:5555
- **Connection Type**: Network-connected Android device (ADB over TCP/IP)

### App Configuration
- **App Package**: com.hot.app
- **App Activity**: .MainActivity
- **Appium Server URL**: http://127.0.0.1:4723/

### Wait Settings
- **Implicit Wait**: 10 seconds
- **Explicit Wait**: 30 seconds

## Project Structure

### Source Code (`src/main/java/com/hot/automation/`)
- **BasePage/**: Base page object with common functionality
- **Pages/**: Page objects for different app screens
  - `HomePage.java`: Home screen interactions
  - `LiveMosaicPage.java`: Live mosaic screen
  - `LoginPage.java`: Regular user login
  - `MosdiLoginPage.java`: Mosdi user login
  - `OTPScreen.java`: OTP verification screen
- **Utils/**: Utility classes
  - `DriverManager.java`: Manages AndroidDriver instance (singleton pattern)
  - `ImageComparator.java`: Image comparison utilities
  - `ScreenshotUtil.java`: Screenshot capture utilities
  - `TestLogger.java`: Logging utilities
  - `DataGenerator.java`: Test data generation
- **config/**: Configuration constants
  - `TestConfig.java`: Centralized test configuration

### Test Code (`src/test/java/com/hot/automation/`)
- **Tests/MosdiUser/**: Tests for Mosdi user type
  - `FullLoginMosdiTest.java`: Complete login flow for Mosdi user
  - `RegularUserLogin.java`: First app launch scenarios
- **Tests/User/**: Tests for regular user type
  - `FullLoginTest.java`: Complete login flow for regular user
  - `RegularUserLogin.java`: First app launch scenarios
- **Validations/**: Validation utilities
  - `AuthValidator.java`: Authentication validation
  - `ManifestValidator.java`: App manifest validation
  - `NetworkValidator.java`: Network-related validations
  - `ScreenValidator.java`: Screen/UI validation
- **runners/**: Test suite runners
  - `MosdiTestSuite.java`: Programmatic TestNG suite for Mosdi tests
  - `UserTestSuite.java`: Programmatic TestNG suite for regular user tests
  - `TestSuite.java`: General test suite

### Configuration Files
- **pom.xml**: Maven dependencies and build configuration
- **src/test/resources/testng-multidevice.xml**: TestNG XML suite configuration

## Key Patterns

### Driver Management
The project uses a singleton pattern for driver management via `DriverManager`:
- `DriverManager.getDriver()`: Returns the active AndroidDriver instance
- `DriverManager.setDriver(AndroidDriver driver)`: Sets the driver instance
- `DriverManager.quitDriver()`: Quits and nullifies the driver

### Page Object Model
All page classes extend from `BasePage` and follow the Page Object Model pattern for maintainable test automation.

### Test Execution
Tests can be run via:
1. Maven: `mvn test` (uses testng-multidevice.xml)
2. TestNG suite runners: `MosdiTestSuite.java`, `UserTestSuite.java`
3. IDE: Right-click on test class and run

### Reporting
- Allure reports are generated in `allure-results/` directory
- Screenshots are saved in `screenshots/` directory
- Test logs are in `test-logs/` directory

## Important Notes for AI Assistant

1. **Driver Type**: Always use `AndroidDriver` from `io.appium.java_client.android.AndroidDriver`
2. **Device Connection**: The target device is connected via network (192.168.0.2:5555), not USB
3. **Appium Server**: Ensure Appium server is running on http://127.0.0.1:4723/ before executing tests
4. **Wait Strategies**: Use the configured implicit/explicit wait times from `TestConfig`
5. **Page Objects**: When suggesting new page classes, extend from `BasePage` and follow existing patterns
6. **Test Structure**: New tests should follow the existing package structure under `Tests/`
7. **Validation**: Use existing validator classes for common validation scenarios

## Common Imports
```java
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import com.hot.automation.Utils.DriverManager;
import com.hot.automation.config.TestConfig;
```

## Appium Capabilities (Typical Setup)
```java
DesiredCapabilities caps = new DesiredCapabilities();
caps.setCapability("deviceName", TestConfig.DEVICE_NAME);
caps.setCapability("udid", TestConfig.DEVICE_UDID);
caps.setCapability("platformName", "Android");
caps.setCapability("appPackage", TestConfig.APP_PACKAGE);
caps.setCapability("appActivity", TestConfig.APP_ACTIVITY);
caps.setCapability("automationName", "UiAutomator2");
caps.setCapability("noReset", true);
```
