@echo off
setlocal
rem このバッチファイルがある場所をカレントディレクトリにする
cd /d %~dp0

set SRC=hack_emu.c
set EXE=hack_emu.exe
set TARGET=..\bin\Prog.hack

echo ===========================================
echo  Simulator Build ^& Run Stage
echo ===========================================

rem 1. コンパイル (最適化オプション -O3 を付けて高速化)
echo [1/2] Compiling %SRC%...
gcc -O3 %SRC% -o %EXE%

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %errorlevel%
)

echo [OK] Compilation successful.

rem 2. 実行 (デフォルトで ../bin/Prog.hack を読み込む)
echo [2/2] Running %EXE% with %TARGET%...
echo -------------------------------------------

if exist "%TARGET%" (
    %EXE% "%TARGET%"
) else (
    echo [WARNING] Target file not found: %TARGET%
    echo Starting simulator without arguments...
    %EXE%
)

echo -------------------------------------------
echo Execution finished.
pause