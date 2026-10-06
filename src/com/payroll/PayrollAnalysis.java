package com.payroll;

public class PayrollAnalysis {
    private final String formattedReport;
    private final double riskScore;
    private final String riskStatus;

    public PayrollAnalysis(String formattedReport, double riskScore, String riskStatus) {
        this.formattedReport = formattedReport;
        this.riskScore = riskScore;
        this.riskStatus = riskStatus;
    }

    public String getFormattedReport() { return formattedReport; }
    public double getRiskScore() { return riskScore; }
    public String getRiskStatus() { return riskStatus; }

    @Override
    public String toString() { return formattedReport; }
}
