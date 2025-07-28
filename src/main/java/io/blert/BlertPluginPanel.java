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
import io.blert.client.WebsocketManager;
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
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BlertPluginPanel extends PluginPanel {
    private final BlertConfig config;
    private final WebsocketManager websocketManager;

    private JPanel userPanel;
    private JPanel challengeStatusPanel;
    private JPanel recentRecordingsPanel;
    private JPanel recentRecordingsContainer;

    private final JLabel activeChallengeLabel = new JLabel();
    private final JLabel severStatusLabel = new JLabel();
    private final JLabel connectionStatusLabel = new JLabel();
    private Timer shutdownLabelTimer;

    private final List<ServerMessage.PastChallenge> recentRecordings = new ArrayList<>();

    private WebsocketEventHandler.Status challengeStatus = WebsocketEventHandler.Status.IDLE;
    private boolean unsupportedVersion = false;
    private Instant shutdownTime = null;

    public BlertPluginPanel(BlertConfig config, WebsocketManager websocketManager) {
        this.config = config;
        this.websocketManager = websocketManager;

        shutdownLabelTimer = new Timer(1000, e -> {
            if (shutdownTime == null) {
                severStatusLabel.setForeground(Color.GREEN);
                severStatusLabel.setText("Blert server is online.");
                return;
            }

            Duration timeUntilShutdown = Duration.between(Instant.now(), shutdownTime);
            if (timeUntilShutdown.isNegative()) {
                severStatusLabel.setText("Server shutting down...");
                severStatusLabel.setForeground(Color.RED);
            } else {
                String time = DurationFormatUtils.formatDuration(timeUntilShutdown.toMillis(), "HH:mm:ss");
                severStatusLabel.setForeground(Color.YELLOW);
                severStatusLabel.setText("Server shutting down in " + time + ".");
            }
        });

        activeChallengeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        severStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        severStatusLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        connectionStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public void startPanel() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(false);

        createUserPanel(null);
        createChallengeStatusPanel(this, Challenge.UNKNOWN_CHALLENGE, null);
        createRecentRecordingsPanel(this);
        populateRecentRecordingsPanel();

        shutdownLabelTimer.start();
    }

    public void stopPanel() {
        shutdownLabelTimer.stop();
    }

    public synchronized void updateUser(@Nullable String username) {
        createUserPanel(username);
    }

    public synchronized void updateChallengeStatus(
            WebsocketEventHandler.Status status, Challenge challenge, @Nullable String challengeId) {
        this.challengeStatus = status;
        createChallengeStatusPanel(this, challenge, challengeId);
    }

    public void setRecentRecordings(@Nullable List<ServerMessage.PastChallenge> recentRecordings) {
        this.recentRecordings.clear();
        if (recentRecordings != null) {
            this.recentRecordings.addAll(recentRecordings);
        }
        populateRecentRecordingsPanel();
    }

    public synchronized void setShutdownTime(@Nullable Instant shutdownTime) {
        this.shutdownTime = shutdownTime;
        if (challengeStatus == WebsocketEventHandler.Status.IDLE) {
            setIdleActiveChallengeLabelText();
        }
    }

    public synchronized void setUnsupportedVersion(boolean unsupportedVersion) {
        this.unsupportedVersion = unsupportedVersion;
        createUserPanel(null);
    }

    private void createUserPanel(@Nullable String username) {
        if (userPanel != null) {
            remove(userPanel);
        }

        userPanel = new JPanel();
        userPanel.setLayout(new BorderLayout());
        userPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        userPanel.add(createHeading("Server Status"), BorderLayout.NORTH);

        if (username != null) {
            connectionStatusLabel.setText(wrapHtml("Connected as: <b style=\"color: white\">" + username + "</b>"));
            connectionStatusLabel.setForeground(Color.GREEN);
            userPanel.add(severStatusLabel, BorderLayout.CENTER);
            userPanel.add(connectionStatusLabel, BorderLayout.SOUTH);
        } else {
            connectionStatusLabel.setText("Not connected.");
            connectionStatusLabel.setForeground(Color.RED);
            userPanel.add(connectionStatusLabel, BorderLayout.CENTER);

            if (Strings.isNullOrEmpty(config.apiKey())) {
                JLabel apiKeyLabel = new JLabel(wrapHtml(
                        "<p style=\"text-align: center\">Enter your Blert API key in the plugin settings.</p>"));
                apiKeyLabel.setForeground(Color.WHITE);
                apiKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                userPanel.add(apiKeyLabel, BorderLayout.SOUTH);
            } else if (unsupportedVersion) {
                connectionStatusLabel.setText(wrapHtml(
                        "<p style=\"text-align: center\">You are running an outdated version of Blert. " +
                                "Please restart Runelite to update your plugin.</p>"));
            } else {
                JPanel connectButtonPanel = createConnectButtonPanel();
                userPanel.add(connectButtonPanel, BorderLayout.SOUTH);
            }
        }

        add(userPanel, BorderLayout.NORTH);
    }

    private @NonNull JPanel createConnectButtonPanel() {
        JPanel connectButtonPanel = new JPanel();
        connectButtonPanel.setLayout(new BorderLayout());
        connectButtonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> {
            connectButton.setEnabled(false);
            connectButton.setText("Connecting...");

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                private boolean success = false;

                @Override
                protected Void doInBackground() {
                    try {
                        success = websocketManager.open().get();
                    } catch (Exception e) {
                        log.error("Error connecting to Blert server", e);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    super.done();
                    connectButton.setEnabled(true);
                    connectButton.setText("Connect");
                    if (!success) {
                        connectionStatusLabel.setText("Failed to connect to server.");
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
        challengeStatusPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel currentStatePanel = createCurrentChallengeStatePanel();

        if (challengeStatus == WebsocketEventHandler.Status.CHALLENGE_ACTIVE) {
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

    @NonNull
    private JPanel createCurrentChallengeStatePanel() {
        JPanel currentStatePanel = new JPanel();
        currentStatePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        currentStatePanel.setLayout(new BorderLayout());

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
        if (challenge == Challenge.MOKHAIOTL) {
            return "Mokhaiotl";
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
        modeLabel.setText(challengeModeToString(challenge.getChallenge(), challenge.getMode()));
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

        challengePanel.add(createChallengeActionsPanel(challenge.getChallenge(), challenge.getId()), BorderLayout.SOUTH);

        return challengePanel;
    }

    void setIdleActiveChallengeLabelText() {
        if (this.shutdownTime != null) {
            activeChallengeLabel.setForeground(Color.RED);
            activeChallengeLabel.setText(
                    wrapHtml("<p style=\"text-align:center\">New challenges cannot be started at this time.</p>"));
        } else {
            activeChallengeLabel.setForeground(Color.LIGHT_GRAY);
            activeChallengeLabel.setText("Not in a PvM challenge.");
        }
    }

    private String challengeUrl(Challenge challenge, String challengeId) {
        String hostname = WebsocketManager.DEFAULT_BLERT_HOST;

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

    static String wrapHtml(String content) {
        return "<html><body>" + content + "</body></html>";
    }
}
