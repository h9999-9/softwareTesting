package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class generateInvoiceTest {

    private static final double DELTA = 0.001;

    private printerAvailability mockPrinter;
    private calculatePrintingCharge calc;
    private generateInvoice invoiceGenerator;

    private customer student;   
    private customer other;     

    private final PrintStream originalOut = System.out;

    @Before
    public void setUp() {
        mockPrinter = mock(printerAvailability.class);
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());
        invoiceGenerator = new generateInvoice();

        student = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                               "0123456789", customer.TYPE_STUDENT, 10);
        other   = new customer("C003", "Siti Aminah", "siti.aminah@gmail.com",
                               "0111234567", customer.TYPE_OTHER, 5);
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }

    /** The standard priced order used by TC#1 to TC#5 and TC#10. */
    private printOrder pricedStudentOrder() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);
        calc.calculateTotalCharge(order);
        return order;
    }

    // ------------------------------------------------------------------
    // TC#1 - DT#3 Rule 1: a complete order produces an invoice
    // ------------------------------------------------------------------
    @Test
    public void TC1_invoiceIsGeneratedForACompleteOrder() {
        String invoice = invoiceGenerator.buildInvoice(pricedStudentOrder());

        org.junit.Assert.assertNotNull(invoice);
        assertFalse(invoice.trim().isEmpty());
        assertTrue(invoice.contains("PRINTMASTER INVOICE"));
    }

    // ------------------------------------------------------------------
    // TC#2 - the customer details appear on the invoice
    // ------------------------------------------------------------------
    @Test
    public void TC2_invoiceContainsTheCustomerDetails() {
        String invoice = invoiceGenerator.buildInvoice(pricedStudentOrder());

        assertTrue(invoice.contains("C001"));
        assertTrue(invoice.contains("Ali Bin Ahmad"));
        assertTrue(invoice.contains("0123456789"));
        assertTrue(invoice.contains("ali@gmail.com"));
        assertTrue(invoice.contains("Student"));
    }

    // ------------------------------------------------------------------
    // TC#3 - the print order details appear on the invoice
    // ------------------------------------------------------------------
    @Test
    public void TC3_invoiceContainsThePrintOrderDetails() {
        String invoice = invoiceGenerator.buildInvoice(pricedStudentOrder());

        assertTrue(invoice.contains("A4"));
        assertTrue(invoice.contains("Colour"));
        assertTrue(invoice.contains("Single-sided"));
        assertTrue(invoice.contains("100"));           
        assertTrue(invoice.contains("500"));           
        assertTrue(invoice.contains("Comb Binding"));
    }

    // ------------------------------------------------------------------
    // TC#4 - the charge breakdown appears and balances
    // ------------------------------------------------------------------
    @Test
    public void TC4_invoiceContainsTheChargeBreakdown() {
        printOrder order = pricedStudentOrder();
        String invoice = invoiceGenerator.buildInvoice(order);

        assertTrue(invoice.contains("400.00"));   
        assertTrue(invoice.contains("5.00"));     
        assertTrue(invoice.contains("405.00"));   
        assertTrue(invoice.contains("58.72"));    
        assertTrue(invoice.contains("346.28"));  

        // the printed breakdown must balance
        assertEquals(order.getTotalCharge(),
                     order.getBaseCharge() + order.getOptionalServiceCharge()
                         - order.getDiscountAmount(),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#5 - no transcription error between the calculation and the invoice
    // ------------------------------------------------------------------
    @Test
    public void TC5_invoiceTotalMatchesTheCalculatedTotal() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);
        double calculated = calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertEquals(calculated, order.getTotalCharge(), DELTA);
        assertTrue(invoice.contains(String.format("%.2f", calculated)));
    }

    // ------------------------------------------------------------------
    // TC#6 - DT#3 Rule 3: the charges have not been calculated
    // ------------------------------------------------------------------
    @Test
    public void TC6_noInvoiceWhenTheChargesAreMissing() {
        printOrder unpriced = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                     100, 5, "Comb", false, false);
        assertFalse(unpriced.isChargesCalculated());

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> invoiceGenerator.buildInvoice(unpriced));
        assertEquals("Printing charge has not been calculated.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#7 - DT#3 Rule 7: the printer was unavailable, so nothing was priced
    // ------------------------------------------------------------------
    @Test
    public void TC7_noInvoiceWhenThePrinterWasUnavailable() {
        printerAvailability unavailable = mock(printerAvailability.class);
        when(unavailable.isPrinterAvailable(anyString(), anyString())).thenReturn(false);
        calculatePrintingCharge failing =
            new calculatePrintingCharge(unavailable, new applyDiscount());

        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);

        assertThrows(IllegalStateException.class, () -> failing.calculateTotalCharge(order));

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> invoiceGenerator.buildInvoice(order));
        assertEquals("Printing charge has not been calculated.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#8 - DT#3 Rule 8: a null print order
    // ------------------------------------------------------------------
    @Test
    public void TC8_noInvoiceForANullPrintOrder() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> invoiceGenerator.buildInvoice(null));
        assertEquals("Print order must not be null.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#9 - a zero discount is displayed, not omitted
    // ------------------------------------------------------------------
    @Test
    public void TC9_invoiceShowsAZeroDiscountWhenNoneApplies() {
        printOrder order = printOrder.createOrder(other, "Black & White", "A5",
                                                  "Single-sided", 10, 1, "None", false, false);
        calc.calculateTotalCharge(order);
        String invoice = invoiceGenerator.buildInvoice(order);

        assertEquals(0.00, order.getDiscountAmount(), DELTA);
        assertTrue(invoice.contains("Discount Applied"));
        assertTrue(invoice.contains("0.00"));
        assertTrue(invoice.contains("1.50"));
        assertTrue(invoice.contains("None selected"));
    }

    // ------------------------------------------------------------------
    // TC#10 - displayInvoice() prints exactly what generateInvoice() returns
    // ------------------------------------------------------------------
    @Test
    public void TC10_displayInvoicePrintsTheGeneratedInvoice() {
        printOrder order = pricedStudentOrder();
        String expected = invoiceGenerator.buildInvoice(order);

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        invoiceGenerator.displayInvoice(order);
        System.setOut(originalOut);

        assertEquals(expected, captured.toString());
    }
}