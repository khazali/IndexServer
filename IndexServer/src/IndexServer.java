import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



public class IndexServer {
	private static int PORT=54543;
	private ServerSocket serverSocket;
	private IndexHolder Indices=new IndexHolder();	
	
	public static void main(String[] args) {
		System.out.println("Index Server started!");		
		try {	
			IndexServer server=new IndexServer();
			server.start();			
		} catch (IOException e) {
			System.err.println("Error occured:"+e.getMessage());
			System.exit(0);
		}
	}
	
	public IndexServer() throws IOException {
			serverSocket=new ServerSocket(PORT);		
	}
	
	private void start() throws IOException {
		Socket socket;
		do {
			socket=serverSocket.accept();
			HttpHandler connection=new HttpHandler(socket, Indices);

			Thread request=new Thread(connection);
			request.start();
		} while (socket.isConnected());
	}
}
