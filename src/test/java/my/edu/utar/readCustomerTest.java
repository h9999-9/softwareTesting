package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.FileParameters;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class readCustomerTest {

    private static final String CUSTOMER_FILE = "customer.txt";

    private final readCustomer reader = new readCustomer(CUSTOMER_FILE);

    // ------------------------------------------------------------------
    // TC#1 - an existing ID returns the full record
    // ------------------------------------------------------------------
    @Test
    public void TC1_existingCustomerIDReturnsTheCorrectRecord() {
        customer c = reader.findCustomer("C001");

        assertNotNull(c);
        assertEquals("C001", c.getCustomerID());
        assertEquals("Ali Bin Ahmad", c.getCustomerName());
        assertEquals("ali@gmail.com", c.getEmail());
        assertEquals("0123456789", c.getPhoneNumber());
        assertEquals("Student", c.getCustomerType());
        assertEquals(10, c.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#2 - a second ID, to prove the search is not returning record one
    // ------------------------------------------------------------------
    @Test
    public void TC2_secondExistingCustomerIDReturnsTheCorrectRecord() {
        customer c = reader.findCustomer("C002");

        assertEquals("C002", c.getCustomerID());
        assertEquals("Tan Wei Ming", c.getCustomerName());
        assertEquals("Corporate", c.getCustomerType());
        assertEquals(25, c.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#3 - PARAMETERISED, values READ FROM A TEXT FILE (not hardcoded)
    // ------------------------------------------------------------------
    @Test
    @FileParameters("customer.txt")
    public void TC3_everyRecordInTheFileIsRetrievableByID(String id, String name, String email, String phone, String type, int previousOrders) {
        customer c = reader.findCustomer(id);

        assertEquals(id, c.getCustomerID());
        assertEquals(name, c.getCustomerName());
        assertEquals(email, c.getEmail());
        assertEquals(phone, c.getPhoneNumber());
        assertEquals(type, c.getCustomerType());
        assertEquals(previousOrders, c.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#4 - every record is read, none skipped
    // ------------------------------------------------------------------
    @Test
    public void TC4_recordCountMatchesTheFile() {
        List<customer> all = reader.readAllCustomers();

        assertEquals(countLines(CUSTOMER_FILE), all.size());
        assertEquals(5, all.size());
        assertEquals("C001", all.get(0).getCustomerID());   // first record not skipped
        assertEquals("C005", all.get(4).getCustomerID());   // last record not skipped
    }

    // ------------------------------------------------------------------
    // TC#5 - EP invalid: well formed ID that is absent from the file
    // ------------------------------------------------------------------
    @Test
    public void TC5_wellFormedButAbsentIDIsRejected() {
        NoSuchElementException e = assertThrows(NoSuchElementException.class,
            () -> reader.findCustomer("C999"));
        assertEquals("Customer not found: C999", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#6 - EP invalid: malformed ID
    // ------------------------------------------------------------------
    @Test
    public void TC6_malformedCustomerIDIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> reader.findCustomer("ABC!!"));
        assertEquals("Invalid customer ID format: ABC!!", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#7 - null or empty search key
    // ------------------------------------------------------------------
    @Test
    @Parameters(method = "blankIDs")
    public void TC7_nullOrEmptyCustomerIDIsRejected(String badID) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> reader.findCustomer(badID));
        assertEquals("Customer ID must not be null or empty.", e.getMessage());
    }

    public Object[] blankIDs() {
        return new Object[] { new Object[] { null }, new Object[] { "" }, new Object[] { "   " } };
    }

    // ------------------------------------------------------------------
    // TC#8 - a missing data file is distinguishable from a missing record
    // ------------------------------------------------------------------
    @Test
    public void TC8_missingCustomerFileIsHandled() {
        readCustomer missing = new readCustomer("no_such_file.txt");

        RuntimeException e = assertThrows(RuntimeException.class, missing::readAllCustomers);
        assertEquals("Customer file not found: no_such_file.txt", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#9 - customerExists() returns a boolean and never throws for an absent record
    // ------------------------------------------------------------------
    @Test
    public void TC9_customerExistsReturnsBooleanWithoutThrowing() {
        assertTrue(reader.customerExists("C001"));
        assertFalse(reader.customerExists("C999"));
    }

    // ------------------------------------------------------------------
    // TC#10 - one malformed record does not make the file unreadable
    // ------------------------------------------------------------------
    @Test
    public void TC10_malformedRecordIsSkippedNotFatal() throws IOException {
        Path temp = Files.createTempFile("read-customer-malformed", ".txt");
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(CUSTOMER_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.trim().isEmpty()) content.append(line).append("\n");
                }
            }
            content.append("BADLINE\n");
            Files.write(temp, content.toString().getBytes(StandardCharsets.UTF_8));

            List<customer> all = new readCustomer(temp.toString()).readAllCustomers();
            assertEquals(5, all.size());   // the six line file yields five valid records
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // ------------------------------------------------------------------
    // Local helper
    // ------------------------------------------------------------------
    private static int countLines(String path) {
        int n = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) n++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + path, e);
        }
        return n;
    }
}