package Jack_Project.Jack_Buildsystem.JackCompiler.io;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 【INPUT】
 * ファイルシステムから Jack ソースコードを探索し、収集する専門家。
 */
public class SourceCollector {

    public Map<String, Path> collect(String inDir, String osDir) {
        LinkedHashMap<String, Path> collected = new LinkedHashMap<>();

        // 1. まずOS標準ライブラリを探査
        if (osDir != null && !osDir.isEmpty()) {
            scan(Paths.get(osDir), collected, false);
        }
        // 2. プロジェクトのディレクトリを探査（同名はプロジェクト優先）
        scan(Paths.get(inDir), collected, true);

        return collected;
    }

    private void scan(Path root, Map<String, Path> map, boolean override) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".jack")) {
                        String base = file.getFileName().toString();
                        if (override || !map.containsKey(base)) {
                            map.put(base, file.toAbsolutePath().normalize());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("ソースコードの探索中にエラーが発生しました: " + root, e);
        }
    }
}