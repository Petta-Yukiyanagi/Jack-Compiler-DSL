package Jack_Project.Jack_Buildsystem.JackVMConverter.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import Jack_Project.Jack_Buildsystem.JackVMConverter.config.VMConfig;

public class SourceProject {
    private final File directory;

    public SourceProject(String path) {
        this.directory = new File(path);
    }

    public List<File> getVmFiles() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".vm"));
        if (files == null) return Collections.emptyList();

        List<File> vmFiles = new ArrayList<>(Arrays.asList(files));
        vmFiles.sort((a, b) -> {
            if (a.getName().equals("Sys.vm")) return -1;
            if (b.getName().equals("Sys.vm")) return 1;
            return a.getName().compareTo(b.getName());
        });
        return vmFiles;
    }

    public String getOutputAsmPath(VMConfig config) {
        if (config.getOutputPath() != null && !config.getOutputPath().isEmpty()) {
            return config.getOutputPath();
        }
        // デフォルト：入力フォルダ名に基づき .asm を作成
        return directory.getAbsolutePath() + File.separator + directory.getName() + ".asm";
    }
}