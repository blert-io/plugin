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

import io.blert.client.WebsocketEventHandler;
import io.blert.proto.ChallengeMode;
import io.blert.proto.ServerMessage;
import io.blert.proto.Stage;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import org.apache.commons.lang3.tuple.Pair;
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
    private JPanel challengeStatusPanel;
    private JPanel recentRecordingsPanel;
    private JPanel recentRecordingsContainer;

    private final List<ServerMessage.PastChallenge> recentRecordings = new ArrayList<>();

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
        createChallengeStatusPanel(this, WebsocketEventHandler.Status.IDLE, null);
        createRecentRecordingsPanel(this);
        populateRecentRecordingsPanel();
    }

    public void updateUser(@Nullable String username) {
        createUserPanel(username);
    }

    public void updateChallengeStatus(WebsocketEventHandler.Status status, @Nullable String challengeId) {
        createChallengeStatusPanel(this, status, challengeId);
    }

    public void setRecentRecordings(@Nullable List<ServerMessage.PastChallenge> recentRecordings) {
        this.recentRecordings.clear();
        if (recentRecordings != null) {
            this.recentRecordings.addAll(recentRecordings);
        }
        populateRecentRecordingsPanel();
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

    private void createChallengeStatusPanel(
            JPanel parent, WebsocketEventHandler.Status status, @Nullable String challengeId) {
        if (challengeStatusPanel != null) {
            parent.remove(challengeStatusPanel);
        }

        challengeStatusPanel = new JPanel();
        challengeStatusPanel.setLayout(new BorderLayout());
        challengeStatusPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel currentStatePanel = createCurrentChallengeStatePanel(status);

        if (status == WebsocketEventHandler.Status.CHALLENGE_ACTIVE) {
            JPanel actionsPanel = createChallengeActionsPanel(challengeId);
            challengeStatusPanel.add(actionsPanel, BorderLayout.SOUTH);
        }

        challengeStatusPanel.add(currentStatePanel, BorderLayout.CENTER);
        parent.add(challengeStatusPanel, BorderLayout.CENTER);
    }

    private void createRecentRecordingsPanel(JPanel parent) {
        if (recentRecordingsPanel != null) {
            parent.remove(recentRecordingsPanel);
        }

        recentRecordingsPanel = new JPanel();
        recentRecordingsPanel.setLayout(new BorderLayout());
        recentRecordingsPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        recentRecordingsPanel.add(createHeading("Previous Recordings"), BorderLayout.NORTH);

        recentRecordingsContainer = new JPanel();
        recentRecordingsContainer.setLayout(new BorderLayout());
        recentRecordingsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JScrollPane scrollPane = new JScrollPane(recentRecordingsContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(0, 600));
        scrollPane.setBorder(new LineBorder(ColorScheme.BORDER_COLOR));
        recentRecordingsPanel.add(scrollPane, BorderLayout.CENTER);

        parent.add(recentRecordingsPanel, BorderLayout.SOUTH);
    }

    private void populateRecentRecordingsPanel() {
        if (recentRecordingsContainer == null) {
            return;
        }

        recentRecordingsContainer.removeAll();

        if (recentRecordings.isEmpty()) {
            JLabel noRecordingsLabel = new JLabel("No past recordings.");
            noRecordingsLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
            noRecordingsLabel.setForeground(Color.GRAY);
            noRecordingsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            recentRecordingsContainer.add(noRecordingsLabel, BorderLayout.NORTH);
        } else {
            JPanel recentRecordingsList = new JPanel();
            recentRecordingsList.setLayout(new GridBagLayout());
            recentRecordingsList.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            int gridy = 0;
            for (ServerMessage.PastChallenge challenge : recentRecordings) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                constraints.gridy = gridy++;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.insets = new Insets(4, 4, 4, 4);
                recentRecordingsList.add(createPastChallengeEntry(challenge), constraints);
            }

            recentRecordingsContainer.add(recentRecordingsList, BorderLayout.NORTH);
        }
    }

    @NotNull
    private static JPanel createCurrentChallengeStatePanel(WebsocketEventHandler.Status status) {
        JPanel currentStatePanel = new JPanel();
        currentStatePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        currentStatePanel.setLayout(new BorderLayout());

        JLabel challengeStatusText = new JLabel();
        challengeStatusText.setHorizontalAlignment(SwingConstants.CENTER);

        switch (status) {
            case IDLE:
                challengeStatusText.setText("Not in a PvM challenge.");
                break;
            case CHALLENGE_STARTING:
                challengeStatusText.setText("Starting...");
                challengeStatusText.setForeground(Color.YELLOW);
                break;
            case CHALLENGE_ACTIVE:
                challengeStatusText.setText("Streaming challenge data!");
                challengeStatusText.setForeground(Color.GREEN);
                break;
        }

        currentStatePanel.add(createHeading("Current Challenge"), BorderLayout.NORTH);
        currentStatePanel.add(challengeStatusText, BorderLayout.CENTER);
        return currentStatePanel;
    }

    @NotNull
    private JPanel createChallengeActionsPanel(@Nullable String challengeId) {
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new GridLayout(1, 0, 5, 0));

        JButton linkButton = new JButton("View Raid");
        linkButton.addActionListener(e -> LinkBrowser.browse(challengeUrl(challengeId)));
        actionsPanel.add(linkButton);

        JButton copyLinkButton = new JButton("Copy Link");
        copyLinkButton.addActionListener(e ->
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(challengeUrl(challengeId)), null));
        actionsPanel.add(copyLinkButton);
        return actionsPanel;
    }

    private static JPanel createHeading(String title) {
        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(new BorderLayout());
        headingPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel heading = new JLabel(String.format("— %s —", title));
        heading.setForeground(Color.WHITE);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        headingPanel.add(heading, BorderLayout.NORTH);
        return headingPanel;
    }

    private Pair<String, Color> getChallengeStatusInfo(ServerMessage.PastChallenge.Status status, Stage stage) {
        if (status == ServerMessage.PastChallenge.Status.IN_PROGRESS) {
            return Pair.of("In Progress", Color.WHITE);
        }
        if (status == ServerMessage.PastChallenge.Status.COMPLETED) {
            return Pair.of("Completed", Color.GREEN);
        }

        String boss = "Unknown";
        switch (stage) {
            case TOB_MAIDEN:
                boss = "Maiden";
                break;
            case TOB_BLOAT:
                boss = "Bloat";
                break;
            case TOB_NYLOCAS:
                boss = "Nylocas";
                break;
            case TOB_SOTETSEG:
                boss = "Sotetseg";
                break;
            case TOB_XARPUS:
                boss = "Xarpus";
                break;
            case TOB_VERZIK:
                boss = "Verzik";
                break;
        }

        if (status == ServerMessage.PastChallenge.Status.WIPED) {
            return Pair.of(boss + " Wipe", Color.RED);
        }
        return Pair.of(boss + " Reset", Color.GRAY);
    }

    private String challengeModeToString(ChallengeMode mode) {
        switch (mode) {
            case TOB_ENTRY:
                return "Entry Mode";
            case TOB_REGULAR:
                return "Regular Mode";
            case TOB_HARD:
                return "Hard Mode";

            case COX_REGULAR:
            case COX_CHALLENGE:
            case TOA_ENTRY:
            case TOA_NORMAL:
            case TOA_EXPERT:
                break;
        }

        return "Unknown";
    }

    private JPanel createPastChallengeEntry(ServerMessage.PastChallenge challenge) {
        JPanel challengePanel = new JPanel();
        challengePanel.setLayout(new BorderLayout());
        challengePanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(1, 0, 5, 0));


        Pair<String, Color> statusInfo = getChallengeStatusInfo(challenge.getStatus(), challenge.getStage());
        JLabel statusLabel = new JLabel();
        statusLabel.setForeground(statusInfo.getRight());
        statusLabel.setText(statusInfo.getLeft());
        statusPanel.add(statusLabel);

        JLabel modeLabel = new JLabel();
        modeLabel.setForeground(Color.WHITE);
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.ITALIC));
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        modeLabel.setText(challengeModeToString(challenge.getMode()));
        statusPanel.add(modeLabel);

        challengePanel.add(statusPanel, BorderLayout.NORTH);

        JPanel partyPanel = new JPanel();

        partyPanel.setLayout(new BorderLayout());
        partyPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
        JLabel partyHeading = new JLabel("Party (" + challenge.getPartyCount() + "):");
        partyHeading.setFont(partyHeading.getFont().deriveFont(Font.BOLD));
        partyHeading.setForeground(Color.WHITE);
        partyPanel.add(partyHeading, BorderLayout.NORTH);

        JLabel partyLabel = new JLabel();
        partyLabel.setForeground(Color.WHITE);
        String party = String.join(", ", challenge.getPartyList());
        partyLabel.setText(wrapHtml("<p style=\"width: 150px\">" + party + "</p>"));
        partyPanel.add(partyLabel, BorderLayout.CENTER);
        challengePanel.add(partyPanel, BorderLayout.CENTER);

        challengePanel.add(createChallengeActionsPanel(challenge.getId()), BorderLayout.SOUTH);

        return challengePanel;
    }

    private String challengeUrl(String challengeId) {
        String hostname = !Strings.isNullOrEmpty(config.webUrl())
                ? config.webUrl()
                : BlertPlugin.DEFAULT_BLERT_HOSTNAME;
        if (!hostname.startsWith("http://") && !hostname.startsWith("https://")) {
            hostname = "https://" + hostname;
        }

        return String.format("%s/raids/tob/%s/overview", hostname, challengeId);
    }

    static String wrapHtml(String content) {
        return "<html><body>" + content + "</body></html>";
    }
}
