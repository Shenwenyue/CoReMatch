@echo off

chcp 65001 >nul
setlocal EnableDelayedExpansion

set "MATCH_OUTPUT=detail"
if /I "%~1"=="simple" set "MATCH_OUTPUT=simple"
if /I "%~1"=="detail" set "MATCH_OUTPUT=detail"
if not "%~1"=="" if /I not "%~1"=="simple" if /I not "%~1"=="detail" (
    echo Usage: run.bat [simple^|detail]
    echo   simple  index stats and results only
    echo   detail  also print each matched event ^(default^)
    pause & exit /b 1
)
echo Match output: %MATCH_OUTPUT%

set "CWD=%~dp0"
if "%CWD:~-1%"=="\" set "CWD=%CWD:~0,-1%"
echo Current script directory (CWD): %CWD%

if not exist "%CWD%\out" mkdir "%CWD%\out"
echo Output directory: %CWD%\out

cd /d "%CWD%\.."
set "EXP_DIR=%cd%"
echo Experiments directory (EXP_DIR): %EXP_DIR%

cd /d "%EXP_DIR%\..\.."
set "ROOT_DIR=%cd%"
echo Project root directory (ROOT_DIR): %ROOT_DIR%

echo Building whole project ...
call mvn clean install -DskipTests
if errorlevel 1 (
    echo Maven build failed, abort.
    pause & exit /b 1
)

set "EXPERIMENTS_BASE_PATH=%ROOT_DIR%\data"
echo Experiments base path (EXPERIMENTS_BASE_PATH): %EXPERIMENTS_BASE_PATH%

cd /d "%EXP_DIR%"
if not exist pom.xml (
    echo [ERROR] pom.xml not found in %cd%
    pause & exit /b 1
)

echo [DEBUG] Running maven ...
call mvn clean compile "dependency:build-classpath" ^
    "-Dmdep.outputFile=load_classpath" ^
    "-Dmdep.overwriteIfNewer=true" ^
    -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven failed
    pause & exit /b 1
)

set "CLASSPATH="
for /f "usebackq delims=" %%j in ("load_classpath") do (
    if defined CLASSPATH (
        set "CLASSPATH=!CLASSPATH!;%%j"
    ) else (
        set "CLASSPATH=%%j"
    )
)
echo Classpath loaded successfully
set CLASSES=CoReMatch.pattern.experiments.CrimeWithComposite

for %%i in (%CLASSES%) do (
    echo ==================================================
    echo Running experiment: %%i
    echo Log file: %CWD%\out\CrimeWithComposite.out
    java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dcorematch.matchOutput=%MATCH_OUTPUT% -cp "target\classes;!CLASSPATH!" %%i > "%CWD%\out\CrimeWithComposite.out" 2>&1
    if errorlevel 1 (
        echo Error running experiment %%i
    ) else (
        echo Experiment %%i completed successfully
    )
    echo ----- begin log -----
    type "%CWD%\out\CrimeWithComposite.out"
    echo ----- end log -----
)

cd /d "%CWD%"
echo Script execution completed.
echo Results saved under: %CWD%\out
pause
endlocal
