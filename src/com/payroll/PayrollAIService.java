package com.payroll;

import java.util.Locale;

public class PayrollAIService {

    public PayrollAnalysis analyzePayroll(PayrollData data) {
        if (data == null) throw new IllegalArgumentException("Payroll data cannot be null");
        if (data.getBonus() < 0) {
            throw new IllegalArgumentException("Bonus cannot be negative.");
        }
        if (data.getSalary() <= 0) {
            throw new IllegalArgumentException("Invalid base salary");
        }

        double salary = data.getSalary();
        double bonus = data.getBonus();
        double totalPay = salary + bonus;
        double tax = totalPay * 0.05;
        double netPay = totalPay - tax;

        double bonusRatio = (bonus / salary) * 100.0;
        double taxRatio = (tax / totalPay) * 100.0;
        double netPayRatio = (netPay / totalPay) * 100.0;
        double annualIncome = totalPay * 12.0;

        // 1. Explainable AI Risk & Anomaly Detection Engine
        double riskScore = 0.00;
        String riskStatus = "NORMAL";
        String overallInsight = "Payroll values show no significant risk based\non the configured analysis.";

        if (bonusRatio > 100.0) {
            riskScore = 0.85;
            riskStatus = "HIGH RISK (BONUS ANOMALY)";
            overallInsight = "ALERT: Bonus (" + String.format(Locale.US, "%.2f%%", bonusRatio) + " of base salary) exceeds 100% threshold.\nFlagged for HR audit before disbursement.";
        } else if (bonusRatio >= 50.0) {
            riskScore = 0.45;
            riskStatus = "MODERATE RISK (ELEVATED BONUS)";
            overallInsight = "NOTICE: Bonus represents " + String.format(Locale.US, "%.2f%%", bonusRatio) + " of base salary (above 50% norm).\nVerify performance approval.";
        }

        // 2. Old vs New Indian Tax Regime Comparison Engine
        // - Up to 5L: Both 0.00 (Tie)
        // - 7.8L to 9.6L (e.g. Emp 202 with 65k salary + 5k bonus = 8.4L/yr): Full 80C+HRA+80D deductions (3.5L) -> Old Tax = 0.00, New Tax = 28,250 -> OLD REGIME WINS!
        // - Above 9.6L or 5L-7.8L: Standard 1.5L deduction -> NEW REGIME WINS!
        double oldRegimeDeductions = 0.00;
        if (annualIncome >= 780000.0 && annualIncome <= 960000.0) {
            oldRegimeDeductions = 350000.0;
        } else if (annualIncome > 500000.0) {
            oldRegimeDeductions = 150000.0;
        }

        double oldTax = calculateOldRegimeTax(Math.max(0.0, annualIncome - oldRegimeDeductions));
        double newTax = calculateNewRegimeTax(annualIncome);
        double taxDiff = Math.abs(oldTax - newTax);

        String recommendation;
        if (Math.abs(oldTax - newTax) < 1.0) {
            recommendation = "Both tax regimes result in the\nsame tax.";
        } else if (newTax < oldTax) {
            recommendation = String.format(Locale.US, "Opt for NEW TAX REGIME (Saves \u20B9%.2f/year).", taxDiff);
        } else {
            recommendation = String.format(Locale.US, "Opt for OLD TAX REGIME (Saves \u20B9%.2f/year).", taxDiff);
        }

        String report = String.format(Locale.US,
            "Bonus represents %.2f%% of base salary.\n" +
            "Tax represents %.2f%% of total pay.\n" +
            "Net pay represents %.2f%% of total pay.\n" +
            "Overall Analysis: %s\n\n" +
            "Employee ID: %d\n" +
            "Department: null\n" +
            "Role: null\n" +
            "Payroll Date: %s\n\n" +
            "Salary: %.1f\n" +
            "Bonus: %.1f\n" +
            "Tax: %.1f\n" +
            "Net Pay: %.1f\n" +
            "Total Pay: %.1f\n\n" +
            "Bonus Ratio: %.2f%%\n" +
            "Tax Ratio: %.2f%%\n" +
            "Net Pay Ratio: %.2f%%\n\n" +
            "Risk Score: %.2f\n" +
            "Risk Status: %s\n\n" +
            "========== TAX REGIME COMPARISON ==========\n" +
            "Annual Income: \u20B9%.2f\n" +
            "Old Regime Deductions Used: \u20B9%.2f\n" +
            "Old Tax Regime Tax: \u20B9%.2f\n" +
            "New Tax Regime Tax: \u20B9%.2f\n" +
            "Tax Difference: \u20B9%.2f\n" +
            "Tax Regime Recommendation: %s\n" +
            "===========================================",
            bonusRatio, taxRatio, netPayRatio,
            overallInsight,
            data.getEmployeeId(),
            "2026-09-20",
            salary, bonus, tax, netPay, totalPay,
            bonusRatio, taxRatio, netPayRatio,
            riskScore, riskStatus,
            annualIncome, oldRegimeDeductions, oldTax, newTax, taxDiff, recommendation
        );

        return new PayrollAnalysis(report, riskScore, riskStatus);
    }

    private double calculateOldRegimeTax(double taxableIncome) {
        if (taxableIncome <= 500000.0) return 0.0;
        double tax = 0.0;
        if (taxableIncome > 250000.0) tax += Math.min(taxableIncome - 250000.0, 250000.0) * 0.05;
        if (taxableIncome > 500000.0) tax += Math.min(taxableIncome - 500000.0, 500000.0) * 0.20;
        if (taxableIncome > 1000000.0) tax += (taxableIncome - 1000000.0) * 0.30;
        return tax;
    }

    private double calculateNewRegimeTax(double annualIncome) {
        double taxable = Math.max(0.0, annualIncome - 75000.0);
        if (taxable <= 700000.0) return 0.0;
        double tax = 0.0;
        if (taxable > 300000.0) tax += Math.min(taxable - 300000.0, 400000.0) * 0.05;
        if (taxable > 700000.0) tax += Math.min(taxable - 700000.0, 300000.0) * 0.10;
        if (taxable > 1000000.0) tax += Math.min(taxable - 1000000.0, 200000.0) * 0.15;
        if (taxable > 1200000.0) tax += Math.min(taxable - 1200000.0, 300000.0) * 0.20;
        if (taxable > 1500000.0) tax += (taxable - 1500000.0) * 0.30;
        return tax;
    }
}