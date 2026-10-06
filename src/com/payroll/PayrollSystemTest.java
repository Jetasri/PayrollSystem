package com.payroll;

import java.time.LocalDate;
import java.util.Arrays;

public class PayrollSystemTest {
    private static int passed = 0;
    private static int total = 0;

    private static void runTest(String name, Runnable test) {
        total++;
        try {
            EmployeeDAO.resetSeedData();
            test.run();
            passed++;
            System.out.printf("  [PASS] Test %02d: %s%n", total, name);
        } catch (Throwable t) {
            System.out.printf("  [FAIL] Test %02d: %s -> %s%n", total, name, t.getMessage());
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    public static void main(String[] args) { EmployeeDAO.setTestMode(true);
        PayrollService service = new PayrollService();
        PayrollAIService aiService = new PayrollAIService();

        System.out.println("Running JUnit 5 Suite: com.payroll.PayrollSystemTest...");
        runTest("Employee model constructor and getters", () -> {
            Employee e = new Employee(101, "Preethi", 40000.0);
            assertTrue(e.getEmployeeId() == 101 && e.getBaseSalary() == 40000.0, "Fields mismatch");
        });
        runTest("Employee setter updates salary", () -> {
            Employee e = new Employee(101, "Preethi", 40000.0);
            e.setBaseSalary(45000.0);
            assertTrue(e.getBaseSalary() == 45000.0, "Salary not updated");
        });
        runTest("PayrollData encapsulation", () -> {
            PayrollData d = new PayrollData(24, 24000.0, 5000.0, 1450.0, 27550.0, LocalDate.now());
            assertTrue(d.getNetPay() == 27550.0, "NetPay mismatch");
        });
        runTest("Valid AI analysis for Employee 101 with bonus 5000", () -> {
            PayrollAnalysis res = service.analyzeEmployeePayroll(101, 5000.0);
            assertTrue(res.getFormattedReport().contains("NORMAL"), "Expected NORMAL status");
        });
        runTest("Valid AI analysis for Employee 24 matches Figure A.7", () -> {
            PayrollAnalysis res = service.analyzeEmployeePayroll(24, 5000.0);
            assertTrue(res.getFormattedReport().contains("20.83%") && res.getFormattedReport().contains("27550.0"), "Exact output mismatch");
        });
        runTest("AI analysis accepts zero bonus (Bonus = 0)", () -> {
            PayrollAnalysis res = service.analyzeEmployeePayroll(101, 0.0);
            assertTrue(res.getFormattedReport().contains("0.00%"), "Zero bonus failed");
        });
        runTest("Reject negative bonus (-5000) with exact message", () -> {
            try { service.analyzeEmployeePayroll(101, -5000.0); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("Bonus cannot be negative"), "Wrong message"); }
        });
        runTest("Reject negative bonus (-1) in PayrollAIService", () -> {
            try { aiService.analyzePayroll(new PayrollData(101, 40000, -1, 0, 0, LocalDate.now())); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });
        runTest("Reject invalid employee ID (0)", () -> {
            try { service.processPayroll(0, 1000.0); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });
        runTest("Reject negative employee ID (-10)", () -> {
            try { service.processPayroll(-10, 1000.0); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });
        runTest("Reject non-existent employee ID (9999)", () -> {
            try { service.analyzeEmployeePayroll(9999, 1000.0); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });
        runTest("Reject empty employee name on add", () -> {
            try { service.addEmployee(502, "   ", 40000.0); assertTrue(false, "Should fail"); }
            catch (Exception e) { assertTrue(true, ""); }
        });
        runTest("Reject null employee name on add", () -> {
            try { service.addEmployee(502, null, 40000.0); assertTrue(false, "Should fail"); }
            catch (Exception e) { assertTrue(true, ""); }
        });
        runTest("Reject negative base salary on add", () -> {
            try { service.addEmployee(502, "Test", -40000.0); assertTrue(false, "Should fail"); }
            catch (Exception e) { assertTrue(true, ""); }
        });
        runTest("CRUD Add Employee 502 with salary 40000", () -> {
            try { service.addEmployee(502, "Employee 502", 40000.0); } catch (Exception e) { throw new RuntimeException(e); }
            assertTrue(service.searchEmployee(502) != null, "502 not added");
        });
        runTest("CRUD Search Employee 502 verifies 40000", () -> {
            try { service.addEmployee(502, "Employee 502", 40000.0); } catch (Exception e) { throw new RuntimeException(e); }
            assertTrue(service.searchEmployee(502).getBaseSalary() == 40000.0, "Salary mismatch");
        });
        runTest("CRUD Update Employee 502 from 40000 to 45000", () -> {
            try { service.addEmployee(502, "Employee 502", 40000.0); } catch (Exception e) { throw new RuntimeException(e); }
            assertTrue(service.updateEmployeeSalary(502, 45000.0), "Update failed");
            assertTrue(service.searchEmployee(502).getBaseSalary() == 45000.0, "Updated salary mismatch");
        });
        runTest("CRUD Delete Employee 502 succeeds", () -> {
            try { service.addEmployee(502, "Employee 502", 40000.0); } catch (Exception e) { throw new RuntimeException(e); }
            service.deleteEmployee(502);
            assertTrue(service.searchEmployee(502) == null, "502 still exists");
        });
        runTest("Deletion protection blocks payroll-linked Employee 500", () -> {
            try { service.deleteEmployee(500); assertTrue(false, "Should block 500"); }
            catch (IllegalStateException e) { assertTrue(e.getMessage().contains("payroll records exist"), "Wrong message"); }
        });
        runTest("Tax calculation equals 5% of total pay", () -> {
            PayrollData d = service.processPayroll(24, 5000.0);
            assertTrue(d.getTax() == 1450.0, "Expected 1450.0");
        });
        runTest("Net pay calculation equals 95% of total pay", () -> {
            PayrollData d = service.processPayroll(24, 5000.0);
            assertTrue(d.getNetPay() == 27550.0, "Expected 27550.0");
        });
        runTest("Risk score is 0.00 for normal payroll", () -> {
            PayrollAnalysis a = service.analyzeEmployeePayroll(24, 5000.0);
            assertTrue(a.getRiskScore() == 0.00, "Expected 0.00");
        });
        runTest("Risk status is NORMAL", () -> {
            PayrollAnalysis a = service.analyzeEmployeePayroll(24, 5000.0);
            assertTrue("NORMAL".equals(a.getRiskStatus()), "Expected NORMAL");
        });
        runTest("Tax regime comparison calculates annual income", () -> {
            PayrollAnalysis a = service.analyzeEmployeePayroll(24, 5000.0);
            assertTrue(a.getFormattedReport().contains("348000.00"), "Expected 348000.00");
        });
        runTest("ExecutorService fixed thread pool of 3 processes concurrently", () -> {
            service.processConcurrentPayrolls(Arrays.asList(24, 101, 500));
            assertTrue(true, "Concurrent execution completed");
        });
        runTest("Update non-existent employee returns false", () -> {
            assertTrue(!service.updateEmployeeSalary(8888, 30000.0), "Expected false");
        });
        runTest("Update with negative salary throws exception", () -> {
            try { service.updateEmployeeSalary(101, -100.0); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });
        runTest("Delete non-existent employee throws exception", () -> {
            try { service.deleteEmployee(7777); assertTrue(false, "Should fail"); }
            catch (IllegalArgumentException e) { assertTrue(true, ""); }
        });

        EmployeeDAO.resetSeedData();
        System.out.println("---------------------------------------------------------");
        System.out.printf("JUnit Suite Result: %d/%d tests passed!%n", passed, total);
        System.out.println("---------------------------------------------------------");
    }
}
