package moneypilot;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.swing.*;

public final class ProjectTest {
    private static int checks;
    static void check(boolean ok,String label) { if(!ok)throw new AssertionError(label);checks++; }
    static void fails(Throwing fn,String label) throws Exception { try{fn.run();}catch(Exception e){checks++;return;}throw new AssertionError(label); }
    interface Throwing {void run() throws Exception;}
    static Transaction tx(String amount,LocalDate date) {return new Transaction.Builder().date(date).type(Transaction.Type.EXPENSE).category("Food").amount(new BigDecimal(amount)).note("Lunch; \"test\"").build();}
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("moneypilot-test-");
        YearMonth month=YearMonth.of(2026,11);
        MoneyPilotFacade f=new MoneyPilotFacade(new FileRepository(dir.resolve("data.properties")));
        fails(()->tx("-1",month.atDay(1)),"negative amount");
        fails(()->tx("1.001",month.atDay(1)),"fractional cent");
        fails(()->new Transaction.Builder().amount(BigDecimal.ONE).build(),"missing type");
        fails(()->tx("0",month.atDay(1)),"zero amount");
        fails(()->new Transaction.Builder().type(Transaction.Type.INCOME).category("invalid").amount(BigDecimal.ONE).build(),"category validation");
        int[] events={0};FinanceListener listener=()->events[0]++;f.events().subscribe(listener);
        f.execute(new TransactionCommand.ImportBatch(DemoData.create(month)));
        check(f.summary(month).income().compareTo(new BigDecimal("180000"))==0,"income");
        check(f.summary(month).expense().compareTo(new BigDecimal("80000"))==0,"expense");
        check(f.summary(month).balance().compareTo(new BigDecimal("100000"))==0,"balance");
        check(f.categories(month).get("Food").compareTo(new BigDecimal("17500"))==0,"category totals");
        check(events[0]==1,"observer");
        BudgetAlert alert=new BudgetAlert(f,()->month);f.events().subscribe(alert);
        check(alert.message().startsWith("On track"),"balanced strategy");
        f.settings(new BigDecimal("100000"),"Cautious");
        check(alert.message().contains("threshold reached"),"cautious strategy");
        Transaction t=tx("0.10",month.atDay(12)); f.execute(new TransactionCommand.Add(t));
        f.execute(new TransactionCommand.Add(tx("0.20",month.atDay(12))));
        check(f.summary(month).expense().compareTo(new BigDecimal("80000.30"))==0,"decimal precision");
        f.undo();f.undo();check(f.all().size()==7,"undo add");
        Transaction old=f.all().get(1);
        Transaction edited=new Transaction.Builder().id(old.id()).date(old.date()).type(old.type()).category(old.category()).note("Edit").amount(new BigDecimal("46000")).build();
        f.execute(new TransactionCommand.Edit(edited));check(f.summary(month).expense().intValue()==81000,"edit");
        f.undo();check(f.summary(month).expense().intValue()==80000,"undo edit");
        f.execute(new TransactionCommand.Delete(old.id()));check(f.all().size()==6,"delete");f.undo();
        check(f.all().get(1).id().equals(old.id()),"undo restores order and identity");
        f.execute(new TransactionCommand.Add(tx("2.00",month.minusMonths(1).atDay(1))));
        check(f.summary(month).expense().intValue()==80000,"month isolation");f.undo();
        f.execute(new TransactionCommand.Add(tx("3.00",month.atDay(12))));
        Path csv=dir.resolve("export.csv");f.exportMonth(csv,month);
        List<Transaction> imported=new LegacyBankAdapter(new LegacyBankCsv()).read(csv);
        check(imported.size()==8,"adapter export roundtrip count");
        check(imported.stream().anyMatch(x->x.note().equals("Lunch; \"test\"")),"CSV quote roundtrip");
        check(imported.stream().filter(x->x.type()==Transaction.Type.EXPENSE).map(Transaction::amount).reduce(BigDecimal.ZERO,BigDecimal::add).intValue()==80003,"CSV sign conversion");
        Path bad=dir.resolve("bad.csv");Files.writeString(bad,"date;amount;category;note\n2026-11-01;-10;Food;OK\n2026-99-01;-20;Food;Bad\n");
        int count=f.all().size();fails(()->f.importFile(bad,new LegacyBankAdapter(new LegacyBankCsv())),"invalid import");check(f.all().size()==count,"import atomicity");
        MoneyPilotFacade restored=new MoneyPilotFacade(new FileRepository(dir.resolve("data.properties")));
        check(restored.all().size()==f.all().size(),"disk reload");check(restored.strategy().name().equals("Cautious"),"settings reload");
        check(!restored.history().canUndo(),"history intentionally session only");
        fails(()->f.settings(BigDecimal.ZERO,"Balanced"),"zero budget");
        Files.writeString(dir.resolve("broken.properties"),"count=oops");
        fails(()->new MoneyPilotFacade(new FileRepository(dir.resolve("broken.properties"))),"corrupt data rejected");
        Repository broken=new Repository(){public State load(){return new State(List.of(),new BigDecimal("100"),"Balanced");}public void save(State s)throws IOException{throw new IOException("Disk full");}};
        MoneyPilotFacade blocked=new MoneyPilotFacade(broken);
        fails(()->blocked.execute(new TransactionCommand.Add(t)),"save failure");
        check(blocked.all().isEmpty()&&!blocked.history().canUndo(),"failed save preserves state and history");
        f.events().unsubscribe(listener);int previous=events[0];f.settings(new BigDecimal("100000"),"Balanced");check(previous==events[0],"unsubscribe");
        SwingUtilities.invokeAndWait(()->{
            DashboardPanel panel=new DashboardPanel(f);panel.showMonth(month);check(panel.visibleRowCount()==8,"dashboard initial rows");
            panel.filterForTest("lunch","Food");check(panel.visibleRowCount()==2,"search and category filter");
            panel.filterForTest("","All categories");panel.clickUndoForTest();check(panel.visibleRowCount()==7,"undo button updates dashboard via observer");
        });
        System.out.println("PASS: "+checks+" checks. Builder, Strategy, Observer, Command, Adapter, Facade, persistence and Swing dashboard.");
    }
}
