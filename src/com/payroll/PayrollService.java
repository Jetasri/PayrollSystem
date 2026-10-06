package com.payroll;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

public class PayrollService {
    private final EmployeeDAO employeeDAO;
    private final PayrollAIService aiService;

    public PayrollService() {
        this.employeeDAO = new EmployeeDAO();
        this.aiService = new PayrollAIService();
    }

    public void addEmployee(int id, String name, double salary) throws SQLException {
        if (id <= 0) throw new IllegalArgumentException("Invalid employee ID");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Required fields cannot be empty.");
        if (salary < 0) throw new IllegalArgumentException("Salary/bonus cannot be negative");
        employeeDAO.addEmployee(new Employee(id, name, salary));
    }

    public Employee searchEmployee(int id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid employee ID");
        return employeeDAO.getEmployee(id);
    }

    public boolean updateEmployeeSalary(int id, double newSalary) {
        if (id <= 0) throw new IllegalArgumentException("Invalid employee ID");
        if (newSalary < 0) throw new IllegalArgumentException("Salary/bonus cannot be negative");
        return employeeDAO.updateSalary(id, newSalary);
    }

    public void deleteEmployee(int id) {
        if (id <= 0) throw new IllegalArgumentException("Invalid employee ID");
        employeeDAO.deleteEmployee(id);
    }

    public PayrollData processPayroll(int employeeId) {
        return processPayroll(employeeId, 0.0);
    }

    public PayrollData processPayroll(int employeeId, double bonus) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }
        if (bonus < 0) {
            throw new IllegalArgumentException("Bonus cannot be negative.");
        }
        Employee emp = employeeDAO.getEmployee(employeeId);
        if (emp == null) {
            throw new IllegalArgumentException("Invalid employee ID.");
        }
        double salary = emp.getBaseSalary();
        if (salary < 0 || bonus < 0) {
            throw new IllegalArgumentException("Salary/bonus cannot be negative");
        }
        double totalPay = salary + bonus;
        double tax = totalPay * 0.05;
        double netPay = totalPay - tax;
        System.out.println("[" + Thread.currentThread().getName() + "] Processed payroll for Employee ID: " + employeeId);
        return new PayrollData(employeeId, salary, bonus, tax, netPay, LocalDate.now());
    }

    public void processConcurrentPayrolls(List<Integer> employeeIds) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int id : employeeIds) {
            executor.submit(() -> processPayroll(id, 5000.0));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public PayrollAnalysis analyzeEmployeePayroll(int employeeId, double bonus) {
        if (bonus < 0) {
            throw new IllegalArgumentException("Bonus cannot be negative.");
        }
        PayrollData data = processPayroll(employeeId, bonus);
        return aiService.analyzePayroll(data);
    }
}
