package Jack_Project.Jack_Buildsystem.JackCompiler.io;

import java.nio.file.Path;
import java.util.Map;

import Jack_Project.Jack_Buildsystem.JackCompiler.config.Config;

/**
 * ファイル操作の全般を管理する。
 * 下位パッケージ（io）の Collector や Preparer を利用して
 * コンパイルに必要なファイル環境を整える。
 */
public class FileManager {
    private final Config config;
    private final SourceCollector collector;
    private final OutputPreparer preparer;

    public FileManager(Config config) {
        this.config = config;
        this.collector = new SourceCollector();
        this.preparer = new OutputPreparer();
    }

    /**
     * コンパイルのための準備（フォルダ作成、ファイル収集）を一括で行う。
     */
    public Map<String, Path> prepareProject() {
        // 出力先の準備
        preparer.prepare(config.getOutVmDir(), config.getOutXmlDir());

        // ソースファイルの収集
        return collector.collect(config.getInDir(), config.getOsDir());
    }
}