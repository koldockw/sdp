package moneypilot;
import java.util.*;
public final class EventBus {
    private final List<FinanceListener> listeners = new ArrayList<>();
    public void subscribe(FinanceListener listener) { listeners.add(listener); }
    public void unsubscribe(FinanceListener listener) { listeners.remove(listener); }
    // Observer: после сохранения сообщаю подписчикам, что данные изменились.
    public void publish() { for (FinanceListener l : List.copyOf(listeners)) l.onChanged(); }
}
