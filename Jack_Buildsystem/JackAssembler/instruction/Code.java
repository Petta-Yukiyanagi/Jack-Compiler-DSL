package Jack_Project.Jack_Buildsystem.JackAssembler.instruction;

import java.util.Map;

public class Code{
    private static final Map<String,String> dest = Map.ofEntries(
        Map.entry("","000"),
        Map.entry("M","001"),
        Map.entry("D","010"),
        Map.entry("DM","011"),
        Map.entry("A","100"),
        Map.entry("AM","101"),
        Map.entry("AD","110"),
        Map.entry("ADM","111")
    );

    private static final Map<String,String> comp = Map.ofEntries(
        Map.entry("0",   "0101010"),
        Map.entry("1",   "0111111"),
        Map.entry("-1",  "0111010"),
        Map.entry("D",   "0001100"),
        Map.entry("A",   "0110000"),
        Map.entry("!D",  "0001101"),
        Map.entry("!A",  "0110001"),
        Map.entry("-D",  "0001111"),
        Map.entry("-A",  "0110011"),
        Map.entry("D+1", "0011111"),
        Map.entry("A+1", "0110111"),
        Map.entry("D-1", "0001110"),
        Map.entry("A-1", "0110010"),
        Map.entry("D+A", "0000010"),
        Map.entry("D-A", "0010011"),
        Map.entry("A-D", "0000111"),
        Map.entry("D&A", "0000000"),
        Map.entry("D|A", "0010101"),
        Map.entry("M",   "1110000"),
        Map.entry("!M",  "1110001"),
        Map.entry("-M",  "1110011"),
        Map.entry("M+1", "1110111"),
        Map.entry("M-1", "1110010"),
        Map.entry("D+M", "1000010"),
        Map.entry("D-M", "1010011"),
        Map.entry("M-D", "1000111"),
        Map.entry("D&M", "1000000"),
        Map.entry("D|M", "1010101")
    );

    private static final Map<String, String> jump = Map.ofEntries(
        Map.entry("",  "000"),
        Map.entry("JGT", "001"),
        Map.entry("JEQ", "010"),
        Map.entry("JGE", "011"),
        Map.entry("JLT", "100"),
        Map.entry("JNE", "101"),
        Map.entry("JLE", "110"),
        Map.entry("JMP", "111")
    );
    public static String dest(String mnemonic) {
        if (mnemonic == null) return "000";
        return dest.getOrDefault(mnemonic, "000");
    }

    public static String comp(String mnemonic) {
        if (mnemonic == null) return "0000000";
        return comp.getOrDefault(mnemonic, "0000000");
    }

    public static String jump(String mnemonic) {
        if (mnemonic == null) return "000";
        return jump.getOrDefault(mnemonic, "000");
    }
}