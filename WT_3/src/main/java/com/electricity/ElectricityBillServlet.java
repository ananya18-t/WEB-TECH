package com.electricity;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;

@WebServlet("/ElectricityBillServlet")
public class ElectricityBillServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Parameter Extraction
        String unitsStr = request.getParameter("units");
        String customerName = request.getParameter("customerName");
        String consumerId = request.getParameter("consumerId");
        String category = request.getParameter("category");
        String billingMonth = request.getParameter("billingMonth");
        String fixedChargeParam = request.getParameter("includeFixedCharge");
        String taxParam = request.getParameter("taxPercent");
        String responseFormat = request.getParameter("format"); // "json" or "html"

        double units = 0.0;
        String errorMessage = null;

        if (unitsStr == null || unitsStr.trim().isEmpty()) {
            errorMessage = "Please provide the number of electricity units consumed.";
        } else {
            try {
                units = Double.parseDouble(unitsStr.trim());
                if (units < 0) {
                    errorMessage = "Units consumed cannot be negative.";
                }
            } catch (NumberFormatException e) {
                errorMessage = "Invalid units entered. Please enter a valid numerical value.";
            }
        }

        boolean includeFixedCharge = "true".equalsIgnoreCase(fixedChargeParam) || "on".equalsIgnoreCase(fixedChargeParam) || "1".equals(fixedChargeParam);
        double taxPercent = 0.0;
        if (taxParam != null && !taxParam.trim().isEmpty()) {
            try {
                taxPercent = Double.parseDouble(taxParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Handle JSON response request (for AJAX frontend)
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = "json".equalsIgnoreCase(responseFormat) || 
                                (acceptHeader != null && acceptHeader.contains("application/json")) ||
                                "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        if (errorMessage != null) {
            if (isJsonRequest) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                PrintWriter out = response.getWriter();
                out.print("{\"status\":\"error\", \"message\":\"" + errorMessage + "\"}");
                return;
            } else {
                renderErrorHtml(response, errorMessage);
                return;
            }
        }

        // Perform calculation
        BillCalculator.CalculationResult result = BillCalculator.calculateBill(
            customerName, consumerId, category, billingMonth, units, includeFixedCharge, taxPercent
        );

        if (isJsonRequest) {
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            Gson gson = new Gson();
            out.print(gson.toJson(result));
        } else {
            renderResultHtml(response, result);
        }
    }

    private void renderErrorHtml(HttpServletResponse response, String errorMessage) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html lang='en'>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Electricity Bill Calculation Error</title>");
        out.println("<style>");
        out.println("body { font-family: 'Segoe UI', system-ui, sans-serif; background: #0f172a; color: #f8fafc; padding: 40px 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }");
        out.println(".error-card { background: rgba(30, 41, 59, 0.8); border: 1px solid #ef4444; padding: 30px; border-radius: 16px; max-width: 480px; width: 100%; text-align: center; box-shadow: 0 20px 25px -5px rgba(239, 68, 68, 0.2); }");
        out.println("h2 { color: #f87171; margin-top: 0; }");
        out.println("p { color: #cbd5e1; margin-bottom: 24px; }");
        out.println("a { display: inline-block; background: #3b82f6; color: white; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: 600; transition: all 0.2s; }");
        out.println("a:hover { background: #2563eb; transform: translateY(-2px); }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='error-card'>");
        out.println("<h2>Calculation Error</h2>");
        out.println("<p>" + errorMessage + "</p>");
        out.println("<a href='index.html'>&larr; Back to Calculator</a>");
        out.println("</div>");
        out.println("</body></html>");
    }

    private void renderResultHtml(HttpServletResponse response, BillCalculator.CalculationResult result) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html lang='en'>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Electricity Bill Invoice - " + result.getBillNo() + "</title>");
        out.println("<link href='https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap' rel='stylesheet'>");
        out.println("<style>");
        out.println("body { font-family: 'Outfit', sans-serif; background: #0b1329; color: #f1f5f9; padding: 30px 15px; margin: 0; display: flex; flex-direction: column; align-items: center; }");
        out.println(".invoice-box { background: #1e293b; border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; max-width: 650px; width: 100%; padding: 35px; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5); }");
        out.println(".header { display: flex; justify-content: space-between; border-bottom: 2px solid #334155; padding-bottom: 20px; margin-bottom: 25px; }");
        out.println(".logo { font-size: 24px; font-weight: 700; color: #38bdf8; display: flex; align-items: center; gap: 8px; }");
        out.println(".bill-meta { text-align: right; font-size: 14px; color: #94a3b8; }");
        out.println(".bill-meta strong { color: #f8fafc; display: block; font-size: 16px; }");
        out.println(".customer-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; background: rgba(15, 23, 42, 0.6); padding: 18px; border-radius: 12px; margin-bottom: 25px; }");
        out.println(".info-item label { font-size: 12px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; display: block; }");
        out.println(".info-item span { font-weight: 600; font-size: 15px; color: #e2e8f0; }");
        out.println("table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }");
        out.println("th { background: #0f172a; text-align: left; padding: 12px; color: #38bdf8; font-size: 13px; text-transform: uppercase; border-bottom: 1px solid #334155; }");
        out.println("td { padding: 12px; border-bottom: 1px solid #334155; font-size: 14px; color: #cbd5e1; }");
        out.println(".summary { background: rgba(56, 189, 248, 0.05); border: 1px solid rgba(56, 189, 248, 0.2); border-radius: 14px; padding: 20px; margin-bottom: 25px; }");
        out.println(".summary-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; color: #cbd5e1; }");
        out.println(".summary-row.total { border-top: 1px solid rgba(255,255,255,0.1); padding-top: 12px; font-size: 20px; font-weight: 700; color: #4ade80; margin-bottom: 0; }");
        out.println(".actions { display: flex; gap: 12px; justify-content: center; }");
        out.println("button, a.btn { background: #38bdf8; color: #0f172a; border: none; padding: 12px 24px; border-radius: 10px; font-weight: 700; text-decoration: none; cursor: pointer; display: inline-flex; align-items: center; gap: 8px; transition: all 0.2s; }");
        out.println("button:hover, a.btn:hover { background: #0284c7; color: white; transform: translateY(-2px); }");
        out.println(".btn-secondary { background: #334155; color: #f8fafc; }");
        out.println(".btn-secondary:hover { background: #475569; }");
        out.println("@media print { .actions { display: none; } body { background: white; color: black; } .invoice-box { border: none; box-shadow: none; padding: 0; background: white; color: black; } th { background: #f1f5f9; color: black; } td { color: black; } }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='invoice-box'>");
        out.println("<div class='header'>");
        out.println("<div class='logo'>⚡ ElectroBill Servlet</div>");
        out.println("<div class='bill-meta'><strong>Invoice #" + result.getBillNo() + "</strong><span>Date: " + result.getBillDate() + "</span></div>");
        out.println("</div>");

        out.println("<div class='customer-grid'>");
        out.println("<div class='info-item'><label>Consumer Name</label><span>" + result.getCustomerName() + "</span></div>");
        out.println("<div class='info-item'><label>Consumer ID</label><span>" + result.getConsumerId() + "</span></div>");
        out.println("<div class='info-item'><label>Tariff Category</label><span>" + result.getCategory() + "</span></div>");
        out.println("<div class='info-item'><label>Units Consumed</label><span>" + String.format("%.2f", result.getTotalUnits()) + " kWh</span></div>");
        out.println("</div>");

        out.println("<h3>Slab Rate Calculation Breakdown</h3>");
        out.println("<table>");
        out.println("<thead><tr><th>Slab Description</th><th>Range</th><th>Units</th><th>Rate (Rs.)</th><th>Amount (Rs.)</th></tr></thead>");
        out.println("<tbody>");

        for (BillCalculator.SlabDetails slab : result.getSlabBreakdown()) {
            out.println("<tr>");
            out.println("<td>" + slab.getSlabName() + "</td>");
            out.println("<td>" + slab.getRange() + "</td>");
            out.println("<td>" + String.format("%.2f", slab.getUnits()) + "</td>");
            out.println("<td>Rs. " + String.format("%.2f", slab.getRate()) + "</td>");
            out.println("<td>Rs. " + String.format("%.2f", slab.getAmount()) + "</td>");
            out.println("</tr>");
        }

        out.println("</tbody></table>");

        out.println("<div class='summary'>");
        out.println("<div class='summary-row'><span>Energy Charge Subtotal:</span><span>Rs. " + String.format("%.2f", result.getBaseAmount()) + "</span></div>");
        if (result.getFixedCharge() > 0) {
            out.println("<div class='summary-row'><span>Fixed Meter Charge:</span><span>Rs. " + String.format("%.2f", result.getFixedCharge()) + "</span></div>");
        }
        if (result.getTaxAmount() > 0) {
            out.println("<div class='summary-row'><span>Electricity Tax (" + result.getTaxPercentage() + "%):</span><span>Rs. " + String.format("%.2f", result.getTaxAmount()) + "</span></div>");
        }
        out.println("<div class='summary-row total'><span>Net Payable Amount:</span><span>Rs. " + String.format("%.2f", result.getNetPayable()) + "</span></div>");
        out.println("</div>");

        out.println("<div class='actions'>");
        out.println("<button onclick='window.print()'>🖨️ Print Invoice</button>");
        out.println("<a href='index.html' class='btn btn-secondary'>&larr; Calculate Another</a>");
        out.println("</div>");

        out.println("</div>");
        out.println("</body></html>");
    }
}
