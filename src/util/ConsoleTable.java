package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Small ASCII table renderer that works consistently in Windows terminals. */
public class ConsoleTable {
    private final String[] headers;
    private final List<String[]> rows = new ArrayList<>();

    public ConsoleTable(String... headers) {
        this.headers = headers;
    }

    public ConsoleTable addRow(Object... values) {
        String[] row = Arrays.stream(values)
                .map(value -> value == null ? "" : String.valueOf(value))
                .toArray(String[]::new);
        if (row.length != headers.length)
            throw new IllegalArgumentException("Row must have " + headers.length + " columns");
        rows.add(row);
        return this;
    }

    public void print() {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) widths[i] = headers[i].length();
        for (String[] row : rows)
            for (int i = 0; i < row.length; i++) widths[i] = Math.max(widths[i], row[i].length());

        printBorder(widths);
        printRow(headers, widths);
        printBorder(widths);
        for (String[] row : rows) printRow(row, widths);
        printBorder(widths);
    }

    private void printBorder(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : widths) line.append("-").append("-".repeat(width)).append("-+");
        System.out.println(line);
    }

    private void printRow(String[] values, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < values.length; i++)
            line.append(' ').append(String.format("%-" + widths[i] + "s", values[i])).append(" |");
        System.out.println(line);
    }
}
