package Jack_Project.Jack_Buildsystem.JackVMConverter.core;

import Jack_Project.Jack_Buildsystem.JackVMConverter.io.CodeWriter;

public class VMConverter {
    private static int callCounter = 0;
    private static int compareCounter = 0; // eq/gt/lt ラベル用

    // ★ 追加：現在の関数名（label/goto/if-goto のスコープ用）
    private static String currentFunction = "";

    // --------------------------------------------------
    // ブートストラップコード
    // --------------------------------------------------
    public static void writeInit(CodeWriter writer) {
        // SP を 256 に初期化
        writer.writeLine("@256");
        writer.writeLine("D=A");
        writer.writeLine("@SP");
        writer.writeLine("M=D");

        // Sys.init を呼び出す
        writeCall(writer, "Sys.init", 0);
    }

    // --------------------------------------------------
    // 算術 / 論理コマンド
    // --------------------------------------------------
    public static void writeArithmetic(CodeWriter writer, String command) {
        switch (command) {
            case "add":
                writer.writeLine("@SP");
                writer.writeLine("AM=M-1");
                writer.writeLine("D=M");
                writer.writeLine("A=A-1");
                writer.writeLine("M=D+M");
                break;

            case "sub":
                writer.writeLine("@SP");
                writer.writeLine("AM=M-1");
                writer.writeLine("D=M");
                writer.writeLine("A=A-1");
                writer.writeLine("M=M-D");
                break;

            case "neg":
                writer.writeLine("@SP");
                writer.writeLine("A=M-1");
                writer.writeLine("M=-M");
                break;

            case "and":
                writer.writeLine("@SP");
                writer.writeLine("AM=M-1");
                writer.writeLine("D=M");
                writer.writeLine("A=A-1");
                writer.writeLine("M=D&M");
                break;

            case "or":
                writer.writeLine("@SP");
                writer.writeLine("AM=M-1");
                writer.writeLine("D=M");
                writer.writeLine("A=A-1");
                writer.writeLine("M=D|M");
                break;

            case "not":
                writer.writeLine("@SP");
                writer.writeLine("A=M-1");
                writer.writeLine("M=!M");
                break;

            case "eq":
            case "gt":
            case "lt": {
                // 一意なラベル名を生成（グローバルにユニークなのでこのままで良い）
                String t = command.toUpperCase() + "_TRUE" + compareCounter;
                String e = command.toUpperCase() + "_END" + compareCounter;
                compareCounter++;

                // y = *SP--, x = *(SP-1) として x - y を評価
                writer.writeLine("@SP");
                writer.writeLine("AM=M-1");    // SP--, A=SP, M=*y
                writer.writeLine("D=M");       // D=y
                writer.writeLine("A=A-1");     // A=SP-1, M=x
                writer.writeLine("D=M-D");     // D = x - y

                // 条件に応じて TRUE ラベルへジャンプ
                writer.writeLine("@" + t);
                switch (command) {
                    case "eq" -> writer.writeLine("D;JEQ");
                    case "gt" -> writer.writeLine("D;JGT");
                    case "lt" -> writer.writeLine("D;JLT");
                }

                // false の場合: *SP = 0
                writer.writeLine("@SP");
                writer.writeLine("A=M-1");
                writer.writeLine("M=0");
                writer.writeLine("@" + e);
                writer.writeLine("0;JMP");

                // true の場合: *SP = -1
                writer.writeLine("(" + t + ")");
                writer.writeLine("@SP");
                writer.writeLine("A=M-1");
                writer.writeLine("M=-1");

                // END ラベル
                writer.writeLine("(" + e + ")");
                break;
            }

            default:
                throw new IllegalArgumentException("未対応のVM命令：" + command);
        }
    }

    // --------------------------------------------------
    // push / pop
    // --------------------------------------------------
    public static void writePushPop(CodeWriter writer, Parser.CommandType type,
                                    String fileName, String segment, int index) {
        if (type == Parser.CommandType.C_PUSH) {
            switch (segment) {
                case "constant":
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    break;

                case "local":
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    writer.writeLine("@LCL");
                    writer.writeLine("D=D+M"); // LCL + index
                    writer.writeLine("A=D");
                    writer.writeLine("D=M");
                    break;

                case "argument":
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    writer.writeLine("@ARG");
                    writer.writeLine("D=D+M");
                    writer.writeLine("A=D");
                    writer.writeLine("D=M");
                    break;

                case "this":
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    writer.writeLine("@THIS");
                    writer.writeLine("D=D+M");
                    writer.writeLine("A=D");
                    writer.writeLine("D=M");
                    break;

                case "that":
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    writer.writeLine("@THAT");
                    writer.writeLine("D=D+M");
                    writer.writeLine("A=D");
                    writer.writeLine("D=M");
                    break;

                case "temp":
                    writer.writeLine("@" + (5 + index));
                    writer.writeLine("D=M");
                    break;

                case "pointer":
                    if (index == 0) writer.writeLine("@THIS");
                    else writer.writeLine("@THAT");
                    writer.writeLine("D=M");
                    break;

                case "static":
                    writer.writeLine("@" + fileName + "." + index);
                    writer.writeLine("D=M");
                    break;

                default:
                    throw new IllegalArgumentException("未対応のセグメント：" + segment);
            }

            // 共通: push D
            writer.writeLine("@SP");
            writer.writeLine("A=M");
            writer.writeLine("M=D");
            writer.writeLine("@SP");
            writer.writeLine("M=M+1");

        } else if (type == Parser.CommandType.C_POP) {
            switch (segment) {
                case "local":
                case "argument":
                case "that":
                case "this": {
                    String base = switch (segment) {
                        case "local"    -> "LCL";
                        case "argument" -> "ARG";
                        case "this"     -> "THIS";
                        default         -> "THAT";
                    };
                    // R13 = base + index
                    writer.writeLine("@" + index);
                    writer.writeLine("D=A");
                    writer.writeLine("@" + base);
                    writer.writeLine("D=D+M");
                    writer.writeLine("@R13");
                    writer.writeLine("M=D");
                    // *addr = pop()
                    writer.writeLine("@SP");
                    writer.writeLine("AM=M-1");
                    writer.writeLine("D=M");
                    writer.writeLine("@R13");
                    writer.writeLine("A=M");
                    writer.writeLine("M=D");
                    break;
                }

                case "temp":
                    // 直接番地をR13に入れてから書き込み
                    writer.writeLine("@" + (5 + index));
                    writer.writeLine("D=A");
                    writer.writeLine("@R13");
                    writer.writeLine("M=D");
                    writer.writeLine("@SP");
                    writer.writeLine("AM=M-1");
                    writer.writeLine("D=M");
                    writer.writeLine("@R13");
                    writer.writeLine("A=M");
                    writer.writeLine("M=D");
                    break;

                case "pointer":
                    writer.writeLine("@SP");
                    writer.writeLine("AM=M-1");
                    writer.writeLine("D=M");
                    if (index == 0) writer.writeLine("@THIS");
                    else writer.writeLine("@THAT");
                    writer.writeLine("M=D");
                    break;

                case "static":
                    writer.writeLine("@SP");
                    writer.writeLine("AM=M-1");
                    writer.writeLine("D=M");
                    writer.writeLine("@" + fileName + "." + index);
                    writer.writeLine("M=D");
                    break;

                default:
                    throw new IllegalArgumentException("未対応のセグメント：" + segment);
            }
        }
    }

    // --------------------------------------------------
    // ラベル用ヘルパー（関数スコープ付きラベル）
    // --------------------------------------------------
    private static String scopedLabel(String label) {
        if (currentFunction == null || currentFunction.isEmpty()) {
            // ブートストラップなど関数外ならそのまま使う
            return label;
        }
        return currentFunction + "$" + label;
    }

    // --------------------------------------------------
    // 分岐コマンド（label / goto / if-goto）
    // --------------------------------------------------
    public static void writeLabel(CodeWriter writer, String label) {
        String l = scopedLabel(label);
        writer.writeLine("(" + l + ")");
    }

    public static void writeGoto(CodeWriter writer, String label) {
        String l = scopedLabel(label);
        writer.writeLine("@" + l);
        writer.writeLine("0;JMP");
    }

    public static void writeIf(CodeWriter writer, String label) {
        String l = scopedLabel(label);
        writer.writeLine("@SP");
        writer.writeLine("AM=M-1");
        writer.writeLine("D=M");
        writer.writeLine("@" + l);
        writer.writeLine("D;JNE"); // スタックトップ≠0 ならジャンプ
    }

    // --------------------------------------------------
    // 関数宣言 / 呼び出し / 戻り
    // --------------------------------------------------
    public static void writeFunction(CodeWriter writer, String functionName, int numLocals) {
        // ★ 関数スコープ更新
        currentFunction = functionName;

        writer.writeLine("(" + functionName + ")");
        for (int i = 0; i < numLocals; i++) {
            writer.writeLine("@SP");
            writer.writeLine("A=M");
            writer.writeLine("M=0");
            writer.writeLine("@SP");
            writer.writeLine("M=M+1");
        }
    }

    public static void writeCall(CodeWriter writer, String functionName, int numArgs) {
        String returnLabel = functionName + "$ret." + callCounter++;

        // push returnAddress
        writer.writeLine("@" + returnLabel);
        writer.writeLine("D=A");
        writer.writeLine("@SP");
        writer.writeLine("A=M");
        writer.writeLine("M=D");
        writer.writeLine("@SP");
        writer.writeLine("M=M+1");

        // push LCL, ARG, THIS, THAT
        for (String seg : new String[]{"LCL","ARG","THIS","THAT"}) {
            writer.writeLine("@" + seg);
            writer.writeLine("D=M");
            writer.writeLine("@SP");
            writer.writeLine("A=M");
            writer.writeLine("M=D");
            writer.writeLine("@SP");
            writer.writeLine("M=M+1");
        }

        // ARG = SP - 5 - numArgs
        writer.writeLine("@SP");
        writer.writeLine("D=M");
        writer.writeLine("@5");
        writer.writeLine("D=D-A");
        writer.writeLine("@" + numArgs);
        writer.writeLine("D=D-A");
        writer.writeLine("@ARG");
        writer.writeLine("M=D");

        // LCL = SP
        writer.writeLine("@SP");
        writer.writeLine("D=M");
        writer.writeLine("@LCL");
        writer.writeLine("M=D");

        // goto functionName
        writer.writeLine("@" + functionName);
        writer.writeLine("0;JMP");

        // (returnLabel)
        writer.writeLine("(" + returnLabel + ")");
    }

    public static void writeReturn(CodeWriter writer) {
        // FRAME = LCL
        writer.writeLine("@LCL");
        writer.writeLine("D=M");
        writer.writeLine("@R13");
        writer.writeLine("M=D"); // R13 = FRAME

        // RET = *(FRAME-5)
        writer.writeLine("@5");
        writer.writeLine("A=D-A");
        writer.writeLine("D=M");
        writer.writeLine("@R14");
        writer.writeLine("M=D"); // R14 = RET

        // *ARG = pop()
        writer.writeLine("@SP");
        writer.writeLine("AM=M-1");
        writer.writeLine("D=M");
        writer.writeLine("@ARG");
        writer.writeLine("A=M");
        writer.writeLine("M=D");

        // SP = ARG + 1
        writer.writeLine("@ARG");
        writer.writeLine("D=M+1");
        writer.writeLine("@SP");
        writer.writeLine("M=D");

        // THAT, THIS, ARG, LCL を復元
        for (int i = 1; i <= 4; i++) {
            writer.writeLine("@R13");
            writer.writeLine("D=M");
            writer.writeLine("@" + i);
            writer.writeLine("A=D-A");
            writer.writeLine("D=M");
            String seg = (i == 1) ? "THAT"
                        : (i == 2) ? "THIS"
                        : (i == 3) ? "ARG"
                        : "LCL";
            writer.writeLine("@" + seg);
            writer.writeLine("M=D");
        }

        // goto RET
        writer.writeLine("@R14");
        writer.writeLine("A=M");
        writer.writeLine("0;JMP");
    }
}
