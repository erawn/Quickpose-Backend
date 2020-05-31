package template.tool;

import java.util.List;
import org.json.*;
import java.io.FileWriter;
import java.io.IOException;
public class Tree {

	public Node root;
	public int curId = 0;
	
	public Tree(Data d) {
		root = new Node(d,this);
	}
	
	public List<Node> getList(){
		return root.allChildren;
	}
	
	public String getJson() {
		JSONArray versions = new JSONArray();
		
		
		
		
        return "{ \"hello\" : \"world\"}";
    }
	
	public String getJSONTest() throws JSONException {
		JSONArray Graph = new JSONArray();
		JSONArray Nodes = new JSONArray();
		JSONArray Edges = new JSONArray();
		JSONObject Node1 = new JSONObject();
		Node1.put("id", "0");
		Node1.put("group", 0);
		Node1.put("label", "Node1");
		Node1.put("level", 0);
		JSONObject Node2 = new JSONObject();
		Node2.put("id", "1");
		Node2.put("group", 0);
		Node2.put("label", "Node2");
		Node2.put("level", 1);
		JSONObject Node3 = new JSONObject();
		Node3.put("id", "2");
		Node3.put("group", 0);
		Node3.put("label", "Node3");
		Node3.put("level", 1);
		Nodes.put(Node1);
		Nodes.put(Node2);
		Nodes.put(Node3);
		
		JSONObject Edge1 = new JSONObject();
		Edge1.put("target", "0");
		Edge1.put("source", "1");
		Edge1.put("strength", 0.1);
		JSONObject Edge2 = new JSONObject();
		Edge2.put("target", "0");
		Edge2.put("source", "2");
		Edge2.put("strength", .1);
		JSONObject Edge3 = new JSONObject();
		Edge3.put("target", "0");
		Edge3.put("source", "fish");
		Edge3.put("strength", .1);
		
		Edges.put(Edge1);
		Edges.put(Edge2);
		Edges.put(Edge3);
		Graph.put(Nodes);
		Graph.put(Edges);
		return Graph.toString();
	
	}
}
