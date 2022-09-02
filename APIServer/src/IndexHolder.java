import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.Vector;
import java.util.HashMap;


public class IndexHolder {
    private Vector<Index> indices = new Vector<Index>();
    private HashMap<String, Integer> namesHash = new HashMap<String, Integer>();

    public IndexHolder() {
        super();
    }

    public synchronized void Pop(String name) {
        this.indices.remove(indices.size()-1);
        this.namesHash.remove(name);
    }

    public synchronized Index AddIndex(String name) {        
        if (this.namesHash.get(name)==null) {
            Integer s=this.indices.size();
            Index newIndex=new Index();
            newIndex.SetIndexName(name);
            this.indices.addElement(newIndex);
            //this.indices.addElement(new Index());
            this.namesHash.put(name, s);
            return newIndex;
        }
        else return null;
    }

    public Index GetByName(String name) {
        Integer s=this.namesHash.get(name);
        if (s!=null) {
            int i=s.intValue();
            if (i<this.indices.size()) return this.indices.get(i);
            else return null;
        }
        else return null;        
    }

    public Index GetByIndex(int index) {
        if ((index>0) && (index<this.indices.size())) return this.indices.get(index);
        else return null;
    }

    public synchronized String DoDividend(String name, double div) {
        int i;
        String retCode="401";
        String ret;

        for (i=0; i<this.indices.size(); i++) {
            ret=(this.indices.get(i)).Dividend(name, div);
            if (ret.equals("400")) return "400";
            else if  (ret.equals("200")) retCode="200";            
        } 
        return retCode;
    }

    public synchronized JSONObject DoStates() {
        int i;
        JSONObject json=new JSONObject();
        JSONArray ja=new JSONArray();        

        for (i=0; i<this.indices.size(); i++) ja.add((this.indices.get(i)).GetState());
        json.put("indexDetails", ja);
        return json;
    }
}