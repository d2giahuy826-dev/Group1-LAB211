package model;

public interface CsvMappable {
    String toCsvLine();
    void fromCsvLine(String csvLine);
}