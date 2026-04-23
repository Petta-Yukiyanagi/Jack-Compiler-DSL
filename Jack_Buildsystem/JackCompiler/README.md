# Jack Compiler

Nand2Tetris の Jack 言語を、XML 解析結果または VM コードに変換するコンパイラです。
入力した `.jack` ファイルを読み取り、設定に応じて `.xml` または `.vm` を出力します。

## できること

- Jack ソースを読み込む
- 構文解析結果を XML で出力する
- VM エミュレータ向けの `.vm` を出力する
- 設定ファイルと CLI 引数で動作を切り替える

## ディレクトリ構成

```text
JackCompiler/
├── config/                  # 実行時設定
├── core/                    # 解析・変換の中心ロジック
├── filemanager/             # 入出力ファイルの収集と準備
├── jack_build/              # 生成物の出力先例
├── out/                     # コンパイル済み class の出力先
├── Main.java                # エントリーポイント
└── README.md
```

## ビルド方法

通常は上位の [compile.bat](../../compile.bat) または [compile.sh](../../compile.sh) を使います。手動で確認する場合はこのディレクトリを起点にします。

```powershell
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null
javac -encoding UTF-8 -d out -sourcepath . Main.java
```

## 実行方法

```powershell
java -cp out Jack_Project.Jack_Buildsystem.JackCompiler.Main
```

## 設定の読み込み順

本コンパイラは次の順で設定を決めます。

1. CLI 引数
2. config.txt
3. デフォルト値

## オプション

実行時に `--キー 値` の形式で指定すると振る舞いを変更できます。

| オプション | デフォルト | 説明 |
| :--- | :--- | :--- |
| `--mode` | `xml` | 出力モード。`xml` (構文ツリーの出力) または `vm` (VMコードの出力) を指定します。 |
| `--in` | `.` | コンパイル対象のJackファイル、またはディレクトリのパスを指定します。 |
| `--out` | `build` | コンパイル結果を出力するルートディレクトリを指定します。 |
| `--dumpTokens` | (未指定) | トークナイズの結果を標準出力にダンプする場合は `--dumpTokens` を付与します。 |

## 実行例

### 例1: 特定のディレクトリを VM モードでコンパイルする

```powershell
java -cp out Jack_Project.Jack_Buildsystem.JackCompiler.Main --mode vm --in ./src_jack --out ./output
```

### 例2: XML モードでトークンを確認しながらコンパイルする

```powershell
java -cp out Jack_Project.Jack_Buildsystem.JackCompiler.Main --mode xml --dumpTokens
```

## 補足

- `Main.java` がエントリーポイントです。
- 生成先は `--out` で変更できます。
- `--mode vm` のときは VM 出力、`--mode xml` のときは XML 出力になります。

## 関連ドキュメント

- `../../README.md` (プロジェクト全体)
- `core/DSL/DSLGenerator.java` (行列 DSL 実装)
- `../JackVMConverter/README.md`
- `../JackAssembler/README.md`