import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.util.Iterator;


public class HttpHandler implements Runnable {
	private Socket socket;
	private String retCode="200";
	private String OutputString;
	private IndexHolder indices;
	private final int MaxBufferSize=8192;
	
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
				//System.exit(0);
			} catch (IOException e1) {
				System.err.println("Error Closing socket Connection.");
				//System.exit(0);
			}
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
		BufferedReader bf=new BufferedReader(new InputStreamReader(input));
		int j=0;
		int record=0;
		char ch;
		

		do {
			ch=(char)(bf.read());
			j++;
		} while ((Character.isWhitespace(ch)) && (j<this.MaxBufferSize));

		while ((!Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
			method=method+ch;
			ch=(char)(bf.read());
			j++;
		}

		while ((Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
			ch=(char)(bf.read());
			j++;
		}

		while ((!Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
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
			} while (j<this.MaxBufferSize);

			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(sentJSON);

			if (context.equals("/create")) {
				this.retCode=AddIndex(json);
			}
			else if (context.equals("/indexAdjustment")) {
				this.retCode=AdjustIndex(json);
			}
			else this.retCode="400";			
		}

		else if (method.equals("GET")) {
			if (context.equals("/indexState")) {
				GetAllStates();
				this.retCode="200";
			}
			else {
				String[] indexName=context.split("/", 0);
				if ((indexName==null) || (indexName.length!=3)) this.retCode="400";
				else {
					GetState(indexName[2]);
					this.retCode="200";
				}
			}
		}
		else this.retCode="400";
		

		PopulateResponse(output);
	}

	private void PopulateResponse(OutputStream output) throws IOException {
		SimpleDateFormat format=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		int l=0;

		String REQ_FOUND="HTTP/1.0 "+this.retCode+Message();
		String SERVER="Server: HTTP server/0.1\n";
		String DATE="Date: " + format.format(new java.util.Date()) + "\n";
		String CONTENT_TYPE="Content-Type:application/json";
		//String CONTENT_TYPE="Content-Type:TEXT\n";
		if (OutputString!=null) l=OutputString.length();
		String LENGTH="Content-Length: "+l+"\n\n";

		String header=REQ_FOUND+SERVER+DATE+CONTENT_TYPE+LENGTH;
		output.write(header.getBytes());
		if (l!=0) output.write(OutputString.getBytes());
		
		output.flush();
	}


	private String AddIndex(JSONObject json) {
		int i=0;
		String indexName, shareName;
		double price, numberOfShares;

		JSONObject subjson=(JSONObject)json.get("index");
		JSONArray ja=(JSONArray) subjson.get("indexshares");

		indexName=(String) subjson.get("indexName");
		if (indexName==null) return "400";		
		Index newIndex=this.indices.AddIndex(indexName);
		if (newIndex==null) return "409";

		Iterator itr=ja.iterator();
		while (itr.hasNext()) {	
			i++;			
			JSONObject subja=(JSONObject) itr.next();
			shareName=(String) subja.get("shareName");
			if (shareName==null) return "400";
			price=Double.parseDouble((String) ((subja.get("sharePrice")).toString()));
			if (price<0) return "400";
			numberOfShares=Double.parseDouble((String) ((subja.get("numberOfshares")).toString()));
			if (numberOfShares<0) return "400";
			if ((newIndex.AddShareCreate(shareName,  price, numberOfShares)).equals("202")) return "400";
		}
		if (i<2) {
			this.indices.Pop(indexName);
			return "400";
		}
		newIndex.CalculateIndex();
		return "201";
	}

	private String AdjustIndex(JSONObject json) {
		Iterator itr=json.keySet().iterator();
		if (!(itr.hasNext())) return "400";
		while (itr.hasNext()) {	
			String key=(String) itr.next();
    		JSONObject subjson=(JSONObject) json.get(key);

			if (key.equals("additionOperation")) {
				String indexName=(String) subjson.get("indexName");
				if (indexName==null) return "400";
				Index Index4Edit=this.indices.GetByName(indexName);
				if (Index4Edit==null) return "404";				

				String shareName=(String) subjson.get("shareName");
				if (shareName==null) return "400";
				double price=Double.parseDouble((String) ((subjson.get("sharePrice")).toString()));
				if (price<0) return "400";
				double numberOfShares=Double.parseDouble((String) ((subjson.get("numberOfshares")).toString()));
				if (numberOfShares<0) return "400";

				return Index4Edit.AddShare(shareName,  price, numberOfShares);
			}
			else if (key.equals("deletionOperation")) {
				String indexName=(String) subjson.get("indexName");
				if (indexName==null) return "400";
				Index Index4Edit=this.indices.GetByName(indexName);
				if (Index4Edit==null) return "404";
				String shareName=(String) subjson.get("shareName");
				if (shareName==null) return "400";

				return Index4Edit.RemoveShare(shareName);
			}
			else if (key.equals("dividendOperation")) {
				String shareName=(String) subjson.get("shareName");
				if (shareName==null) return "400";
				double div=Double.parseDouble((String) ((subjson.get("dividendValue")).toString()));
				if (div<0) return "400";

				return this.indices.DoDividend(shareName, div);
			}
			else return "400";
		}
		return "200";
	}

	private void GetAllStates() {
		JSONObject json=this.indices.DoStates();
		this.OutputString=Pretty(json.toString());
	}

	private void GetState(String indexName) {		
		Index index=this.indices.GetByName(indexName); 
		if (index!=null) {
			JSONObject json=new JSONObject();
			JSONObject subjson=index.GetState();
			json.put("indexDetails", subjson);
			this.OutputString=Pretty(json.toString());
		}
	}

	private String Pretty(String jsonString) {
		int count=0;
		int s=jsonString.length();
		int i, j;
		String out="";
		char ch, ch1;

		for (i=0; i<s; i++) {
			ch=jsonString.charAt(i);
			if ((i+1)<s) ch1=jsonString.charAt(i+1);
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

	private String Message() {
		if (this.retCode.equals("200")) return " OK\n";
		else if (this.retCode.equals("201")) return " Created\n";
		else if (this.retCode.equals("202")) return " Accepted\n";
		else if (this.retCode.equals("400")) return " Bad Request\n";
		else if (this.retCode.equals("401")) return " Unauthorized\n";
		else if (this.retCode.equals("404")) return " Not Found\n";
		else if (this.retCode.equals("405")) return " Method Not Allowed\n";
		else if (this.retCode.equals("409")) return " Conflict\n";
		else return null;
	}
}


