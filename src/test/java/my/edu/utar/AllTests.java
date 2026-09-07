package my.edu.utar;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({

    customerTest.class,
    readCustomerTest.class,
    addNewCustomerTest.class,
    printOrderTest.class,
    calculateBaseChargeTest.class,
    calculateOptionalChargeTest.class,
    applyDiscountTest.class,
    calculatePrintingChargeTest.class,
    printerAvailabilityTest.class,
    generateInvoiceTest.class,
    integrationTest.class
})
public class AllTests {
}