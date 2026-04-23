package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM;

import Jack_Project.Jack_Buildsystem.JackCompiler.core.DSL.DSLGenerator;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.SymbolTable.SymbolTable;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.TokenType;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;

public class VMGenerator extends DSLGenerator {
    private String className;

    private SymbolTable classSymbols; // static / field
    private SymbolTable subSymbols; // arg / var

    public VMGenerator(Tokenizer tokenizer, VMWriter vmWriter) {
        super(tokenizer, vmWriter);
        this.classSymbols = new SymbolTable();

        tokenizer.advance(); // 最初のトークンへ
    }

    public void close() {
        Writer.close();
    }

    // ============================================
    // class のコンパイル
    // ============================================
    public void compileClass() {
        Writer.writeComment("=== Class Compilation Start ===");

        eat("class");

        className = tokenizer.token();
        eatIdentifier();

        Writer.writeComment("Class: " + className);

        eat("{");

        // static / field
        while (tokenIs("static") || tokenIs("field")) {
            compileClassVarDec();
        }

        // constructor / method / function
        while (tokenIs("constructor") || tokenIs("function") || tokenIs("method")) {
            compileSubroutine();
        }

        eat("}");

        Writer.writeComment("=== Class Compilation End ===");
    }

    // ============================================
    // class 変数宣言
    // ============================================
    private void compileClassVarDec() {
        String kindStr = tokenizer.token(); // static or field
        eat(kindStr);

        SymbolTable.Kind kind = kindStr.equals("static") ? SymbolTable.Kind.STATIC : SymbolTable.Kind.FIELD;

        String type = tokenizer.token();
        eatType();

        String name = tokenizer.token();
        eatIdentifier();

        classSymbols.define(name, type, kind);
        Writer.writeComment("ClassVar: " + kindStr + " " + type + " " + name);

        while (tokenIs(",")) {
            eat(",");
            name = tokenizer.token();
            eatIdentifier();

            classSymbols.define(name, type, kind);
            Writer.writeComment("ClassVar: " + kindStr + " " + type + " " + name);
        }

        eat(";");
    }

    // ============================================
    // subroutine のコンパイル
    // ============================================
    private void compileSubroutine() {
        // サブルーチンのシンボルテーブルをリセット
        subSymbols = new SymbolTable();

        String subKind = tokenizer.token(); // constructor/function/method
        eat(subKind);

        // 戻り値の型
        if (tokenIs("void")) {
            eat("void");
        } else {
            eatType();
        }

        String subName = tokenizer.token();
        eatIdentifier();

        String fullName = className + "." + subName;

        eat("(");

        // method のとき argument 0 = this
        if (subKind.equals("method")) {
            subSymbols.define("this", className, SymbolTable.Kind.ARG);
            Writer.writeComment("Method implicit argument: this");
        }

        compileParameterList();
        eat(")");

        eat("{");

        int nLocals = compileVarDec();

        Writer.writeFunction(fullName, nLocals);
        Writer.writeComment("Function: " + fullName);

        // constructor の 初期化
        if (subKind.equals("constructor")) {
            int nFields = classSymbols.varCount(SymbolTable.Kind.FIELD);
            Writer.writeComment("Constructor initialization");
            Writer.writePush("constant", nFields);
            Writer.writeCall("Memory.alloc", 1);
            Writer.writePop("pointer", 0);
        }

        // method の this セット
        if (subKind.equals("method")) {
            Writer.writeComment("Method: set this pointer");
            Writer.writePush("argument", 0);
            Writer.writePop("pointer", 0);
        }

        compileStatements();

        eat("}");
    }

    // ============================================
    // parameterList
    // ============================================
    private void compileParameterList() {
        if (tokenIs(")"))
            return;

        String type = tokenizer.token();
        eatType();

        String name = tokenizer.token();
        eatIdentifier();

        subSymbols.define(name, type, SymbolTable.Kind.ARG);
        Writer.writeComment("Parameter: " + type + " " + name);

        while (tokenIs(",")) {
            eat(",");
            type = tokenizer.token();
            eatType();
            name = tokenizer.token();
            eatIdentifier();
            subSymbols.define(name, type, SymbolTable.Kind.ARG);
            Writer.writeComment("Parameter: " + type + " " + name);
        }
    }

    // ============================================
    // var 宣言
    // ============================================
    private int compileVarDec() {
        int count = 0;

        while (tokenIs("var")) {
            eat("var");
            String type = tokenizer.token();
            eatType();

            String name = tokenizer.token();
            eatIdentifier();
            subSymbols.define(name, type, SymbolTable.Kind.VAR);
            count++;

            Writer.writeComment("Var: " + type + " " + name);

            while (tokenIs(",")) {
                eat(",");
                name = tokenizer.token();
                eatIdentifier();
                subSymbols.define(name, type, SymbolTable.Kind.VAR);
                Writer.writeComment("Var: " + type + " " + name);
                count++;
            }

            eat(";");
        }

        return count;
    }

    // ============================================
    // statements
    // ============================================
    private void compileStatements() {
        while (true) {
            String t = tokenizer.token();

            if (t.equals("let")) {
                compileLet();
            } else if (t.equals("if")) {
                compileIf();
            } else if (t.equals("while")) {
                compileWhile();
            } else if (t.equals("do")) {
                compileDo();
            } else if (t.equals("dsl")) {
                compileDsl();
            } else if (t.equals("return")) {
                compileReturn();
            } else {
                break;
            }
        }
    }

    // ============================================
    // let 文
    // ============================================
    private void compileLet() {
        Writer.writeComment("let statement");

        eat("let");

        String base = tokenizer.token();
        eatIdentifier();

        boolean isIndexed = false;

        // a[expr] の場合
        if (tokenIs("[")) {
            isIndexed = true;
            eat("[");
            compileExpression();
            eat("]");

            Writer.writePop("temp", 1); // index を temp 1 に保存
        }

        eat("=");

        compileExpression();
        Writer.writePop("temp", 0); // value を temp 0 に保存
        eat(";");

        // -------------------------
        // 配列代入
        // -------------------------
        if (isIndexed) {
            Writer.writePush("temp", 1); // index
            pushVar(base); // base address
            Writer.writeArithmetic("+");
            Writer.writePop("pointer", 1); // THAT ポインタ設定
            Writer.writePush("temp", 0); // value
            Writer.writePop("that", 0); // that[0] = value

            return;
        }

        // -------------------------
        // 通常変数
        // -------------------------
        Writer.writePush("temp", 0); // value
        popVar(base);
    }

    // ============================================
    // if 文
    // ============================================
    private void compileIf() {
        Writer.writeComment("if statement");

        eat("if");
        eat("(");
        compileExpression();
        eat(")");

        String falseLabel = Writer.generateIfFalseLabel();
        String endLabel = Writer.generateIfEndLabel();

        // 条件反転: if-goto false
        Writer.writeArithmetic("~");
        Writer.writeIf(falseLabel);

        eat("{");
        compileStatements();
        eat("}");

        Writer.writeGoto(endLabel);
        Writer.writeLabel(falseLabel);

        // else 部
        if (tokenIs("else")) {
            eat("else");
            eat("{");
            compileStatements();
            eat("}");
        }

        Writer.writeLabel(endLabel);
    }

    // ============================================
    // while 文
    // ============================================
    private void compileWhile() {
        Writer.writeComment("while statement");

        eat("while");

        String expLabel = Writer.generateWhileExpLabel();
        String endLabel = Writer.generateWhileEndLabel();

        Writer.writeLabel(expLabel);

        eat("(");
        compileExpression();
        eat(")");

        // 条件が false なら終了
        // expression の結果が true(-1) なら continue, false(0) なら end
        Writer.writeArithmetic("~"); // NOT
        Writer.writeIf(endLabel);

        eat("{");
        compileStatements();
        eat("}");

        Writer.writeGoto(expLabel);
        Writer.writeLabel(endLabel);
    }

    // ============================================
    // do 文
    // ============================================
    private void compileDo() {
        Writer.writeComment("do statement");

        eat("do");

        compileSubroutineCall();

        eat(";");

        // 戻り値を破棄
        Writer.writePop("temp", 0);
    }



    // 既存の設計に合わせてセグメントを決めるヘルパー
    @Override
    protected void pushVar(SymbolTable.Kind kind, int index) {
        String segment;
        switch (kind) {
            case ARG:
                segment = "argument";
                break;
            case VAR:
                segment = "local";
                break;
            case FIELD:
                segment = "this";
                break;
            case STATIC:
                segment = "static";
                break;
            default:
                throw new RuntimeException("Unknown kind: " + kind);
        }
        Writer.writePush(segment, index);
    }

    // ============================================
    // return 文
    // ============================================
    private void compileReturn() {
        Writer.writeComment("return statement");

        eat("return");

        // 戻り値あり
        if (!tokenIs(";")) {
            compileExpression();
        } else {
            // void: 0 push
            Writer.writePush("constant", 0);
        }

        eat(";");

        Writer.writeReturn();
    }
    // ============================================
    // expression
    // term (整数・文字列・キーワード・変数・配列・呼び出し)
    // subroutineCall
    // expressionList
    // ============================================

    protected void compileExpression() {
        compileTerm();

        while (isOp(tokenizer.token())) {
            String op = tokenizer.token();
            eat(op);

            compileTerm();

            // --- 拡張比較演算子に対応 ---
            switch (op) {
                case "==":
                    Writer.writeArithmetic("eq");
                    break;
                case "!=":
                    Writer.writeArithmetic("eq");
                    Writer.writeArithmetic("not");
                    break;
                case "<=":
                    Writer.writeArithmetic("gt");
                    Writer.writeArithmetic("not");
                    break;
                case ">=":
                    Writer.writeArithmetic("lt");
                    Writer.writeArithmetic("not");
                    break;
                case "<":
                    Writer.writeArithmetic("lt");
                    break;
                case ">":
                    Writer.writeArithmetic("gt");
                    break;
                case "&&":
                    Writer.writeArithmetic("and");
                    break;
                case "||":
                    Writer.writeArithmetic("or");
                    break;
                default:
                    // 既存（+ - * / など）
                    Writer.writeArithmetic(op);
            }
        }
    }

    protected void compileTerm() {
        String t = tokenizer.token();
        TokenType tt = tokenizer.tokenType();

        // -----------------------------------------
        // INT_CONST
        // -----------------------------------------
        if (tt == TokenType.INT_CONST) {
            int v = Integer.parseInt(t);

            // 負の数はサポートしない（unary minus で処理される）
            if (v < 0 || v > 32767) {
                error("Integer constant out of range: " + v);
            }

            Writer.writePush("constant", v);
            eat(t);
            return;
        }

        // -----------------------------------------
        // STRING_CONST
        // -----------------------------------------
        if (tt == TokenType.STRING_CONST) {
            Writer.writeStringConstant(t);
            eat(t);
            return;
        }

        // -----------------------------------------
        // keyword const
        // true, false, null, this
        // -----------------------------------------
        if (tt == TokenType.KEYWORD) {
            switch (t) {
                case "true":
                    Writer.writePush("constant", 0);
                    Writer.writeArithmetic("~"); // NOT 0 = -1
                    eat("true");
                    return;

                case "false":
                case "null":
                    Writer.writePush("constant", 0);
                    eat(t);
                    return;

                case "this":
                    Writer.writePush("pointer", 0);
                    eat("this");
                    return;
            }
        }

        // -----------------------------------------
        // "(" expression ")"
        // -----------------------------------------
        if (t.equals("(")) {
            eat("(");
            compileExpression();
            eat(")");
            return;
        }

        // -----------------------------------------
        // unaryOp term
        // -----------------------------------------
        if (t.equals("-") || t.equals("~")) {
            String op = t;
            eat(op);
            compileTerm();
            Writer.writeArithmetic(op.equals("-") ? "neg" : "~");
            return;
        }

        // -----------------------------------------
        // IDENTIFIER: var / array / subroutineCall / ClassName
        // -----------------------------------------
        if (tt == TokenType.IDENTIFIER) {
            String name = t;
            // 先にシンボルテーブルを見ておく
            SymbolTable.Kind kind = getKind(name);

            eatIdentifier();

            // ① subroutineCall (obj.method(...) / Class.func(...))
            if (tokenIs("(") || tokenIs(".")) {
                compileSubroutineCall(name);
                return;
            }

            // ② 配列アクセス: a[expr]
            if (tokenIs("[")) {
                if (kind == SymbolTable.Kind.NONE) {
                    error("Undefined array variable: " + name);
                }

                eat("[");
                pushVar(name);
                compileExpression();
                eat("]");

                // base + index → THAT[0]
                Writer.writeArithmetic("+");
                Writer.writePop("pointer", 1);
                Writer.writePush("that", 0);
                return;
            }

            // ③ 通常変数
            if (kind != SymbolTable.Kind.NONE) {
                pushVar(name);
                return;
            }

            // ④ ここまで来てシンボルに無い & 先頭大文字 → Class 名として扱う
            if (Character.isUpperCase(name.charAt(0))) {
                Writer.writeComment("Class identifier used as term: " + name);
                return;
            }

            // ⑤ それ以外は未定義識別子
            error("Undefined identifier in term: " + name);
        }

        error("term が不正: " + t);
    }

    // ============================================
    // subroutineCall
    // ============================================

    protected void compileSubroutineCall() {
        String name = tokenizer.token();
        eatIdentifier();
        compileSubroutineCall(name);
    }

    protected void compileSubroutineCall(String firstName) {
        String functionName;
        int nArgs = 0;

        // className.subroutine
        if (tokenIs(".")) {
            eat(".");
            String second = tokenizer.token();
            eatIdentifier();

            // firstName が変数か？
            SymbolTable.Kind kind = getKind(firstName);

            if (kind != SymbolTable.Kind.NONE) {
                // 変数 → メソッド呼び出し
                pushVar(firstName);
                String typ = getType(firstName);
                functionName = typ + "." + second;
                nArgs = 1;
            } else {
                // クラス名
                functionName = firstName + "." + second;
            }
        }
        // subroutine(...) → this.method(...)
        else {
            Writer.writePush("pointer", 0);
            functionName = className + "." + firstName;
            nArgs = 1;
        }

        eat("(");
        nArgs += compileExpressionList();
        eat(")");

        Writer.writeCall(functionName, nArgs);
    }

    // ============================================
    // expressionList
    // ============================================

    protected int compileExpressionList() {
        int count = 0;

        if (!tokenIs(")")) {
            compileExpression();
            count++;

            while (tokenIs(",")) {
                eat(",");
                compileExpression();
                count++;
            }
        }
        return count;
    }
    // ============================================
    // 変数アクセス（var / arg / static / field）
    // ============================================

    @Override
    protected void pushVar(String name) {
        SymbolTable.Kind kind = getKind(name);
        int index = getIndex(name);

        switch (kind) {
            case STATIC:
                Writer.writePush("static", index);
                break;

            case FIELD:
                Writer.writePush("this", index);
                break;

            case ARG:
                Writer.writePush("argument", index);
                break;

            case VAR:
                Writer.writePush("local", index);
                break;

            case NONE:
            default:
                error("Undefined variable: " + name);
        }
    }

    @Override
    protected void popVar(String name) {
        SymbolTable.Kind kind = getKind(name);
        int index = getIndex(name);

        switch (kind) {
            case STATIC:
                Writer.writePop("static", index);
                break;

            case FIELD:
                Writer.writePop("this", index);
                break;

            case ARG:
                Writer.writePop("argument", index);
                break;

            case VAR:
                Writer.writePop("local", index);
                break;

            case NONE:
            default:
                error("Undefined variable: " + name);
        }
    }

    @Override
    protected SymbolTable.Kind getKind(String name) {
        SymbolTable.Kind k = subSymbols.kindOf(name);
        if (k != SymbolTable.Kind.NONE)
            return k;
        return classSymbols.kindOf(name);
    }

    private String segmentOf(SymbolTable.Kind kind) {
        switch (kind) {
            case STATIC:
                return "static";
            case FIELD:
                return "this";
            case ARG:
                return "argument";
            case VAR:
                return "local";
            case NONE:
            default:
                error("Unknown kind: " + kind);
                return null;
        }
    }

    @Override
    protected int getIndex(String name) {
        int idx = subSymbols.indexOf(name);
        if (idx != -1)
            return idx;

        idx = classSymbols.indexOf(name);
        if (idx != -1)
            return idx;

        error("Variable not found: " + name);
        return -1;
    }

    private String getType(String name) {
        String t = subSymbols.typeOf(name);
        if (t != null)
            return t;

        t = classSymbols.typeOf(name);
        if (t != null)
            return t;

        error("Type not found for variable: " + name);
        return "Object";
    }

}
