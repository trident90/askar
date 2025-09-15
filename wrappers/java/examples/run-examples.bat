@echo off
rem Askar Java Examples Runner Script for Windows
rem This script builds and runs the Askar Java examples

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo === Askar Java Examples Runner ===
echo Current directory: %CD%

rem Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java not found. Please install Java 8 or higher.
    exit /b 1
)

echo Java version:
java -version

rem Check if Maven is available
mvn -version >nul 2>&1
if errorlevel 1 (
    echo Error: Maven not found. Please install Maven 3.6 or higher.
    exit /b 1
)

echo Maven version:
mvn -version | findstr "Apache Maven"

rem Set library path for native library
set ASKAR_LIB_PATH=%CD%\..\..\target\release
set PATH=%ASKAR_LIB_PATH%;%PATH%

echo Native library path: %ASKAR_LIB_PATH%

rem Check if native library exists
set NATIVE_LIB=%ASKAR_LIB_PATH%\aries_askar.dll

if not exist "%NATIVE_LIB%" (
    echo Warning: Native library not found at %NATIVE_LIB%
    echo Building native library...

    rem Build the native library
    cd ..\..\
    where cargo >nul 2>&1
    if errorlevel 1 (
        echo Error: Cargo not found. Please install Rust toolchain to build the native library.
        exit /b 1
    )
    cargo build --release
    if errorlevel 1 (
        echo Error: Failed to build native library.
        exit /b 1
    )
    echo Native library built successfully
    cd wrappers\java\examples
) else (
    echo Native library found: %NATIVE_LIB%
)

rem Check if parent wrapper JAR exists
set WRAPPER_JAR=..\target\aries-askar-0.4.5.jar
if not exist "%WRAPPER_JAR%" (
    echo Wrapper JAR not found. Building parent project...
    cd ..\
    mvn clean package -DskipTests
    if errorlevel 1 (
        echo Error: Failed to build wrapper JAR.
        exit /b 1
    )
    cd examples\
    echo Wrapper JAR built successfully
) else (
    echo Wrapper JAR found: %WRAPPER_JAR%
)

echo.
echo === Building Examples ===
mvn clean compile
if errorlevel 1 (
    echo Error: Failed to compile examples.
    exit /b 1
)

echo.
echo === Running Examples ===

echo.
echo --- Running BasicStoreExample ---
echo Main class: org.hyperledger.aries.askar.examples.BasicStoreExample

mvn exec:java ^
    -Dexec.mainClass="org.hyperledger.aries.askar.examples.BasicStoreExample" ^
    -Djava.library.path="%ASKAR_LIB_PATH%" ^
    -Dexec.cleanupDaemonThreads=false

if errorlevel 1 (
    echo Error: BasicStoreExample failed.
    exit /b 1
)
echo --- BasicStoreExample completed ---

echo.
echo --- Running CryptographyExample ---
echo Main class: org.hyperledger.aries.askar.examples.CryptographyExample

mvn exec:java ^
    -Dexec.mainClass="org.hyperledger.aries.askar.examples.CryptographyExample" ^
    -Djava.library.path="%ASKAR_LIB_PATH%" ^
    -Dexec.cleanupDaemonThreads=false

if errorlevel 1 (
    echo Error: CryptographyExample failed.
    exit /b 1
)
echo --- CryptographyExample completed ---

echo.
echo === All Examples Completed Successfully! ===

endlocal