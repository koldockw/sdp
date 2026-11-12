package moneypilot;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.YearMonth;
import java.util.*;

public final class MoneyPilotFacade {
    public record Summary(BigDecimal income, BigDecimal expense) {
        public BigDecimal balance() { return income.subtract(expense); }
    }
    private final Repository repository;
    private final EventBus events=new EventBus();
    private final CommandHistory history=new CommandHistory();
    private List<Transaction> transactions;
    private BigDecimal budget;
    private BudgetStrategy strategy;
    public MoneyPilotFacade(Repository repository) throws IOException {
        this.repository=repository;
        Repository.State state=repository.load();
        transactions=state.transactions(); budget=validBudget(state.budget()); strategy=BudgetStrategy.from(state.policy());
    }
    public static BigDecimal validBudget(BigDecimal budget) {
        budget=Transaction.money(budget);
        if(budget.signum()<=0 || budget.compareTo(new BigDecimal("999999999999.99"))>0)
            throw new IllegalArgumentException("Budget must be between 0.01 and 999999999999.99");
        return budget;
    }
    public EventBus events() { return events; }
    public List<Transaction> all() { return transactions; }
    public BigDecimal budget() { return budget; }
    public BudgetStrategy strategy() { return strategy; }
    public CommandHistory history() { return history; }
    // Facade: окно вызывает один метод, а сохранение, история и уведомления уже здесь.
    public void execute(TransactionCommand command) throws IOException {
        List<Transaction> next=history.prepare(command,transactions);
        repository.save(new Repository.State(next,budget,strategy.name()));
        history.remember(command,transactions); transactions=next; events.publish();
    }
    public void undo() throws IOException {
        List<Transaction> before=history.peek().before();
        repository.save(new Repository.State(before,budget,strategy.name()));
        transactions=before; history.pop(); events.publish();
    }
    public void settings(BigDecimal value,String policy) throws IOException {
        BigDecimal next=validBudget(value); BudgetStrategy nextStrategy=BudgetStrategy.from(policy);
        repository.save(new Repository.State(transactions,next,nextStrategy.name()));
        budget=next; strategy=nextStrategy; events.publish();
    }
    public List<Transaction> month(YearMonth month) {
        return transactions.stream().filter(t->YearMonth.from(t.date()).equals(month))
            .sorted(Comparator.comparing(Transaction::date).reversed()).toList();
    }
    public Summary summary(YearMonth month) {
        BigDecimal income=BigDecimal.ZERO,expense=BigDecimal.ZERO;
        for(Transaction t:month(month)) {
            if(t.type()==Transaction.Type.INCOME) income=income.add(t.amount());
            else expense=expense.add(t.amount());
        }
        return new Summary(income,expense);
    }
    public Map<String,BigDecimal> categories(YearMonth month) {
        Map<String,BigDecimal> map=new LinkedHashMap<>();
        for(Transaction t:month(month)) if(t.type()==Transaction.Type.EXPENSE) map.merge(t.category(),t.amount(),BigDecimal::add);
        return map;
    }
    public void importFile(Path path,TransactionImporter importer) throws IOException {
        List<Transaction> batch=importer.read(path);
        if(batch.isEmpty()) throw new IOException("No transactions to import");
        execute(new TransactionCommand.ImportBatch(batch));
    }
    public void exportMonth(Path path,YearMonth month) throws IOException {
        StringBuilder csv=new StringBuilder("date;amount;category;note\n");
        for(Transaction t:month(month)) {
            BigDecimal signed=t.type()==Transaction.Type.EXPENSE?t.amount().negate():t.amount();
            csv.append(t.date()).append(';').append(signed.toPlainString()).append(';')
                .append(Csv.quote(t.category())).append(';').append(Csv.quote(t.note())).append('\n');
        }
        Files.writeString(path,csv.toString());
    }
}
