package html;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import data.CommentDetails;
import engine.Engine;
import adapter.MailRequest;
import utils.SymbolConverter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class MorningReportBuilder {

		//Constructs and Sends the report
		
		private final static Logger logger = Logger.getLogger(MorningReportBuilder.class
				.getName());
		
		private static String pathString = "\\\\t65-w7-eqcash\\incoming\\";
		

		public static void buildReport(String fullName, ArrayList<CommentDetails> comments, Double eret, Double evol, Double mret, Double mvol) {

	        Calendar cal = Calendar.getInstance();
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
	        String date = sdf.format(cal.getTime());
	        
	        Utilities.moveImages(date, "MorningIndexReturns");
	        Utilities.moveImages(date, "MorningSectorsSnapshot");
			
			// Git commit/push images to remote repository
			Process p;
			try {
				p = Runtime.getRuntime().exec(pathString+"ChalkServer\\gitpush.bat");
				p.waitFor();
			} catch (IOException | InterruptedException e) {
				logger.info("Exception ocurred: git push to images remote repository");
			}

			
			
			String body = Utilities.getHTMLString("MorningUpdateTemplate.html");

			String fileSave = "ChalkTalkEmail.html"; 
			String formattedDate = (new SimpleDateFormat("EEEE, MMMM d, yyyy")).format(new Date());   
			String imagePath = "https://raw.githubusercontent.com/chalktalkimages/Chalkimages/master/archive/";
			String bellImage = imagePath + "MorningIndexReturns"+date+".png";
			String sectorsImage = imagePath + "MorningSectorsSnapshot"+ date+".png";
			String energyComments = buildSymbolComments(comments, "Energy"); 
			String materialsComments = buildSymbolComments(comments, "Materials");
			String otherComments = buildSymbolComments(comments, "Other");
			String htmlSignature = Utilities.getHTMLString("morningSignature.html");
			
			String buylist = getIOItradelist("buy");
			String selllist = getIOItradelist("sell");
			
			String energyStats = (eret >= 0 ? "<font color=\"#27AE60\">" : "<font color=\"#C0392B\">" ) + (eret >= 0 ? "+" : "" ) + String.format("%1$.2f", eret*100) + "%" + "</font>, "+ String.format("%1$.2f", evol) + "x ADV";
			String materialsStats = (mret >= 0 ? "<font color=\"#27AE60\">" : "<font color=\"#C0392B\">" ) +  (mret >= 0 ? "+" : "" ) + String.format("%1$.2f", mret*100) + "%" + "</font>, "+ String.format("%1$.2f", mvol) + "x ADV";
			
			body = body.replace("{{formattedDate}}", formattedDate)
					.replace("{{sectorsImage}}",sectorsImage)
					.replace("{{bellImage}}",bellImage)
					.replace("{{energyStats}}",energyStats)
					.replace("{{materialsStats}}", materialsStats)
					.replace("{{energyComments}}",  energyComments)
					.replace("{{materialsComments}}",  materialsComments)
					.replace("{{otherComments}}",  otherComments)
					.replace("{{morningSignature}}", htmlSignature)
					.replace("{{buylist}}", buylist)
					.replace("{{selllist}}", selllist)
					.replace("{{group}}", "EQUITY");
			
			
			body = body.replaceAll(Pattern.quote("ʼ"), "'");
			body = body.replaceAll(Pattern.quote("’"), "'");
			body = body.replaceAll(Pattern.quote("–"), "-");
			body = body.replaceAll(Pattern.quote("–"), "-");
			body = body.replaceAll(Pattern.quote("“"), "\"");
			body = body.replaceAll(Pattern.quote("”"), "\"");
			body = body.replaceAll(Pattern.quote("…"), "...");
			body = body.replaceAll(Pattern.quote("—"), "-");
			
			try (PrintWriter out = new PrintWriter(fileSave)) {
				out.println(body);
				//System.out.print("Template Printed to: " + fileSave + "\n");
				logger.info("Template Printed to: " + fileSave + "\n");
				
			} 
			catch (FileNotFoundException e) {
				//System.err.println("Error writing html to file: " + e.getMessage() + "\n");
				logger.info("Error writing html to file: " + e.getMessage() + "\n");
			}
			
			
			try {
				Utilities.updateEmailRecipients(fullName, "Scotiabank Morning Update");
			} catch (IOException e) {
				//System.err.println("Error sending email: " + e.getMessage() + "\n");
				logger.info("Error sending email: " + e.getMessage() + "\n");
			}
			

			Engine.getInstance().serverStatus.remove(fullName);
			MailRequest.sendRequest();
		}

		
		public static String buildSymbolComments(ArrayList<CommentDetails> comments, String sector) {

			String result = "";
			String temp = "";
			String macroComment = Utilities.getHTMLString("MorningComment.html");
			
			if (sector.equals("Other"))
			{
				for(CommentDetails comment : comments) {
					if(!comment.sector().equals("Energy") && !comment.sector().equals("Materials")) {
						temp = macroComment;
						temp = temp.replace("{{belongsTo}}", SymbolConverter.getPrefix(SymbolConverter.RIC2Symbol(comment.belongsTo())))
							.replace("{{evol}}", String.format("%1$.2f", comment.excessVol))
							.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + ": " : "" ) + Utilities.parseQuoteComment(comment.body(),false, false)); 
						if (comment.returns >= 0)
						{
							temp =  temp.replace("{{ret}}", "+"+String.format("%1$.2f", comment.returns*100))
									.replace("{{color}}", "#27AE60");
							
						}
						else
						{
							temp =  temp.replace("{{ret}}", String.format("%1$.2f", comment.returns*100))
									.replace("{{color}}", "#C0392B");
							
						}					
						result = result + temp + "\n"; 
					} 
				}				
			}
			else
			{			
				for(CommentDetails comment : comments) {
					if(comment.sector().equals(sector)) { //indicates that this comment belongs to sector
						temp = macroComment;
						temp = temp.replace("{{belongsTo}}", comment.belongsTo())
							.replace("{{evol}}", String.format("%1$.2f", comment.excessVol))
							.replace("{{body}}", (comment.sentiment() != null ? comment.sentiment() + ": " : "" ) + Utilities.parseQuoteComment(comment.body(),false, false)); 
						if (comment.returns >= 0)
						{
							temp =  temp.replace("{{ret}}", "+"+String.format("%1$.2f", comment.returns*100))
									.replace("{{color}}", "#27AE60");
							
						}
						else
						{
							temp =  temp.replace("{{ret}}", String.format("%1$.2f", comment.returns*100))
									.replace("{{color}}", "#C0392B");
							
						}									
						result = result + temp + "\n"; 
					} 
				}
			}
			return result; 
		}


			  
		private static String getIOItradelist(String side)
		{
			
			String list = "";
			int counter = 0;
			
		    try {
				JSONObject json = Utilities.readJsonFromUrl("http://t65-w7-eqcash:8001/" + side + "-ioi");
				JSONArray iois = (JSONArray) json.get("iois");
				for (int i = 0; i < iois.length(); i++)
				{
					JSONObject obj = iois.getJSONObject(i);
					JSONObject partialsmap = obj.getJSONObject("partialsMap");
					
					Iterator array = partialsmap.keys();
					String key = "";
					
					while (array.hasNext())
					{
						key = (String) array.next();
					}
					JSONObject a;
					String ric;
					String ticker;
					if (!key.equals(""))
					{
						
						 a = partialsmap.getJSONObject(key);
						 ticker = a.getString("ticker");
						 ric = a.getString("RIC");
					}
					else
					{
						a = obj;
						ticker = a.getString("tickerPrefix");
						ric = a.getString("ticker");
					}
					
					
					boolean working = a.getBoolean("working");
					boolean retail = a.getBoolean("retail");
					
					if (counter < 10 && !working && !retail && ticker != null && ric.endsWith(".TO") && !ric.contains("db.TO"))
					{
						int numshares = obj.getInt("shares");
						list += ", " + String.format("%1$d", numshares) + " " + ticker;
						counter++;
					}
				}
				list = list.replaceFirst(", ", "");
			} catch (Exception e) {
				logger.info("Error in getting list of IOI buys/sells: " + e.getMessage());
			}
			
		    
		    return list;
		}
	
}
