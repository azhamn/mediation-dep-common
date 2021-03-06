package com.wso2telco.dep.common.mediation;

import com.wso2telco.dep.common.mediation.service.APIService;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MSISDNWhitelistMediator extends AbstractMediator {

	public boolean mediate(MessageContext messageContext) {

		String msisdn = (String) messageContext.getProperty("paramValue");
		String paramArray = (String) messageContext.getProperty("paramArray");
		String apiName = (String) messageContext.getProperty("API_NAME");
		String apiVersion = (String) messageContext.getProperty("VERSION");
		String apiPublisher = (String) messageContext.getProperty("API_PUBLISHER");
		String appID = messageContext.getProperty("api.ut.application.id").toString();
		String regexPattern = (String) messageContext.getProperty("msisdnRegex");
		String regexGroupNumber = (String) messageContext.getProperty("msisdnRegexGroup");

		Pattern pattern = Pattern.compile(regexPattern);
		Matcher matcher = pattern.matcher(msisdn);

		String formattedPhoneNumber = null;
		if (matcher.matches()) {
			formattedPhoneNumber = matcher.group(Integer.parseInt(regexGroupNumber));
		}

		try {
			APIService apiService = new APIService();
			String apiID = apiService.getAPIId(apiPublisher, apiName, apiVersion);
			String subscriptionID = apiService.getSubscriptionID(apiID, appID);
			if(log.isDebugEnabled()){
				log.debug("WhiteListHandler subscription id:" + subscriptionID);
			}

			if(apiService.isWhiteListed(formattedPhoneNumber, appID, subscriptionID, apiID)){
				messageContext.setProperty("WHITELISTED_MSISDN", "true");
			} else {
				log.info("Not a WhiteListed number:" + formattedPhoneNumber);
				messageContext.setProperty(SynapseConstants.ERROR_CODE, "POL0001:");
				messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, "Internal Server Error. Not a white listed" +
						" Number");
				messageContext.setProperty("WHITELISTED_MSISDN", "false");

				String errorVariable = msisdn;

				if(paramArray != null){
					errorVariable = paramArray;
				}
				setErrorInContext(
						messageContext,
						"SVC0004",
						" Not a whitelisted number. %1",
						errorVariable,
						"400", "POLICY_EXCEPTION");
			}

		} catch(Exception e) {

			log.error("error in MSISDNWhitelistMediator mediate : "
					+ e.getMessage());

			String errorVariable = msisdn;

			if(paramArray != null){
				errorVariable = paramArray;
			}
			setErrorInContext(
					messageContext,
					"SVC0001",
					"A service error occurred. Error code is %1",
					errorVariable,					"500", "SERVICE_EXCEPTION");
			messageContext.setProperty("INTERNAL_ERROR", "true");
		}
		return true;
	}

	private void setErrorInContext(MessageContext synContext, String messageId,
	                               String errorText, String errorVariable, String httpStatusCode,
	                               String exceptionType) {

		synContext.setProperty("messageId", messageId);
		synContext.setProperty("mediationErrorText", errorText);
		synContext.setProperty("errorVariable", errorVariable);
		synContext.setProperty("httpStatusCode", httpStatusCode);
		synContext.setProperty("exceptionType", exceptionType);
	}
}
