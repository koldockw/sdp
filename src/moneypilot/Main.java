package moneypilot;
import javax.swing.*;
import java.nio.file.*;
public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            try {
                for(UIManager.LookAndFeelInfo info:UIManager.getInstalledLookAndFeels())
                    if(info.getName().equals("Nimbus")) UIManager.setLookAndFeel(info.getClassName());
                Path path=args.length>0?Path.of(args[0]):Path.of(System.getProperty("user.home"),".moneypilot","data.properties");
                MoneyPilotFacade finance=new MoneyPilotFacade(new FileRepository(path));
                JFrame window=new JFrame("MoneyPilot - SE-2509");window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setContentPane(new DashboardPanel(finance));window.setSize(1240,800);window.setMinimumSize(new java.awt.Dimension(1180,760));window.setLocationRelativeTo(null);window.setVisible(true);
            } catch(Exception e) { JOptionPane.showMessageDialog(null,e.getMessage(),"MoneyPilot could not start",JOptionPane.ERROR_MESSAGE); }
        });
    }
}
