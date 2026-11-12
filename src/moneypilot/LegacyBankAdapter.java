package moneypilot;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
public final class LegacyBankAdapter implements TransactionImporter {
    private final LegacyBankCsv legacy;
    public LegacyBankAdapter(LegacyBankCsv legacy) { this.legacy=legacy; }
    public List<Transaction> read(Path path) throws IOException {
        List<Transaction> result=new ArrayList<>();
        // Adapter: перевожу строки старого CSV в наши Transaction.
        for(LegacyBankCsv.Row row:legacy.loadRows(path)) {
            try {
                BigDecimal signed=new BigDecimal(row.signedAmount());
                result.add(new Transaction.Builder().date(LocalDate.parse(row.date()))
                    .type(signed.signum()<0?Transaction.Type.EXPENSE:Transaction.Type.INCOME)
                    .amount(signed.abs()).category(row.category()).note(row.description()).build());
            } catch(RuntimeException e) { throw new IOException("Invalid CSV record: "+e.getMessage(),e); }
        }
        return List.copyOf(result);
    }
}
