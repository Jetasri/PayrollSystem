package com.payroll;

import java.time.LocalDate;

public class PayrollData {
    private int employeeId;
    private double salary;
    private double bonus;
    private double tax;
    private double netPay;
    private LocalDate payrollDate;

    public PayrollData(int employeeId, double salary, double bonus, double tax, double netPay, LocalDate payrollDate) {
        this.employeeId = employeeId;
        this.salary = salary;
        this.bonus = bonus;
        this.tax = tax;
        this.netPay = netPay;
        this.payrollDate = payrollDate;
    }

    public int getEmployeeId() { return employeeId; }
    public double getSalary() { return salary; }
    public double getBonus() { return bonus; }
    public double getTax() { return tax; }
    public double getNetPay() { return netPay; }
    public LocalDate getPayrollDate() { return payrollDate; }
}
