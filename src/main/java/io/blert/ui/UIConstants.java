package io.blert.ui;

import java.awt.Color;
import java.awt.Font;

import net.runelite.client.ui.ColorScheme;

public class UIConstants {
    public static final Color BG_BASE = ColorScheme.DARKER_GRAY_COLOR;
    public static final Color BG_BASE_HOVER = BG_BASE.brighter();
    public static final Color BG_CARD = ColorScheme.DARK_GRAY_COLOR;
    public static final Color BG_CARD_HOVER = BG_CARD.brighter();
    public static final Color BORDER = BG_CARD.brighter();
    public static final Color TEXT_MAIN = ColorScheme.TEXT_COLOR;
    public static final Color TEXT_MUTED = ColorScheme.TEXT_COLOR.darker();
    public static final Color ACCENT_GREEN = new Color(62, 160, 85);
    public static final Color ACCENT_RED = new Color(185, 55, 55);
    public static final Color ACCENT_YELLOW = new Color(195, 155, 30);
    public static final Color BUTTON_HOVER = BG_BASE.brighter();

    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 12);
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_SMALLEST = new Font("SansSerif", Font.PLAIN, 10);

}
