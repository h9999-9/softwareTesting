package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class printOrderTest {

    private static final double DELTA = 0.001;

    private customer student;      // C001, Student, 10 previous orders
    private customer other;        // C003, Other,   5 previous orders

    @Before
    public void setUp() {
        student = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                               "0123456789", customer.TYPE_STUDENT, 10);
        other   = new customer("C003", "Siti Aminah", "siti.aminah@gmail.com",
                               "0111234567", customer.TYPE_OTHER, 5);
    }

    private calculatePrintingCharge calculatorWithAvailablePrinter() {
        printerAvailability mockPrinter = mock(printerAvailability.class);
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
        return new calculatePrintingCharge(mockPrinter, new applyDiscount());
    }

    // ------------------------------------------------------------------
    // TC#1 - every attribute is stored
    // ------------------------------------------------------------------
    @Test
    public void TC1_validPrintOrderStoresEveryAttribute() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);

        assertEquals(student, order.getCustomerDetails());
        assertEquals("Colour", order.getPrintType());
        assertEquals("A4", order.getPaperSize());
        assertEquals("Single-sided", order.getPrintingSide());
        assertEquals(100, order.getNumberOfPages());
        assertEquals(5, order.getNumberOfCopies());
        assertEquals("Comb", order.getBindingOption());
        assertFalse(order.isLaminationOption());
        assertFalse(order.isExpressPrinting());

        String details = order.getOrderDetails();
        assertTrue(details.contains("C001"));
        assertTrue(details.contains("A4"));
        assertTrue(details.contains("Colour"));
        assertTrue(details.contains("Comb"));
    }

    // ------------------------------------------------------------------
    // TC#2 - default statuses
    // ------------------------------------------------------------------
    @Test
    public void TC2_defaultOrderAndPaymentStatus() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);
        assertEquals("Pending Payment", order.getOrderStatus());
        assertEquals("Unpaid", order.getPaymentStatus());
    }

    // ------------------------------------------------------------------
    // TC#3 - DT#1 Rule 7: no optional services is a valid selection
    // ------------------------------------------------------------------
    @Test
    public void TC3_orderWithNoOptionalServicesIsCreated() {
        printOrder order = printOrder.createOrder(student, "Black & White", "A5",
                                                  "Single-sided", 10, 1, "None", false, false);
        assertEquals("None", order.getBindingOption());
        assertFalse(order.isLaminationOption());
        assertFalse(order.isExpressPrinting());
    }

    // ------------------------------------------------------------------
    // TC#4 - the calculated breakdown is written back to the order
    // ------------------------------------------------------------------
    @Test
    public void TC4_calculatedChargesAreStoredOnTheOrder() {
        printOrder order = printOrder.createOrder(other, "Black & White", "A4",
                                                  "Single-sided", 10, 2, "None", false, false);
        calculatorWithAvailablePrinter().calculateTotalCharge(order);

        assertEquals(4.00, order.getBaseCharge(), DELTA);
        assertEquals(0.00, order.getOptionalServiceCharge(), DELTA);
        assertEquals(0.00, order.getDiscountAmount(), DELTA);
        assertEquals(4.00, order.getTotalCharge(), DELTA);
        assertEquals(4.00, order.getSubtotal(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#5 - DT#1 Rule 5: invalid page count
    // ------------------------------------------------------------------
    @Test
    public void TC5_invalidPageCountIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                         0, 1, "None", false, false));
        assertEquals("Pages must be between 1 and 500.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#6 - DT#1 Rule 5: invalid copy count
    // ------------------------------------------------------------------
    @Test
    public void TC6_invalidCopyCountIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                         1, 1001, "None", false, false));
        assertEquals("Copies must be between 1 and 1000.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#7 - Business Rule 9: only one binding option
    // ------------------------------------------------------------------
    @Test
    public void TC7_moreThanOneBindingOptionIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                         10, 1, "Comb, Spiral", false, false));
        assertEquals("Only one binding option may be selected.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#8 to TC#10 - DT#1 Rules 3, 4 and 6: a required selection is missing
    // ------------------------------------------------------------------
    public Object[] missingSelections() {
        return new Object[] {
            // printType, paperSize, printingSide, expected message
            new Object[] { null,     "A4",  "Single-sided", "Print type must be selected."    },
            new Object[] { "Colour", null,  "Single-sided", "Paper size must be selected."    },
            new Object[] { "Colour", "A4",  null,           "Printing side must be selected." }
        };
    }

    @Test
    @Parameters(method = "missingSelections")
    public void TC8_TC10_missingSelectionIsRejected(String printType, String paperSize,
                                                    String printingSide, String expectedMessage) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(student, printType, paperSize, printingSide,
                                         10, 1, "None", false, false));
        assertEquals(expectedMessage, e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#11 - a null customer
    // ------------------------------------------------------------------
    @Test
    public void TC11_nullCustomerIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(null, "Colour", "A4", "Single-sided",
                                         10, 1, "None", false, false));
        assertEquals("Customer must not be null.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#12 - status after a successful payment (STUB)
    // ------------------------------------------------------------------
    @Test
    public void TC12_statusAfterSuccessfulPayment() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);

        payment stubPayment = mock(payment.class);
        when(stubPayment.processPayment(payment.METHOD_EWALLET, 8.00)).thenReturn(true);

        boolean paid = stubPayment.processPayment(payment.METHOD_EWALLET, 8.00);
        order.updateStatusAfterPayment(paid);

        assertTrue(paid);
        assertEquals("Completed", order.getOrderStatus());
        assertEquals("Paid", order.getPaymentStatus());
    }

    // ------------------------------------------------------------------
    // TC#13 - Business Rule 10: total printed pages
    // ------------------------------------------------------------------
    @Test
    public void TC13_totalPrintedPagesIsPagesTimesCopies() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "None", false, false);
        assertEquals(500, order.getTotalPrintedPages());
    }

    // ------------------------------------------------------------------
    // TC#14 - EP invalid: unsupported paper size
    // ------------------------------------------------------------------
    @Test
    public void TC14_unsupportedPaperSizeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> printOrder.createOrder(student, "Black & White", "A6", "Single-sided",
                                         10, 1, "None", false, false));
        assertEquals("Invalid paper size: A6", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#15 - status after an unsuccessful payment (STUB)
    // ------------------------------------------------------------------
    @Test
    public void TC15_statusAfterUnsuccessfulPayment() {
        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);

        payment stubPayment = mock(payment.class);
        when(stubPayment.processPayment(anyString(), org.mockito.ArgumentMatchers.anyDouble()))
            .thenReturn(false);

        order.updateStatusAfterPayment(
            stubPayment.processPayment(payment.METHOD_CREDIT_CARD, 8.00));

        assertEquals("Pending Payment", order.getOrderStatus());
        assertEquals("Unpaid", order.getPaymentStatus());
    }

    // ------------------------------------------------------------------
    // TC#16 - the charges calculated flag
    // ------------------------------------------------------------------
    @Test
    public void TC16_chargesCalculatedFlag() {
        printOrder order = printOrder.createOrder(other, "Black & White", "A5",
                                                  "Single-sided", 10, 1, "None", false, false);
        assertFalse(order.isChargesCalculated());

        calculatorWithAvailablePrinter().calculateTotalCharge(order);
        assertTrue(order.isChargesCalculated());
    }
}