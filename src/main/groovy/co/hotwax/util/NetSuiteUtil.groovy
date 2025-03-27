package co.hotwax.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.moqui.context.ExecutionContext
import org.moqui.util.RestClient
import org.moqui.util.RestClient.RestResponse
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import org.moqui.entity.EntityValue
import org.moqui.impl.context.ExecutionContextFactoryImpl

public class NetSuiteUtil {
    /*
      * Method to get acess token from NetSuite
      * The value of the access_token parameter is in JSON Web Token (JWT) format.
      * The access token is valid for 60 minutes.
    */
    public static String generateAcessToken(ExecutionContextFactoryImpl ecfi) {
        ExecutionContext ec = ecfi.getExecutionContext();
        try {
            String accessToken = null
            long iat = System.currentTimeMillis() / 1000
            long exp = iat + 4000

            EntityValue systemMessageRemote = ecfi.entityFacade.find("moqui.service.message.SystemMessageRemote")
                    .condition("systemMessageRemoteId", "NS_SCRIPT_RESTLET").useCache(true).disableAuthz().one();
            String tokenEndpoint = null;
            String certificateId = null;
            String consumerKey = null;
            String privateKeyStr = null;

            if (systemMessageRemote != null) {
                tokenEndpoint =  systemMessageRemote.getString("receiveUrl");
                certificateId = systemMessageRemote.getString("sendSharedSecret");
                consumerKey = systemMessageRemote.getString("sharedSecret");
                privateKeyStr = systemMessageRemote.getString("privateKey");
            }

            // JWT Header
            Map<String, Object> headerClaims = new HashMap<>();
            headerClaims.put("alg", "ES256");
            headerClaims.put("typ", "JWT");
            // certificate id
            headerClaims.put("kid", certificateId);

            // JWT Payload
            Map<String, Object> payloadClaims = new HashMap<>();
            // consumer key
            payloadClaims.put("iss", consumerKey);
            payloadClaims.put("scope", "restlets");
            payloadClaims.put("iat", iat);
            payloadClaims.put("exp", exp);
            payloadClaims.put("aud", tokenEndpoint);

            // Load private key
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            Algorithm signer = Algorithm.ECDSA256((ECPrivateKey) privateKey);

            // Generate JWT
            String token = JWT.create().withHeader(headerClaims).withPayload(payloadClaims).sign(signer);
            ec.getLogger().info("JWT token: " + token);

            RestClient restClient = ec.getService().rest();
            try {
                RestResponse restResponse = restClient.method("POST")
                        .uri(tokenEndpoint)
                        .contentType("application/json")
                        .addHeader("Authorization", "Bearer " + token)
                        .addBodyParameter("grant_type", "client_credentials")
                        .addBodyParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                        .addBodyParameter("client_assertion", token)
                        .call();

                Map<String, Object> responseMap = restResponse.jsonObject();
                int statusCode = restResponse.getStatusCode();

                // Handle the response
                if (statusCode == 200) {
                    accessToken = (String) responseMap.get("access_token");
                    ec.getLogger().info("Access Token: " + accessToken);
                } else {
                    ec.getLogger().error("RESTlet Call Failed with Status Code: " + statusCode);
                    return ec.message.addError("Unable to get access token from netsuite. Status Code: ${statusCode}")
                }
            } catch (Exception e) {
                ec.getLogger().error("Error in API call: " + e.getMessage());
                return ec.message.addError("Error in access token API call: " + e.getMessage())
            }
            return accessToken;
        } catch (Exception e) {
            ec.getLogger().error("Error generating JWT: " + e.getMessage());
            return ec.message.addError("Error in generating JWT: " + e.getMessage())
        }
    }

    public static String getNetSuiteRestletInstanceUrl(ExecutionContextFactoryImpl ecfi) {
        String netSuiteInstanceUrl = null;
        EntityValue netSuiteInstance = ecfi.entityFacade.find("moqui.service.message.SystemMessageRemote").condition("systemMessageRemoteId", "NS_SCRIPT_RESTLET").useCache(true).disableAuthz().one();
        if (netSuiteInstance != null && netSuiteInstance.getString("sendUrl") != null) {
            netSuiteInstanceUrl =  netSuiteInstance.getString("sendUrl");
        }
        return netSuiteInstanceUrl;
    }
}