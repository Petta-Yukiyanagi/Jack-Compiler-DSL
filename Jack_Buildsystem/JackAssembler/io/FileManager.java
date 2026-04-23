package Jack_Project.Jack_Buildsystem.JackAssembler.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileManager {
    /**
     * 出力ファイルが置かれる予定のフォルダを準備する
     */
    public void prepareOutputEnvironment(String outputFilePath) {
        Path path = Paths.get(outputFilePath).getParent();
        if (path != null && !Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("出力フォルダの作成に失敗しました: " + path, e);
            }
        }
    }
}