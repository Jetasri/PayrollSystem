package com.payroll;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PayrollServlet {

    public static void main(String[] args) throws Exception {
        EmployeeDAO.setTestMode(false);
        EmployeeDAO.reloadFromDisk();
        PayrollService service = new PayrollService();
        EmployeeDAO dao = new EmployeeDAO();
        service.processConcurrentPayrolls(Arrays.asList(24, 101, 500));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> handleStatic(exchange, "index.html"));
        server.createContext("/add", exchange -> handleStatic(exchange, "add.html"));
        server.createContext("/search", exchange -> handleStatic(exchange, "search.html"));
        server.createContext("/update", exchange -> handleStatic(exchange, "update.html"));
        server.createContext("/delete", exchange -> handleStatic(exchange, "delete.html"));
        server.createContext("/ai", exchange -> handleStatic(exchange, "ai.html"));

        server.createContext("/api/list", exchange -> {
            StringBuilder sb = new StringBuilder("[");
            List<Employee> list = dao.getAllEmployees();
            for (int i = 0; i < list.size(); i++) {
                Employee e = list.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format(Locale.US,
                    "{\"id\":%d,\"name\":\"%s\",\"salary\":%.2f,\"protected\":%b}",
                    e.getEmployeeId(), e.getName().replace("\"", ""), e.getBaseSalary(), dao.isPayrollLinked(e.getEmployeeId())));
            }
            sb.append("]");
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        });

        server.createContext("/api/add", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String idStr = params.getOrDefault("id", "").trim();
                String name = params.getOrDefault("name", "").trim();
                String salStr = params.getOrDefault("salary", "").trim();
                if (idStr.isEmpty() || name.isEmpty() || salStr.isEmpty()) {
                    sendText(exchange, "Required fields cannot be empty.");
                    return;
                }
                int id = Integer.parseInt(idStr);
                double salary = Double.parseDouble(salStr);
                service.addEmployee(id, name, salary);
                sendText(exchange, "SUCCESS: Employee " + id + " (" + name + ") saved to database with Base Salary Rs. " + String.format(Locale.US, "%.2f", salary));
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid numeric input for Employee ID or Base Salary.");
            } catch (Exception e) {
                sendText(exchange, e.getMessage());
            }
        });

        server.createContext("/api/search", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String idStr = params.getOrDefault("id", "").trim();
                if (idStr.isEmpty()) { sendText(exchange, "Required fields cannot be empty."); return; }
                int id = Integer.parseInt(idStr);
                Employee emp = service.searchEmployee(id);
                if (emp == null) {
                    sendText(exchange, "Employee ID " + id + " not found in database.");
                } else {
                    String status = dao.isPayrollLinked(id) ? "Payroll Processed (Deletion Protected)" : "Active (No Payroll Processed Yet)";
                    sendText(exchange,
                        "=== EMPLOYEE RECORD FOUND ===\n" +
                        "Employee ID    : " + emp.getEmployeeId() + "\n" +
                        "Employee Name  : " + emp.getName() + "\n" +
                        "Base Salary    : Rs. " + String.format(Locale.US, "%.2f", emp.getBaseSalary()) + "\n" +
                        "Payroll Status : " + status);
                }
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid Employee ID.");
            } catch (Exception e) {
                sendText(exchange, e.getMessage());
            }
        });

        server.createContext("/api/update", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String idStr = params.getOrDefault("id", "").trim();
                String salStr = params.getOrDefault("salary", "").trim();
                if (idStr.isEmpty() || salStr.isEmpty()) { sendText(exchange, "Required fields cannot be empty."); return; }
                int id = Integer.parseInt(idStr);
                double salary = Double.parseDouble(salStr);
                Employee before = service.searchEmployee(id);
                if (before == null) {
                    sendText(exchange, "Employee ID " + id + " not found.");
                    return;
                }
                double oldSal = before.getBaseSalary();
                boolean ok = service.updateEmployeeSalary(id, salary);
                sendText(exchange, ok
                    ? "SUCCESS: Employee " + id + " (" + before.getName() + ") salary updated from Rs. " + String.format(Locale.US, "%.2f", oldSal) + " to Rs. " + String.format(Locale.US, "%.2f", salary)
                    : "Employee not found.");
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid numeric input.");
            } catch (Exception e) {
                sendText(exchange, e.getMessage());
            }
        });

        server.createContext("/api/delete", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String idStr = params.getOrDefault("id", "").trim();
                if (idStr.isEmpty()) { sendText(exchange, "Required fields cannot be empty."); return; }
                int id = Integer.parseInt(idStr);
                service.deleteEmployee(id);
                sendText(exchange, "SUCCESS: Employee " + id + " deleted permanently from database.");
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid Employee ID.");
            } catch (Exception e) {
                sendText(exchange, e.getMessage());
            }
        });

        server.createContext("/api/analyze", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            try {
                String idStr = params.getOrDefault("id", "").trim();
                String bonusStr = params.getOrDefault("bonus", "").trim();
                if (idStr.isEmpty() || bonusStr.isEmpty()) {
                    sendText(exchange, "Required fields cannot be empty.");
                    return;
                }
                int id = Integer.parseInt(idStr);
                double bonus = Double.parseDouble(bonusStr);
                PayrollAnalysis analysis = service.analyzeEmployeePayroll(id, bonus);
                // Link payroll record dynamically so any analyzed employee gets deletion protection!
                dao.linkPayrollRecord(id);
                sendText(exchange, analysis.getFormattedReport());
            } catch (NumberFormatException e) {
                sendText(exchange, "Required fields cannot be empty.");
            } catch (Exception e) {
                sendText(exchange, e.getMessage());
            }
        });

        server.createContext("/api/reset", exchange -> {
            EmployeeDAO.resetSeedData();
            sendText(exchange, "Database reset to initial demonstration state.");
        });

        server.setExecutor(null);
        server.start();
        System.out.println("=========================================================");
        System.out.println("PREMIUM UI Server started on http://localhost:8080");
        System.out.println("=========================================================");
    }

    private static void handleStatic(HttpExchange exchange, String fileName) throws IOException {
        File file = new File("web/" + fileName);
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static void sendText(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }
}