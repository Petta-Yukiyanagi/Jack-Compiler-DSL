package Jack_Project.Jack_Buildsystem.JackCompiler.core.Paser.codewriter.XML;
import java.io.IOException;
import java.nio.file.*;

/**
 * XML形式で出力するWriter実装
 */
public class XMLWriter implements IXMLWriter {
    private StringBuilder output;
    
    public XMLWriter() {
        this.output = new StringBuilder();
    }
    
    @Override
    public void writeClassStart() {
        writeLine("<class>");
    }
    
    @Override
    public void writeClassEnd() {
        writeLine("</class>");
    }
    
    @Override
    public void writeKeyword(String keyword) {
        writeLine("<keyword>" + keyword + "</keyword>");
    }
    
    @Override
    public void writeIdentifier(String name) {
        writeLine("<identifier>" + name + "</identifier>");
    }
    
    @Override
    public void writeSymbol(String symbol) {
        String escaped = escape(symbol);
        writeLine("<symbol>" + escaped + "</symbol>");
    }
    
    @Override
    public void writeIntConstant(String value) {
        writeLine("<integerConstant>" + value + "</integerConstant>");
    }
    
    @Override
    public void writeStringConstant(String value) {
        writeLine("<stringConstant>" + value + "</stringConstant>");
    }
    
    @Override
    public void writeTagStart(String tag) {
        writeLine("<" + tag + ">");
    }
    
    @Override
    public void writeTagEnd(String tag) {
        writeLine("</" + tag + ">");
    }
    
    @Override
    public void saveToFile(String filePath) throws IOException {
        Files.writeString(Paths.get(filePath), output.toString());
    }
    
    @Override
    public String getOutput() {
        return output.toString();
    }
    
    // 内部ユーティリティ
    private void writeLine(String line) {
        output.append(line).append("\n");
    }
    
    private String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}