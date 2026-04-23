# JackAssembler

JackAssembler は、Jack VM Converter が出力した `.asm` ファイルを読み取り、Hack マシン語の `.hack` ファイルに変換するアセンブラです。

## 役割

このモジュールは次の流れを担当します。

1. 設定ファイル `config.txt` を読み込む
2. 入力 `.asm` ファイルの存在を確認する
3. ラベル、変数、命令を解決して Hack バイナリに変換する
4. `.hack` ファイルとして出力する

## ディレクトリ構成

```text
JackAssembler/
├── MainASM.java            # 実行入口
├── README.md               # この説明書
├── config/                 # 設定クラス置き場
│   └── ASMConfig.java      # 入力・出力パス設定の読込
├── core/                   # アセンブルの中心ロジック
│   ├── AssemblerEngine.java
│   ├── Parse.java
│   ├── SecondParse.java
│   └── SymbolTable.java
├── io/                     # 入出力まわり
│   ├── FileManager.java    # 出力先ディレクトリ準備
│   ├── HackWriter.java     # .hack 書き込み
│   └── SourceProject.java  # 入力ファイルと出力パスの管理
└── out/                    # コンパイル済み class の出力先
```

## 設定ファイル

設定は [config.txt](../../config.txt) から読み込まれます。

主な項目は次のとおりです。

- `asm.in` : 入力する `.asm` ファイルのパス
- `asm.out` : 出力する `.hack` ファイルのパス

例:

```txt
asm.in=Jack_Buildsystem/JackVMConverter/jack_build/output.asm
asm.out=bin/Prog.hack
```

## 実行方法

通常は上位の `compile.bat` または `compile.sh` から呼び出します。

```bat
java -Dfile.encoding=UTF-8 -cp "Jack_Buildsystem/JackAssembler/out" Jack_Project.Jack_Buildsystem.JackAssembler.MainASM
```

## 生成物

- 入力: `.asm` ファイル
- 出力: `.hack` ファイル
- 既定の出力例: [../../bin/Prog.hack](../../bin/Prog.hack)

## 補足

- `MainASM.java` がエントリーポイントです。
- 入力ファイルの存在確認は `MainASM.java` と `SourceProject.java` が担当します。
- 命令の第1パスと第2パスは `core/AssemblerEngine.java` がまとめて実行します。
- 文字列の変換表は `core/Parse.java`、`core/SecondParse.java`、`core/SymbolTable.java` に分かれています。
- 出力先フォルダの準備は `io/FileManager.java`、実ファイルの書き込みは `io/HackWriter.java` が担当します。

## 関連ドキュメント

- `../../README.md` (プロジェクト全体)
- `../JackCompiler/README.md` (DSL を含むコンパイラ解説)
- `../JackCompiler/core/DSL/DSLGenerator.java` (行列 DSL 実装)
- `../JackVMConverter/README.md`
