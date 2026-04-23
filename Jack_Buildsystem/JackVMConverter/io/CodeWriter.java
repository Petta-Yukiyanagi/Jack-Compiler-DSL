package Jack_Project.Jack_Buildsystem.JackVMConverter.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import Jack_Project.Jack_Buildsystem.JackVMConverter.core.VMConverter;

public class CodeWriter {
    private BufferedWriter writer;
    
    //ファイルを開いて書き込み準備
    public CodeWriter(String fileName) {
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            System.out.println("ファイルを開けませんでした："+ e.getMessage());
        }
    }

// 1行書く
public void writeLine(String line) {
    try {
        writer.write(line);
        writer.newLine();
    } catch (IOException e) {
        System.out.println("書き込みエラー" + e.getMessage());
    }
}

    //書き終えたら閉じる
public void close() {
    try {
        writer.close();
    } catch (IOException e) {
        System.out.println("クローズに失敗" + e.getMessage());
    }
}
public void writeInit() {
    VMConverter.writeInit(this);
}
}