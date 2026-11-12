package moneypilot;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class DemoData {
    private DemoData() {}
    public static List<Transaction> create(YearMonth month) {
        List<Transaction> list=new ArrayList<>();
        list.add(tx(month,1,Transaction.Type.INCOME,"Salary","180000","Part-time work"));
        list.add(tx(month,2,Transaction.Type.EXPENSE,"Housing","45000","Room rent"));
        list.add(tx(month,3,Transaction.Type.EXPENSE,"Food","12400","Groceries"));
        list.add(tx(month,4,Transaction.Type.EXPENSE,"Transport","3200","Bus and taxi"));
        list.add(tx(month,5,Transaction.Type.EXPENSE,"Education","6500","Course materials"));
        list.add(tx(month,6,Transaction.Type.EXPENSE,"Entertainment","7800","Cinema with friends"));
        list.add(tx(month,7,Transaction.Type.EXPENSE,"Food","5100","Lunch"));
        return list;
    }
    private static Transaction tx(YearMonth m,int d,Transaction.Type type,String category,String amount,String note) {
        return new Transaction.Builder().date(m.atDay(d)).type(type).category(category).amount(new BigDecimal(amount)).note(note).build();
    }
}
