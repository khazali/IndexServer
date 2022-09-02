//import java.beans.MethodDescriptor;
import java.io.BufferedReader;
//import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
//import java.net.URLConnection;
//import java.nio.file.Files;
//import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

//import java.util.concurrent.TimeUnit;

import java.util.Iterator;
import java.util.Map;


public class HttpHandler implements Runnable {
	private Socket socket;
	private String retCode;
	private String OutputString;
	private IndexHolder indices;
	
	public HttpHandler(Socket socket, IndexHolder inIndices) {
		this.socket=socket;
		this.indices=inIndices;
	}

	public void run() {
		try {
			handleRequest();
		} catch (Exception e) {
			System.err.println("Error Occured: " + e.getMessage());
			try {
				socket.close();
				System.exit(0);
			} catch (IOException e1) {
				System.err.println("Error Closing socket Connection.");
				System.exit(0);
			}
			System.err.println("Server is Terminating!");
		}
	}


	private void handleRequest() throws Exception {
		InputStream input;
		OutputStream output;
		
		input = socket.getInputStream();
		output = socket.getOutputStream();

		serverRequest(input, output);	

		
		output.close();
		input.close();		
		socket.close();
	}


	private void serverRequest(InputStream input, OutputStream output) throws Exception {
		String sentJSON="";
		String method="";
		String context="";
		BufferedReader bf = new BufferedReader(new InputStreamReader(input));
		int j=0;
		int record=0;
		char ch;

		do {
			ch=(char)(bf.read());
			j++;
		} while (Character.isWhitespace(ch));

		while (!Character.isWhitespace(ch)) {			
			method=method+ch;
			ch=(char)(bf.read());
			j++;
		}

		while (Character.isWhitespace(ch)) {			
			ch=(char)(bf.read());
			j++;
		}

		while (!Character.isWhitespace(ch)) {			
			context=context+ch;
			ch=(char)(bf.read());
			j++;
		}

		if (method.equals("POST")) {
			do {
				if (ch=='{') record++;
				if (record!=0) sentJSON=sentJSON+ch;
				if (ch=='}') {
					record--;
					if (record==0) break;
				}			

				ch=(char)(bf.read());
				j++;				
				
				//if (j==8191) throw Exception;		
			} while (j<8192);

			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(sentJSON);

			if (context.equals("/create")) {
				AddIndex(json);
			}
			else if (context.equals("/indexAdjustment")) {
				AdjustIndex(json);
			}
			//else throw exception 


			
			
			
		}

		else if (method.equals("GET")) {
			if (context.equals("/indexState")) {
				GetAllStates();
			}
			else {
				String[] indexName=context.split("/", 0);
				GetState(indexName[2]);
			}
		}



		populateResponse(output);
	}

	private void populateResponse(OutputStream output) throws IOException {
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		int l=0;

		String REQ_FOUND="HTTP/1.0 200 OK\n";
		String SERVER="Server: HTTP server/0.1\n";
		String DATE="Date: " + format.format(new java.util.Date()) + "\n";
		String CONTENT_TYPE = "Content-Type:application/json";
		//String CONTENT_TYPE="Content-Type:TEXT\n";
		if (OutputString!=null) l=OutputString.length();
		String LENGTH="Content-Length: " + l + "\n\n";

		String header = REQ_FOUND + SERVER + DATE + CONTENT_TYPE + LENGTH;
		output.write(header.getBytes());
		if (l!=0) output.write(OutputString.getBytes());
		
		output.flush();
	}


	private void AddIndex(JSONObject json) {
		JSONObject subjson=(JSONObject)json.get("index");
		JSONArray ja=(JSONArray) subjson.get("indexshares");

		Index newIndex=this.indices.AddIndex(((String) subjson.get("indexName")));
		Iterator itr=ja.iterator();
		while (itr.hasNext()) {				
			JSONObject subja=(JSONObject) itr.next();
			newIndex.AddShareCreate(((String) subja.get("shareName")),  (Double.parseDouble((String) ((subja.get("sharePrice")).toString()))), (Double.parseDouble((String) ((subja.get("numberOfshares")).toString()))));
		}
		newIndex.CalculateIndex();
	}

	private void AdjustIndex(JSONObject json) {
		Iterator itr=json.keySet().iterator();
		while (itr.hasNext()) {	
			String key=(String) itr.next();
    		JSONObject subjson=(JSONObject) json.get(key);

			if (key.equals("additionOperation")) {
				Index Index4Edit=this.indices.GetByName((String) subjson.get("indexName"));
				Index4Edit.AddShare((String) subjson.get("shareName"),  (Double.parseDouble((String) ((subjson.get("sharePrice")).toString()))), (Double.parseDouble((String) ((subjson.get("numberOfshares")).toString()))));
			}
			else if (key.equals("deletionOperation")) {
				Index Index4Edit=this.indices.GetByName((String) subjson.get("indexName"));
				Index4Edit.RemoveShare((String) subjson.get("shareName"));
			}
			else if (key.equals("dividendOperation")) {
				this.indices.DoDividend((String) subjson.get("shareName"),  (Double.parseDouble((String) ((subjson.get("dividendValue")).toString()))));
			}
			//analyse returns
			//else throw

		}
	}

	private void GetAllStates() {
		JSONObject json=this.indices.DoStates();
		this.OutputString=Pretty(json.toString());
	}

	private void GetState(String indexName) {
		JSONObject json=new JSONObject();
		JSONObject subjson=(this.indices.GetByName(indexName)).GetState();
		json.put("indexDetails", subjson);
		this.OutputString=Pretty(json.toString());
	}

	private String Pretty(String jsonString) {
		int count=0;
		int s=jsonString.length();
		int i, j;
		String out="";
		char ch, ch1;

		for (i=0; i<s; i++) {
			ch=jsonString.charAt(i);
			if (i+1<s) ch1=jsonString.charAt(i+1);
			else ch1='\0';

			out=out+ch;

			if ((ch=='{') || (ch=='[')) count++;
			if ((ch1=='}') || (ch1==']')) count--;

			if ((ch=='{') || (ch=='[') || (ch1=='}') || (ch1==']') || (ch==',')) {
				out=out+'\n';
				for (j=0; j<count; j++) out=out+'\t';
			}			
		}
		return out;
	}
}


