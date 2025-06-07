<#-- Fetching address related to the item -->
<#assign shippingAddress = ec.entity.find("co.hotwax.financial.PostalAddressView")
    .condition("contactMechId", brokerOrderItem.postalContactMechId)
    .useCache(true)
    .one()!>

<#-- Fetching the Good Identification related to the product -->
<#assign goodIdentificationList = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
    .condition("productId", brokerOrderItem.productId)
    .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
    .useCache(true)
    .list()!>

<#assign orderIdentifications = ec.entity.find("co.hotwax.order.OrderIdentification")
    .condition("orderId", brokerOrderItem.orderId)
    .condition("orderIdentificationTypeId", "NETSUITE_ORDER_ID")
    .list()!>

<#assign shippingMethodMapping =  ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
    .condition("integrationTypeId","NETSUITE_SHP_MTHD")
    .condition("mappingKey",brokerOrderItem.shipmentMethodTypeId)
    .list()!>

{
  "Line Id": "${brokerOrderItem.netsuiteItemLineId!''}",
  "External Id": "${brokerOrderItem.orderId!''}",
  <#if orderIdentifications?has_content> "Internal Id": "${orderIdentifications[0].idValue!}", </#if>
  <#if goodIdentificationList?has_content> "Item": "${goodIdentificationList[0].idValue!}", </#if>
  "Closed": "",
  "Quantity": "${brokerOrderItem.orderItemQuantity!''}",
  "Location": "${brokerOrderItem.facilityExternalId!''}",
  <#if shippingAddress?has_content>
      "Addressee": "${shippingAddress.toName!''}",
      "Address 1": "${shippingAddress.address1!''}",
      "Address 2": "${shippingAddress.address2!''}",
      "City": "${shippingAddress.city!''}",
      "State": "${shippingAddress.stateProvinceGeoId!''}",
      "Country": "${shippingAddress.countryGeoId!''}",
      "Zip": "${shippingAddress.postalCode!''}",
  </#if>
  "Shipping Method": <#if shippingMethodMapping?has_content>"${shippingMethodMapping[0].mappingValue!''}"<#else>"${brokerOrderItem.shipmentMethodTypeId!''}" </#if>
}
