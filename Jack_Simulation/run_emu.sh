#!/bin/sh

# run_emu.bat と同等の処理を POSIX 系シェルで実行する

set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || {
    echo "[ERROR] Failed to change directory: $SCRIPT_DIR"
    exit 1
}

SRC="hack_emu.c"
EXE="hack_emu.exe"
TARGET="../bin/Prog.hack"

echo "==========================================="
echo "  Simulator Build & Run Stage"
echo "==========================================="

echo "[1/2] Compiling $SRC..."
gcc -O3 "$SRC" -o "$EXE"

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed."
    exit 1
fi

echo "[OK] Compilation successful."

echo "[2/2] Running $EXE with $TARGET..."
echo "-------------------------------------------"

if [ -f "$TARGET" ]; then
    ./$EXE "$TARGET"
else
    echo "[WARNING] Target file not found: $TARGET"
    echo "Starting simulator without arguments..."
    ./$EXE
fi

echo "-------------------------------------------"
echo "Execution finished."

printf 'Press any key to continue . . .'
if [ -r /dev/tty ]; then
    IFS= read -r _ </dev/tty
else
    IFS= read -r _
fi
echo
