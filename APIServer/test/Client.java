public class Client {	
	public static void main(String[] args) {
        System.out.println("Client Start!\n\n\n\n\n");		
        for (int i=0; i<4; i++) {            
            Thread send=new Thread(new HttpSender());
            send.start();
        }
	}	
}
