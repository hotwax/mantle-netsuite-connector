<#assign goodIdentification = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
    .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
    .condition("productId", "${transferOrderItem.productId}")
    .list()
    .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)/>

<#assign facilityIdentification = ec.entity.find("co.hotwax.facility.FacilityIdentification")
    .condition("facilityId", "${transferOrderItem.originFacilityId!}")
    .condition("facilityIdenTypeId", "ORDR_ORGN_DPT")
    .list()
    .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)/>

<#assign orderFacilityExternalId = ec.entity.find("org.apache.ofbiz.product.facility.Facility")
    .condition("facilityId", "${transferOrderItem.orderFacilityId}")
    .list()/>

<#assign originFacilityExternalId = ec.entity.find("org.apache.ofbiz.product.facility.Facility")
    .condition("facilityId", "${transferOrderItem.originFacilityId!}")
    .list()/>

{
    "HCOrderId": "${transferOrderItem.orderId}",
    "salesChannel": "WEB_SALES_CHANNEL",
    "HCShopifySalesOrderId": "${transferOrderItem.externalId}",
    "externalId":"${transferOrderItem.externalId}",
    "formLocation": "${originFacilityExternalId[0].externalId!}",
    "orderNote": "",
    "shippingMethod": "",
    "subsidiary": "${transferOrderItem.productStoreExternalId}",
    "date": "${transferOrderItem.orderDate}",
    "toLocation": "${orderFacilityExternalId[0].externalId}",
    <#if facilityIdentification?has_content>"department": "${facilityIdentification[0].idValue}",</#if>
    "amount": "",
    "orderLineId": "${transferOrderItem.orderItemSeqId}",
    "closed": "FALSE",
    <#if goodIdentification?has_content>"item": "${goodIdentification[0].idValue}",</#if>
    "quantity": "${transferOrderItem.quantity}",
    "transferPrice": "${transferOrderItem.unitPrice}"
}