package my.edu.utar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * FR4: reads customer information from customer.txt and returns the customer
 * matching a given customer ID.
 *
 * File format (6 fields):
 *   customerID,name,email,phoneNumber,customerType,previousOrders
 * Legacy 5-field records are accepted, with previousOrders defaulting to 0.
 *
 * DESIGN NOTES (changes from the specification):
 *
 *  1. THE FILE PATH IS INJECTABLE.
 *     A hardcoded path cannot be pointed at a test fixture file, and a test that
 *     writes to the live customer.txt corrupts the data for later tests.
 *
 *  2. THREE DISTINCT FAILURES ARE DISTINGUISHED (Partition Table #8):
 *       null or blank ID  -> IllegalArgumentException
 *       malformed ID      -> IllegalArgumentException
 *       ID not in file    -> NoSuchElementException
 *       file missing      -> RuntimeException
 *     The original returned null for all of them, so a missing file could not be
 *     told apart from a missing customer.
 *
 *  3. customerExists() DOES NOT THROW.
 *     addNewCustomer needs a plain true/false duplicate check, so the internal
 *     search method returns null and only findCustomer() converts that into an
 *     exception.
 *
 * ASSUMPTION: a customer ID is the letter C followed by three digits (C001).
 */
public class readCustomer {

    private static final String DEFAULT_FILE = "customer.txt";

    /** Partition Table #8: valid customer ID format. */
    private static final Pattern ID_PATTERN = Pattern.compile("^[Cc][0-9]{3}$");

    private String filePath;

    public readCustomer() {
        this(DEFAULT_FILE);
    }

    public readCustomer(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty.");
        }
        this.filePath = filePath;
    }

    public String getFilePath() { return filePath; }

    /**
     * Returns the customer whose ID matches the search key.
     *
     * @throws IllegalArgumentException if the ID is null, blank or malformed
     * @throws NoSuchElementException   if no such customer exists in the file
     * @throws RuntimeException         if the file is missing or unreadable
     */
    public customer findCustomer(String targetID) {
        validateCustomerID(targetID);

        customer found = search(targetID);
        if (found == null) {
            throw new NoSuchElementException("Customer not found: " + targetID);
        }
        return found;
    }

    /** @return true if a customer with this ID already exists; never throws for a missing record. */
    public boolean customerExists(String targetID) {
        validateCustomerID(targetID);
        return search(targetID) != null;
    }

    /**
     * Reads every valid customer record in the file.
     *
     * NAMING NOTE: not called readCustomer(). A method sharing its class's name
     * is legal Java but is reported by Eclipse as "Method with a constructor
     * name" and reads like a malformed constructor.
     *
     * @throws RuntimeException if the file is missing or unreadable
     */
    public List<customer> readAllCustomers() {
        List<customer> customers = new ArrayList<customer>();
        File file = requireFile();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                customer c = parseLine(line);
                if (c != null) customers.add(c);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading customer file: " + e.getMessage(), e);
        } finally {
            closeQuietly(br);
        }
        return customers;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Searches the file and returns null when no record matches. */
    private customer search(String targetID) {
        File file = requireFile();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                customer c = parseLine(line);
                if (c != null && c.getCustomerID().equalsIgnoreCase(targetID.trim())) {
                    return c;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading customer file: " + e.getMessage(), e);
        } finally {
            closeQuietly(br);
        }
        return null;
    }

    private void validateCustomerID(String targetID) {
        if (targetID == null || targetID.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID must not be null or empty.");
        }
        if (!ID_PATTERN.matcher(targetID.trim()).matches()) {
            throw new IllegalArgumentException("Invalid customer ID format: " + targetID);
        }
    }

    private File requireFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Customer file not found: " + filePath);
        }
        return file;
    }

    /**
     * Parses one record. Returns null for a malformed line rather than throwing,
     * so one bad record does not make the whole file unreadable.
     */
    private customer parseLine(String line) {
        String[] d = line.split(",");
        try {
            if (d.length == 6) {
                int prevOrders = Integer.parseInt(d[5].trim());
                return new customer(d[0].trim(), d[1].trim(), d[2].trim(),
                                    d[3].trim(), d[4].trim(), prevOrders);
            }
            if (d.length == 5) { // legacy record, no previous order count
                return new customer(d[0].trim(), d[1].trim(), d[2].trim(),
                                    d[3].trim(), d[4].trim(), 0);
            }
        } catch (NumberFormatException e) {
            return null;            // previousOrders is not an integer
        } catch (IllegalArgumentException e) {
            return null;            // rejected by the customer constructor
        }
        return null;                // wrong number of fields
    }

    private void closeQuietly(BufferedReader br) {
        if (br != null) {
            try { br.close(); } catch (IOException ignored) { }
        }
    }
}