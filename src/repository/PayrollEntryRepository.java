package repository;

import exception.DuplicatePaymentException;
import exception.OptimisticLockException;
import java.io.*;
import java.util.*;
import model.PayrollEntry;
import model.PayStatus;
import util.ProjectPath;

public class PayrollEntryRepository {

    private final String filePath;

    public PayrollEntryRepository(String filePath) {
        this.filePath = ProjectPath.resolve(filePath).toString();
    }

    public List<PayrollEntry> findAll() {
        List<PayrollEntry> entries = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            return entries;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isHeaderLine(line, "entryId")) continue;

                try {
                    PayrollEntry entry = new PayrollEntry();
                    entry.fromCsvLine(line);
                    entries.add(entry);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: skip invalid payroll CSV row: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Cannot read payroll entries file.", e);
        }

        return entries;
    }

    private boolean isHeaderLine(String line, String firstColumnName) {
        String[] parts = line.split(",", -1);
        return parts.length > 0 && parts[0].trim().equalsIgnoreCase(firstColumnName);
    }

    public PayrollEntry findById(String id) {
        for (PayrollEntry entry : findAll()) {
            if (entry.getId().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    public void save(PayrollEntry newEntry) {
        List<PayrollEntry> entries = findAll();

        for (PayrollEntry entry : entries) {
            if (entry.getId().equals(newEntry.getId())) {
                throw new DuplicatePaymentException(
                        "Payroll entry already exists: " + newEntry.getId()
                );
            }
        }

        entries.add(newEntry);
        writeAll(entries);
    }

    public synchronized void processWithSync(String payrollId) {
        PayrollEntry entry = findById(payrollId);

        if (entry == null) {
            throw new IllegalArgumentException("Payroll entry not found: " + payrollId);
        }

        if (entry.isProcessed() || entry.getStatus() == PayStatus.PROCESSED) {
            throw new DuplicatePaymentException(
                    "Payroll already processed: " + payrollId
            );
        }

        entry.setStatus(PayStatus.PROCESSED);
        update(entry);
    }

    public void processWithOptimistic(PayrollEntry updatedEntry, int expectedVersion) {
        PayrollEntry current = findById(updatedEntry.getId());

        if (current == null) {
            throw new IllegalArgumentException("Payroll entry not found: " + updatedEntry.getId());
        }

        if (current.getVersion() != expectedVersion) {
            throw new OptimisticLockException(
                    "Version conflict. Expected: " + expectedVersion +
                    ", Current: " + current.getVersion()
            );
        }

        if (current.isProcessed() || current.getStatus() == PayStatus.PROCESSED) {
            throw new DuplicatePaymentException(
                    "Payroll already processed: " + updatedEntry.getId()
            );
        }

        updatedEntry.setStatus(PayStatus.PROCESSED);
        updatedEntry.setVersion(expectedVersion + 1);

        update(updatedEntry);
    }

    public void update(PayrollEntry updatedEntry) {
        List<PayrollEntry> entries = findAll();
        boolean found = false;

        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId().equals(updatedEntry.getId())) {
                entries.set(i, updatedEntry);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Payroll entry not found: " + updatedEntry.getId());
        }

        writeAll(entries);
    }

    private void writeAll(List<PayrollEntry> entries) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (PayrollEntry entry : entries) {
                bw.write(entry.toCsvLine());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write payroll entries file.", e);
        }
    }
}
