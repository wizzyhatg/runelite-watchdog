package com.adamk33n3r.runelite.watchdog.ui.panels;

import com.adamk33n3r.runelite.watchdog.alerts.Alert;
import com.adamk33n3r.runelite.watchdog.notifications.IMessageNotification;
import com.adamk33n3r.runelite.watchdog.ui.PlaceholderTextField;
import com.adamk33n3r.runelite.watchdog.ui.SearchBar;
import com.adamk33n3r.runelite.watchdog.ui.StretchedStackedLayout;

import com.google.common.base.Splitter;
import net.runelite.client.ui.MultiplexingPluginPanel;
import net.runelite.client.ui.PluginPanel;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.adamk33n3r.runelite.watchdog.ui.panels.AlertPanel.BACK_ICON;
import static com.adamk33n3r.runelite.watchdog.ui.panels.AlertPanel.BACK_ICON_HOVER;

@Slf4j
//@Singleton
public class HistoryPanel extends PluginPanel {
    private final Provider<MultiplexingPluginPanel> muxer;
    private final ScrollablePanel historyItems;
    private final List<HistoryEntryPanel> previousAlerts = new ArrayList<>();

    private static final int MAX_HISTORY_ITEMS = 100;
    private static final Splitter SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();

    @Inject
    public HistoryPanel(Provider<MultiplexingPluginPanel> muxer) {
        super(false);
        this.muxer = muxer;

        this.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
        JButton backButton = PanelUtils.createActionButton(
            BACK_ICON,
            BACK_ICON_HOVER,
            "Back",
            (btn, modifiers) -> this.muxer.get().popState()
        );
        backButton.setPreferredSize(new Dimension(22, 16));
        backButton.setBorder(new EmptyBorder(0, 0, 0, 5));
        topPanel.add(backButton, BorderLayout.WEST);
        topPanel.add(new SearchBar(this::updateFilter));
        this.add(topPanel, BorderLayout.NORTH);

        this.historyItems = new ScrollablePanel(new StretchedStackedLayout(3, 3));
        this.historyItems.setBorder(new EmptyBorder(0, 10, 0, 10));
        this.historyItems.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
        this.historyItems.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
        this.historyItems.setScrollableBlockIncrement(ScrollablePanel.VERTICAL, ScrollablePanel.IncrementType.PERCENT, 10);
        JScrollPane scroll = new JScrollPane(this.historyItems, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(scroll, BorderLayout.CENTER);
    }

    public void addEntry(Alert alert, String[] triggerValues) {
        HistoryEntryPanel historyEntryPanel = new HistoryEntryPanel(alert, triggerValues);
        this.previousAlerts.add(0, historyEntryPanel);
        this.historyItems.add(historyEntryPanel, 0);
        if (this.historyItems.getComponents().length > MAX_HISTORY_ITEMS) {
            this.previousAlerts.remove(this.previousAlerts.size() - 1);
            this.historyItems.remove(this.historyItems.getComponents().length - 1);
        }
        this.revalidate();
        this.repaint();
    }

    // TODO: Abstract this out into a filterpanel type thing
    private void updateFilter(String search) {
        this.historyItems.removeAll();
        String upperSearch = search.toUpperCase();
        this.previousAlerts.stream().filter(historyEntryPanel -> {
            Alert alert = historyEntryPanel.getAlert();
            Stream<String> keywords = Stream.concat(Stream.of(
                alert.getName(),
                alert.getType().getName()
            ), alert.getNotifications().stream().flatMap(notification -> {
                Stream<String> notificationType = Stream.of(notification.getType().getName());
                if (notification instanceof IMessageNotification) {
                    return Stream.concat(notificationType, Stream.of(((IMessageNotification) notification).getMessage()));
                }
                return notificationType;
            })).map(String::toUpperCase);
            return Text.matchesSearchTerms(SPLITTER.split(upperSearch), keywords.collect(Collectors.toList()));
        }).forEach(this.historyItems::add);
        this.revalidate();
        // Idk why I need to repaint sometimes and the PluginListPanel doesn't
        this.repaint();
    }
}
