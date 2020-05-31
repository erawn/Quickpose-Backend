package template.tool;

import java.util.List;
import org.json.*;
import java.io.FileWriter;
import java.io.IOException;
public class Tree {

	public Node root;
	public int curId = 0;
	
	public Tree(Data d) {
		root = new Node(d,this,null);
	}
	
	public List<Node> getList(){
		return root.allChildren;
	}
	
	
	
	public Node getNode(int id) {
		for(int i = 0; i < root.allChildren.size(); i++) {
			if(root.allChildren.get(i).id == id) {
				return root.allChildren.get(i);
			}
		}
		return null;
	}
	public boolean idExists(int id) {
		System.out.println(root.allChildren.size());
		for(Node i : root.allChildren) {
			if(i.id == id) {
				return true;
			}
		}
		return false;
	}
	public String toString() {
		String all = "";
		for(int i = 0; i < root.allChildren.size(); i++) {
			Node n = root.allChildren.get(i);
			all += "Node : " + n.id + "\n";
			all += "Children of Node : " + n.id + "\n";
			for(int j = 0; j < n.children.size(); j++) {
				all += "----- Child ID:" + n.children.get(j) + "\n"; 
			}
		}
		return all;
	}
	
	public String getJSON() throws JSONException{
		JSONArray Graph = new JSONArray();
		JSONArray Nodes = new JSONArray();
		JSONArray Edges = new JSONArray();
		for(int i = 0; i < root.allChildren.size(); i++) {
			Node n = root.allChildren.get(i);
			JSONObject node = new JSONObject();
			node.put("id", Integer.toString(n.id));
			node.put("group", 0);
			node.put("label", "Node" + Integer.toString(n.id));
			node.put("level", 0);
			Nodes.put(node);
			for(int j = 0; j < n.children.size(); j++) {
				 Node c = n.children.get(j);
				 JSONObject edge = new JSONObject();
				 edge.put("target", Integer.toString(c.id));
				 edge.put("source", Integer.toString(n.id));
				 edge.put("strength", 0.1);
				 Edges.put(edge);
			}
			
		}
		Graph.put(Nodes);
		Graph.put(Edges);
		
        return Graph.toString();
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
	
		Edges.put(Edge1);
		Edges.put(Edge2);

		Graph.put(Nodes);
		Graph.put(Edges);
		return Graph.toString();
	
	}
}
