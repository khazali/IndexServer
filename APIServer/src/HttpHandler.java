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


public class HttpHandler implements Runnable {
	private Socket socket;
	//private String res;
	
	public HttpHandler(Socket socket) {
		//this.res = null;
		this.socket = socket;
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

	/**
	 * @throws Exception
	 */
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

	/**
	 * @param input
	 * @param output
	 * @param root 
	 * @throws Exception
	 */
	private void serverRequest(InputStream input, OutputStream output) throws Exception {
		String sentJSON="";
		String method="";
		String context="";
		BufferedReader bf = new BufferedReader(new InputStreamReader(input));
		int j=0;
		boolean record=false;
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
				if (ch=='{') record=true;
				if (record) sentJSON=sentJSON+ch;
				if (ch=='}') break;

				ch=(char)(bf.read());
				j++;				
				
				//if (j==8191) throw Exception;		
			} while (true);
			//curl -X POST -H "Content-Type: application/json" -d "{\"login\":\"my_login\",\"password\":\"my_password\"}" http://127.0.0.1:8080/test
			JSONParser parser = new JSONParser();
			//JSONObject json = (JSONObject) parser.parse("{\"Name\":\"Raja\",\"EmployeeId\":\"115\",\"Age\":\"30\"}");
			JSONObject json = (JSONObject) parser.parse(sentJSON);
			System.out.println((String) json.get("login"));
		}

		populateResponse(output);
	}

	/**
	 * @param resource
	 * @param output
	 * @throws IOException
	 */
	private void populateResponse(OutputStream output) throws IOException {
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));

		String resource="khazal\n";
		String REQ_FOUND = "HTTP/1.0 200 OK\n";
		String SERVER = "Server: HTTP server/0.1\n";
		String DATE = "Date: " + format.format(new java.util.Date()) + "\n";
		//String CONTENT_TYPE = "Content-Type:application/json";
		String CONTENT_TYPE = "Content-Type:TEXT\n";
		String LENGTH = "Content-Length: " + (resource.length()) + "\n\n";

		String header = REQ_FOUND + SERVER + DATE + CONTENT_TYPE + LENGTH;
		output.write(header.getBytes());
		output.write(resource.getBytes());
		
		output.flush();
	}
}