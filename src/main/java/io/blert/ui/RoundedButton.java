package io.blert.ui;

import static io.blert.ui.UIConstants.BG_CARD_HOVER;
import static io.blert.ui.UIConstants.BORDER;
import static io.blert.ui.UIConstants.BUTTON_HOVER;
import static io.blert.ui.UIConstants.FONT_BOLD;
import static io.blert.ui.UIConstants.TEXT_MAIN;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

public class RoundedButton extends JButton {
    public RoundedButton(String text) {
        super(text);
        setFont(FONT_BOLD);
        setForeground(TEXT_MAIN);
        setBackground(BG_CARD_HOVER);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(3, 8, 3, 8));

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setBackground(BUTTON_HOVER);
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                setBackground(BG_CARD_HOVER);
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

        super.paintComponent(g);
    }
}
