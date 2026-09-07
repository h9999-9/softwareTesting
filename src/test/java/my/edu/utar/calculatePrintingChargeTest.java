package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;


@RunWith(JUnitParamsRunner.class)
public class calculatePrintingChargeTest {

    private static final double DELTA = 0.001;

    private printerAvailability mockPrinter;
    private calculatePrintingCharge calc;

    private customer student;     // C001, Student, 10 previous orders
    private customer corporate;   // C002, Corporate, 25 previous orders
    private customer other;       // C003, Other, 5 previous orders

    @Before
    public void setUp() {
        mockPrinter = mock(printerAvailability.class);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());

        student   = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                                 "0123456789", customer.TYPE_STUDENT, 10);
        corporate = new customer("C002", "Tan Wei Ming", "tanwm@printcorp.com",
                                 "0129876543", customer.TYPE_CORPORATE, 25);
        other     = new customer("C003", "Siti Aminah", "siti.aminah@gmail.com",
                                 "0111234567", customer.TYPE_OTHER, 5);
    }

    // ------------------------------------------------------------------
    // TC#1 - DT#1 Rule 1: the complete flow for an existing student
    // ------------------------------------------------------------------
    @Test
    public void TC1_fullCalculationForAnExistingStudent() {
        when(mockPrinter.isPrinterAvailable("A4", "Colour")).thenReturn(true);

        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);
        double total = calc.calculateTotalCharge(order);

        assertEquals(346.28, total, DELTA);
        assertEquals(400.00, order.getBaseCharge(), DELTA);
        assertEquals(5.00,   order.getOptionalServiceCharge(), DELTA);
        assertEquals(405.00, order.getSubtotal(), DELTA);
        assertEquals(58.72,  order.getDiscountAmount(), DELTA);
        assertEquals(346.28, order.getTotalCharge(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#2 - DT#1 Rule 2: a newly registered customer receives no discount
    // ------------------------------------------------------------------
    @Test
    public void TC2_flowForANewlyRegisteredCustomer() {
        when(mockPrinter.isPrinterAvailable("A4", "Black & White")).thenReturn(true);

        customer newCustomer = new customer("C006", "Siti Nurhaliza", "siti@gmail.com",
                                            "0198765432", customer.TYPE_OTHER);
        printOrder order = printOrder.createOrder(newCustomer, "Black & White", "A4",
                                                  "Single-sided", 10, 1, "None", false, false);

        assertEquals(2.00, calc.calculateTotalCharge(order), DELTA);
        assertEquals(0.00, order.getDiscountAmount(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#3 - DT#1 Rule 7: no optional services still completes
    // ------------------------------------------------------------------
    @Test
    public void TC3_orderWithNoOptionalServicesCompletes() {
        when(mockPrinter.isPrinterAvailable("A5", "Black & White")).thenReturn(true);

        printOrder order = printOrder.createOrder(other, "Black & White", "A5",
                                                  "Single-sided", 10, 1, "None", false, false);

        assertEquals(1.50, calc.calculateTotalCharge(order), DELTA);
        assertEquals(0.00, order.getOptionalServiceCharge(), DELTA);
        assertEquals(true, order.isChargesCalculated());
    }

    // ------------------------------------------------------------------
    // TC#4 - the maximum charge, all optional services and all discounts
    // ------------------------------------------------------------------
    @Test
    public void TC4_maximumChargeWithAllOptionalServices() {
        when(mockPrinter.isPrinterAvailable("A3", "Colour")).thenReturn(true);

        printOrder order = printOrder.createOrder(corporate, "Colour", "A3", "Double-sided",
                                                  100, 2, "Spiral", true, true);
        double total = calc.calculateTotalCharge(order);

        assertEquals(280.00, order.getBaseCharge(), DELTA);
        assertEquals(328.00, order.getOptionalServiceCharge(), DELTA);
        assertEquals(608.00, order.getSubtotal(), DELTA);
        assertEquals(141.59, order.getDiscountAmount(), DELTA);
        assertEquals(466.41, total, DELTA);
    }

    // ------------------------------------------------------------------
    // TC#5 - DT#1 Rule 8 / Appendix A: the printer is unavailable
    // ------------------------------------------------------------------
    @Test
    public void TC5_orderTerminatesWhenThePrinterIsUnavailable() {
        when(mockPrinter.isPrinterAvailable("A3", "Colour")).thenReturn(false);

        printOrder order = printOrder.createOrder(student, "Colour", "A3", "Single-sided",
                                                  10, 1, "None", false, false);

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> calc.calculateTotalCharge(order));

        assertEquals("Selected printer is currently unavailable.", e.getMessage());
        assertFalse(order.isChargesCalculated());
    }

    // ------------------------------------------------------------------
    // TC#6 - the module is called once, with the arguments unchanged
    // ------------------------------------------------------------------
    @Test
    public void TC6_printerModuleCalledOnceWithCorrectArguments() {
        when(mockPrinter.isPrinterAvailable("A4", "Colour")).thenReturn(true);

        printOrder order = printOrder.createOrder(other, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);
        calc.calculateTotalCharge(order);

        verify(mockPrinter, times(1)).isPrinterAvailable("A4", "Colour");
    }

    // ------------------------------------------------------------------
    // TC#7 - nothing is stored on the order when the printer is unavailable
    // ------------------------------------------------------------------
    @Test
    public void TC7_noChargeIsStoredWhenThePrinterIsUnavailable() {
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(false);

        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);

        assertThrows(IllegalStateException.class, () -> calc.calculateTotalCharge(order));

        assertEquals(0.00, order.getTotalCharge(), DELTA);
        assertEquals(0.00, order.getBaseCharge(), DELTA);
        assertFalse(order.isChargesCalculated());
    }

    // ------------------------------------------------------------------
    // TC#8 - the module is NOT called when validation fails
    // ------------------------------------------------------------------
    @Test
    public void TC8_printerModuleIsNotCalledWhenValidationFails() {
        printOrder mockOrder = mock(printOrder.class);
        when(mockOrder.getCustomerDetails()).thenReturn(student);
        when(mockOrder.getNumberOfPages()).thenReturn(0);      
        when(mockOrder.getNumberOfCopies()).thenReturn(1);
        when(mockOrder.getPaperSize()).thenReturn("A4");
        when(mockOrder.getPrintType()).thenReturn("Colour");
        when(mockOrder.getPrintingSide()).thenReturn("Single-sided");
        when(mockOrder.getBindingOption()).thenReturn("None");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateTotalCharge(mockOrder));

        assertEquals("Pages must be between 1 and 500.", e.getMessage());
        verify(mockPrinter, never()).isPrinterAvailable(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#9 - DT#1 Rule 3: a missing print type
    // ------------------------------------------------------------------
    @Test
    public void TC9_missingPrintTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", null, "Single-sided", 10, 1));
        assertEquals("Print type must be selected.", e.getMessage());
        verify(mockPrinter, never()).isPrinterAvailable(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#10 - DT#1 Rule 4: a missing paper size
    // ------------------------------------------------------------------
    @Test
    public void TC10_missingPaperSizeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge(null, "Colour", "Single-sided", 10, 1));
        assertEquals("Paper size must be selected.", e.getMessage());
        verify(mockPrinter, never()).isPrinterAvailable(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#11 - DT#1 Rule 5: an out of range page count
    // ------------------------------------------------------------------
    @Test
    public void TC11_invalidPagesOrCopiesIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Colour", "Single-sided", 501, 1));
        assertEquals("Pages must be between 1 and 500.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#12 - DT#1 Rule 6: a missing printing side
    // ------------------------------------------------------------------
    @Test
    public void TC12_missingPrintingSideIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Colour", null, 10, 1));
        assertEquals("Printing side must be selected.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#13 - Business Rule 5: the final total is rounded to two decimals
    // ------------------------------------------------------------------
    @Test
    public void TC13_finalTotalIsRoundedToTwoDecimalPlaces() {
        when(mockPrinter.isPrinterAvailable("A4", "Colour")).thenReturn(true);

        printOrder order = printOrder.createOrder(student, "Colour", "A4", "Single-sided",
                                                  100, 5, "Comb", false, false);
        double total = calc.calculateTotalCharge(order);

        assertEquals(346.28, total, DELTA);
        // the printed breakdown must balance
        assertEquals(order.getTotalCharge(),
                     order.getBaseCharge() + order.getOptionalServiceCharge()
                         - order.getDiscountAmount(),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#14 - the constructor rejects a null dependency
    // ------------------------------------------------------------------
    @Test
    public void TC14_constructorRejectsNullDependencies() {
        assertEquals("Printer availability service must not be null.",
            assertThrows(IllegalArgumentException.class,
                () -> new calculatePrintingCharge(null, new applyDiscount())).getMessage());

        assertEquals("Discount calculator must not be null.",
            assertThrows(IllegalArgumentException.class,
                () -> new calculatePrintingCharge(mockPrinter, null)).getMessage());
    }
}