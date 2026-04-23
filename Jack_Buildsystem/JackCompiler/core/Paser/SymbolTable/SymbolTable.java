package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.SymbolTable;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    public enum Kind {
        STATIC,
        FIELD,
        ARG,
        VAR,
        NONE
    }

    public static class Symbol {
    String type;
    Kind kind;
    int index;

Symbol(String type, Kind kind, int index) {
    this.type = type;
    this.kind = kind;
    this.index = index;
}
}

private Map<String, Symbol> table;

private int staticIndex;
private int fieldIndex;
private int argIndex;
private int varIndex;

public SymbolTable() {
    this.table = new HashMap<>();
    reset();
}

public void reset() {
    table.clear();
    staticIndex = 0;
    fieldIndex = 0;
    argIndex = 0;
    varIndex = 0;
}

public void define(String name, String type, Kind kind) {
    int index;
    switch (kind) {
        case STATIC:
            index = staticIndex++;
            break;
        case FIELD:
            index = fieldIndex++;
            break;
        case ARG:
            index = argIndex++;
            break;
        case VAR:
            index = varIndex++;
            break;
        default:
            throw new IllegalArgumentException("Invalid kind: " + kind);
    }

    table.put(name, new Symbol(type, kind, index));
}

public int varCount(Kind kind) {
    switch (kind) {
        case STATIC:
            return staticIndex;
        case FIELD:
            return fieldIndex;
        case ARG:
            return argIndex;
        case VAR:
            return varIndex;
        default:
            return 0;
    }
}

public Kind kindOf(String name) {
    Symbol symbol = table.get(name);
    if (symbol == null) {
        return Kind.NONE;
    }
    return symbol.kind;
}

public String typeOf(String name) {
    Symbol symbol = table.get(name);
    if (symbol == null) {
        return null;
    }
    return symbol.type;
}

public int indexOf(String name) {
    Symbol symbol = table.get(name);
    if (symbol == null) {
        return -1;
    }
    return symbol.index;
}

public void printTable() {
    System.out.println("=== Symbol Table ===");
    for (Map.Entry<String, Symbol> entry : table.entrySet()) {
        Symbol s = entry.getValue();
        System.out.printf("%-15s | %-10s | %-8s | index: %d%n",
            entry.getKey(), s.type, s.kind, s.index);
    }
    System.out.println("====================");
    }
}
