import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

 



public class DashboardMain {
    private static final Color BACKGROUND = new Color(12, 21, 35);
    private static final Color PANEL = new Color(23, 36, 55);
    private static final Color PANEL_ALT = new Color(29, 46, 68);
    private static final Color TEXT = new Color(231, 239, 248);
    private static final Color MUTED = new Color(165, 184, 204);
    private static final Color ACCENT = new Color(67, 209, 173);
    private static final Color ROUTE = new Color(255, 202, 69);

    private JFrame frame;
    private JComboBox<String> startBox;
    private JComboBox<String> destinationBox;
    private JComboBox<String> scenarioBox;
    private JComboBox<String> vehicleBox;
    private final Icon[] vehicleIcons = {new VehicleIcon(0), new VehicleIcon(1), new VehicleIcon(2)};
    private JLabel routeTimeValue;
    private JLabel arrivalValue;
    private JLabel savingValue;
    private JLabel preemptionValue;
    private JLabel routeTitle;
    private JTextArea routeDetails;
    private JTextArea insightDetails;
    private JTextArea conditionsOutput;
    private JTextArea reviewOutput;
    private JTextArea finderOutput;
    private JTable roadReviewTable;
    private RoadTableModel roadReviewModel;
    private JTextArea roadReviewStatus;
    private JCheckBox showRoadTimesOnMap;
    private boolean heavyRoadFallback;
    private JLabel playbackLabel;
    private JLabel liveStatusLabel;
    private JButton playButton;
    private JComboBox<String> roadBox;
    private JComboBox<String> reservationNodeBox;
    private JSpinner densityInput;
    private JSpinner congestionThresholdInput;
    private boolean greenCorridorActivated;
    private int congestionThreshold = 70;
    private JCheckBox roadOpenInput;
    private JCheckBox sustainedInput;
    private NetworkPanel networkPanel;
    private javax.swing.Timer animation;

    private Graph graph;
    private SignalControl signals;
    private SignalScheduler scheduler;
    private JSplitPane mainSplitPane;
    private List<Graph.Route> routes;
    private SignalScheduler.Plan plan;
    private int start;
    private int destination;
    private double playbackProgress;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DashboardMain().show());
    }

    private void show() {
        configureLookAndFeel();
        resetScenario();

        frame = new JFrame("Emergency Vehicle Green Corridor | Simulated Road Status");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1120, 720));
        frame.setSize(1400, 860);
        frame.setLocationByPlatform(true);
        frame.setContentPane(buildContent());
        installKeyboardShortcuts();
        refreshRoute();
        frame.setVisible(true);
    }

    private void configureLookAndFeel() {
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.foreground", BACKGROUND);
        UIManager.put("Button.background", ACCENT);
        UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.BOLD, 11));
        UIManager.put("Button.margin", new Insets(2, 8, 2, 8));
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.background", PANEL_ALT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.background", PANEL);
        UIManager.put("ToolTip.background", new Color(245, 249, 253));
        UIManager.put("ToolTip.foreground", BACKGROUND);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", PANEL_ALT);
        UIManager.put("ScrollBar.thumb", new Color(67, 91, 119));
        UIManager.put("ScrollBar.track", BACKGROUND);
        UIManager.put("Spinner.background", PANEL_ALT);
        UIManager.put("Spinner.foreground", TEXT);
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(18, 22, 18, 22));
        root.add(header(), BorderLayout.NORTH);

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMapArea(), buildSidePanel());
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(BACKGROUND);
        mainSplitPane.setContinuousLayout(true);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.setDividerSize(14);
        mainSplitPane.setResizeWeight(0.66);
        mainSplitPane.setToolTipText("Drag the divider to resize the map and tabs; use the arrows to expand one side.");
        SwingUtilities.invokeLater(() -> mainSplitPane.setDividerLocation(0.66));
        root.add(mainSplitPane, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("EMERGENCY VEHICLE GREEN CORRIDOR");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        heading.setForeground(TEXT);
        JLabel subtitle = new JLabel("Interactive Emergency Response Simulation  •  ROAD STATUS + TRAVEL-TIME DASHBOARD");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        title.add(heading);
        title.add(Box.createVerticalStrut(3));
        title.add(subtitle);
        header.add(title, BorderLayout.WEST);

        JLabel notice = new JLabel("SIMULATION ONLY  •  No live traffic data  •  No real signal control");
        notice.setOpaque(true);
        notice.setBackground(new Color(73, 46, 24));
        notice.setForeground(new Color(255, 216, 144));
        notice.setBorder(new CompoundBorder(new LineBorder(new Color(156, 108, 44)), new EmptyBorder(9, 12, 9, 12)));
        header.add(notice, BorderLayout.EAST);
        return header;
    }

    private JComponent buildMapArea() {
        JPanel area = panel(new BorderLayout(0, 10));
        area.setMinimumSize(new Dimension(420, 1));
        JPanel mapHeader = new JPanel(new BorderLayout(10, 0));
        mapHeader.setOpaque(false);
        routeTitle = new JLabel();
        routeTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        routeTitle.setForeground(TEXT);
        mapHeader.add(routeTitle, BorderLayout.CENTER);
        JPanel mapFooter = new JPanel(new BorderLayout(8, 0));
        mapFooter.setOpaque(false);
        JLabel mapHint = new JLabel("LEFT-CLICK: SET START   •   RIGHT-CLICK: SET DESTINATION");
        mapHint.setForeground(MUTED);
        mapHint.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        mapFooter.add(mapHint, BorderLayout.CENTER);
        showRoadTimesOnMap = new JCheckBox("SHOW ROAD TIMES", true);
        showRoadTimesOnMap.setOpaque(false);
        showRoadTimesOnMap.setForeground(MUTED);
        showRoadTimesOnMap.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        showRoadTimesOnMap.setToolTipText("Show the current estimated time for every road segment directly on the map.");
        showRoadTimesOnMap.addActionListener(e -> networkPanel.repaint());
        mapFooter.add(showRoadTimesOnMap, BorderLayout.EAST);
        mapHeader.add(mapFooter, BorderLayout.SOUTH);
        area.add(mapHeader, BorderLayout.NORTH);
        networkPanel = new NetworkPanel();
        networkPanel.setBorder(new LineBorder(new Color(51, 72, 96)));
        area.add(networkPanel, BorderLayout.CENTER);
        return area;
    }

    private JComponent buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setBackground(BACKGROUND);
        side.setMinimumSize(new Dimension(340, 1));
        JLabel resizeHint = new JLabel("DRAG THE DIVIDER TO RESIZE MAP / TABS", SwingConstants.CENTER);
        resizeHint.setForeground(MUTED);
        resizeHint.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        resizeHint.setBorder(new EmptyBorder(3, 2, 3, 2));
        side.add(resizeHint, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        tabs.setMinimumSize(new Dimension(340, 1));
        tabs.addTab("ROUTE", scroll(routePanel()));
        tabs.addTab("CONDITIONS", scroll(conditionsPanel()));
        tabs.addTab("ROAD REVIEW", scroll(roadReviewPanel()));
        tabs.addTab("FIND", scroll(finderPanel()));
        side.add(tabs, BorderLayout.CENTER);
        return side;
    }

    private JComponent routePanel() {
        JPanel side = new ResponsivePanel();
        side.setBackground(BACKGROUND);
        side.add(controlPanel());
        side.add(Box.createVerticalStrut(10));
        side.add(metricsPanel());
        side.add(Box.createVerticalStrut(10));
        side.add(detailsPanel());
        side.add(Box.createVerticalStrut(10));
        side.add(insightsPanel());
        return side;
    }

    private JScrollPane scroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(BACKGROUND);
        return scroll;
    }

    private JComponent conditionsPanel() {
        JPanel content = stack();
        JPanel road = panel(new GridBagLayout());
        GridBagConstraints c = constraints();
        c.gridwidth = 2;
        road.add(sectionLabel("ROAD CONDITIONS"), c);
        c.gridy = 1; c.gridwidth = 1; c.insets = new Insets(10, 0, 4, 8);
        road.add(label("Road"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 4, 0);
        roadBox = new JComboBox<>(roadNames());
        roadBox.addActionListener(e -> loadSelectedRoad());
        road.add(roadBox, c);
        c.gridx = 0; c.gridy = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8);
        road.add(label("Density %"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0);
        densityInput = spinner(10, 0, 100, 1); road.add(densityInput, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.insets = new Insets(4, 0, 4, 0);
        roadOpenInput = new JCheckBox("Road is open"); roadOpenInput.setBackground(PANEL); roadOpenInput.setForeground(TEXT); road.add(roadOpenInput, c);
        c.gridy = 4; sustainedInput = new JCheckBox("Use this density for all three recent readings"); sustainedInput.setBackground(PANEL); sustainedInput.setForeground(TEXT); road.add(sustainedInput, c);
        c.gridy = 5; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 0, 0);
        JButton applyRoad = new JButton("APPLY ROAD UPDATE"); applyRoad.addActionListener(e -> applyRoadUpdate()); road.add(applyRoad, c);
        c.gridy = 6; c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 4, 0);
        road.add(sectionLabel("ROUTE DECISION THRESHOLD"), c);
        c.gridy = 7; c.gridwidth = 1; c.gridx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8);
        road.add(label("Congestion threshold (%)"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0);
        congestionThresholdInput = spinner(congestionThreshold, 1, 100, 5); road.add(congestionThresholdInput, c);
        c.gridy = 8; c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 0, 0);
        JButton applyThreshold = new JButton("APPLY DECISION THRESHOLD");
        applyThreshold.addActionListener(e -> {
            congestionThreshold = value(congestionThresholdInput);
            refreshRoute();
            conditionsOutput.setText("Route decision threshold set to " + congestionThreshold + "% average road density.\n" + decisionSummary());
        });
        road.add(applyThreshold, c);
        content.add(road);
        content.add(Box.createVerticalStrut(10));

        JPanel signal = panel(new GridBagLayout());
        GridBagConstraints s = constraints(); s.gridwidth = 2; signal.add(sectionLabel("SIGNAL RESERVATIONS"), s);
        s.gridx = 0; s.gridy = 1; s.gridwidth = 1; s.weightx = 0; s.fill = GridBagConstraints.NONE; s.insets = new Insets(4, 0, 4, 8);
        signal.add(label("Reservation location"), s);
        s.gridx = 1; s.weightx = 1; s.fill = GridBagConstraints.HORIZONTAL; s.insets = new Insets(4, 0, 4, 0);
        reservationNodeBox = new JComboBox<>(nodeNames()); signal.add(reservationNodeBox, s);
        JTextField startTime = new JTextField("0"), endTime = new JTextField("30"), reservationLabel = new JTextField("Planned priority movement");
        addFormRow(signal, s, 2, "Start / end (s)", pair(startTime, endTime));
        addFormRow(signal, s, 3, "Reservation label", reservationLabel);
        s.gridx = 0; s.gridy = 4; s.gridwidth = 2; s.weightx = 1; s.fill = GridBagConstraints.HORIZONTAL; s.insets = new Insets(8, 0, 0, 0);
        JButton addReservation = new JButton("ADD RESERVATION");
        addReservation.addActionListener(e -> addReservation(startTime, endTime, reservationLabel)); signal.add(addReservation, s);
        content.add(signal);
        content.add(Box.createVerticalStrut(10));

        JPanel actions = panel(new GridLayout(1, 2, 8, 0));
        JButton reset = new JButton("RESTORE SAMPLE"); reset.addActionListener(e -> restoreSample()); actions.add(reset);
        JButton show = new JButton("SHOW ROAD STATUS"); show.addActionListener(e -> showRoadStatus()); actions.add(show);
        content.add(actions);
        content.add(Box.createVerticalStrut(10));
        conditionsOutput = textArea(); conditionsOutput.setRows(8); content.add(outputPanel("UPDATE STATUS", conditionsOutput, 220));
        loadSelectedRoad();
        return content;
    }

    private JComponent roadReviewPanel() {
        JPanel content = stack();
        content.add(sectionLabel("EDIT ROAD CONDITIONS & TRAVEL TIMES"));
        content.add(Box.createVerticalStrut(6));
        roadReviewModel = new RoadTableModel();
        roadReviewTable = new JTable(roadReviewModel);
        roadReviewTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        roadReviewTable.setRowHeight(25);
        roadReviewTable.setFillsViewportHeight(true);
        roadReviewTable.setForeground(TEXT);
        roadReviewTable.setBackground(PANEL);
        roadReviewTable.setGridColor(new Color(45, 67, 92));
        roadReviewTable.setSelectionBackground(new Color(51, 82, 110));
        roadReviewTable.setSelectionForeground(Color.WHITE);
        roadReviewTable.getTableHeader().setBackground(PANEL_ALT);
        roadReviewTable.getTableHeader().setForeground(TEXT);
        roadReviewTable.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        int[] widths = {158, 68, 52, 90, 94};
        for (int i = 0; i < widths.length; i++) roadReviewTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        DefaultTableCellRenderer roadRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                Road road = graph.roads.get(modelRow);
                if (!selected) {
                    if (!road.available || road.density() >= 70) component.setBackground(new Color(91, 39, 49));
                    else if (road.density() >= 35) component.setBackground(new Color(88, 66, 37));
                    else component.setBackground(PANEL);
                    component.setForeground(TEXT);
                }
                if (component instanceof JLabel) ((JLabel) component).setBorder(new EmptyBorder(0, 5, 0, 5));
                return component;
            }
        };
        roadReviewTable.setDefaultRenderer(Object.class, roadRenderer);
        roadReviewTable.setDefaultRenderer(Number.class, roadRenderer);
        roadReviewTable.setDefaultRenderer(Double.class, roadRenderer);
        roadReviewTable.setDefaultRenderer(Integer.class, roadRenderer);
        JScrollPane roadTableScroll = new JScrollPane(roadReviewTable);
        roadTableScroll.setPreferredSize(new Dimension(390, 285));
        roadTableScroll.setBorder(new LineBorder(new Color(45, 67, 92)));
        roadTableScroll.getViewport().setBackground(PANEL);
        content.add(roadTableScroll);
        content.add(Box.createVerticalStrut(5));
        JTextArea tableTip = hintArea("Edit Traffic % or Open? directly. ETA and status update automatically.");
        content.add(tableTip);
        content.add(Box.createVerticalStrut(8));
        JButton applyTable = new JButton("APPLY / RECALCULATE FROM ROAD TABLE");
        applyTable.addActionListener(e -> applyRoadTableEdits());
        content.add(applyTable);
        content.add(Box.createVerticalStrut(10));
        content.add(sectionLabel("QUICK TRAFFIC INPUT — SELECT A ROAD ROW"));
        content.add(Box.createVerticalStrut(6));
        JPanel trafficPresets = new JPanel(new GridLayout(1, 3, 6, 0));
        trafficPresets.setOpaque(false);
        JButton lightTraffic = new JButton("LIGHT 20%");
        lightTraffic.setBackground(new Color(85, 167, 151));
        lightTraffic.addActionListener(e -> setRoadTrafficPreset(20, "LIGHT"));
        JButton moderateTraffic = new JButton("MODERATE 50%");
        moderateTraffic.setBackground(new Color(246, 158, 55));
        moderateTraffic.addActionListener(e -> setRoadTrafficPreset(50, "MODERATE"));
        JButton heavyTraffic = new JButton("HEAVY 85%");
        heavyTraffic.setBackground(new Color(220, 45, 58));
        heavyTraffic.setForeground(Color.WHITE);
        heavyTraffic.addActionListener(e -> setRoadTrafficPreset(85, "HEAVY"));
        trafficPresets.add(lightTraffic);
        trafficPresets.add(moderateTraffic);
        trafficPresets.add(heavyTraffic);
        content.add(trafficPresets);
        roadReviewStatus = hintArea("Presets update the selected road, map color, and route immediately.");
        content.add(Box.createVerticalStrut(4));
        content.add(roadReviewStatus);
        content.add(Box.createVerticalStrut(10));

        JPanel controls = panel(new GridBagLayout());
        GridBagConstraints c = constraints(); c.gridwidth = 2; controls.add(sectionLabel("ROAD & NETWORK REVIEW"), c);
        c.gridy = 1; c.gridwidth = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 4, 6);
        JButton refresh = new JButton("REFRESH REVIEW"); refresh.addActionListener(e -> showRoadReview()); controls.add(refresh, c);
        c.gridx = 1; c.insets = new Insets(10, 0, 4, 0);
        JButton cityTimes = new JButton("CITY TIMES"); cityTimes.addActionListener(e -> showCityTimes()); controls.add(cityTimes, c);
        JSpinner fromRoad = spinner(1, 1, graph.roads.size(), 1), toRoad = spinner(graph.roads.size(), 1, graph.roads.size(), 1);
        addFormRow(controls, c, 2, "Road range", pair(fromRoad, toRoad));
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 4, 0);
        JButton range = new JButton("CHECK ROAD RANGE"); range.addActionListener(e -> showRoadRange(value(fromRoad), value(toRoad))); controls.add(range, c);
        c.gridy = 4; c.insets = new Insets(4, 0, 0, 0);
        JButton detailed = new JButton("SHOW DETAILED FEATURE VIEW"); detailed.addActionListener(e -> showFeatureDetails()); controls.add(detailed, c);
        c.gridy = 5; c.insets = new Insets(8, 0, 0, 0);
        JButton connectivity = new JButton("OPEN-ROAD CONNECTION GROUPS"); connectivity.addActionListener(e -> showDSUConnectivity()); controls.add(connectivity, c);
        content.add(controls);
        content.add(Box.createVerticalStrut(10));
        reviewOutput = textArea(); reviewOutput.setRows(20); content.add(outputPanel("SIMULATED ROAD REVIEW", reviewOutput, 300));
        return content;
    }

    private JComponent finderPanel() {
        JPanel content = stack();
        JPanel finder = panel(new GridBagLayout());
        GridBagConstraints c = constraints(); c.gridwidth = 2; finder.add(sectionLabel("LOCATION FINDER"), c);
        JTextField name = new JTextField();
        addFormRow(finder, c, 1, "Name or prefix", name);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 8, 0);
        JButton nameSearch = new JButton("FIND BY NAME"); nameSearch.addActionListener(e -> findByName(name.getText())); finder.add(nameSearch, c);
        JSpinner x = spinner(0, -100, 100, 1), y = spinner(0, -100, 100, 1);
        addFormRow(finder, c, 3, "Coordinates", pair(x, y));
        c.gridx = 0; c.gridy = 4; c.gridwidth = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(8, 0, 0, 6);
        JButton nearest = new JButton("NEAREST LOCATION"); nearest.addActionListener(e -> findNearest(value(x), value(y))); finder.add(nearest, c);
        c.gridx = 1; c.insets = new Insets(8, 0, 0, 0);
        JButton hospital = new JButton("NEAREST HOSPITAL"); hospital.addActionListener(e -> findHospital(value(x), value(y))); finder.add(hospital, c);
        content.add(finder);
        content.add(Box.createVerticalStrut(10));
        finderOutput = textArea(); finderOutput.setRows(15); content.add(outputPanel("FIND RESULTS", finderOutput, 230));
        return content;
    }

    private JPanel stack() {
        JPanel panel = new ResponsivePanel(); panel.setBackground(BACKGROUND); panel.setBorder(new EmptyBorder(8, 8, 8, 8)); return panel;
    }

    private JTextArea hintArea(String text) {
        JTextArea hint = new JTextArea(text);
        hint.setEditable(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setOpaque(false);
        hint.setForeground(MUTED);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        hint.setBorder(new EmptyBorder(0, 0, 0, 0));
        hint.setRows(2);
        return hint;
    }

    private final class ResponsivePanel extends JPanel implements Scrollable {
        ResponsivePanel() { setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); }

        @Override protected void addImpl(Component component, Object constraints, int index) {
            super.addImpl(component, constraints, index);
            if (component instanceof JComponent child && !(component instanceof Box.Filler)) {
                Dimension preferred = child.getPreferredSize();
                Dimension maximum = child.getMaximumSize();
                child.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.min(preferred.height, maximum.height)));
                child.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(440, 600); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(24, visibleRect.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static final class VehicleIcon implements Icon {
        private final int style;

        VehicleIcon(int style) { this.style = style; }
        @Override public int getIconWidth() { return 38; }
        @Override public int getIconHeight() { return 24; }

        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color outline = new Color(20, 29, 42);
            Color glass = new Color(150, 214, 239);

            if (style == 0) {
                g2.setColor(new Color(239, 246, 250));
                g2.fillRoundRect(3, 6, 31, 12, 4, 4);
                g2.setColor(glass);
                g2.fillRect(24, 8, 7, 5);
                g2.setColor(new Color(218, 50, 61));
                g2.fillRect(12, 8, 3, 8);
                g2.fillRect(9, 10, 9, 3);
                g2.fillRoundRect(14, 3, 10, 3, 2, 2);
            } else if (style == 1) {
                g2.setColor(new Color(208, 49, 54));
                g2.fillRoundRect(3, 7, 32, 11, 3, 3);
                g2.setColor(glass);
                g2.fillRect(5, 9, 7, 5);
                g2.setColor(new Color(255, 190, 63));
                g2.fillRect(22, 5, 12, 2);
                g2.drawLine(24, 4, 24, 7);
                g2.drawLine(28, 4, 28, 7);
                g2.drawLine(32, 4, 32, 7);
            } else {
                g2.setColor(new Color(41, 73, 115));
                g2.fillRoundRect(3, 9, 32, 9, 6, 5);
                g2.setColor(new Color(229, 239, 247));
                g2.fillRoundRect(11, 6, 15, 7, 4, 4);
                g2.setColor(glass);
                g2.fillRect(14, 7, 5, 4);
                g2.fillRect(20, 7, 4, 4);
                g2.setColor(new Color(219, 52, 63));
                g2.fillRoundRect(15, 3, 4, 3, 2, 2);
                g2.setColor(new Color(66, 161, 239));
                g2.fillRoundRect(20, 3, 4, 3, 2, 2);
            }

            g2.setColor(outline);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(3, style == 2 ? 9 : (style == 1 ? 7 : 6), 31, style == 2 ? 9 : (style == 1 ? 11 : 12), 4, 4);
            g2.fillOval(8, 16, 6, 6);
            g2.fillOval(26, 16, 6, 6);
            g2.setColor(new Color(194, 204, 214));
            g2.fillOval(10, 18, 2, 2);
            g2.fillOval(28, 18, 2, 2);
            g2.dispose();
        }
    }

    private JComponent outputPanel(String title, JTextArea output, int initialHeight) {
        return new ResizableOutputPanel(title, output, initialHeight);
    }

    private final class ResizableOutputPanel extends JPanel {
        private final JLabel resizeLabel;
        private int dragStartY;
        private int dragStartHeight;

        ResizableOutputPanel(String title, JTextArea output, int initialHeight) {
            super(new BorderLayout(0, 8));
            setBackground(PANEL);
            setBorder(new CompoundBorder(new LineBorder(new Color(45, 67, 92)), new EmptyBorder(12, 12, 6, 12)));
            setPreferredSize(new Dimension(440, initialHeight));
            setMinimumSize(new Dimension(260, 150));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, initialHeight));
            add(sectionLabel(title), BorderLayout.NORTH);
            add(new JScrollPane(output), BorderLayout.CENTER);

            JPanel resizeHandle = new JPanel(new BorderLayout());
            resizeHandle.setBackground(PANEL_ALT);
            resizeHandle.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            resizeHandle.setToolTipText("Drag up or down to resize this text area.");
            resizeHandle.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, new Color(67, 91, 119)),
                    new EmptyBorder(3, 0, 3, 0)));
            resizeLabel = new JLabel("DRAG UP / DOWN TO RESIZE", SwingConstants.CENTER);
            resizeLabel.setForeground(MUTED);
            resizeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            resizeHandle.add(resizeLabel, BorderLayout.CENTER);
            MouseAdapter resizeAction = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) {
                    dragStartY = event.getYOnScreen();
                    dragStartHeight = getHeight();
                }
                @Override public void mouseDragged(MouseEvent event) {
                    int newHeight = Math.max(150, Math.min(800, dragStartHeight + event.getYOnScreen() - dragStartY));
                    setPreferredSize(new Dimension(getPreferredSize().width, newHeight));
                    setMaximumSize(new Dimension(Integer.MAX_VALUE, newHeight));
                    revalidate();
                    Container parent = getParent();
                    while (parent != null) { parent.revalidate(); parent.repaint(); parent = parent.getParent(); }
                }
            };
            resizeHandle.addMouseListener(resizeAction);
            resizeHandle.addMouseMotionListener(resizeAction);
            resizeLabel.addMouseListener(resizeAction);
            resizeLabel.addMouseMotionListener(resizeAction);
            add(resizeHandle, BorderLayout.SOUTH);
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints c, int row, String caption, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8); panel.add(label(caption), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0); panel.add(field, c);
    }

    private JComponent pair(JComponent first, JComponent second) {
        JPanel pair = new JPanel(new GridLayout(1, 2, 6, 0)); pair.setOpaque(false); pair.add(first); pair.add(second); return pair;
    }

    private JSpinner spinner(int initial, int minimum, int maximum, int step) { return new JSpinner(new SpinnerNumberModel(initial, minimum, maximum, step)); }
    private int value(JSpinner spinner) { return ((Number) spinner.getValue()).intValue(); }
    private String durationText(int seconds) {
        int safe = Math.max(0, seconds);
        return (safe / 60) + " min " + String.format(Locale.ROOT, "%02d", safe % 60) + " sec";
    }
    private String shortDuration(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%dm%02ds", safe / 60, safe % 60);
    }
    private String roadStatus(Road road) {
        if (!road.available) return "CLOSED";
        if (road.density() >= 70) return "HEAVY TRAFFIC";
        if (road.density() >= 35) return "MODERATE";
        return "CLEAR";
    }
    private String[] roadNames() { String[] names = new String[graph.roads.size()]; for (Road road : graph.roads) names[road.id] = (road.id + 1) + ". " + graph.name(road.a) + " – " + graph.name(road.b); return names; }

     



    private final class RoadTableModel extends AbstractTableModel {
        private final String[] columns = {"Road segment", "Traffic %", "Open?", "ETA", "Condition"};
        @Override public int getRowCount() { return graph.roads.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Class<?> getColumnClass(int column) {
            switch (column) {
                case 1: return Integer.class;
                case 2: return Boolean.class;
                default: return String.class;
            }
        }
        @Override public boolean isCellEditable(int row, int column) { return column == 1 || column == 2; }
        @Override public Object getValueAt(int row, int column) {
            Road road = graph.roads.get(row);
            switch (column) {
                case 0: return graph.name(road.a) + " ↔ " + graph.name(road.b);
                case 1: return road.density();
                case 2: return road.available;
                case 3: return shortDuration(road.travelTime());
                case 4: return roadStatus(road);
                default: return "";
            }
        }
        @Override public void setValueAt(Object value, int row, int column) {
            Road road = graph.roads.get(row);
            try {
                if (column == 1) {
                    int density = value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(String.valueOf(value).trim());
                    if (density < 0 || density > 100) throw new IllegalArgumentException("Traffic density must be between 0 and 100%.");
                    road.recent.fill(density);
                } else if (column == 2) {
                    road.available = value instanceof Boolean ? ((Boolean) value).booleanValue() : Boolean.parseBoolean(String.valueOf(value));
                } else return;
                refreshRoute();
                fireTableDataChanged();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(frame, "Road update rejected: " + ex.getMessage(), "Invalid road value", JOptionPane.WARNING_MESSAGE);
                fireTableDataChanged();
            }
        }
    }

    private void applyRoadTableEdits() {
        if (roadReviewTable != null && roadReviewTable.isEditing()) roadReviewTable.getCellEditor().stopCellEditing();
        refreshRoute();
        if (roadReviewModel != null) roadReviewModel.fireTableDataChanged();
        showRoadReview();
        JOptionPane.showMessageDialog(frame,
                "Road inputs applied. The route, warning symbols, and segment ETAs have been recalculated.",
                "Road scenario updated", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setRoadTrafficPreset(int density, String level) {
        if (roadReviewTable != null && roadReviewTable.isEditing()
                && !roadReviewTable.getCellEditor().stopCellEditing()) return;
        int selectedRow = roadReviewTable == null ? -1 : roadReviewTable.getSelectedRow();
        if (selectedRow < 0) {
            roadReviewStatus.setText("Select a road row first, then choose a traffic level.");
            return;
        }
        int modelRow = roadReviewTable.convertRowIndexToModel(selectedRow);
        Road road = graph.roads.get(modelRow);
        road.recent.fill(density);
        refreshRoute();
        roadReviewModel.fireTableDataChanged();
        roadReviewTable.setRowSelectionInterval(selectedRow, selectedRow);
        String status = level + " traffic set to " + density + "% on " + graph.name(road.a) + " – " + graph.name(road.b) + ".";
        if (!road.available) status += " Road remains closed; reopen it to show the traffic color.";
        else status += " The map color and route are updated.";
        roadReviewStatus.setText(status);
    }

    private void loadSelectedRoad() {
        if (roadBox == null || densityInput == null) return;
        Road road = graph.roads.get(roadBox.getSelectedIndex());
        densityInput.setValue(road.density());
        roadOpenInput.setSelected(road.available);
    }

    private void applyRoadUpdate() {
        Road road = graph.roads.get(roadBox.getSelectedIndex());
        road.available = roadOpenInput.isSelected();
        if (sustainedInput.isSelected()) road.recent.fill(value(densityInput)); else road.recent.add(value(densityInput));
        refreshRoute();
        conditionsOutput.setText("Updated simulated road: " + graph.name(road.a) + " – " + graph.name(road.b)
                + "\nDensity: " + road.density() + "%\nTraffic-adjusted estimate: " + durationText(road.travelTime())
                + "\nStatus: " + roadStatus(road)
                + "\n\n" + decisionSummary());
    }

    private void addReservation(JTextField startTime, JTextField endTime, JTextField reservationLabel) {
        try {
            int begin = Integer.parseInt(startTime.getText().trim()), end = Integer.parseInt(endTime.getText().trim());
            if (begin < 0 || end <= begin) throw new IllegalArgumentException("End time must be greater than start time.");
            int node = reservationNodeBox.getSelectedIndex();
            Interval conflict = signals.addReservation(node, begin, end, reservationLabel.getText().trim().isEmpty() ? "Planned movement" : reservationLabel.getText().trim());
            if (conflict == null) { refreshRoute(); conditionsOutput.setText("Reservation added at " + graph.name(node) + " for " + Fmt.t(begin) + " to " + Fmt.t(end) + "."); }
            else conditionsOutput.setText("Reservation not added: it overlaps " + conflict.label() + " (" + Fmt.t(conflict.start()) + " to " + Fmt.t(conflict.end()) + ").");
        } catch (IllegalArgumentException exception) { conditionsOutput.setText("Reservation not added: " + exception.getMessage()); }
    }

    private void restoreSample() {
        resetScenario();
        congestionThreshold = 70;
        if (congestionThresholdInput != null) congestionThresholdInput.setValue(congestionThreshold);
        scenarioBox.setSelectedIndex(0); startBox.setSelectedItem("Airport"); destinationBox.setSelectedItem("City Hospital"); vehicleBox.setSelectedIndex(0);
        roadBox.setModel(new DefaultComboBoxModel<>(roadNames())); reservationNodeBox.setModel(new DefaultComboBoxModel<>(nodeNames()));
        loadSelectedRoad(); refreshRoute(); conditionsOutput.setText("Original simulated city, reservations, road conditions, and timing defaults restored.");
    }

    private void showRoadStatus() {
        StringBuilder text = new StringBuilder("SIMULATED ROAD STATUS — OPEN, CONGESTED, OR CLOSED\n\n");
        for (Road road : graph.roads) text.append(String.format(Locale.ROOT, "%2d. %-27s density %3d%% | estimate %s | %s%n", road.id + 1,
                graph.name(road.a) + " – " + graph.name(road.b), road.density(), durationText(road.travelTime()), roadStatus(road)));
        text.append("\nMap key: green = light traffic (<35%); orange = moderate (35–69%); red = heavy (70%+).\n");
        text.append("Markers: red warning triangle = heavy; amber warning triangle = moderate; red X = closed. Yellow stripe = selected route. Labels = estimated road time.\n");
        conditionsOutput.setText(text.toString());
    }

    private void updateRouteSelection() {
        start = graph.find(String.valueOf(startBox.getSelectedItem())); destination = graph.find(String.valueOf(destinationBox.getSelectedItem())); refreshRoute();
    }

    private VehicleType selectedVehicle() { return VehicleType.values()[vehicleBox.getSelectedIndex()]; }

     
    private String buildImpactReport() {
        StringBuilder text = new StringBuilder("EMERGENCY CORRIDOR — SCENARIO IMPACT ANALYSIS\n\n");
        text.append("Scenario: current editable simulated road and signal conditions\n");
        text.append("Vehicle: ").append(selectedVehicle().label).append("\n");
        text.append("Start: ").append(graph.name(start)).append("\nDestination: ").append(graph.name(destination)).append("\n");
        text.append("Policy decision: ").append(greenCorridorActivated ? "GREEN CORRIDOR REQUESTED" : "NORMAL ROUTE SELECTED").append("\n");
        text.append("Activation rule: the corridor is requested only when every evaluated candidate route has average road density at or above ")
                .append(congestionThreshold).append("%.\n");
        text.append("Routing safety rule: closed roads are excluded; roads at 70%+ density are avoided when an open lower-congestion route exists.\n");
        if (heavyRoadFallback) text.append("Heavy-road fallback: no route avoids all heavy roads, so the quickest available open-road route is shown with warning markers.\n");
        text.append("\n");

        if (routes == null || routes.isEmpty()) {
            text.append("No comparison is possible because the selected endpoints are disconnected by open roads. Reopen a road or choose different endpoints.\n");
        } else {
            Graph.Route selected = routes.get(0);
            text.append("SELECTED ROUTE\n").append(routeString(selected)).append("\n");
            text.append("Road-only estimated time: ").append(durationText(selected.total)).append(" (" ).append(selected.total).append(" s)\n");
            text.append("Average simulated density: ").append(averageDensity(selected)).append("%\n");
            text.append("Open roads in selected route: ").append(selected.roads.size()).append("\n\n");

            if (selected.roads.isEmpty()) {
                text.append("The start and destination are the same intersection, so a signal impact comparison is not needed.\n\n");
            } else {
                int normalArrival = scheduler.baselineArrival(selected);
                SignalScheduler.Plan coordinated = scheduler.schedule(selected, selectedVehicle(), true);
                text.append("BEFORE / AFTER ESTIMATES (SAME ROUTE)\n");
                text.append("Ordinary signal timing estimate: ").append(durationText(normalArrival)).append(" (" ).append(normalArrival).append(" s)\n");
                if (coordinated == null) {
                    text.append("Coordinated signal estimate: no feasible schedule under current simulated constraints.\n");
                    text.append("Interpretation: check reservations, maximum wait, clearance time, or cross-traffic hold limits.\n");
                } else {
                    int delta = normalArrival - coordinated.destArrival;
                    double pct = normalArrival > 0 ? (100.0 * delta / normalArrival) : 0.0;
                    text.append("Coordinated-signal what-if estimate: ").append(durationText(coordinated.destArrival)).append(" (" ).append(coordinated.destArrival).append(" s)\n");
                    if (delta > 0) {
                        text.append("Predicted difference: ").append(delta).append(" s earlier (" ).append(String.format(Locale.ROOT, "%.1f", pct)).append("% of baseline estimate).\n");
                    } else if (delta < 0) {
                        text.append("Predicted difference: ").append(-delta).append(" s later than the ordinary-signal estimate; this scenario shows no estimated time benefit.\n");
                    } else {
                        text.append("Predicted difference: 0 s; no estimated time benefit for this setting.\n");
                    }
                    int preemptions = 0, heldTotal = 0;
                    for (int i = 1; i < coordinated.steps.length - 1; i++) {
                        SignalScheduler.Step step = coordinated.steps[i];
                        if (step.preempted) preemptions++;
                        heldTotal += step.held;
                    }
                    text.append("Planned signal priority changes: ").append(preemptions).append("\n");
                    text.append("Estimated cross-traffic vehicles held across the plan: ").append(heldTotal).append(" (simulation units)\n");
                    text.append("Rejected candidate timing choices during search: ").append(coordinated.rejected).append("\n");
                }
                text.append("\nNote: the coordinated schedule is a counterfactual comparison when policy mode is NORMAL ROUTE. It is not represented as an active real-world signal command.\n\n");
            }

            text.append("ROUTE CANDIDATES\n");
            for (int i = 0; i < Math.min(3, routes.size()); i++) {
                Graph.Route candidate = routes.get(i);
                text.append(i == 0 ? "Selected: " : "Alternative " + i + ": ")
                        .append(routeString(candidate)).append(" | road-only ").append(candidate.total).append(" s | average density ")
                        .append(averageDensity(candidate)).append("% | ")
                        .append(averageDensity(candidate) < congestionThreshold ? "below threshold" : "at/above threshold").append("\n");
            }
        }

        DSU groups = graph.connectivity();
        text.append("\nNETWORK CONNECTIVITY CHECK\n");
        text.append("Connected components across currently open roads: ").append(groups.components).append("\n");
        text.append("Start and destination connected: ").append(groups.connected(start, destination) ? "YES" : "NO").append("\n");
        text.append("\nPROJECT DIFFERENTIATOR\n");
        text.append("The application combines closed-road exclusion, heavy-road avoidance when a clean alternative exists, editable per-road travel times, visible road-risk markers, constrained signal-timing planning, and before/after what-if analysis in one simulation. The highlighted route is only the visualization; it is not the contribution by itself.\n");
        text.append("\nSAFETY / SCOPE NOTICE\nAll road conditions, estimated travel times, vehicle movement, and signal actions are simulated. This prototype does not consume live traffic feeds, track real vehicles, or connect to traffic-signal hardware.\n");
        return text.toString();
    }

    private void showRoadReview() {
        StringBuilder text = new StringBuilder("SIMULATED ROAD REVIEW\n\n");
        DSU groups = graph.connectivity();
        Map<Integer, List<String>> members = new TreeMap<>();
        for (Intersection node : graph.nodes) members.computeIfAbsent(groups.find(node.id), unused -> new ArrayList<>()).add(node.name);
        text.append("Open-road groups: ").append(groups.components).append('\n');
        int number = 1; for (List<String> member : members.values()) text.append("  Group ").append(number++).append(": ").append(member).append('\n');
        text.append("Selected locations connected: ").append(groups.connected(start, destination) ? "YES" : "NO").append("\n\n");
        text.append("ROAD CONDITIONS & TIME ESTIMATES\n");
        for (Road road : graph.roads) {
            text.append("  ").append(graph.name(road.a)).append(" → ").append(graph.name(road.b))
                    .append(" | base ").append(durationText(road.baseTime))
                    .append(" | ETA ").append(durationText(road.travelTime()))
                    .append(" | density ").append(road.density()).append("% | ").append(roadStatus(road)).append("\n");
        }
        text.append("\nCURRENT POLICY-SELECTED ROUTE\n");
        Graph.Route current = routes == null || routes.isEmpty() ? null : routes.get(0);
        text.append(current == null ? "No route through currently open roads.\n" : routeString(current) + " | total " + durationText(current.total) + "\n");
        if (heavyRoadFallback) text.append("Note: all available routes need at least one heavy-traffic road; the fastest open-road fallback is selected.\n");
        FlowNetwork review = graph.toFlowNetwork();
        int first = review.fordFulkerson(start, destination), firstUpdates = review.augmentations;
        int second = review.edmondsKarp(start, destination), secondUpdates = review.augmentations;
        text.append("Normal-road movement estimate A: ").append(first).append(" (updates ").append(firstUpdates).append(")\n");
        text.append("Normal-road movement estimate B: ").append(second).append(" (updates ").append(secondUpdates).append(")\n");
        int limit = 0; List<int[]> limiting = review.cutEdges(start);
        text.append("Limiting roads:\n");
        for (int[] road : limiting) { text.append("  ").append(graph.name(road[0])).append(" → ").append(graph.name(road[1])).append(" : ").append(road[2]).append('\n'); limit += road[2]; }
        text.append("Capacity cross-check: ").append(limit).append(" and ").append(second).append(limit == second ? " are consistent." : " need attention.").append("\n");
        long[] lowImpact = graph.toFlowNetwork().minCostMaxFlow(start, destination);
        text.append("Low-impact movement estimate: ").append(lowImpact[0]).append("; total travel impact: ").append(lowImpact[1]).append(".");
        reviewOutput.setText(text.toString());
    }

     
    private void showDSUConnectivity() {
        DSU groups = graph.connectivity();
        Map<Integer, List<String>> members = new TreeMap<>();
        for (Intersection node : graph.nodes) {
            members.computeIfAbsent(groups.find(node.id), unused -> new ArrayList<>()).add(node.name);
        }
        StringBuilder text = new StringBuilder("OPEN-ROAD CONNECTION GROUPS\n\n");
        text.append("Only currently open roads are included.\n");
        text.append("Roads considered: currently OPEN roads only\n");
        text.append("Connected components: ").append(groups.components).append("\n\n");
        int number = 1;
        for (List<String> component : members.values()) {
            text.append("Component ").append(number++).append(": ").append(component).append('\n');
        }
        text.append("\nSelected start: ").append(graph.name(start));
        text.append("\nSelected destination: ").append(graph.name(destination));
        text.append("\nConnected by open roads: ").append(groups.connected(start, destination) ? "YES" : "NO");
        text.append("\n\nTry this: close a road in CONDITIONS, then check these groups again.");
        text.append("\nThis view shows reachability; the route panel compares estimated travel times.");
        reviewOutput.setText(text.toString());
    }

    private void showRoadRange(int first, int second) {
        int from = Math.min(first, second) - 1, to = Math.max(first, second) - 1, sum = 0, high = 0;
        for (int i = from; i <= to; i++) { sum += graph.roads.get(i).density(); high = Math.max(high, graph.roads.get(i).density()); }
        reviewOutput.setText("SIMULATED ROAD RANGE\n\nRoads " + (from + 1) + " to " + (to + 1) + "\nTotal density: " + sum
                + "\nAverage density: " + String.format("%.1f%%", (double) sum / (to - from + 1)) + "\nHighest single-road density: " + high + "%");
    }

    private void showCityTimes() {
        long[][] times = graph.allPairs();
        StringBuilder text = new StringBuilder("ESTIMATED CITY TRAVEL TIMES — SHORTEST OPEN-ROAD PATHS\nTimes use simulated traffic-adjusted weights; heavy roads are penalized but not removed in this all-pairs review.\n\n");
        for (int i = 0; i < graph.size(); i++) {
            text.append(graph.name(i)).append(":\n");
            for (int j = 0; j < graph.size(); j++) if (i != j) text.append("  → ").append(graph.name(j)).append(" = ")
                    .append(times[i][j] >= Graph.INF ? "UNREACHABLE" : durationText((int) times[i][j])).append("\n");
            text.append("\n");
        }
        reviewOutput.setText(text.toString());
    }

    private void showFeatureDetails() {
        StringBuilder text = new StringBuilder("DETAILED FEATURE VIEW\n\n");
        Graph.Route selected = graph.dijkstra(start, destination, null);
        if (selected == null) { reviewOutput.setText("No open-road route is available for the selected locations."); return; }
        text.append("ROUTE DECISION SEQUENCE\n");
        for (int[] item : graph.lastSettled) text.append("  ").append(graph.name(item[0])).append(" — ").append(item[1]).append(" s\n");
        SignalScheduler.Plan full = scheduler.schedule(selected, selectedVehicle(), false);
        SignalScheduler.Plan focused = scheduler.schedule(selected, selectedVehicle(), true);
        text.append("\nSIGNAL-PLAN COMPARISON\n");
        if (full == null || focused == null) text.append("  No feasible plan exists under the current simulated settings.\n");
        else text.append("  Full review: ").append(full.states).append(" options, cost ").append(full.cost)
                .append("\n  Focused review: ").append(focused.states).append(" options, cost ").append(focused.cost)
                .append("\n  Same result: ").append(full.cost == focused.cost ? "YES" : "NO").append('\n');
        text.append("\nNORMAL-TRAFFIC RELEASE\n");
        for (int i = 1; i < selected.nodes.size() - 1; i++) {
            Approach choice = Greedy.choose(signals.approaches.get(selected.nodes.get(i)), selected.nodes.get(i - 1), selected.nodes.get(i + 1), Greedy.Rule.LONGEST_QUEUE);
            text.append("  ").append(graph.name(selected.nodes.get(i))).append(": ")
                    .append(choice == null ? "no waiting vehicles" : "release from " + graph.name(choice.from) + " (" + choice.queue.size() + " vehicles)").append('\n');
        }
        text.append("\nHOW THE REVIEWS HELP\n")
                .append("  Route selection compares available roads and their current time estimates.\n")
                .append("  City travel times compare every location with every other location.\n")
                .append("  Road-movement review identifies capacity limits and lower-impact options.\n")
                .append("  Location lookup supports names, prefixes, map coordinates, and hospitals.\n")
                .append("  Signal-plan review checks timing, reservations, and safety limits.\n");
        reviewOutput.setText(text.toString());
    }

    private void findByName(String query) {
        Directory directory = new Directory(graph); List<Integer> matches = directory.find(query);
        if (query == null || query.trim().isEmpty()) { finderOutput.setText("Enter an intersection name, prefix, or number."); return; }
        if (matches.isEmpty()) { finderOutput.setText("No simulated intersection matches '" + query.trim() + "'."); return; }
        StringBuilder text = new StringBuilder("MATCHING INTERSECTIONS\n\n");
        for (int id : matches) { Intersection node = graph.nodes.get(id); text.append((id + 1)).append(". ").append(node.name).append("  (").append(node.x).append(", ").append(node.y).append(")").append(node.hospital ? "  HOSPITAL" : "").append('\n'); }
        finderOutput.setText(text.toString());
    }

    private void findNearest(int x, int y) {
        Intersection node = new KDTree(graph.nodes).nearest(x, y);
        finderOutput.setText("NEAREST SIMULATED INTERSECTION\n\nInput coordinates: (" + x + ", " + y + ")\nResult: " + node.name + " at (" + node.x + ", " + node.y + ")");
    }

    private void findHospital(int x, int y) {
        Intersection origin = new KDTree(graph.nodes).nearest(x, y); List<Intersection> hospitals = new ArrayList<>();
        for (Intersection node : graph.nodes) if (node.hospital) hospitals.add(node);
        Intersection direct = new KDTree(hospitals).nearest(x, y); Graph.Route quickest = null;
        for (Intersection hospital : hospitals) { Graph.Route route = graph.dijkstra(origin.id, hospital.id, null); if (route != null && (quickest == null || route.total < quickest.total)) quickest = route; }
        String quickestText = quickest == null ? "None" : graph.name(quickest.nodes.get(quickest.nodes.size() - 1)) + " (" + quickest.total + " s)";
        finderOutput.setText("HOSPITAL LOOKUP\n\nNearest location to input: " + origin.name + "\nNearest hospital by map position: " + direct.name
                + "\nQuickest reachable hospital by simulated road time: " + quickestText);
    }

    private JComponent controlPanel() {
        JPanel controls = panel(new GridBagLayout());
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 224));
        GridBagConstraints c = constraints();
        c.gridwidth = 2;
        JLabel heading = sectionLabel("SCENARIO CONTROLS");
        controls.add(heading, c);

        c.gridy = 1; c.gridwidth = 1; c.insets = new Insets(10, 0, 4, 8);
        controls.add(label("Vehicle"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 4, 0);
        vehicleBox = new JComboBox<>(new String[]{"Ambulance", "Fire Engine", "Police Vehicle"});
        vehicleBox.setToolTipText("Affects how the simulated signal plan balances emergency waiting against holding other traffic. It does not change the road route or road-only time.");
        vehicleBox.addActionListener(e -> updateRouteSelection());
        vehicleBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                JLabel cell = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                int iconIndex = index >= 0 ? index : vehicleBox.getSelectedIndex();
                cell.setIcon(iconIndex >= 0 && iconIndex < vehicleIcons.length ? vehicleIcons[iconIndex] : null);
                cell.setIconTextGap(8);
                return cell;
            }
        });
        controls.add(vehicleBox, c);

        c.gridx = 0; c.gridy = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8);
        controls.add(label("Start"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 4, 0);
        startBox = new JComboBox<>(nodeNames());
        startBox.setSelectedItem("Airport");
        controls.add(startBox, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8);
        controls.add(label("Destination"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0);
        destinationBox = new JComboBox<>(nodeNames());
        destinationBox.setSelectedItem("City Hospital");
        controls.add(destinationBox, c);

        c.gridx = 0; c.gridy = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.insets = new Insets(4, 0, 4, 8);
        controls.add(label("Traffic"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0);
        scenarioBox = new JComboBox<>(new String[]{"Normal simulated traffic", "Sustained congestion: Saidapet - Adyar"});
        controls.add(scenarioBox, c);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(10, 0, 0, 6);
        JButton routeButton = new JButton("CALCULATE BEST ROUTE");
        routeButton.addActionListener(e -> updateRouteSelection());
        controls.add(routeButton, c);
        c.gridx = 1; c.insets = new Insets(10, 0, 0, 0);
        JButton presetButton = new JButton("LOAD PRESET");
        presetButton.addActionListener(e -> resetAndRefresh());
        controls.add(presetButton, c);

        c.gridx = 0; c.gridy = 6; c.gridwidth = 1; c.insets = new Insets(7, 0, 0, 6);
        JButton swapButton = new JButton("SWAP ENDPOINTS");
        swapButton.setToolTipText("Swap the selected start and destination intersections");
        swapButton.addActionListener(e -> {
            Object oldStart = startBox.getSelectedItem();
            startBox.setSelectedItem(destinationBox.getSelectedItem());
            destinationBox.setSelectedItem(oldStart);
            updateRouteSelection();
        });
        controls.add(swapButton, c);
        c.gridx = 1; c.insets = new Insets(7, 0, 0, 0);
        JButton exportButton = new JButton("EXPORT ROUTE REPORT");
        exportButton.setToolTipText("Save the current route, alternatives, and signal plan as a text report");
        exportButton.addActionListener(e -> exportRouteReport());
        controls.add(exportButton, c);
        return controls;
    }

    private JComponent metricsPanel() {
        JPanel metrics = panel(new GridLayout(2, 2, 8, 8));
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 164));
        routeTimeValue = metric(metrics, "ROAD-ONLY ESTIMATE");
        arrivalValue = metric(metrics, "CORRIDOR PREDICTION");
        savingValue = metric(metrics, "PREDICTED TIME SAVED");
        preemptionValue = metric(metrics, "SIGNALS PREEMPTED");
        return metrics;
    }

    private JLabel metric(JPanel parent, String caption) {
        JPanel card = new JPanel();
        card.setBackground(PANEL_ALT);
        card.setBorder(new EmptyBorder(9, 10, 9, 10));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(MUTED);
        captionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        JLabel value = new JLabel("—");
        value.setForeground(ACCENT);
        value.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        card.add(captionLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(value);
        parent.add(card);
        return value;
    }

    private JComponent detailsPanel() {
        routeDetails = textArea();
        return outputPanel("SELECTED ROUTE & ALTERNATIVES", routeDetails, 270);
    }

    private JComponent insightsPanel() {
        insightDetails = textArea();
        return outputPanel("ROAD STATUS SUMMARY", insightDetails, 210);
    }

    private JComponent footer() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel status = new JPanel();
        status.setOpaque(false);
        status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));
        playbackLabel = new JLabel("Playback ready — all values are simulated.");
        playbackLabel.setForeground(MUTED);
        liveStatusLabel = new JLabel("● SYSTEM READY  |  SIMULATION MODE");
        liveStatusLabel.setForeground(ACCENT);
        liveStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        status.add(liveStatusLabel);
        status.add(Box.createVerticalStrut(3));
        status.add(playbackLabel);
        footer.add(status, BorderLayout.WEST);
        playButton = new JButton("PLAY ROUTE VISUALIZATION");
        playButton.addActionListener(e -> playRoute());
        footer.add(playButton, BorderLayout.EAST);
        return footer;
    }

    private void installKeyboardShortcuts() {
        JRootPane root = frame.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "refreshRoute");
        root.getActionMap().put("refreshRoute", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { updateRouteSelection(); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl E"), "exportRoute");
        root.getActionMap().put("exportRoute", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { exportRouteReport(); }
        });
    }

    private void exportRouteReport() {
        if (routes == null || routes.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "There is no reachable route to export.", "Route report", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export simulated corridor report");
        chooser.setSelectedFile(new File("green-corridor-report.txt"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) target = new File(target.getParentFile(), target.getName() + ".txt");
        StringBuilder report = new StringBuilder();
        report.append("EMERGENCY VEHICLE GREEN CORRIDOR — SIMULATION REPORT\n");
        report.append("Generated: ").append(new java.util.Date()).append("\n");
        report.append("NOTICE: Fictional academic simulation; no live traffic or real signal control.\n\n");
        report.append("Vehicle: ").append(selectedVehicle().label).append('\n');
        report.append("Start: ").append(graph.name(start)).append("\nDestination: ").append(graph.name(destination)).append('\n');
        report.append("\n").append(routeText()).append("\n\n").append(insightText()).append("\n\n").append(buildImpactReport()).append('\n');
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
            writer.write(report.toString());
            JOptionPane.showMessageDialog(frame, "Report saved to:\n" + target.getAbsolutePath(), "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Could not save the report: " + ex.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetScenario() {
        graph = Graph.sampleCity();
        signals = new SignalControl(graph);
        signals.loadSampleReservations(graph);
        scheduler = new SignalScheduler(signals);
        start = graph.find("Airport");
        destination = graph.find("City Hospital");
    }

    private String[] nodeNames() {
        String[] names = new String[graph.nodes.size()];
        for (int i = 0; i < graph.nodes.size(); i++) names[i] = graph.name(i);
        return names;
    }

    private void resetAndRefresh() {
        String startName = String.valueOf(startBox.getSelectedItem());
        String destinationName = String.valueOf(destinationBox.getSelectedItem());
        String scenario = String.valueOf(scenarioBox.getSelectedItem());
        resetScenario();
        start = graph.find(startName);
        destination = graph.find(destinationName);
        if (scenario.startsWith("Sustained")) graph.roads.get(5).recent.fill(100);
        refreshRoute();
    }

    private void refreshRoute() {
        stopPlayback();
        Graph.Route congestionFreeCheck = graph.dijkstraAvoidingHeavyTraffic(start, destination, 70);
        heavyRoadFallback = congestionFreeCheck == null && graph.dijkstra(start, destination, null) != null;
        List<Graph.Route> candidates = graph.routesAvoidingHeavyTraffic(start, destination, Math.max(2, graph.roads.size()), 70);
        routes = candidates;
        greenCorridorActivated = false;
        plan = null;
        if (candidates != null && !candidates.isEmpty()) {
            Graph.Route chosenNormal = null;
            for (Graph.Route candidate : candidates) {
                if (averageDensity(candidate) < congestionThreshold) { chosenNormal = candidate; break; }
            }
            if (chosenNormal != null) {
                routes = selectedFirst(candidates, chosenNormal);
            } else {
                greenCorridorActivated = true;
                routes = candidates;
                plan = scheduler.schedule(routes.get(0), selectedVehicle(), true);
            }
        }
        if (routes == null) {
            routeTitle.setText("No route exists through the open simulated roads");
            routeTimeValue.setText("UNREACHABLE");
            arrivalValue.setText("—");
            savingValue.setText("—");
            preemptionValue.setText("—");
            routeDetails.setText("The selected intersections are disconnected in this simulated scenario.");
            insightDetails.setText("No open-road connection is available. Try a different start or destination.");
        } else {
            Graph.Route best = routes.get(0);
            String routePrefix = greenCorridorActivated ? "GREEN CORRIDOR ACTIVATED" : "NORMAL ROUTE";
            if (heavyRoadFallback) routePrefix += " • NO CLEAR ROUTE — BEST AVAILABLE DETOUR";
            routeTitle.setText(routePrefix + " — " + routeString(best));
            routeTimeValue.setText(shortDuration(best.total) + " (" + best.total + "s)");
            if (plan == null) {
                arrivalValue.setText("NOT ACTIVATED");
                savingValue.setText("Not needed");
                preemptionValue.setText("0 — not activated");
            } else {
                int baseline = scheduler.baselineArrival(best);
                int preemptions = 0;
                for (int i = 1; i < plan.steps.length - 1; i++) if (plan.steps[i].preempted) preemptions++;
                arrivalValue.setText(shortDuration(plan.destArrival));
                savingValue.setText((baseline - plan.destArrival) + " s");
                preemptionValue.setText(preemptions + " / " + Math.max(0, plan.steps.length - 2));
            }
            routeDetails.setText(routeText());
            insightDetails.setText(insightText());
        }
        if (routes != null && routes.get(0).roads.isEmpty()) {
            playButton.setEnabled(false);
            playbackLabel.setText("Choose different start and destination intersections to replay a route.");
        } else if (routes != null) {
            playButton.setEnabled(true);
        }
        if (roadReviewModel != null) roadReviewModel.fireTableDataChanged();
        networkPanel.repaint();
    }

    private int averageDensity(Graph.Route route) {
        if (route.roads.isEmpty()) return 0;
        int sum = 0;
        for (Road road : route.roads) sum += road.density();
        return (int) Math.round((double) sum / route.roads.size());
    }

    private List<Graph.Route> selectedFirst(List<Graph.Route> candidates, Graph.Route selected) {
        List<Graph.Route> ordered = new ArrayList<>();
        ordered.add(selected);
        for (Graph.Route route : candidates) if (!route.sameAs(selected)) ordered.add(route);
        return ordered;
    }

    private String decisionSummary() {
        if (routes == null || routes.isEmpty()) return "No feasible open-road route exists. The corridor cannot be scheduled until a route is available.";
        StringBuilder out = new StringBuilder();
        out.append(greenCorridorActivated ? "DECISION: GREEN CORRIDOR ACTIVATED" : "DECISION: USE A NORMAL ROUTE")
           .append("\nCorridor activation threshold: ").append(congestionThreshold).append("% average density per route\n")
           .append("Routing rule: closed roads are excluded; roads at 70%+ density are avoided when a clear route exists.\n");
        if (heavyRoadFallback) out.append("WARNING: no route avoids every heavy-traffic road, so the fastest available open-road route is used.\n");
        for (int i = 0; i < routes.size(); i++) {
            Graph.Route route = routes.get(i);
            out.append(i + 1).append(i == 0 ? ". SELECTED: " : ". ALTERNATIVE: ")
               .append(routeString(route)).append(" | average density ").append(averageDensity(route)).append("% | ")
               .append(Fmt.t(route.total)).append(greenCorridorActivated ? "\n" : (averageDensity(route) < congestionThreshold ? " | acceptable\n" : " | congested\n"));
        }
        if (greenCorridorActivated) out.append("All evaluated candidate routes meet/exceed the congestion threshold. The signal scheduler is used for the selected feasible route.");
        else out.append("At least one candidate route is below the threshold; the quickest acceptable route is selected without activating corridor signal preemption.");
        return out.toString();
    }

    private String routeText() {
        Graph.Route best = routes.get(0);
        StringBuilder text = new StringBuilder("SELECTED ROUTE\n");
        text.append(routeString(best)).append("\nEstimated total: ").append(durationText(best.total)).append("\n");
        if (heavyRoadFallback) text.append("Warning: every available route includes heavy traffic; showing the quickest open route.\n");
        text.append("\nROAD-BY-ROAD ESTIMATES\n");
        int cumulativeSeconds = 0;
        for (int i = 0; i < best.roads.size(); i++) {
            Road road = best.roads.get(i);
            cumulativeSeconds += road.travelTime();
            text.append(i + 1).append(". ").append(graph.name(best.nodes.get(i))).append(" → ")
                    .append(graph.name(best.nodes.get(i + 1))).append(" | base ").append(durationText(road.baseTime))
                    .append(" | estimate ").append(durationText(road.travelTime()))
                    .append(" | ").append(roadStatus(road)).append(" (").append(road.density()).append("%)")
                    .append("\n   Arrival estimate: ").append(durationText(cumulativeSeconds)).append("\n");
        }
        for (int i = 1; i < routes.size(); i++) {
            Graph.Route alternative = routes.get(i);
            int delta = alternative.total - best.total;
            text.append("\nAlternative ").append(i).append(": ").append(routeString(alternative))
                    .append(" | ").append(durationText(alternative.total)).append(" | ")
                    .append(delta >= 0 ? "+" : "").append(durationText(Math.abs(delta))).append(" vs selected");
        }
        return text.toString();
    }

    private String insightText() {
        if (routes == null || routes.isEmpty()) return "No open-road route is available. Change endpoints or reopen a road.";
        if (heavyRoadFallback) return "HEAVY TRAFFIC ON EVERY AVAILABLE ROUTE\nThe quickest open route is shown. Red roads and ! markers identify heavy traffic; closed roads are not used.";
        return "Route selected from your road times and traffic inputs. Closed roads are excluded, and heavy roads are avoided when a clear alternative is available. All values are simulated.";
    }

    private String routeString(Graph.Route route) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < route.nodes.size(); i++) {
            if (i > 0) path.append(" → ");
            path.append(graph.name(route.nodes.get(i)));
        }
        return path.toString();
    }

    private void playRoute() {
        if (routes == null || routes.get(0).roads.isEmpty()) return;
        stopPlayback();
        playbackProgress = 0;
        playButton.setEnabled(false);
        playbackLabel.setText("Playback started — vehicle movement is simulated, not live tracking.");
        if (liveStatusLabel != null) liveStatusLabel.setText("● ROUTE REPLAY ACTIVE  |  SIMULATION MODE");
        animation = new javax.swing.Timer(28, e -> {
            playbackProgress += 0.006;
            int roadIndex = Math.min(routes.get(0).roads.size() - 1, (int) (playbackProgress * routes.get(0).roads.size()));
            int nodeIndex = Math.min(roadIndex, routes.get(0).nodes.size() - 1);
            playbackLabel.setText("Simulated vehicle: " + graph.name(routes.get(0).nodes.get(nodeIndex)) + "  •  " + Math.min(100, (int) (playbackProgress * 100)) + "% of route");
            liveStatusLabel.setText("● ROUTE REPLAY ACTIVE  |  SIMULATION MODE");
            if (playbackProgress >= 1) {
                playbackProgress = 1;
                stopPlayback();
                playbackLabel.setText("Playback complete — arrival is a simulated prediction, not a real-world event.");
                liveStatusLabel.setText("● SIMULATION COMPLETE  |  NO LIVE SIGNAL CONTROL");
            }
            networkPanel.repaint();
        });
        animation.start();
    }

    private void stopPlayback() {
        if (animation != null) animation.stop();
        animation = null;
        playbackProgress = 0;
        if (playButton != null) playButton.setEnabled(routes != null && !routes.get(0).roads.isEmpty());
        if (liveStatusLabel != null && (animation == null)) liveStatusLabel.setText("● SYSTEM READY  |  SIMULATION MODE");
    }

    private JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(PANEL);
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(45, 67, 92)), new EmptyBorder(14, 14, 14, 14)));
        return panel;
    }

    private GridBagConstraints constraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        return label;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    private JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBorder(new EmptyBorder(2, 2, 2, 2));
        area.setCaretPosition(0);
        return area;
    }

    private final class NetworkPanel extends JPanel {
        NetworkPanel() {
            setBackground(new Color(16, 29, 45));
            ToolTipManager.sharedInstance().registerComponent(this);
            setToolTipText("Click an intersection to use it as a route endpoint");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent event) {
                    Intersection closest = null;
                    double shortest = 22;
                    for (Intersection node : graph.nodes) {
                        double distance = pointFor(node).distance(event.getPoint());
                        if (distance < shortest) { shortest = distance; closest = node; }
                    }
                    if (closest == null) return;
                    if (SwingUtilities.isRightMouseButton(event) || event.isControlDown()) {
                        destinationBox.setSelectedItem(closest.name);
                    } else {
                        startBox.setSelectedItem(closest.name);
                    }
                    updateRouteSelection();
                    playbackLabel.setText("Route endpoint updated from map: " + closest.name);
                }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawGrid(g2);
            for (Road road : graph.roads) drawRoad(g2, road, false);
            if (routes != null) for (Road road : routes.get(0).roads) drawRoad(g2, road, true);
            if (showRoadTimesOnMap != null && showRoadTimesOnMap.isSelected()) {
                for (Road road : graph.roads) drawRoadTimeLabel(g2, road);
            }
            for (Road road : graph.roads) drawRoadStatusMarker(g2, road);
            for (Intersection node : graph.nodes) drawNode(g2, node);
            if (routes != null && playbackProgress > 0) drawVehicle(g2);
            drawLegend(g2);
            g2.dispose();
        }

        @Override public String getToolTipText(MouseEvent event) {
            Intersection closest = null;
            double shortest = 22;
            for (Intersection node : graph.nodes) {
                Point point = pointFor(node);
                double distance = point.distance(event.getPoint());
                if (distance < shortest) { shortest = distance; closest = node; }
            }
            if (closest != null) return closest.name + (closest.hospital ? " (hospital)" : "") + " — simulated intersection";
            Road closestRoad = null;
            double roadDistance = 12.0;
            for (Road road : graph.roads) {
                Point a = pointFor(graph.nodes.get(road.a));
                Point b = pointFor(graph.nodes.get(road.b));
                double distance = Line2D.ptSegDist(a.x, a.y, b.x, b.y, event.getX(), event.getY());
                if (distance < roadDistance) { roadDistance = distance; closestRoad = road; }
            }
            if (closestRoad == null) return "Simulated road network — highlighted line is the selected route.";
            return graph.name(closestRoad.a) + " ↔ " + graph.name(closestRoad.b)
                    + " | " + roadStatus(closestRoad) + " | density " + closestRoad.density() + "%"
                    + " | estimated leg time " + durationText(closestRoad.travelTime())
                    + " | base time " + durationText(closestRoad.baseTime)
                    + (closestRoad.available ? "" : " | CLOSED: route calculation will avoid this road");
        }

        private void drawGrid(Graphics2D g2) {
            g2.setColor(new Color(29, 48, 69));
            for (int x = 20; x < getWidth(); x += 40) g2.drawLine(x, 0, x, getHeight());
            for (int y = 20; y < getHeight(); y += 40) g2.drawLine(0, y, getWidth(), y);
        }

        private void drawRoad(Graphics2D g2, Road road, boolean selected) {
            Point first = pointFor(graph.nodes.get(road.a));
            Point second = pointFor(graph.nodes.get(road.b));
            if (selected) {
                g2.setColor(ROUTE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g2.setColor(roadColor(road));
                g2.setStroke(new BasicStroke(road.available ? 6f : 4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        10f, road.available ? null : new float[]{5f, 5f}, 0f));
            }
            g2.drawLine(first.x, first.y, second.x, second.y);
        }

         
        private void drawRoadTimeLabel(Graphics2D g2, Road road) {
            Point a = pointFor(graph.nodes.get(road.a));
            Point b = pointFor(graph.nodes.get(road.b));
            double dx = b.x - a.x, dy = b.y - a.y;
            double length = Math.max(1.0, Math.hypot(dx, dy));
            int x = (int) Math.round((a.x + b.x) / 2.0 - (dy / length) * 10.0);
            int y = (int) Math.round((a.y + b.y) / 2.0 + (dx / length) * 10.0);
            String label = shortDuration(road.travelTime());
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 9);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(label) + 10, h = fm.getHeight() + 2;
            g2.setColor(new Color(6, 14, 25, 225));
            g2.fillRoundRect(x - w / 2, y - h / 2, w, h, 6, 6);
            g2.setColor(roadColor(road));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x - w / 2, y - h / 2, w, h, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(label, x - fm.stringWidth(label) / 2, y + fm.getAscent() / 2 - 1);
        }

         
        private void drawRoadStatusMarker(Graphics2D g2, Road road) {
            if (road.available && road.density() < 35) return;
            Point a = pointFor(graph.nodes.get(road.a));
            Point b = pointFor(graph.nodes.get(road.b));
            double dx = b.x - a.x, dy = b.y - a.y;
            double length = Math.max(1.0, Math.hypot(dx, dy));
            int x = (int) Math.round((a.x + b.x) / 2.0 + (dy / length) * 12.0);
            int y = (int) Math.round((a.y + b.y) / 2.0 - (dx / length) * 12.0);
            if (!road.available) {
                int r = 9;
                g2.setColor(new Color(180, 35, 48));
                g2.fillOval(x - r, y - r, r * 2, r * 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x - 4, y - 4, x + 4, y + 4);
                g2.drawLine(x - 4, y + 4, x + 4, y - 4);
            } else {
                int size = 19;
                Polygon triangle = new Polygon(new int[]{x, x - size / 2, x + size / 2},
                        new int[]{y - size / 2, y + size / 2, y + size / 2}, 3);
                g2.setColor(road.density() >= 70 ? new Color(235, 55, 67) : new Color(230, 151, 45));
                g2.fillPolygon(triangle);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                g2.drawString("!", x - 3, y + 5);
            }
        }

        private Color roadColor(Road road) {
            if (!road.available) return new Color(112, 124, 140);
            if (road.density() >= 70) return new Color(245, 56, 66);
            if (road.density() >= 35) return new Color(246, 158, 55);
            return new Color(85, 167, 151);
        }

        private void drawNode(Graphics2D g2, Intersection node) {
            Point point = pointFor(node);
            boolean onRoute = routes != null && routes.get(0).nodes.contains(node.id);
            int radius = node.id == start || node.id == destination ? 10 : 7;
            if (onRoute) {
                g2.setColor(ROUTE);
                g2.fillOval(point.x - radius - 3, point.y - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
            }
            if (node.id == start) g2.setColor(new Color(65, 171, 255));
            else if (node.id == destination || node.hospital) g2.setColor(new Color(245, 102, 111));
            else g2.setColor(new Color(217, 230, 242));
            g2.fillOval(point.x - radius, point.y - radius, radius * 2, radius * 2);
            g2.setColor(BACKGROUND);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(point.x - radius, point.y - radius, radius * 2, radius * 2);
            g2.setColor(TEXT);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            String label = node.name + (node.hospital ? "  +" : "");
            g2.drawString(label, point.x + 10, point.y - 8);
        }

        private void drawVehicle(Graphics2D g2) {
            Graph.Route route = routes.get(0);
            double scaled = playbackProgress * route.roads.size();
            int index = Math.min(route.roads.size() - 1, (int) scaled);
            double fraction = Math.min(1, scaled - index);
            Point from = pointFor(graph.nodes.get(route.nodes.get(index)));
            Point to = pointFor(graph.nodes.get(route.nodes.get(index + 1)));
            int x = (int) Math.round(from.x + (to.x - from.x) * fraction);
            int y = (int) Math.round(from.y + (to.y - from.y) * fraction);
            int vehicleIndex = Math.max(0, Math.min(vehicleBox.getSelectedIndex(), vehicleIcons.length - 1));
            vehicleIcons[vehicleIndex].paintIcon(this, g2, x - vehicleIcons[vehicleIndex].getIconWidth() / 2,
                    y - vehicleIcons[vehicleIndex].getIconHeight() / 2);
        }

        private void drawLegend(Graphics2D g2) {
            int top = getHeight() - 51;
            g2.setColor(new Color(6, 14, 25, 225));
            g2.fillRoundRect(10, top, 470, 43, 10, 10);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            int firstRow = top + 15;
            int secondRow = top + 34;
            legendItem(g2, 18, firstRow, ROUTE, "selected route");
            legendItem(g2, 145, firstRow, new Color(85, 167, 151), "light <35%");
            legendItem(g2, 280, firstRow, new Color(246, 158, 55), "moderate 35–69%");
            legendItem(g2, 18, secondRow, new Color(245, 56, 66), "heavy ≥70% !");
            legendItem(g2, 180, secondRow, new Color(112, 124, 140), "closed (dashed + X)");
            g2.setColor(MUTED);
            g2.drawString("Road labels = estimated time", 350, secondRow + 4);
        }

        private void legendItem(Graphics2D g2, int x, int y, Color color, String label) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(4f));
            g2.drawLine(x, y, x + 18, y);
            g2.setColor(MUTED);
            g2.drawString(label, x + 24, y + 4);
        }

        private Point pointFor(Intersection node) {
            int left = 62, right = 150, top = 50, bottom = 62;
            double x = left + node.x / 12.0 * Math.max(1, getWidth() - left - right);
            double y = top + (9 - node.y) / 9.0 * Math.max(1, getHeight() - top - bottom);
            return new Point((int) Math.round(x), (int) Math.round(y));
        }
    }
}
