package moneypilot;
import java.io.IOException;
import java.util.*;
public final class Csv {
    private Csv() {}
    public static String quote(String value) { return "\""+value.replace("\"", "\"\"")+"\""; }
    public static List<String> split(String line) throws IOException {
        List<String> out=new ArrayList<>(); StringBuilder field=new StringBuilder();
        boolean quoted=false, closed=false;
        for(int i=0;i<line.length();i++) {
            char c=line.charAt(i);
            if(quoted) {
                if(c=='"') {
                    if(i+1<line.length() && line.charAt(i+1)=='"') { field.append('"'); i++; }
                    else { quoted=false; closed=true; }
                } else field.append(c);
            } else if(c==';') { out.add(field.toString()); field.setLength(0); closed=false; }
            else if(c=='"' && field.length()==0 && !closed) quoted=true;
            else if(closed || c=='"') throw new IOException("Malformed CSV quoting");
            else field.append(c);
        }
        if(quoted) throw new IOException("Unclosed CSV quote");
        out.add(field.toString()); return out;
    }
}
