package Jack_Project.Jack_Buildsystem.JackAssembler.core;

import java.util.ArrayList;
import java.util.List;

public class SecondParse {

    public static List<String> run(List<String> parsedCommands) {
        List<String> machineCodes = new ArrayList<>();

        for (String command : parsedCommands) {
            if (command == null) continue;

            command = command.trim();
            if (command.isEmpty()) continue;

            // A命令 (@xxx)
            if (command.startsWith("@")) {
                String symbol = command.substring(1).trim();
                String binary;

                if (symbol.matches("\\d+")) {
                    // 数値（例: @21）
                    int value = Integer.parseInt(symbol);
                    binary = toAInstruction(value);

                } else if (SymbolTable.contains(symbol)) {
                    // ラベル or 事前定義シンボル（R0, LCL, LOOP, END など）
                    int address = SymbolTable.getAddress(symbol);
                    binary = toAInstruction(address);

                } else {
                    // 変数シンボル
                    // 既存の getOrAddVariableAsBinary() が返すものが
                    // 2進数/10進数どちらでも動くように吸収する
                    String raw = SymbolTable.getOrAddVariableAsBinary(symbol);
                    raw = raw.trim();

                    int value;
                    if (raw.matches("[01]+")) {
                        // 2進数文字列とみなす
                        value = Integer.parseInt(raw, 2);
                    } else if (raw.matches("\\d+")) {
                        // 10進数文字列とみなす
                        value = Integer.parseInt(raw);
                    } else {
                        // 想定外フォーマットの場合は 0 にフォールバック
                        value = 0;
                    }
                    binary = toAInstruction(value);
                }

                machineCodes.add(binary);

            } else {
                // 既に第1パスで完成している C命令 or ラベル除去後命令はそのまま
                machineCodes.add(command);
            }
        }

        return machineCodes;
    }

    /**
     * Hack の A 命令フォーマットに変換するヘルパー
     * 常に「先頭ビット 0 + 15bit の値」にする。
     */
    private static String toAInstruction(int value) {
        int v = value & 0x7FFF; // 15bit にマスク
        String bits15 = String.format("%15s", Integer.toBinaryString(v)).replace(' ', '0');
        return "0" + bits15;
    }
}
