
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;



public class IndexHolder {
    Vector<Index> vec = new Vector<Index>();

    IndexHolder(String context, JSONObject json) {
        ProcessInput(context, json);
    }

    int ProcessInput(String context, JSONObject json) {
        if (context.equals("/create")) {
            


        }
        return 200;
    }
}