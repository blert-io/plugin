package io.blert.ui;

import com.google.common.base.Strings;
import io.blert.util.Tick;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static io.blert.ui.UIConstants.*;

public class FeedItem extends JPanel {
    private final Color accentColor;
    private boolean isHovered = false;

    public FeedItem(String title, String mode,
                    int ticks, @Nullable String timestamp,
                    Color accent, List<String> party) {
        this.accentColor = accent;
        setOpaque(false);
        setLayout(new BorderLayout(0, 4));
        setBorder(new EmptyBorder(5, 12, 5, 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel header = getTitlePanel(title, mode, ticks, timestamp);

        add(header, BorderLayout.NORTH);

        if (!party.isEmpty()) {
            JPanel partyPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
            partyPanel.setOpaque(false);
            partyPanel.setBorder(new EmptyBorder(0, -4, 0, 0));

            for (String member : party) {
                partyPanel.add(new PlayerChip(member, this));
            }
            add(partyPanel, BorderLayout.CENTER);
        }

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                setCardHovered(true);
            }

            public void mouseExited(MouseEvent e) {
                setCardHovered(false);
            }
        });
    }

    private static JPanel getTitlePanel(String title, String mode, int ticks,
                                        @Nullable String timestamp) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel leftHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftHead.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_BOLD.deriveFont(11f));
        titleLabel.setForeground(TEXT_MAIN);
        leftHead.add(titleLabel);

        if (!mode.isEmpty()) {
            JLabel modeLabel = new JLabel(" • " + mode);
            modeLabel.setFont(FONT_SMALLEST);
            modeLabel.setForeground(TEXT_MUTED);
            leftHead.add(modeLabel);
        }

        if (ticks > 0) {
            JLabel ticksLabel = new JLabel(" • " + Tick.asTimeString(ticks));
            ticksLabel.setFont(FONT_SMALLEST);
            ticksLabel.setForeground(TEXT_MUTED);
            leftHead.add(ticksLabel);
        }

        header.add(leftHead, BorderLayout.WEST);

        if (Strings.isNullOrEmpty(timestamp)) {
            return header;
        }

        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(FONT_SMALLEST);
        timeLabel.setForeground(TEXT_MUTED);
        header.add(timeLabel, BorderLayout.EAST);
        return header;
    }

    void setCardHovered(boolean hovered) {
        this.isHovered = hovered;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        g2.setColor(isHovered ? BG_CARD_HOVER : BG_CARD);
        g2.fillRoundRect(0, 0, w, h, 8, 8);

        g2.setColor(accentColor);
        g2.fillRoundRect(3, 4, 3, h - 8, 2, 2);

        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, w, h, 8, 8);
    }
}
