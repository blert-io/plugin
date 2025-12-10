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

import io.blert.client.WebSocketEventHandler;
import io.blert.client.WebSocketManager;
import io.blert.proto.Challenge;
import io.blert.proto.ChallengeMode;
import io.blert.proto.ServerMessage;
import io.blert.proto.Stage;
import joptsimple.internal.Strings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
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

    private final JLabel activeChallengeLabel = new JLabel();
    private final JLabel serverStatusLabel = new JLabel();
    private final JLabel connectionStatusLabel = new JLabel();
    private Timer shutdownLabelTimer;

    private final List<ServerMessage.PastChallenge> recentRecordings = new ArrayList<>();

    private WebSocketEventHandler.Status challengeStatus = WebSocketEventHandler.Status.IDLE;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private @Nullable String connectedUsername = null;
    private Instant shutdownTime = null;

    public BlertPluginPanel(BlertConfig config, WebSocketManager websocketManager) {
        this.config = config;
        this.websocketManager = websocketManager;

        shutdownLabelTimer = new Timer(1000, e -> {
            if (shutdownTime == null) {
                serverStatusLabel.setForeground(Color.GREEN);
                serverStatusLabel.setText("Blert server is online.");
                return;
            }

            Duration timeUntilShutdown = Duration.between(Instant.now(), shutdownTime);
            if (timeUntilShutdown.isNegative()) {
                serverStatusLabel.setText("Server shutting down...");
                serverStatusLabel.setForeground(Color.RED);
            } else {
                String time = DurationFormatUtils.formatDuration(timeUntilShutdown.toMillis(), "HH:mm:ss");
                serverStatusLabel.setForeground(Color.YELLOW);
                serverStatusLabel.setText("Server shutting down in " + time + ".");
            }
        });

        activeChallengeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        serverStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        serverStatusLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        connectionStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public void startPanel() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(false);

        rebuildUserPanel();
        createChallengeStatusPanel(this, Challenge.UNKNOWN_CHALLENGE, null);
        createRecentRecordingsPanel(this);
        populateRecentRecordingsPanel();

        shutdownLabelTimer.start();
    }

    public void stopPanel() {
        shutdownLabelTimer.stop();
    }

    /**
     * Updates the connection state displayed in the panel.
     *
     * @param state    The new connection state.
     * @param username The username if connected, or null otherwise.
     */
    public void updateConnectionState(ConnectionState state, @Nullable String username) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.connectionState = state;
                this.connectedUsername = username;
                rebuildUserPanel();
                revalidate();
                repaint();
            }
        });
    }

    public void updateChallengeStatus(
            WebSocketEventHandler.Status status, Challenge challenge, @Nullable String challengeId) {
        SwingUtilities.invokeLater(() -> {
            synchronized (this) {
                this.challengeStatus = status;
                createChallengeStatusPanel(this, challenge, challengeId);
                revalidate();
                repaint();
            }
        });
    }

    public void setRecentRecordings(@Nullable List<ServerMessage.PastChallenge> recentRecordings) {
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
                if (challengeStatus == WebSocketEventHandler.Status.IDLE) {
                    setIdleActiveChallengeLabelText();
                }
            }
        });
    }

    /**
     * Rebuilds the user panel based on the current connection state.
     */
    private void rebuildUserPanel() {
        if (userPanel != null) {
            remove(userPanel);
        }

        userPanel = new JPanel();
        userPanel.setLayout(new BorderLayout());
        userPanel.setBorder(createSectionBorder());

        userPanel.add(createHeading("Server Status"), BorderLayout.NORTH);

        switch (connectionState) {
            case CONNECTED:
                connectionStatusLabel.setText(wrapHtml(
                        statusIndicator("green") + " Connected as: <b style=\"color: white\">" + connectedUsername + "</b>"));
                connectionStatusLabel.setForeground(Color.GREEN);
                userPanel.add(serverStatusLabel, BorderLayout.CENTER);
                userPanel.add(connectionStatusLabel, BorderLayout.SOUTH);
                break;

            case CONNECTING:
                connectionStatusLabel.setText(wrapHtml(statusIndicator("yellow") + " Connecting..."));
                connectionStatusLabel.setForeground(Color.YELLOW);
                userPanel.add(connectionStatusLabel, BorderLayout.CENTER);
                break;

            case REJECTED:
                connectionStatusLabel.setText(wrapHtml(
                        "<p style=\"text-align: center\">" + statusIndicator("red") + " Connection rejected.<br>" +
                                "Please check your API key in the plugin settings.</p>"));
                connectionStatusLabel.setForeground(Color.RED);
                userPanel.add(connectionStatusLabel, BorderLayout.CENTER);
                userPanel.add(createConnectButtonPanel(), BorderLayout.SOUTH);
                break;

            case UNSUPPORTED_VERSION:
                connectionStatusLabel.setText(wrapHtml(
                        "<p style=\"text-align: center\">" + statusIndicator("red") + " Connection rejected.<br>" +
                                "You are running an outdated version of Blert. " +
                                "Please restart RuneLite to update your plugin.</p>"));
                connectionStatusLabel.setForeground(Color.RED);
                userPanel.add(connectionStatusLabel, BorderLayout.CENTER);
                break;

            case DISCONNECTED:
            default:
                connectionStatusLabel.setText(wrapHtml(statusIndicator("red") + " Not connected."));
                connectionStatusLabel.setForeground(Color.RED);
                userPanel.add(connectionStatusLabel, BorderLayout.CENTER);

                if (Strings.isNullOrEmpty(config.apiKey())) {
                    JLabel apiKeyLabel = new JLabel(wrapHtml(
                            "<p style=\"text-align: center\">Enter your Blert API key in the plugin settings.</p>"));
                    apiKeyLabel.setForeground(Color.WHITE);
                    apiKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    userPanel.add(apiKeyLabel, BorderLayout.SOUTH);
                } else {
                    userPanel.add(createConnectButtonPanel(), BorderLayout.SOUTH);
                }
                break;
        }

        add(userPanel, BorderLayout.NORTH);
    }

    private @NonNull JPanel createConnectButtonPanel() {
        JPanel connectButtonPanel = new JPanel();
        connectButtonPanel.setLayout(new BorderLayout());
        connectButtonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            // Update state to show connecting status
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
                            // Connection failed and no other state update came through.
                            updateConnectionState(ConnectionState.DISCONNECTED, null);
                        }
                    } catch (Exception e) {
                        log.error("Error getting connection result", e);
                        updateConnectionState(ConnectionState.DISCONNECTED, null);
                    }
                }
            };
            worker.execute();
        });
        connectButtonPanel.add(connectButton, BorderLayout.CENTER);
        return connectButtonPanel;
    }

    private void createChallengeStatusPanel(JPanel parent, Challenge challenge, @Nullable String challengeId) {
        if (challengeStatusPanel != null) {
            parent.remove(challengeStatusPanel);
        }

        challengeStatusPanel = new JPanel();
        challengeStatusPanel.setLayout(new BorderLayout());
        challengeStatusPanel.setBorder(createSectionBorder());

        JPanel currentStatePanel = createCurrentChallengeStatePanel();

        if (challengeStatus == WebSocketEventHandler.Status.CHALLENGE_ACTIVE) {
            JPanel actionsPanel = createChallengeActionsPanel(challenge, challengeId);
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

            int index = 0;
            for (ServerMessage.PastChallenge challenge : recentRecordings) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                constraints.gridy = index;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.weightx = 1.0;
                constraints.insets = new Insets(0, 0, 0, 6);
                recentRecordingsList.add(createPastChallengeEntry(challenge, index), constraints);
                index++;
            }

            recentRecordingsContainer.add(recentRecordingsList, BorderLayout.NORTH);
        }
    }

    @NonNull
    private JPanel createCurrentChallengeStatePanel() {
        JPanel currentStatePanel = new JPanel();
        currentStatePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        currentStatePanel.setLayout(new BorderLayout());

        if (connectionState != ConnectionState.CONNECTED) {
            activeChallengeLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            activeChallengeLabel.setText(wrapHtml(
                    "<p style=\"text-align:center\">Connect to server to record challenges.</p>"));
        } else {
            switch (challengeStatus) {
                case IDLE:
                    setIdleActiveChallengeLabelText();
                    break;
                case CHALLENGE_STARTING:
                    activeChallengeLabel.setText("Starting...");
                    activeChallengeLabel.setForeground(Color.YELLOW);
                    break;
                case CHALLENGE_ENDING:
                    activeChallengeLabel.setText("Ending...");
                    activeChallengeLabel.setForeground(Color.YELLOW);
                    break;
                case CHALLENGE_ACTIVE:
                    activeChallengeLabel.setText("Streaming challenge data!");
                    activeChallengeLabel.setForeground(Color.GREEN);
                    break;
            }
        }

        currentStatePanel.add(createHeading("Current Challenge"), BorderLayout.NORTH);
        currentStatePanel.add(activeChallengeLabel, BorderLayout.CENTER);
        return currentStatePanel;
    }

    @NonNull
    private JPanel createChallengeActionsPanel(Challenge challenge, @Nullable String challengeId) {
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new GridLayout(1, 0, 5, 0));

        JButton linkButton = new JButton("View Raid");
        linkButton.addActionListener(e -> LinkBrowser.browse(challengeUrl(challenge, challengeId)));
        actionsPanel.add(linkButton);

        JButton copyLinkButton = new JButton("Copy Link");
        copyLinkButton.addActionListener(e ->
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(new StringSelection(challengeUrl(challenge, challengeId)), null));
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
        if (status == ServerMessage.PastChallenge.Status.ABANDONED) {
            return Pair.of("Abandoned", Color.GRAY);
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
        }

        if (status == ServerMessage.PastChallenge.Status.WIPED) {
            return Pair.of(boss + " Wipe", Color.RED);
        }
        return Pair.of(boss + " Reset", Color.GRAY);
    }

    private String challengeModeToString(Challenge challenge, ChallengeMode mode) {
        if (challenge == Challenge.COLOSSEUM) {
            return "Colosseum";
        }
        if (challenge == Challenge.INFERNO) {
            return "Inferno";
        }

        switch (mode) {
            case TOB_ENTRY:
                return "Entry Mode";
            case TOB_REGULAR:
                return "Regular Mode";
            case TOB_HARD:
                return "Hard Mode";

            case COX_REGULAR:
                return "Cox Regular";
            case COX_CHALLENGE:
                return "CoX CM";

            case TOA_ENTRY:
                return "TOA Entry";
            case TOA_NORMAL:
                return "TOA Normal";
            case TOA_EXPERT:
                return "TOA Expert";
        }

        return "Unknown";
    }

    private JPanel createPastChallengeEntry(ServerMessage.PastChallenge challenge, int index) {
        JPanel challengePanel = new JPanel();
        challengePanel.setLayout(new BorderLayout());
        challengePanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        Color bgColor = (index % 2 == 0)
                ? ColorScheme.DARKER_GRAY_COLOR
                : ColorScheme.DARK_GRAY_COLOR;
        challengePanel.setBackground(bgColor);
        challengePanel.setOpaque(true);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayout(1, 0, 5, 0));
        statusPanel.setOpaque(false);

        Pair<String, Color> statusInfo = getChallengeStatusInfo(challenge.getStatus(), challenge.getStage());
        JLabel statusLabel = new JLabel();
        statusLabel.setForeground(statusInfo.getRight());
        statusLabel.setText(statusInfo.getLeft());
        statusPanel.add(statusLabel);

        JLabel modeLabel = new JLabel();
        modeLabel.setForeground(Color.WHITE);
        modeLabel.setFont(modeLabel.getFont().deriveFont(Font.ITALIC));
        modeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        modeLabel.setText(challengeModeToString(challenge.getChallenge(), challenge.getMode()));
        statusPanel.add(modeLabel);

        challengePanel.add(statusPanel, BorderLayout.NORTH);

        JPanel partyPanel = new JPanel();
        partyPanel.setLayout(new BorderLayout());
        partyPanel.setBorder(new EmptyBorder(8, 0, 8, 0));
        partyPanel.setOpaque(false);
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

        challengePanel.add(createChallengeActionsPanel(challenge.getChallenge(), challenge.getId()), BorderLayout.SOUTH);

        return challengePanel;
    }

    void setIdleActiveChallengeLabelText() {
        if (this.shutdownTime != null) {
            activeChallengeLabel.setForeground(Color.RED);
            activeChallengeLabel.setText(
                    wrapHtml("<p style=\"text-align:center\">New challenges cannot be started at this time.</p>"));
        } else {
            activeChallengeLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            activeChallengeLabel.setText(wrapHtml(
                    "<p style=\"text-align:center\">Begin a challenge to start recording.</p>"));
        }
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
            case UNRECOGNIZED:
            case UNKNOWN_CHALLENGE:
                return hostname;
        }

        return hostname;
    }

    /**
     * Creates a border for a section panel with a bottom separator line.
     */
    private static Border createSectionBorder() {
        Border padding = new EmptyBorder(5, 5, 10, 5);
        Border separator = new MatteBorder(0, 0, 1, 0, ColorScheme.LIGHT_GRAY_COLOR);
        return new CompoundBorder(separator, padding);
    }

    /**
     * Returns an HTML colored circle indicator.
     */
    private static String statusIndicator(String color) {
        return "<span style=\"color: " + color + "\">●</span>";
    }

    static String wrapHtml(String content) {
        return "<html><body>" + content + "</body></html>";
    }
}
