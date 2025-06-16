<#assign orderMaster = ec.entity.find("org.apache.ofbiz.order.order.OrderHeader").condition("orderId", orderId).oneMaster("default")!>

<#-- Process phone numbers -->
<#assign billingPhone = ((orderDetails.billingCountryCode!'') + '' + (orderDetails.billingAreaCode!'') + '' + (orderDetails.billingContactNumber!'')?trim)>
<#assign shippingPhone = ((orderDetails.shippingCountryCode!'') + '' + (orderDetails.shippingAreaCode!'') + '' + (orderDetails.shippingContactNumber!'')?trim)>

<#list orderDetails.contactMechs as orderContactMech>
    <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_LOCATION">
        <#assign shippingContactMech = orderContactMech>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "BILLING_LOCATION">
            <#assign billingContactMech = orderContactMech>
        </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "SHIPPING_EMAIL">
        <#assign shippingEmail = orderContactMech.contactMech.infoString>
    </#if>
    <#if orderContactMech.contactMechPurposeTypeId == "ORDER_EMAIL">
        <#assign billingEmail = orderContactMech.contactMech.infoString>
    </#if>
</#list>


{
    "orderName": "${orderDetails.orderName!}",
    "orderId": "${orderDetails.orderId!}",
    "date": "${orderDetails.orderDate!}",
    "customer": "${orderDetails.customer!}",
    "salesChannel": "${orderDetails.salesChannel!}",
    "addressee": "${shippingContactMech.addressee!}",
    "address1": "${shippingContactMech.address1!}",
    "address2": "${shippingContactMech.address2!}",
    "city": "${shippingContactMech.city!}",
    "state": "${shippingContactMech.state!}",
    "zip": "${shippingContactMech.zip!}",
    "country": "${shippingContactMech.country!}",
    <#if billingContactMech?has_content>
        "billingAddress1": "${billingContactMech.address1!}",
        "billingAddress2": "${billingContactMech.address2!}",
        "billingCity": "${billingContactMech.city!}",
        "billingState": "${billingContactMech.state!}",
        "billingZip": "${billingContactMech.zip!}",
        "billingCountry": "${billingContactMech.country!}",
        "billingAddressee": "${billingContactMech.addressee!}",
        <#else>
        "billingAddress1": "${shippingContactMech.address1!}",
        "billingAddress2": "${shippingContactMech.address2!}",
        "billingCity": "${shippingContactMech.city!}",
        "billingState": "${shippingContactMech.state!}",
        "billingZip": "${shippingContactMech.zip!}",
        "billingCountry": "${shippingContactMech.country!}",
        "billingAddressee": "${shippingContactMech.addressee!}",
    </#if>
    "shippingCost": ${orderDetails.shippingCost!0},
    "shippingPhone": "${shippingPhone!}",
    "billingContactMechId": "${orderDetails.billingContactMechId!}",
    "billingAddress1": "${orderDetails.billingAddress1!}",
    "billingAddress2": "${orderDetails.billingAddress2!}",
    "billingCity": "${orderDetails.billingCity!}",
    "billingState": "${orderDetails.billingState!}",
    "billingZip": "${orderDetails.billingZip!}",
    "billingCountry": "${orderDetails.billingCountry!}",
    "billingCountryCode": "${orderDetails.billingCountryCode!}",
    "billingAreaCode": "${orderDetails.billingAreaCode!}",
    "billingContactNumber": "${orderDetails.billingContactNumber!}",
    "billingAddressee": "${orderDetails.billingAddressee!}",
    "billingEmail": "${orderDetails.billingEmail!}",
    "email": "${orderDetails.email!}",
    "shippingCost": ${orderDetails.shippingCost!0}
}
