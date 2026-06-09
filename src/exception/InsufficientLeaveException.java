package exception;

public class InsufficientLeaveException extends Exception {

    public InsufficientLeaveException(String message) {
        super(message);
    }
}
if (daysRequested > remainingDays) {
    throw new InsufficientLeaveException(
        "Not enough leave balance."
    );
}       