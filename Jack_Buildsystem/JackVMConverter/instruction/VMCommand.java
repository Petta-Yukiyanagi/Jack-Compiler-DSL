package Jack_Project.Jack_Buildsystem.JackVMConverter.instruction;

import Jack_Project.Jack_Buildsystem.JackVMConverter.core.Parser;
import Jack_Project.Jack_Buildsystem.JackVMConverter.io.CodeWriter;

public interface VMCommand {
    void execute(CodeWriter writer, Parser parser, String fileName);
}