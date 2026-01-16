package io.blert.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getWidth();
            if (targetWidth == 0) {
                Container container = target.getParent();
                while (container != null) {
                    targetWidth = container.getWidth();
                    if (targetWidth > 0) break;
                    container = container.getParent();
                }
            }
            if (targetWidth == 0) targetWidth = 200;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int maxwidth = targetWidth - (insets.left + insets.right + hgap * 2);

            int width = 0;
            int rowWidth = 0;
            int rowHeight = 0;
            int totalHeight = 0;

            for (Component m : target.getComponents()) {
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxwidth) {
                        totalHeight += rowHeight + vgap;
                        width = Math.max(width, rowWidth);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    rowWidth += d.width + hgap;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }
            totalHeight += rowHeight + vgap + insets.top + insets.bottom;
            width = Math.max(width, rowWidth) + insets.left + insets.right + hgap * 2;
            return new Dimension(width, totalHeight);
        }
    }
}
