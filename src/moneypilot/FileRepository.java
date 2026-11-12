package moneypilot;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public final class FileRepository implements Repository {
    private final Path file;
    public FileRepository(Path file) { this.file=file.toAbsolutePath(); }
    public State load() throws IOException {
        if (!Files.exists(file)) return new State(List.of(), new BigDecimal("100000.00"), "Balanced");
        Properties p=new Properties();
        try (Reader r=Files.newBufferedReader(file)) { p.load(r); }
        try {
            int size=Integer.parseInt(p.getProperty("count"));
            if(size<0 || size>100000) throw new IllegalArgumentException("Invalid record count");
            List<Transaction> list=new ArrayList<>();
            Set<UUID> ids=new HashSet<>();
            for(int i=0;i<size;i++) {
                String k="tx."+i+".";
                Transaction t=new Transaction.Builder().id(UUID.fromString(p.getProperty(k+"id")))
                    .date(LocalDate.parse(p.getProperty(k+"date"))).type(Transaction.Type.valueOf(p.getProperty(k+"type")))
                    .category(p.getProperty(k+"category")).amount(new BigDecimal(p.getProperty(k+"amount")))
                    .note(p.getProperty(k+"note", "")).build();
                if(!ids.add(t.id())) throw new IllegalArgumentException("Duplicate ID");
                list.add(t);
            }
            BigDecimal budget=MoneyPilotFacade.validBudget(new BigDecimal(p.getProperty("budget")));
            String policy=BudgetStrategy.from(p.getProperty("policy")).name();
            return new State(list,budget,policy);
        } catch (RuntimeException e) { throw new IOException("Cannot read saved data. The original file was kept: "+file, e); }
    }
    public void save(State state) throws IOException {
        Properties p=new Properties();
        p.setProperty("count", ""+state.transactions().size());
        p.setProperty("budget",state.budget().toPlainString()); p.setProperty("policy",state.policy());
        int i=0;
        for(Transaction t:state.transactions()) {
            String k="tx."+(i++)+".";
            p.setProperty(k+"id",t.id().toString()); p.setProperty(k+"date",t.date().toString());
            p.setProperty(k+"type",t.type().name()); p.setProperty(k+"category",t.category());
            p.setProperty(k+"amount",t.amount().toPlainString()); p.setProperty(k+"note",t.note());
        }
        Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),"moneypilot-", ".tmp");
        try {
            try(Writer w=Files.newBufferedWriter(temp)) { p.store(w,"MoneyPilot local data"); }
            // Сначала пишу временный файл. Так старые данные не пропадут при ошибке записи.
            try { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
