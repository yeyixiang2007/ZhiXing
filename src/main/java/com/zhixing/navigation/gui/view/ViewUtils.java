package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ViewUtils {
    private ViewUtils() {
    }

    public static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(UiStyles.formLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
        gbc.weightx = 0;
    }

    public static DefaultTableModel createReadOnlyTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static <T> void selectComboByMatcher(JComboBox<T> comboBox, Matcher<T> matcher) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            T item = comboBox.getItemAt(i);
            if (matcher.match(item)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    public static void sortVertices(List<Vertex> vertices) {
        Collections.sort(vertices, new Comparator<Vertex>() {
            @Override
            public int compare(Vertex left, Vertex right) {
                return left.getId().compareTo(right.getId());
            }
        });
    }

    public static void sortRoads(List<Edge> roads) {
        Collections.sort(roads, new Comparator<Edge>() {
            @Override
            public int compare(Edge left, Edge right) {
                String leftKey = left.getFromVertex().getId() + "->" + left.getToVertex().getId();
                String rightKey = right.getFromVertex().getId() + "->" + right.getToVertex().getId();
                return leftKey.compareTo(rightKey);
            }
        });
    }

    public interface Matcher<T> {
        boolean match(T value);
    }
}
