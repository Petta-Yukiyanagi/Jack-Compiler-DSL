package Jack_Project.Jack_Buildsystem.JackVMConverter.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class VMConfig {
    private final String inputPath;
    private final String outputPath;

    public VMConfig(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
    }

    public static VMConfig load(String configPath) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            prop.load(fis);
        } catch (IOException e) {
            System.err.println("[Config] 警告: 設定ファイルの読み込み失敗。デフォルトを使用。");
        }

        // 全ての取得値に .trim() を適用して ) などのゴミを除去
        String in = prop.getProperty("vm.in");
        String out = prop.getProperty("vm.out");

        return new VMConfig(
            (in != null) ? in.trim() : null,
            (out != null) ? out.trim() : null
        );
    }

    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
}