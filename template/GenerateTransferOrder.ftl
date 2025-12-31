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

<#assign netsuiteShipmentMethod = Static["co.hotwax.netsuite.NetSuiteMappingWorker"].getIntegrationTypeMappingValue(ec,'NETSUITE_SHP_MTHD', transferOrderItem.shipmentMethodTypeId)!>

{
    "HCOrderId": "${transferOrderItem.orderId}",
    "salesChannel": "${transferOrderItem.salesChannel!}",
    "externalId":"${transferOrderItem.orderId}-${transferOrderItem.orderName!}",

    <#if facilityList?has_content>
        <#assign fromLocation = facilityList?filter(f -> f.facilityId == transferOrderItem.originFacilityId!)?first!/>
        <#if fromLocation?has_content>"fromLocation": "${fromLocation.externalId!}",</#if>
        <#assign toLocation = facilityList?filter(f -> f.facilityId == transferOrderItem.orderFacilityId!)?first!/>
        <#if toLocation?has_content>"toLocation": "${toLocation.externalId!}",</#if>
    </#if>

    "orderNote": "",
    "shippingMethod": "${netsuiteShipmentMethod!}",
    "subsidiary": "${transferOrderItem.productStoreExternalId!}",
    "date": "${transferOrderItem.orderDate}",
    <#if facilityIdentification?has_content>"department": "${facilityIdentification[0].idValue}",</#if>
    "amount": "",
    "orderLineId": "${transferOrderItem.orderItemSeqId}",
    "closed": "FALSE",
    <#if goodIdentification?has_content>"item": "${goodIdentification[0].idValue}",</#if>
    "quantity": "${transferOrderItem.quantity}",
    "transferPrice": "${transferOrderItem.unitPrice}"
}