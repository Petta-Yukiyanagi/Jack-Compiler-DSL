#!/bin/sh

# compile.bat と同等の処理を POSIX 系シェルで実行する

set -u

error_exit() {
    code="${1:-1}"
    echo "[ERROR] Build failed."
    printf 'Press any key to continue . . .'
    if [ -r /dev/tty ]; then
        IFS= read -r _ </dev/tty
    else
        IFS= read -r _
    fi
    echo
    exit "$code"
}

# スクリプトのあるフォルダを起点にする
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || error_exit 1

BUILD_ROOT="$SCRIPT_DIR/Jack_Buildsystem"
COMPILER_SRC="$BUILD_ROOT/JackCompiler"
COMPILER_OUT="$COMPILER_SRC/out"
VM_SRC="$BUILD_ROOT/JackVMConverter"
VM_OUT="$VM_SRC/out"
ASM_SRC="$BUILD_ROOT/JackAssembler"
ASM_OUT="$ASM_SRC/out"
HACK_OUT="$SCRIPT_DIR/bin/Prog.hack"

collect_java_sources() {
    find "$1" -type f -name '*.java' | sort
}

compile_sources() {
    source_dir="$1"
    output_dir="$2"
    sources=$(collect_java_sources "$source_dir") || return 1

    if [ -z "$sources" ]; then
        echo "[ERROR] No Java sources found in: $source_dir"
        return 1
    fi

    javac -encoding UTF-8 -d "$output_dir" $sources
}

echo "[1/7] Cleaning up generated source code only..."

# --- 1. JackCompilerの出力（VMファイル群）をリセット ---
rm -rf "$BUILD_ROOT/JackCompiler/jack_build"
mkdir -p "$BUILD_ROOT/JackCompiler/jack_build" || error_exit $?

# --- 2. VMConverterの出力（ASMファイル）をリセット ---
rm -f "$BUILD_ROOT/JackVMConverter/jack_build/output.asm"

# --- 3. JackAssemblerの出力（HACKファイル）をリセット ---
# ※compile.bat と同じ対象を削除
rm -f "$HACK_OUT"

# class用フォルダがない場合のみ作成（削除はしない）
mkdir -p "$COMPILER_OUT" "$VM_OUT" "$ASM_OUT" || error_exit $?

echo
echo "[2/7] Compiling JackCompiler..."
compile_sources "$COMPILER_SRC" "$COMPILER_OUT" || error_exit $?

echo
echo "[3/7] Compiling JackVMConverter..."
compile_sources "$VM_SRC" "$VM_OUT" || error_exit $?

echo
echo "[4/7] Compiling JackAssembler..."
compile_sources "$ASM_SRC" "$ASM_OUT" || error_exit $?

echo
echo "[5/7] Running JackCompiler (Jack > VM)"
java -Dfile.encoding=UTF-8 -cp "$COMPILER_OUT" Jack_Project.Jack_Buildsystem.JackCompiler.Main || error_exit $?

echo
echo "[6/7] Running JackVMConverter (VM > ASM)"
java -Dfile.encoding=UTF-8 -cp "$VM_OUT" Jack_Project.Jack_Buildsystem.JackVMConverter.MainVM || error_exit $?

echo
echo "[7/7] Running JackAssembler (ASM > HACK)"
java -Dfile.encoding=UTF-8 -cp "$ASM_OUT" Jack_Project.Jack_Buildsystem.JackAssembler.MainASM || error_exit $?

echo
echo "Full Pipeline Successful!"
printf 'Press any key to continue . . .'
if [ -r /dev/tty ]; then
    IFS= read -r _ </dev/tty
else
    IFS= read -r _
fi
echo