package Jack_Project.Jack_Buildsystem.JackAssembler.core;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private static final Map<String, Integer> table = new HashMap<>();
    private static int nextVariableAddress = 16;

    static {
        table.put("SP", 0);
        table.put("LCL", 1);
        table.put("ARG", 2);
        table.put("THIS", 3);
        table.put("THAT", 4);
        for (int i = 0; i <= 15; i++) {
            table.put("R" + i, i);
        }
        table.put("SCREEN", 16384);
        table.put("KBD", 24576);
    }

    // ラベル（ROM番地）を追加
    public static void addEntry(String symbol, int address) {
        table.put(symbol, address);
    }

    // ラベルや変数が登録されているか？
    public static boolean contains(String symbol) {
        return table.containsKey(symbol);
    }

    // ラベルや変数のアドレス取得
    public static int getAddress(String symbol) {
        return table.get(symbol);
    }

    // 変数（RAM番地）を割り当て（16以降）
    public static int getOrAddVariable(String label) {
        if (!table.containsKey(label)) {
            System.out.println("新規変数登録: " + label + " → " + nextVariableAddress);
            table.put(label, nextVariableAddress++);
        } else{
            System.out.println("既存変数再利用: " + label + " → " + table.get(label));
        }
        return table.get(label);
    }

    // 16ビットバイナリで返す（変数用）
    public static String getOrAddVariableAsBinary(String label) {
        int address = getOrAddVariable(label);
        return String.format("%16s", Integer.toBinaryString(address)).replace(' ', '0');
    }
}
