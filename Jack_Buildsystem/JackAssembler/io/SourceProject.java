package Jack_Project.Jack_Buildsystem.JackAssembler.io;

import java.io.File;

import Jack_Project.Jack_Buildsystem.JackAssembler.config.ASMConfig;

public class SourceProject {
    private final File inputFile;

    public SourceProject(String inputPath) {
        this.inputFile = new File(inputPath);
    }

    public File getInputFile() { return inputFile; }

    /**
     * 出力先のフルパスを決定する（論理的な計算のみ）
     */
    public String getOutputHackPath(ASMConfig config) {
        if (config.getOutputFilePath() != null && !config.getOutputFilePath().isEmpty()) {
            return config.getOutputFilePath();
        }
        String path = inputFile.getAbsolutePath();
        return path.substring(0, path.lastIndexOf('.')) + ".hack";
    }

    public boolean exists() {
        return inputFile.exists() && inputFile.isFile();
    }
}