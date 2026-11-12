package moneypilot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class Transaction {
    public enum Type { INCOME, EXPENSE }
    public static final List<String> CATEGORIES = List.of("Food", "Transport", "Housing", "Shopping", "Entertainment", "Health", "Education", "Salary", "Other");
    private final UUID id;
    private final LocalDate date;
    private final Type type;
    private final String category, note;
    private final BigDecimal amount;
    private Transaction(Builder b) {
        id = Objects.requireNonNull(b.id, "ID is required");
        date = Objects.requireNonNull(b.date, "Date is required");
        type = Objects.requireNonNull(b.type, "Type is required");
        if (!CATEGORIES.contains(b.category)) throw new IllegalArgumentException("Choose a valid category");
        category = b.category;
        amount = money(b.amount);
        if (amount.signum() <= 0 || amount.compareTo(new BigDecimal("999999999999.99")) > 0)
            throw new IllegalArgumentException("Amount must be between 0.01 and 999999999999.99");
        note = Objects.requireNonNull(b.note).strip();
        if (note.length() > 180 || note.contains("\n") || note.contains("\r"))
            throw new IllegalArgumentException("Note must be one line, up to 180 characters");
    }
    public static BigDecimal money(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Enter an amount");
        try { return value.setScale(2, java.math.RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Use at most two decimal places"); }
    }
    public UUID id() { return id; }
    public LocalDate date() { return date; }
    public Type type() { return type; }
    public String category() { return category; }
    public String note() { return note; }
    public BigDecimal amount() { return amount; }

    // Builder: собираю операцию по полям, чтобы не путать аргументы конструктора.
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private LocalDate date = LocalDate.now();
        private Type type;
        private String category = "Other", note = "";
        private BigDecimal amount;
        public Builder id(UUID v) { id=v; return this; }
        public Builder date(LocalDate v) { date=v; return this; }
        public Builder type(Type v) { type=v; return this; }
        public Builder category(String v) { category=v; return this; }
        public Builder note(String v) { note=v; return this; }
        public Builder amount(BigDecimal v) { amount=v; return this; }
        public Transaction build() { return new Transaction(this); }
    }
}
