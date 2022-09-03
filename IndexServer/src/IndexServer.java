import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



public class IndexServer {
	private static int PORT=54543;
	private ServerSocket serverSocket;
	private IndexHolder Indices=new IndexHolder();	
	
	public static void main(String[] args) {
		System.out.println("Index Server started!");	
		
		//Start the server!
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
			HttpHandler connection=new HttpHandler(socket, Indices);		//Attend to the connection/request

			/*
				The server is run in parallel using the threads model. Therefore, it is imperative to keep the data in check and to ensure the consistency of the system.
				It is tried to implement the algorithm with minimum possible locks to increase the performance.
				However, every thread which tries to change the shared data status, or wants to see the status, has to require the lock from other threads which are changing the system.
				"synchronized" keyword in critical methods is used to achieve such consistency and thread-safety.
				Every possible action which does not deal with shared data was transferred to the "threaded" part of the code. The code follows the OOP design principle as much as possible.
			*/

			Thread request=new Thread(connection);
			request.start();
		} while (socket.isConnected());
	}
}
