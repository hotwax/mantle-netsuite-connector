    <#-- Fetching address related to the item -->
    <#assign address = ec.entity.find("co.hotwax.financial.PostalAddressView")
      .condition("contactMechId", brokerOrderItem.postalContactMechId)
      .useCache(true)
      .one()!{}>
    <#-- Fetching the Good Identification related to the product -->
    <#assign goodId = ec.entity.find("GoodIdentification")
      .condition("productId", brokerOrderItem.productId)
      .condition("goodIdentificationTypeId", "NETSUITE_PRODUCT_ID")
      .useCache(true)
      .one()!{}>
    {
      "Line Id": "${brokerOrderItem.netsuiteItemLineId!''}",
      "External Id": "${brokerOrderItem.orderId!''}",
      "Item": "${goodId.idValue!''}",
      "Closed": "",
      "Quantity": "${brokerOrderItem.orderItemQuantity!''}",
      "Location": "${brokerOrderItem.facilityExternalId!''}",
      "Addressee": "${address.toName!''}",
      "Address 1": "${address.address1!''}",
      "Address 2": "${address.address2!''}",
      "City": "${address.city!''}",
      "State": "${address.stateProvinceGeoId!''}",
      "Country": "${address.countryGeoId!''}",
      "Zip": "${address.postalCode!''}",
      "Shipping Method": "${brokerOrderItem.shipmentMethodTypeId!''}"
    }
