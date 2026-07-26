package crmapp.petsfort.JLogics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Protects the ordinal-based wire contract used by the Android client. */
public class CrmRpcContractTest {
    @Test
    public void businessRpcOrdinalsRemainStable() {
        assertEquals(1, CrmRpc.POST_PRODUCTS_QUERY.ordinal());
        assertEquals(15, CrmRpc.POST_PRODUCTS_BULK_DETAILS.ordinal());
        assertEquals(16, CrmRpc.POST_ORDERS_CHECKOUT.ordinal());
        assertEquals(18, CrmRpc.POST_ORDERS_QUERY.ordinal());
        assertEquals(21, CrmRpc.GET_CATEGORIES.ordinal());
        assertEquals(26, CrmRpc.GET_SUBCATEGORIES_BY_CATEGORY.ordinal());
        assertEquals(29, CrmRpc.GET_USERDATA.ordinal());
        assertEquals(30, CrmRpc.GET_USER.ordinal());
        assertEquals(32, CrmRpc.PUT_USERDATA.ordinal());
        assertEquals(60, CrmRpc.values().length);
    }
}
