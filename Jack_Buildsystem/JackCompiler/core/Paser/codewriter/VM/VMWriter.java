package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * VMWriter: VMコード出力の実装クラス
 * IVMWriterのルールに従って、実際にファイルへ文字列を書き込む
 */
public class VMWriter implements IVMWriter {
    private final Writer out;
    private int ifCounter = 0;
    private int whileCounter = 0;
    private String currentFunction = "";
    private boolean denyThisSegment = true;

    public VMWriter(String path) throws IOException {
        this.out = new BufferedWriter(new FileWriter(path));
    }

    // ========================================
    // 内部専用のユーティリティ
    // ========================================
    private void writeLine(String line) {
        try {
            out.write(line);
            out.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================
    // インターフェースの実装
    // ========================================

    @Override
    public void setCurrentFunction(String funcName) {
        this.currentFunction = funcName == null ? "" : funcName;
    }

    @Override
    public void writePush(String segment, int index) {
        writeLine(String.format("push %s %d", segment, index));
    }

    @Override
    public void writePop(String segment, int index) {
        if ("constant".equals(segment)) {
            throw new IllegalArgumentException("pop constant は無効な操作です");
        }
        writeLine(String.format("pop %s %d", segment, index));
    }

    @Override
    public void writeArithmetic(String op) {
        switch (op) {
            case "+": case "add": writeLine("add"); return;
            case "-": case "sub": writeLine("sub"); return;
            case "neg": writeLine("neg"); return;
            case "&": case "and": case "&&": writeLine("and"); return;
            case "|": case "or": case "||": writeLine("or"); return;
            case "~": case "not": writeLine("not"); return;
            case "<": case "lt": writeLine("lt"); return;
            case ">": case "gt": writeLine("gt"); return;
            case "=": case "==": case "eq": writeLine("eq"); return;
            case "!=": writeLine("eq"); writeLine("not"); return;
            case "<=": writeLine("gt"); writeLine("not"); return;
            case ">=": writeLine("lt"); writeLine("not"); return;
            case "*": case "mul": writeCall("Math.multiply", 2); return;
            case "/": case "div": writeCall("Math.divide", 2); return;
            default: throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    @Override
    public void writeLabel(String label) {
        writeLine("label " + label);
    }

    @Override
    public void writeGoto(String label) {
        writeLine("goto " + label);
    }

    @Override
    public void writeIf(String label) {
        writeLine("if-goto " + label);
    }

    @Override
    public String generateIfTrueLabel() {
        return String.format("IF_TRUE%d", ifCounter);
    }

    @Override
    public String generateIfFalseLabel() {
        return String.format("IF_FALSE%d", ifCounter);
    }

    @Override
    public String generateIfEndLabel() {
        int current = ifCounter;
        ifCounter++;
        return String.format("IF_END%d", current);
    }

    @Override
    public String generateWhileExpLabel() {
        return String.format("WHILE_EXP%d", whileCounter);
    }

    @Override
    public String generateWhileEndLabel() {
        int current = whileCounter;
        whileCounter++;
        return String.format("WHILE_END%d", current);
    }

    @Override
    public void writeCall(String name, int nArgs) {
        writeLine(String.format("call %s %d", name, nArgs));
    }

    @Override
    public void writeFunction(String name, int nLocals) {
        writeLine(String.format("function %s %d", name, nLocals));
        setCurrentFunction(name);
    }

    @Override
    public void writeReturn() {
        writeLine("return");
    }

    @Override
    public void writeStringConstant(String str) {
        writePush("constant", str.length());
        writeCall("String.new", 1);
        for (char c : str.toCharArray()) {
            writePush("constant", (int) c);
            writeCall("String.appendChar", 2);
        }
    }

    @Override
    public void safePush(String segment, int index) {
        if (denyThisSegment && segment.equals("this")) {
            throw new RuntimeException("ERROR: push this " + index +
                    " is not allowed in DSL matrix mode (THIS corruption risk)");
        }
        writePush(segment, index);
    }

    @Override
    public void safePop(String segment, int index) {
        if (denyThisSegment && segment.equals("this")) {
            throw new RuntimeException("ERROR: pop this " + index +
                    " is not allowed in DSL matrix mode (THIS corruption risk)");
        }
        writePop(segment, index);
    }

    @Override
    public void writeComment(String comment) {
        writeLine("// " + comment);
    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}