package moneypilot;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.function.Supplier;
public final class BudgetAlert implements FinanceListener {
    private final MoneyPilotFacade finance;
    private final Supplier<YearMonth> month;
    private String message;
    public BudgetAlert(MoneyPilotFacade finance,Supplier<YearMonth> month) { this.finance=finance; this.month=month; onChanged(); }
    public void onChanged() {
        BigDecimal spent=finance.summary(month.get()).expense();
        BigDecimal threshold=finance.strategy().warningLimit(finance.budget());
        if(spent.compareTo(finance.budget())>0) message="Budget exceeded by "+spent.subtract(finance.budget()).toPlainString()+" KZT";
        else if(spent.compareTo(threshold)>=0) message="Warning: "+finance.strategy().name()+" threshold reached";
        else message="On track. Remaining budget: "+finance.budget().subtract(spent).toPlainString()+" KZT";
    }
    public String message() { return message; }
}
