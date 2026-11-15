package moneypilot;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class CsvLineErrorTest {
    static int checks;
    static void rejects(String rows,String expected,Path dir) throws Exception {
        Path path=dir.resolve("sample.csv");Files.writeString(path,"date;amount;category;note\n"+rows);
        try {new LegacyBankAdapter(new LegacyBankCsv()).read(path);throw new AssertionError("Expected invalid input");}
        catch(IOException e){if(!e.getMessage().startsWith(expected))throw new AssertionError(e.getMessage());checks++;}
    }
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("csv-lines-");
        try {
            String good="2026-11-01;-10;Food;Lunch\n";
            rejects(good+"\n2026-99-01;-20;Food;Bad\n","CSV line 4:",dir);
            rejects("2026-11-01;abc;Food;Bad\n","CSV line 2:",dir);
            rejects("2026-11-01;-1;Unknown;Bad\n","CSV line 2:",dir);
            rejects("2026-11-01;0;Food;Bad\n","CSV line 2:",dir);
            rejects("2026-11-01;-1;Food;\"Unclosed\n","CSV line 2:",dir);
            rejects("2026-11-01;-1;Food\n","CSV line 2:",dir);
            Path valid=dir.resolve("valid.csv");Files.writeString(valid,"date;amount;category;note\n\n"+good);
            List<LegacyBankCsv.Row> rows=new LegacyBankCsv().loadRows(valid);
            if(rows.get(0).lineNumber()!=3)throw new AssertionError();checks++;
            if(new LegacyBankAdapter(new LegacyBankCsv()).read(valid).size()!=1)throw new AssertionError();checks++;
            System.out.println("PASS: "+checks+" CSV line-number checks");
        } finally {try(var files=Files.list(dir)){for(Path p:files.toList())Files.delete(p);}Files.delete(dir);}
    }
}
