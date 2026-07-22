package simulation;

public class SimulationResult {
    public final LockMechanism mechanism;
    public final int success;
    public final int doublePayment;
    public final int wrongLeave;
    public final long elapsedMs;
    public final double tps;
    public final String outputFile;

    public SimulationResult(LockMechanism mechanism, int success, int doublePayment,
                            int wrongLeave, long elapsedMs, String outputFile) {
        this.mechanism = mechanism;
        this.success = success;
        this.doublePayment = doublePayment;
        this.wrongLeave = wrongLeave;
        this.elapsedMs = elapsedMs;
        this.tps = elapsedMs == 0 ? success * 1000.0 : success * 1000.0 / elapsedMs;
        this.outputFile = outputFile;
    }
}
