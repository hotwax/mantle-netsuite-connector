<#assign goodIdentification = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
    .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
    .condition("productId", "${transferOrderItem.productId}")
    .list()
    .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)/>

<#assign facilityIdentification = ec.entity.find("co.hotwax.facility.FacilityIdentification")
    .condition("facilityId", "${transferOrderItem.orderFacilityId}")
    .list()
    .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)/>


{
    "HCOrderId": "${transferOrderItem.orderId}",
    "salesChannel": "WEB_SALES_CHANNEL",
    "HCShopifySalesOrderId": "${transferOrderItem.externalId}",
    "externalId":"${transferOrderItem.externalId}",
    "formLocation": "${transferOrderItem?.originFacilityId}",
    "orderNote": "",
    "shippingMethod": "",
    "subsidiary": "${transferOrderItem.productStoreExternalId}",
    "date": "${transferOrderItem.orderDate}",
    "toLocation": "${transferOrderItem.orderFacilityId}",
    <#if facilityIdentification?has_content>"department": "${facilityIdentification[0].idValue}",</#if>
    "amount": "",
    "orderLineId": "${transferOrderItem.orderItemSeqId}",
    "closed": "FALSE",
    <#if goodIdentification?has_content>"item": "${goodIdentification[0].idValue}",</#if>
    "quantity": "${transferOrderItem.quantity}",
    "transferPrice": "${transferOrderItem.unitPrice}"
}