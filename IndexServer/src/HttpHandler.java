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
	private String outputString;
	private IndexHolder indices;					//Holds the index list wrapper
	private final int MaxBufferSize=8192;			//Maximum input buffer size. Its default is 8192
	
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
			} catch (IOException e1) {
				System.err.println("Error Closing socket Connection.");
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
		int k=0;
        boolean readLength=false;
        String Leng="";
        boolean checkLen=true;
        int ContentLength=0;
		char[] Len= new char[14];
		
		//High-performance buffer parsing
		do {
			ch=(char)(bf.read());
			j++;
		} while ((Character.isWhitespace(ch)) && (j<this.MaxBufferSize));


		//Extract the method
		while ((!Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
			method=method+ch;
			ch=(char)(bf.read());
			j++;
		}

		while ((Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
			ch=(char)(bf.read());
			j++;
		}

		//Extract the context
		while ((!Character.isWhitespace(ch)) && (j<this.MaxBufferSize)) {			
			context=context+ch;
			ch=(char)(bf.read());
			j++;
		}

		if (method.equals("POST")) {
			//Extract the JSON string
			do {
				if (readLength) {
					checkLen=false;
					if (Character.isDigit(ch)) Leng=Leng+ch;
					else if ((ch!=':') && (ch!=' ')){
						readLength=false;
						ContentLength=Integer.parseInt(Leng);
						if (ContentLength==0) break;		//Protects against empty data parsing                 
					}
				}
				if (checkLen) {			//Find Content-Length value
					for (k=0; k<13; k++) Len[k]=Len[k+1];
					Len[13]=ch;
					if ((Len[0]=='C') && (Len[1]=='o') && (Len[2]=='n') && (Len[3]=='t') && (Len[4]=='e') && (Len[5]=='n') && (Len[6]=='t') && (Len[7]=='-') && (Len[8]=='L') && (Len[9]=='e') && (Len[10]=='n') && (Len[11]=='g') && (Len[12]=='t') && (Len[13]=='h')) readLength=true;
				}

				if (ch=='{') record++;
				if (record!=0) sentJSON=sentJSON+ch;
				if (ch=='}') {
					record--;
					if (record==0) break;
				}
				
				ch=(char)(bf.read());
				j++;				
			} while (j<this.MaxBufferSize);

			if ((sentJSON==null) || (sentJSON.length()==0)) this.retCode="400";			//Checks the validation of JSON string
			else {
				JSONParser parser = new JSONParser();
				JSONObject json=null;
				boolean nparsed=false;
				try {
					json = (JSONObject) parser.parse(sentJSON);
				}
				catch (ParseException ex) {
					nparsed=true;
				}

				if (nparsed) retCode="400";
				else if (context.equals("/create")) {
					this.retCode=AddIndex(json);					//Create index
				}
				else if (context.equals("/indexAdjustment")) {
					this.retCode=AdjustIndex(json);					//Adjustments
				}
				else this.retCode="400";
			}			
		}

		else if (method.equals("GET")) {
			if (context.equals("/indexState")) {
				GetAllStates();									//Get the state of all indices
				this.retCode="200";
			}
			else {
				String[] indexName=context.split("/", 0);
				if ((indexName==null) || (indexName.length!=3)) this.retCode="400";
				else {
					GetState(indexName[2]);					//Get the state of an index
					this.retCode="200";
				}
			}
		}
		else this.retCode="400";


		PopulateResponse(output);
	}

	private void PopulateResponse(OutputStream output) throws IOException {
		//Creates the HTTP response

		SimpleDateFormat format=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		int l=0;

		String REQ_FOUND="HTTP/1.0 "+this.retCode+Message();
		String SERVER="Server: HTTP server/0.1\n";
		String DATE="Date: " + format.format(new java.util.Date()) + "\n";
		String CONTENT_TYPE="Content-Type: application/json\n";
		String ACCEPT="Accept: application/json\n";
		if (this.outputString!=null) l=this.outputString.length();
		String LENGTH="Content-Length: "+l+"\n\n";

		String header=REQ_FOUND+SERVER+DATE+ACCEPT+CONTENT_TYPE+LENGTH;
		output.write(header.getBytes());
		if (l!=0) output.write(this.outputString.getBytes());
		
		output.flush();
	}


	private String AddIndex(JSONObject json) {
		int i=0;
		String indexName, shareName;
		double price, numberOfShares;
		boolean isInvalid=false;
		JSONObject subja;


		//Checking the validity of the request parameters
		JSONObject subjson=(JSONObject)json.get("index");
		if (subjson==null) return "400";
		JSONArray ja=(JSONArray) subjson.get("indexshares");
		if (ja==null) return "400";

		indexName=(String) subjson.get("indexName");
		if ((indexName==null) || (indexName.length()==0)) return "400";		
		Index newIndex=this.indices.AddIndex(indexName);
		if (newIndex==null) return "409";

		Iterator itr=ja.iterator();
		while (itr.hasNext()) {						//Get the request details
			i++;			
			subja=(JSONObject) itr.next();

			//Checking and setting the parameters
			shareName=(String) subja.get("shareName");
			if ((shareName==null) || (shareName.length()==0)) {
				isInvalid=true;
				break;
			}
			try {
				price=Double.parseDouble((String) ((subja.get("sharePrice")).toString()));
			}
			catch (NumberFormatException ex) {
				isInvalid=true;
				break;
			}
			if (price<0) {
				isInvalid=true;
				break;
			}
			try {
				numberOfShares=Double.parseDouble((String) ((subja.get("numberOfshares")).toString()));
			}
			catch (NumberFormatException ex) {
				isInvalid=true;
				break;
			}
			if (numberOfShares<0) {
				isInvalid=true;
				break;
			}
			if ((newIndex.AddShareCreate(shareName,  price, numberOfShares)).equals("202")) {			//Calling the actual function to do the work!
				isInvalid=true;
				break;
			}
		}

		//If any parameter was invalid, the new index must not be added to the index list!
		if ((isInvalid) || (i<2)) {
			this.indices.Pop(indexName);
			return "400";
		}
		newIndex.CalculateIndex();
		return "201";
	}

	private String AdjustIndex(JSONObject json) {						//Handles the adjustment requests. The function first does the validation check of the JSON, then it calls the actual method.
		Iterator itr=json.keySet().iterator();
		double price, numberOfShares, div;
		JSONObject subjson;
		String key;

		if (!(itr.hasNext())) return "400";
		while (itr.hasNext()) {	
			key=(String) itr.next();
    		subjson=(JSONObject) json.get(key);
			if (subjson==null) return "400";

			if (key.equals("additionOperation")) {
				String indexName=(String) subjson.get("indexName");
				if ((indexName==null) || (indexName.length()==0)) return "400";
				Index Index4Edit=this.indices.GetByName(indexName);
				if (Index4Edit==null) return "404";				

				String shareName=(String) subjson.get("shareName");
				if ((shareName==null) || (shareName.length()==0)) return "400";

				try {
					price=Double.parseDouble((String) ((subjson.get("sharePrice")).toString()));
				}
				catch (NumberFormatException ex) {
					return "400";
				}
				if (price<0) return "400";
				
				try {
					numberOfShares=Double.parseDouble((String) ((subjson.get("numberOfshares")).toString()));
				}
				catch (NumberFormatException ex) {
					return "400";
				}
				if (numberOfShares<0) return "400";

				return Index4Edit.AddShare(shareName,  price, numberOfShares);				//The "actual" worker
			}
			else if (key.equals("deletionOperation")) {
				String indexName=(String) subjson.get("indexName");
				if ((indexName==null) || (indexName.length()==0)) return "400";
				Index Index4Edit=this.indices.GetByName(indexName);
				if (Index4Edit==null) return "404";
				String shareName=(String) subjson.get("shareName");
				if ((shareName==null) || (shareName.length()==0)) return "400";

				return Index4Edit.RemoveShare(shareName);								//Removes the share from the selected index
			}
			else if (key.equals("dividendOperation")) {
				String shareName=(String) subjson.get("shareName");
				if ((shareName==null) || (shareName.length()==0)) return "400";

				try {
					div=Double.parseDouble((String) ((subjson.get("dividendValue")).toString()));
				}
				catch (NumberFormatException ex) {
					return "400";
				}
				if (div<=0) return "400";

				return this.indices.DoDividend(shareName, div);						//This method has to be managed by the index list holder since dividends can affect multiple indices.
			}
			else return "400";
		}
		return "200";
	}

	private void GetAllStates() {
		JSONObject json=this.indices.DoStates();
		if (json==null) this.outputString="";
		else this.outputString=Pretty(json.toString());								//This method has to be managed by the index list holder since we require all indices data
	}

	private void GetState(String indexName) {										//We need the data of only one index here, so this method is performed by an index.	
		Index index=this.indices.GetByName(indexName); 
		if (index!=null) {
			JSONObject json=new JSONObject();
			JSONObject subjson=index.GetState();
			json.put("indexDetails", subjson);
			this.outputString=Pretty(json.toString());
		}
	}

	private String Pretty(String jsonString) {										//Formatting the output JSON string. Shockingly, no function is available to do that. So I wrote one here.
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

	private String Message() {																		//Translates HTML code to its equivalent message 
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


