package moneypilot;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;

public final class DashboardPanel extends JPanel implements FinanceListener {
    private static final long serialVersionUID=1L;
    private static final Color BG=new Color(244,247,251), INK=new Color(22,39,60), GREEN=new Color(20,130,105), MUTED=new Color(98,115,134);
    private final MoneyPilotFacade finance;
    private final JTextField monthField=new JTextField(YearMonth.now().toString(),7), search=new JTextField(14);
    private final JComboBox<String> categoryFilter=new JComboBox<>();
    private YearMonth month=YearMonth.now();
    private final DefaultTableModel model=new DefaultTableModel(new String[]{"Date","Type","Category","Note","Amount (KZT)"},0) {
        private static final long serialVersionUID=1L;
        public boolean isCellEditable(int r,int c) { return false; }
        public Class<?> getColumnClass(int c) { return c==4?BigDecimal.class:String.class; }
    };
    private final JTable table=new JTable(model);
    private final JLabel income=new JLabel(),expense=new JLabel(),balance=new JLabel(),budgetText=new JLabel(),alertText=new JLabel();
    private final JProgressBar progress=new JProgressBar(0,100);
    private final JButton undo=button("Undo last action");
    private final Chart chart=new Chart();
    private final BudgetAlert alert;
    private List<Transaction> visible=List.of();

    public DashboardPanel(MoneyPilotFacade finance) {
        this.finance=finance;
        setLayout(new BorderLayout(22,22)); setBackground(BG); setBorder(new EmptyBorder(24,28,20,28));
        JPanel header=clear(new BorderLayout());
        JLabel title=label("MoneyPilot",30,INK); header.add(title,BorderLayout.WEST);
        header.add(label("SE-2509  /  Personal finance assistant",14,MUTED),BorderLayout.EAST);
        add(header,BorderLayout.NORTH);
        JPanel center=clear(new BorderLayout(18,18)); add(center,BorderLayout.CENTER);
        JPanel top=clear(new GridLayout(1,3,18,0));
        top.add(metric("MONTHLY INCOME",income,GREEN)); top.add(metric("MONTHLY EXPENSES",expense,new Color(202,78,77))); top.add(metric("MONTHLY NET",balance,INK));
        center.add(top,BorderLayout.NORTH);
        JPanel main=clear(new BorderLayout(16,16)); center.add(main,BorderLayout.CENTER);
        JPanel toolbar=clear(new FlowLayout(FlowLayout.LEFT,8,0));
        toolbar.add(new JLabel("Month")); toolbar.add(monthField);
        JButton apply=button("Apply"); toolbar.add(apply); apply.addActionListener(e->applyMonth()); monthField.addActionListener(e->applyMonth());
        categoryFilter.addItem("All categories"); for(String c:Transaction.CATEGORIES) categoryFilter.addItem(c);
        toolbar.add(categoryFilter); toolbar.add(new JLabel("Search")); toolbar.add(search);
        main.add(toolbar,BorderLayout.NORTH);
        table.setRowHeight(36); table.setFont(new Font("SansSerif",Font.PLAIN,14)); table.setForeground(INK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setAutoCreateRowSorter(true);
        table.setShowVerticalLines(false); table.setGridColor(new Color(232,237,243));
        table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13)); table.getTableHeader().setPreferredSize(new Dimension(100,36));
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        JScrollPane scroll=new JScrollPane(table); scroll.setColumnHeaderView(table.getTableHeader()); scroll.setBorder(new LineBorder(new Color(226,232,240))); main.add(scroll,BorderLayout.CENTER);
        JPanel buttons=clear(new FlowLayout(FlowLayout.LEFT,7,0));
        JButton add=button("+ Add transaction"),edit=button("Edit selected"),delete=button("Delete selected");
        buttons.add(add);buttons.add(edit);buttons.add(delete);buttons.add(undo);
        add.addActionListener(e->editDialog(null)); edit.addActionListener(e->{ Transaction t=selected(); if(t!=null)editDialog(t); });
        delete.addActionListener(e->{ Transaction t=selected(); if(t!=null && JOptionPane.showConfirmDialog(this,"Delete this transaction?","Delete",JOptionPane.YES_NO_OPTION)==0)run(()->finance.execute(new TransactionCommand.Delete(t.id()))); });
        undo.addActionListener(e->run(finance::undo)); main.add(buttons,BorderLayout.SOUTH);
        JPanel side=new JPanel(); side.setBackground(Color.WHITE); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS)); side.setBorder(new EmptyBorder(18,18,18,18)); side.setPreferredSize(new Dimension(295,400));
        side.add(label("Monthly budget",19,INK)); side.add(Box.createVerticalStrut(12));side.add(budgetText);side.add(Box.createVerticalStrut(10));
        progress.setStringPainted(true);progress.setForeground(GREEN); progress.setMaximumSize(new Dimension(270,24));side.add(progress);
        side.add(Box.createVerticalStrut(10)); alertText.setFont(new Font("SansSerif",Font.PLAIN,12));alertText.setMaximumSize(new Dimension(260,38));side.add(alertText);
        JButton settings=button("Budget settings");side.add(Box.createVerticalStrut(12));side.add(settings);settings.addActionListener(e->settingsDialog());
        side.add(Box.createVerticalStrut(14));side.add(label("Expenses by category",18,INK));side.add(Box.createVerticalStrut(12));side.add(chart);for(Component c:side.getComponents()) if(c instanceof JComponent) ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);center.add(side,BorderLayout.EAST);
        JPanel footer=clear(new BorderLayout()); JPanel tools=clear(new FlowLayout(FlowLayout.LEFT,8,0));
        JButton load=button("Load demo"),imp=button("Import CSV"),exp=button("Export month"); tools.add(load);tools.add(imp);tools.add(exp);
        load.addActionListener(e->{ if(JOptionPane.showConfirmDialog(this,"Append 7 fictional demo records to "+month+"?","Demo data",JOptionPane.YES_NO_OPTION)==0)run(()->finance.execute(new TransactionCommand.ImportBatch(DemoData.create(month)))); });
        imp.addActionListener(e->fileDialog(true)); exp.addActionListener(e->fileDialog(false));
        footer.add(tools,BorderLayout.WEST); footer.add(label("Local data  /  KZT  /  No bank connection",12,MUTED),BorderLayout.EAST); add(footer,BorderLayout.SOUTH);
        alert=new BudgetAlert(finance,()->month); finance.events().subscribe(alert);finance.events().subscribe(this);
        categoryFilter.addActionListener(e->refreshTable());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){refreshTable();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){refreshTable();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){refreshTable();}
        });
        onChanged();
    }
    private static JPanel clear(LayoutManager layout) { JPanel p=new JPanel(layout);p.setOpaque(false);return p; }
    private static JLabel label(String s,int size,Color color) { JLabel l=new JLabel(s);l.setFont(new Font("SansSerif",Font.BOLD,size));l.setForeground(color);return l; }
    private static JButton button(String text) { JButton b=new JButton(text);b.setFocusPainted(false);b.setFont(new Font("SansSerif",Font.PLAIN,13));b.setMargin(new Insets(8,10,8,10));return b; }
    private JPanel metric(String title,JLabel value,Color color) {
        JPanel p=new JPanel(new BorderLayout(0,8));p.setBackground(Color.WHITE);p.setBorder(new EmptyBorder(17,20,17,20));
        p.add(label(title,12,MUTED),BorderLayout.NORTH);value.setFont(new Font("SansSerif",Font.BOLD,25));value.setForeground(color);p.add(value);return p;
    }
    static String money(BigDecimal v) { return String.format(Locale.US,"%,.2f KZT",v); }
    private void applyMonth() {
        try { month=YearMonth.parse(monthField.getText().strip());alert.onChanged();onChanged(); }
        catch(RuntimeException ex) { error("Use month format YYYY-MM"); }
    }
    public void onChanged() {
        MoneyPilotFacade.Summary s=finance.summary(month);income.setText(money(s.income()));expense.setText(money(s.expense()));balance.setText(money(s.balance()));
        budgetText.setText(money(finance.budget())+" / "+finance.strategy().name());
        int percent=s.expense().multiply(new BigDecimal("100")).divide(finance.budget(),0,java.math.RoundingMode.DOWN).min(new BigDecimal("100")).intValue();
        progress.setValue(percent);progress.setString(percent+"% used");
        // Текст в HTML нужен только для переноса длинного предупреждения.
        alertText.setText("<html><body style='width:245px'>"+alert.message()+"</body></html>");
        undo.setEnabled(finance.history().canUndo());undo.setToolTipText(finance.history().label());
        refreshTable();chart.repaint();
    }
    private void refreshTable() {
        if(finance==null)return;
        String q=search.getText().strip().toLowerCase(Locale.ROOT),cat=(String)categoryFilter.getSelectedItem();
        visible=finance.month(month).stream().filter(t->"All categories".equals(cat)||t.category().equals(cat))
            .filter(t->(t.note()+" "+t.category()+" "+t.type()).toLowerCase(Locale.ROOT).contains(q)).toList();
        model.setRowCount(0);
        for(Transaction t:visible) model.addRow(new Object[]{t.date().toString(),t.type().name(),t.category(),t.note(),t.type()==Transaction.Type.EXPENSE?t.amount().negate():t.amount()});
    }
    private Transaction selected() {
        int row=table.getSelectedRow();if(row<0){error("Select a transaction first");return null;}
        return visible.get(table.convertRowIndexToModel(row));
    }
    private void editDialog(Transaction old) {
        JTextField date=new JTextField(old==null?month.atDay(Math.min(LocalDate.now().getDayOfMonth(),month.lengthOfMonth())).toString():old.date().toString());
        JTextField amount=new JTextField(old==null?"":old.amount().toPlainString()),note=new JTextField(old==null?"":old.note());
        JComboBox<Transaction.Type> type=new JComboBox<>(Transaction.Type.values());type.setSelectedItem(old==null?Transaction.Type.EXPENSE:old.type());
        JComboBox<String> category=new JComboBox<>(Transaction.CATEGORIES.toArray(String[]::new));category.setSelectedItem(old==null?"Food":old.category());
        JPanel p=new JPanel(new GridLayout(0,2,10,10));p.add(new JLabel("Date (YYYY-MM-DD)"));p.add(date);p.add(new JLabel("Type"));p.add(type);p.add(new JLabel("Category"));p.add(category);p.add(new JLabel("Amount, positive KZT"));p.add(amount);p.add(new JLabel("Note"));p.add(note);
        while(JOptionPane.showConfirmDialog(this,p,old==null?"Add transaction":"Edit transaction",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                Transaction.Builder b=new Transaction.Builder().date(LocalDate.parse(date.getText().strip())).type((Transaction.Type)type.getSelectedItem()).category((String)category.getSelectedItem()).amount(new BigDecimal(amount.getText().strip())).note(note.getText());
                if(old!=null)b.id(old.id());Transaction t=b.build();
                finance.execute(old==null?new TransactionCommand.Add(t):new TransactionCommand.Edit(t));return;
            } catch(Exception e) { error(e.getMessage()); }
        }
    }
    private void settingsDialog() {
        JTextField amount=new JTextField(finance.budget().toPlainString());JComboBox<String> policy=new JComboBox<>(new String[]{"Balanced","Cautious"});policy.setSelectedItem(finance.strategy().name());
        JPanel p=new JPanel(new GridLayout(0,2,10,10));p.add(new JLabel("Monthly limit (all months)"));p.add(amount);p.add(new JLabel("Warning policy"));p.add(policy);
        if(JOptionPane.showConfirmDialog(this,p,"Budget: Balanced 100%, Cautious 80%",JOptionPane.OK_CANCEL_OPTION)==0)
            run(()->finance.settings(new BigDecimal(amount.getText().strip()),(String)policy.getSelectedItem()));
    }
    private void fileDialog(boolean importing) {
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File(importing?"samples/bank-demo.csv":"moneypilot-"+month+".csv"));
        int result=importing?chooser.showOpenDialog(this):chooser.showSaveDialog(this);
        if(result!=JFileChooser.APPROVE_OPTION)return;
        Path path=chooser.getSelectedFile().toPath();
        if(!importing && Files.exists(path) && JOptionPane.showConfirmDialog(this,"Replace this export file?","Export",JOptionPane.YES_NO_OPTION)!=0)return;
        run(()->{ if(importing)finance.importFile(path,new LegacyBankAdapter(new LegacyBankCsv()));else finance.exportMonth(path,month); });
    }
    @FunctionalInterface private interface Work { void run() throws Exception; }
    private void run(Work action) { try { action.run(); } catch(Exception e){error(e.getMessage());} }
    private void error(String message) { JOptionPane.showMessageDialog(this,message,"MoneyPilot",JOptionPane.ERROR_MESSAGE); }
    public void showMonth(YearMonth value) { month=value;monthField.setText(value.toString());alert.onChanged();onChanged(); }
    public int visibleRowCount() { return model.getRowCount(); }
    public void filterForTest(String text,String category) { categoryFilter.setSelectedItem(category);search.setText(text); }
    public void clickUndoForTest() { undo.doClick(); }
    private final class Chart extends JPanel {
        private static final long serialVersionUID=1L;
        private final Color[] colors={new Color(20,130,105),new Color(65,110,190),new Color(215,158,54),new Color(142,99,183),new Color(201,91,113),new Color(72,155,179),new Color(130,140,155),new Color(172,130,70),new Color(78,105,115)};
        Chart() { setBackground(Color.WHITE);setPreferredSize(new Dimension(255,280));setMaximumSize(new Dimension(270,300)); }
        protected void paintComponent(Graphics raw) {
            super.paintComponent(raw);Graphics2D g=(Graphics2D)raw.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Map<String,BigDecimal> map=finance.categories(month);BigDecimal total=finance.summary(month).expense();
            if(total.signum()==0){g.setColor(MUTED);g.drawString("No expenses this month",10,30);g.dispose();return;}
            int i=0;double angle=90;
            for(var entry:map.entrySet()) {
                double extent=entry.getValue().divide(total,10,java.math.RoundingMode.HALF_UP).doubleValue()*360;
                g.setColor(colors[i%colors.length]);g.fill(new java.awt.geom.Arc2D.Double(52,0,100,100,angle,-extent,java.awt.geom.Arc2D.PIE));angle-=extent;i++;
            }
            g.setColor(Color.WHITE);g.fillOval(76,24,52,52);int y=120;i=0;g.setFont(new Font("SansSerif",Font.PLAIN,12));
            for(var entry:map.entrySet()) {g.setColor(colors[i++%colors.length]);g.fillRoundRect(2,y-10,9,9,3,3);g.setColor(INK);g.drawString(entry.getKey()+"  "+String.format(Locale.US,"%,.0f",entry.getValue()),18,y);y+=17;}
            g.dispose();
        }
    }
}
