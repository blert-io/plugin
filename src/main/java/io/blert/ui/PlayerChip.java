package io.blert.ui;

import static io.blert.ui.UIConstants.BG_BASE;
import static io.blert.ui.UIConstants.BG_BASE_HOVER;
import static io.blert.ui.UIConstants.BORDER;
import static io.blert.ui.UIConstants.FONT_SMALLEST;
import static io.blert.ui.UIConstants.TEXT_MAIN;
import static io.blert.ui.UIConstants.TEXT_MUTED;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.util.LinkBrowser;

public class PlayerChip extends JLabel {
    private boolean isChipHovered = false;

    public PlayerChip(String username, FeedItem parentCard) {
        super(username);

        setFont(FONT_SMALLEST);
        setForeground(TEXT_MUTED);
        setBorder(new EmptyBorder(1, 5, 1, 5));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isChipHovered = true;
                if (parentCard != null) parentCard.setCardHovered(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isChipHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    String encoded = java.net.URLEncoder.encode(
                            username,
                            StandardCharsets.UTF_8
                    ).replace("+", "%20");
                    LinkBrowser.browse("https://blert.io/players/" + encoded);
                } catch (Exception ignored) {
                }
                e.consume();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(isChipHovered ? BG_BASE_HOVER : BG_BASE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.setColor(isChipHovered ? BORDER.brighter() : BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        setForeground(isChipHovered ? TEXT_MAIN : TEXT_MUTED);
        super.paintComponent(g);
    }
}
