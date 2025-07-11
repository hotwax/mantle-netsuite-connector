package co.hotwax.netsuite

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.moqui.impl.entity.EntityListImpl

class NetSuiteMappingWorker {
    /**
     * Gets the mapped value from IntegrationTypeMapping for the given integration type and key.
     * @param ec ExecutionContext
     * @param integrationTypeId The integration type ID (e.g., 'NETSUITE_SHP_MTHD')
     * @param mappingKey The key to look up in the mapping
     * @return The mapped value or null if not found
     */
    static String getIntegrationTypeMappingValue(ExecutionContext ec, String integrationTypeId, String mappingKey) {
        EntityValue mapping = ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
            .condition([integrationTypeId: integrationTypeId, mappingKey: mappingKey])
            .useCache(true)
            .one()
        return mapping?.mappingValue
    }

    /**
     * Gets the NetSuite product ID for the given HotWax product ID.
     * @param ec ExecutionContext
     * @param hcProductId The HotWax Commerce product ID
     * @return The NetSuite product ID or null if not found
     */
    static String getProductId(ExecutionContext ec, String hcProductId) {
        EntityList gid = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
            .condition([productId: hcProductId, goodIdentificationTypeEnumId: "NETSUITE_PRODUCT_ID"])
            .list()
            .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)
        return gid?.first?.idValue
    }

    /**
     * Gets facility identification value for the given facility and identification type.
     * @param ec ExecutionContext
     * @param facilityId The facility ID
     * @param facilityIdenTypeId The identification type (e.g., 'ORDR_ORGN_SLS_CHNL')
     * @return The identification value or null if not found
     */
    static String getFacilityIdentifications(ExecutionContext ec, String facilityId, String facilityIdenTypeId) {
        EntityList identifications = ec.entity.find("co.hotwax.facility.FacilityIdentification")
            .condition("facilityId", facilityId)
            .condition("facilityIdenTypeId", facilityIdenTypeId)
            .useCache(true)
            .list()
            .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)
        return (identifications && !identifications.isEmpty()) ? identifications.first().idValue : null
    }

    /**
     * Calculates the grand total for an order.
     * @param adjustmentTotalAmount The total adjustment amount
     * @param orderItems List of order items
     * @return The calculated grand total
     */
    static BigDecimal getGrandTotal(BigDecimal adjustmentTotalAmount, List<Map> orderItems) {
        BigDecimal grandTotal = adjustmentTotalAmount ?: BigDecimal.ZERO

        if (orderItems) {
            orderItems.each { orderItem ->
                BigDecimal price = orderItem.price ?: BigDecimal.ZERO
                BigDecimal quantity = orderItem.quantity ?: BigDecimal.ZERO
                grandTotal = grandTotal.add(price * quantity)
            }
        }
        return grandTotal.setScale(2, java.math.RoundingMode.HALF_UP)
    }

    /**
     * Gets the total gift card payment amount for an order
     * @param ec ExecutionContext
     * @param orderId The order ID to get gift card payments for
     * @return The total gift card payment amount as BigDecimal, or 0 if none found
     */
    static BigDecimal getGiftCardPaymentTotal(ExecutionContext ec, String orderId) {
        def giftCardPayment = ec.entity.find("co.hotwax.netsuite.order.NonRefundedGiftCardPayment")
            .condition("orderId", orderId)
            .useCache(true).list()

        return giftCardPayment ? giftCardPayment[0].giftCardPaymentTotal : null
    }

    /**
     * Gets the shipping method for an order.
     * @param ec ExecutionContext
     * @param isMixCartOrder 'Y' if it's a mixed cart order, 'N' otherwise
     * @param orderItems List of order items
     * @return The mapped shipping method or null if not found
     */
    static String getShippingMethod(ExecutionContext ec, String isMixCartOrder, List<Map> orderItems) {

        if ("Y".equals(isMixCartOrder)) {
            // For mixed cart orders, find all unique shipping methods excluding POS_COMPLETED and STOREPICKUP
            def shippingMethods = orderItems.collect { it.shipmentMethodTypeId }
                    .findAll { it && it != 'POS_COMPLETED' && it != 'STOREPICKUP' }

            // Get the first shipping method if available
            def selectedMethod = shippingMethods ? shippingMethods.first() : null
            return selectedMethod ? getIntegrationTypeMappingValue(ec, 'NETSUITE_SHP_MTHD', (String) selectedMethod) : null
        } else {
            // For non-mixed cart orders, use the first valid shipping method
            return getIntegrationTypeMappingValue(ec, 'NETSUITE_SHP_MTHD', orderItems[0].shipmentMethodTypeId)
        }
    }

    /**
     * Gets the shipping tax code for an order.
     * @param ec ExecutionContext
     * @param orderId The order ID
     * @return The shipping tax code, defaults to "-Not Taxable-"
     */
    static String getShippingTaxCode(ExecutionContext ec, String orderId) {
        // Check for shipping tax adjustments
        def shippingTaxAdjustments = ec.entity.find("org.apache.ofbiz.order.order.OrderAdjustment")
                .condition("orderId", orderId)
                .condition("orderAdjustmentTypeId", "SHIPPING_SALES_TAX")
                .count()

        if (shippingTaxAdjustments > 0) {
            return getIntegrationTypeMappingValue(ec, 'NETSUITE_TAX_CODE', 'DEFAULT')
        }
        return "-Not Taxable-"
    }

    /**
     * Gets the tax code for an item.
     * @param ec ExecutionContext
     * @param item The item to get the tax code for
     * @return The tax code, defaults to "-Not Taxable-"
     */
    static String getTaxCode(ExecutionContext ec, Map item) {
        // Default to not taxable
        String taxCode = "-Not Taxable-"

        // If item has tax adjustments, use the default tax code
        if (item.taxAdjustments && !item.taxAdjustments.isEmpty()) {
            taxCode = getIntegrationTypeMappingValue(ec, 'NETSUITE_TAX_CODE', 'DEFAULT') ?: taxCode
        }
        return taxCode
    }

    /**
     * Gets the price level.
     * @param ec ExecutionContext
     * @return The price level or null if not found
     */
    static String getPriceLevel(ExecutionContext ec) {
        return getIntegrationTypeMappingValue(ec, 'NETSUITE_PRICE_LEVEL', 'PRICE_LEVEL')
    }

    /**
     * Gets discount items for an order item.
     * @param ec ExecutionContext
     * @param orderId The order ID
     * @param orderItem The order item to get discounts for
     * @return List of discount items
     */
    static  Map<String, Object> getDiscountItem(ExecutionContext ec, String orderId, HashMap orderItem) {
        Map<String, Object> discountRow;
        // Get promotion adjustments for this item
        def orderAdjustmentList = ec.entity.find("co.hotwax.order.OrderItemAdjustmentAndAttribute")
            .condition("orderId", orderId)
            .condition("orderItemSeqId", orderItem.orderItemSeqId)
            .condition("orderAdjustmentTypeId", "EXT_PROMO_ADJUSTMENT")
            .condition("attrName", null, "IS NULL")
            .list()
        if (orderAdjustmentList) {
            BigDecimal totalPromotionAmount = orderAdjustmentList.collect { it.amount ?: 0 }.sum() as BigDecimal
            if (totalPromotionAmount != 0) {
                discountRow = new HashMap<>(orderItem)
                discountRow.price = totalPromotionAmount
                discountRow.isDiscountRow = true
            }
        }
        return discountRow
    }

    /**
     * Processes order details and merges them with order items.
     * @param orderDetails Map containing order details including 'orderItems' key
     * @return List of processed order items with order details merged in
     */
    static List<Map<String, Object>> prepareNetSuiteOrderItemList(Map orderDetails) {
        if (!orderDetails) return [] as List<Map<String, Object>>

        List<Map<String, Object>> netsuiteOrderItemList = []
        List<Map> orderItems = orderDetails.remove("orderItems") as List<Map> ?: []

        orderItems.each { Map item ->
            Map<String, Object> processedItem = new HashMap<>(item)

            processedItem.putAll(orderDetails)
            processedItem.keySet().removeAll([
                'productStoreExternalId', 'shippingContactMechId', 'orderSalesChannelCode', '_entity',
                'billingCountryCode', 'shipmentMethodTypeId', 'billingContactNumber', 'netsuiteProductId',
                'orderItemSeqId', 'orderSalesChannelDescription', 'productId', 'adjustmentTotalAmount',
                'orderExternalId', 'netsuiteCustomerId', 'shippingContactNumber', 'billingContactMechId',
                'itemStatus', 'orderName', 'facilityId', 'shippingCountryCode', 'isDiscountRow',
                'orderFacilityExternalId', 'facilityExternalId', 'billingAreaCode', 'shippingAreaCode',
                'orderFacilityId'
            ])
            netsuiteOrderItemList.add(processedItem)
        }
        return netsuiteOrderItemList
    }
}
