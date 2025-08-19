@echo off
chcp 65001 >nul
echo ========================================
echo Tobacco Weight System - License Manager
echo ========================================
echo.

REM Quick check for essential files
if not exist "target\classes\com\tobacco\weight\license\LicenseManagementUI.class" (
    echo Error: Compiled class files not found
    echo Please compile project first using: mvn compile
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

if not exist "target\dependency" (
    echo Error: Dependency directory not found
    echo Please copy dependencies first using: mvn dependency:copy-dependencies
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)
echo.

echo Starting license management interface...
echo Note: If interface does not appear, check firewall or security software
echo.

REM Change to project directory and start license management interface
cd /d "d:\_TA_Work\YC\tobacco_weight_windows"
java --module-path "target\dependency" --add-modules javafx.controls,javafx.fxml -cp "target\classes;target\dependency\*;." com.tobacco.weight.license.LicenseManagementUI

REM Save exit code
set EXIT_CODE=%errorlevel%

echo.
if %EXIT_CODE% neq 0 (
    echo Program exited with error code: %EXIT_CODE%
    echo Please check log files or contact technical support
) else (
    echo Program exited normally
)

echo.
echo Press any key to close window...
pause >nul