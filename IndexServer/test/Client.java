public class Client {	
	public static void main(String[] args) {
        HttpSender client;
        String result;

        System.out.println("Client Started!\n\n");        

        client=new HttpSender(null, false);
        client.run();        
        result=client.GetOutString();
        //System.out.println(result);

        for (int i=0; i<50; i++) {            
            Thread send=new Thread(new HttpSender(result, false));
            send.start();
        }

        client=new HttpSender(null, true);
        client.run();        
        result=client.GetOutString();
        System.out.println(result);
	}	
}
