package Jack_Project.Jack_Buildsystem.JackCompiler.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import Jack_Project.Jack_Buildsystem.JackCompiler.config.Config;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM.VMGenerator;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.VM.VMWriter;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML.XMLGenerator;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML.XMLWriter;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Tokenizer.Tokenizer;

public class Luncher {
    private final Config config;
    private final Map<String, Path> fileMap;

    public Luncher(Config config, Map<String, Path> fileMap) {
        this.config = config;
        this.fileMap = fileMap;
    }

    public void run() {
        System.out.println("[Mode] " + config.getMode());
        System.out.println("[Files] " + fileMap.size() + " files found.\n");

        for (Map.Entry<String, Path> entry : fileMap.entrySet()) {
            String sourceName = entry.getKey();
            String outputBaseName = sourceName.endsWith(".jack")
                    ? sourceName.substring(0, sourceName.length() - ".jack".length())
                    : sourceName;
            Path path = entry.getValue();

            processFile(sourceName, outputBaseName, path);
        }
    }

    private void processFile(String sourceName, String outputBaseName, Path path) {
        System.out.println("Processing: " + sourceName);
        String mode = config.getMode();

        try {
            // 1. トークナイザーの準備（XML/VM共通）
            Tokenizer tokenizer = new Tokenizer(Files.readAllLines(path));

            // 2. モードによる分岐と実行
            if ("xml".equals(mode)) {

                // 出力先のパスを生成 (例: build/xml/Main.xml)
                Path outPath = config.getOutXmlDir().resolve(outputBaseName + ".xml");
                Files.createDirectories(outPath.getParent()); // フォルダがなければ作る

                // XML用の頭（Engine）と手（Writer）を組み立てる
                XMLWriter xmlWriter = new XMLWriter();
                XMLGenerator xmlEngine = new XMLGenerator(tokenizer, xmlWriter);

                // コンパイル実行
                xmlEngine.compileClass();

                // 終了処理（ファイル書き込み完了）
                xmlWriter.saveToFile(outPath.toString());

            } else if ("vm".equals(mode)) {

                // 出力先のパスを生成 (例: build/vm/Main.vm)
                Path outPath = config.getOutVmDir().resolve(outputBaseName + ".vm");
                Files.createDirectories(outPath.getParent()); // フォルダがなければ作る

                // VM用の頭（Engine）と手（Writer）を組み立てる
                VMWriter vmWriter = new VMWriter(outPath.toString());
                VMGenerator vmEngine = new VMGenerator(tokenizer, vmWriter);

                // コンパイル実行
                vmEngine.compileClass();

                // 終了処理
                vmWriter.close();

            } else {
                System.err.println("エラー：不明なモードが指定されました (" + mode + ")");
            }

        } catch (Exception e) {
            System.err.println(sourceName + " の処理中にエラーが発生しました:");
            e.printStackTrace();
        }
    }
}