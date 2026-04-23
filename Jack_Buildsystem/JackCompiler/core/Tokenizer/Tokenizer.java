package Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer;

import java.util.List;
import java.util.Set;

public class Tokenizer {
    private final String input;
    private int pos;
    private String currentToken;
    private TokenType tokenType;

    // Jack の 19 個の Keyword
    private static final Set<String> KEYWORDS = Set.of(
        "class","constructor","function","method",
        "field","static","var",
        "int","char","boolean","void",
        "true","false","null","this",
        "let","do","dsl","if","else","while","return"
    );

    private static final List<String> MULTI_SYMBOLS = List.of(
    "==", "!=", "<=", ">=", "&&", "||"
    );

    // Jack の Symbol 一覧
    private static final String SYMBOLS = "{}()[].,;+-*/&|<>=~!";

    public Tokenizer(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        this.input = sb.toString();
        this.pos = 0;
    }

    // トークンが残っているか
    public boolean hasMoreTokens() {
        skipWhitespaceAndComments();
        return pos < input.length();
    }

    // 次のトークンへ進める
    public void advance() {
        skipWhitespaceAndComments();
        if (pos >= input.length()) return;

        char c = input.charAt(pos);

        // 整数
        if (Character.isDigit(c)) {
            readNumber();
        }
        // 文字列リテラル
        else if (c == '"') {
            readString();
        }
        // 識別子 or キーワード
        else if (isIdentifierStart(c)) {
            readIdentifierOrKeyword();
        }
        // 記号
        else if (isSymbol(c)) {

            // ★ 複数文字記号（最大2文字）を優先的にチェック
            for (String sym : MULTI_SYMBOLS) {
                int len = sym.length();
                if (pos + len <= input.length() &&
                    input.substring(pos, pos + len).equals(sym)) {
                    currentToken = sym;
                    tokenType = TokenType.SYMBOL;
                    pos += len;
                    return;
                }
            }

            // ★ 単一文字記号
            currentToken = String.valueOf(c);
            tokenType = TokenType.SYMBOL;
            pos++;
        }
        else {
            throw new RuntimeException("Unexpected character: '" + c + "'");
        }
    }

    // 現在のトークン
    public String token() {
        return currentToken;
    }

    public TokenType tokenType() {
        return tokenType;
    }

    // ======= 内部処理 =======

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private boolean isSymbol(char c) {
        return SYMBOLS.indexOf(c) != -1;
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = input.charAt(pos);

            // ホワイトスペース
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // コメント //
            if (c == '/' && pos + 1 < input.length() && input.charAt(pos + 1) == '/') {
                pos += 2;
                while (pos < input.length() && input.charAt(pos) != '\n') pos++;
                continue;
            }

            // コメント /* */
            if (c == '/' && pos + 1 < input.length() && input.charAt(pos + 1) == '*') {
                pos += 2;
                while (pos + 1 < input.length() &&
                       !(input.charAt(pos) == '*' && input.charAt(pos + 1) == '/')) {
                    pos++;
                }
                pos += 2; // */ をスキップ
                continue;
            }

            break;
        }
    }

    private void readNumber() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        currentToken = input.substring(start, pos);
        tokenType = TokenType.INT_CONST;
    }

    private void readString() {
        pos++; // 最初の " をスキップ
        int start = pos;
        while (pos < input.length() && input.charAt(pos) != '"') pos++;
        currentToken = input.substring(start, pos);
        tokenType = TokenType.STRING_CONST;
        pos++; // 終了の " をスキップ
    }

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < input.length() && isIdentifierPart(input.charAt(pos))) pos++;
        currentToken = input.substring(start, pos);

        if (KEYWORDS.contains(currentToken)) {
            tokenType = TokenType.KEYWORD;
        } else {
            tokenType = TokenType.IDENTIFIER;
        }
    }
}



