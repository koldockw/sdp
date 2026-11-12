package moneypilot;
import java.util.*;
public interface TransactionCommand {
    void apply(List<Transaction> transactions);
    String label();

    record Add(Transaction transaction) implements TransactionCommand {
        public void apply(List<Transaction> list) {
            if(list.stream().anyMatch(t->t.id().equals(transaction.id()))) throw new IllegalArgumentException("Duplicate transaction");
            list.add(transaction);
        }
        public String label() { return "Add transaction"; }
    }
    record Delete(UUID id) implements TransactionCommand {
        public void apply(List<Transaction> list) {
            if(!list.removeIf(t->t.id().equals(id))) throw new IllegalArgumentException("Transaction not found");
        }
        public String label() { return "Delete transaction"; }
    }
    record Edit(Transaction transaction) implements TransactionCommand {
        public void apply(List<Transaction> list) {
            for(int i=0;i<list.size();i++) if(list.get(i).id().equals(transaction.id())) { list.set(i,transaction); return; }
            throw new IllegalArgumentException("Transaction not found");
        }
        public String label() { return "Edit transaction"; }
    }
    record ImportBatch(List<Transaction> batch) implements TransactionCommand {
        public ImportBatch { batch=List.copyOf(batch); }
        public void apply(List<Transaction> list) { for(Transaction t:batch) new Add(t).apply(list); }
        public String label() { return "Import "+batch.size()+" transactions"; }
    }
}
