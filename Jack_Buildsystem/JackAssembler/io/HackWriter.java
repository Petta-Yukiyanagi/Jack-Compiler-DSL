package Jack_Project.Jack_Buildsystem.JackAssembler.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 【OUTPUT】
 * マシンコード（バイナリ文字列）を .hack ファイルに書き込む専門家。
 */
public class HackWriter implements AutoCloseable {
    private BufferedWriter writer;

    public HackWriter(String fileName) {
        try {
            this.writer = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            throw new RuntimeException("ファイルを開けませんでした: " + fileName, e);
        }
    }

    /**
     * 16ビットのバイナリ文字列を1行書き出す
     */
    public void writeLine(String machineCode) {
        try {
            writer.write(machineCode);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("書き込みエラー: " + machineCode, e);
        }
    }

    /**
     * 書き終えたら閉じる
     */
    @Override
    public void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("クローズに失敗しました", e);
        }
    }
}