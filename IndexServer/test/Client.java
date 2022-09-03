public class Client {	
	public static void main(String[] args) {
        HttpSender client;
        String result;

        System.out.println("Client Started!\n\n");        

        client=new HttpSender(null, false);
        client.run();        
        result=client.GetOutString();
        //System.out.println(result);


        //Testing the server with multiple parallel similar requests
        for (int i=0; i<50; i++) {            
            Thread send=new Thread(new HttpSender(result, false));
            send.start();
        }


        //Printing the results of the Solactive sample
        client=new HttpSender(null, true);
        client.run();        
        result=client.GetOutString();
        System.out.println(result);
	}	
}
