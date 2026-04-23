package Jack_Project.Jack_Buildsystem.JackCompiler.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 【OUTPUT】
 * コンパイル結果を出力するための環境（ディレクトリ）を整える専門家。
 */
public class OutputPreparer {

    /**
     * 指定されたディレクトリが存在することを保証する（なければ作る）。
     */
    public void prepare(Path... dirs) {
        for (Path dir : dirs) {
            ensureDir(dir);
        }
    }

    private void ensureDir(Path dir) {
        try {
            // 既に存在していてもエラーにならず、中間のフォルダも作成する
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("出力ディレクトリの作成に失敗しました: " + dir, e);
        }
    }
}