package io.blert.ui;

import static io.blert.ui.UIConstants.BG_CARD;
import static io.blert.ui.UIConstants.BORDER;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class CardPanel extends JPanel {
    public CardPanel() {
        setOpaque(false);
        setBackground(BG_CARD);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w, h, 8, 8);

        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, w, h, 8, 8);
    }
}
