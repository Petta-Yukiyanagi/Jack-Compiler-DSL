package Jack_Project.Jack_Buildsystem.JackVMConverter.core;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import Jack_Project.Jack_Buildsystem.JackVMConverter.instruction.VMCommand;
import Jack_Project.Jack_Buildsystem.JackVMConverter.io.CodeWriter;

public class VMTranslatorEngine {
    private final CodeWriter writer;
    private final Map<Parser.CommandType, VMCommand> commandMap;

    public VMTranslatorEngine(CodeWriter writer) {
        this.writer = writer;
        this.commandMap = new EnumMap<>(Parser.CommandType.class);
        initializeCommands();
    }

    private void initializeCommands() {
        // 各命令と VMConverter のメソッドを紐付け（ラムダ式を使用）
        commandMap.put(Parser.CommandType.C_ARITHMETIC, (w, p, f) -> VMConverter.writeArithmetic(w, p.arg1()));
        commandMap.put(Parser.CommandType.C_PUSH, (w, p, f) -> VMConverter.writePushPop(w, Parser.CommandType.C_PUSH, f, p.arg1(), p.arg2()));
        commandMap.put(Parser.CommandType.C_POP,  (w, p, f) -> VMConverter.writePushPop(w, Parser.CommandType.C_POP, f, p.arg1(), p.arg2()));
        commandMap.put(Parser.CommandType.C_LABEL, (w, p, f) -> VMConverter.writeLabel(w, p.arg1()));
        commandMap.put(Parser.CommandType.C_GOTO,  (w, p, f) -> VMConverter.writeGoto(w, p.arg1()));
        commandMap.put(Parser.CommandType.C_IF,    (w, p, f) -> VMConverter.writeIf(w, p.arg1()));
        commandMap.put(Parser.CommandType.C_FUNCTION, (w, p, f) -> VMConverter.writeFunction(w, p.arg1(), p.arg2()));
        commandMap.put(Parser.CommandType.C_RETURN,   (w, p, f) -> VMConverter.writeReturn(w));
        commandMap.put(Parser.CommandType.C_CALL,     (w, p, f) -> VMConverter.writeCall(w, p.arg1(), p.arg2()));
    }

    public void translate(File vmFile) {
        String fileName = vmFile.getName().replace(".vm", "");
        try (Parser parser = new Parser(vmFile.getAbsolutePath())) {
            while (parser.hasMoreLines()) {
                parser.advance();
                if (parser.getCurrentCommand() == null) continue;

                Parser.CommandType type = parser.commandType(parser.getCurrentCommand());
                VMCommand command = commandMap.get(type);
                
                if (command != null) {
                    command.execute(writer, parser, fileName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}