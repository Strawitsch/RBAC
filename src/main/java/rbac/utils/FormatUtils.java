package rbac.utils;

import java.util.ArrayList;
import java.util.List;

public final class FormatUtils {
    private FormatUtils() {}

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (rows.isEmpty()) return "No data.";
        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < columnWidths.length; i++) {
                if (row[i] != null && row[i].length() > columnWidths[i]) {
                    columnWidths[i] = row[i].length();
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        // top border
        sb.append("+");
        for (int width : columnWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        sb.append("\n");

        // header
        sb.append("|");
        for (int i = 0; i < headers.length; i++) {
            sb.append(" ").append(padRight(headers[i], columnWidths[i])).append(" |");
        }
        sb.append("\n");

        // separator
        sb.append("+");
        for (int width : columnWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        sb.append("\n");

        // rows
        for (String[] row : rows) {
            sb.append("|");
            for (int i = 0; i < columnWidths.length; i++) {
                String cell = (i < row.length && row[i] != null) ? row[i] : "";
                sb.append(" ").append(padRight(cell, columnWidths[i])).append(" |");
            }
            sb.append("\n");
        }

        // bottom border
        sb.append("+");
        for (int width : columnWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }

    public static String formatBox(String text) {
        int width = text.length() + 4;
        StringBuilder sb = new StringBuilder();
        sb.append("+").append("-".repeat(width - 2)).append("+\n");
        sb.append("| ").append(text).append(" |\n");
        sb.append("+").append("-".repeat(width - 2)).append("+");
        return sb.toString();
    }

    public static String formatHeader(String text) {
        return "\n=== " + text.toUpperCase() + " ===\n";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        return String.format("%-" + length + "s", text);
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        return String.format("%" + length + "s", text);
    }
}
