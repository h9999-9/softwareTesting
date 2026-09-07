package my.edu.utar;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * FR5: calculates the total printing charge for a print order.
 *
 * Business Rules 1 to 5 are applied in this order:
 *   1/2. Base charge = Base Rate (Table 2) x Pages x Copies
 *   3.   Optional service charges (Table 3) are added
 *   4.   Customer discounts (Table 4) are applied to the subtotal
 *   5.   The final total is rounded to two decimal places
 *
 * Appendix A: printerAvailability.isPrinterAvailable() is invoked BEFORE the
 * charges are calculated. If it returns false, the message is displayed and the
 * order creation process terminates, so no charge is calculated and no invoice
 * is generated.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. ROUNDING HAPPENS ONCE, ON THE FINAL TOTAL (Business Rule 5).
 *     Intermediate values are kept exact; only the final total is rounded, half
 *     up, to two decimal places.
 *
 *  2. BigDecimal IS USED FOR ALL MONEY ARITHMETIC.
 *     405.00 * 0.90 * 0.95 evaluates to 346.27499999999998 in double
 *     arithmetic, which rounds DOWN to RM346.27 instead of up to RM346.28.
 *
 *  3. INPUT VALIDATION RUNS BEFORE THE PRINTER IS CHECKED.
 *     isPrinterAvailable() is never called for an invalid order, and the
 *     printer is still checked before any charge is calculated.
 *
 *  4. THE CALCULATED VALUES ARE WRITTEN BACK TO THE PRINT ORDER, so
 *     generateInvoice can produce the full breakdown from the order alone.
 *
 *  5. THE DISCOUNT SHOWN IS DERIVED FROM THE ROUNDED FIGURES, so the printed
 *     breakdown balances: base + optional - discount = total. The exact
 *     discount on a RM405.00 subtotal is RM58.725, which would round to
 *     RM58.73, and RM405.00 - RM58.73 = RM346.27 rather than RM346.28.
 */
public class calculatePrintingCharge {

    /** Appendix A: the exact message displayed when no printer is available. */
    public static final String MSG_PRINTER_UNAVAILABLE =
            "Selected printer is currently unavailable.";

    // Table 3: optional service charges
    public static final double CHARGE_STAPLE     = 2.00;
    public static final double CHARGE_COMB       = 5.00;
    public static final double CHARGE_SPIRAL     = 8.00;
    public static final double CHARGE_LAMINATION = 1.50;   // per printed page
    public static final double CHARGE_EXPRESS    = 20.00;  // per order

    private final printerAvailability printerCheck;
    private final applyDiscount discountCalculator;

    /**
     * Constructor injection allows printerAvailability to be replaced by a
     * Mockito mock and applyDiscount by a stub during testing.
     */
    public calculatePrintingCharge(printerAvailability printerCheck,
                                   applyDiscount discountCalculator) {
        if (printerCheck == null) {
            throw new IllegalArgumentException("Printer availability service must not be null.");
        }
        if (discountCalculator == null) {
            throw new IllegalArgumentException("Discount calculator must not be null.");
        }
        this.printerCheck       = printerCheck;
        this.discountCalculator = discountCalculator;
    }

    // ------------------------------------------------------------------
    // Table 2: base printing rate
    // ------------------------------------------------------------------

    /**
     * Returns the per page rate from Table 2.
     * Public so the twelve rate partitions (Partition Table #3) can be tested
     * directly with a parameterised test.
     *
     * @throws IllegalArgumentException if any selection is missing or unsupported
     */
    public double getBaseRate(String paperSize, String printType, String printingSide) {
        requireSelected(paperSize,    "Paper size");
        requireSelected(printType,    "Print type");
        requireSelected(printingSide, "Printing side");

        if (!printOrder.isValidPaperSize(paperSize)) {
            throw new IllegalArgumentException("Invalid paper size: " + paperSize);
        }
        if (!printOrder.isValidPrintType(printType)) {
            throw new IllegalArgumentException("Invalid print type: " + printType);
        }
        if (!printOrder.isValidPrintingSide(printingSide)) {
            throw new IllegalArgumentException("Invalid printing side: " + printingSide);
        }

        String size   = paperSize.trim();
        boolean single = printingSide.trim().equalsIgnoreCase(printOrder.SIDE_SINGLE);
        boolean bw     = printType.trim().equalsIgnoreCase(printOrder.TYPE_BW);

        if (size.equalsIgnoreCase(printOrder.SIZE_A4)) {
            if (bw) return single ? 0.20 : 0.18;
            return single ? 0.80 : 0.75;
        }
        if (size.equalsIgnoreCase(printOrder.SIZE_A3)) {
            if (bw) return single ? 0.40 : 0.35;
            return single ? 1.50 : 1.40;
        }
        // A5
        if (bw) return single ? 0.15 : 0.13;
        return single ? 0.60 : 0.55;
    }

    // ------------------------------------------------------------------
    // Business Rule 2: base printing charge
    // ------------------------------------------------------------------

    /**
     * Base Charge = Base Rate x Number of Pages x Number of Copies.
     * NOT rounded here; Business Rule 5 rounds the final total.
     */
    public double calculateBaseCharge(String paperSize, String printType,
                                      String printingSide, int pages, int copies) {
        validatePages(pages);
        validateCopies(copies);
        double rate = getBaseRate(paperSize, printType, printingSide);

        return BigDecimal.valueOf(rate)
                .multiply(BigDecimal.valueOf(pages))
                .multiply(BigDecimal.valueOf(copies))
                .doubleValue();
    }

    // ------------------------------------------------------------------
    // Business Rule 3: optional service charges (Table 3)
    // ------------------------------------------------------------------

    /**
     * Business Rule 9: only one binding option may be selected.
     * Business Rule 10: lamination is charged on the total printed pages
     * (pages x copies), not on the page count alone.
     *
     * @param binding    Staple, Comb, Spiral, or None / null / blank for no binding
     * @param totalPages pages x copies
     */
    public double calculateOptionalServiceCharge(String binding, boolean lamination,
                                                 boolean express, int totalPages) {
        if (totalPages < 1) {
            throw new IllegalArgumentException("Pages and copies must be at least 1.");
        }

        BigDecimal total = BigDecimal.ZERO;
        total = total.add(BigDecimal.valueOf(getBindingCharge(binding)));

        if (lamination) {
            total = total.add(BigDecimal.valueOf(CHARGE_LAMINATION)
                                        .multiply(BigDecimal.valueOf(totalPages)));
        }
        if (express) {
            total = total.add(BigDecimal.valueOf(CHARGE_EXPRESS));
        }
        return total.doubleValue();
    }

    /**
     * Flat charge for the selected binding option.
     * No binding is a VALID selection costing RM0.00 (Decision Table #1, Rule 7).
     */
    public double getBindingCharge(String binding) {
        if (binding == null || binding.trim().isEmpty()) {
            return 0.00;
        }
        String b = binding.trim();

        // Business Rule 9: a multi valued selection such as "Comb, Spiral"
        if (b.contains(",")) {
            throw new IllegalArgumentException("Only one binding option may be selected.");
        }
        if (b.equalsIgnoreCase(printOrder.BINDING_NONE))   return 0.00;
        if (b.equalsIgnoreCase(printOrder.BINDING_STAPLE)) return CHARGE_STAPLE;
        if (b.equalsIgnoreCase(printOrder.BINDING_COMB))   return CHARGE_COMB;
        if (b.equalsIgnoreCase(printOrder.BINDING_SPIRAL)) return CHARGE_SPIRAL;

        throw new IllegalArgumentException("Invalid binding option: " + binding);
    }

    // ------------------------------------------------------------------
    // Full calculation (FR5)
    // ------------------------------------------------------------------

    /**
     * Calculates the total printing charge, stores the full breakdown on the
     * order, and returns the rounded total.
     *
     * Order of operations:
     *   1. validate the order          (no external call is made if it fails)
     *   2. check printer availability  (Appendix A: before any calculation)
     *   3. base charge                 (Business Rules 1 and 2)
     *   4. optional service charges    (Business Rule 3)
     *   5. discount on the subtotal    (Business Rule 4)
     *   6. round the final total       (Business Rule 5)
     *
     * @throws IllegalArgumentException if the order is invalid
     * @throws IllegalStateException    if no suitable printer is available
     */
    public double calculateTotalCharge(printOrder order) {
        validateOrder(order);
        customer cust = order.getCustomerDetails();

        // Appendix A: invoked before the printing charges are calculated
        if (!printerCheck.isPrinterAvailable(order.getPaperSize(), order.getPrintType())) {
            System.out.println(MSG_PRINTER_UNAVAILABLE);
            throw new IllegalStateException(MSG_PRINTER_UNAVAILABLE);
        }

        double base = calculateBaseCharge(order.getPaperSize(), order.getPrintType(),
                                          order.getPrintingSide(), order.getNumberOfPages(),
                                          order.getNumberOfCopies());

        double optional = calculateOptionalServiceCharge(order.getBindingOption(),
                                                         order.isLaminationOption(),
                                                         order.isExpressPrinting(),
                                                         order.getTotalPrintedPages());

        BigDecimal subtotal = BigDecimal.valueOf(base).add(BigDecimal.valueOf(optional));

        // Business Rule 4: discounts applied after all optional service charges
        double discount = discountCalculator.calculateDiscount(
                cust.getCustomerType(), subtotal.doubleValue(), cust.getPreviousOrders());

        BigDecimal total = subtotal.subtract(BigDecimal.valueOf(discount));

        // Business Rule 5: round the final printing charge to two decimal places
        double roundedTotal    = round2(total);
        double roundedBase     = round2(BigDecimal.valueOf(base));
        double roundedOptional = round2(BigDecimal.valueOf(optional));
        double roundedDiscount = round2(BigDecimal.valueOf(roundedBase)
                                        .add(BigDecimal.valueOf(roundedOptional))
                                        .subtract(BigDecimal.valueOf(roundedTotal)));

        order.setCharges(roundedBase, roundedOptional, roundedDiscount, roundedTotal);
        return roundedTotal;
    }

    /**
     * Overload retained for compatibility with the original signature. The
     * supplied previous order count overrides the value held on the customer,
     * which is convenient when driving a test from an external data file.
     */
    public double calculateTotalCharge(printOrder order, int previousOrders) {
        validateOrder(order);
        if (previousOrders < 0) {
            throw new IllegalArgumentException("Previous orders must not be negative.");
        }
        order.getCustomerDetails().setPreviousOrders(previousOrders);
        return calculateTotalCharge(order);
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Validates every order input. Called BEFORE the external printer service,
     * so an invalid order never reaches printerAvailability
     * (asserted with Mockito verify(never())).
     */
    public void validateOrder(printOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Print order must not be null.");
        }
        if (order.getCustomerDetails() == null) {
            throw new IllegalArgumentException("Customer must not be null.");
        }
        validatePages(order.getNumberOfPages());
        validateCopies(order.getNumberOfCopies());
        getBaseRate(order.getPaperSize(), order.getPrintType(), order.getPrintingSide());
        getBindingCharge(order.getBindingOption());
    }

    /** Business Rules 6 and 7. */
    private void validatePages(int pages) {
        if (pages < printOrder.MIN_PAGES || pages > printOrder.MAX_PAGES) {
            throw new IllegalArgumentException("Pages must be between "
                    + printOrder.MIN_PAGES + " and " + printOrder.MAX_PAGES + ".");
        }
    }

    /** Business Rules 6 and 8. */
    private void validateCopies(int copies) {
        if (copies < printOrder.MIN_COPIES || copies > printOrder.MAX_COPIES) {
            throw new IllegalArgumentException("Copies must be between "
                    + printOrder.MIN_COPIES + " and " + printOrder.MAX_COPIES + ".");
        }
    }

    private void requireSelected(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must be selected.");
        }
    }

    /** Business Rule 5: half up rounding to two decimal places. */
    private double round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}