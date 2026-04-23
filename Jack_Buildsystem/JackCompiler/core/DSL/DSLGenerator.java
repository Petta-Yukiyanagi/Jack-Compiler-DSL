package Jack_Project.Jack_Buildsystem.JackCompiler.core.DSL;

import java.util.HashMap;
import java.util.Map;

import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.SymbolTable.SymbolTable;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM.IVMWriter;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM.VMSyntaxSupport;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;

public abstract class DSLGenerator extends VMSyntaxSupport {
    protected static class MatrixInfo {
        final int rows;
        final int cols;

        MatrixInfo(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
        }
    }

    protected final IVMWriter Writer;
    protected final Map<String, MatrixInfo> matrixInfo = new HashMap<>();

    protected DSLGenerator(Tokenizer tokenizer, IVMWriter writer) {
        super(tokenizer);
        this.Writer = writer;
    }

    protected abstract void pushVar(String name);
    protected abstract void popVar(String name);
    protected abstract void pushVar(SymbolTable.Kind kind, int index);
    protected abstract SymbolTable.Kind getKind(String name);
    protected abstract int getIndex(String name);
    protected abstract void compileExpression();
    
    // ============================================
    // dsl 文
    // ============================================
    protected void compileDsl() {
        Writer.writeComment("dsl statement");

        eat("dsl");

        String cmd = tokenizer.token();
        eatIdentifier();

        if (cmd.equals("matrix")) {
            compileDslMatrix();
        } else if (cmd.equals("mul")) {
            compileDslMul();
        } else if (cmd.equals("set")) {
            compileDslSet();
        } else {
            error("Unknown dsl command: " + cmd);
        }

        eat(";");

    }

    // ============================================
    // dsl matrix
    // ============================================
    // dsl matrix A(16, 16);
    protected void compileDslMatrix() {

        String name = tokenizer.token();
        eatIdentifier();

        eat("(");
        int rows = Integer.parseInt(tokenizer.token());
        eat(tokenizer.token());
        eat(",");
        int cols = Integer.parseInt(tokenizer.token());
        eat(tokenizer.token());
        eat(")");

        matrixInfo.put(name, new MatrixInfo(rows, cols));
        int size = rows * cols;

        Writer.writeComment("dsl matrix " + name + "(" + rows + "," + cols + ")");

        // Array.new(size)
        Writer.writePush("constant", size);
        Writer.writeCall("Array.new", 1);

        popVar(name);
    }

    // ============================================
    // DslMul 文
    // ============================================
    // dsl mul C, A, B;
    protected void compileDslMul() {
        Writer.writeComment("dsl mul");

        // C
        String cName = tokenizer.token();
        eatIdentifier();

        eat(",");

        // A
        String aName = tokenizer.token();
        eatIdentifier();

        eat(",");

        // B
        String bName = tokenizer.token();
        eatIdentifier();

        // サイズ情報をコンパイル時に取得
        MatrixInfo miA = matrixInfo.get(aName);
        MatrixInfo miB = matrixInfo.get(bName);
        if (miA == null || miB == null) {
            error("matrix not declared by 'dsl matrix': " + aName + " or " + bName);
        }

        int m = miA.rows;
        int n = miA.cols;
        int p = miB.cols;

        // サイズ可変＋アンローリングをやる場所
        if (m == 4 && n == 4 && p == 4) {

            // 4×4 完全アンローリング版
            emitUnrolledMulKernel4(cName, aName, bName, m, n, p);

        } else if (m == 8 && n == 8 && p == 8) {

            // 8×8 アンローリング版
            emitUnrolledMulKernel8(cName, aName, bName, m, n, p);

        } else if (m == 16 && n == 16 && p == 16) {

            // 16×16 アンローリング版
            emitUnrolledMulKernel16(cName, aName, bName, m, n, p);

        } else {

            // フォールバック：generic ループ版
            emitGenericMulCall(cName, aName, bName, m, n, p);

        }

    }

    // ------------------------------------------------------------
    // UNROLLED MUL KERNEL
    // C[m×p] = A[m×n] * B[n×p]
    // A[i][k] = A_base + (i * n + k)
    // B[k][j] = B_base + (k * p + j)
    // C[i][j] = C_base + (i * p + j)
    // i,j,k は「コンパイラ側(Java)のループ」で完全アンローリング
    // ------------------------------------------------------------
    // C = A(4x4) * B(4x4) 専用の完全アンローリング版
    protected void emitUnrolledMulKernel4(
            String cName,
            String aName,
            String bName,
            int m, int n, int p) {

        // 4x4 専用にしておく（他サイズで呼ばれたら即バグに気付けるように）
        if (m != 4 || n != 4 || p != 4) {
            throw new IllegalArgumentException("emitUnrolledMulKernel: 4x4 only");
        }

        Writer.writeComment("=== BEGIN UNROLLED MUL 4x4 (SAFE VERSION) ===");

        // ------------------------------------------------
        // base addresses:
        // temp 4 = A_base
        // temp 5 = B_base
        // temp 6 = C_base
        // ------------------------------------------------
        {
            pushVar(aName);
            Writer.writePop("temp", 4);
        }
        {
            pushVar(bName);
            Writer.writePop("temp", 5);
        }
        {
            pushVar(cName);
            Writer.writePop("temp", 6);
        }

        int debugBase = 30026; // C のデバッグミラー開始位置

        // ------------------------------------------------
        // ここから下の for は「VM コード生成用のループ」なので
        // 生成される VM コード自体には while/if などのループは一切出ません。
        // 完全に 16 要素ぶんの直列コードになります。
        // ------------------------------------------------
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {

                Writer.writeComment("C[" + i + "][" + j + "] compute");

                // sum = 0 → temp3
                Writer.writePush("constant", 0);
                Writer.writePop("temp", 3);

                // k 方向に 4 回分、完全展開
                for (int k = 0; k < 4; k++) {
                    int aOffset = i * 4 + k;
                    int bOffset = k * 4 + j;

                    Writer.writeComment("  k = " + k);

                    // ---------- A[i][k] を読む ----------
                    Writer.writePush("temp", 4); // A_base
                    Writer.writePush("constant", aOffset); // offset
                    Writer.writeArithmetic("+"); // A_base + offset
                    Writer.writePop("pointer", 1); // THAT = addr
                    Writer.writePush("that", 0); // stack: A[i][k]

                    // ---------- B[k][j] を読む ----------
                    Writer.writePush("temp", 5); // B_base
                    Writer.writePush("constant", bOffset); // offset
                    Writer.writeArithmetic("+"); // B_base + offset
                    Writer.writePop("pointer", 1); // THAT = addr
                    Writer.writePush("that", 0); // stack: A[i][k], B[k][j]

                    // prod = A[i][k] * B[k][j]
                    Writer.writeCall("Math.multiply", 2); // stack: prod

                    // sum を読み出す
                    Writer.writePush("temp", 3); // stack: prod, sum

                    // sum += prod
                    Writer.writeArithmetic("+"); // newSum
                    Writer.writePop("temp", 3); // temp3 = newSum
                }

                // ---------- C[i][j] に書き込む ----------
                int cOffset = i * 4 + j;

                // addr = C_base + cOffset
                Writer.writePush("temp", 6); // C_base
                Writer.writePush("constant", cOffset);
                Writer.writeArithmetic("+");
                Writer.writePop("pointer", 1); // THAT = addr

                // *addr = sum
                Writer.writePush("temp", 3);
                Writer.writePop("that", 0);

                // ---------- デバッグミラー ----------
                Writer.writeComment("DEBUG: store C[" + i + "][" + j + "]");

                Writer.writePush("constant", debugBase + cOffset);
                Writer.writePop("pointer", 1);
                Writer.writePush("temp", 3);
                Writer.writePop("that", 0);
            }
        }

        Writer.writeComment("=== END UNROLLED MUL 4x4 (SAFE VERSION) ===");
    }

    // ============================================
    // 8x8 アンローリング版
    // ============================================

    protected void emitUnrolledMulKernel8(
            String cName,
            String aName,
            String bName,
            int m, int n, int p) {

        if (m != 8 || n != 8 || p != 8) {
            throw new IllegalArgumentException("emitUnrolledMulKernel8: 8x8 only");
        }

        Writer.writeComment("=== BEGIN UNROLLED MUL 8x8 (TEMP-SAFE) ===");
        Writer.writeComment("TEMP MAP:");
        Writer.writeComment("  temp0: SCRATCH ONLY (may be clobbered by Math.*)");
        Writer.writeComment("  temp1: rowA");
        Writer.writeComment("  temp2: rowC");
        Writer.writeComment("  temp3: sum");
        Writer.writeComment("  temp4: A_base");
        Writer.writeComment("  temp5: B_base");
        Writer.writeComment("  temp6: C_base");
        Writer.writeComment("  temp7: i");

        // --- base addresses ---
        pushVar(aName);
        Writer.writePop("temp", 4); // A_base
        pushVar(bName);
        Writer.writePop("temp", 5); // B_base
        pushVar(cName);
        Writer.writePop("temp", 6); // C_base

        // i = 0 -> temp7
        Writer.writePush("constant", 0);
        Writer.writePop("temp", 7);

        String loopI = Writer.generateWhileExpLabel();
        String loopEnd = Writer.generateWhileEndLabel();

        Writer.writeLabel(loopI);

        // if (!(i < 8)) goto loopEnd; // i>=8 を lt + not で作る
        Writer.writePush("temp", 7);
        Writer.writePush("constant", 8);
        Writer.writeArithmetic("lt");
        Writer.writeArithmetic("not");
        Writer.writeIf(loopEnd);

        // -------- rowA = A_base + i*8 (i*8は temp0 スクラッチで作る) --------
        // temp0 = i
        Writer.writePush("temp", 7);
        Writer.writePop("temp", 0);

        // temp0 = 2i
        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        // temp0 = 4i
        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        // temp0 = 8i
        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        // rowA = A_base + temp0 -> temp1
        Writer.writePush("temp", 4);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 1);

        // -------- rowC = C_base + i*8 (同様に temp0 を再計算) --------
        Writer.writePush("temp", 7);
        Writer.writePop("temp", 0);

        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        Writer.writePush("temp", 0);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 0);

        Writer.writePush("temp", 6);
        Writer.writePush("temp", 0);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 2);

        // -------- j loop (compile-time unroll) --------
        for (int j = 0; j < 8; j++) {

            Writer.writeComment("---- j = " + j + " ----");

            // sum = 0 -> temp3
            Writer.writePush("constant", 0);
            Writer.writePop("temp", 3);

            for (int k = 0; k < 8; k++) {
                // push A[i][k]
                Writer.writePush("temp", 1); // rowA
                Writer.writePush("constant", k);
                Writer.writeArithmetic("+");
                Writer.writePop("pointer", 1);
                Writer.writePush("that", 0);

                // push B[k][j]
                int offsetB = k * 8 + j;
                Writer.writePush("temp", 5); // B_base
                Writer.writePush("constant", offsetB);
                Writer.writeArithmetic("+");
                Writer.writePop("pointer", 1);
                Writer.writePush("that", 0);

                // prod = Math.multiply(A,B)
                // ★ここで Math.multiply が temp0 を壊してもOK
                // （カーネル状態は temp1..7 に置いてある）
                Writer.writeCall("Math.multiply", 2); // stack: prod

                // sum += prod
                Writer.writePush("temp", 3);
                Writer.writeArithmetic("+");
                Writer.writePop("temp", 3);
            }

            // C[i][j] = sum
            Writer.writePush("temp", 2); // rowC
            Writer.writePush("constant", j);
            Writer.writeArithmetic("+");
            Writer.writePop("pointer", 1);

            Writer.writePush("temp", 3);
            Writer.writePop("that", 0);
        }

        // i++
        Writer.writePush("temp", 7);
        Writer.writePush("constant", 1);
        Writer.writeArithmetic("+");
        Writer.writePop("temp", 7);

        Writer.writeGoto(loopI);
        Writer.writeLabel(loopEnd);

        Writer.writeComment("=== END UNROLLED MUL 8x8 (TEMP-SAFE) ===");
    }

    protected void emitUnrolledMulKernel16(
            String cName,
            String aName,
            String bName,
            int m, int n, int p) {

        if (m != 16 || n != 16 || p != 16) {
            throw new IllegalArgumentException("emitUnrolledMulKernel16: 16x16 only");
        }

        Writer.writeComment("=== BEGIN UNROLLED MUL 16x16 (k-only, NO TEMP, STATIC SAFE) ===");

        // static40=A_base, 41=B_base, 42=C_base
        // static43=i, 44=j, 45=rowA, 46=sum, 47=i16
        pushVar(aName);
        Writer.writePop("static", 40);
        pushVar(bName);
        Writer.writePop("static", 41);
        pushVar(cName);
        Writer.writePop("static", 42);

        // i = 0
        Writer.writePush("constant", 0);
        Writer.writePop("static", 43);

        String loopI = Writer.generateWhileExpLabel();
        String endI = Writer.generateWhileEndLabel();
        Writer.writeLabel(loopI);

        // if !(i < 16) goto endI
        Writer.writePush("static", 43);
        Writer.writePush("constant", 16);
        Writer.writeArithmetic("<"); // lt
        Writer.writeArithmetic("not");
        Writer.writeIf(endI);

        // ---- i16 = i*16 (shift left 4) ※Math.multiplyを使わない ----
        // i16 = i
        Writer.writePush("static", 43);
        Writer.writePop("static", 47);
        // i16 *= 2 (4回)
        for (int t = 0; t < 4; t++) {
            Writer.writePush("static", 47);
            Writer.writePush("static", 47);
            Writer.writeArithmetic("+");
            Writer.writePop("static", 47);
        }

        // rowA = A_base + i16
        Writer.writePush("static", 40);
        Writer.writePush("static", 47);
        Writer.writeArithmetic("+");
        Writer.writePop("static", 45);

        // j = 0
        Writer.writePush("constant", 0);
        Writer.writePop("static", 44);

        String loopJ = Writer.generateWhileExpLabel();
        String endJ = Writer.generateWhileEndLabel();
        Writer.writeLabel(loopJ);

        // if !(j < 16) goto endJ
        Writer.writePush("static", 44);
        Writer.writePush("constant", 16);
        Writer.writeArithmetic("<");
        Writer.writeArithmetic("not");
        Writer.writeIf(endJ);

        // sum = 0
        Writer.writePush("constant", 0);
        Writer.writePop("static", 46);

        // k unroll
        for (int k = 0; k < 16; k++) {

            // A[i][k] = *(rowA + k)
            Writer.writePush("static", 45);
            Writer.writePush("constant", k);
            Writer.writeArithmetic("+");
            Writer.writePop("pointer", 1);
            Writer.writePush("that", 0);

            // B[k][j] = *(B_base + k*16 + j)
            Writer.writePush("static", 41);
            Writer.writePush("constant", k * 16);
            Writer.writeArithmetic("+");
            Writer.writePush("static", 44);
            Writer.writeArithmetic("+");
            Writer.writePop("pointer", 1);
            Writer.writePush("that", 0);

            // prod = A * B
            Writer.writeCall("Math.multiply", 2);

            // sum += prod
            Writer.writePush("static", 46);
            Writer.writeArithmetic("+");
            Writer.writePop("static", 46);
        }

        // C[i][j] address = C_base + i16 + j
        Writer.writePush("static", 42); // C_base
        Writer.writePush("static", 47); // i16
        Writer.writeArithmetic("+");
        Writer.writePush("static", 44); // j
        Writer.writeArithmetic("+");
        Writer.writePop("pointer", 1);

        Writer.writePush("static", 46);
        Writer.writePop("that", 0);

        // j++
        Writer.writePush("static", 44);
        Writer.writePush("constant", 1);
        Writer.writeArithmetic("+");
        Writer.writePop("static", 44);

        Writer.writeGoto(loopJ);
        Writer.writeLabel(endJ);

        // i++
        Writer.writePush("static", 43);
        Writer.writePush("constant", 1);
        Writer.writeArithmetic("+");
        Writer.writePop("static", 43);

        Writer.writeGoto(loopI);
        Writer.writeLabel(endI);

        Writer.writeComment("=== END UNROLLED MUL 16x16 (k-only, NO TEMP, STATIC SAFE) ===");
    }

    // ============================================
    // Dsl set 文
    // ============================================

    protected void compileDslSet() {
        Writer.writeComment("dsl set");

        String cName = tokenizer.token();
        eatIdentifier();
        eat(",");

        compileExpression(); // i
        Writer.writePop("temp", 1);
        eat(",");

        compileExpression(); // j
        Writer.writePop("temp", 2);
        eat(",");

        MatrixInfo miC = matrixInfo.get(cName);
        if (miC == null) {
            error("matrix not declared by 'dsl matrix': " + cName);
        }
        int cols = miC.cols;

        // index = i*cols + j
        Writer.writePush("temp", 1);
        Writer.writePush("constant", cols);
        Writer.writeCall("Math.multiply", 2);
        Writer.writePush("temp", 2);
        Writer.writeArithmetic("+"); // index

        // address = base + index
        pushVar(cName); // base
        Writer.writeArithmetic("+"); // base + index

        Writer.writePop("pointer", 1); // THAT = address

        compileExpression(); // value
        Writer.writePop("that", 0);
    }

    // ============================================
    // DSL set 文
    // dsl set A, i, j, value;
    // ============================================
    protected void emitGenericMulCall(String c, String a, String b, int m, int n, int p) {
        // C, A, B のセグメントとインデックスを取得
        SymbolTable.Kind kindC = getKind(c);
        int indexC = getIndex(c);
        SymbolTable.Kind kindA = getKind(a);
        int indexA = getIndex(a);
        SymbolTable.Kind kindB = getKind(b);
        int indexB = getIndex(b);
        // push C, A, B, m, n, p
        pushVar(kindC, indexC);
        pushVar(kindA, indexA);
        pushVar(kindB, indexB);
        Writer.writePush("constant", m);
        Writer.writePush("constant", n);
        Writer.writePush("constant", p);
        Writer.writeCall("Matrix.mul", 6);
        Writer.writePop("temp", 0); // 戻り値を捨てる
    }
}
