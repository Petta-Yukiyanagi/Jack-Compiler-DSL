# Jack_Project

Jack_Project は、Nand2Tetris の Jack ソースを VM/ASM/Hack へ変換するビルド一式です。

本プロジェクトは、書籍「コンピュータシステムの理論と実装」（NAND2tetris）をベースに作成しています。

コンパイル対象の言語は Jack 言語で、これは「コンピュータシステムの理論と実装」で設計・使用される教育用言語です。
Nand2tetris プロジェクトの公式サイト: https://www.nand2tetris.org/

Keywords: Nand2tetris, NAND2Tetris, The Elements of Computing Systems, Jack language, Jack compiler, VM translator, Hack assembler, Hack emulator, Matrix DSL, Loop Unrolling

このプロジェクトには、通常の Jack 構文に加えて、行列演算向けの DSL が含まれています。
DSL の実装は `Jack_Buildsystem/JackCompiler/core/DSL/DSLGenerator.java` にあり、`dsl matrix` / `dsl set` / `dsl mul` を扱います。

## 特徴 (Features)

- Matrix DSL: 行列演算を Jack language にネイティブ統合し、`dsl matrix` / `dsl set` / `dsl mul` を提供。4x4・8x8・16x16 ではループ展開（Loop Unrolling）による最適化で高速な VM code 生成を実現。
- Integrated Pipeline: Java 製の Jack compiler / VM translator / assembler と、C 製の Hack emulator を統合。Nand2tetris ワークフロー（`.jack` -> `.vm` -> `.asm` -> `.hack`）を一貫して実行可能。
- Reproducible Build: `compile.bat` と `compile.sh` によるクロス環境ビルド、`config.txt` による入出力設定の一元化、`run_emu.bat` / `run_emu.sh` によるシミュレーション検証をサポート。

## 全体パイプライン

1. JackCompiler が `.jack` を `.vm` に変換
2. JackVMConverter が `.vm` を `.asm` に変換
3. JackAssembler が `.asm` を `.hack` に変換

最終出力は `bin/Prog.hack` です。

## ディレクトリ構成

```text
Jack_Project/
├── README.md
├── compile.bat
├── compile.sh
├── config.txt
├── src/                        # ユーザー側の Jack ソース
├── JackOS/                     # Jack 標準ライブラリ
├── bin/                        # 最終生成物 (.hack)
├── Jack_Buildsystem/
│   ├── README.md
│   ├── JackCompiler/
│   ├── JackVMConverter/
│   └── JackAssembler/
└── Jack_Simulation/
    ├── hack_emu.c
    ├── run_emu.sh
    ├── run_emu.bat
    └── hack_emu.exe
```

## DSL 構文

### 1. 行列領域の確保

```jack
dsl matrix A(4,4);
```

効果:
- 要素数 `rows * cols` の Array を確保
- 変数 `A` にベースアドレスを格納
- 行列サイズ情報をコンパイラ内部に記録

### 2. 行列要素の代入

```jack
dsl set A, i, j, value;
```

効果:
- `A[i][j]` の線形アドレスを計算して代入
- 添字 `i`, `j` と `value` は式を許容（`compileExpression` を使用）

### 3. 行列積

```jack
dsl mul C, A, B;
```

効果:
- `C = A × B` の VM コードを生成
- サイズに応じて実装を切替
  - 4x4: 完全アンローリング
  - 8x8: アンローリング（temp セーフ設計）
  - 16x16: アンローリング（static 使用）
  - その他: `Matrix.mul` 呼び出しの汎用経路

## DSL 利用例

`src/Main.jack` では、以下の流れで DSL を使います。

1. `dsl matrix` で A/B/C を確保
2. `dsl set` で A, B を初期化
3. `dsl mul` で C に積を計算
4. 必要に応じて `Memory.poke` で RAM にダンプして検証

## 実行方法

### Windows

```bat
compile.bat
```

### Unix系シェル

```sh
./compile.sh
```

## シミュレータ

`Jack_Simulation/` には、生成した Hack プログラムの動作確認に使う実行環境があります。

- `Jack_Simulation/hack_emu.exe`: Hack エミュレータ本体
- `Jack_Simulation/run_emu.sh`: Unix系シェル用の起動スクリプト
- `Jack_Simulation/run_emu.bat`: Windows 用の起動スクリプト

実行仕様（`hack_emu.c` 準拠）:

1. 引数なし実行時は既定で `../bin/Prog.hack` を読み込みます
2. 第1引数で任意の `.hack` ファイルを指定できます
3. 第2引数で最大ステップ数、 第3引数でトレースログ出力先を指定できます

例:

```bat
cd Jack_Simulation
hack_emu.exe ..\bin\Prog.hack
hack_emu.exe ..\bin\Prog.hack 5000000
hack_emu.exe ..\bin\Prog.hack 5000000 log\trace.txt
```

`run_emu.bat` / `run_emu.sh` は `hack_emu.c` を再コンパイルした上で、対象ファイル（既定 `../bin/Prog.hack`）を実行します。

起動例:

```bat
cd Jack_Simulation
run_emu.bat
```

```sh
cd Jack_Simulation
./run_emu.sh
```

補足:

- 実行結果は PASS/HALT フラグで判定されます。
  - `RAM[30000]` (`PASS_ADDR`) が `0xFFFF` なら PASS
  - `RAM[30001]` (`HALT_ADDR`) が非 0 なら FAIL（エラーコード）
- 実行後に `DEBUG AREA`（`RAM[30001]` 以降）が標準出力にダンプされます。
- 第3引数を指定した場合、トレースログもファイルに出力されます（例: `Jack_Simulation/log/trace.txt`）。
- 行列 DSL の検証では、`src/Main.jack` 内の `Memory.poke` で RAM に書き出した値を観測する方法が有効です。
- 必要に応じて `check_comparator` 系スクリプトと併用し、期待値との差分を確認できます。

## 設定

ビルド設定は `config.txt` で管理します。

主なキー:
- `mode` : `xml` または `vm`
- `in` : 入力 `.jack` ディレクトリ
- `os` : JackOS ディレクトリ
- `out` : JackCompiler 出力ルート
- `vm.in`, `vm.out` : VMConverter 入出力
- `asm.in`, `asm.out` : Assembler 入出力

## 関連 README

- `Jack_Buildsystem/README.md`
- `Jack_Buildsystem/JackCompiler/README.md`
- `Jack_Buildsystem/JackVMConverter/README.md`
- `Jack_Buildsystem/JackAssembler/README.md`
- `Jack_Buildsystem/JackCompiler/core/DSL/DSLGenerator.java` (DSL 実装本体)

## 補足

- `JackCompiler` は出力名生成時に `.jack` 拡張子を除去して `.vm`/`.xml` を付与します。
- DSL の最適化方針を変更する場合は `DSLGenerator.java` を中心に確認してください。

## ライセンス

このプロジェクトは MIT License のもとで公開されています。

- `LICENSE`
