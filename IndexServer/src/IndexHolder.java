import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.Vector;
import java.util.HashMap;


public class IndexHolder {
    private Vector<Index> indices = new Vector<Index>();                                        //Holds the "actual" index list
    private HashMap<String, Integer> namesHash = new HashMap<String, Integer>();                //Tranlate the index name to its index [in the vector]

    public IndexHolder() {
        super();
    }

    public synchronized void Pop(String name) {                     //Removes the index if its parameters are found to be faulty- only employed in the index addition procedure.
        this.indices.remove(this.indices.size()-1);
        this.namesHash.remove(name);
    }

    public synchronized Index AddIndex(String name) {               //Adds an index to the list. Returns null if the index already exists.     
        if (this.namesHash.get(name)==null) {
            Integer s=this.indices.size();
            Index newIndex=new Index();
            newIndex.SetIndexName(name);
            this.indices.addElement(newIndex);
            this.namesHash.put(name, s);
            return newIndex;
        }
        else return null;
    }

    public Index GetByName(String name) {                           //Determines the index of an index by its name :D
        Integer s=this.namesHash.get(name);
        if (s!=null) {
            int i=s.intValue();
            if (i<this.indices.size()) return this.indices.get(i);
            else return null;
        }
        else return null;        
    }

    public Index GetByIndex(int index) {                            //Fetches an index by its index :)))
        if ((index>0) && (index<this.indices.size())) return this.indices.get(index);
        else return null;
    }


    //Two following methods perform collective operations on all indices, which cannot be handled by indices themselves.
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
        int i, s;        
        s=this.indices.size();
        if (s==0) return null;
        else {
            JSONObject json=new JSONObject();
            JSONArray ja=new JSONArray();        
            for (i=0; i<s; i++) ja.add((this.indices.get(i)).GetState());
            json.put("indexDetails", ja);
            return json;
        }
    }
}