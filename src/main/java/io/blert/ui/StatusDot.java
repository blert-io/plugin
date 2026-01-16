package io.blert.ui;

import static io.blert.ui.UIConstants.TEXT_MUTED;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

public class StatusDot extends JComponent {
    private Color color = TEXT_MUTED;

    public StatusDot() {
        setPreferredSize(new Dimension(8, 8));
        setMinimumSize(new Dimension(8, 8));
    }

    public void setColor(Color c) {
        this.color = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, getWidth(), getHeight());
    }
}
