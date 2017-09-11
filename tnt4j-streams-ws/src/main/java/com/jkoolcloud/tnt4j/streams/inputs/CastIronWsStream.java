package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.soap.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WsStreamConstants;

/**
 * Class for IBM Cast Iron Logs and Jobs streaming. It handles login to Cast Iron and determine witch parser should be
 * used to parse specific reply.
 *
 *
 */

public class CastIronWsStream extends WsStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CastIronWsStream.class);

	private String securityResponseParserTag = "login";
	private static final String tokenCacheKey = "Token";
	private static final String loginRequestS = "<sec:login xmlns:sec=\"http://www.approuter.com/schemas/2008/1/security\"><sec:username>";
	private static final String loginRequestM = "</sec:username><sec:password>";
	private static final String loginRequestE = "</sec:password></sec:login>";

	private String loginUrl = "";
	private String securityUserName = "";
	private String securityPassword = "";

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Resolves dynamic values eg. ${name} in soapRequest from entries within cache.
	 *
	 * @param soapRequestData
	 *            SOAP request from configuration
	 *
	 * @return altered SOAP request
	 */
	@Override
	protected String preProcess(String soapRequestData) {
		String resp = resolveRequestCacheEntries(soapRequestData);
		return resp;
	}

	private static String resolveRequestCacheEntries(String soapRequestData) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, soapRequestData);

		for (String var : vars) {
			if (var.length() < 3)
				continue;
			Object cachedValue = StreamsCache.getValue(var.substring(2, var.length() - 1));
			if (cachedValue != null) {
				return soapRequestData.replace(var, cachedValue.toString());
			}
		}
		return soapRequestData;
	}

	/**
	 * It adds SOAP header with token saved in {@link StreamsCache} to existing SOAP request message.
	 *
	 * @param soapRequest
	 *            current SOAP request message
	 * @return message with token in SOAP header
	 */
	@Override
	protected void addSoapHeaders(SOAPMessage soapRequest) throws SOAPException {
		SOAPFactory soapFactory = SOAPFactory.newInstance();
		SOAPElement securityElem = soapFactory.createElement("sessionId", "log",
				"http://www.approuter.com/schemas/2008/1/lognotif");

		String cachedToken = String.valueOf(StreamsCache.getValue(tokenCacheKey));
		logger().log(OpLevel.DEBUG, "Adding Token {0} to request ", cachedToken);
		securityElem.setTextContent(cachedToken);
		soapRequest.getSOAPHeader().addChildElement(securityElem);
	}

	@Override
	public void setProperties(Collection<Entry<String, String>> props) {
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
				securityUserName = value;
			} else if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
				securityPassword = value;
			} else if (WsStreamProperties.PROP_SECURITY_RESPONSE_PARSER.equalsIgnoreCase(name)) {
				securityResponseParserTag = value;
			} else if (WsStreamProperties.PROP_LOGIN_URL.equalsIgnoreCase(name)) {
				loginUrl = value;
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_USERNAME.equalsIgnoreCase(name)) {
			return securityUserName;
		}
		if (StreamProperties.PROP_PASSWORD.equalsIgnoreCase(name)) {
			return securityPassword;
		}
		if (WsStreamProperties.PROP_SECURITY_RESPONSE_PARSER.equalsIgnoreCase(name)) {
			return securityResponseParserTag;
		}
		if (WsStreamProperties.PROP_LOGIN_URL.equalsIgnoreCase(name)) {
			return loginUrl;
		}

		return super.getProperty(name);
	}

	/**
	 * Check's that fault and if it's session invalid tries to login.
	 *
	 * @param fault
	 *            Soap fault
	 * @throws Exception
	 */
	@Override
	protected synchronized void handleFault(SOAPFault fault) throws Exception {
		if (fault.getFaultString().equals("Expired or invalid session ID")) {
			login();
			throw new Exception("Logged in!");
		} else {
			super.handleFault(fault);
		}
	}

	private void login() {
		final String soapRequestData = loginRequestS + securityUserName + loginRequestM + securityPassword
				+ loginRequestE;
		try {
			final SOAPMessage soapRequest = createMessage(soapRequestData, this, new HashMap<String, String>(), false);
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CastIronStream.login.Request"),
					convertSoapMessageToXMLString(soapRequest));

			SOAPMessage soapResponse = getSoapConnection().call(soapRequest, loginUrl);

			if (soapResponse.getSOAPBody().hasFault()) {
				logger().log(OpLevel.ERROR, StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME,
						"CastIronStream.login.Failed"), soapResponse.getSOAPBody().getFault().getFaultString());
			}

			final String responseString = convertSoapMessageToXMLString(soapResponse);

			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CastIronStream.login.Response"),
					responseString);

			applyParsers(responseString);
		} catch (Exception e) {
			logger().log(OpLevel.ERROR,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CastIronStream.login.Failed"),
					e.getMessage());
		}
	}

	/**
	 * Determines parser to use. It check's SOAP's message for method and uses uses the parser with tag of that method
	 * name
	 *
	 * @param data
	 *            activity data item to get tags
	 * @return
	 */
	@Override
	protected String[] getDataTags(Object data) {
		InputStream is = new ByteArrayInputStream(data.toString().getBytes());
		SOAPMessage request;
		String currentMethod = null;
		try {
			request = MessageFactory.newInstance().createMessage(null, is);
			currentMethod = request.getSOAPBody().getFirstChild().getLocalName().replace("Response", "");
		} catch (Exception e) {
			logger().log(OpLevel.ERROR,
					StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CastIronStream.failed.parser"),
					data, e.getMessage());
		}
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(WsStreamConstants.RESOURCE_BUNDLE_NAME, "CastIronStream.apply.parser"),
				currentMethod);
		return new String[] { currentMethod };
	}

}
