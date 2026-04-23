package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML;
import java.io.IOException;

/**
 * コンパイラの出力を抽象化するインターフェース
 */
public interface IXMLWriter {
    
    // クラス定義
    void writeClassStart();
    void writeClassEnd();
    
    // キーワード
    void writeKeyword(String keyword);
    
    // 識別子
    void writeIdentifier(String name);
    
    // シンボル
    void writeSymbol(String symbol);
    
    // 定数
    void writeIntConstant(String value);
    void writeStringConstant(String value);
    
    // 構造タグ
    void writeTagStart(String tag);
    void writeTagEnd(String tag);
    
    // ファイル保存
    void saveToFile(String filePath) throws IOException;
    
    // 出力内容の取得
    String getOutput();
}