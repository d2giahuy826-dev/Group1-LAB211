package exception;

public class CsvParseException extends RuntimeException {

    public CsvParseException(String message) {
        super(message);
    }

    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
    }

    // Optional detailed constructor used by CsvRepository: include line content and line number
    private String lineContent = null;
    private int lineNumber = 0;

    public CsvParseException(String message, String lineContent, int lineNumber) {
        super(message);
        this.lineContent = lineContent;
        this.lineNumber = lineNumber;
    }

    public String getLineContent() { return lineContent; }
    public int getLineNumber() { return lineNumber; }
}