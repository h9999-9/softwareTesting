package my.edu.utar;

/**
 * FR8: payment module. NOT TO BE DEVELOPED.
 * Method signatures only - used as a test double during testing.
 *
 * Once payment is successful the order status is updated to Completed.
 * If payment is unsuccessful the order status is updated to Pending Payment.
 * The caller applies this via printOrder.updateStatusAfterPayment(boolean).
 */
public class payment {

    public static final String METHOD_EWALLET        = "e-Wallet";
    public static final String METHOD_CREDIT_CARD    = "Credit Card";
    public static final String METHOD_ONLINE_BANKING = "Online Banking";

    public static final String STATUS_SUCCESSFUL = "Successful";
    public static final String STATUS_FAILED     = "Failed";

    private double paymentAmount;
    private String paymentMethod;
    private String paymentStatus;

    /**
     * Processes the payment for a print order.
     *
     * @param paymentMethod e-Wallet, Credit Card or Online Banking
     * @param amount        the total amount payable
     * @return true if the payment succeeded
     */
    public boolean processPayment(String paymentMethod, double amount) {
        throw new UnsupportedOperationException(
            "Not to be developed - used as a test double.");
    }

    public double getPaymentAmount() { return paymentAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
}