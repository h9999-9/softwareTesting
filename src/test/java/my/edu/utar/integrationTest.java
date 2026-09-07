package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class integrationTest {

    private static final double DELTA = 0.001;

    private static final String TESTDATA_FILE = "testdata.txt";

    private static final String BASELINE =
        "C001,Ali Bin Ahmad,ali@gmail.com,0123456789,Student,10\n"
      + "C002,Tan Wei Ming,tanwm@printcorp.com,0129876543,Corporate,25\n"
      + "C003,Siti Aminah,siti.aminah@gmail.com,0111234567,Other,5\n"
      + "C004,Rajesh Kumar,rajesh@yahoo.com,01123456789,Student,25\n"
      + "C005,Lim Mei Ling,meiling@hotmail.com,0198765432,Corporate,3\n";

    private Path tempFile;
    private String path;
    private printerAvailability mockPrinter;
    private calculatePrintingCharge calc;
    private generateInvoice invoiceGenerator;
    private readCustomer reader;

    @Before
    public void setUp() throws IOException {
        tempFile = Files.createTempFile("integration-customer", ".txt");
        Files.write(tempFile, BASELINE.getBytes(StandardCharsets.UTF_8));
        path = tempFile.toString();

        mockPrinter = mock(printerAvailability.class);
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());
        invoiceGenerator = new generateInvoice();
        reader = new readCustomer(path);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    // ------------------------------------------------------------------
    // TC#1 - end to end for an existing customer
    // ------------------------------------------------------------------
    @Test
    public void TC1_endToEndFlowForAnExistingCustomer() {
        customer c = reader.findCustomer("C001");                 
        printOrder order = printOrder.createOrder(c, "Colour", "A4", "Single-sided", 100, 5, "Comb", false, false);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertEquals(400.00, order.getBaseCharge(), DELTA);
        assertEquals(5.00,   order.getOptionalServiceCharge(), DELTA);
        assertEquals(405.00, order.getSubtotal(), DELTA);
        assertEquals(58.72,  order.getDiscountAmount(), DELTA);
        assertEquals(346.28, order.getTotalCharge(), DELTA);

        assertTrue(invoice.contains("400.00"));
        assertTrue(invoice.contains("346.28"));
    }

    // ------------------------------------------------------------------
    // TC#2 - end to end for a newly registered customer
    // ------------------------------------------------------------------
    @Test
    public void TC2_endToEndFlowForANewlyRegisteredCustomer() {
        addNewCustomer adder = new addNewCustomer(path);
        adder.registerCustomer("C006", "Siti Nurhaliza", "siti@gmail.com",
                             "0198765432", customer.TYPE_OTHER);

        customer c = reader.findCustomer("C006");
        assertEquals(0, c.getPreviousOrders());

        printOrder order = printOrder.createOrder(c, "Black & White", "A5", "Double-sided",
                                                  50, 5, "None", false, false);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertEquals(32.50, order.getBaseCharge(), DELTA);   // 250 printed pages x RM0.13
        assertEquals(0.00,  order.getDiscountAmount(), DELTA);
        assertEquals(32.50, order.getTotalCharge(), DELTA);
        assertTrue(invoice.contains("32.50"));
    }

    // ------------------------------------------------------------------
    // TC#3 - the discount component is genuinely integrated
    // ------------------------------------------------------------------
    @Test
    public void TC3_discountComponentIsCorrectlyIntegrated() {
        customer c = reader.findCustomer("C002");             
        printOrder order = printOrder.createOrder(c, "Colour", "A3", "Double-sided", 100, 2, "Spiral", true, true);
        calc.calculateTotalCharge(order);

        double exactDiscount = new applyDiscount()
                .calculateDiscount(c.getCustomerType(), 608.00, c.getPreviousOrders());

        assertEquals(608.00, order.getSubtotal(), DELTA);
        assertEquals(141.588, exactDiscount, DELTA);            
        assertEquals(141.59, order.getDiscountAmount(), DELTA); 
        assertEquals(466.41, order.getTotalCharge(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#4 - printer unavailability stops the whole chain
    // ------------------------------------------------------------------
    @Test
    public void TC4_printerUnavailabilityPropagatesThroughTheFlow() {
        printerAvailability unavailable = mock(printerAvailability.class);
        when(unavailable.isPrinterAvailable(anyString(), anyString())).thenReturn(false);
        calculatePrintingCharge failing =
            new calculatePrintingCharge(unavailable, new applyDiscount());

        customer c = reader.findCustomer("C001");
        printOrder order = printOrder.createOrder(c, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);

        IllegalStateException fromCalculation = assertThrows(IllegalStateException.class,
            () -> failing.calculateTotalCharge(order));
        assertEquals("Selected printer is currently unavailable.", fromCalculation.getMessage());
        assertFalse(order.isChargesCalculated());

        IllegalStateException fromInvoice = assertThrows(IllegalStateException.class,
            () -> invoiceGenerator.buildInvoice(order));
        assertEquals("Printing charge has not been calculated.", fromInvoice.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#5 - every figure on the invoice comes from the order object
    // ------------------------------------------------------------------
    @Test
    public void TC5_invoiceFiguresMatchTheStoredOrderValues() {
        customer c = reader.findCustomer("C002");
        printOrder order = printOrder.createOrder(c, "Colour", "A3", "Double-sided", 100, 2, "Spiral", true, true);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertTrue(invoice.contains(String.format("%.2f", order.getBaseCharge())));
        assertTrue(invoice.contains(String.format("%.2f", order.getOptionalServiceCharge())));
        assertTrue(invoice.contains(String.format("%.2f", order.getDiscountAmount())));
        assertTrue(invoice.contains(String.format("%.2f", order.getTotalCharge())));

        assertEquals(280.00, order.getBaseCharge(), DELTA);
        assertEquals(328.00, order.getOptionalServiceCharge(), DELTA);
        assertEquals(141.59, order.getDiscountAmount(), DELTA);
        assertEquals(466.41, order.getTotalCharge(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#6 - a successful payment completes the order and sends the email
    // ------------------------------------------------------------------
    @Test
    public void TC6_orderStatusAfterASuccessfulPayment() {
        customer c = reader.findCustomer("C001");
        printOrder order = printOrder.createOrder(c, "Colour", "A4", "Single-sided", 100, 5, "Comb", false, false);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        payment mockPayment = mock(payment.class);
        emailInvoice mockEmail = mock(emailInvoice.class);
        when(mockPayment.processPayment(payment.METHOD_EWALLET, order.getTotalCharge()))
            .thenReturn(true);
        when(mockEmail.sendInvoiceEmail(anyString(), anyString())).thenReturn(true);

        boolean paid = mockPayment.processPayment(payment.METHOD_EWALLET, order.getTotalCharge());
        order.updateStatusAfterPayment(paid);
        if (paid) {
            mockEmail.sendInvoiceEmail(c.getEmail(), invoice);
        }

        assertEquals("Completed", order.getOrderStatus());
        assertEquals("Paid", order.getPaymentStatus());
        verify(mockPayment, times(1)).processPayment(payment.METHOD_EWALLET, 346.28);
        verify(mockEmail, times(1)).sendInvoiceEmail("ali@gmail.com", invoice);
    }

    // ------------------------------------------------------------------
    // TC#7 - a failed payment leaves the order pending and sends no email
    // ------------------------------------------------------------------
    @Test
    public void TC7_orderStatusAfterAnUnsuccessfulPayment() {
        customer c = reader.findCustomer("C001");
        printOrder order = printOrder.createOrder(c, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        payment mockPayment = mock(payment.class);
        emailInvoice mockEmail = mock(emailInvoice.class);
        when(mockPayment.processPayment(anyString(),
             org.mockito.ArgumentMatchers.anyDouble())).thenReturn(false);

        boolean paid = mockPayment.processPayment(payment.METHOD_CREDIT_CARD,
                                                  order.getTotalCharge());
        order.updateStatusAfterPayment(paid);
        if (paid) {
            mockEmail.sendInvoiceEmail(c.getEmail(), invoice);
        }

        assertEquals("Pending Payment", order.getOrderStatus());
        assertEquals("Unpaid", order.getPaymentStatus());
        verify(mockEmail, never()).sendInvoiceEmail(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#8 - the whole flow driven by EXTERNAL TEST DATA
    // ------------------------------------------------------------------
    public Object[] integrationDataFromFile() {
        List<Object> sets = new ArrayList<Object>();
        try (BufferedReader br = new BufferedReader(new FileReader(TESTDATA_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] f = line.split(",");
                sets.add(new Object[] {
                    f[0].trim(),                          // customerID
                    f[1].trim(),                          // paperSize
                    f[2].trim(),                          // printType
                    f[3].trim(),                          // printingSide
                    Integer.parseInt(f[4].trim()),        // pages
                    Integer.parseInt(f[5].trim()),        // copies
                    f[6].trim(),                          // binding
                    Boolean.parseBoolean(f[7].trim()),    // lamination
                    Boolean.parseBoolean(f[8].trim()),    // express
                    Double.parseDouble(f[9].trim())       // expected total
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + TESTDATA_FILE, e);
        }
        return sets.toArray();
    }

    @Test
    @Parameters(method = "integrationDataFromFile")
    public void TC8_integrationFlowDrivenByExternalTestData(String customerID, String paperSize,
                                                            String printType, String printingSide,
                                                            int pages, int copies, String binding,
                                                            boolean lamination, boolean express,
                                                            double expectedTotal) {
        customer c = reader.findCustomer(customerID);
        printOrder order = printOrder.createOrder(c, printType, paperSize, printingSide,
                                                  pages, copies, binding, lamination, express);
        double total = calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertEquals("Total for " + customerID, expectedTotal, total, DELTA);
        assertTrue(invoice.contains(String.format("%.2f", expectedTotal)));
    }

    // ------------------------------------------------------------------
    // TC#9 - an invalid registration stops the whole flow
    // ------------------------------------------------------------------
    @Test
    public void TC9_invalidRegistrationStopsTheWholeFlow() {
        addNewCustomer adder = new addNewCustomer(path);
        int before = countLines();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> adder.registerCustomer("C007", "Ali Bin Ahmad", "ali@gmail.com",
                                       "012345678", customer.TYPE_OTHER));
        assertEquals("Phone number must be 10 to 11 digits.", e.getMessage());

        // nothing was written, so the customer cannot be retrieved and no order follows
        assertEquals(before, countLines());
        assertFalse(reader.customerExists("C007"));
        assertThrows(NoSuchElementException.class, () -> reader.findCustomer("C007"));
    }

    // ------------------------------------------------------------------
    // Local helper
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
}