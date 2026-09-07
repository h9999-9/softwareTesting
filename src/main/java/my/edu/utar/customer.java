package my.edu.utar;

/**
 * FR2: stores customer information.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. previousOrders ADDED.
 *     Table 4 grants an additional 5% to an existing customer with more than 20
 *     previous orders. The specification does not say where that count is held,
 *     so it is stored here as a separate int field. A newly registered customer
 *     always has 0.
 *
 *  2. "Existing Customer" IS DERIVED, NOT STORED.
 *     previousOrders > 20 already implies an existing customer, because a new
 *     customer necessarily has 0 previous orders. No separate flag is needed.
 *
 *  3. VALIDATION IN THE CONSTRUCTOR.
 *     The customer ID, name and customer type are validated at construction, so
 *     an invalid customer object can never exist (Test Cases customer TC#4 to
 *     TC#7). Email and phone number FORMAT is validated in addNewCustomer,
 *     which is where FR3 requires them to be entered.
 *
 * ASSUMPTION: Table 4 requires a valid student ID for the 10% student discount.
 * A customer recorded with the type "Student" is assumed to have presented a
 * valid student ID at registration, since no verification service is available.
 * The discount therefore depends on the customer type alone.
 */
public class customer {

    // Customer category constants (Table 4)
    public static final String TYPE_STUDENT   = "Student";
    public static final String TYPE_CORPORATE = "Corporate";
    public static final String TYPE_OTHER     = "Other";

    /** Table 4: an additional 5% applies above this number of previous orders. */
    public static final int LOYALTY_THRESHOLD = 20;

    private String customerID;
    private String name;
    private String email;
    private String phoneNumber;
    private String customerType;   // Student / Corporate / Other
    private int    previousOrders; // 0 for newly registered customers

    /** Full constructor. */
    public customer(String customerID, String name, String email,
                    String phoneNumber, String customerType, int previousOrders) {

        if (customerID == null || customerID.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID must not be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name must not be null or empty.");
        }
        if (customerType == null || customerType.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer type must not be null or empty.");
        }
        if (!isValidCustomerType(customerType)) {
            throw new IllegalArgumentException("Invalid customer type: " + customerType);
        }
        if (previousOrders < 0) {
            throw new IllegalArgumentException("Previous orders must not be negative.");
        }

        this.customerID     = customerID.trim();
        this.name           = name.trim();
        this.email          = (email == null) ? null : email.trim();
        this.phoneNumber    = (phoneNumber == null) ? null : phoneNumber.trim();
        this.customerType   = customerType.trim();
        this.previousOrders = previousOrders;
    }

    /** Convenience constructor for a newly registered customer (0 previous orders). */
    public customer(String customerID, String name, String email,
                    String phoneNumber, String customerType) {
        this(customerID, name, email, phoneNumber, customerType, 0);
    }

    public String getCustomerID()   { return customerID; }
    public String getCustomerName() { return name; }
    public String getEmail()        { return email; }
    public String getPhoneNumber()  { return phoneNumber; }
    public String getCustomerType() { return customerType; }
    public int    getPreviousOrders() { return previousOrders; }

    public void setPreviousOrders(int previousOrders) {
        if (previousOrders < 0) {
            throw new IllegalArgumentException("Previous orders must not be negative.");
        }
        this.previousOrders = previousOrders;
    }

    /** An existing customer is one with at least one recorded previous order. */
    public boolean isExistingCustomer() {
        return previousOrders > 0;
    }

    /** Table 4: qualifies for the additional 5% loyalty discount. */
    public boolean qualifiesForLoyaltyDiscount() {
        return previousOrders > LOYALTY_THRESHOLD;
    }

    /** @return true if the type is one of Student, Corporate or Other. */
    public static boolean isValidCustomerType(String type) {
        if (type == null) return false;
        String t = type.trim();
        return t.equalsIgnoreCase(TYPE_STUDENT)
            || t.equalsIgnoreCase(TYPE_CORPORATE)
            || t.equalsIgnoreCase(TYPE_OTHER);
    }

    /**
     * Record format written to and read from customer.txt:
     *   customerID,name,email,phoneNumber,customerType,previousOrders
     */
    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%d",
                customerID, name, email, phoneNumber, customerType, previousOrders);
    }
}