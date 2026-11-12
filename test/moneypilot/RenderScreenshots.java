package moneypilot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.imageio.ImageIO;

public final class RenderScreenshots {
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
    static void save(DashboardPanel panel,Path path)throws Exception{
        panel.setSize(1240,800);layout(panel);
        BufferedImage image=new BufferedImage(1240,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);panel.printAll(g);g.dispose();ImageIO.write(image,"png",path.toFile());
    }
    public static void main(String[]args)throws Exception{
        Path dir=Path.of(args[0]);Files.createDirectories(dir);Path data=Files.createTempDirectory("moneypilot-render-").resolve("data.properties");
        SwingUtilities.invokeAndWait(()->{try{
            for(UIManager.LookAndFeelInfo info:UIManager.getInstalledLookAndFeels()) if(info.getName().equals("Nimbus")) UIManager.setLookAndFeel(info.getClassName());
            MoneyPilotFacade f=new MoneyPilotFacade(new FileRepository(data));YearMonth m=YearMonth.of(2026,11);
            f.execute(new TransactionCommand.ImportBatch(DemoData.create(m)));
            DashboardPanel p=new DashboardPanel(f);p.showMonth(m);save(p,dir.resolve("dashboard.png"));
            f.settings(new BigDecimal("100000"),"Cautious");save(p,dir.resolve("budget-warning.png"));
            p.filterForTest("","Food");save(p,dir.resolve("food-filter.png"));
        }catch(Exception e){throw new RuntimeException(e);}});
    }
}
