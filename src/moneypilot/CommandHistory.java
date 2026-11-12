package moneypilot;
import java.util.*;
public final class CommandHistory {
    public record Entry(TransactionCommand command, List<Transaction> before) {}
    private final Deque<Entry> history=new ArrayDeque<>();
    // Command: храню команду и список до её выполнения, чтобы сделать undo.
    public List<Transaction> prepare(TransactionCommand command,List<Transaction> current) {
        List<Transaction> next=new ArrayList<>(current); command.apply(next); return List.copyOf(next);
    }
    public void remember(TransactionCommand command,List<Transaction> before) { history.push(new Entry(command,List.copyOf(before))); }
    public boolean canUndo() { return !history.isEmpty(); }
    public Entry peek() { if(!canUndo()) throw new IllegalStateException("Nothing to undo"); return history.peek(); }
    public void pop() { history.pop(); }
    public String label() { return canUndo()?history.peek().command().label():"Nothing to undo"; }
}
