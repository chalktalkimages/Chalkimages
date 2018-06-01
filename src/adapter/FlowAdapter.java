package adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import data.Comment;
import data.CorporateEvent;
  
//Adapter used to communicate with the Flow Server
public class FlowAdapter {
	
	private static String requestUrl = "http://t65-w7-eqcash:9001/"; 
	//private static String requestUrl = "http://t68-w7-bliu:9001/";
	
	public static List<CorporateEvent> getEvents() {
		try {
			
			//Request comment list from Flow
			OkHttpClient client = new OkHttpClient();
			Request request = new Request.Builder()
			  .url(requestUrl + "get-events")
			  .addHeader("content-type", "application/json")
			  .addHeader("cache-control", "no-cache")
			  .addHeader("postman-token", "83c10249-87e8-f755-1915-e2d6119e0e4a")
			  .build();
			Response response = client.newCall(request).execute();
			
			//Parse response into array
			List<CorporateEvent> listEvents = new ArrayList<CorporateEvent>(); 
			JSONArray jsonArray = new JSONArray(response.body().string());
			JSONObject jsonObject = new JSONObject();
			for (int i = 0; i < jsonArray.length(); i++) {
			    jsonObject = jsonArray.getJSONObject(i);
			    listEvents.add(new CorporateEvent(jsonObject.getString("startdate"), jsonObject.getString("enddate"), jsonObject.getString("event"), jsonObject.getString("category"), jsonObject.getString("sector"))); 
			}

			return listEvents;
			
		} 
		catch(IOException e) {
			System.out.println("Error loading events: " + e.getMessage());
			return new ArrayList<CorporateEvent>(); 
		}
		catch (JSONException e) {
			System.out.println("Error loading events: " + e.getMessage());
			return new ArrayList<CorporateEvent>(); 
		}		
	}
	
	public static ArrayList<Comment> getComments() {
		
		try {
		
			//Request comment list from Flow
			OkHttpClient client = new OkHttpClient();
			MediaType mediaType = MediaType.parse("application/json");
			RequestBody body = RequestBody.create(mediaType, "null");
			Request request = new Request.Builder()
			  .url(requestUrl + "all-comments")
			  .post(body)
			  .addHeader("content-type", "application/json")
			  .addHeader("cache-control", "no-cache")
			  .addHeader("postman-token", "83c10249-87e8-f755-1915-e2d6119e0e4a")
			  .build();
			Response response = client.newCall(request).execute();
			
			//Parse response into array
			ArrayList<Comment> commentArray = new ArrayList<Comment>(); 
			JSONArray jsonArray = new JSONArray(response.body().string());
			JSONObject jsonObject = new JSONObject();
			for (int i = 0; i < jsonArray.length(); i++) {
			    jsonObject = jsonArray.getJSONObject(i);
			    commentArray.add(new Comment(jsonObject.getInt("id"), jsonObject.getString("RIC"), jsonObject.getString("belongsTo"), jsonObject.getString("body"), jsonObject.getString("link"))); 
			}
			return commentArray;
		} 
		catch(Exception e) {
			System.out.println("Error loading comments: " + e.getMessage());
			return new ArrayList<Comment>(); 
		}
	}
	
	
	public static String getQuote(String symbol, boolean isAndrewMoffatt, boolean isChalk) {
		
		try {
		
			//Request quote from FlowServer
			OkHttpClient client = new OkHttpClient();
			MediaType mediaType = MediaType.parse("application/json");
			RequestBody body = RequestBody.create(mediaType, symbol);
			Request request = new Request.Builder()
			  .url(requestUrl + "quote-symbol?symbol="+symbol)
			  .addHeader("content-type", "application/json")
			  .addHeader("cache-control", "no-cache")
			  .addHeader("postman-token", "83c10249-87e8-f755-1915-e2d6119e0e4a")
			  .build();
			Response response = client.newCall(request).execute();
		
			Double ret;
		    boolean isUp;
		    String dirInd, fontColor;
		    
			JSONObject obj = new JSONObject(response.body().string());
			Double close = obj.getDouble("close");
			Double last = obj.getDouble("last");
			Double bid = obj.getDouble("bid");
			Double ask = obj.getDouble("ask");
			
			if (isChalk) { 
				if (last == 0 || last == null || close == 0 || close == null) { return symbol; }
				else { ret = last/close - 1;  }
			} 
			else if (close == 0 || close == null) { ret = (double) 0;}
			  else if (last == 0 || last == null) { ret = (bid+ask)/2/close - 1; }
			  else { ret = last/close - 1; }
			  ret = ret * 100; // percentage terms return
			  isUp = ret >= 0 ? true : false;
			  ret = Math.round(ret * 100.0) / 100.0; // round to 2 decimals
			  dirInd = isUp?"<span style='font-size:9px'>&#9650;</span>":"<span style='font-size:9px'>&#9660;</span>";
			  fontColor = isUp ? (isAndrewMoffatt ? "<font color='#008000'>" : "<font color='#27AE60'>") : (isAndrewMoffatt ? "<font color='#ed1b2e'>": "<font color='#C0392B'>");

			return fontColor + symbol + ' '+ dirInd+ ' '+ ret + '%' + "</font>";
			
		} 
		catch(IOException e) {
			System.out.println("Error getting quote: " + e.getMessage());
		}
		catch (JSONException e) {
			System.out.println("Error getting quote: " + e.getMessage());
		}
		return symbol;
	}	
	
	public static void main(String[] args) {
		FlowAdapter.getComments();
	}
	
	
}

