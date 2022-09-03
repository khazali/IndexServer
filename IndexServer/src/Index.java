import java.util.HashMap;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;



public class Index {
    private HashMap<String, Integer> namesHash = new HashMap<String, Integer>();                               //Map to translate the share name to its index in the list                           
    private ArrayList<Share> shares = new ArrayList<Share>();                                                  //Unsynchronized list of shares. It helps us to increase performance. The thread-safety is achieved by synchronized methods.
    private String indexName;
    private double indexValue;


    public Index() {
        super();
    }

    public Index(String name) {
        this.indexName=name;
    }

    public void SetIndexName(String name) {
        this.indexName=name;
    }
    public String GetIndexName() {
        return this.indexName;
    }
    
    public double CalculateIndex() {                                                                            //Calculates total index value and the weight percent of each share
        int s=this.shares.size();
        int i;

        for (i=0; i<s; i++) this.indexValue+=this.shares.get(i).GetIndexValue();
        for (i=0; i<s; i++) this.shares.get(i).CalcWeightPct(this.indexValue);
        return this.indexValue;
    }

    public String AddShareCreate(String name, double price, double number) {                                    //Validates the input parameters and adds a share to a new index.
        if (this.namesHash.get(name)==null) {
            Integer s=this.shares.size();
            this.shares.add(new Share(name, price, number));
            this.namesHash.put(name, s);
            return "201";
        }
        else return "202";
    }

    public synchronized String AddShare(String name, double price, double number) {                             //Validates the input parameters, readjusts the index, and adds a share.
        if (this.namesHash.get(name)==null) {
            int i;
            Integer s=this.shares.size();
            Share newShare=new Share(name, price, number);
            newShare.CalcWeightPct(this.indexValue);
            double factor=(this.indexValue-newShare.GetIndexValue())/this.indexValue;
            for (i=0; i<s; i++) this.shares.get(i).MultIndexValue(factor);            
            this.shares.add(newShare);
            this.namesHash.put(name, s);
            
            return "201";
        }
        else return "202";
    }

    public synchronized String RemoveShare(String name) {                                           //Removes a share from an index that contains more than two shares. Returns appropriate codes for the performed task.
        Integer id=this.namesHash.get(name);
        if (id==null) return "401";
        Integer s=this.shares.size()-1;
        if (s<2) return "405";
        
        int i;
        int d=id.intValue();
        double r=(this.shares.get(d)).GetIndexValue();
        this.shares.remove(d);
        this.namesHash.remove(name);
        double factor=this.indexValue/(this.indexValue-r);           
        for (i=0; i<s; i++) this.shares.get(i).MultIndexValue(factor); 
        return "200";
       
    }

    public synchronized String Dividend(String name, double div) {                                  //If the requested share is available in this index, the share price is reduced by the dividend value, and the index is readjusted.
        double factor;
        int i;
        Integer sn=this.namesHash.get(name);
        if (sn!=null) {
            double diff=(this.shares.get(sn.intValue())).GetSharePrice()-div;
            if (diff>0) {
                double r=(this.shares.get(sn.intValue())).SetDiffPrice(diff);
                factor=this.indexValue/(this.indexValue-r);
                this.shares.get(sn.intValue()).CalcWeightPct(this.indexValue);
                Integer s=this.shares.size();
                for (i=0; i<s; i++) (this.shares.get(i)).MultIndexValue(factor);                
                
                return "200";
            }
            else return "400";
        }
        else return "401";
    }

    public synchronized JSONObject GetState() {                                                                 //Returns the state of the index in a JSON object
        int s=this.shares.size();
        int i;
        JSONArray ja=new JSONArray();
        JSONObject json=new JSONObject();
        
        json.put("indexName", this.indexName);
        json.put("indexValue", String.valueOf(this.indexValue));
        for (i=0; i<s; i++) {
            Map m= new LinkedHashMap(5);
            m.put("shareName", (this.shares.get(i)).GetShareName());
            m.put("sharePrice", String.valueOf((this.shares.get(i)).GetSharePrice()));
            m.put("numberOfshares", String.valueOf((this.shares.get(i)).GetNumberOfShares()));
            m.put("indexWeightPct", String.valueOf((this.shares.get(i)).GetIndexWeightPct()));
            m.put("indexValue", String.valueOf((this.shares.get(i)).GetIndexValue()));            
            ja.add(m);
        }
        json.put("indexMembers", ja);

        return json;
    }

}