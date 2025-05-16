{
  "order_id": "${fulfilledOrderItem.orderId?json_string}",
  "maySplit": "${fulfilledOrderItem.maySplit?default('N')?json_string}",
  "items": [
    {
      "line_id": "${fulfilledOrderItem.orderItemSeqId?json_string}",
      "shipment_method_type_id": "${fulfilledOrderItem.slaShipmentMethodTypeId?json_string}",
      "quantity": "${fulfilledOrderItem.quantity?string?json_string}",
      "location_id": "${fulfilledOrderItem.facilityExternalId?json_string}",
      "tags": "hotwax-fulfilled"
    }
  ]
}
