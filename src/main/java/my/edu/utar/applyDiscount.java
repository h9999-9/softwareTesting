package my.edu.utar;

import java.math.BigDecimal;

/**
 * FR5 / Table 4: calculates the total discount amount.
 *
 * Discounts are cumulative and applied SEQUENTIALLY to the running subtotal:
 *     Student                                  -> 10%
 *     Corporate Customer                       -> 15%
 *     Order subtotal exceeds RM300             -> additional 5%
 *     Existing customer with > 20 prev orders  -> additional 5%
 *
 * Returns the EXACT, UNROUNDED discount amount (subtotal - discounted total).
 *
 * WHY THE VALUE IS NOT ROUNDED HERE:
 * Business Rule 5 rounds the FINAL printing charge. Rounding the discount here
 * as well would round twice and lose a sen. Rounding therefore happens once, in
 * calculatePrintingCharge, on the final total.
 *
 * WHY BigDecimal IS USED:
 * Chained double multiplication accumulates binary floating point error.
 * 405.0 * 0.90 * 0.95 evaluates to 346.27499999999998 in double arithmetic,
 * which rounds DOWN to RM346.27 instead of up to the correct RM346.28.
 * BigDecimal keeps the decimal value exact so the final rounding is correct.
 *
 * ASSUMPTION: the volume discount is assessed on the ORIGINAL subtotal, before
 * any category discount is applied. Table 4 refers to the "order subtotal",
 * which Business Rule 4 defines as the value after optional service charges and
 * before discounts.
 */
public class applyDiscount {

    private static final BigDecimal STUDENT_RATE   = new BigDecimal("0.90"); // 10% off
    private static final BigDecimal CORPORATE_RATE = new BigDecimal("0.85"); // 15% off
    private static final BigDecimal EXTRA_RATE     = new BigDecimal("0.95"); // 5% off

    public static final double VOLUME_THRESHOLD  = 300.00;
    public static final int    LOYALTY_THRESHOLD = 20;

    /**
     * @param customerType   Student, Corporate or Other
     * @param subtotal       base charge + optional service charges
     * @param previousOrders number of previous orders (0 for a new customer)
     * @return the exact, unrounded discount amount to subtract from the subtotal
     *
     * @throws IllegalArgumentException if the type is null or unsupported, or if
     *                                  the subtotal or order count is negative
     */
    public double calculateDiscount(String customerType, double subtotal, int previousOrders) {

        if (customerType == null || customerType.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer type must not be null or empty.");
        }
        if (!customer.isValidCustomerType(customerType)) {
            throw new IllegalArgumentException("Invalid customer type: " + customerType);
        }
        if (subtotal < 0) {
            throw new IllegalArgumentException("Subtotal must not be negative.");
        }
        if (previousOrders < 0) {
            throw new IllegalArgumentException("Previous orders must not be negative.");
        }

        BigDecimal original = BigDecimal.valueOf(subtotal);
        BigDecimal running  = original;
        String type = customerType.trim();

        // --- Customer category discount (Table 4) ---
        if (type.equalsIgnoreCase(customer.TYPE_STUDENT)) {
            running = running.multiply(STUDENT_RATE);
        } else if (type.equalsIgnoreCase(customer.TYPE_CORPORATE)) {
            running = running.multiply(CORPORATE_RATE);
        }
        // "Other" receives no category discount

        // --- Additional 5%: order subtotal EXCEEDS RM300 (strictly greater) ---
        if (subtotal > VOLUME_THRESHOLD) {
            running = running.multiply(EXTRA_RATE);
        }

        // --- Additional 5%: MORE THAN 20 previous orders (strictly greater) ---
        // previousOrders > 20 already implies an existing customer, because a
        // newly registered customer always has 0 previous orders.
        if (previousOrders > LOYALTY_THRESHOLD) {
            running = running.multiply(EXTRA_RATE);
        }

        return original.subtract(running).doubleValue();
    }

    /** Convenience overload taking the customer object directly. */
    public double calculateDiscount(customer cust, double subtotal) {
        if (cust == null) {
            throw new IllegalArgumentException("Customer must not be null.");
        }
        return calculateDiscount(cust.getCustomerType(), subtotal, cust.getPreviousOrders());
    }
}