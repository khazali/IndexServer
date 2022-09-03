import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;


public class HttpSender implements Runnable {
    private final int PORT=54543;
    private final String HOST="127.0.0.1";
    private final int MaxBufferSize=8192;
    private InputStream input;
    private OutputStream output;
    private Socket socket; 
    private String ID;
    private String testString; 
    private String outString;
    private boolean DoDividend=false;  
    
    
    public HttpSender(String inTest, boolean inDoDividend) {
        this.testString=inTest;
        this.DoDividend=inDoDividend;        
    }

    public void run() {
        ID="Index_"+String.valueOf(Thread.currentThread().getId());
        String outputString1="""
            {
                \"index\": {
                \"indexName\": \"INDEX_1\",
                \"indexshares\": [
                {
                \"shareName\": \"A.OQ\",
                \"sharePrice\": 10.0,
                \"numberOfshares\": 20.0
                },
                {
                \"shareName\": \"B.OQ\",
                \"sharePrice\": 20.0,
                \"numberOfshares\": 30.0
                },
                {
                \"shareName\": \"C.OQ\",
                \"sharePrice\": 30.0,
                \"numberOfshares\": 40.0
                },
                {
                \"shareName\": \"D.OQ\",
                \"sharePrice\": 40.0,
                \"numberOfshares\": 50.0
                },
                ]
                }
            }
            """;       

        String outputString2="""
            {
                \"additionOperation\": {
                \"shareName\": \"E.OQ\",
                \"sharePrice\": 10,
                \"numberOfshares\": 20,
                \"indexName\": \"INDEX_1\"
                }
            }
            """;
        
        String outputString3="""
            {
                \"deletionOperation\": {
                \"operationType\": \"DELETION\",
                \"shareName\": \"D.OQ\",
                \"indexName\": \"INDEX_1\"
                }
            }
            """;

        String outputString4="""
            {
                \"dividendOperation\": {
                \"operationType\": \"CASH_DIVIDEND\",
                \"shareName\": \"A.OQ\",
                \"dividendValue\": 2.0
                }
            }
            """;

        try {
			this.socket = new Socket(this.HOST, this.PORT);
            this.input = this.socket.getInputStream();
		    this.output = this.socket.getOutputStream();

            SendData("POST", "/create", outputString1);
            ReceiveData();
            this.socket.close();

            this.socket = new Socket(this.HOST, this.PORT);
            this.input = this.socket.getInputStream();
		    this.output = this.socket.getOutputStream();
            SendData("POST", "/indexAdjustment", outputString2);
            ReceiveData();
            this.socket.close();

            this.socket = new Socket(this.HOST, this.PORT);
            this.input = this.socket.getInputStream();
		    this.output = this.socket.getOutputStream();
            SendData("POST", "/indexAdjustment", outputString3);
            ReceiveData();
            this.socket.close();

            if (this.DoDividend) {
                this.socket = new Socket(this.HOST, this.PORT);
                this.input = this.socket.getInputStream();
                this.output = this.socket.getOutputStream();
                SendData("POST", "/indexAdjustment", outputString4);
                ReceiveData();
                this.socket.close();
            }

            this.socket = new Socket(this.HOST, this.PORT);
            this.input = this.socket.getInputStream();
		    this.output = this.socket.getOutputStream();
            SendData("GET", "/indexState/"+ID, "");
            ReceiveData();
            //if (this.testString!=null) System.out.println(this.outString);
            if ((this.testString!=null) && (this.outString.equals(this.testString))) System.out.println("Thread "+String.valueOf(Thread.currentThread().getId())+": Success!");
            this.socket.close();

		} catch (Exception e) {
			System.out.println("Error in thread "+String.valueOf(Thread.currentThread().getId())+": "+e.getMessage());
			//System.exit(0);
		}		
    }

    public void SendData(String method, String context, String json) throws IOException {
        String sentjson=json.replaceAll("INDEX_1", ID);
        int l=0;
        String HEAD=method+" "+context+" HTTP/1.1\n";
        String ADDRESS="Host: "+this.HOST+":"+String.valueOf(this.PORT)+"\n";
        String CLIENT="User-Agent: Java_Multithread_HTTP_Client\n";
        String ACCEPT="Accept: application/json\n";
        String CONNECTION="Connection: close\n";
        String CONTENT_TYPE="Content-Type: application/json\n";
		if (sentjson!=null) l=sentjson.length();
		String LENGTH="Content-Length: "+l+"\n\n";

        String header=HEAD+ADDRESS+CLIENT+ACCEPT+CONNECTION+CONTENT_TYPE+LENGTH;
		this.output.write(header.getBytes());
		if (l!=0) this.output.write(sentjson.getBytes());
		
		this.output.flush();
    }

    public void ReceiveData() throws Exception {
        String data="";
        int record=0;
        char ch;
        char[] Len= new char[14];
        while (this.input.available() == 0) {
            Thread.sleep(100L);
        }
        BufferedReader bf=new BufferedReader(new InputStreamReader(this.input));
        int j=0;
        int k=0;
        boolean readLength=false;
        String Leng="";
        boolean checkLen=true;
        int ContentLength=0;

        do {
            if ((!readLength) && (!checkLen)) {
                if (ContentLength==0) break;
                else ContentLength--;
            }
            ch=(char)(bf.read());
            j++;

            

            if (readLength) {
                checkLen=false;
                if (Character.isDigit(ch)) Leng=Leng+ch;
                else if ((ch!=':') && (ch!=' ')){
                    readLength=false;
                    ContentLength=Integer.parseInt(Leng)+165;       //165 is the approximate header size
                    if (ContentLength==165) break;                  //This prevents reading the garbage in the buffer
                }
            }
            if (checkLen) {
                for (k=0; k<13; k++) Len[k]=Len[k+1];
                Len[13]=ch;
                if ((Len[0]=='C') && (Len[1]=='o') && (Len[2]=='n') && (Len[3]=='t') && (Len[4]=='e') && (Len[5]=='n') && (Len[6]=='t') && (Len[7]=='-') && (Len[8]=='L') && (Len[9]=='e') && (Len[10]=='n') && (Len[11]=='g') && (Len[12]=='t') && (Len[13]=='h')) readLength=true;
            }

            if (ch=='{') record++;
            //if (record!=0) data=data+ch;
            data=data+ch;
            if (ch=='}') {
                record--;
                if (record==0) break;
            }            				
        } while (j<this.MaxBufferSize);
        
        String results=data.replaceAll(ID, "INDEX_1");
        String[] lines=results.split("\n");
        for (int i=0; i<lines.length; i++){
            if ((lines[i].startsWith("Date:")) || (lines[i].startsWith("Content-Length:"))){
                lines[i]="";
            }
        }
        StringBuilder finalStringBuilder=new StringBuilder("");
        for (String s:lines){
        if (!s.equals("")){
            finalStringBuilder.append(s).append(System.getProperty("line.separator"));
            }
        }
        this.outString=finalStringBuilder.toString();       
    }

    public String GetOutString() {
        return this.outString;
    }

}