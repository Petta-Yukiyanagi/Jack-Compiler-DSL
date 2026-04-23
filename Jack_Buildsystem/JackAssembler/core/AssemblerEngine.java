package Jack_Project.Jack_Buildsystem.JackAssembler.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Jack_Project.Jack_Buildsystem.JackAssembler.instruction.Code;
import Jack_Project.Jack_Buildsystem.JackAssembler.io.HackWriter;
import Jack_Project.Jack_Buildsystem.JackAssembler.io.SourceProject;

/**
 * アセンブルの工程（第1パス、第2パスの実行）を管理するエンジン。
 */
public class AssemblerEngine {
    private final SourceProject project;
    private final String outputPath;

    public AssemblerEngine(SourceProject project, String outputPath) {
        this.project = project;
        this.outputPath = outputPath;
    }

    public void run() {
        try {
            // 1. 第1パス：命令のクリーニング、ラベル登録、C命令のバイナリ化
            List<String> intermediateCommands = executeFirstPass();

            // 2. 第2パス：SecondParseを利用した変数・シンボルの解決
            List<String> machineCodes = SecondParse.run(intermediateCommands);

            // 3. 出力：HackWriterを利用したファイル書き出し
            save(machineCodes);

            System.out.println("Assemble Successful: " + outputPath);
        } catch (IOException e) {
            System.err.println("アセンブル中にエラーが発生しました: " + e.getMessage());
        }
    }

    /**
     * 第1パス：ファイルを読み込み、ラベルをシンボルテーブルに登録しつつ、
     * C命令をバイナリ化した中間リストを作成する。
     */
    private List<String> executeFirstPass() throws IOException {
        List<String> parsedList = new ArrayList<>();
        int romAddress = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(project.getInputFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 1. クリーニング（空白除去、コメント除去）
                line = clean(line);
                if (line.isEmpty()) continue;

                // 2. 命令タイプの判定
                Parse.CommandType type = Parse.commandType(line);

                switch (type) {
                    case A_INSTRUCTION:
                        // A命令はそのままリストに追加（SecondParseでアドレス解決するため）
                        String aSymbol = Parse.symbol(line, type);
                        parsedList.add("@" + aSymbol);
                        romAddress++;
                        break;

                    case L_INSTRUCTION:
                        // ラベルはシンボルテーブルに登録（リストには追加しない）
                        String label = Parse.symbol(line, type);
                        SymbolTable.addEntry(label, romAddress);
                        break;

                    case C_INSTRUCTION:
                        // C命令はこの段階でバイナリ化してリストに追加
                        String binary = encodeC(line);
                        parsedList.add(binary);
                        romAddress++;
                        break;
                }
            }
        }
        return parsedList;
    }

    /**
     * C命令をビット列(111 + comp + dest + jump)に変換する
     */
    private String encodeC(String command) {
        String d = Parse.dest(command);
        String c = Parse.comp(command);
        String j = Parse.jump(command);
        // Codeクラス（変換テーブル）を利用
        return "111" + Code.comp(c) + Code.dest(d) + Code.jump(j);
    }

    /**
     * コメントと空白を除去するヘルパー
     */
    private String clean(String line) {
        if (line.contains("//")) {
            line = line.split("//")[0];
        }
        return line.trim();
    }

    /**
     * 最終的なマシンコードをファイルに書き出す
     */
    private void save(List<String> machineCodes) throws IOException {
        try (HackWriter writer = new HackWriter(outputPath)) {
            for (String code : machineCodes) {
                writer.writeLine(code);
            }
        }
    }
}