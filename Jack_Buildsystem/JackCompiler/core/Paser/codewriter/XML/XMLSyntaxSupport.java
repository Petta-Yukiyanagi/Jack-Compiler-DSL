package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;
// 新しいファイル: BaseParser.java
public abstract class XMLSyntaxSupport {
    protected Tokenizer tokenizer;

    // ========================================
        // トークンを消費するメソッド
    // ========================================
    void eat(String expected) {
        String actual = tokenizer.token();
        String type = tokenizer.tokenType().toString();

        boolean matched = false;

        switch (expected) {
            case "identifier":
                matched = type.equals("IDENTIFIER");
                break;
            case "integerConstant":
                matched = type.equals("INT_CONST");
                break;
            case "stringConstant":
                matched = type.equals("STRING_CONST");
                break;
            case "keyword":
                matched = type.equals("KEYWORD");
                break;
            case "symbol":
                matched = type.equals("SYMBOL");
                break;
            default:
                matched = actual.equals(expected);
        }
        
        if (matched) {
            tokenizer.advance();
            return;
        }
        
        throw new RuntimeException(
            "構文エラー：" + expected + "を期待したが" + actual + "が来た"
        );
    }
    // ============================================
    // 判定ヘルパー
    // ============================================
    protected boolean tokenIs(String s) {
        return tokenizer.token().equals(s);
    }
}