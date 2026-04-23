package Jack_Project.Jack_Buildsystem.JackAssembler;

import Jack_Project.Jack_Buildsystem.JackAssembler.config.ASMConfig;
import Jack_Project.Jack_Buildsystem.JackAssembler.core.AssemblerEngine;
import Jack_Project.Jack_Buildsystem.JackAssembler.io.FileManager;
import Jack_Project.Jack_Buildsystem.JackAssembler.io.SourceProject;

/**
 * アセンブラのメインエントリーポイント。
 * 設定のロード、プロジェクトの初期化、エンジンの起動を統括する。
 */
public class MainASM {

    private static final String CONFIG_FILE = "config.txt";

    public static void main(String[] args) {
        System.out.println("=== Jack Assembler Start ===");

        try {
            // 1. 設定層: 外部ファイル(config.txt)から設定を読み込む
            ASMConfig config = ASMConfig.load(CONFIG_FILE);

            // 2. I/O層: 入力ファイルの特定と出力パスの解決
            SourceProject project = new SourceProject(config.getInputFilePath());
            if (!project.exists()) {
                System.err.println("[ERROR] 入力ファイルが見つかりません: " + config.getInputFilePath());
                System.exit(1);
            }

            String outputPath = project.getOutputHackPath(config);

            // 3. I/O層: 出力先ディレクトリの自動生成（FileManagerの責務）
            FileManager fileManager = new FileManager();
            fileManager.prepareOutputEnvironment(outputPath);

            // 4. コア層: アセンブルエンジンの起動
            // ここに Parse, SecondParse, SymbolTable などの複雑なロジックが全て隠蔽されている
            AssemblerEngine engine = new AssemblerEngine(project, outputPath);
            engine.run();

            System.out.println("=== Assembly Process Finished ===");
        } catch (RuntimeException e) {
            System.err.println("[ERROR] Assembly failed: " + e.getMessage());
            System.exit(1);
        }
    }
}