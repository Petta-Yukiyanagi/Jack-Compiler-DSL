package Jack_Project.Jack_Buildsystem.JackVMConverter.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser implements AutoCloseable {
    private BufferedReader reader; // ファイル読み込み用のBufferedReader
    private String currentCommand; // 現在の命令を保持する変数
    private String nextLine; // hasMoreLines() で一時保存する次の行

    //コンストラクタ：ファイル名を受け取り、読み込み準備
    public Parser(String fileName) {
        try{ 
            reader = new BufferedReader(new FileReader(fileName));  // ファイルを開く
        } catch (IOException e){
            System.out.println("ファイルのオープンに失敗しました：" + e.getMessage());
        }
    }

    //外から命令を取得するためのメソッド
    public String getCurrentCommand() {
        return currentCommand;
    }

    //リソースを閉じるメソッド
    public void close() {
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
            System.out.println("リソースのクローズに失敗しました：" + e.getMessage());
        }
    }

    //コメントを削除し、前後の空白を取り除く
    private String cleanLine(String line) {
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex);
        }
        return line.trim();
    }
    
    //次の行が存在するか確認するメソッド
    //hasMoreLines()は、次の行が存在するかどうかを確認し、存在する場合はその行をnextLineに保存します。
    public boolean hasMoreLines() {
        try {
            if (nextLine != null) {
                return true;
            }

            String line;
            while ((line = reader.readLine()) != null) {// 行を読み込む
                // コメントを削除し、前後の空白を取り除く
                line = cleanLine(line);
                if (!line.isEmpty()) {  // 空行でない場合
                    // 次の行を保存
                    nextLine = line;
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            System.out.println("読み込みの状態の確認に失敗しました：" + e.getMessage());
            return false;
        }
    
    
    }

    //コマンドの種類を表す
    public enum CommandType{
        C_ARITHMETIC,
        C_PUSH,
        C_POP,
        C_LABEL,
        C_GOTO,
        C_IF,
        C_FUNCTION,
        C_RETURN,
        C_CALL
    }

    //コマンドの種類を判定するメソッド
    public CommandType commandType(String command){
        if(command.startsWith("push")){
            return CommandType.C_PUSH;
        }else if (command.startsWith("pop")){
            return CommandType.C_POP;
        }else if (command.startsWith("label")){
            return CommandType.C_LABEL;
        }else if (command.startsWith("goto")){
            return CommandType.C_GOTO;
        }else if (command.startsWith("if")){
            return CommandType.C_IF;
        }else if (command.startsWith("function")){
            return CommandType.C_FUNCTION;
        }else if (command.startsWith("return")){
            return CommandType.C_RETURN;
        }else if (command.startsWith("call")){
            return CommandType.C_CALL;
        }else {
            return CommandType.C_ARITHMETIC; // 算術命令
        }
    }

    //現在のコマンドの種類を取得するメソッド
    public String arg1(){
        if (currentCommand == null) {
            throw new IllegalStateException("currentCommandは空です。 advance()を呼び出してから使用してください。");
        }
        
        CommandType type = commandType(currentCommand);
        String[] parts = currentCommand.split("\\s+");

        switch (type) {
            case C_ARITHMETIC:
                return parts[0]; // 算術命令の場合、最初の部分を返す
            case C_PUSH:
            case C_POP:
            case C_LABEL:
            case C_GOTO:
            case C_IF:
            case C_FUNCTION:
            case C_CALL:
                return parts[1]; // 他のコマンドの場合、2番目の部分を返す
            default:
                throw new IllegalStateException("不明なコマンドタイプです。");
        } 
    }

    //現在のコマンドの第2引数を取得するメソッド
    public int arg2(){
        if (currentCommand == null) {
            throw new IllegalStateException("advance()を呼び出してから使用してください。");
        }
        String[] parts = currentCommand.split("\\s+");
        CommandType type = commandType(currentCommand);
     
        switch (type) {
            case C_PUSH:
            case C_POP:
            case C_FUNCTION:
            case C_CALL:
                return Integer.parseInt(parts[2]); //第2引数は3つ目の単語

            default:
                throw new IllegalStateException("このコマンドタイプにはarg2()は使用できません。");
            }
        } 
        

    //次の行を取得し、currentCommandに設定するメソッド
    //advance()は、hasMoreLines()で確認した次の行をcurrentCommandに設定し、nextLineをクリアします。
    public void advance(){
        try {
            if (nextLine != null) {

                currentCommand = cleanLine(nextLine);
                nextLine = null;//消費したのでクリア
            } else {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = cleanLine(line);
                    if (!line.isEmpty()) {
                        currentCommand = line;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("advanece中に読み込みエラー：" + e.getMessage());
            currentCommand = null;
        }
        
    }
}
