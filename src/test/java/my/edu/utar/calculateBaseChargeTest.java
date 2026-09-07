package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class calculateBaseChargeTest {

    private static final double DELTA = 0.001;

    private calculatePrintingCharge calc;

    @Before
    public void setUp() {
        printerAvailability mockPrinter = mock(printerAvailability.class);
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());
    }

    // ------------------------------------------------------------------
    // TC#1 - all twelve rate combinations of Table 2 (10 pages x 2 copies)
    // ------------------------------------------------------------------
    public Object[] tableTwoCombinations() {
        return new Object[] {
            new Object[] { "A4", "Black & White", "Single-sided",  4.00 },
            new Object[] { "A4", "Black & White", "Double-sided",  3.60 },
            new Object[] { "A4", "Colour",        "Single-sided", 16.00 },
            new Object[] { "A4", "Colour",        "Double-sided", 15.00 },
            new Object[] { "A3", "Black & White", "Single-sided",  8.00 },
            new Object[] { "A3", "Black & White", "Double-sided",  7.00 },
            new Object[] { "A3", "Colour",        "Single-sided", 30.00 },
            new Object[] { "A3", "Colour",        "Double-sided", 28.00 },
            new Object[] { "A5", "Black & White", "Single-sided",  3.00 },
            new Object[] { "A5", "Black & White", "Double-sided",  2.60 },
            new Object[] { "A5", "Colour",        "Single-sided", 12.00 },
            new Object[] { "A5", "Colour",        "Double-sided", 11.00 }
        };
    }

    @Test
    @Parameters(method = "tableTwoCombinations")
    public void TC1_baseChargeForEveryRateCombination(String paperSize, String printType,
                                                      String printingSide, double expected) {
        assertEquals(expected,
                     calc.calculateBaseCharge(paperSize, printType, printingSide, 10, 2),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#2 - BVA: minimum valid pages and copies
    // ------------------------------------------------------------------
    @Test
    public void TC2_bvaLowerValidBoundaryForPagesAndCopies() {
        assertEquals(0.20,
                     calc.calculateBaseCharge("A4", "Black & White", "Single-sided", 1, 1),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#3 - BVA: 0 pages, one below the minimum
    // ------------------------------------------------------------------
    @Test
    public void TC3_bvaLowerInvalidBoundaryForPages() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Colour", "Single-sided", 0, 1));
        assertEquals("Pages must be between 1 and 500.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#4 - BVA: 500 pages, the maximum
    // ------------------------------------------------------------------
    @Test
    public void TC4_bvaUpperValidBoundaryForPages() {
        assertEquals(100.00,
                     calc.calculateBaseCharge("A4", "Black & White", "Single-sided", 500, 1),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#5 - BVA: 501 pages, one above the maximum
    // ------------------------------------------------------------------
    @Test
    public void TC5_bvaUpperInvalidBoundaryForPages() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Black & White", "Single-sided", 501, 1));
        assertEquals("Pages must be between 1 and 500.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#6 - BVA: 0 copies, one below the minimum
    // ------------------------------------------------------------------
    @Test
    public void TC6_bvaLowerInvalidBoundaryForCopies() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Black & White", "Single-sided", 10, 0));
        assertEquals("Copies must be between 1 and 1000.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#7 - BVA: 1000 copies, the maximum
    // ------------------------------------------------------------------
    public void TC7_bvaUpperValidBoundaryForCopies() {
        assertEquals(200.00,
                     calc.calculateBaseCharge("A4", "Black & White", "Single-sided", 1, 1000),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#8 - BVA: 1001 copies, one above the maximum
    // ------------------------------------------------------------------
    @Test
    public void TC8_bvaUpperInvalidBoundaryForCopies() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A3", "Black & White", "Single-sided", 1, 1001));
        assertEquals("Copies must be between 1 and 1000.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#9 - BVA: both maxima at once, no overflow or precision loss
    // ------------------------------------------------------------------
    @Test
    public void TC9_maximumValidOrderVolume() {
        assertEquals(375000.00,
                     calc.calculateBaseCharge("A4", "Colour", "Double-sided", 500, 1000),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#10 - EP invalid partitions for pages and copies
    // ------------------------------------------------------------------
    public Object[] invalidPageAndCopyPartitions() {
        return new Object[] {
            new Object[] {  -50,    1, "Pages must be between 1 and 500."    },
            new Object[] {  550,    1, "Pages must be between 1 and 500."    },
            new Object[] {   10,  -50, "Copies must be between 1 and 1000."  },
            new Object[] {   10, 1050, "Copies must be between 1 and 1000."  }
        };
    }

    @Test
    @Parameters(method = "invalidPageAndCopyPartitions")
    public void TC10_epInvalidPartitionsForPagesAndCopies(int pages, int copies,
                                                          String expectedMessage) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Black & White", "Single-sided",
                                           pages, copies));
        assertEquals(expectedMessage, e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#11 - EP invalid: unsupported paper size
    // ------------------------------------------------------------------
    @Test
    public void TC11_unsupportedPaperSizeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A6", "Black & White", "Single-sided", 10, 1));
        assertEquals("Invalid paper size: A6", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#12 - EP invalid: null paper size (distinct message)
    // ------------------------------------------------------------------
    @Test
    public void TC12_nullPaperSizeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge(null, "Colour", "Double-sided", 10, 1));
        assertEquals("Paper size must be selected.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#13 - EP invalid: unsupported print type
    // ------------------------------------------------------------------
    @Test
    public void TC13_unsupportedPrintTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Blue", "Single-sided", 10, 1));
        assertEquals("Invalid print type: Blue", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#14 - EP invalid: null printing side
    // ------------------------------------------------------------------
    @Test
    public void TC14_nullPrintingSideIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateBaseCharge("A4", "Colour", null, 10, 1));
        assertEquals("Printing side must be selected.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#15 - the per page rate lookup on its own
    // ------------------------------------------------------------------
    public Object[] tableTwoRates() {
        return new Object[] {
            new Object[] { "A4", "Black & White", "Single-sided", 0.20 },
            new Object[] { "A4", "Black & White", "Double-sided", 0.18 },
            new Object[] { "A4", "Colour",        "Single-sided", 0.80 },
            new Object[] { "A4", "Colour",        "Double-sided", 0.75 },
            new Object[] { "A3", "Black & White", "Single-sided", 0.40 },
            new Object[] { "A3", "Black & White", "Double-sided", 0.35 },
            new Object[] { "A3", "Colour",        "Single-sided", 1.50 },
            new Object[] { "A3", "Colour",        "Double-sided", 1.40 },
            new Object[] { "A5", "Black & White", "Single-sided", 0.15 },
            new Object[] { "A5", "Black & White", "Double-sided", 0.13 },
            new Object[] { "A5", "Colour",        "Single-sided", 0.60 },
            new Object[] { "A5", "Colour",        "Double-sided", 0.55 }
        };
    }

    @Test
    @Parameters(method = "tableTwoRates")
    public void TC15_perPageRateLookup(String paperSize, String printType,
                                       String printingSide, double expectedRate) {
        assertEquals(expectedRate, calc.getBaseRate(paperSize, printType, printingSide), DELTA);
    }
}