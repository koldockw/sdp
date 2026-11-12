package moneypilot;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
public interface Repository {
    record State(List<Transaction> transactions, BigDecimal budget, String policy) {
        public State { transactions = List.copyOf(transactions); }
    }
    State load() throws IOException;
    void save(State state) throws IOException;
}
