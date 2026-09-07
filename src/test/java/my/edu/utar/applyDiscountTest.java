package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;


@RunWith(JUnitParamsRunner.class)
public class applyDiscountTest {

    private static final double DELTA = 0.0001;

    private applyDiscount discount;

    @Before
    public void setUp() {
        discount = new applyDiscount();
    }

    // ------------------------------------------------------------------
    // TC#1 to TC#12 - every rule of Decision Table #2
    // ------------------------------------------------------------------
    public Object[] decisionTableTwoRules() {
        return new Object[] {
            new Object[] { "R9  TC#1 ", "Other",     100.00,  5,  0.00, 100.00 },
            new Object[] { "R1  TC#2 ", "Student",   100.00,  5, 10.00,  90.00 },
            new Object[] { "R2  TC#3 ", "Student",   400.00,  5, 58.00, 342.00 },
            new Object[] { "R3  TC#4 ", "Student",   100.00, 25, 14.50,  85.50 },
            new Object[] { "R4  TC#5 ", "Student",   400.00, 25, 75.10, 324.90 },
            new Object[] { "R5  TC#6 ", "Corporate", 100.00,  5, 15.00,  85.00 },
            new Object[] { "R6  TC#7 ", "Corporate", 400.00,  5, 77.00, 323.00 },
            new Object[] { "R7  TC#8 ", "Corporate", 100.00, 25, 19.25,  80.75 },
            new Object[] { "R8  TC#9 ", "Corporate", 400.00, 25, 93.15, 306.85 },
            new Object[] { "R10 TC#10", "Other",     400.00,  5, 20.00, 380.00 },
            new Object[] { "R11 TC#11", "Other",     100.00, 25,  5.00,  95.00 },
            new Object[] { "R12 TC#12", "Other",     400.00, 25, 39.00, 361.00 }
        };
    }

    @Test
    @Parameters(method = "decisionTableTwoRules")
    public void TC1_TC12_decisionTableTwoAllRules(String rule, String customerType,
                                                  double subtotal, int previousOrders,
                                                  double expectedDiscount,
                                                  double expectedTotal) {
        double actual = discount.calculateDiscount(customerType, subtotal, previousOrders);

        assertEquals("Discount for " + rule, expectedDiscount, actual, DELTA);
        assertEquals("Total for " + rule, expectedTotal, subtotal - actual, DELTA);
    }

    // ------------------------------------------------------------------
    // TC#13 to TC#15 - BVA on the RM300 subtotal threshold
    // ------------------------------------------------------------------
    public Object[] subtotalThresholdBoundaries() {
        return new Object[] {
            // subtotal, expected discount (Other customer, 5 previous orders)
            new Object[] { 299.99,  0.0000 },   // TC#13 
            new Object[] { 300.00,  0.0000 },   // TC#14 
            new Object[] { 300.01, 15.0005 }    // TC#15 
        };
    }

    @Test
    @Parameters(method = "subtotalThresholdBoundaries")
    public void TC13_TC15_bvaSubtotalThreshold(double subtotal, double expectedDiscount) {
        assertEquals(expectedDiscount,
                     discount.calculateDiscount("Other", subtotal, 5), DELTA);
    }

    // ------------------------------------------------------------------
    // TC#16 to TC#18 - BVA on the previous orders threshold
    // ------------------------------------------------------------------
    public Object[] previousOrderBoundaries() {
        return new Object[] {
            new Object[] { 19, 0.00 },   // TC#16 
            new Object[] { 20, 0.00 },   // TC#17 
            new Object[] { 21, 5.00 }    // TC#18 
        };
    }

    @Test
    @Parameters(method = "previousOrderBoundaries")
    public void TC16_TC18_bvaPreviousOrdersThreshold(int previousOrders,
                                                     double expectedDiscount) {
        assertEquals(expectedDiscount,
                     discount.calculateDiscount("Other", 100.00, previousOrders),DELTA);
    }

    // ------------------------------------------------------------------
    // TC#19 - the discount is returned exact and unrounded
    // ------------------------------------------------------------------
    
    @Test
    public void TC19_discountIsReturnedExactAndUnrounded() {
        double actual = discount.calculateDiscount("Student", 111.11, 5);

        assertEquals(11.111, actual, DELTA);          // not RM11.11
        assertEquals(99.999, 111.11 - actual, DELTA); // rounded to RM100.00 by the caller
    }

    // ------------------------------------------------------------------
    // TC#20 - EP invalid: a null customer type
    // ------------------------------------------------------------------

    @Test
    public void TC20_nullCustomerTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> discount.calculateDiscount((String) null, 350.00, 25));
        assertEquals("Customer type must not be null or empty.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#21 - EP invalid: an unsupported customer type
    // ------------------------------------------------------------------

    @Test
    public void TC21_unsupportedCustomerTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> discount.calculateDiscount("VIP", 350.00, 25));
        assertEquals("Invalid customer type: VIP", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#22 - EP invalid: a negative subtotal
    // ------------------------------------------------------------------

    @Test
    public void TC22_negativeSubtotalIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> discount.calculateDiscount("Student", -50.00, 5));
        assertEquals("Subtotal must not be negative.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#23 - EP invalid: a negative previous order count
    // ------------------------------------------------------------------

    @Test
    public void TC23_negativePreviousOrdersIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> discount.calculateDiscount("Student", 350.00, -5));
        assertEquals("Previous orders must not be negative.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#24 - the customer object overload agrees with the String version
    // ------------------------------------------------------------------
    @Test
    public void TC24_customerOverloadAgreesWithTheStringVersion() {
        customer corporate = new customer("C002", "Tan Wei Ming", "tanwm@printcorp.com",
                                          "0129876543", customer.TYPE_CORPORATE, 25);

        double viaString   = discount.calculateDiscount("Corporate", 400.00, 25);
        double viaCustomer = discount.calculateDiscount(corporate, 400.00);

        assertEquals(93.15, viaString, DELTA);
        assertEquals(viaString, viaCustomer, DELTA);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> discount.calculateDiscount((customer) null, 400.00));
        assertEquals("Customer must not be null.", e.getMessage());
    }
}