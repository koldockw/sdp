package moneypilot;
import java.math.BigDecimal;
import java.util.*;
public final class HistoryLimitTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    static Transaction transaction(String amount) {
        return new Transaction.Builder().type(Transaction.Type.EXPENSE).amount(new BigDecimal(amount)).build();
    }
    public static void main(String[] args) {
        CommandHistory h=new CommandHistory(2);
        Transaction first=transaction("1"),second=transaction("2"),third=transaction("3");
        var a=new TransactionCommand.Add(first);var b=new TransactionCommand.Add(second);var c=new TransactionCommand.Add(third);
        List<Transaction> before=new ArrayList<>();
        h.remember(a,before);before.add(first);
        check(h.peek().before().isEmpty()); // History owns a copy.
        h.remember(b,List.of(first));h.remember(c,List.of(first,second));
        check(h.peek().command()==c);h.pop();check(h.peek().command()==b);h.pop();
        check(!h.canUndo()); // The oldest action was evicted.
        boolean rejected=false;try{new CommandHistory(0);}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        CommandHistory defaults=new CommandHistory();
        for(int i=0;i<101;i++) defaults.remember(a,List.of());
        int count=0;while(defaults.canUndo()){defaults.pop();count++;}check(count==100);
        CommandHistory one=new CommandHistory(1);one.remember(a,List.of());one.remember(b,List.of(first));
        check(one.peek().command()==b);one.pop();check(!one.canUndo());
        System.out.println("PASS: history eviction, LIFO order, snapshot isolation, invalid limit, default limit, single entry limit");
    }
}
