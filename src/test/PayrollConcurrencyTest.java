package test;

public class PayrollConcurrencyTest {

    private static boolean alreadyPaid = false;

    public static synchronized void processPayroll(String employeeId) {

        System.out.println(Thread.currentThread().getName()
                + " processing payroll for " + employeeId);

        if (alreadyPaid) {
            System.out.println("Double payment prevented!");
            return;
        }

        // Giả lập xử lý lâu
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        alreadyPaid = true;

        System.out.println("Payroll processed successfully for "
                + employeeId);
    }

    public static void main(String[] args) {

        Thread t1 = new Thread(() -> {
            processPayroll("EMP001");
        });

        Thread t2 = new Thread(() -> {
            processPayroll("EMP001");
        });

        t1.start();
        t2.start();
    }
}