/*
 * This software is in the public domain under CC0 1.0 Universal plus a
 * Grant of Patent License.
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package co.hotwax.impl.service.runner

import groovy.transform.CompileStatic
import org.moqui.context.NotificationMessage
import org.moqui.impl.context.ContextJavaUtil
import org.moqui.impl.context.ExecutionContextImpl
import org.moqui.impl.service.ServiceDefinition
import org.moqui.impl.service.ServiceFacadeImpl
import org.moqui.impl.service.ServiceRunner
import org.moqui.util.RestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.moqui.entity.EntityValue
import org.moqui.impl.context.ExecutionContextFactoryImpl

@CompileStatic
class NetSuiteRestServiceRunner implements ServiceRunner {
    protected final static Logger logger = LoggerFactory.getLogger(NetSuiteRestServiceRunner.class)

    protected ServiceFacadeImpl sfi = null

    NetSuiteRestServiceRunner() {}

    ServiceRunner init(ServiceFacadeImpl sfi) { this.sfi = sfi; return this }

    Map<String, Object> runService(ServiceDefinition sd, Map<String, Object> parameters) {
        ExecutionContextImpl eci = sfi.ecfi.getEci()
        ExecutionContextFactoryImpl ecfi = eci.ecfi;

        String location = sd.location
        if (!location) throw new IllegalArgumentException("Location required to call remote netsuite service ${sd.serviceName}")
        String method = sd.method
        if (method == null || method.isEmpty()) {
            // default to verb IFF it is a valid method, otherwise default to POST
            if (RestClient.METHOD_SET.contains(sd.verb.toUpperCase())) method = sd.verb
            else method = "POST"
        }

        String netSuiteInstanceUrl = null;
        EntityValue netSuiteInstanceProperty = ecfi.entityFacade.find("org.apache.ofbiz.common.property.SystemProperty").condition("systemResourceId", "Restlet_Config").condition("systemPropertyId", location).useCache(true).disableAuthz().one();
        if (netSuiteInstanceProperty != null) {
            netSuiteInstanceUrl = netSuiteInstanceProperty.getString("systemPropertyValue");
        }

        RestClient rc = eci.serviceFacade.rest().method(method)
        String netSuiteBaseUrl = co.hotwax.util.NetSuiteUtil.getNetSuiteRestletInstanceUrl(eci.ecfi);
        String token = co.hotwax.util.NetSuiteUtil.generateAcessToken(eci.ecfi);

        rc.addHeader("Authorization", "Bearer " + token);
        StringBuffer uri;

        uri = new StringBuffer(netSuiteBaseUrl).append(netSuiteInstanceUrl);

        if (RestClient.GET.is(rc.getMethod())) {
            String parmsStr = RestClient.parametersMapToString(parameters)
            if (parmsStr != null && !parmsStr.isEmpty()) {
                if (uri.contains("?")) {
                    uri.append("&")
                } else {
                    uri.append("?");
                }
                uri.append(parmsStr)
            }
            rc.uri(uri.toString())
        } else {
            rc.uri(uri.toString())
            // NOTE: another option for parameters might be addBodyParameters(parameters), but a JSON body in the request is more common except for GET
            if (parameters != null && !parameters.isEmpty()) rc.jsonObject(parameters)
        }

        try {
            rc.maxResponseSize(10 * 1024 * 1024)
            RestClient.RestResponse response = rc.call()
            if (response.statusCode < 200 || response.statusCode >= 300) {
                logger.warn("Remote REST service " + sd.serviceName + " error " + response.statusCode + " (" + response.reasonPhrase + ") in response to " + rc.method + " to " + rc.uriString + ", response text:\n" + response.text())
                eci.messageFacade.addError("Remote service error ${response.statusCode}: ${getErrorMessage((Map)response.jsonObject())}")
                return null
            }
            logger.info("status code: ${response.statusCode}")

            Object responseObj = response.jsonObject()
            logger.info("responseObj: ${responseObj}");
            if (responseObj instanceof Map) {
                String error = getErrorMessage(responseObj);
                if (!error.isEmpty()) {
                    eci.messageFacade.addMessage(error, NotificationMessage.NotificationType.warning)
                }
                //TODO: We are getting serialization exception while returning responseObj as Map, explicitly call the json to map conversion
                Map<String, Object> outResult = new HashMap<>();
                combineResults(sd, outResult, (Map) ContextJavaUtil.jacksonMapper.readValue(response.text(), Map.class))
                return outResult;
            } else {
                return [response: responseObj]
            }
        } catch (Exception e) {
            logger.error(e.getMessage() + ":" + e.getCause().getMessage());
            eci.messageFacade.addError("Unable to call netsuite rest api : ${e.getCause().getMessage()}")
            return null
        }
    }

    String getErrorMessage(Map<String, ? extends Object> result) {
        StringBuilder errorMessage = new StringBuilder();

        if (result.get("error") != null) {
            errorMessage.append((String) result.get("error"));
        }
        if (result.get("errorMessage") != null) {
            errorMessage.append((String) result.get("errorMessage"));
        }

        if (result.get("errorMessageList") != null) {
            List<? extends Object> errors =(List) result.get("errorMessageList");
            for (Object message: errors) {
                // NOTE: this MUST use toString and not cast to String because it may be a MessageString object
                String curMessage = message.toString();
                if (errorMessage.length() > 0) {
                    errorMessage.append(", ");
                }
                errorMessage.append(curMessage);
            }
        }
        return errorMessage.toString();
    }
    void destroy() { }
    static void combineResults(ServiceDefinition sd, Map<String, Object> autoResult, Map<String, Object> csMap) {
        // if there are fields in ec.context that match out-parameters but that aren't in the result, set them
        boolean autoResultUsed = autoResult.size() > 0;
        String[] outParameterNames = sd.outParameterNameArray;
        int outParameterNamesSize = outParameterNames.length;
        for (int i = 0; i < outParameterNamesSize; i++) {
            String outParameterName = outParameterNames[i];
            Object outValue = csMap.get(outParameterName);
            if ((!autoResultUsed || !autoResult.containsKey(outParameterName)) && outValue != null) {
                autoResult.put(outParameterName, outValue);
            }
        }
    }

}