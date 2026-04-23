package Jack_Project.Jack_Buildsystem.JackVMConverter;

import java.io.File;

import Jack_Project.Jack_Buildsystem.JackVMConverter.config.VMConfig;
import Jack_Project.Jack_Buildsystem.JackVMConverter.core.VMTranslatorEngine;
import Jack_Project.Jack_Buildsystem.JackVMConverter.io.CodeWriter;
import Jack_Project.Jack_Buildsystem.JackVMConverter.io.SourceProject;

public class MainVM {
    public static void main(String[] args) {
        // 設定ファイルの場所を指定（引数がない場合はデフォルトパス）
        String configPath = (args.length > 0) ? args[0] : "config.txt";

        // 1. Config層：設定ファイルから情報をロード
        VMConfig config = VMConfig.load(configPath);

        // 2. Source層：入力パスを決定（Configに指定がなければ引数などから補完）
        String inputPath = (config.getInputPath() != null) ? config.getInputPath() : ".";
        SourceProject project = new SourceProject(inputPath);
        
        // 3. Output層：出力パスを決定
        String outputFileName = project.getOutputAsmPath(config);
        CodeWriter writer = new CodeWriter(outputFileName);

        // 4. Engine層：翻訳実行
        VMTranslatorEngine engine = new VMTranslatorEngine(writer);

        // --- 翻訳プロセス ---
        writer.writeInit();
        for (File vmFile : project.getVmFiles()) {
            engine.translate(vmFile);
        }
        writer.close();

        System.out.println("VM変換完了：" + outputFileName);
    }
}