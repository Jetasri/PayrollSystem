package com.payroll;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EmployeeDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/payroll_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    private static final File DB_FILE = new File("payroll_data.csv");
    private static final Map<Integer, Employee> DB_STORE = new ConcurrentHashMap<>();
    private static final Set<Integer> PAYROLL_LINKED_IDS = ConcurrentHashMap.newKeySet();
    private static boolean testMode = false;

    static {
        loadFromDisk();
    }

    public static void setTestMode(boolean mode) {
        testMode = mode;
    }

    public static void resetSeedData() {
        DB_STORE.clear();
        PAYROLL_LINKED_IDS.clear();
        // 1. Employee 24 -> Both Tax Regimes Tie (Rs. 0.00 tax, matches Report Fig A.7)
        DB_STORE.put(24, new Employee(24, "Karthik R", 24000.0));
        // 2. Employee 101 -> New Tax Regime Wins (or Moderate/High Risk demo)
        DB_STORE.put(101, new Employee(101, "T. Preethi", 40000.0));
        // 3. Employee 202 -> Old Tax Regime Wins (Rs. 65,000/mo + Rs. 5,000 bonus = Rs. 8.4L/yr with HRA/80C/80D)
        DB_STORE.put(202, new Employee(202, "DK Jeta Sri", 65000.0));
        // 4. Employee 500 -> Payroll-Linked Deletion Protection (Matches Report Fig 5.4)
        DB_STORE.put(500, new Employee(500, "Arjun Kumar", 90000.0));
        PAYROLL_LINKED_IDS.add(500);
        if (!testMode) saveToDisk();
    }

    public static void reloadFromDisk() {
        loadFromDisk();
    }

    private static void loadFromDisk() {
        if (!DB_FILE.exists()) {
            resetSeedData();
            saveToDisk();
            return;
        }
        try {
            DB_STORE.clear();
            PAYROLL_LINKED_IDS.clear();
            List<String> lines = Files.readAllLines(DB_FILE.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length >= 4) {
                    int id = Integer.parseInt(p[0].trim());
                    String name = p[1].trim();
                    double sal = Double.parseDouble(p[2].trim());
                    boolean linked = Boolean.parseBoolean(p[3].trim());
                    DB_STORE.put(id, new Employee(id, name, sal));
                    if (linked) PAYROLL_LINKED_IDS.add(id);
                }
            }
            if (DB_STORE.isEmpty()) resetSeedData();
        } catch (Exception e) {
            resetSeedData();
        }
    }

    private static void saveToDisk() {
        if (testMode) return;
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(DB_FILE), StandardCharsets.UTF_8))) {
            List<Employee> list = new ArrayList<>(DB_STORE.values());
            list.sort(Comparator.comparingInt(Employee::getEmployeeId));
            for (Employee e : list) {
                boolean linked = PAYROLL_LINKED_IDS.contains(e.getEmployeeId());
                out.printf(Locale.US, "%d,%s,%.2f,%b%n", e.getEmployeeId(), e.getName().replace(",", " "), e.getBaseSalary(), linked);
            }
        } catch (IOException ignored) {}
    }

    public void executeJdbcPreparedStatementDemo(Connection conn, int employeeId, String name, double salary) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO employee (employee_id, name, base_salary) VALUES (?, ?, ?)");
        ps.setInt(1, employeeId);
        ps.setString(2, name);
        ps.setDouble(3, salary);
        ps.executeUpdate();
        conn.commit();
    }

    public void addEmployee(Employee emp) throws SQLException {
        int employeeId = emp.getEmployeeId();
        String name = emp.getName();
        double salary = emp.getBaseSalary();

        if (employeeId <= 0) throw new IllegalArgumentException("Invalid employee ID");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Required fields cannot be empty.");
        if (salary < 0) throw new IllegalArgumentException("Salary/bonus cannot be negative");

        DB_STORE.put(employeeId, new Employee(employeeId, name.trim(), salary));
        saveToDisk();
    }

    public Employee getEmployee(int employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Invalid employee ID");
        return DB_STORE.get(employeeId);
    }

    public List<Employee> getAllEmployees() {
        List<Employee> list = new ArrayList<>(DB_STORE.values());
        list.sort(Comparator.comparingInt(Employee::getEmployeeId));
        return list;
    }

    public boolean isPayrollLinked(int employeeId) {
        return PAYROLL_LINKED_IDS.contains(employeeId);
    }

    public boolean updateSalary(int employeeId, double newSalary) {
        if (employeeId <= 0) throw new IllegalArgumentException("Invalid employee ID");
        if (newSalary < 0) throw new IllegalArgumentException("Salary/bonus cannot be negative");
        Employee existing = DB_STORE.get(employeeId);
        if (existing == null) return false;
        existing.setBaseSalary(newSalary);
        saveToDisk();
        return true;
    }

    public void deleteEmployee(int employeeId) {
        if (employeeId <= 0) throw new IllegalArgumentException("Invalid employee ID");
        if (PAYROLL_LINKED_IDS.contains(employeeId)) {
            throw new IllegalStateException("Employee cannot be deleted because payroll records exist for this employee.");
        }
        if (!DB_STORE.containsKey(employeeId)) {
            throw new IllegalArgumentException("Invalid employee ID");
        }
        DB_STORE.remove(employeeId);
        saveToDisk();
    }

    public void linkPayrollRecord(int employeeId) {
        PAYROLL_LINKED_IDS.add(employeeId);
        saveToDisk();
    }
}