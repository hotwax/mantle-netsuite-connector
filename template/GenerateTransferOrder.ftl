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

<#assign facilityIds = [transferOrderItem.originFacilityId!, transferOrderItem.orderFacilityId!]>

<#assign facilityList = ec.entity.find("org.apache.ofbiz.product.facility.Facility")
    .condition("facilityId", "in", facilityIds)
    .list()!/>

{
    "HCOrderId": "${transferOrderItem.orderId}",
    "salesChannel": "WEB_SALES_CHANNEL",
    "HCShopifySalesOrderId": "${transferOrderItem.externalId!}",
    "externalId":"${transferOrderItem.externalId!}",

    <#if facilityList?has_content>
        <#assign fromLocation = facilityList?filter(f -> f.facilityId == transferOrderItem.originFacilityId!)?first!/>
        <#if fromLocation?has_content>"formLocation": "${fromLocation.externalId!}",</#if>
        <#assign toLocation = facilityList?filter(f -> f.facilityId == transferOrderItem.orderFacilityId!)?first!/>
        <#if toLocation?has_content>"toLocation": "${toLocation.externalId!}",</#if>
    </#if>

    "orderNote": "",
    "shippingMethod": "",
    "subsidiary": "${transferOrderItem.productStoreExternalId}",
    "date": "${transferOrderItem.orderDate}",
    <#if facilityIdentification?has_content>"department": "${facilityIdentification[0].idValue}",</#if>
    "amount": "",
    "orderLineId": "${transferOrderItem.orderItemSeqId}",
    "closed": "FALSE",
    <#if goodIdentification?has_content>"item": "${goodIdentification[0].idValue}",</#if>
    "quantity": "${transferOrderItem.quantity}",
    "transferPrice": "${transferOrderItem.unitPrice}"
}