package Jack_Project.Jack_Buildsystem.JackAssembler.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ASMConfig {
    private final String inputFilePath;
    private final String outputFilePath;

    public ASMConfig(String inputFilePath, String outputFilePath) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
    }

    public static ASMConfig load(String configPath) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            prop.load(fis);
        } catch (IOException e) {
            System.err.println("[Config] 警告: 設定ファイルの読み込み失敗。デフォルトを使用。");
        }

        String in = prop.getProperty("asm.in");
        String out = prop.getProperty("asm.out");

        return new ASMConfig(
            (in != null) ? in.trim() : "Prog.asm",
            (out != null) ? out.trim() : null
        );
    }

    public String getInputFilePath() { return inputFilePath; }
    public String getOutputFilePath() { return outputFilePath; }
}