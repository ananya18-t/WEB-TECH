# Electricity Bill Calculator - JSP Web Application

A responsive, feature-rich web application built using JSP, HTML5, CSS3, and JavaScript to calculate household and commercial electricity bills based on units consumed or meter readings.

## ⚡ Pricing Structure
The bill calculation is based on the following progressive slabs:
*   **First 50 Units**: ₹3.50 / unit
*   **Next 100 Units (51 - 150)**: ₹4.00 / unit
*   **Next 100 Units (151 - 250)**: ₹5.20 / unit
*   **Above 250 Units**: ₹6.50 / unit

## 🌟 Key Features
1.  **Dual Input Modes**: Input either direct **Total Units Consumed** or **Meter Readings** (Previous and Current).
2.  **Adaptive Category Fees**:
    *   *Residential Connection*: Adds a fixed fee of ₹50.00.
    *   *Commercial Connection*: Adds a fixed fee of ₹150.00.
3.  **Taxes & Discounts**:
    *   Government Duty Tax of 8% on energy consumption charges.
    *   1.5% Digital Payment Incentive Discount toggle.
4.  **Responsive Dashboard**:
    *   High-fidelity Glassmorphic dark/light dashboard themes.
    *   Interactive CSS-rendered progress gauge showing the breakdown of consumed units in each slab.
    *   Breakdown table showing exact costs per slab block.
5.  **Calculations History**: Locally synced using `localStorage` to view last 6 calculations.
6.  **Printable Invoices**: Built-in CSS `@media print` support to print/PDF the receipt directly.

## 📁 Project Structure
The project files are mapped inside the workspace:
*   `ebill/index.jsp` - Contains calculations, JSP tags, form elements, and results board.
*   `ebill/css/styles.css` - Contains styling tokens, layout grids, animations, and theme configurations.
*   `ebill/js/app.js` - Contains client logic, validation, theme persistence, and history sync.
*   `ebill/WEB-INF/web.xml` - Tomcat Deployment descriptor.

## 🚀 How to Run locally
1.  Tomcat 9 is preconfigured in the workspace root directory.
2.  Start the Tomcat Web server:
    *   Open terminal in `<workspace>\apache-tomcat-9.0.121`
    *   Run command: `bin\startup.bat` (or `bin\catalina.bat run` to view console logs directly).
3.  Open browser and navigate to:
    *   `http://localhost:8080/ebill/index.jsp`
