package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML;


import java.io.IOException;

import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;

public class XMLGenerator extends XMLSyntaxSupport {
    
    private IXMLWriter writer;
    
    // コンストラクタでWriterを注入
    public XMLGenerator(Tokenizer tokenizer, IXMLWriter writer) {
        this.tokenizer = tokenizer;
        this.writer = writer;
        tokenizer.advance();
    }

    public void compileClass() {
        Class();
    }
    
    // ファイル保存
    void saveToFile(String filePath) {
        try {
            writer.saveToFile(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // ========================================
    // 文法規則に従い、構文解析を行うメソッド群
    // ========================================

    // プログラムの構造
    void Class() {
        writer.writeClassStart();
        eat("class");

        String className = tokenizer.token();
        eat("identifier");
        writer.writeIdentifier(className);

        eat("{");

        while (tokenizer.token().equals("static") || tokenizer.token().equals("field")) {
            ClassVarDec();
        }

        while (tokenizer.token().equals("constructor") ||
               tokenizer.token().equals("function") ||
               tokenizer.token().equals("method")) {
            SubroutineDec();
        }
    
        eat("}");
        writer.writeClassEnd();
    }

    void ClassVarDec() {
        writer.writeTagStart("classVarDec");
        
        String kind = tokenizer.token();
        eat(kind);
        writer.writeKeyword(kind);

        Type();
        VarName();

        while (tokenizer.token().equals(",")) {
            eat(",");
            writer.writeSymbol(",");
            VarName();
        }

        eat(";");
        writer.writeSymbol(";");
        writer.writeTagEnd("classVarDec");
    }

    void Type() {
        String token = tokenizer.token();

        if (token.equals("int") || token.equals("char") || token.equals("boolean")) {
            eat(token);
            writer.writeKeyword(token);
        } else {
            String name = tokenizer.token();
            eat("identifier");
            writer.writeIdentifier(name);
        }
    }

    void SubroutineDec() {
        writer.writeTagStart("subroutineDec");

        String keyword = tokenizer.token();
        eat(keyword);
        writer.writeKeyword(keyword);
        
        if (tokenizer.token().equals("void")) {
            eat("void");
            writer.writeKeyword("void");
        } else {
            Type();
        }

        String subroutineName = tokenizer.token();
        eat("identifier");
        writer.writeIdentifier(subroutineName);

        eat("(");
        writer.writeSymbol("(");

        ParameterList();

        eat(")");
        writer.writeSymbol(")");

        SubroutineBody();
        writer.writeTagEnd("subroutineDec");
    }

    void ParameterList() {
        writer.writeTagStart("parameterList");

        if (!tokenizer.token().equals(")")) {
            Type();
            VarName();

            while (tokenizer.token().equals(",")) {
                eat(",");
                writer.writeSymbol(",");
                Type();
                VarName();
            }
        }

        writer.writeTagEnd("parameterList");
    }

    void SubroutineBody() {
        writer.writeTagStart("subroutineBody");
        eat("{");
        writer.writeSymbol("{");

        while (tokenizer.token().equals("var")) {
            VarDec();
        }
        Statements();
        eat("}");
        writer.writeSymbol("}");
        writer.writeTagEnd("subroutineBody");
    }

    void VarDec() {
        writer.writeTagStart("varDec");
        eat("var");
        writer.writeKeyword("var");

        Type();
        VarName();
        while (tokenizer.token().equals(",")) {
            eat(",");
            writer.writeSymbol(",");
            VarName();
        }

        eat(";");
        writer.writeSymbol(";");
        writer.writeTagEnd("varDec");
    }

    void VarName() {
        String name = tokenizer.token();
        eat("identifier");
        writer.writeIdentifier(name);
    }
    
    // 文
    void Statements() {
        writer.writeTagStart("statements");
        while (isStatement(tokenizer.token())) {
            Statement();
        }
        writer.writeTagEnd("statements");
    }

    void Statement() {
        String token = tokenizer.token();
        switch (token) {
            case "let":
                LetStatement();
                break;
            case "if":
                IfStatement();
                break;
            case "while":
                WhileStatement();
                break;
            case "do":
                DoStatement();
                break;
            case "dsl":
                DslStatement();
                break;
            case "return":
                ReturnStatement();
                break;
            default:
                throw new RuntimeException("不明なステートメント: " + token);
        }
    }

void LetStatement() {
    writer.writeTagStart("letStatement");

    eat("let");
    writer.writeKeyword("let");

    // 左辺識別子
    String first = tokenizer.token();
    eat("identifier");
    writer.writeIdentifier(first);

    String second = null;

    // クラス名.静的変数 の場合
    if (tokenizer.token().equals(".")) {
        eat(".");
        writer.writeSymbol(".");
        second = tokenizer.token();
        eat("identifier");
        writer.writeIdentifier(second);
    }

    // 配列または Class.var 配列
    if (tokenizer.token().equals("[")) {
        eat("[");
        writer.writeSymbol("[");
        Expression();
        eat("]");
        writer.writeSymbol("]");
    }

    // '='
    eat("=");
    writer.writeSymbol("=");

    // 右辺
    Expression();

    eat(";");
    writer.writeSymbol(";");

    writer.writeTagEnd("letStatement");
}


    void IfStatement() {
        writer.writeTagStart("ifStatement");
        eat("if");
        writer.writeKeyword("if");
        eat("(");
        writer.writeSymbol("(");
        Expression();
        eat(")");
        writer.writeSymbol(")");

        eat("{");
        writer.writeSymbol("{");
        Statements();
        eat("}");
        writer.writeSymbol("}");

        if (tokenizer.token().equals("else")) {
            eat("else");
            writer.writeKeyword("else");
            eat("{");
            writer.writeSymbol("{");
            Statements();
            eat("}");
            writer.writeSymbol("}");
        }

        writer.writeTagEnd("ifStatement");
    }

    void WhileStatement() {
        writer.writeTagStart("whileStatement");

        eat("while");
        writer.writeKeyword("while");

        eat("(");
        writer.writeSymbol("(");
        Expression();
        eat(")");
        writer.writeSymbol(")");

        eat("{");
        writer.writeSymbol("{");
        Statements();
        eat("}");
        writer.writeSymbol("}");

        writer.writeTagEnd("whileStatement");
    }

    void DoStatement() {
        writer.writeTagStart("doStatement");

        eat("do");
        writer.writeKeyword("do");

        SubroutineCall();

        eat(";");
        writer.writeSymbol(";");
        writer.writeTagEnd("doStatement");
    }
    void DslStatement() {
        writer.writeTagStart("dslStatement");

        eat("dsl");
        writer.writeKeyword("dsl");

        // DSL固有の構文解析をここに実装

        eat(";");
        writer.writeSymbol(";");
        writer.writeTagEnd("dslStatement");
    }

    void ReturnStatement() {
        writer.writeTagStart("returnStatement");

        eat("return");
        writer.writeKeyword("return");

        if (!tokenizer.token().equals(";")) {
            Expression();
        }

        eat(";");
        writer.writeSymbol(";");

        writer.writeTagEnd("returnStatement");
    }

    boolean isStatement(String token) {
        return token.equals("let") ||
               token.equals("if") ||
               token.equals("while") ||
               token.equals("do") ||
                token.equals("dsl") ||
               token.equals("return");
    }

    // 式
    void Expression() {
        writer.writeTagStart("expression");

        Term();

        while (isOperator(tokenizer.token())) {
            String op = tokenizer.token();
            eat(op);
            writer.writeSymbol(op);
            Term();
        }

        writer.writeTagEnd("expression");
    }

    boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") ||
                token.equals("*") || token.equals("/") ||
                token.equals("&") || token.equals("|") ||
                token.equals("<") || token.equals(">") ||
                token.equals("=") ||
                token.equals("==") || token.equals("!=") ||
                token.equals("<=") || token.equals(">=") ||
                token.equals("&&") || token.equals("||");

    }

    void Term() {
        writer.writeTagStart("term");
        String token = tokenizer.token();
        String type = tokenizer.tokenType().toString();

        if (type.equals("INT_CONST")) {
            eat(token);
            writer.writeIntConstant(token);
        }
        else if (type.equals("STRING_CONST")) {
            eat(token);
            writer.writeStringConstant(token);
        }
        else if (type.equals("KEYWORD") &&
                 (token.equals("true") || token.equals("false") ||
                  token.equals("null") || token.equals("this"))) {
            eat(token);
            writer.writeKeyword(token);
        }
        else if (token.equals("(")) {
            eat("(");
            writer.writeSymbol("(");
            Expression();
            eat(")");
            writer.writeSymbol(")");
        }
        else if (token.equals("-") || token.equals("~")) {
            eat(token);
            writer.writeSymbol(token);
            Term();
        }
        else if (type.equals("IDENTIFIER")) {
            String name = token;
            eat("identifier");
            writer.writeIdentifier(name);

            if (tokenizer.token().equals("[")) {
                eat("[");
                writer.writeSymbol("[");
                Expression();
                eat("]");
                writer.writeSymbol("]");
            } else if (tokenizer.token().equals("(") || tokenizer.token().equals(".")) {
                SubroutineCall(name);
            }
        }
        else {
            throw new RuntimeException("不明な term: " + token);
        }

        writer.writeTagEnd("term");
    }

void SubroutineCall() {
    String first = tokenizer.token();
    eat("identifier");
    SubroutineCall(first);
}

void SubroutineCall(String firstName) {
    // まず firstName を書く
    writer.writeIdentifier(firstName);

    // A.method の形式か？
    if (tokenizer.token().equals(".")) {
        eat(".");
        writer.writeSymbol(".");
        String second = tokenizer.token();
        eat("identifier");
        writer.writeIdentifier(second);
    }

    eat("(");
    writer.writeSymbol("(");
    ExpressionList();
    eat(")");
    writer.writeSymbol(")");
}

    void ExpressionList() {
        writer.writeTagStart("expressionList");

        if (!tokenizer.token().equals(")")) {
            Expression();

            while (tokenizer.token().equals(",")) {
                eat(",");
                writer.writeSymbol(",");
                Expression();
            }
        }

        writer.writeTagEnd("expressionList");
    }
}
