package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM;

import java.io.Closeable;

/**
 * VMコード出力を抽象化するインターフェース
 */
public interface IVMWriter extends Closeable {
    
    void setCurrentFunction(String funcName);

    // 基本的なVM命令
    void writePush(String segment, int index);
    void writePop(String segment, int index);
    void writeArithmetic(String op);

    // ラベル・制御フロー
    void writeLabel(String label);
    void writeGoto(String label);
    void writeIf(String label);

    // ラベル生成
    String generateIfTrueLabel();
    String generateIfFalseLabel();
    String generateIfEndLabel();
    String generateWhileExpLabel();
    String generateWhileEndLabel();

    // 関数呼び出し・定義
    void writeCall(String name, int nArgs);
    void writeFunction(String name, int nLocals);
    void writeReturn();

    // 文字列・特殊操作
    void writeStringConstant(String str);
    void safePush(String segment, int index);
    void safePop(String segment, int index);

    // デバッグ用
    void writeComment(String comment);

    // クローズ処理
    void close();
}