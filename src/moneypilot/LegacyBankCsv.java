package moneypilot;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class LegacyBankCsv {
    public record Row(String date,String signedAmount,String category,String description) {}
    // У старого формата другие поля и сумма со знаком.
    public List<Row> loadRows(Path path) throws IOException {
        List<String> lines=Files.readAllLines(path);
        if(lines.isEmpty() || !lines.get(0).replace("\uFEFF", "").equals("date;amount;category;note"))
            throw new IOException("CSV header must be date;amount;category;note");
        List<Row> rows=new ArrayList<>();
        for(int i=1;i<lines.size();i++) {
            if(lines.get(i).isBlank()) continue;
            List<String> fields=Csv.split(lines.get(i));
            if(fields.size()!=4) throw new IOException("CSV line "+(i+1)+": expected four columns");
            rows.add(new Row(fields.get(0),fields.get(1),fields.get(2),fields.get(3)));
        }
        return rows;
    }
}
