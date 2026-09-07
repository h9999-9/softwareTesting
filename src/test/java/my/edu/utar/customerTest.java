package my.edu.utar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class customerTest {

    // ------------------------------------------------------------------
    // TC#1 - valid construction, all getters
    // ------------------------------------------------------------------
    @Test
    public void TC1_validCustomerStoresEveryAttribute() {
        customer c = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                                  "0123456789", customer.TYPE_STUDENT);

        assertEquals("C001", c.getCustomerID());
        assertEquals("Ali Bin Ahmad", c.getCustomerName());
        assertEquals("ali@gmail.com", c.getEmail());
        assertEquals("0123456789", c.getPhoneNumber());
        assertEquals("Student", c.getCustomerType());
        // the five argument convenience constructor defaults previousOrders to 0
        assertEquals(0, c.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#2 - EP valid: the three customer types of Table 4
    // ------------------------------------------------------------------
    public Object[] validCustomerTypes() {
        return new Object[] {
            new Object[] { customer.TYPE_STUDENT   },
            new Object[] { customer.TYPE_CORPORATE },
            new Object[] { customer.TYPE_OTHER     }
        };
    }

    @Test
    @Parameters(method = "validCustomerTypes")
    public void TC2_allValidCustomerTypesAreAccepted(String type) {
        customer c = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                                  "0123456789", type);
        assertEquals(type, c.getCustomerType());
    }

    // ------------------------------------------------------------------
    // TC#3 - previous order count is stored
    // ------------------------------------------------------------------
    @Test
    public void TC3_previousOrderCountIsStored() {
        customer c = new customer("C002", "Tan Wei Ming", "tanwm@printcorp.com",
                                  "0129876543", customer.TYPE_CORPORATE, 25);
        assertEquals(25, c.getPreviousOrders());
    }

    // ------------------------------------------------------------------
    // TC#4 - EP invalid: unsupported customer type
    // ------------------------------------------------------------------
    @Test
    public void TC4_invalidCustomerTypeIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new customer("C003", "Siti Aminah", "siti@gmail.com",
                               "0111234567", "VIP", 0));
        assertEquals("Invalid customer type: VIP", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#5 - null or empty customer ID
    // ------------------------------------------------------------------
    @Test
    @Parameters(method = "blankValues")
    public void TC5_nullOrEmptyCustomerIDIsRejected(String badID) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new customer(badID, "Ali Bin Ahmad", "ali@gmail.com",
                               "0123456789", customer.TYPE_STUDENT));
        assertEquals("Customer ID must not be null or empty.", e.getMessage());
    }

    public Object[] blankValues() {
        return new Object[] { new Object[] { null }, new Object[] { "" } };
    }

    // ------------------------------------------------------------------
    // TC#6 - null or empty customer name
    // ------------------------------------------------------------------
    @Test
    @Parameters(method = "blankValues")
    public void TC6_nullOrEmptyCustomerNameIsRejected(String badName) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new customer("C004", badName, "x@gmail.com",
                               "0123456789", customer.TYPE_OTHER));
        assertEquals("Customer name must not be null or empty.", e.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#7 - negative previous order count, constructor and setter
    // ------------------------------------------------------------------
    @Test
    public void TC7_negativePreviousOrdersIsRejected() {
        IllegalArgumentException fromConstructor = assertThrows(IllegalArgumentException.class,
            () -> new customer("C005", "Ali Bin Ahmad", "ali@gmail.com",
                               "0123456789", customer.TYPE_STUDENT, -5));
        assertEquals("Previous orders must not be negative.", fromConstructor.getMessage());

        customer valid = new customer("C005", "Ali Bin Ahmad", "ali@gmail.com",
                                      "0123456789", customer.TYPE_STUDENT, 5);
        IllegalArgumentException fromSetter = assertThrows(IllegalArgumentException.class,
            () -> valid.setPreviousOrders(-5));
        assertEquals("Previous orders must not be negative.", fromSetter.getMessage());
    }

    // ------------------------------------------------------------------
    // TC#8 - BVA on the Table 4 loyalty threshold (more than 20 orders)
    // ------------------------------------------------------------------
    public Object[] loyaltyBoundaries() {
        return new Object[] {
            new Object[] {  0, false, false },  
            new Object[] { 20, true,  false },   
            new Object[] { 21, true,  true  }    
        };
    }

    @Test
    @Parameters(method = "loyaltyBoundaries")
    public void TC8_loyaltyThresholdHelpers(int previousOrders,
                                            boolean expectedExisting,
                                            boolean expectedLoyalty) {
        customer c = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                                  "0123456789", customer.TYPE_OTHER, previousOrders);
        assertEquals(expectedExisting, c.isExistingCustomer());
        assertEquals(expectedLoyalty, c.qualifiesForLoyaltyDiscount());
    }

    // ------------------------------------------------------------------
    // TC#9 - toString() produces the customer.txt record format
    // ------------------------------------------------------------------
    @Test
    public void TC9_toStringProducesTheRecordFormat() {
        customer c = new customer("C001", "Ali Bin Ahmad", "ali@gmail.com",
                                  "0123456789", customer.TYPE_STUDENT, 10);
        assertEquals("C001,Ali Bin Ahmad,ali@gmail.com,0123456789,Student,10", c.toString());
    }

    @Test
    public void TC2_TC4_customerTypeValidator() {
        assertTrue(customer.isValidCustomerType("Student"));
        assertTrue(customer.isValidCustomerType("corporate"));  
        assertTrue(customer.isValidCustomerType("Other"));
        assertFalse(customer.isValidCustomerType("VIP"));
        assertFalse(customer.isValidCustomerType(null));
    }
}