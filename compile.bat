@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion
rem スクリプトのあるフォルダを起点にする
cd /d "%~dp0"

set "PROJECT_ROOT=%~dp0"
set "BUILD_ROOT=%PROJECT_ROOT%Jack_Buildsystem"
set "COMPILER_SRC=%BUILD_ROOT%\JackCompiler"
set "COMPILER_OUT=%COMPILER_SRC%\out"
set "VM_SRC=%BUILD_ROOT%\JackVMConverter"
set "VM_OUT=%VM_SRC%\out"
set "ASM_SRC=%BUILD_ROOT%\JackAssembler"
set "ASM_OUT=%ASM_SRC%\out"
set "HACK_OUT=%PROJECT_ROOT%bin\Prog.hack"

echo [1/7] Cleaning up generated source code only...

rem --- 1. JackCompilerの出力（VMファイル群）をリセット ---
if exist "%BUILD_ROOT%\JackCompiler\jack_build" ( rd /s /q "%BUILD_ROOT%\JackCompiler\jack_build" )
mkdir "%BUILD_ROOT%\JackCompiler\jack_build"

rem --- 2. VMConverterの出力（ASMファイル）をリセット ---
rem 特定の出力ファイルを消去する場合
if exist "%BUILD_ROOT%\JackVMConverter\jack_build\output.asm" ( del "%BUILD_ROOT%\JackVMConverter\jack_build\output.asm" )

rem --- 3. JackAssemblerの出力（HACKファイル）をリセット ---
rem ※通常、JavaのFileWriterが上書きしますが、念のため削除
if exist "%HACK_OUT%" ( del "%HACK_OUT%" )

rem class用フォルダがない場合のみ作成（削除はしない）
if not exist "%COMPILER_OUT%" mkdir "%COMPILER_OUT%"
if not exist "%VM_OUT%" mkdir "%VM_OUT%"
if not exist "%ASM_OUT%" mkdir "%ASM_OUT%"

echo.
echo [2/7] Compiling JackCompiler...
set "COMPILER_SOURCES="
for /r "%COMPILER_SRC%" %%f in (*.java) do (
    set "COMPILER_SOURCES=!COMPILER_SOURCES! "%%f""
)
javac -encoding UTF-8 -d "%COMPILER_OUT%" ^
    !COMPILER_SOURCES!

if %errorlevel% neq 0 goto error

echo.
echo [3/7] Compiling JackVMConverter...
set "VM_SOURCES="
for /r "%VM_SRC%" %%f in (*.java) do (
    set "VM_SOURCES=!VM_SOURCES! "%%f""
)
javac -encoding UTF-8 -d "%VM_OUT%" ^
    !VM_SOURCES!

if %errorlevel% neq 0 goto error

echo.
echo [4/7] Compiling JackAssembler...
set "ASM_SOURCES="
for /r "%ASM_SRC%" %%f in (*.java) do (
    set "ASM_SOURCES=!ASM_SOURCES! "%%f""
)
javac -encoding UTF-8 -d "%ASM_OUT%" ^
    !ASM_SOURCES!

if %errorlevel% neq 0 goto error

echo.
echo [5/7] Running JackCompiler (Jack ^> VM)
java -Dfile.encoding=UTF-8 -cp "%COMPILER_OUT%" Jack_Project.Jack_Buildsystem.JackCompiler.Main
if errorlevel 1 goto error

echo.
echo [6/7] Running JackVMConverter (VM ^> ASM)
java -Dfile.encoding=UTF-8 -cp "%VM_OUT%" Jack_Project.Jack_Buildsystem.JackVMConverter.MainVM
if errorlevel 1 goto error

echo.
echo [7/7] Running JackAssembler (ASM ^> HACK)
java -Dfile.encoding=UTF-8 -cp "%ASM_OUT%" Jack_Project.Jack_Buildsystem.JackAssembler.MainASM
if errorlevel 1 goto error

echo.
echo Full Pipeline Successful!
goto end

:error
echo [ERROR] Build failed.
pause
exit /b %errorlevel%

:end
pause