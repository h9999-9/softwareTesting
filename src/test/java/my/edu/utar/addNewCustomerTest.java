package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class addNewCustomerTest {

    private static final String BASELINE =
        "C001,Ali Bin Ahmad,ali@gmail.com,0123456789,Student,10\n"
      + "C002,Tan Wei Ming,tanwm@printcorp.com,0129876543,Corporate,25\n"
      + "C003,Siti Aminah,siti.aminah@gmail.com,0111234567,Other,5\n"
      + "C004,Rajesh Kumar,rajesh@yahoo.com,01123456789,Student,25\n"
      + "C005,Lim Mei Ling,meiling@hotmail.com,0198765432,Corporate,3\n";

    private Path tempFile;
    private String path;
    private addNewCustomer adder;

    @Before
    public void setUp() throws IOException {
        tempFile = Files.createTempFile("add-customer", ".txt");
        Files.write(tempFile, BASELINE.getBytes(StandardCharsets.UTF_8));
        path = tempFile.toString();
        adder = new addNewCustomer(path);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    // ------------------------------------------------------------------
    // TC#1 - a valid record is appended
    // ------------------------------------------------------------------
    @Test
    public void TC1_validCustomerIsWrittenToTheFile() {
        int before = countLines();

        adder.registerCustomer("C006", "Siti Nurhaliza", "siti@gmail.com", "0198765432", customer.TYPE_OTHER);

        assertEquals(before + 1, countLines());
        assertEquals("C006,Siti Nurhaliza,siti@gmail.com,0198765432,Other,0", lastLine());
    }

    // ------------------------------------------------------------------
    // TC#2 - the new record is retrievable afterwards
    // ------------------------------------------------------------------
    @Test
    public void TC2_newCustomerIsRetrievableAfterwards() {
        adder.registerCustomer("C006", "Siti Nurhaliza", "siti@gmail.com", "0198765432", customer.TYPE_OTHER);

        customer found = new readCustomer(path).findCustomer("C006");
        assertEquals("C006", found.getCustomerID());
        assertEquals("Siti Nurhaliza", found.getCustomerName());
        assertEquals("siti@gmail.com", found.getEmail());
        assertEquals("0198765432", found.getPhoneNumber());
        assertEquals("Other", found.getCustomerType());
        assertEquals(0, found.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#3 - a duplicate ID is rejected and the file is unmodified
    // ------------------------------------------------------------------
    @Test
    public void TC3_duplicateCustomerIDIsRejected() {
        int before = countLines();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> adder.registerCustomer("C001", "Ahmad Bin Ali", "ahmad@gmail.com", "0123334444", customer.TYPE_OTHER));

        assertEquals("Customer ID already exists: C001", e.getMessage());
        assertEquals(before, countLines());
    }

    // ------------------------------------------------------------------
    // TC#4 - an empty name is rejected by the customer constructor
    // ------------------------------------------------------------------
    @Test
    public void TC4_emptyCustomerNameIsRejected() {
        int before = countLines();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> adder.registerCustomer("C007", "", "x@gmail.com",
                                       "0123456789", customer.TYPE_OTHER));

        assertEquals("Customer name must not be null or empty.", e.getMessage());
        assertEquals(before, countLines());
    }

    // ------------------------------------------------------------------
    // TC#5 - EP invalid: a name containing digits or symbols
    // ------------------------------------------------------------------
    @Test
    public void TC5_nameContainingDigitsIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> adder.registerCustomer("C007", "Ali123", "ali@gmail.com",
                                       "0123456789", customer.TYPE_OTHER));

        assertEquals("Invalid customer name: Ali123", e.getMessage());
        assertFalse(adder.isValidName("Ali123"));
        assertTrue(adder.isValidName("Ali Bin Ahmad"));
    }

    // ------------------------------------------------------------------
    // TC#6 to TC#9 - BVA on the phone number length (10 to 11 digits)
    // ------------------------------------------------------------------
    public Object[] phoneLengthBoundaries() {
        return new Object[] {
            new Object[] { "012345678",    false },  // TC#6  9 digits
            new Object[] { "0123456789",   true  },  // TC#7  10 digits
            new Object[] { "01123456789",  true  },  // TC#8  11 digits
            new Object[] { "012345678901", false }   // TC#9  12 digits
        };
    }

    @Test
    @Parameters(method = "phoneLengthBoundaries")
    public void TC6_TC9_phoneNumberLengthBoundaries(String phone, boolean shouldBeAccepted) {
        assertEquals(shouldBeAccepted, adder.isValidPhone(phone));

        if (shouldBeAccepted) {
            assertTrue(adder.addCustomer(new customer("C007", "Ali Bin Ahmad", "ali@gmail.com", phone, customer.TYPE_OTHER)));
            assertEquals(6, countLines());
        } else {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> adder.registerCustomer("C007", "Ali Bin Ahmad", "ali@gmail.com", 
            		phone, customer.TYPE_OTHER));
            assertEquals("Phone number must be 10 to 11 digits.", e.getMessage());
            assertEquals(5, countLines());
        }
    }

    // ------------------------------------------------------------------
    // TC#10 - EP invalid: a non numeric phone number (distinct message)
    // ------------------------------------------------------------------
    @Test
    public void TC10_nonNumericPhoneNumberIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> adder.registerCustomer("C007", "Ali Bin Ahmad", "ali@gmail.com",
                                       "01Y3456789", customer.TYPE_OTHER));
        assertEquals("Phone number must contain digits only.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#11 to TC#13 - EP on the email address
    // ------------------------------------------------------------------
    public Object[] emailPartitions() {
        return new Object[] {
            new Object[] { "ali.gmail.com",  false },  // TC#11 no "@"
            new Object[] { "ali@",           false },  // TC#12 no domain
            new Object[] { "ali@gmail.com",  true  }   // TC#13 valid local@domain.tld
        };
    }

    @Test
    @Parameters(method = "emailPartitions")
    public void TC11_TC13_emailAddressPartitions(String email, boolean shouldBeAccepted) {
        assertEquals(shouldBeAccepted, adder.isValidEmail(email));

        if (shouldBeAccepted) {
            assertTrue(adder.addCustomer(new customer("C007", "Ali Bin Ahmad",
                    email, "0123456789", customer.TYPE_OTHER)));
            assertEquals(6, countLines());
        } else {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> adder.registerCustomer("C007", "Ali Bin Ahmad", email,
                                           "0123456789", customer.TYPE_OTHER));
            assertEquals("Invalid email address: " + email, e.getMessage());
            assertEquals(5, countLines());
        }
    }

    // ------------------------------------------------------------------
    // TC#14 - the next sequential customer ID
    // ------------------------------------------------------------------
    @Test
    public void TC14_nextCustomerIDIsGenerated() {
        assertEquals("C006", adder.generateCustomerID());

        // with no file present, registration starts at C001
        addNewCustomer noFile = new addNewCustomer("no_such_file.txt");
        assertEquals("C001", noFile.generateCustomerID());
    }

    // ------------------------------------------------------------------
    // TC#15 - a rejected record leaves the file untouched
    // ------------------------------------------------------------------
    @Test
    public void TC15_fileIsUnchangedWhenARecordIsRejected() {
        int beforeCount = countLines();
        String beforeLast = lastLine();

        assertThrows(IllegalArgumentException.class,
            () -> adder.registerCustomer("C007", "Ali Bin Ahmad", "ali@gmail.com",
                                       "012345678", customer.TYPE_OTHER));

        assertEquals(beforeCount, countLines());
        assertEquals(beforeLast, lastLine());
    }

    // ------------------------------------------------------------------
    // TC#16 - the duplicate check driven by a MOCK of readCustomer
    // ------------------------------------------------------------------
 
    @Test
    public void TC16_duplicateCheckUsesTheInjectedReader() {
        readCustomer mockReader = mock(readCustomer.class);
        when(mockReader.customerExists("C001")).thenReturn(true);

        addNewCustomer withMock = new addNewCustomer(path, mockReader);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> withMock.addCustomer(new customer("C001", "Ahmad Bin Ali",
                    "ahmad@gmail.com", "0123334444", customer.TYPE_OTHER)));

        assertEquals("Customer ID already exists: C001", e.getMessage());
        verify(mockReader, times(1)).customerExists("C001");
    }

    // ------------------------------------------------------------------
    // local helpers
    // ------------------------------------------------------------------
    private int countLines() {
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

    private String lastLine() {
        String last = null;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) last = line.trim();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + path, e);
        }
        return last;
    }
}