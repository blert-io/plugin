/*
 * Copyright (c) 2024 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert;

import io.blert.client.WebSocketEventHandler;
import io.blert.client.WebSocketManager;
import io.blert.core.Challenge;
import io.blert.core.ChallengeMode;
import io.blert.core.Stage;
import io.blert.json.PastChallenge;
import io.blert.ui.CardPanel;
import io.blert.ui.FeedItem;
import io.blert.ui.RoundedButton;
import io.blert.ui.ScrollablePanel;
import io.blert.ui.StatusDot;
import io.blert.ui.ThinScrollBarUI;

import static io.blert.ui.UIConstants.*;

import java.util.Objects;

import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BlertPluginPanel extends PluginPanel {
    /**
     * Tracks the websocket connection state for UI display purposes.
     */
    public enum ConnectionState {
        /**
         * Not connected to the server.
         */
        DISCONNECTED,
        /**
         * Currently attempting to connect.
         */
        CONNECTING,
        /**
         * Successfully connected and authenticated.
         */
        CONNECTED,
        /**
         * Connection was rejected (invalid API key).
         */
        REJECTED,
        /**
         * Connection was rejected due to outdated plugin version.
         */
        UNSUPPORTED_VERSION,
    }

    private final BlertConfig config;
    private final WebSocketManager websocketManager;

    private JPanel userPanel;
    private JPanel challengeStatusPanel;
    private JPanel recentRecordingsPanel;
    private JPanel recentRecordingsContainer;
    private final JLabel serverStatusLabel = new JLabel();
    private final Timer shutdownLabelTimer;

    private final List<PastChallenge> recentRecordings = new ArrayList<>();

    private WebSocketEventHandler.Status challengeStatus = WebSocketEventHandler.Status.IDLE;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private @Nullable String connectedUsername = null;
    private Instant shutdownTime = null;
    private Challenge currentChallenge = null;
    private String currentChallengeId = null;

    public BlertPluginPanel(BlertConfig config, WebSocketManager websocketManager) {
        super(false);
        this.config = config;
        this.websocketManager = websocketManager;

        shutdownLabelTimer = new Timer(1000, e -> updateShutdownLabel());

        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(BG_BASE);
        setLayout(new BorderLayout());
    }

    public void startPanel() {
        removeAll();

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(BG_BASE);

        userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(BG_BASE);
        userPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topContainer.add(userPanel);

        challengeStatusPanel = new JPanel(new BorderLayout());
        challengeStatusPanel.setBackground(BG_BASE);
        challengeStatusPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topContainer.add(challengeStatusPanel);

        add(topContainer, BorderLayout.NORTH);

        createRecentRecordingsPanel();
        add(recentRecordingsPanel, BorderLayout.CENTER);

        rebuildUserPanel();
        rebuildChallengePanel();
        populateRecentRecordingsPanel();

        shutdownLabelTimer.start();
        revalidate();
        repaint();
    }

    public void stopPanel() {
        shutdownLabelTimer.stop();
    }

    private void updateShutdownLabel() {
        if (shutdownTime == null) {
            if (connectionState == ConnectionState.CONNECTED) {
                serverStatusLabel.setText("✔ Blert server is online");
                serverStatusLabel.setForeground(TEXT_MUTED);
            } else if (connectionState != ConnectionState.REJECTED && connectionState != ConnectionState.UNSUPPORTED_VERSION) {
                serverStatusLabel.setText("");
            }
        } else {
            Duration timeUntilShutdown = Duration.between(Instant.now(), shutdownTime);
            if (timeUntilShutdown.isNegative()) {
                serverStatusLabel.setText("✘ Server shutting down...");
                serverStatusLabel.setForeground(ACCENT_RED);
            } else {
                String time = DurationFormatUtils.formatDuration(timeUntilShutdown.toMillis(), "HH:mm:ss");
                serverStatusLabel.setForeground(ACCENT_YELLOW);
                serverStatusLabel.setText("⚠ Shutdown in " + time);
            }
        }
    }

    public void updateConnectionState(ConnectionState state, @Nullable String username) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.connectionState = state;
                this.connectedUsername = username;
                rebuildUserPanel();
                rebuildChallengePanel();
                revalidate();
                repaint();
            }
        });
    }

    public void updateChallengeStatus(
            WebSocketEventHandler.Status status,
            @Nullable Challenge challenge,
            @Nullable String challengeId
    ) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.challengeStatus = status;
                this.currentChallenge = challenge;
                this.currentChallengeId = challengeId;
                rebuildChallengePanel();
                revalidate();
                repaint();
            }
        });
    }

    public void setRecentRecordings(@Nullable List<PastChallenge> recentRecordings) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.recentRecordings.clear();
                if (recentRecordings != null) {
                    this.recentRecordings.addAll(recentRecordings);
                }
                populateRecentRecordingsPanel();
                revalidate();
                repaint();
            }
        });
    }

    public void setShutdownTime(@Nullable Instant shutdownTime) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.shutdownTime = shutdownTime;
                updateShutdownLabel();
                rebuildChallengePanel();
            }
        });
    }

    /**
     * Rebuilds the user panel based on the current connection state.
     */
    private void rebuildUserPanel() {
        userPanel.removeAll();

        userPanel.add(createHeader("SERVER CONNECTION"), BorderLayout.NORTH);

        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(6, 8, 6, 8));
        userPanel.add(card, BorderLayout.CENTER);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statusContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusContainer.setOpaque(false);
        statusContainer.setBorder(new EmptyBorder(0, -6, 0, 0));

        StatusDot statusDot = new StatusDot();

        JLabel statusText = new JLabel();
        statusText.setFont(FONT_BOLD);

        JLabel userLabel = new JLabel();
        userLabel.setFont(FONT_REGULAR);
        userLabel.setForeground(TEXT_MUTED);

        statusContainer.add(statusDot);
        statusContainer.add(statusText);

        topRow.add(statusContainer, BorderLayout.WEST);
        topRow.add(userLabel, BorderLayout.EAST);

        serverStatusLabel.setFont(FONT_SMALLEST);
        serverStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverStatusLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        RoundedButton connectButton = new RoundedButton("Connect");
        connectButton.addActionListener(e -> connectToServer());
        connectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        connectButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));

        JLabel configHint = new JLabel("Enter API Key in config");
        configHint.setFont(FONT_SMALL);
        configHint.setForeground(TEXT_MUTED);
        configHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        configHint.setBorder(new EmptyBorder(4, 0, 0, 0));

        Color stateColor = ACCENT_RED;
        String stateTitle;
        String detailMsg = null;
        boolean showUser = false;
        boolean showButton = false;
        boolean hasApiKey = !Strings.isNullOrEmpty(config.apiKey());

        switch (connectionState) {
            case CONNECTED:
                stateColor = ACCENT_GREEN;
                stateTitle = "Connected";
                showUser = true;
                userLabel.setText(connectedUsername != null ? connectedUsername : "Unknown");
                serverStatusLabel.setForeground(TEXT_MAIN);
                serverStatusLabel.setHorizontalAlignment(SwingConstants.LEFT);
                updateShutdownLabel();
                break;
            case CONNECTING:
                stateColor = ACCENT_YELLOW;
                stateTitle = "Connecting...";
                break;
            case REJECTED:
                stateTitle = "Connection Failed";
                detailMsg = "Invalid API Key, check plugin config.";
                showButton = hasApiKey;
                break;
            case UNSUPPORTED_VERSION:
                stateTitle = "Update Required";
                detailMsg = "Plugin version out of date, restart client.";
                break;
            case DISCONNECTED:
            default:
                stateTitle = "Disconnected";
                showButton = hasApiKey;
                break;
        }

        statusDot.setColor(stateColor);
        statusText.setText(stateTitle);
        statusText.setForeground(stateColor == ACCENT_GREEN ? TEXT_MAIN : stateColor);
        userLabel.setVisible(showUser);

        card.add(topRow);

        if (connectionState == ConnectionState.CONNECTED) {
            card.add(serverStatusLabel);
        } else if (detailMsg != null) {
            serverStatusLabel.setText(detailMsg);
            serverStatusLabel.setForeground(ACCENT_RED);
            card.add(serverStatusLabel);
        }

        card.add(Box.createVerticalStrut(4));
        if (showButton) {
            card.add(connectButton);
        } else if (!hasApiKey && connectionState != ConnectionState.CONNECTED && connectionState != ConnectionState.CONNECTING) {
            card.add(configHint);
        }
    }

    private void connectToServer() {
        updateConnectionState(ConnectionState.CONNECTING, null);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    return websocketManager.open().get();
                } catch (Exception e) {
                    log.error("Error connecting to Blert server", e);
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (!success && connectionState == ConnectionState.CONNECTING) {
                        updateConnectionState(ConnectionState.DISCONNECTED, null);
                    }
                } catch (Exception e) {
                    updateConnectionState(ConnectionState.DISCONNECTED, null);
                }
            }
        };
        worker.execute();
    }

    private void rebuildChallengePanel() {
        challengeStatusPanel.removeAll();
        challengeStatusPanel.add(createHeader("CURRENT CHALLENGE"), BorderLayout.NORTH);

        CardPanel card = new CardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(8, 8, 8, 8));

        card.add(Box.createVerticalGlue());

        if (connectionState != ConnectionState.CONNECTED) {
            JLabel msg = new JLabel("Offline");
            msg.setFont(FONT_BOLD);
            msg.setForeground(TEXT_MUTED);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(msg);

        } else if (challengeStatus == WebSocketEventHandler.Status.IDLE) {
            boolean shuttingDown = (shutdownTime != null);
            JLabel msg = new JLabel(shuttingDown ? "Server Maintenance" : "No Active Challenge");
            msg.setFont(FONT_BOLD);
            msg.setForeground(shuttingDown ? ACCENT_RED : TEXT_MAIN);
            msg.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(msg);

        } else {
            JPanel activeRaidPanel = createActiveRaidPanel();
            card.add(activeRaidPanel);

            if (currentChallenge != null && currentChallengeId != null) {
                card.add(Box.createVerticalStrut(8));

                JPanel buttonPanel = createViewCopyButtonPanel();
                card.add(buttonPanel);
            }
        }

        card.add(Box.createVerticalGlue());

        challengeStatusPanel.add(card, BorderLayout.CENTER);
        challengeStatusPanel.revalidate();
        challengeStatusPanel.repaint();
    }

    private JPanel createViewCopyButtonPanel() {
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 26));

        RoundedButton viewBtn = new RoundedButton("View");
        viewBtn.addActionListener(e -> LinkBrowser.browse(challengeUrl(currentChallenge, currentChallengeId)));

        RoundedButton copyBtn = new RoundedButton("Copy Link");
        copyBtn.addActionListener(e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(challengeUrl(currentChallenge, currentChallengeId)), null));

        btnPanel.add(viewBtn);
        btnPanel.add(copyBtn);
        return btnPanel;
    }

    private JPanel createActiveRaidPanel() {
        String title = (currentChallenge != null) ? currentChallenge.getName() : "Unknown Raid";
        Color statusColor = ACCENT_GREEN;

        if (challengeStatus == WebSocketEventHandler.Status.CHALLENGE_STARTING ||
                challengeStatus == WebSocketEventHandler.Status.CHALLENGE_ENDING) {
            statusColor = ACCENT_YELLOW;
        }

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        titlePanel.setOpaque(false);
        titlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 30));

        StatusDot statusDot = new StatusDot();
        statusDot.setColor(statusColor);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_BOLD.deriveFont(14f));
        titleLabel.setForeground(TEXT_MAIN);

        titlePanel.add(statusDot);
        titlePanel.add(titleLabel);
        return titlePanel;
    }

    private void createRecentRecordingsPanel() {
        recentRecordingsPanel = new JPanel(new BorderLayout());
        recentRecordingsPanel.setBackground(BG_BASE);

        recentRecordingsPanel.add(createHeader("RECENT ACTIVITY"), BorderLayout.NORTH);

        recentRecordingsContainer = new ScrollablePanel(new GridBagLayout());
        recentRecordingsContainer.setBackground(BG_BASE);

        JScrollPane scrollPane = new JScrollPane(
                recentRecordingsContainer,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.getViewport().setBackground(BG_BASE);
        scrollPane.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);


        scrollPane.setPreferredSize(new Dimension(0, 0));

        recentRecordingsPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void populateRecentRecordingsPanel() {
        if (recentRecordingsContainer == null) {
            return;
        }

        recentRecordingsContainer.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 5, 0);

        if (recentRecordings.isEmpty()) {
            JLabel noRecordingsLabel = new JLabel("No past recordings.");
            noRecordingsLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
            noRecordingsLabel.setFont(FONT_REGULAR);
            noRecordingsLabel.setForeground(TEXT_MUTED);
            noRecordingsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            recentRecordingsContainer.add(noRecordingsLabel, gbc);
        } else {
            for (PastChallenge challenge : recentRecordings) {
                Pair<String, Color> statusInfo = getChallengeStatusInfo(challenge.status, challenge.stage);

                FeedItem item = new FeedItem(
                        statusInfo.getLeft(),
                        challengeModeToString(challenge.challenge, challenge.mode),
                        formatTimeAgo(challenge.instant),
                        statusInfo.getRight(),
                        challenge.party != null ? challenge.party : new ArrayList<>()
                );

                item.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        LinkBrowser.browse(challengeUrl(Objects.requireNonNull(Challenge.fromId(challenge.challenge)), challenge.id));
                    }
                });

                recentRecordingsContainer.add(item, gbc);
                gbc.gridy++;
            }

            GridBagConstraints glueGbc = new GridBagConstraints();
            glueGbc.gridx = 0;
            glueGbc.gridy = gbc.gridy;
            glueGbc.weighty = 1.0;
            recentRecordingsContainer.add(Box.createVerticalGlue(), glueGbc);
        }

        recentRecordingsContainer.revalidate();
        recentRecordingsContainer.repaint();
    }

    private Pair<String, Color> getChallengeStatusInfo(int status, int stageId) {
        if (status == PastChallenge.STATUS_IN_PROGRESS) {
            return Pair.of("In Progress", Color.WHITE);
        }
        if (status == PastChallenge.STATUS_COMPLETED) {
            return Pair.of("Completed", Color.GREEN);
        }
        if (status == PastChallenge.STATUS_ABANDONED) {
            return Pair.of("Abandoned", Color.GRAY);
        }

        String boss = "Unknown";
        Stage stage = Stage.fromId(stageId);
        if (stage != null) {
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

                case COLOSSEUM_WAVE_1:
                    boss = "Wave 1";
                    break;
                case COLOSSEUM_WAVE_2:
                    boss = "Wave 2";
                    break;
                case COLOSSEUM_WAVE_3:
                    boss = "Wave 3";
                    break;
                case COLOSSEUM_WAVE_4:
                    boss = "Wave 4";
                    break;
                case COLOSSEUM_WAVE_5:
                    boss = "Wave 5";
                    break;
                case COLOSSEUM_WAVE_6:
                    boss = "Wave 6";
                    break;
                case COLOSSEUM_WAVE_7:
                    boss = "Wave 7";
                    break;
                case COLOSSEUM_WAVE_8:
                    boss = "Wave 8";
                    break;
                case COLOSSEUM_WAVE_9:
                    boss = "Wave 9";
                    break;
                case COLOSSEUM_WAVE_10:
                    boss = "Wave 10";
                    break;
                case COLOSSEUM_WAVE_11:
                    boss = "Wave 11";
                    break;
                case COLOSSEUM_WAVE_12:
                    boss = "Sol Heredit";
                    break;

                case MOKHAIOTL_DELVE_1:
                    boss = "Delve 1";
                    break;
                case MOKHAIOTL_DELVE_2:
                    boss = "Delve 2";
                    break;
                case MOKHAIOTL_DELVE_3:
                    boss = "Delve 3";
                    break;
                case MOKHAIOTL_DELVE_4:
                    boss = "Delve 4";
                    break;
                case MOKHAIOTL_DELVE_5:
                    boss = "Delve 5";
                    break;
                case MOKHAIOTL_DELVE_6:
                    boss = "Delve 6";
                    break;
                case MOKHAIOTL_DELVE_7:
                    boss = "Delve 7";
                    break;
                case MOKHAIOTL_DELVE_8:
                    boss = "Delve 8";
                    break;
                case MOKHAIOTL_DELVE_8PLUS:
                    boss = "Delve 8+";
                    break;
            }
        }

        if (stageId >= Stage.INFERNO_WAVE_1.getId() && stageId <= Stage.INFERNO_WAVE_69.getId()) {
            int wave = stageId - Stage.INFERNO_WAVE_1.getId() + 1;
            boss = "Wave " + wave;
        }

        if (status == PastChallenge.STATUS_WIPED) {
            return Pair.of(boss + " Wipe", Color.RED);
        }
        return Pair.of(boss + " Reset", Color.GRAY);
    }

    private String challengeModeToString(int challengeId, int modeId) {
        if (challengeId == Challenge.COLOSSEUM.getId()) {
            return "Colosseum";
        }
        if (challengeId == Challenge.INFERNO.getId()) {
            return "Inferno";
        }
        if (challengeId == Challenge.MOKHAIOTL.getId()) {
            return "Mokhaiotl";
        }

        if (modeId == ChallengeMode.TOB_ENTRY.getId()) {
            return "Entry Mode";
        }
        if (modeId == ChallengeMode.TOB_REGULAR.getId()) {
            return "Regular Mode";
        }
        if (modeId == ChallengeMode.TOB_HARD.getId()) {
            return "Hard Mode";
        }

        if (modeId == ChallengeMode.COX_REGULAR.getId()) {
            return "Cox Regular";
        }
        if (modeId == ChallengeMode.COX_CHALLENGE.getId()) {
            return "CoX CM";
        }

        if (modeId == ChallengeMode.TOA_ENTRY.getId()) {
            return "TOA Entry";
        }
        if (modeId == ChallengeMode.TOA_NORMAL.getId()) {
            return "TOA Normal";
        }
        if (modeId == ChallengeMode.TOA_EXPERT.getId()) {
            return "TOA Expert";
        }

        return "Unknown";
    }

    private String challengeUrl(Challenge challenge, String challengeId) {
        String hostname = WebSocketManager.DEFAULT_BLERT_HOST;

        switch (challenge) {
            case TOB:
                return String.format("%s/raids/tob/%s/overview", hostname, challengeId);
            case COX:
                return String.format("%s/raids/cox/%s/overview", hostname, challengeId);
            case TOA:
                return String.format("%s/raids/toa/%s/overview", hostname, challengeId);
            case COLOSSEUM:
                return String.format("%s/challenges/colosseum/%s/overview", hostname, challengeId);
            case INFERNO:
                return String.format("%s/challenges/inferno/%s/overview", hostname, challengeId);
            case MOKHAIOTL:
                return String.format("%s/challenges/mokhaiotl/%s/overview", hostname, challengeId);
        }

        return hostname;
    }

    private JLabel createHeader(String name) {
        JLabel header = new JLabel(name);
        header.setFont(FONT_SMALLEST);
        header.setForeground(TEXT_MUTED);
        header.setBorder(new EmptyBorder(0, 4, 4, 0));
        return header;
    }

    private String formatTimeAgo(Instant time) {
        long seconds = Duration.between(time, Instant.now()).getSeconds();
        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 30) return days + "d ago";
        return (days / 365) + "y ago";
    }

}