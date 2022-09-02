import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



public class Server {
	private static int PORT=54543;
	private ServerSocket serverSocket;
	private IndexHolder Indices=new IndexHolder();	
	
	public static void main(String[] args) {		
		try {	
			Server server=new Server();
			server.start();			
		} catch (IOException e) {
			System.err.println("Error occured:"+e.getMessage());
			System.exit(0);
		}
	}
	
	public Server() throws IOException {
			serverSocket=new ServerSocket(PORT);		
	}
	
	private void start() throws IOException {
		while (true) {
			Socket socket=serverSocket.accept();
			HttpHandler connection=new HttpHandler(socket, Indices);

			Thread request=new Thread(connection);
			request.start();
		}
	}
}
