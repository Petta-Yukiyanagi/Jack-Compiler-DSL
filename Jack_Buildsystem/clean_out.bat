@echo off
rem スクリプトのあるフォルダを起点にする
cd /d %~dp0
cd ..

set ROOT_DIR=Jack_Buildsystem

echo [CLEAN] Removing all 'out' (class) directories...

rem --- 1. JackCompiler の out フォルダを削除 ---
if exist "%ROOT_DIR%\JackCompiler\out" (
    echo Deleting JackCompiler\out...
    rd /s /q "%ROOT_DIR%\JackCompiler\out"
)

rem --- 2. JackVMConverter の out フォルダを削除 ---
if exist "%ROOT_DIR%\JackVMConverter\out" (
    echo Deleting JackVMConverter\out...
    rd /s /q "%ROOT_DIR%\JackVMConverter\out"
)

rem --- 3. JackAssembler の out フォルダを削除 ---
if exist "%ROOT_DIR%\JackAssembler\out" (
    echo Deleting JackAssembler\out...
    rd /s /q "%ROOT_DIR%\JackAssembler\out"
)

echo.
echo All 'out' directories have been cleared.
echo Next compilation will be a full rebuild.
echo.

pause