package my.edu.utar;

/**
 * FR1: holds all details of a print order.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. VALIDATION IN THE CONSTRUCTOR (Decision Table #1, Rules 3 to 6).
 *     An order with a missing print type, paper size or printing side, with an
 *     out of range page or copy count, or with more than one binding option, is
 *     rejected at creation and never exists as an object.
 *
 *  2. THE CALCULATED VALUES ARE STORED ON THE ORDER.
 *     The specification requires printOrder to store the base printing charge,
 *     additional service charges, discount, total, order status and payment
 *     status. setCharges() records all four figures at once and marks the order
 *     as priced, so generateInvoice can produce a full breakdown from the order
 *     alone and can refuse to invoice an order that has not been priced.
 *
 *  3. A NEW ORDER STARTS AS Pending Payment / Unpaid.
 *     payment.java updates it to Completed / Paid on success.
 *
 * Business Rule 9: only ONE binding option may be selected, so bindingOption is
 * a single String rather than a list. A multi valued string such as
 * "Comb, Spiral" is rejected.
 */
public class printOrder {

    // Order status constants
    public static final String STATUS_PENDING_PAYMENT = "Pending Payment";
    public static final String STATUS_COMPLETED       = "Completed";

    // Payment status constants
    public static final String PAYMENT_UNPAID = "Unpaid";
    public static final String PAYMENT_PAID   = "Paid";

    // Binding options (Table 3)
    public static final String BINDING_NONE   = "None";
    public static final String BINDING_STAPLE = "Staple";
    public static final String BINDING_COMB   = "Comb";
    public static final String BINDING_SPIRAL = "Spiral";

    // Paper sizes, print types and printing sides (Table 2)
    public static final String SIZE_A3 = "A3";
    public static final String SIZE_A4 = "A4";
    public static final String SIZE_A5 = "A5";
    public static final String TYPE_BW     = "Black & White";
    public static final String TYPE_COLOUR = "Colour";
    public static final String SIDE_SINGLE = "Single-sided";
    public static final String SIDE_DOUBLE = "Double-sided";

    // Business Rules 6, 7 and 8
    public static final int MIN_PAGES  = 1;
    public static final int MAX_PAGES  = 500;
    public static final int MIN_COPIES = 1;
    public static final int MAX_COPIES = 1000;

    // --- Order inputs ---
    private customer customerDetails;
    private String  printType;
    private String  paperSize;
    private String  printingSide;
    private int     numberOfPages;
    private int     numberOfCopies;
    private String  bindingOption;
    private boolean laminationOption;
    private boolean expressPrinting;

    // --- Calculated values ---
    private double  baseCharge;
    private double  optionalServiceCharge;
    private double  discountAmount;
    private double  totalCharge;
    private boolean chargesCalculated = false;

    // --- Statuses ---
    private String orderStatus   = STATUS_PENDING_PAYMENT;
    private String paymentStatus = PAYMENT_UNPAID;

    /**
     * Creates and validates a print order.
     *
     * @throws IllegalArgumentException if any selection is missing or invalid
     */
    public printOrder(customer cust, String printType, String paperSize, String printingSide,
                      int pages, int copies, String binding,
                      boolean lamination, boolean express) {

        if (cust == null) {
            throw new IllegalArgumentException("Customer must not be null.");
        }
        requireSelected(printType,    "Print type");
        requireSelected(paperSize,    "Paper size");
        requireSelected(printingSide, "Printing side");

        if (!isValidPrintType(printType)) {
            throw new IllegalArgumentException("Invalid print type: " + printType);
        }
        if (!isValidPaperSize(paperSize)) {
            throw new IllegalArgumentException("Invalid paper size: " + paperSize);
        }
        if (!isValidPrintingSide(printingSide)) {
            throw new IllegalArgumentException("Invalid printing side: " + printingSide);
        }
        if (pages < MIN_PAGES || pages > MAX_PAGES) {
            throw new IllegalArgumentException(
                "Pages must be between " + MIN_PAGES + " and " + MAX_PAGES + ".");
        }
        if (copies < MIN_COPIES || copies > MAX_COPIES) {
            throw new IllegalArgumentException(
                "Copies must be between " + MIN_COPIES + " and " + MAX_COPIES + ".");
        }

        this.bindingOption    = normaliseBinding(binding);
        this.customerDetails  = cust;
        this.printType        = printType.trim();
        this.paperSize        = paperSize.trim();
        this.printingSide     = printingSide.trim();
        this.numberOfPages    = pages;
        this.numberOfCopies   = copies;
        this.laminationOption = lamination;
        this.expressPrinting  = express;
    }

    /** Factory method matching Test Item 4.2 in the test plan. */
    public static printOrder createOrder(customer cust, String printType, String paperSize,
                                         String printingSide, int pages, int copies,
                                         String binding, boolean lamination, boolean express) {
        return new printOrder(cust, printType, paperSize, printingSide,
                              pages, copies, binding, lamination, express);
    }

    // --- Getters: inputs ---
    public customer getCustomerDetails() { return customerDetails; }
    public String  getPrintType()        { return printType; }
    public String  getPaperSize()        { return paperSize; }
    public String  getPrintingSide()     { return printingSide; }
    public int     getNumberOfPages()    { return numberOfPages; }
    public int     getNumberOfCopies()   { return numberOfCopies; }
    public String  getBindingOption()    { return bindingOption; }
    public boolean isLaminationOption()  { return laminationOption; }
    public boolean isExpressPrinting()   { return expressPrinting; }

    /** Business Rule 10: lamination is charged on the total printed pages. */
    public int getTotalPrintedPages() { return numberOfPages * numberOfCopies; }

    /**
     * Returns every order input as a single readable summary.
     * Individual getters remain available and are the preferred way to assert
     * on a single attribute.
     */
    public String getOrderDetails() {
        return String.format(
            "Customer=%s, PrintType=%s, PaperSize=%s, PrintingSide=%s, "
          + "Pages=%d, Copies=%d, Binding=%s, Lamination=%s, Express=%s",
            customerDetails.getCustomerID(), printType, paperSize, printingSide,
            numberOfPages, numberOfCopies, bindingOption,
            (laminationOption ? "Yes" : "No"), (expressPrinting ? "Yes" : "No"));
    }

    // --- Calculated values ---

    /** Records the full charge breakdown and marks the order as priced. */
    public void setCharges(double base, double optional, double discount, double total) {
        this.baseCharge            = base;
        this.optionalServiceCharge = optional;
        this.discountAmount        = discount;
        this.totalCharge           = total;
        this.chargesCalculated     = true;
    }

    public double getBaseCharge()             { return baseCharge; }
    public double getOptionalServiceCharge()  { return optionalServiceCharge; }
    public double getDiscountAmount()         { return discountAmount; }
    public double getTotalCharge()            { return totalCharge; }

    /** Subtotal = base charge + optional service charges, before any discount. */
    public double getSubtotal() { return baseCharge + optionalServiceCharge; }

    /** @return true once calculatePrintingCharge has priced this order. */
    public boolean isChargesCalculated() { return chargesCalculated; }

    // --- Statuses ---
    public String getOrderStatus()   { return orderStatus; }
    public String getPaymentStatus() { return paymentStatus; }

    public void setOrderStatus(String status) {
        requireSelected(status, "Order status");
        this.orderStatus = status.trim();
    }

    public void setPaymentStatus(String status) {
        requireSelected(status, "Payment status");
        this.paymentStatus = status.trim();
    }

    /**
     * Applies the outcome reported by payment.java:
     * success -> Completed / Paid, failure -> Pending Payment / Unpaid.
     */
    public void updateStatusAfterPayment(boolean paymentSuccessful) {
        if (paymentSuccessful) {
            this.orderStatus   = STATUS_COMPLETED;
            this.paymentStatus = PAYMENT_PAID;
        } else {
            this.orderStatus   = STATUS_PENDING_PAYMENT;
            this.paymentStatus = PAYMENT_UNPAID;
        }
    }

    // --- Validation helpers ---

    /** Business Rule 9: rejects a multi valued or unsupported binding selection. */
    private String normaliseBinding(String binding) {
        if (binding == null || binding.trim().isEmpty()) {
            return BINDING_NONE;
        }
        String b = binding.trim();
        if (b.contains(",")) {
            throw new IllegalArgumentException("Only one binding option may be selected.");
        }
        if (b.equalsIgnoreCase(BINDING_NONE))   return BINDING_NONE;
        if (b.equalsIgnoreCase(BINDING_STAPLE)) return BINDING_STAPLE;
        if (b.equalsIgnoreCase(BINDING_COMB))   return BINDING_COMB;
        if (b.equalsIgnoreCase(BINDING_SPIRAL)) return BINDING_SPIRAL;
        throw new IllegalArgumentException("Invalid binding option: " + binding);
    }

    public static boolean isValidPaperSize(String size) {
        if (size == null) return false;
        String s = size.trim();
        return s.equalsIgnoreCase(SIZE_A3) || s.equalsIgnoreCase(SIZE_A4)
            || s.equalsIgnoreCase(SIZE_A5);
    }

    public static boolean isValidPrintType(String type) {
        if (type == null) return false;
        String t = type.trim();
        return t.equalsIgnoreCase(TYPE_BW) || t.equalsIgnoreCase(TYPE_COLOUR);
    }

    public static boolean isValidPrintingSide(String side) {
        if (side == null) return false;
        String s = side.trim();
        return s.equalsIgnoreCase(SIDE_SINGLE) || s.equalsIgnoreCase(SIDE_DOUBLE);
    }

    private static void requireSelected(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must be selected.");
        }
    }
}