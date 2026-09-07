package my.edu.utar;

/**
 * FR7: generates an invoice for a completed print order.
 *
 * The invoice contains the customer details, the print order details, a full
 * breakdown of the printing charges, the optional service charges, the discount
 * applied and the final amount payable.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. generateInvoice() RETURNS THE INVOICE AS A String.
 *     A void method writing to System.out produces nothing that can be asserted
 *     against. displayInvoice() is retained and prints what generateInvoice()
 *     produces.
 *
 *  2. AN UNPRICED ORDER IS REFUSED (Decision Table #3, Rules 3 and 7).
 *     printOrder.isChargesCalculated() is false until calculatePrintingCharge
 *     has priced the order, so an invoice showing RM0.00 can never be produced.
 *
 *  3. THE DISCOUNT LINE IS ALWAYS SHOWN, even when it is RM0.00.
 */
public class generateInvoice {

    private static final String LINE   = "========================================";
    private static final String DIVIDE = "----------------------------------------";

    /**
     * Builds the invoice text for a priced print order.
     *
     * @throws IllegalArgumentException if the order or its customer is null
     * @throws IllegalStateException    if the printing charge has not been calculated
     */
    public String buildInvoice(printOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Print order must not be null.");
        }
        customer cust = order.getCustomerDetails();
        if (cust == null) {
            throw new IllegalArgumentException("Customer details must not be null.");
        }
        if (!order.isChargesCalculated()) {
            throw new IllegalStateException("Printing charge has not been calculated.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(LINE).append("\n");
        sb.append("          PRINTMASTER INVOICE           ").append("\n");
        sb.append(LINE).append("\n");

        // --- Customer details ---
        sb.append("Customer Details:").append("\n");
        sb.append(String.format("Customer ID    : %s%n", cust.getCustomerID()));
        sb.append(String.format("Customer Name  : %s%n", cust.getCustomerName()));
        sb.append(String.format("Phone Number   : %s%n", cust.getPhoneNumber()));
        sb.append(String.format("Email          : %s%n", cust.getEmail()));
        sb.append(String.format("Customer Type  : %s%n", cust.getCustomerType()));
        sb.append(DIVIDE).append("\n");

        // --- Print order details ---
        sb.append("Order Details:").append("\n");
        sb.append(String.format("Paper Size     : %s%n", order.getPaperSize()));
        sb.append(String.format("Print Type     : %s%n", order.getPrintType()));
        sb.append(String.format("Printing Side  : %s%n", order.getPrintingSide()));
        sb.append(String.format("Pages          : %d%n", order.getNumberOfPages()));
        sb.append(String.format("Copies         : %d%n", order.getNumberOfCopies()));
        sb.append(String.format("Printed Pages  : %d%n", order.getTotalPrintedPages()));
        sb.append(DIVIDE).append("\n");

        // --- Optional services ---
        sb.append("Optional Services:").append("\n");
        String binding = order.getBindingOption();
        boolean anyService = false;
        if (binding != null && !binding.equalsIgnoreCase(printOrder.BINDING_NONE)) {
            sb.append(String.format("  %-22s: %s%n", binding + " Binding", "Yes"));
            anyService = true;
        }
        if (order.isLaminationOption()) {
            sb.append(String.format("  %-22s: %d printed pages%n",
                    "Lamination", order.getTotalPrintedPages()));
            anyService = true;
        }
        if (order.isExpressPrinting()) {
            sb.append(String.format("  %-22s: %s%n", "Express Printing", "Yes"));
            anyService = true;
        }
        if (!anyService) {
            sb.append("  None selected").append("\n");
        }
        sb.append(DIVIDE).append("\n");

        // --- Charge breakdown ---
        sb.append("Charge Breakdown:").append("\n");
        sb.append(String.format("Base Printing Charge   : RM %10.2f%n", order.getBaseCharge()));
        sb.append(String.format("Optional Service Charge: RM %10.2f%n",
                order.getOptionalServiceCharge()));
        sb.append(String.format("Subtotal               : RM %10.2f%n", order.getSubtotal()));
        sb.append(String.format("Discount Applied       : -RM%10.2f%n", order.getDiscountAmount()));
        sb.append(DIVIDE).append("\n");
        sb.append(String.format("TOTAL AMOUNT PAYABLE   : RM %10.2f%n", order.getTotalCharge()));
        sb.append(DIVIDE).append("\n");
        sb.append(String.format("Order Status   : %s%n", order.getOrderStatus()));
        sb.append(String.format("Payment Status : %s%n", order.getPaymentStatus()));
        sb.append(LINE).append("\n");

        return sb.toString();
    }

    /** Prints the invoice to standard output. */
    public void displayInvoice(printOrder order) {
        System.out.print(buildInvoice(order));
    }
}