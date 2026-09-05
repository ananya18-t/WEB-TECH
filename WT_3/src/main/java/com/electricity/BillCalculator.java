package com.electricity;

import java.util.ArrayList;
import java.util.List;

public class BillCalculator {

    public static class SlabDetails {
        private String slabName;
        private String range;
        private double units;
        private double rate;
        private double amount;

        public SlabDetails(String slabName, String range, double units, double rate, double amount) {
            this.slabName = slabName;
            this.range = range;
            this.units = units;
            this.rate = rate;
            this.amount = amount;
        }

        public String getSlabName() { return slabName; }
        public String getRange() { return range; }
        public double getUnits() { return units; }
        public double getRate() { return rate; }
        public double getAmount() { return amount; }
    }

    public static class CalculationResult {
        private String customerName;
        private String consumerId;
        private String category;
        private String billingMonth;
        private double totalUnits;
        private List<SlabDetails> slabBreakdown;
        private double baseAmount;
        private double fixedCharge;
        private double taxPercentage;
        private double taxAmount;
        private double netPayable;
        private String billDate;
        private String dueDate;
        private String billNo;

        public CalculationResult() {
            this.slabBreakdown = new ArrayList<>();
        }

        // Getters and Setters
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getConsumerId() { return consumerId; }
        public void setConsumerId(String consumerId) { this.consumerId = consumerId; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getBillingMonth() { return billingMonth; }
        public void setBillingMonth(String billingMonth) { this.billingMonth = billingMonth; }

        public double getTotalUnits() { return totalUnits; }
        public void setTotalUnits(double totalUnits) { this.totalUnits = totalUnits; }

        public List<SlabDetails> getSlabBreakdown() { return slabBreakdown; }
        public void setSlabBreakdown(List<SlabDetails> slabBreakdown) { this.slabBreakdown = slabBreakdown; }

        public double getBaseAmount() { return baseAmount; }
        public void setBaseAmount(double baseAmount) { this.baseAmount = baseAmount; }

        public double getFixedCharge() { return fixedCharge; }
        public void setFixedCharge(double fixedCharge) { this.fixedCharge = fixedCharge; }

        public double getTaxPercentage() { return taxPercentage; }
        public void setTaxPercentage(double taxPercentage) { this.taxPercentage = taxPercentage; }

        public double getTaxAmount() { return taxAmount; }
        public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

        public double getNetPayable() { return netPayable; }
        public void setNetPayable(double netPayable) { this.netPayable = netPayable; }

        public String getBillDate() { return billDate; }
        public void setBillDate(String billDate) { this.billDate = billDate; }

        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }

        public String getBillNo() { return billNo; }
        public void setBillNo(String billNo) { this.billNo = billNo; }
    }

    /**
     * Calculates electricity bill based on specified slab conditions:
     * - First 50 units @ Rs. 3.50/unit
     * - Next 100 units (51-150) @ Rs. 4.00/unit
     * - Next 100 units (151-250) @ Rs. 5.20/unit
     * - Above 250 units (>250) @ Rs. 6.50/unit
     */
    public static CalculationResult calculateBill(String customerName, String consumerId, String category, 
                                                   String billingMonth, double units, boolean includeFixedCharge, double taxPercent) {
        CalculationResult result = new CalculationResult();
        result.setCustomerName(customerName == null || customerName.trim().isEmpty() ? "Valued Consumer" : customerName.trim());
        result.setConsumerId(consumerId == null || consumerId.trim().isEmpty() ? "ELE-" + (int)(Math.random() * 899999 + 100000) : consumerId.trim());
        result.setCategory(category == null || category.trim().isEmpty() ? "Residential" : category);
        result.setBillingMonth(billingMonth == null || billingMonth.trim().isEmpty() ? "Current Month" : billingMonth);
        result.setTotalUnits(units);

        List<SlabDetails> breakdown = new ArrayList<>();
        double remainingUnits = Math.max(0, units);
        double baseAmount = 0.0;

        // Slab 1: First 50 units @ Rs. 3.50/unit
        if (remainingUnits > 0) {
            double u1 = Math.min(remainingUnits, 50.0);
            double rate1 = 3.50;
            double amt1 = u1 * rate1;
            breakdown.add(new SlabDetails("Slab 1 (First 50 Units)", "1 - 50 Units", u1, rate1, amt1));
            baseAmount += amt1;
            remainingUnits -= u1;
        } else {
            breakdown.add(new SlabDetails("Slab 1 (First 50 Units)", "1 - 50 Units", 0, 3.50, 0.0));
        }

        // Slab 2: Next 100 units (51 to 150) @ Rs. 4.00/unit
        if (remainingUnits > 0) {
            double u2 = Math.min(remainingUnits, 100.0);
            double rate2 = 4.00;
            double amt2 = u2 * rate2;
            breakdown.add(new SlabDetails("Slab 2 (Next 100 Units)", "51 - 150 Units", u2, rate2, amt2));
            baseAmount += amt2;
            remainingUnits -= u2;
        } else {
            breakdown.add(new SlabDetails("Slab 2 (Next 100 Units)", "51 - 150 Units", 0, 4.00, 0.0));
        }

        // Slab 3: Next 100 units (151 to 250) @ Rs. 5.20/unit
        if (remainingUnits > 0) {
            double u3 = Math.min(remainingUnits, 100.0);
            double rate3 = 5.20;
            double amt3 = u3 * rate3;
            breakdown.add(new SlabDetails("Slab 3 (Next 100 Units)", "151 - 250 Units", u3, rate3, amt3));
            baseAmount += amt3;
            remainingUnits -= u3;
        } else {
            breakdown.add(new SlabDetails("Slab 3 (Next 100 Units)", "151 - 250 Units", 0, 5.20, 0.0));
        }

        // Slab 4: Above 250 units @ Rs. 6.50/unit
        if (remainingUnits > 0) {
            double u4 = remainingUnits;
            double rate4 = 6.50;
            double amt4 = u4 * rate4;
            breakdown.add(new SlabDetails("Slab 4 (Above 250 Units)", "> 250 Units", u4, rate4, amt4));
            baseAmount += amt4;
            remainingUnits = 0;
        } else {
            breakdown.add(new SlabDetails("Slab 4 (Above 250 Units)", "> 250 Units", 0, 6.50, 0.0));
        }

        result.setSlabBreakdown(breakdown);
        result.setBaseAmount(Math.round(baseAmount * 100.0) / 100.0);

        // Fixed Charge
        double fixedCharge = includeFixedCharge ? 50.00 : 0.00;
        result.setFixedCharge(fixedCharge);

        // Electricity Duty / Govt Tax
        double taxAmount = (baseAmount + fixedCharge) * (taxPercent / 100.0);
        taxAmount = Math.round(taxAmount * 100.0) / 100.0;
        result.setTaxPercentage(taxPercent);
        result.setTaxAmount(taxAmount);

        // Net Payable
        double netPayable = Math.round((baseAmount + fixedCharge + taxAmount) * 100.0) / 100.0;
        result.setNetPayable(netPayable);

        // Metadata
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        result.setBillDate(sdf.format(cal.getTime()));
        cal.add(java.util.Calendar.DAY_OF_MONTH, 15);
        result.setDueDate(sdf.format(cal.getTime()));
        result.setBillNo("INV-" + System.currentTimeMillis() % 10000000);

        return result;
    }
}
