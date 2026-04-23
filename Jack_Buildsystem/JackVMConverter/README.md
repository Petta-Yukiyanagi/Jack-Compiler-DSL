# JackVMConverter

JackVMConverter は、Jack コンパイラが出力した `.vm` ファイルを読み取り、Hack アセンブリ `.asm` にまとめて出力する変換モジュールです。

## 役割

このモジュールは次の流れを担当します。

1. 入力元ディレクトリから `.vm` ファイルを収集する
2. `Sys.vm` を先頭にして変換順を整える
3. 各 VM 命令を解析する
4. `.asm` ファイルとして出力する

## ディレクトリ構成

```text
JackVMConverter/
├── MainVM.java          # 実行入口
├── config/
│   └── VMConfig.java    # 入力・出力パス設定の読込
├── core/
│   ├── Parser.java      # VM 1行ごとの解析
│   └── VMTranslatorEngine.java
├── instruction/
│   └── VMCommand.java   # 命令処理の共通インターフェース
├── io/
│   ├── CodeWriter.java  # asm 出力
│   └── SourceProject.java # 入力 VM ファイル収集
├── jack_build/
│   └── output.asm       # 生成物の出力先例
└── out/                 # コンパイル済み class の出力先
```

## 設定ファイル

設定は [config.txt](../../config.txt) から読み込まれます。

主な項目:

- `vm.in` : 変換元 `.vm` ファイルの入力ディレクトリ
- `vm.out` : `.asm` の出力先ファイル

例:

```txt
vm.in=Jack_Buildsystem/JackCompiler/jack_build/vm
vm.out=Jack_Buildsystem/JackVMConverter/jack_build/output.asm
```

## 実行方法

通常は上位のビルド手順から呼び出します。

```bat
java -cp "Jack_Buildsystem/JackVMConverter/out" Jack_Project.Jack_Buildsystem.JackVMConverter.MainVM
```

設定ファイルを明示したい場合は、引数にパスを渡します。

```bat
java -cp "Jack_Buildsystem/JackVMConverter/out" Jack_Project.Jack_Buildsystem.JackVMConverter.MainVM config.txt
```

## 生成物

- 入力 VM ファイル群
- 結合済みの ASM ファイル
- 既定の出力例: [jack_build/output.asm](jack_build/output.asm)

## 補足

- `MainVM` は設定ファイルを読み取り、入力ディレクトリを走査します。
- VM 命令の解析は `core/Parser.java` が担当します。
- 実際の命令変換は `core/VMTranslatorEngine.java` が担当します。
- 出力ファイルの書き込みは `io/CodeWriter.java` が担当します。

## 関連ドキュメント

- `../../README.md` (プロジェクト全体)
- `../JackCompiler/README.md` (DSL を含むコンパイラ解説)
- `../JackCompiler/core/DSL/DSLGenerator.java` (行列 DSL 実装)
- `../JackAssembler/README.md`
