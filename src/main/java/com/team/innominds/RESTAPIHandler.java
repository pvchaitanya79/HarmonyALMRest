package com.team.innominds;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RESTAPIHandler {	
	static Logger log = LoggerFactory.getLogger("com.team.innominds.RESTAPIHandler");
	
	public static void main(String[] args) {
		CloseableHttpClient httpclient = HttpClients
                .custom()
                .build();
		//These variables are replaced with actual HPALM on-premise details in the Core Integration
		//Hence this main method is just a placeholder.
	    String projectName="901637940_DEMO";
	    String domainName="DEFAULT_901637940";
		String almAuthUrl = "https://almalm1250saastrial.saas.hpe.com/qcbin/api/authentication/sign-in";
		String almUrl = "https://almalm1250saastrial.saas.hpe.com/qcbin/rest/";
		String userName = "cpinnamaraju_innominds.com";
		final String secretKey = "Innominds123$";	
		String encryptedString = "LnsFoVq5OSoJ0C+ar5Z8SQ==";
		RESTAPIHandler restHandle = new RESTAPIHandler(); 
		restHandle.almAuthenticate(httpclient, almAuthUrl, userName, EncryptionHandler.decrypt(encryptedString, secretKey));
		
		//This part of the code gets triggered from Harmony Framework
		try {
			//Read mapping file and update test status for each test
			BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir")+"src/main/resources/mapping.csv"));
			String line = br.readLine();
			while (line!=null) {
				String[] tokens = line.split(",");
				restHandle.updateTestStatus(httpclient, almUrl+"domains/"+domainName+"/projects/"+projectName+"/", tokens[1], "Passed");
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			log.error("File Not found Exception: "+e.getMessage());
		} catch (IOException e) {
			log.error("IO Exception: "+e.getMessage());
		}		
		restHandle.cleanUp(httpclient);  
		
	}
	
	public ResponseHandler<String> responseHandler() {
		ResponseHandler<String> responseHandler1 = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 400) {                
            	HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        };
		return responseHandler1;
	}
	
	private void updateTestStatus(CloseableHttpClient httpclient, String almUrl, String testID, String tstatus) {
		String tesinstanceQueryURL = almUrl + "test-instances?query={test-id["+testID+"]}";
		try {             
        	HttpUriRequest request1 = RequestBuilder.get()
                    .setUri(tesinstanceQueryURL)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "text/xml")                 
                    .build();
        	ResponseHandler<String> responseHandler1 = responseHandler();
            String responseBody = "";
            responseBody = httpclient.execute(request1, responseHandler1);
			log.info("Test Instance Query success with response: "+responseBody);
			InputSource source = new InputSource(new StringReader(responseBody));
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(source);
			
			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();
			ArrayList<StringBuilder> runXMLData = new ArrayList<StringBuilder>();
			runXMLData.add(new StringBuilder(xpath.evaluate("//Field[@Name=\"test-config-id\"]", document)));
			runXMLData.add(new StringBuilder(xpath.evaluate("//Field[@Name=\"cycle-id\"]", document)));
			runXMLData.add(new StringBuilder(testID));			
			runXMLData.add(new StringBuilder(xpath.evaluate("//Field[@Name=\"id\"]", document)));
			//Arbitrary run name
			runXMLData.add(new StringBuilder("HarmonyRun1"));
			runXMLData.add(new StringBuilder(xpath.evaluate("//Field[@Name=\"owner\"]", document)));
			runXMLData.add(new StringBuilder(tstatus));
			String execution_date = "2018-05-07";
			runXMLData.add(new StringBuilder(execution_date));
			String execution_time = "11:55:00";
			runXMLData.add(new StringBuilder(execution_time));
			
			String runXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>	<Entity Type=\"run\">	<Fields>	<Field Name=\"test-config-id\"><Value>##param1</Value></Field>	<Field Name=\"cycle-id\"><Value>##param2</Value></Field>	<Field Name=\"test-id\"><Value>##param3</Value></Field>	<Field Name=\"testcycl-id\"><Value>##param4</Value></Field>	<Field Name=\"build-revision\"><Value>1</Value></Field>	<Field Name=\"name\"><Value>##param5</Value></Field>	<Field Name=\"owner\"><Value>##param6</Value></Field>	<Field Name=\"status\"><Value>##param7</Value></Field>	<Field Name=\"subtype-id\"><Value>hp.qc.run.external-test</Value></Field>	<Field Name=\"duration\"><Value>5</Value></Field>	<Field Name=\"execution-date\"><Value>##param8</Value></Field>	<Field Name=\"execution-time\"><Value>##param9</Value></Field>	<Field Name=\"status\"><Value>##param7</Value></Field>	</Fields>	</Entity>";
			for (int i=1; i<=9; i++) {
				runXML = runXML.replaceAll("##param"+i, runXMLData.get(i).toString());
			}
            String runUrl = almUrl + "runs";
            //HTTP POST here
            HttpUriRequest request2 = RequestBuilder.post()
                    .setUri(runUrl)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "text/xml")  
                    .setEntity(new ByteArrayEntity(runXML.getBytes("UTF-8")))
                    .build();
            String responseBody1 = "";
            ResponseHandler<String> responseHandler2 = responseHandler();
			responseBody1 = httpclient.execute(request2, responseHandler2);
			log.info("ALM Test Status Updated successfully: "+responseBody1);
        } catch (IOException e1) {
        	log.error("Test Instance Query failure: "+e1.getMessage());
		} catch (ParserConfigurationException e) {
			log.error("Parse failure: "+e.getMessage());
		} catch (XPathExpressionException e) {
			log.error("XPath Expression failure: "+e.getMessage());
		} catch (SAXException e) {
			log.error("SAX Exception: "+e.getMessage());
		}
	}

	public void cleanUp(CloseableHttpClient httpclient) {
		try {
			httpclient.close();
		} catch (IOException e) {
			log.error("HTTP Client connection close failed: "+e.getMessage());
		}
	}
	
	//Trigger a Jenkins job
	public boolean almAuthenticate(CloseableHttpClient httpclient, String almAuthUrl, String userName, String password) {
        boolean success = false;
        String auth = userName+":"+password;
        String encodedauth = "Basic "+Base64.getEncoder().encodeToString(auth.getBytes());        
        try {             
            //Authenticate
        	HttpUriRequest request1 = RequestBuilder.get()
                    .setUri(almAuthUrl)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "text/xml")
                    .setHeader(HttpHeaders.AUTHORIZATION, encodedauth)
                    .build();
        	ResponseHandler<String> responseHandler1 = responseHandler();
            String responseBody = "";
            responseBody = httpclient.execute(request1, responseHandler1);
			log.info("Authentication with API successful and respense is: "+responseBody);
			success = true;
            
        } catch (IOException e1) {
        	log.error("Authentication failure: "+e1.getMessage());
		}            
        return success;
	}
}
