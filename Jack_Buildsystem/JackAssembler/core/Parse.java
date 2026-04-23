package Jack_Project.Jack_Buildsystem.JackAssembler.core;

public class Parse {
    public enum CommandType{
        A_INSTRUCTION,
        C_INSTRUCTION,
        L_INSTRUCTION
    }

    
    public static CommandType commandType(String command){
        if(command.startsWith("@")){
            return CommandType.A_INSTRUCTION;
        }else if (command.startsWith("(")&& command.endsWith(")")){
            return CommandType.L_INSTRUCTION;
        }else {
            return CommandType.C_INSTRUCTION;
        }
    }

    //A命令orLabel
    public static String symbol(String command, CommandType type){
        if (type == CommandType.A_INSTRUCTION){
            return command.substring(1); //先頭@を除去
        }else if(type == CommandType.L_INSTRUCTION) {
            return command.substring(1, command.length()-1);//先頭/と後方/を除去
        }else {
            return null;
        }
    }

    //C命令
    public static String dest(String command){
        if (command.contains("=")) {
            return command.split("=")[0];//dest分部を返す。=の左側の文字を取り出す。
        }
        return null;
    }
    
    // compの部分を返す。=の右または;の前の文字列を取り出す。
    public static String comp(String command) {
        String compPart = command;

        if (command.contains("=")) {
            compPart = command.split("=")[1];
        }
        if (compPart.contains(";")) {
            compPart = compPart.split(";")[0];
        }

        return compPart;
    }

    // jumpの部分を返す。;の右側の文字列を取り出す。
    public static String jump(String command) {
        if (command.contains(";")) {
            return command.split(";")[1];
        }
        return null;
    }
}
