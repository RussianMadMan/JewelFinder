package ru.rmm.jewelfinder;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.rmm.jewelfinder.PoeTradeRequest.MAX_SEEDS_GUEST;
import static ru.rmm.jewelfinder.PoeTradeRequest.MAX_SEEDS_LOGGED;

public class MainWindow {
    private JComboBox jewelChooser;
    private JButton doSearchButton;
    private JTextArea seedsTextArea;
    private JPanel root;
    private JTextField POESESSID;
    private JComboBox leagueChooser;
    private JPanel settingsPanel;
    private JScrollPane textScroll;
    private JPanel buttonPanel;

    private JewelType selected;
    private String league;
    private static Pattern p = Pattern.compile("^(\\d+).+");

    public static void main(String[] args) {
        JFrame frame = new JFrame("Timeless Searcher");
        frame.setLocationRelativeTo(null);
        frame.setContentPane(new MainWindow().root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static String doRegex(String line){
        var matcher = p.matcher(line);
        String result = null;
        if(matcher.find()){
            result = matcher.group(1);
        }
        return result;
    }

    public MainWindow()  {
        jewelChooser.setModel(new ComboBoxModel() {
            @Override
            public void setSelectedItem(Object anItem) {
                selected = (JewelType)anItem;
            }

            @Override
            public Object getSelectedItem() {
                return selected;
            }

            @Override
            public int getSize() {
                return JewelType.values().length;
            }

            @Override
            public Object getElementAt(int index) {
                return JewelType.values()[index];
            }

            @Override
            public void addListDataListener(ListDataListener l) {

            }

            @Override
            public void removeListDataListener(ListDataListener l) {

            }
        });
        jewelChooser.setSelectedIndex(0);
        doSearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String[] lines = seedsTextArea.getText().split("\n");
                StringBuilder seeds = new StringBuilder();
                var linesList = Arrays  .stream(lines)
                                        .map(String::trim)
                                        .map(MainWindow::doRegex)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                String cookie = POESESSID.getText();
                String league = (String)leagueChooser.getSelectedItem();
                cookie = cookie.trim();
                if(linesList.size() == 0) return;
                if(linesList.size() > MAX_SEEDS_GUEST && cookie.isEmpty() || linesList.size() > MAX_SEEDS_LOGGED){
                    JOptionPane.showMessageDialog(root, "Maximum seeds is " + MAX_SEEDS_GUEST + " for quest and " + MAX_SEEDS_LOGGED + " for logged users");
                    return;
                }
                JewelType type = (JewelType)jewelChooser.getSelectedItem();
                PoeTradeRequest req;
                if(cookie.isEmpty()) {
                    req = new PoeTradeRequest(linesList, type, league);
                }else{
                    req = new PoeTradeRequest(linesList, type, league, cookie);
                }
                var result = req.doRequest();
                if(result.ex != null){
                    JOptionPane.showMessageDialog(root, result.ex);
                }else if(result.statusLine != null){
                    JOptionPane.showMessageDialog(root, result.statusLine);
                }else{
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(result.url);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(root, ex);
                        }
                    }
                }
                saveSettings();
            }


        });
        try {
            var leagues = PoeTradeRequest.getLeagues();
            leagueChooser.setModel(new ComboBoxModel() {
                @Override
                public void setSelectedItem(Object anItem) {
                    league = (String)anItem;
                }

                @Override
                public Object getSelectedItem() {
                    return league;
                }

                @Override
                public int getSize() {
                    return leagues.size();
                }

                @Override
                public Object getElementAt(int index) {
                    return leagues.get(index);
                }

                @Override
                public void addListDataListener(ListDataListener l) {

                }

                @Override
                public void removeListDataListener(ListDataListener l) {

                }
            });
            restoreSettings();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error retrieving leagues\n" + e.getStackTrace());
            return;
        }

    }

    private void createUIComponents() {
        POESESSID = new HintTextField("POESESSID cookie");
    }

    private void saveSettings(){
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.put("POESESSID", POESESSID.getText());
        prefs.put("League", (String)leagueChooser.getSelectedItem());
    }

    private void restoreSettings(){
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        POESESSID.setText(prefs.get("POESESSID", ""));
        String league = prefs.get("League", null);
        if(league != null) {
            leagueChooser.setSelectedItem(league);
        }
    }
}
