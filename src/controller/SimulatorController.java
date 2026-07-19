package controller;

import repository.LeaveBalanceRepository;
import repository.LeaveRequestRepository;
import simulation.LockMechanism;
import simulation.PayrollSimulationService;
import simulation.SimulationDetector;
import simulation.SimulationResult;

import java.nio.file.Path;
import util.ProjectPath;

public class SimulatorController {
    private final PayrollSimulationService service;
    private final SimulationDetector detector;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveRequestRepository requestRepository;
    private final String sourceCsv;
    private final String outputDirectory;

    public SimulatorController(LeaveBalanceRepository balanceRepository,
                               LeaveRequestRepository requestRepository) {
        this(new PayrollSimulationService(), new SimulationDetector(), balanceRepository,
                requestRepository, "data/payroll_entries.csv", "data/simulation");
    }

    public SimulatorController(PayrollSimulationService service, SimulationDetector detector,
                               LeaveBalanceRepository balanceRepository,
                               LeaveRequestRepository requestRepository,
                               String sourceCsv, String outputDirectory) {
        this.service = service;
        this.detector = detector;
        this.balanceRepository = balanceRepository;
        this.requestRepository = requestRepository;
        this.sourceCsv = ProjectPath.resolve(sourceCsv).toString();
        this.outputDirectory = ProjectPath.resolve(outputDirectory).toString();
    }

    public SimulationResult runSimulation(int threads, int departments,
                                          int employeesPerDepartment,
                                          LockMechanism mechanism) {
        String output = Path.of(outputDirectory,
                mechanism.name().toLowerCase() + "_payroll.csv").toString();
        long started = System.nanoTime();
        int success = service.run(threads, departments, employeesPerDepartment,
                mechanism, sourceCsv, output);
        long elapsedMs = Math.max(1, (System.nanoTime() - started) / 1_000_000);
        int duplicates = detector.detectDoublePayments(output);
        int wrongLeave = detector.detectWrongLeaveDeductions(
                balanceRepository.loadAll(), requestRepository.loadAll());
        return new SimulationResult(mechanism, success, duplicates, wrongLeave, elapsedMs, output);
    }
}
