// SyntaxSupport.java
package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.TokenType;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;

public abstract class VMSyntaxSupport {
    
    // 子クラスからもアクセスできるように protected にします
    protected final Tokenizer tokenizer;

    // コンストラクタで Tokenizer を受け取る
    public VMSyntaxSupport(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    // ============================================
    // ここから下に、CodeGenerator にあった以下のメソッドを
    // そっくりそのまま移動（カット＆ペースト）させます。
    // ※ private だったものは protected に変更してください。
    // ============================================

   protected void eatIdentifier() {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            error("identifier expected but '" + tokenizer.token() + "'");
        }
        tokenizer.advance();
    }

    protected void eatType() {
        // int, char, boolean, クラス名
        String tok = tokenizer.token();
        TokenType tt = tokenizer.tokenType();

        if (tok.equals("int") || tok.equals("char") || tok.equals("boolean")) {
            tokenizer.advance();
        } else if (tt == TokenType.IDENTIFIER) {
            tokenizer.advance();
        } else {
            error("type expected but '" + tok + "'");
        }
    }

    protected void eat(String expected) {
        String tok = tokenizer.token();
        if (!tok.equals(expected)) {
            error("expected '" + expected + "' but '" + tok + "'");
        }
        tokenizer.advance();
    }

    // ============================================
    // 判定ヘルパー
    // ============================================

    protected boolean tokenIs(String s) {
        return tokenizer.token().equals(s);
    }

    protected boolean isOp(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/") ||
                s.equals("&") || s.equals("|") ||
                s.equals("<") || s.equals(">") || s.equals("=") ||
                s.equals("==") || s.equals("!=") ||
                s.equals("<=") || s.equals(">=") ||
                s.equals("&&") || s.equals("||");
    }

    protected void error(String msg) {
        throw new RuntimeException("[CodeGenerator Error] " + msg);
    }
}