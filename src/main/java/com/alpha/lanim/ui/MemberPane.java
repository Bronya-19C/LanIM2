package com.alpha.lanim.ui;

import com.alpha.lanim.model.MemberRow;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemberPane {

    private final VBox root;
    private final Label headerLabel;
    private final ListView<String> listView;
    private final Map<String, String> remotes = new LinkedHashMap<>();
    private String localNickname = "";

    public MemberPane() {
        headerLabel = new Label("Members (0)");
        headerLabel.getStyleClass().add("pane-header");

        listView = new ListView<>();
        listView.getStyleClass().add("member-list");

        root = new VBox(8);
        root.getStyleClass().addAll("sidebar", "sidebar-left");
        root.getChildren().addAll(headerLabel, listView);
        VBox.setVgrow(listView, javafx.scene.layout.Priority.ALWAYS);
    }

    public Node getNode() {
        return root;
    }

    public void setMembers(String localNickname, List<MemberRow> rows) {
        this.localNickname = localNickname != null ? localNickname : "";
        remotes.clear();
        if (rows != null) {
            for (MemberRow row : rows) {
                if (row == null) {
                    continue;
                }
                if (row.nickname().equals(this.localNickname)) {
                    continue;
                }
                remotes.put(row.peerId(), row.nickname());
            }
        }
        refresh();
    }

    public void addMember(String peerId, String nickname) {
        if (peerId == null || remotes.containsKey(peerId)) {
            return;
        }
        remotes.put(peerId, nickname);
        refresh();
    }

    public void removeMember(String peerId) {
        if (peerId != null && remotes.remove(peerId) != null) {
            refresh();
        }
    }

    public int size() {
        return remotes.size() + 1;
    }

    private void refresh() {
        listView.getItems().clear();
        listView.getItems().add(localNickname + " (You)");
        for (String nickname : remotes.values()) {
            listView.getItems().add(nickname + " [online]");
        }
        headerLabel.setText("Members (" + size() + ")");
    }
}
