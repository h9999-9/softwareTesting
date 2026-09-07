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
import org.mockito.ArgumentCaptor;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class printerAvailabilityTest {

    private static final double DELTA = 0.001;

    private printerAvailability mockPrinter;
    private calculatePrintingCharge calc;
    private customer other;

    @Before
    public void setUp() {
        mockPrinter = mock(printerAvailability.class);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());
        other = new customer("C003", "Siti Aminah", "siti.aminah@gmail.com",
                             "0111234567", customer.TYPE_OTHER, 5);
    }

    // ------------------------------------------------------------------
    // TC#1 - the stub reports an available printer
    // ------------------------------------------------------------------
    @Test
    public void TC1_orderProceedsWhenTheMockReturnsTrue() {
        when(mockPrinter.isPrinterAvailable("A4", "Colour")).thenReturn(true);

        printOrder order = printOrder.createOrder(other, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);

        assertEquals(8.00, calc.calculateTotalCharge(order), DELTA);
        assertEquals(8.00, order.getBaseCharge(), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#2 - the stub reports no available printer
    // ------------------------------------------------------------------
    @Test
    public void TC2_orderTerminatesWhenTheMockReturnsFalse() {
        when(mockPrinter.isPrinterAvailable("A3", "Colour")).thenReturn(false);

        printOrder order = printOrder.createOrder(other, "Colour", "A3", "Single-sided",
                                                  10, 1, "None", false, false);

        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> calc.calculateTotalCharge(order));

        assertEquals("Selected printer is currently unavailable.", e.getMessage());
        assertFalse(order.isChargesCalculated());
    }

    // ------------------------------------------------------------------
    // TC#3 - the module is not polled repeatedly
    // ------------------------------------------------------------------
    @Test
    public void TC3_moduleIsCalledExactlyOncePerOrder() {
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);

        printOrder order = printOrder.createOrder(other, "Black & White", "A4",
                                                  "Single-sided", 10, 1, "None", false, false);
        calc.calculateTotalCharge(order);

        verify(mockPrinter, times(1)).isPrinterAvailable(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#4 - the arguments are forwarded unchanged
    // ------------------------------------------------------------------
    @Test
    public void TC4_argumentsPassedToTheMockMatchTheOrder() {
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);

        printOrder order = printOrder.createOrder(other, "Black & White", "A5",
                                                  "Single-sided", 10, 1, "None", false, false);
        calc.calculateTotalCharge(order);

        ArgumentCaptor<String> paperSize = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> printType = ArgumentCaptor.forClass(String.class);
        verify(mockPrinter).isPrinterAvailable(paperSize.capture(), printType.capture());

        assertEquals("A5", paperSize.getValue());
        assertEquals("Black & White", printType.getValue());
    }

    // ------------------------------------------------------------------
    // TC#5 - every paper size and print type pair, stubbed both ways
    // ------------------------------------------------------------------
    public Object[] paperSizeAndPrintTypePairs() {
        return new Object[] {
            new Object[] { "A3", "Black & White" },
            new Object[] { "A3", "Colour"        },
            new Object[] { "A4", "Black & White" },
            new Object[] { "A4", "Colour"        },
            new Object[] { "A5", "Black & White" },
            new Object[] { "A5", "Colour"        }
        };
    }
    
    @Test
    @Parameters(method = "paperSizeAndPrintTypePairs")
    public void TC5_stubbingAcrossEveryPaperSizeAndPrintTypePair(String paperSize,
                                                                 String printType) {
        // stubbed available: the calculation proceeds
        when(mockPrinter.isPrinterAvailable(paperSize, printType)).thenReturn(true);
        printOrder available = printOrder.createOrder(other, printType, paperSize,
                                                      "Single-sided", 10, 1, "None", false, false);
        calc.calculateTotalCharge(available);
        org.junit.Assert.assertTrue(available.isChargesCalculated());

        // stubbed unavailable: the order terminates
        when(mockPrinter.isPrinterAvailable(paperSize, printType)).thenReturn(false);
        printOrder unavailable = printOrder.createOrder(other, printType, paperSize,
                                                        "Single-sided", 10, 1, "None", false, false);
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> calc.calculateTotalCharge(unavailable));
        assertEquals("Selected printer is currently unavailable.", e.getMessage());
        assertFalse(unavailable.isChargesCalculated());
    }

    // ------------------------------------------------------------------
    // TC#6 - validation happens before the external module is contacted
    // ------------------------------------------------------------------
    @Test
    public void TC6_nullPaperSizeIsRejectedBeforeTheMockIsCalled() {
        printOrder mockOrder = mock(printOrder.class);
        when(mockOrder.getCustomerDetails()).thenReturn(other);
        when(mockOrder.getNumberOfPages()).thenReturn(10);
        when(mockOrder.getNumberOfCopies()).thenReturn(1);
        when(mockOrder.getPaperSize()).thenReturn(null);          // invalid
        when(mockOrder.getPrintType()).thenReturn("Colour");
        when(mockOrder.getPrintingSide()).thenReturn("Single-sided");
        when(mockOrder.getBindingOption()).thenReturn("None");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateTotalCharge(mockOrder));

        assertEquals("Paper size must be selected.", e.getMessage());
        verify(mockPrinter, never()).isPrinterAvailable(anyString(), anyString());
    }

    // ------------------------------------------------------------------
    // TC#7 - a failure of the external module is not swallowed
    // ------------------------------------------------------------------
    @Test
    public void TC7_exceptionFromTheExternalModulePropagates() {
        when(mockPrinter.isPrinterAvailable(anyString(), anyString()))
            .thenThrow(new RuntimeException("Service unavailable"));

        printOrder order = printOrder.createOrder(other, "Colour", "A4", "Single-sided",
                                                  10, 1, "None", false, false);

        RuntimeException e = assertThrows(RuntimeException.class,
            () -> calc.calculateTotalCharge(order));

        assertEquals("Service unavailable", e.getMessage());
        assertEquals(0.00, order.getTotalCharge(), DELTA);
        assertFalse(order.isChargesCalculated());
    }
}