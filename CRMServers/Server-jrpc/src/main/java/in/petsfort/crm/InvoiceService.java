package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

final class InvoiceService {
    JsonObject generate(String orderId, Instant createdAt, String userId, JsonArray products,
                        JsonObject user, JsonObject otherData) {
        JsonObject invoice = new JsonObject();
        invoice.add("company", company());
        JsonObject details = new JsonObject();
        details.addProperty("invoiceNo", "INV-" + orderId);
        details.addProperty("dated", LocalDate.ofInstant(createdAt, ZoneOffset.UTC).toString());
        details.addProperty("deliveryNote", "DN-" + orderId);
        details.addProperty("refNoDate", "REF-" + userId);
        details.addProperty("otherRef", string(otherData, "notes", "N/A"));
        details.addProperty("checkedBy", "System Generated");
        invoice.add("details", details);

        JsonObject consignee = new JsonObject();
        consignee.addProperty("name", string(user, "name", "N/A"));
        consignee.addProperty("address", string(otherData, "address", "N/A"));
        invoice.add("consignee", consignee);
        JsonObject buyer = new JsonObject();
        buyer.addProperty("name", string(user, "name", "N/A"));
        buyer.addProperty("address", string(user, "address", "N/A"));
        buyer.addProperty("contactNo", string(user, "contact", "N/A"));
        buyer.addProperty("gstin", string(user, "gstin", "N/A"));
        invoice.add("buyer", buyer);

        JsonArray items = new JsonArray();
        Map<String, GstGroup> gstGroups = new LinkedHashMap<>();
        BigDecimal subtotal = Money.ZERO, cgstTotal = Money.ZERO, sgstTotal = Money.ZERO;
        int serial = 1;
        for (JsonElement element : products) {
            JsonObject product = element.getAsJsonObject();
            BigDecimal mrp = Money.of(product, "cost_mrp");
            BigDecimal rate = Money.of(product, "cost_rate");
            int shipped = integer(product, "count", 0);
            int paid = integer(product, "paid_count", shipped);
            int free = integer(product, "free_count", 0);
            BigDecimal discount = Money.of(product, "cost_dis");
            BigDecimal gst = Money.of(product, "cost_gst");
            BigDecimal halfRate = gst.divide(new BigDecimal("2"), 12, Money.ROUNDING);
            BigDecimal taxable = rate.multiply(BigDecimal.valueOf(paid));
            BigDecimal cgst = Money.percent(taxable, halfRate);
            BigDecimal sgst = Money.percent(taxable, halfRate);

            JsonObject item = new JsonObject();
            item.addProperty("sNo", serial++);
            item.addProperty("description", string(product, "product_name", "N/A") + (free > 0 ? " (" + free + " free)" : ""));
            item.addProperty("hsnSac", string(product, "product_hsn", "N/A"));
            item.addProperty("partNo", string(product, "product_cid", "N/A"));
            item.addProperty("quantityShipped", shipped + " No"); item.addProperty("quantityBilled", paid + " No");
            item.addProperty("mrp", Money.output(mrp)); item.addProperty("discount", plain(discount) + " %");
            item.addProperty("rate", Money.output(rate)); item.addProperty("amount", Money.output(taxable));
            items.add(item);

            String groupKey = string(product, "product_cid", "N/A");
            gstGroups.computeIfAbsent(groupKey, ignored -> new GstGroup(halfRate)).add(taxable, cgst, sgst);
            subtotal = subtotal.add(taxable); cgstTotal = cgstTotal.add(cgst); sgstTotal = sgstTotal.add(sgst);
        }
        invoice.add("items", items);
        JsonArray gstDetails = new JsonArray();
        gstGroups.forEach((hsn, group) -> gstDetails.add(group.json(hsn)));
        invoice.add("gstDetails", gstDetails);

        BigDecimal total = subtotal.add(cgstTotal).add(sgstTotal);
        JsonObject totals = new JsonObject();
        totals.addProperty("subTotal", Money.output(subtotal)); totals.addProperty("cgstAmount", Money.output(cgstTotal));
        totals.addProperty("sgstAmount", Money.output(sgstTotal)); totals.addProperty("specialDiscount", new BigDecimal("0.00"));
        totals.addProperty("roundOff", new BigDecimal("0.00")); totals.addProperty("total", Money.output(total));
        invoice.add("totals", totals);
        JsonObject words = new JsonObject();
        words.addProperty("amountChargeable", amountToWords(total));
        words.addProperty("taxAmount", amountToWords(cgstTotal.add(sgstTotal)));
        invoice.add("amountsInWords", words);
        return invoice;
    }

    private static JsonObject company() {
        JsonObject company = new JsonObject(); company.addProperty("name", "Petsfort");
        company.addProperty("address", "Your Company Address, City, Postal Code");
        company.addProperty("gstNo", "YOUR_GST_NUMBER"); company.addProperty("email", "petsfort.in@gamil.com"); return company;
    }

    static String amountToWords(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, Money.ROUNDING);
        long rupees = normalized.longValue(); int paise = normalized.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String result = "INR " + words(rupees) + " Rupees";
        if (paise > 0) result += " and " + words(paise) + " Paise";
        return result + " Only";
    }

    private static String words(long n) {
        if (n == 0) return "Zero";
        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        StringBuilder out = new StringBuilder();
        long[] divisors = {10_000_000L, 100_000L, 1_000L, 100L}; String[] labels = {"Crore", "Lakh", "Thousand", "Hundred"};
        for (int i = 0; i < divisors.length; i++) if (n >= divisors[i]) { append(out, words(n / divisors[i]) + " " + labels[i]); n %= divisors[i]; }
        if (n > 0) { String tail = n < 20 ? ones[(int)n] : tens[(int)n / 10] + (n % 10 == 0 ? "" : " " + ones[(int)n % 10]); append(out, (out.length() > 0 ? "and " : "") + tail); }
        return out.toString();
    }

    private static void append(StringBuilder out, String text) { if (out.length() > 0) out.append(' '); out.append(text); }
    private static String string(JsonObject object, String key, String fallback) { JsonElement value = object.get(key); return value == null || value.isJsonNull() ? fallback : value.getAsString(); }
    private static int integer(JsonObject object, String key, int fallback) { JsonElement value = object.get(key); return value == null || value.isJsonNull() ? fallback : value.getAsInt(); }
    private static String plain(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }

    private static final class GstGroup {
        private final BigDecimal rate; private BigDecimal taxable = Money.ZERO, cgst = Money.ZERO, sgst = Money.ZERO;
        GstGroup(BigDecimal rate) { this.rate = rate; }
        void add(BigDecimal taxable, BigDecimal cgst, BigDecimal sgst) { this.taxable = this.taxable.add(taxable); this.cgst = this.cgst.add(cgst); this.sgst = this.sgst.add(sgst); }
        JsonObject json(String hsn) { JsonObject value = new JsonObject(); value.addProperty("hsnSac", hsn); value.addProperty("taxableValue", Money.output(taxable));
            value.addProperty("cgstRate", plain(rate) + "%"); value.addProperty("cgstAmount", Money.output(cgst));
            value.addProperty("sgstUtgstRate", plain(rate) + "%"); value.addProperty("sgstUtgstAmount", Money.output(sgst));
            value.addProperty("totalTaxAmount", Money.output(cgst.add(sgst))); return value; }
    }
}
