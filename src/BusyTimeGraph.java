import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class BusyTimeGraph extends JComponent {

    private final Map<Integer, Integer> busyTime;

    public BusyTimeGraph(Map<Integer, Integer> busyTime) {
        this.busyTime = busyTime;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = this.getWidth() / 2;
        int h = this.getHeight() / 2;

        Graphics2D g1 = (Graphics2D) g;
        g1.setStroke(new BasicStroke(2));
        g1.setColor(Color.black);
        g1.drawLine(0, h, w * 2, h);
        g1.drawLine(w, 0, w, h * 2);
        g1.drawString("0", w - 7, h + 13);

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        Polygon p = new Polygon();
        for (Map.Entry<Integer, Integer> waitTimeEntry : busyTime.entrySet()) {
            p.addPoint(w + waitTimeEntry.getKey(), h - waitTimeEntry.getValue());
        }
        g2.drawPolyline(p.xpoints, p.ypoints, p.npoints);

        Polygon p1 = new Polygon();
        for (int x = -10; x <= 10; x++) {
            p1.addPoint(w + x, h - ((x * x * x) / 100) - x + 10);
        }
        g2.drawPolyline(p1.xpoints, p1.ypoints, p1.npoints);
    }

}
