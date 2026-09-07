package my.edu.utar;

/**
 * FR9: email invoice module. NOT TO BE DEVELOPED.
 * Method signatures only - used as a test double during testing.
 *
 * Once payment is successful, an invoice is emailed to the customer containing
 * the customer details, print order details, breakdown of printing charges,
 * discounts applied and the final amount paid.
 */
public class emailInvoice {

    /**
     * Emails the invoice (PDF) to the customer.
     *
     * @param emailAddress the customer's email address
     * @param invoice      the invoice text produced by generateInvoice
     * @return true if the email was dispatched successfully
     */
    public boolean sendInvoiceEmail(String emailAddress, String invoice) {
        throw new UnsupportedOperationException(
            "Not to be developed - used as a test double.");
    }
}