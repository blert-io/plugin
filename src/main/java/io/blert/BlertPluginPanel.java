/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert;

import io.blert.client.ServerMessage;
import io.blert.client.WebsocketEventHandler;
import io.blert.raid.Mode;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BlertPluginPanel extends PluginPanel {
    private final BlertPlugin plugin;
    private final BlertConfig config;

    private JPanel userPanel;
    private JPanel raidStatusPanel;
    private JPanel raidHistoryPanel;
    private JPanel raidHistoryContainer;

    private final List<ServerMessage.PastRaid> raidHistory = new ArrayList<>();

    public BlertPluginPanel(BlertPlugin plugin, BlertConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void startPanel() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(false);

        createUserPanel(null);
        createRaidStatusPanel(this, WebsocketEventHandler.Status.IDLE, null);
        createRaidHistoryPanel(this);
        populateRaidHistoryPanel();
    }

    public void updateUser(@Nullable String username) {
        createUserPanel(username);
    }

    public void updateRaidStatus(WebsocketEventHandler.Status status, @Nullable String raidId) {
        createRaidStatusPanel(this, status, raidId);
    }

    public void setRaidHistory(@Nullable List<ServerMessage.PastRaid> raidHistory) {
        this.raidHistory.clear();
        if (raidHistory != null) {
            this.raidHistory.addAll(raidHistory);
        }
        populateRaidHistoryPanel();
    }

    private void createUserPanel(@Nullable String username) {
        if (userPanel != null) {
            remove(userPanel);
        }

        userPanel = new JPanel();
        userPanel.setLayout(new BorderLayout());
        userPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        userPanel.add(createHeading("Server Status"), BorderLayout.NORTH);

        JLabel connectionLabel = new JLabel();
        connectionLabel.setForeground(Color.WHITE);
        connectionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (username != null) {
            connectionLabel.setText(wrapHtml("Connected as: <b style=\"color: white\">" + username + "</b>"));
            connectionLabel.setForeground(Color.GREEN);
            userPanel.add(connectionLabel, BorderLayout.SOUTH);
        } else {
            connectionLabel.setText("Not connected.");
            connectionLabel.setForeground(Color.RED);
            userPanel.add(connectionLabel, BorderLayout.CENTER);

            if (Strings.isNullOrEmpty(config.apiKey())) {
                JLabel apiKeyLabel = new JLabel(wrapHtml(
                        "<p style=\"text-align: center\">Enter your Blert API key in the plugin settings.</p>"));
                apiKeyLabel.setForeground(Color.WHITE);
                apiKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                userPanel.add(apiKeyLabel, BorderLayout.SOUTH);
            } else if (config.dontConnect()) {
                JLabel dontConnectLabel = new JLabel("Blert is disabled in settings.");
                dontConnectLabel.setForeground(Color.WHITE);
                dontConnectLabel.setHorizontalAlignment(SwingConstants.CENTER);
                userPanel.add(dontConnectLabel, BorderLayout.SOUTH);
            } else {
                JPanel connectButtonPanel = new JPanel();
                connectButtonPanel.setLayout(new BorderLayout());
                connectButtonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
                JButton connectButton = new JButton("Connect");
                connectButton.addActionListener(e -> plugin.initializeWebSocketClient());
                connectButtonPanel.add(connectButton, BorderLayout.CENTER);
                userPanel.add(connectButtonPanel, BorderLayout.SOUTH);
            }
        }

        add(userPanel, BorderLayout.NORTH);
    }

    private void createRaidStatusPanel(JPanel parent, WebsocketEventHandler.Status status, @Nullable String raidId) {
        if (raidStatusPanel != null) {
            parent.remove(raidStatusPanel);
        }

        raidStatusPanel = new JPanel();
        raidStatusPanel.setLayout(new BorderLayout());
        raidStatusPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel currentStatePanel = createCurrentRaidStatePanel(status);

        if (status == WebsocketEventHandler.Status.RAID_ACTIVE) {
            JPanel actionsPanel = createRaidActionsPanel(raidId);
            raidStatusPanel.add(actionsPanel, BorderLayout.SOUTH);
        }

        raidStatusPanel.add(currentStatePanel, BorderLayout.CENTER);
        parent.add(raidStatusPanel, BorderLayout.CENTER);
    }

    private void createRaidHistoryPanel(JPanel parent) {
        if (raidHistoryPanel != null) {
            parent.remove(raidHistoryPanel);
        }

        raidHistoryPanel = new JPanel();
        raidHistoryPanel.setLayout(new BorderLayout());
        raidHistoryPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        raidHistoryPanel.add(createHeading("Recent Raids"), BorderLayout.NORTH);

        raidHistoryContainer = new JPanel();
        raidHistoryContainer.setLayout(new BorderLayout());
        raidHistoryContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(raidHistoryContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(0, 600));
        scrollPane.setBorder(new LineBorder(ColorScheme.BORDER_COLOR));
        raidHistoryPanel.add(scrollPane, BorderLayout.CENTER);

        parent.add(raidHistoryPanel, BorderLayout.SOUTH);
    }

    private void populateRaidHistoryPanel() {
        if (raidHistoryContainer == null) {
            return;
        }

        raidHistoryContainer.removeAll();

        if (raidHistory.isEmpty()) {
            JLabel noRaidHistoryLabel = new JLabel("No past raids available.");
            noRaidHistoryLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
            noRaidHistoryLabel.setForeground(Color.GRAY);
            noRaidHistoryLabel.setHorizontalAlignment(SwingConstants.CENTER);
            raidHistoryContainer.add(noRaidHistoryLabel, BorderLayout.NORTH);
        } else {
            JPanel raidHistoryList = new JPanel();
            raidHistoryList.setLayout(new GridBagLayout());
            raidHistoryList.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            int gridy = 0;
            for (ServerMessage.PastRaid raid : raidHistory) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                constraints.gridy = gridy++;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.insets = new Insets(4, 4, 4, 4);
                raidHistoryList.add(createPastRaidEntry(raid), constraints);
            }

            raidHistoryContainer.add(raidHistoryList, BorderLayout.NORTH);
        }
    }

    @NotNull
    private static JPanel createCurrentRaidStatePanel(WebsocketEventHandler.Status status) {
        JPanel currentStatePanel = new JPanel();
        currentStatePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        currentStatePanel.setLayout(new BorderLayout());

        JLabel raidStatusText = new JLabel();
        raidStatusText.setHorizontalAlignment(SwingConstants.CENTER);

        switch (status) {
            case IDLE:
                raidStatusText.setText("Not in a raid");
                break;
            case RAID_STARTING:
                raidStatusText.setText("Starting raid");
                raidStatusText.setForeground(Color.YELLOW);
                break;
            case RAID_ACTIVE:
                raidStatusText.setText("Streaming raid data");
                raidStatusText.setForeground(Color.GREEN);
                break;
        }

        currentStatePanel.add(createHeading("Raid Status"), BorderLayout.NORTH);
        currentStatePanel.add(raidStatusText, BorderLayout.CENTER);
        return currentStatePanel;
    }

    @NotNull
    private JPanel createRaidActionsPanel(@Nullable String raidId) {
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new GridLayout(1, 0, 5, 0));

        JButton raidLinkButton = new JButton("View Raid");
        raidLinkButton.addActionListener(e -> LinkBrowser.browse(raidUrl(raidId)));
        actionsPanel.add(raidLinkButton);

        JButton copyRaidLinkButton = new JButton("Copy Link");
        copyRaidLinkButton.addActionListener(e ->
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(raidUrl(raidId)), null));
        actionsPanel.add(copyRaidLinkButton);
        return actionsPanel;
    }

    private static JPanel createHeading(String title) {
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(new BorderLayout());
        headingPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel heading = new JLabel(title);
        heading.setForeground(Color.WHITE);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        headingPanel.add(heading, BorderLayout.NORTH);
        return headingPanel;
    }

    private JPanel createPastRaidEntry(ServerMessage.PastRaid raid) {
        JPanel raidPanel = new JPanel();
        raidPanel.setLayout(new BorderLayout());
        raidPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(1, 0, 5, 0));

        JLabel statusLabel = new JLabel();
        if (raid.getStatus().isCompletion()) {
            statusLabel.setForeground(Color.GREEN);
        } else if (raid.getStatus().isWipe()) {
            statusLabel.setForeground(Color.RED);
        } else if (raid.getStatus().isReset()) {
            statusLabel.setForeground(Color.GRAY);
        } else {
            statusLabel.setForeground(Color.WHITE);
        }
        statusLabel.setText(raid.getStatus().toString());
        statusPanel.add(statusLabel);

        JLabel modeLabel = new JLabel();
        modeLabel.setForeground(Color.WHITE);
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.ITALIC));
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        Mode mode = raid.getMode();
        if (mode != null) {
            String modeText = mode.toString().charAt(0) + mode.toString().substring(1).toLowerCase();
            modeLabel.setText(modeText + " Mode");
        } else {
            modeLabel.setText("Unknown Mode");
        }
        statusPanel.add(modeLabel);

        raidPanel.add(statusPanel, BorderLayout.NORTH);

        JPanel partyPanel = new JPanel();

        partyPanel.setLayout(new BorderLayout());
        partyPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel partyHeading = new JLabel("Party (" + raid.getParty().size() + "):");
        partyHeading.setFont(partyHeading.getFont().deriveFont(Font.BOLD));
        partyHeading.setForeground(Color.WHITE);
        partyPanel.add(partyHeading, BorderLayout.NORTH);

        JLabel partyLabel = new JLabel();
        partyLabel.setForeground(Color.WHITE);
        String party = String.join(", ", raid.getParty());
        partyLabel.setText(wrapHtml("<p style=\"width: 150px\">" + party + "</p>"));
        partyPanel.add(partyLabel, BorderLayout.CENTER);
        raidPanel.add(partyPanel, BorderLayout.CENTER);

        raidPanel.add(createRaidActionsPanel(raid.getId()), BorderLayout.SOUTH);

        return raidPanel;
    }

    private String raidUrl(String raidId) {
        String hostname = !Strings.isNullOrEmpty(config.webUrl())
                ? config.webUrl()
                : BlertPlugin.DEFAULT_BLERT_HOSTNAME;
        if (!hostname.startsWith("http://") && !hostname.startsWith("https://")) {
            hostname = "https://" + hostname;
        }

        return String.format("%s/raids/tob/%s/overview", hostname, raidId);
    }

    static String wrapHtml(String content) {
        return "<html><body>" + content + "</body></html>";
    }
}
