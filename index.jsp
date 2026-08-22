<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // Request parameter extraction
    String customerName = request.getParameter("customerName");
    String meterNo = request.getParameter("meterNo");
    String connType = request.getParameter("connType"); // "Residential" or "Commercial"
    String month = request.getParameter("month");
    String inputType = request.getParameter("inputType"); // "units" or "readings"
    
    // Check if digital pay discount is enabled
    boolean hasDigitalDiscount = request.getParameter("digitalDiscount") != null;

    double units = 0;
    double prevReading = 0;
    double currReading = 0;
    boolean calculated = false;
    String error = null;

    // Check if form is submitted
    if (customerName != null) {
        customerName = customerName.trim();
        meterNo = meterNo.trim();
        
        try {
            if ("readings".equals(inputType)) {
                String prevStr = request.getParameter("prevReading");
                String currStr = request.getParameter("currReading");
                if (prevStr == null || currStr == null || prevStr.isEmpty() || currStr.isEmpty()) {
                    error = "Please provide both previous and current meter readings.";
                } else {
                    prevReading = Double.parseDouble(prevStr);
                    currReading = Double.parseDouble(currStr);
                    if (prevReading > currReading) {
                        error = "Current reading cannot be less than previous reading.";
                    } else if (0 > currReading || 0 > prevReading) {
                        error = "Meter readings cannot be negative.";
                    } else {
                        units = currReading - prevReading;
                        calculated = true;
                    }
                }
            } else {
                String unitsStr = request.getParameter("units");
                if (unitsStr == null || unitsStr.isEmpty()) {
                    error = "Please provide the total units consumed.";
                } else {
                    units = Double.parseDouble(unitsStr);
                    if (0 > units) {
                        error = "Units consumed cannot be negative.";
                    } else {
                        calculated = true;
                    }
                }
            }
        } catch (NumberFormatException e) {
            error = "Invalid inputs. Please check that readings or units are numeric.";
        }
    }

    // Bill Calculations variables
    double slab1Units = 0, slab2Units = 0, slab3Units = 0, slab4Units = 0;
    double slab1Cost = 0, slab2Cost = 0, slab3Cost = 0, slab4Cost = 0;
    double energyCharge = 0;
    double fixedCharge = 0;
    double taxAmount = 0;
    double discountAmount = 0;
    double netBillAmount = 0;

    // Output formatted strings to keep JSP tags in HTML very clean
    String unitsStr = "0.00";
    String grossAmountStr = "0.00";
    String netBillAmountStr = "0.00";
    String energyChargeStr = "0.00";
    String fixedChargeStr = "0.00";
    String taxAmountStr = "0.00";
    String discountAmountStr = "0.00";

    String slab1UnitsStr = "0.00";
    String slab2UnitsStr = "0.00";
    String slab3UnitsStr = "0.00";
    String slab4UnitsStr = "0.00";

    String slab1CostStr = "0.00";
    String slab2CostStr = "0.00";
    String slab3CostStr = "0.00";
    String slab4CostStr = "0.00";

    if (calculated && error == null) {
        double tempUnits = units;

        // Slab 1: First 50 units @ Rs. 3.50/unit
        if (tempUnits > 50) {
            slab1Units = 50;
            tempUnits -= 50;
        } else {
            slab1Units = tempUnits;
            tempUnits = 0;
        }
        slab1Cost = slab1Units * 3.50;

        // Slab 2: Next 100 units @ Rs. 4.00/unit
        if (tempUnits > 100) {
            slab2Units = 100;
            tempUnits -= 100;
        } else {
            slab2Units = tempUnits;
            tempUnits = 0;
        }
        slab2Cost = slab2Units * 4.00;

        // Slab 3: Next 100 units @ Rs. 5.20/unit
        if (tempUnits > 100) {
            slab3Units = 100;
            tempUnits -= 100;
        } else {
            slab3Units = tempUnits;
            tempUnits = 0;
        }
        slab3Cost = slab3Units * 5.20;

        // Slab 4: Above 250 units @ Rs. 6.50/unit
        if (tempUnits > 0) {
            slab4Units = tempUnits;
        }
        slab4Cost = slab4Units * 6.50;

        energyCharge = slab1Cost + slab2Cost + slab3Cost + slab4Cost;

        // Fixed charges based on connection type
        if ("Commercial".equalsIgnoreCase(connType)) {
            fixedCharge = 150.00;
        } else {
            fixedCharge = 50.00; // Residential
        }

        // Tax / Duty: 8% of energy charges
        taxAmount = energyCharge * 0.08;

        // Total before discount
        double grossTotal = energyCharge + fixedCharge + taxAmount;

        // Digital Payment Discount: 1.5% off total
        if (hasDigitalDiscount) {
            discountAmount = grossTotal * 0.015;
        }

        netBillAmount = grossTotal - discountAmount;

        // Format variables for rendering
        unitsStr = String.format("%.2f", units);
        grossAmountStr = String.format("%.2f", grossTotal);
        netBillAmountStr = String.format("%.2f", netBillAmount);
        energyChargeStr = String.format("%.2f", energyCharge);
        fixedChargeStr = String.format("%.2f", fixedCharge);
        taxAmountStr = String.format("%.2f", taxAmount);
        discountAmountStr = String.format("%.2f", discountAmount);

        slab1UnitsStr = String.format("%.2f", slab1Units);
        slab2UnitsStr = String.format("%.2f", slab2Units);
        slab3UnitsStr = String.format("%.2f", slab3Units);
        slab4UnitsStr = String.format("%.2f", slab4Units);

        slab1CostStr = String.format("%.2f", slab1Cost);
        slab2CostStr = String.format("%.2f", slab2Cost);
        slab3CostStr = String.format("%.2f", slab3Cost);
        slab4CostStr = String.format("%.2f", slab4Cost);
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Calculate your monthly electricity bill instantly with slab breakdowns, charts, and digital invoices. Responsive UI built with JSP.">
    <title>Electricity Bill Calculator | Smart Web App</title>
    
    <!-- External Fonts & Icons -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <!-- Beautiful Styles -->
    <link rel="stylesheet" href="css/styles.css">
</head>
<body>

    <!-- Header Section -->
    <header>
        <div class="nav-container">
            <div class="logo-section">
                <div class="logo-icon">
                    <i class="fa-solid fa-bolt"></i>
                </div>
                <div class="logo-text">
                    <h1>ELECTRO-BILL</h1>
                    <span>Smart Electricity Billing Portal</span>
                </div>
            </div>
            
            <div class="header-actions">
                <button class="theme-toggle" id="theme-toggle-btn" title="Toggle Theme" aria-label="Toggle Theme">
                    <i class="fas fa-sun"></i>
                </button>
            </div>
        </div>
    </header>

    <!-- Main Layout Container -->
    <main class="main-layout">
        
        <!-- Sidebar Input Form -->
        <aside class="sidebar-wrapper" style="display: flex; flex-direction: column; gap: 20px;">
            <div class="glass-card sidebar">
                <h2>Bill Details</h2>
                
                <% if (error != null) { %>
                    <div class="alert-error">
                        <i class="fas fa-triangle-exclamation"></i>
                        <span><%= error %></span>
                    </div>
                <% } %>
                
                <form action="index.jsp" method="POST" id="bill-form" data-conn="<%= (connType != null) ? connType : "Residential" %>" data-month="<%= (month != null) ? month : "August" %>" data-discount="<%= hasDigitalDiscount %>">
                    <!-- Client Selection for Input Mode -->
                    <input type="hidden" name="inputType" id="inputType" value="<%= (inputType != null) ? inputType : "units" %>">
                    
                    <div class="toggle-tab-container">
                        <div class="toggle-tab active" id="tab-units">
                            <i class="fas fa-list-numeric"></i> Enter Units
                        </div>
                        <div class="toggle-tab" id="tab-readings">
                            <i class="fas fa-gauge-high"></i> Meter Readings
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="customerName"><i class="fas fa-user-circle"></i> Customer Name</label>
                        <input type="text" id="customerName" name="customerName" class="form-control" placeholder="e.g. John Doe" required value="<%= (customerName != null) ? customerName : "" %>">
                    </div>

                    <div class="form-group">
                        <label for="meterNo"><i class="fas fa-server"></i> Meter Number</label>
                        <input type="text" id="meterNo" name="meterNo" class="form-control" placeholder="e.g. MTR-98271" required value="<%= (meterNo != null) ? meterNo : "" %>">
                    </div>

                    <div class="form-row" style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                        <div class="form-group">
                            <label for="connType"><i class="fas fa-building"></i> Connection</label>
                            <select id="connType" name="connType" class="form-select">
                                <option value="Residential">Residential</option>
                                <option value="Commercial">Commercial</option>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="month"><i class="fas fa-calendar-alt"></i> Billing Month</label>
                            <select id="month" name="month" class="form-select">
                                <option value="January">January</option>
                                <option value="February">February</option>
                                <option value="March">March</option>
                                <option value="April">April</option>
                                <option value="May">May</option>
                                <option value="June">June</option>
                                <option value="July">July</option>
                                <option value="August">August</option>
                                <option value="September">September</option>
                                <option value="October">October</option>
                                <option value="November">November</option>
                                <option value="December">December</option>
                            </select>
                        </div>
                    </div>

                    <!-- Direct Units Section -->
                    <div id="group-units" class="form-group visible-section">
                        <label for="units"><i class="fas fa-lightbulb"></i> Total Units Consumed</label>
                        <input type="number" step="0.01" id="units" name="units" class="form-control" placeholder="e.g. 185" value="<%= (units > 0 && "units".equals(inputType)) ? units : "" %>">
                    </div>

                    <!-- Readings Section -->
                    <div id="group-readings" class="form-group hidden-section">
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                            <div>
                                <label for="prevReading">Previous Reading</label>
                                <input type="number" step="0.01" id="prevReading" name="prevReading" class="form-control" placeholder="e.g. 12000" value="<%= (prevReading > 0) ? prevReading : "" %>">
                            </div>
                            <div>
                                <label for="currReading">Current Reading</label>
                                <input type="number" step="0.01" id="currReading" name="currReading" class="form-control" placeholder="e.g. 12185" value="<%= (currReading > 0) ? currReading : "" %>">
                            </div>
                        </div>
                    </div>

                    <div class="checkbox-group">
                        <input type="checkbox" id="digitalDiscount" name="digitalDiscount">
                        <label for="digitalDiscount" style="margin-bottom: 0; cursor: pointer;">
                            <i class="fas fa-mobile-screen-button"></i> Apply Digital Pay Discount (1.5%)
                        </label>
                    </div>

                    <button type="submit" class="btn-calc">
                        <i class="fas fa-calculator"></i> Calculate Bill
                    </button>
                </form>
            </div>
            
            <!-- Tariff Rates Cards -->
            <div class="glass-card" style="margin-top: 1.5rem;">
                <h3 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 1rem; color: var(--primary);">
                    <i class="fas fa-circle-info"></i> Tariff Rate Directory
                </h3>
                <div style="display: flex; flex-direction: column; gap: 0.6rem; font-size: 0.9rem;">
                    <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--glass-border-dark); padding-bottom: 0.4rem;">
                        <span>First 50 Units</span>
                        <span style="font-weight: 700; color: var(--slab-1);">₹3.50 / unit</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--glass-border-dark); padding-bottom: 0.4rem;">
                        <span>Next 100 Units (51-150)</span>
                        <span style="font-weight: 700; color: var(--slab-2);">₹4.00 / unit</span>
                    </div>
                    <div style="display: flex; justify-content: space-between; border-bottom: 1px solid var(--glass-border-dark); padding-bottom: 0.4rem;">
                        <span>Next 100 Units (151-250)</span>
                        <span style="font-weight: 700; color: var(--slab-3);">₹5.20 / unit</span>
                    </div>
                    <div style="display: flex; justify-content: space-between;">
                        <span>Above 250 Units</span>
                        <span style="font-weight: 700; color: var(--slab-4);">₹6.50 / unit</span>
                    </div>
                </div>
            </div>
        </aside>

        <!-- Output Dashboard Grid -->
        <article class="results-dashboard">
            <% if (!calculated || error != null) { %>
                <!-- Empty State -->
                <div class="glass-card empty-state">
                    <div class="empty-state-icon">
                        <i class="fas fa-receipt"></i>
                    </div>
                    <h3>Ready to Calculate</h3>
                    <p>Enter the customer name, meter details and units consumed in the sidebar to generate a gorgeous detailed breakdown of your electricity charges.</p>
                </div>
            <% } else { %>
                <!-- Computed Results Panel -->
                
                <!-- Top Summary Cards -->
                <div class="top-summary-grid">
                    <div class="summary-card glass-card">
                        <div class="card-icon-container icon-blue">
                            <i class="fas fa-bolt"></i>
                        </div>
                        <div class="summary-card-info">
                            <p>Units Consumed</p>
                            <h3><%= unitsStr %> <span style="font-size: 0.9rem; font-weight: 500;">kWh</span></h3>
                        </div>
                    </div>
                    
                    <div class="summary-card glass-card">
                        <div class="card-icon-container icon-green">
                            <i class="fas fa-indian-rupee-sign"></i>
                        </div>
                        <div class="summary-card-info">
                            <p>Gross Amount</p>
                            <h3>₹<%= grossAmountStr %></h3>
                        </div>
                    </div>
                    
                    <div class="summary-card glass-card">
                        <div class="card-icon-container icon-purple">
                            <i class="fas fa-chart-line"></i>
                        </div>
                        <div class="summary-card-info">
                            <p>Net Bill Amount</p>
                            <h3>₹<%= netBillAmountStr %></h3>
                        </div>
                    </div>
                </div>

                <!-- Visual progress bar showing slab usage proportions -->
                <div class="glass-card">
                    <div class="section-title">
                        <i class="fas fa-chart-bar" style="color: var(--primary);"></i> Consumption Slab Distribution
                    </div>
                    
                    <%
                        double maxScaleUnits = Math.max(units, 300); // Scale bar to at least 300 units representation
                        double p1 = (slab1Units / maxScaleUnits) * 100;
                        double p2 = (slab2Units / maxScaleUnits) * 100;
                        double p3 = (slab3Units / maxScaleUnits) * 100;
                        double p4 = (slab4Units / maxScaleUnits) * 100;
                        double pEmpty = 100 - (p1 + p2 + p3 + p4);
                    %>
                    <div class="slab-progress-container">
                        <div class="slab-progress-bar">
                            <% if (slab1Units > 0) { %>
                                <div class="slab-segment slab-segment-1" style="width: <%= p1 %>%;" title="Slab 1 (0-50): <%= slab1UnitsStr %> Units">
                                    <%= (int)slab1Units %>
                                </div>
                            <% } %>
                            <% if (slab2Units > 0) { %>
                                <div class="slab-segment slab-segment-2" style="width: <%= p2 %>%;" title="Slab 2 (51-150): <%= slab2UnitsStr %> Units">
                                    <%= (int)slab2Units %>
                                </div>
                            <% } %>
                            <% if (slab3Units > 0) { %>
                                <div class="slab-segment slab-segment-3" style="width: <%= p3 %>%;" title="Slab 3 (151-250): <%= slab3UnitsStr %> Units">
                                    <%= (int)slab3Units %>
                                </div>
                            <% } %>
                            <% if (slab4Units > 0) { %>
                                <div class="slab-segment slab-segment-4" style="width: <%= p4 %>%;" title="Slab 4 (>250): <%= slab4UnitsStr %> Units">
                                    <%= (int)slab4Units %>
                                </div>
                            <% } %>
                        </div>
                        
                        <div class="slab-legend">
                            <div class="legend-item">
                                <div class="legend-color slab-segment-1"></div>
                                <span>Slab 1 (0-50 units)</span>
                            </div>
                            <div class="legend-item">
                                <div class="legend-color slab-segment-2"></div>
                                <span>Slab 2 (51-150 units)</span>
                            </div>
                            <div class="legend-item">
                                <div class="legend-color slab-segment-3"></div>
                                <span>Slab 3 (151-250 units)</span>
                            </div>
                            <div class="legend-item">
                                <div class="legend-color slab-segment-4"></div>
                                <span>Slab 4 (&gt;250 units)</span>
                            </div>
                        </div>
                    </div>

                    <!-- Breakdown Table -->
                    <div class="table-responsive">
                        <table class="slab-table">
                            <thead>
                                <tr>
                                    <th>Consumption Slab</th>
                                    <th>Unit Rate</th>
                                    <th>Consumed in Slab</th>
                                    <th style="text-align: right;">Calculated Cost</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td><span class="slab-badge slab-segment-1">Slab 1 (0 - 50)</span></td>
                                    <td>₹3.50 / unit</td>
                                    <td><%= slab1UnitsStr %> units</td>
                                    <td style="text-align: right; font-weight: 700;">₹<%= slab1CostStr %></td>
                                </tr>
                                <tr>
                                    <td><span class="slab-badge slab-segment-2">Slab 2 (51 - 150)</span></td>
                                    <td>₹4.00 / unit</td>
                                    <td><%= slab2UnitsStr %> units</td>
                                    <td style="text-align: right; font-weight: 700;">₹<%= slab2CostStr %></td>
                                </tr>
                                <tr>
                                    <td><span class="slab-badge slab-segment-3">Slab 3 (151 - 250)</span></td>
                                    <td>₹5.20 / unit</td>
                                    <td><%= slab3UnitsStr %> units</td>
                                    <td style="text-align: right; font-weight: 700;">₹<%= slab3CostStr %></td>
                                </tr>
                                <tr>
                                    <td><span class="slab-badge slab-segment-4">Slab 4 (&gt; 250)</span></td>
                                    <td>₹6.50 / unit</td>
                                    <td><%= slab4UnitsStr %> units</td>
                                    <td style="text-align: right; font-weight: 700;">₹<%= slab4CostStr %></td>
                                </tr>
                                <tr style="border-top: 2px solid var(--glass-border-dark);">
                                    <td colspan="2" style="font-weight: 700;">Net Energy Charges:</td>
                                    <td><%= unitsStr %> units</td>
                                    <td style="text-align: right; font-weight: 800; color: var(--primary);">₹<%= energyChargeStr %></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Printable Receipt -->
                <div class="glass-card bill-receipt-wrapper">
                    <div class="receipt-header">
                        <h4>MUNICIPAL ELECTRICITY BOARD</h4>
                        <p>OFFICIAL BILL TRANSACTION RECEIPT</p>
                        <span style="font-size: 0.75rem; letter-spacing: 1px; color: var(--text-secondary-dark);">
                            DATE GENERATED: <%= new java.util.Date().toString() %>
                        </span>
                    </div>

                    <div class="receipt-details-list">
                        <div class="receipt-detail-item">
                            <p>Customer Name</p>
                            <p><%= customerName %></p>
                        </div>
                        <div class="receipt-detail-item">
                            <p>Meter Number</p>
                            <p><%= meterNo %></p>
                        </div>
                        <div class="receipt-detail-item">
                            <p>Connection Category</p>
                            <p><%= connType %></p>
                        </div>
                        <div class="receipt-detail-item">
                            <p>Billing Cycle</p>
                            <p><%= month %> 2026</p>
                        </div>
                    </div>

                    <div class="receipt-summary-lines">
                        <div class="summary-line">
                            <span>Energy Consumption Charges</span>
                            <span>₹<%= energyChargeStr %></span>
                        </div>
                        <div class="summary-line">
                            <span>Fixed Connection Fee (<%= connType %>)</span>
                            <span>₹<%= fixedChargeStr %></span>
                        </div>
                        <div class="summary-line">
                            <span>Government Electricity Duty Tax (8%)</span>
                            <span>₹<%= taxAmountStr %></span>
                        </div>
                        <% if (hasDigitalDiscount) { %>
                            <div class="summary-line" style="color: var(--success); font-weight: 600;">
                                <span>Digital Pay Incentive (1.5%) - Discount</span>
                                <span>- ₹<%= discountAmountStr %></span>
                            </div>
                        <% } %>
                        <div class="summary-line total">
                            <span>Total Payable Amount</span>
                            <span>₹<%= netBillAmountStr %></span>
                        </div>
                    </div>

                    <div class="receipt-actions">
                        <button class="btn-secondary" onclick="printBill()">
                            <i class="fas fa-print"></i> Print Invoice
                        </button>
                    </div>
                </div>

                <!-- Bridge data for JavaScript storage sync -->
                <div id="bill-bridge" 
                     data-customer="<%= customerName != null ? customerName.replaceAll("\"", "&quot;") : "" %>" 
                     data-meter="<%= meterNo != null ? meterNo.replaceAll("\"", "&quot;") : "" %>" 
                     data-month="<%= month %>" 
                     data-units="<%= units %>" 
                     data-amount="<%= netBillAmount %>" 
                     style="display: none;"></div>
            <% } %>
        </article>

    </main>

    <!-- Calculation History Section -->
    <section class="history-section" id="history-section" style="display: none;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <h2 class="section-title" style="margin-bottom:0;"><i class="fas fa-history" style="color: var(--primary);"></i> Recent Calculations</h2>
            <button class="btn-clear-history" onclick="clearHistory()"><i class="fas fa-trash-can"></i> Clear All</button>
        </div>
        <div class="history-grid" id="history-grid"></div>
    </section>

    <!-- Interactive conservation advice -->
    <section style="max-width: 1400px; width: 100%; margin: 2rem auto; padding: 0 2rem;">
        <div class="glass-card">
            <h2 class="section-title"><i class="fas fa-leaf" style="color: var(--success);"></i> Energy Saving Expert Tips</h2>
            <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-top:1rem;">
                <div style="background:rgba(255,255,255,0.02); padding:1.25rem; border-radius:16px; border: 1px solid var(--glass-border-dark);">
                    <h4 style="font-weight:700; margin-bottom:0.5rem; display:flex; align-items:center; gap:0.5rem; color: #fbbf24;"><i class="fas fa-temperature-empty"></i> Smart AC Settings</h4>
                    <p style="font-size:0.875rem; color: var(--text-secondary-dark); line-height: 1.4;">Keep your air conditioner thermostat set at 24°C-26°C. Every 1°C increase blocks up to 6% of power losses.</p>
                </div>
                <div style="background:rgba(255,255,255,0.02); padding:1.25rem; border-radius:16px; border: 1px solid var(--glass-border-dark);">
                    <h4 style="font-weight:700; margin-bottom:0.5rem; display:flex; align-items:center; gap:0.5rem; color: #a78bfa;"><i class="fas fa-plug"></i> Kill Phantom Load</h4>
                    <p style="font-size:0.875rem; color: var(--text-secondary-dark); line-height: 1.4;">Unplug laptop chargers, micro-waves and smart TVs from the main outlet. Devices on standby draw 5-10% of idle household juice.</p>
                </div>
                <div style="background:rgba(255,255,255,0.02); padding:1.25rem; border-radius:16px; border: 1px solid var(--glass-border-dark);">
                    <h4 style="font-weight:700; margin-bottom:0.5rem; display:flex; align-items:center; gap:0.5rem; color: #60a5fa;"><i class="fas fa-lightbulb"></i> Pivot to LEDs</h4>
                    <p style="font-size:0.875rem; color: var(--text-secondary-dark); line-height: 1.4;">Exchange standard lighting with LED bulbs. LEDs use 75% less electric current and easily survive 25 times longer than typical incandescent globes.</p>
                </div>
            </div>
        </div>
    </section>

    <!-- Footer -->
    <footer>
        <p>&copy; 2026 Municipal Electricity Board. All Rights Reserved. Built with JSP & dynamic client assets.</p>
    </footer>

    <!-- App JavaScript -->
    <script src="js/app.js"></script>
</body>
</html>
