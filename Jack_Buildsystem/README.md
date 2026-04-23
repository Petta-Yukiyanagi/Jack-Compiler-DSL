# Jack_Project

Jack_Project は、Nand2Tetris の Jack ソースを `JackCompiler` で VM に変換し、`JackVMConverter` で ASM にまとめ、`JackAssembler` で Hack マシン語に変換する一連のビルド基盤です。

この README ではリポジトリ全体の構成と、各モジュールの役割をまとめます。詳細は各モジュールの README を参照してください。

## 全体の流れ

1. `JackCompiler` が `.jack` を読み込み、`.vm` を出力する
2. `JackVMConverter` が `.vm` を読み込み、`.asm` を出力する
3. `JackAssembler` が `.asm` を読み込み、`.hack` を出力する
4. `compile.bat` で 3 つの工程を順番に実行する

## ディレクトリ構成

```text
Jack_Project/
├── README.md
├── compile.bat          # Windows 用の一括ビルド・実行スクリプト
├── compile.sh           # Unix 系向けのスクリプト
├── config.txt           # 3 モジュール共通の設定ファイル
├── bin/                 # 最終的な .hack 出力先
├── src/                 # Jack の入力ソース例
├── JackOS/              # Jack 標準ライブラリ
└── Jack_Buildsystem/
	├── JackCompiler/    # Jack -> VM 変換モジュール
	├── JackVMConverter/ # VM -> ASM 変換モジュール
	└── JackAssembler/   # ASM -> Hack 変換モジュール
```

## 各モジュール

- [JackCompiler/README.md](JackCompiler/README.md)
- [JackVMConverter/README.md](JackVMConverter/README.md)
- [JackAssembler/README.md](JackAssembler/README.md)

## 設定ファイル

共通設定は [config.txt](../config.txt) で管理しています。

主な項目:

- `mode` : `xml` か `vm` を指定する
- `in` : Jack 入力ディレクトリ
- `os` : JackOS の場所
- `out` : JackCompiler の出力先
- `vm.in` : JackVMConverter の入力先
- `vm.out` : JackVMConverter の出力先
- `asm.in` : JackAssembler の入力先
- `asm.out` : JackAssembler の出力先

## 実行方法

Windows では、基本的に [compile.bat](../compile.bat) を実行します。

```bat
..\compile.bat
```

内部では各モジュールを UTF-8 でコンパイルし、順番に実行します。

## 生成物

- `Jack_Buildsystem/JackCompiler/jack_build/` : JackCompiler の中間生成物
- `Jack_Buildsystem/JackVMConverter/jack_build/output.asm` : VM から生成された ASM
- `bin/Prog.hack` : 最終的な Hack バイナリ

## 補足

- `JackCompiler` は Jack 言語の解析と VM/XML 出力を担当します。
- `JackVMConverter` は VM 命令を ASM に変換します。
- `JackAssembler` は ASM を Hack バイナリに変換します。
- 文字コードは UTF-8 を前提にしています。
