package my.edu.utar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * FR3: registers a new customer and appends the record to customer.txt.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. THE FILE PATH AND THE READER ARE INJECTABLE.
 *     Tests can write to a fixture file, and the duplicate check can be driven
 *     by a Mockito mock of readCustomer instead of a real file.
 *
 *  2. FULL FIELD VALIDATION (Partition Table #9).
 *     FR3 requires a name, phone number and email address to be entered, so all
 *     three are validated before the record is written.
 *
 *  3. DUPLICATE IDs ARE REJECTED and the file is left unmodified.
 *
 * ASSUMPTIONS (Partition Table #9):
 *  - a customer name contains alphabetic characters and spaces only
 *  - a Malaysian phone number is 10 or 11 digits with no spaces or symbols
 *  - an email address takes the form local@domain.tld
 *  - no field may contain a comma, because customer.txt is comma delimited
 */
public class addNewCustomer {

    private static final String DEFAULT_FILE = "customer.txt";

    public static final int PHONE_MIN_DIGITS = 10;
    public static final int PHONE_MAX_DIGITS = 11;

    private static final Pattern NAME_PATTERN  = Pattern.compile("^[A-Za-z ]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,11}$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private String filePath;
    private readCustomer reader;

    public addNewCustomer() {
        this(DEFAULT_FILE);
    }

    public addNewCustomer(String filePath) {
        this.filePath = filePath;
        this.reader   = new readCustomer(filePath);
    }

    /** Allows a Mockito mock of readCustomer to be injected for the duplicate check. */
    public addNewCustomer(String filePath, readCustomer reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader must not be null.");
        }
        this.filePath = filePath;
        this.reader   = reader;
    }

    public String getFilePath() { return filePath; }

    /**
     * Validates the customer and appends the record to the file.
     *
     * @return true if the record was written successfully
     * @throws IllegalArgumentException if any field is invalid or the ID is a duplicate
     */
    public boolean addCustomer(customer newCustomer) {
        validateCustomer(newCustomer);

        File file = new File(filePath);
        if (file.exists() && reader.customerExists(newCustomer.getCustomerID())) {
            throw new IllegalArgumentException(
                "Customer ID already exists: " + newCustomer.getCustomerID());
        }

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(newCustomer.toString());
            bw.newLine();
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Error writing to customer file: " + e.getMessage(), e);
        } finally {
            if (bw != null) {
                try { bw.close(); } catch (IOException ignored) { }
            }
        }
    }

    /**
     * FR3: builds the customer from the details entered by the service staff and
     * writes the record. A newly registered customer always has 0 previous orders.
     *
     * NAMING NOTE: this method is deliberately NOT called addNewCustomer(). A
     * method sharing its class's name is legal Java but is reported by Eclipse
     * as "Method with a constructor name", and reads like a broken constructor.
     */
    public customer registerCustomer(String customerID, String name, String email,
                                     String phoneNumber, String customerType) {
        customer c = new customer(customerID, name, email, phoneNumber, customerType, 0);
        addCustomer(c);
        return c;
    }

    /**
     * Generates the next sequential customer ID (C001, C002, ...).
     * ASSUMPTION: IDs follow the pattern C + 3 digits.
     */
    public String generateCustomerID() {
        File file = new File(filePath);
        if (!file.exists()) return "C001";

        List<customer> existing = reader.readAllCustomers();
        int highest = 0;
        for (customer c : existing) {
            String id = c.getCustomerID();
            if (id != null && id.matches("(?i)C[0-9]+")) {
                int n = Integer.parseInt(id.substring(1));
                if (n > highest) highest = n;
            }
        }
        return String.format("C%03d", highest + 1);
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /** Validates every required field before the record is written. */
    public void validateCustomer(customer c) {
        if (c == null) {
            throw new IllegalArgumentException("Customer must not be null.");
        }
        requireNonBlank(c.getCustomerID(),   "Customer ID");
        requireNonBlank(c.getCustomerName(), "Customer name");
        requireNonBlank(c.getPhoneNumber(),  "Phone number");
        requireNonBlank(c.getEmail(),        "Email address");
        requireNonBlank(c.getCustomerType(), "Customer type");

        if (!isValidName(c.getCustomerName())) {
            throw new IllegalArgumentException("Invalid customer name: " + c.getCustomerName());
        }
        if (!isNumeric(c.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number must contain digits only.");
        }
        if (!isValidPhone(c.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number must be "
                    + PHONE_MIN_DIGITS + " to " + PHONE_MAX_DIGITS + " digits.");
        }
        if (!isValidEmail(c.getEmail())) {
            throw new IllegalArgumentException("Invalid email address: " + c.getEmail());
        }
        if (!customer.isValidCustomerType(c.getCustomerType())) {
            throw new IllegalArgumentException("Invalid customer type: " + c.getCustomerType());
        }
        if (containsComma(c.getCustomerID())   || containsComma(c.getCustomerName())
         || containsComma(c.getEmail())        || containsComma(c.getPhoneNumber())
         || containsComma(c.getCustomerType())) {
            throw new IllegalArgumentException("Customer fields must not contain commas.");
        }
    }

    /** Alphabetic characters and spaces only. */
    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    /** Exactly 10 or 11 digits, no spaces or symbols. */
    public boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /** Form local@domain.tld. */
    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isNumeric(String s) {
        return s != null && s.trim().matches("^[0-9]+$");
    }

    private boolean containsComma(String s) {
        return s != null && s.contains(",");
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be null or empty.");
        }
    }
}