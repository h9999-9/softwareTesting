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
public class calculateOptionalChargeTest {

    private static final double DELTA = 0.001;

    private calculatePrintingCharge calc;

    @Before
    public void setUp() {
        printerAvailability mockPrinter = mock(printerAvailability.class);
        when(mockPrinter.isPrinterAvailable(anyString(), anyString())).thenReturn(true);
        calc = new calculatePrintingCharge(mockPrinter, new applyDiscount());
    }

    // ------------------------------------------------------------------
    // TC#1 - EP valid: no optional service selected
    // ------------------------------------------------------------------
    @Test
    public void TC1_noOptionalServiceCostsNothing() {
        assertEquals(0.00,
                     calc.calculateOptionalServiceCharge("None", false, false, 20),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#2 - EP: each binding rate in isolation
    // ------------------------------------------------------------------
    public Object[] bindingRates() {
        return new Object[] {
            new Object[] { "Staple", 2.00 },
            new Object[] { "Comb",   5.00 },
            new Object[] { "Spiral", 8.00 }
        };
    }

    @Test
    @Parameters(method = "bindingRates")
    public void TC2_eachBindingRateInIsolation(String binding, double expected) {
        assertEquals(expected,
                     calc.calculateOptionalServiceCharge(binding, false, false, 20),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#3 - Business Rule 10: lamination is per printed page
    // ------------------------------------------------------------------
    @Test
    public void TC3_laminationIsChargedPerPrintedPage() {
        // 20 printed pages x RM1.50, NOT a flat RM1.50
        assertEquals(30.00,
                     calc.calculateOptionalServiceCharge("None", true, false, 20),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#4 - express printing is a flat charge per order
    // ------------------------------------------------------------------
    @Test
    public void TC4_expressPrintingIsAFlatChargePerOrder() {
        // RM20.00 per order, NOT multiplied by the 20 printed pages
        assertEquals(20.00,
                     calc.calculateOptionalServiceCharge("None", false, true, 20),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#5 - two services combined
    // ------------------------------------------------------------------
    @Test
    public void TC5_twoOptionalServicesCombined() {
        // Staple RM2.00 + Lamination 20 x RM1.50 = RM32.00
        assertEquals(32.00,
                     calc.calculateOptionalServiceCharge("Staple", true, false, 20),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#6 - all three services combined
    // ------------------------------------------------------------------
    @Test
    public void TC6_allThreeOptionalServicesCombined() {
        // Comb RM5.00 + Express RM20.00 + Lamination 50 x RM1.50 = RM100.00
        assertEquals(100.00,
                     calc.calculateOptionalServiceCharge("Comb", true, true, 50),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#7 - BVA: the smallest laminated order
    // ------------------------------------------------------------------
    @Test
    public void TC7_bvaLowerValidBoundaryForLamination() {
        assertEquals(1.50,
                     calc.calculateOptionalServiceCharge("None", true, false, 1),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#8 - BVA: the largest laminated order
    // ------------------------------------------------------------------
    @Test
    public void TC8_bvaUpperValidBoundaryForLamination() {
        // Spiral RM8.00 + 500,000 x RM1.50 = RM750,008.00
        assertEquals(750008.00,
                     calc.calculateOptionalServiceCharge("Spiral", true, false, 500000),
                     DELTA);
    }

    // ------------------------------------------------------------------
    // TC#9 - BVA: a zero printed page count is rejected, not charged RM0.00
    // ------------------------------------------------------------------
    @Test
    public void TC9_zeroPrintedPageCountIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateOptionalServiceCharge("None", true, false, 0));
        assertEquals("Pages and copies must be at least 1.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#10 - EP invalid: a negative printed page count
    // ------------------------------------------------------------------
    @Test
    public void TC10_negativePrintedPageCountIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateOptionalServiceCharge("None", true, false, -100));
        assertEquals("Pages and copies must be at least 1.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#11 - EP invalid: an unsupported binding type
    // ------------------------------------------------------------------
    @Test
    public void TC11_unsupportedBindingTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateOptionalServiceCharge("Hardcover", false, false, 10));
        assertEquals("Invalid binding option: Hardcover", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#12 - Business Rule 9: only one binding option
    // ------------------------------------------------------------------
    @Test
    public void TC12_moreThanOneBindingOptionIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> calc.calculateOptionalServiceCharge("Comb, Spiral", false, false, 10));
        assertEquals("Only one binding option may be selected.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#13 - a null or blank binding means no binding
    // ------------------------------------------------------------------
    @Test
    @Parameters(method = "blankBindings")
    public void TC13_nullOrBlankBindingIsTreatedAsNoBinding(String binding) {
        assertEquals(0.00,
                     calc.calculateOptionalServiceCharge(binding, false, false, 20),
                     DELTA);
    }

    public Object[] blankBindings() {
        return new Object[] { new Object[] { null }, new Object[] { "" }, new Object[] { "   " } };
    }

    // ------------------------------------------------------------------
    // TC#14 - the binding charge lookup on its own
    // ------------------------------------------------------------------
    public Object[] bindingLookups() {
        return new Object[] {
            new Object[] { "None",         0.00 },
            new Object[] { "Staple",       2.00 },
            new Object[] { "Comb",         5.00 },
            new Object[] { "Spiral",       8.00 }
        };
    }

    @Test
    @Parameters(method = "bindingLookups")
    public void TC14_bindingChargeLookupValidPartitions(String binding, double expected) {
        assertEquals(expected, calc.getBindingCharge(binding), DELTA);
    }

    @Test
    public void TC14_bindingChargeLookupInvalidPartitions() {
        assertEquals("Invalid binding option: Hardcover",
            assertThrows(IllegalArgumentException.class,
                () -> calc.getBindingCharge("Hardcover")).getMessage());

        assertEquals("Only one binding option may be selected.",
            assertThrows(IllegalArgumentException.class,
                () -> calc.getBindingCharge("Comb, Spiral")).getMessage());
    }
}