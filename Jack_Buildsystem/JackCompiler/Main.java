package Jack_Project.Jack_Buildsystem.JackCompiler;

import java.nio.file.Path;
import java.util.Map;

import Jack_Project.Jack_Buildsystem.JackCompiler.config.Config;
import Jack_Project.Jack_Buildsystem.JackCompiler.core.Luncher;
import Jack_Project.Jack_Buildsystem.JackCompiler.io.FileManager;

public class Main {
    public static void main(String[] args) {
        Config config = new Config(args);

        // ファイル周りの管理を任せる
        FileManager fileManager = new FileManager(config);
        Map<String, Path> sourceFiles = fileManager.prepareProject();

        if (sourceFiles.isEmpty()) {
            System.err.println("コンパイル対象が見つかりません。");
            return;
        }

        // 実行へ
        Luncher luncher = new Luncher(config, sourceFiles);
        luncher.run();
    }
}