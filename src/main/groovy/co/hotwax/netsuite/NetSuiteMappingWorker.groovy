package co.hotwax.netsuite

import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue

class NetSuiteMappingWorker {
    /**
     * Returns the mapped NetSuite value from IntegrationTypeMapping
     * based on the given integrationTypeId and mappingKey.
     *
     * @param ec ExecutionContext
     * @param integrationTypeId e.g., 'NETSUITE_SHP_MTHD'
     * @param mappingKey e.g., '2_DAY_SHIPPING_67G'
     * @return Mapped NetSuite value or null
     */
    static String getIntegrationMappingValue(ExecutionContext ec, String integrationTypeId, String mappingKey) {
        if (!integrationTypeId || !mappingKey) return null

        EntityValue mapping = ec.entity.find("co.hotwax.integration.IntegrationTypeMapping")
            .condition([integrationTypeId: integrationTypeId, mappingKey: mappingKey])
            .useCache(true)
            .one()
        return mapping?.mappingValue
    }

    /**
     * Returns the NetSuite product ID for the given HC product ID,
     * using GoodIdentification with type 'NETSUITE_PRODUCT_ID'.
     *
     * @param ec ExecutionContext
     * @param hcProductId HotWax Commerce product ID
     * @return NetSuite product ID or null
     */
    static String getNetSuiteProductId(ExecutionContext ec, String hcProductId) {
        if (!hcProductId) return null
        EntityList gid = ec.entity.find("org.apache.ofbiz.product.product.GoodIdentification")
            .condition([productId: hcProductId, goodIdentificationTypeEnumId: "NETSUITE_PRODUCT_ID"])
            .list()
            .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)
        return gid?.first?.idValue
    }

    /**
     * Get NetSuite order type for a facility.
     */
    static String getOrderType(ExecutionContext ec, String facilityId) {
        return getFacilityIdentifications(ec, facilityId, "NETSUITE_ORDR_TYPE")
    }

    /**
     * Get department ID for a facility.
     */
    static String getFacilityDepartment(ExecutionContext ec, String facilityId) {
        return getFacilityIdentifications(ec, facilityId, "ORDR_ORGN_DPT")
    }

    /**
     * Get sales channel for a facility.
     */
    static String getFacilitySalesChannel(ExecutionContext ec, String facilityId) {
        return getFacilityIdentifications(ec, facilityId, "ORDR_ORGN_SLS_CHNL")
    }

    /**
     * Get blanket customer ID for a facility.
     */
    static String getDefaultFacilityCustomer(ExecutionContext ec, String facilityId) {
        return getFacilityIdentifications(ec, facilityId, "FAC_BLKT_CUST")
    }

    /**
     * Gets facility identification values for a specific facility ID and identification type.
     *
     * @param ec ExecutionContext
     * @param facilityId Facility ID to get identification for
     * @param facilityIdenTypeId Type of facility identification (e.g., 'ORDR_ORGN_SLS_CHNL')
     * @return identification values matching the criteria
     */
    static String getFacilityIdentifications(ExecutionContext ec, String facilityId, String facilityIdenTypeId) {
        if (!facilityId || !facilityIdenTypeId) return null

        EntityList identifications = ec.entity.find("co.hotwax.facility.FacilityIdentification")
            .condition("facilityId", facilityId)
            .condition("facilityIdenTypeId", facilityIdenTypeId)
            .list()
            .filterByDate("fromDate", "thruDate", ec.user.nowTimestamp)
        return identifications?.first?.idValue
    }

}
