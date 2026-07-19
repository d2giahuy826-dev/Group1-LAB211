package simulation;

import model.PayrollEntry;
import model.PayStatus;
import repository.PayrollEntryRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class PayrollSimulationService {
    private static final String HEADER = "entryId,empId,deptId,month,year,baseSalary,overtimePay,absenceDeduction,bonus,taxAmount,netSalary,status,version,processedAt";
    private final Object monitor = new Object();
    private final Set<String> optimisticVersions = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> processedIds = Collections.synchronizedSet(new HashSet<>());

    public int run(int threadCount, int departmentCount, int employeesPerDepartment,
                   LockMechanism mechanism, String sourceCsv, String outputCsv) {
        if (threadCount < 1 || departmentCount < 1 || employeesPerDepartment < 1)
            throw new IllegalArgumentException("Thread, department and employee counts must be positive");

        int limit = departmentCount * employeesPerDepartment;
        List<PayrollEntry> source = new PayrollEntryRepository(sourceCsv).findAll();
        List<PayrollEntry> pending = new ArrayList<>();
        for (PayrollEntry entry : source) {
            if (entry.getStatus() == PayStatus.PENDING && pending.size() < limit) pending.add(entry);
        }
        if (pending.isEmpty()) {
            for (PayrollEntry entry : source) {
                if (pending.size() >= limit) break;
                pending.add(entry);
            }
        }
        prepareOutput(outputCsv);
        optimisticVersions.clear();
        processedIds.clear();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (mechanism == LockMechanism.FILE_LOCK) {
                        processFileLockBatch(pending, outputCsv, success);
                    } else {
                        for (PayrollEntry entry : pending) {
                            if (process(entry, mechanism, outputCsv)) success.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        try {
            ready.await();
            start.countDown();
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try { executor.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return success.get();
    }

    private boolean process(PayrollEntry entry, LockMechanism mechanism, String outputCsv) {
        switch (mechanism) {
            case NO_LOCK:
                appendUnsafe(outputCsv, processedLine(entry));
                return true;
            case SYNCHRONIZED:
                synchronized (monitor) {
                    if (!processedIds.add(entry.getId())) return false;
                    appendSafe(outputCsv, processedLine(entry));
                    return true;
                }
            case OPTIMISTIC:
                String token = entry.getId() + "@" + entry.getVersion();
                if (!optimisticVersions.add(token)) return false;
                synchronized (monitor) { appendSafe(outputCsv, processedLine(entry)); }
                return true;
            case FILE_LOCK:
                throw new IllegalStateException("FILE_LOCK must be processed as a batch");
            default:
                return false;
        }
    }

    private void processFileLockBatch(List<PayrollEntry> entries, String path,
                                      AtomicInteger success) {
        StringBuilder batch = new StringBuilder();
        int claimed = 0;
        for (PayrollEntry entry : entries) {
            if (processedIds.add(entry.getId())) {
                batch.append(processedLine(entry)).append(System.lineSeparator());
                claimed++;
            }
        }
        if (claimed == 0) return;
        // FileChannel.lock protects different processes. The JVM monitor prevents
        // overlapping FileLock requests from threads inside this same process.
        synchronized (monitor) {
            appendBatchWithFileLock(path, batch.toString());
        }
        success.addAndGet(claimed);
    }

    private void appendBatchWithFileLock(String path, String batch) {
        try (FileChannel channel = FileChannel.open(Path.of(path), StandardOpenOption.READ,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             java.nio.channels.FileLock ignored = channel.lock()) {
            channel.position(channel.size());
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(batch);
            while (buffer.hasRemaining()) channel.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException("FILE_LOCK write failed", e);
        }
    }

    private String processedLine(PayrollEntry source) {
        PayrollEntry copy = new PayrollEntry();
        copy.fromCsvLine(source.toCsvLine());
        copy.setStatus(PayStatus.PROCESSED);
        copy.setVersion(source.getVersion() + 1);
        copy.setProcessedAt(LocalDate.now().toString());
        return copy.toCsvLine();
    }

    private void prepareOutput(String path) {
        Path output = Path.of(path);
        IOException lastError = null;
        for (int attempt = 1; attempt <= 20; attempt++) {
            try {
                if (output.getParent() != null) Files.createDirectories(output.getParent());
                Files.writeString(output, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
                return;
            } catch (IOException e) {
                lastError = e;
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while preparing simulation output: " + output,
                            interrupted);
                }
            }
        }
        throw new RuntimeException("Cannot prepare simulation output after 20 retries: " + output,
                lastError);
    }

    private void appendSafe(String path, String line) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            writer.write(line); writer.newLine();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private void appendUnsafe(String path, String line) {
        // No shared Java/file lock: this path intentionally exposes duplicate payments.
        appendSafe(path, line);
    }
}
