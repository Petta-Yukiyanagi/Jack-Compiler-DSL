package Jack_Project.Jack_Buildsystem.JackVMConverter.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FaileManager {
    private final File directory;

    /**
     * @param path 入力ディレクトリまたはファイルのパス
     */
    public FaileManager(String path) {
        this.directory = new File(path);
        if (!directory.exists()) {
            throw new IllegalArgumentException("指定されたパスが存在しません: " + path);
        }
    }

    /**
     * 変換対象となる .vm ファイルのリストを取得します。
     * Sys.vm が存在する場合は、必ずリストの先頭に配置されます。
     */
    public List<File> getVmFiles() {
        if (directory.isFile()) {
            return directory.getName().endsWith(".vm") ? List.of(directory) : List.of();
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".vm"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        List<File> vmFiles = new ArrayList<>(Arrays.asList(files));

        // Sys.vm を先頭にし、それ以外は名前順に並べ替える
        vmFiles.sort((a, b) -> {
            String nameA = a.getName();
            String nameB = b.getName();
            if (nameA.equals("Sys.vm"))
                return -1;
            if (nameB.equals("Sys.vm"))
                return 1;
            return nameA.compareTo(nameB);
        });

        return vmFiles;
    }

    /**
     * 出力先となる .asm ファイルのフルパスを生成します。
     * ルール: ディレクトリパス / ディレクトリ名.asm
     */
    public String getOutputAsmPath() {
        if (directory.isFile()) {
            return directory.getAbsolutePath().replace(".vm", ".asm");
        }
        // ディレクトリの場合: /path/to/Dir/Dir.asm
        return new File(directory, directory.getName() + ".asm").getAbsolutePath();
    }
}