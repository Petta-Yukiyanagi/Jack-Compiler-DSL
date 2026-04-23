package Jack_Project.Jack_Buildsystem.JackCompiler.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 実行時設定を保持するクラス。
 * 設定ファイル(config.txt)から読み込み、CLI引数で上書きする。
 */
public class Config {
    private final Map<String, String> opts = new HashMap<>();

    private static final String CONFIG_PATH = "config.txt";

    public Config(String[] args) {
        // 1. デフォルト値のセット
        setDefaults();

        // 2. 設定ファイルからの読み込み
        loadFromFile(CONFIG_PATH);

        // 3. CLI引数による上書き（これが最優先）
        parseArgs(args);
    }

    private void setDefaults() {
        opts.put("mode", "xml");
        opts.put("in", ".");
        opts.put("out", "build");
    }

    private void loadFromFile(String path) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value != null) {
                    // trim() で前後を掃除し、put する
                    opts.put(key, value.trim());
                }
            }
            System.out.println("[Config] 設定ファイルを読み込みました: " + path);
        } catch (IOException e) {
            System.err.println("[Config] 設定ファイルの読み込みに失敗しました。デフォルト値を使用します。: " + e.getMessage());
        }
    }

    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--") && i + 1 < args.length) {
                opts.put(a.substring(2), args[++i]);
            } else if (a.equals("--dumpTokens")) {
                opts.put("dumpTokens", "true");
            }
        }
    }

    // 各種設定値へのアクセス口（Getter）
    public String getMode() {
        return opts.get("mode").toLowerCase();
    }

    public String getInDir() {
        return opts.get("in");
    }

    public String getOsDir() {
        return opts.getOrDefault("os", "");
    }

    public String getOutRoot() {
        return opts.get("out");
    }

    public boolean isDumpTokens() {
        return "true".equalsIgnoreCase(opts.get("dumpTokens"));
    }

    public Path getOutVmDir() {
        return Paths.get(getOutRoot(), "vm");
    }

    public Path getOutXmlDir() {
        return Paths.get(getOutRoot(), "xml");
    }
}