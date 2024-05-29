package resources;

import java.net.URI;
import java.net.URISyntaxException;
 
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang.SystemUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * 
 * @author AYUSH BHARDWAJ (1788364)
 *
 */

public class Send_Mail_BUILD_Regression {
	
	public static boolean check = false;
	public static void regressionInitiationMail() {
		
		String formattedDate = "";
		DateFormat df = new SimpleDateFormat("dd-MMM-yy hh:mm a");

		Date today = new Date();
		df.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
		formattedDate = df.format(today);
		System.out.println("Date : " + formattedDate);
		
		String subject = "Regression Initiation Mail | " + ENV_Variables.PROJECT + " | " + AC_MAIN_V.REG_PKG;
		
		String html = "<p><span style=\"font-family: Calibri, sans-serif;\">Dear All,</span></p>\r\n" + 
				"<div>\r\n" + 
				"    <div><br></div>\r\n" + 
				"    <div><span style=\"font-family: Calibri, sans-serif;\">Below I-TAS Regression is in Progress.<br>Package : <strong>"+AC_MAIN_V.REG_PKG+"</strong><br>Environment : <strong>"+ENV_Variables.PROJECT+"</strong><br>Execution Start Date &amp; Time : <strong>"+formattedDate+"</strong></span></div>\r\n" + 
				"    <div><br></div>\r\n" + 
				"    <div><span style=\"font-family: Calibri, sans-serif;\">Kindly avoid any changes or restarts on environment during execution.</span></div>\r\n" + 
				"    <p><br></p>\r\n" + 
				"    <div><span style=\"font-family: Calibri, sans-serif;\">Kind Regards,</span></div>\r\n" + 
				"    <div><br></div>\r\n" + 
				"    <div><span style=\"font-family: Calibri, sans-serif;\">I-TAS Support Team</span></div>\r\n" + 
				"    <div><br></div>\r\n" + 
				"    <p><span style=\"font-family: Calibri, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p>\r\n" + 
				"</div>";
		html = html.replaceAll("\\r\\n|\\r|\\n", "");
		
		String bcc = props.getProperty("bcc"); 
		String replyTo = "";
		String cc = props.getProperty("cc"); 
		String to = props.getProperty("to"); 
		
		sendGeneralMailWithoutFile(to, replyTo, cc, bcc, subject, html, false);
		
	}
	
	public static Properties props = null;
	
	public static Properties getMailProperties() {
		Properties prop = new Properties();
		
		String cmd = "cat " + ENV_Variables.Env_Home + ENV_Variables.RFT_SCRIPTS + "reportsMailConfig.properties";
		
		Putty_Login.createExecChannel(cmd);
		
		String result = Putty_Login.output;
		
		StringReader sr = new StringReader(result);
		
		try {
			prop.load(sr);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sr != null) {
				sr.close();
			}
		}
		
		props = prop;
		
		return prop;
	}
	
	public static String getAttachmentLink(String filepath) {
		
		String link = "";
		
		String URL_UPLOAD_FILE = "http://172.21.163.176:8082/drive/upload";
		String pathToFile = filepath;

		String password = "test123";

		Map<String, String> params = new HashMap<String, String>(1);
		params.put("password", password);

		try {
			String result = multipartRequest(URL_UPLOAD_FILE, params, pathToFile, "file", "file/zip");
			link = parseJSONResult(result);
			System.out.println("Link is :: " + link);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to Get the Attachment Link.");
		}
		return link;
	}

	public static void sendGeneralMailWithoutFile(String to, String replyTo, String cc, String bcc, String subject,
			String htmlbody, boolean generateMailChain) {

		String URL_UPLOAD_FILE = "http://172.21.163.176:8082/send-mail";
		try {
			String result = multiPartRequestWithoutFile(URL_UPLOAD_FILE, to, replyTo, cc, bcc, htmlbody, subject);
			if(generateMailChain) {
				setMessageId(result);
			}
		} catch (Exception e) {
			System.out.println("Unable to send the mail.");
		}

	}

	public static void sendGeneralMailWithFile(String to, String replyTo, String cc, String bcc, String subject,
			String htmlbody, String filepath, boolean generateMailChain) {

		String password = "test123";

		Map<String, String> params = new HashMap<String, String>(7);
		params.put("password", password);
		params.put("replyTo", replyTo);
		params.put("subject", subject);
		params.put("to", to);
		params.put("html", htmlbody);
		params.put("cc", cc);
		params.put("bcc", bcc);

		String URL_UPLOAD_FILE = "http://172.21.163.176:8082/send-mail";

		try {
			String result = multipartRequest(URL_UPLOAD_FILE, params, filepath, "file", "file/zip");
			System.out.println("Result is :: " + result);
			if(generateMailChain) {
				setMessageId(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to Send Mail.");
		}
		String boundary = "***********" + Long.toString(System.currentTimeMillis()) + "**********";
		String lineEnd = "\r\n";
		if (check) {
			System.out.println(lineEnd);
			System.out.println(boundary);
			System.out.println(".......UNSUCCESSFUL ATTEMPT.......");
		} else {
			System.out.println(lineEnd);
			System.out.println(boundary);
			System.out.println(".......MAIL SENT SUCCESSFULLY.......");
		}
	}

	public static String multiPartRequestWithoutFile(String url, String to, String replyTo, String cc, String bcc,
			String html, String subject) throws URISyntaxException, ClientProtocolException, IOException {

		String result = null;

		CloseableHttpClient httpclient = HttpClients.createDefault();

		try {
			HttpUriRequest httppost = RequestBuilder.post().setUri(new URI(url)).addParameter("to", to)
					.addParameter("cc", cc).addParameter("bcc", bcc).addParameter("subject", subject)
					.addParameter("password", "test123").addParameter("html", html).addParameter("replyTo", replyTo)
					.build();

			CloseableHttpResponse response = httpclient.execute(httppost);

			try {
				result = EntityUtils.toString(response.getEntity());
				System.out.println(result);
			} catch (Exception e) {
				System.out.println("Unable to get the result.");
				e.printStackTrace();
			} finally {
				response.close();
			}

		} catch (Exception e) {
			System.out.println("Unable to get the request.");
			e.printStackTrace();
		} finally {
			httpclient.close();
		}

		return result;
	}
	
	public static void send_mail_BUILD (boolean holdStat) {

		//AC_MAIN_V.REG_PKG = "RABO-Securities-Transfers";
		if(AC_MAIN_V.user_name.equals("989768")) {
			getMailProperties();
		}
		
		String bcc = props.getProperty("bcc"); 
		String replyTo = "";
		String cc = props.getProperty("cc"); 
		String password = "test123";
		String subject = "i-TAS Regression Report || " + AC_MAIN_V.REG_PKG;
		String to = props.getProperty("to"); 
				
		DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm aa");
		String executionEndTime = dateFormat.format(new Date()).toString();
		
		
		//String html = "<body style=\"font-family:Verdana;\"><p style=\"font-size: 110%\"><strong>Hi All,</strong></p><br><p><i>Please find attached Regression Report.</i></p><p>"+AC_MAIN_V.REG_PKG+" :- <strong><span style=\"background-color:#00FA9A;\">&nbsp; Successful &nbsp; </span></strong></p><br><p> <u>The Summary of the Regression is below :</u><br><li>Regression Started : "+Regression_BUILD_Report.getStartTime()+"</li><li> Regression Completed : "+Regression_BUILD_Report.getEndTime()+"</li><li>Test Case Executed : "+Regression_BUILD_Report.totalCount+"</li><li>Successful : "+Regression_BUILD_Report.successCount+"</li><li>Terminated : "+Regression_BUILD_Report.failedCount+"</li></p><br><p>Regards</p><p>ATT Support</p><br><br>This is system generated mail. Please do not respond.</p></body>";
		String html = null;
		if (holdStat) {
			int totalCount = Regression_BUILD_Report.passfailhold[0] + Regression_BUILD_Report.passfailhold[1] + Regression_BUILD_Report.passfailhold[2];
			//html = "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"+AC_MAIN_V.REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Status of Regression is <strong>OnHold</strong>.</span></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>";
			html = "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"+AC_MAIN_V.REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Status of Regression is <strong>OnHold</strong>.<br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please refer regression details below:</span></p><ul><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression Start Time : "+Regression_BUILD_Report.executionStartTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression End Time : "+executionEndTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "+totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "+Regression_BUILD_Report.passfailhold[0]+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Terminated : "+Regression_BUILD_Report.passfailhold[1]+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">OnHold : "+Regression_BUILD_Report.passfailhold[2]+"</li></ul><p><br></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>";
		} 
		else if(AC_MAIN_V.resubmitStatus) {
			
			int totalBeforeResubmit=Regression_BUILD_Report.reSubmitStat[1] + Regression_BUILD_Report.reSubmitStat[2]+Regression_BUILD_Report.reSubmitStat[3];
			int others=Regression_BUILD_Report.reSubmitStat[2] + Regression_BUILD_Report.reSubmitStat[3];
			int totalAfterResubmit=Integer.parseInt(Regression_BUILD_Report.totalCount) - totalBeforeResubmit;
			int succAfterResubmit=Integer.parseInt(Regression_BUILD_Report.successCount) - Regression_BUILD_Report.reSubmitStat[1];
			int failAfterResubmit=Integer.parseInt(Regression_BUILD_Report.failedCount) -others;
			html = "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"+AC_MAIN_V.REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please refer regression details below:</span></p><span>Status of Regression Before submit<br></span><p></p><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "+totalBeforeResubmit+"Out of"+Regression_BUILD_Report.reSubmitStat[0]+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "+succAfterResubmit+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Others : "+others+"</li></ul><br><p>Status of Regression After ReSubmit</p><br><ul><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression Start Time : "+Regression_BUILD_Report.executionStartTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression End Time : "+executionEndTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "+totalAfterResubmit+"Out of"+Regression_BUILD_Report.totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "+Regression_BUILD_Report.reSubmitStat[0]+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Terminated : "+failAfterResubmit+"</li></ul><p><br></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>";

		}
		else {
			html = "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"+AC_MAIN_V.REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please refer regression details below:</span></p><ul><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression Start Time : "+Regression_BUILD_Report.executionStartTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression End Time : "+executionEndTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "+Regression_BUILD_Report.totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "+Regression_BUILD_Report.successCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Terminated : "+Regression_BUILD_Report.failedCount+"</li></ul><p><br></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>";
		}
		
		Map<String, String> params = new HashMap<String, String>(7);
		params.put("password", password);
		params.put("subject", subject);
		params.put("replyTo", replyTo);
		params.put("to", to);
		params.put("html", html);
		params.put("cc", cc);
		params.put("bcc", bcc);

		String URL_UPLOAD_FILE = "http://172.21.163.176:8082/send-mail";
		String pathToFile = null;
		String file_name = AC_MAIN_V.REG_PKG.trim() + ".xlsx";
		try {
			file_name = AC_MAIN_V.REG_PKG.trim().replace(" ", "_") + ".xlsx";
		} catch (Exception e) {
			System.out.println("Unable to replace the blank spaces.");
		}
		if (SystemUtils.IS_OS_LINUX) {
			pathToFile = System.getProperty("user.dir") + "/NRTDetails/Regression_BUILD_Report/" + file_name;
			System.out.println("LINUX2");
		} else
			pathToFile = System.getProperty("user.dir") + "\\Regression_BUILD_Report\\" + file_name;
		// pathToFile = "C:/NRTDetails/Regression_BUILD_Report/Testreport.xlsx";
		try {
			String result = multipartRequest(URL_UPLOAD_FILE, params, pathToFile, "file", "file/xlsx");
			System.out.println("Result is :: " + result);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to Send Mail.");
		}
		String boundary = "***********" + Long.toString(System.currentTimeMillis()) + "**********";
		String lineEnd = "\r\n";
		if (check) {
			System.out.println(lineEnd);
			System.out.println(boundary);
			System.out.println(".......UNSUCCESSFUL ATTEMPT.......");
		} else {
			System.out.println(lineEnd);
			System.out.println(boundary);
			System.out.println(".......MAIL SENT SUCCESSFULLY.......");
		}
	}
	/*
	 * public static void send_mail_BUILD_For_Resubmit () {
	 * 
	 * System.out.println("resubmit start");
	 * 
	 * //AC_MAIN_V.REG_PKG = "RABO-Securities-Transfers";
	 * if(AC_MAIN_V.user_name.equals("989768")) { getMailProperties(); }
	 * 
	 * String bcc = props.getProperty("bcc"); String replyTo = ""; String cc =
	 * props.getProperty("cc"); String password = "test123"; String subject =
	 * "i-TAS Regression Report || " + AC_MAIN_V.REG_PKG; String to =
	 * props.getProperty("to");
	 * 
	 * DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm aa"); String
	 * executionEndTime = dateFormat.format(new Date()).toString();
	 * 
	 * 
	 * //String html =
	 * "<body style=\"font-family:Verdana;\"><p style=\"font-size: 110%\"><strong>Hi All,</strong></p><br><p><i>Please find attached Regression Report.</i></p><p>"
	 * +AC_MAIN_V.
	 * REG_PKG+" :- <strong><span style=\"background-color:#00FA9A;\">&nbsp; Successful &nbsp; </span></strong></p><br><p> <u>The Summary of the Regression is below :</u><br><li>Regression Started : "
	 * +Regression_BUILD_Report.getStartTime()+"</li><li> Regression Completed : "
	 * +Regression_BUILD_Report.getEndTime()+"</li><li>Test Case Executed : "
	 * +Regression_BUILD_Report.totalCount+"</li><li>Successful : "
	 * +Regression_BUILD_Report.successCount+"</li><li>Terminated : "
	 * +Regression_BUILD_Report.
	 * failedCount+"</li></p><br><p>Regards</p><p>ATT Support</p><br><br>This is system generated mail. Please do not respond.</p></body>"
	 * ; String html = null;
	 * 
	 * int totalCount = Regression_BUILD_Report.reSubmitStat[0] +
	 * Regression_BUILD_Report.reSubmitStat[1] +
	 * Regression_BUILD_Report.reSubmitStat[2]; //html =
	 * "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"
	 * +AC_MAIN_V.
	 * REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Status of Regression is <strong>OnHold</strong>.</span></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>"
	 * ; //html =
	 * "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"
	 * +AC_MAIN_V.
	 * REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Status of Regression is <strong>OnHold</strong>.<br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please refer regression details below:</span></p><ul><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression Start Time : "
	 * +Regression_BUILD_Report.
	 * rexecutionStartTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression End Time : "
	 * +executionEndTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "
	 * +totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "
	 * +Regression_BUILD_Report.reSubmitStat[0]
	 * +"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Terminated : "
	 * +Regression_BUILD_Report.reSubmitStat[2]
	 * +"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\"></li></ul><p><br></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>"
	 * ; int others=Integer.parseInt(Regression_BUILD_Report.failedCount) +
	 * Integer.parseInt(Regression_BUILD_Report.othersCount); //int
	 * others=Integer.parseInt(ot);
	 * 
	 * html =
	 * "<body><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Dear All,</span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please find attached <strong>I-TAS Regression Report</strong> for <strong>"
	 * +AC_MAIN_V.
	 * REG_PKG+".</strong><br></span></p><p><span style=\"font-family: Arial, Helvetica, sans-serif;\">Please refer regression details below:</span></p><span>Status of Regression Before submit<br></span><p></p><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "
	 * +Regression_BUILD_Report.
	 * totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "
	 * +Regression_BUILD_Report.
	 * successCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Others : "
	 * +others+"</li></ul><br><p>Status of Regression After ReSubmit</p><br><ul><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression Start Time : "
	 * +Regression_BUILD_Report.
	 * rexecutionStartTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Regression End Time : "
	 * +executionEndTime+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Total Cases to be Executed : "
	 * +totalCount+"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Successful : "
	 * +Regression_BUILD_Report.reSubmitStat[0]
	 * +"</li><li style=\"font-family: Arial, Helvetica, sans-serif;\">Terminated : "
	 * +Regression_BUILD_Report.reSubmitStat[2]
	 * +"</li></ul><p><br></p><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">Kind Regards,</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\">I-TAS Support Team</span></div><div><span style=\"font-family: Arial, Helvetica, sans-serif;\"><br></span></div><p><span style=\"font-family: Arial, Helvetica, sans-serif;\"><em>This is an automatically generated email &ndash; please do not reply to it</em><em>.</em></span></p></body>"
	 * ;
	 * 
	 * System.out.println("before regsubmit");
	 * System.out.println(AC_MAIN_V.REG_PKG);
	 * System.out.println("Total Cases to be Executed :"+Regression_BUILD_Report.
	 * totalCount);
	 * System.out.println("Successful : "+Regression_BUILD_Report.successCount);
	 * System.out.println("Others : "+others); System.out.println("After resubmit");
	 * System.out.println("Total Cases to be Executed :"+totalCount);
	 * System.out.println("Successful : "+Regression_BUILD_Report.reSubmitStat[0]);
	 * System.out.println("terminate : "+Regression_BUILD_Report.reSubmitStat[2]);
	 * 
	 * Map<String, String> params = new HashMap<String, String>(7);
	 * params.put("password", password); params.put("subject", subject);
	 * params.put("replyTo", replyTo); params.put("to", to); params.put("html",
	 * html); params.put("cc", cc); params.put("bcc", bcc);
	 * 
	 * String URL_UPLOAD_FILE = "http://172.21.163.176:8082/send-mail"; String
	 * pathToFile = null; String file_name = AC_MAIN_V.REG_PKG.trim() + ".xlsx"; try
	 * { file_name = AC_MAIN_V.REG_PKG.trim().replace(" ", "_") + ".xlsx"; } catch
	 * (Exception e) { System.out.println("Unable to replace the blank spaces."); }
	 * if (SystemUtils.IS_OS_LINUX) { pathToFile = System.getProperty("user.dir") +
	 * "/NRTDetails/Regression_BUILD_Report/" + file_name;
	 * System.out.println("LINUX2"); } else pathToFile =
	 * System.getProperty("user.dir") + "\\Regression_BUILD_Report\\" + file_name;
	 * // pathToFile = "C:/NRTDetails/Regression_BUILD_Report/Testreport.xlsx"; try
	 * { String result = multipartRequest(URL_UPLOAD_FILE, params, pathToFile,
	 * "file", "file/xlsx"); System.out.println("Result is :: " + result);
	 * 
	 * } catch (Exception e) { e.printStackTrace();
	 * System.out.println("Unable to Send Mail."); } String boundary = "***********"
	 * + Long.toString(System.currentTimeMillis()) + "**********"; String lineEnd =
	 * "\r\n"; if (check) { System.out.println(lineEnd);
	 * System.out.println(boundary);
	 * System.out.println(".......UNSUCCESSFUL ATTEMPT......."); } else {
	 * System.out.println(lineEnd); System.out.println(boundary);
	 * System.out.println(".......MAIL SENT SUCCESSFULLY......."); } }
	 */
	
	public static String multipartRequest(String urlTo, Map<String, String> parmas, String filepath, String filefield,
			String fileMimeType) {
		HttpURLConnection connection = null;
		DataOutputStream outputStream = null;
		InputStream inputStream = null;

		String twoHyphens = "--";
		String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
		String lineEnd = "\r\n";

		String result = "";

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;

		String[] q = filepath.split("/");
		int idx = q.length - 1;

		try {
			File file = new File(filepath);
			FileInputStream fileInputStream = new FileInputStream(file);

			URL url = new URL(urlTo);
			connection = (HttpURLConnection) url.openConnection();

			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

			outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(twoHyphens + boundary + lineEnd);
			outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx]
					+ "\"" + lineEnd);
			outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
			outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

			outputStream.writeBytes(lineEnd);

			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			while (bytesRead > 0) {
				outputStream.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}

			outputStream.writeBytes(lineEnd);

			// Upload POST Data
			Iterator<String> keys = parmas.keySet().iterator();
			while (keys.hasNext()) {
				String key = keys.next();
				String value = parmas.get(key);

				outputStream.writeBytes(twoHyphens + boundary + lineEnd);
				outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
				outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
				outputStream.writeBytes(lineEnd);
				outputStream.writeBytes(value);
				outputStream.writeBytes(lineEnd);
			}

			outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			if (200 != connection.getResponseCode()) {
				System.out.println("Failed to upload code:" + connection.getResponseCode() + " "
						+ connection.getResponseMessage());
				check = true;
			}

			inputStream = connection.getInputStream();

			result = convertStreamToString(inputStream);

			fileInputStream.close();
			inputStream.close();
			outputStream.flush();
			outputStream.close();

		} catch (Exception e) {
			System.out.println("Unable to Read and Write.");
			e.printStackTrace();
		}
		return result;
	}

	public static String parseJSONResult(String result) {

		String url = "";
		try {
			JSONObject obj = new JSONObject(result);
			url = obj.getJSONObject("urls").getString("file");
		} catch (JSONException e) {
			System.out.println("Unable to parse the POST request.");
			e.printStackTrace();
		}
		return url;

	}

	public static String convertStreamToString(InputStream is) {

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String getMessageId() {
		String command = "cat /attmulti1/attmultil/Scheduling/mail-Id/"+AC_MAIN_V.REG_PKG+"_"+ENV_Variables.ENV_PART_ID+".txt";
		String result = executePuttyComnd(command);
		result = result.replaceAll("(\\r|\\n)", "").trim();
		return result;
	}
	
	public static void setMessageId(String result) {
		String id = "";
		
		try {
			JSONObject obj = new JSONObject(result);
			id = obj.getString("messageId");
		} catch (JSONException e) {
			System.out.println("Unable to parse the POST request messageId.");
			e.printStackTrace();
		}
		String command = "touch /attmulti1/attmultil/Scheduling/mail-Id/"+AC_MAIN_V.REG_PKG+"_"+ENV_Variables.ENV_PART_ID+".txt; echo \""+id.trim()+"\" >| /attmulti1/attmultil/Scheduling/mail-Id/"+AC_MAIN_V.REG_PKG+"_"+ENV_Variables.ENV_PART_ID+".txt";
		String output = executePuttyComnd(command);
		System.out.println("OutPut : " + output);
		//copymessagetoPutty(id, path);
		
	}
	
	public static String executePuttyComnd(String command) {
		String output = "";
		try {
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			JSch jsch = new JSch();
			Session session = jsch.getSession("attmulti1", "172.21.157.115", 22);
			session.setPassword("attmulti1@tcs2022");
			session.setConfig(config);
			session.connect();
			System.out.println("Connected");
			StringBuilder outputBuffer = new StringBuilder();

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);

			InputStream in = channel.getInputStream();
			channel.connect();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					outputBuffer.append(new String(tmp, 0, i));
					output = outputBuffer.toString();
					// System.out.print(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					System.out.println("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			session.disconnect();
			System.out.println("DONE");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return output;

	}
}
