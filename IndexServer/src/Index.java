import java.util.HashMap;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;



public class Index {
    private HashMap<String, Integer> namesHash = new HashMap<String, Integer>();
    private ArrayList<Share> shares = new ArrayList<Share>();
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
    
    public double CalculateIndex() {
        int s=this.shares.size();
        int i;

        for (i=0; i<s; i++) this.indexValue+=this.shares.get(i).GetIndexValue();
        for (i=0; i<s; i++) this.shares.get(i).CalcWeightPct(this.indexValue);
        return this.indexValue;
    }

    public String AddShareCreate(String name, double price, double number) {
        if (this.namesHash.get(name)==null) {
            Integer s=this.shares.size();
            this.shares.add(new Share(name, price, number));
            this.namesHash.put(name, s);
            return "201";
        }
        else return "202";
    }

    public synchronized String AddShare(String name, double price, double number) {
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

    public synchronized String RemoveShare(String name) {
        Integer id=this.namesHash.get(name);
        Integer s=this.shares.size();
        if (s<3) return "400";
        if (id!=null) {
            int i;
            int d=id.intValue();
            double r=(this.shares.get(d)).GetIndexValue();
            this.shares.remove(d);
            this.namesHash.remove(name);
            double factor=this.indexValue/(this.indexValue-r);           
            for (i=0; i<s; i++) this.shares.get(i).MultIndexValue(factor); 
            return "200";
        }
        else return "401";
    }

    public synchronized String Dividend(String name, double div) {
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

    public synchronized JSONObject GetState() {
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