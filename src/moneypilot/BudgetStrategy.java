package moneypilot;
import java.math.BigDecimal;

public interface BudgetStrategy {
    String name();
    BigDecimal warningLimit(BigDecimal monthlyBudget);
    // Strategy: меняется порог предупреждения, остальной код остаётся тем же.
    class Balanced implements BudgetStrategy {
        public String name() { return "Balanced"; }
        public BigDecimal warningLimit(BigDecimal budget) { return budget; }
    }
    class Cautious implements BudgetStrategy {
        public String name() { return "Cautious"; }
        public BigDecimal warningLimit(BigDecimal budget) { return budget.multiply(new BigDecimal("0.80")); }
    }
    static BudgetStrategy from(String name) {
        return switch(name) {
            case "Balanced" -> new Balanced();
            case "Cautious" -> new Cautious();
            default -> throw new IllegalArgumentException("Unknown budget strategy");
        };
    }
}
