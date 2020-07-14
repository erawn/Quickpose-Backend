package template.tool;

import java.util.List;
import org.json.*;
import java.io.FileWriter;
import java.io.IOException;
public class Tree {

	public Node root;
	public int curId = 0;
	
	public Tree(Data d) {
		root = new Node(d,this,null,0);
	}
	
	public List<Node> getList(){
		return root.allChildren;
	}
	
	
	
	public Node getNode(int id) {
		if(id == 0) {
			return root;
		}else {
			for(int i = 0; i < root.allChildren.size(); i++) {
				if(root.allChildren.get(i).id == id) {
					return root.allChildren.get(i);
				}
			}
			return null;
		}
		
	}
	public boolean idExists(int id) {
		if(id == 0) {
			return true;
		}else {
			for(Node i : root.allChildren) {
				if(i.id == id) {
					return true;
				}
			}
		}
		return false;
	}
	public void printAll() {
		System.out.println(root.id);
	}
	public String toString() {
		String all = "";
		all += "Root :" + root.id + "\n";
		for(Node n :root.children) {
			all += "----- Child ID:" + n.id + "\n";
		}
		for(Node n : root.allChildren) {
			all += "Node : " + n.id + "\n";
			for(Node j : n.children) {
				all += "----- Child ID:" + j.id + "\n"; 
			}
		}
		return all; 
	}
	
	public String getJSON(){
		try {
			JSONArray Graph = new JSONArray();
			JSONArray Nodes = new JSONArray();
			JSONArray Edges = new JSONArray();
			JSONObject jRoot = new JSONObject();
			jRoot.put("id", Integer.toString(root.id));
			jRoot.put("group", 0);
			jRoot.put("label", "Node" + Integer.toString(root.id));
			jRoot.put("level", 0);
			Nodes.put(jRoot);
			for(int j = 0; j < root.children.size(); j++) {
				 Node c = root.children.get(j);
				 JSONObject edge = new JSONObject();
				 edge.put("target", Integer.toString(c.id));
				 edge.put("source", Integer.toString(root.id));
				 edge.put("strength", 0.1);
				 Edges.put(edge);
			}
			
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
		} catch (Exception e) {
			System.out.println("Exception Occured : getJSON()");
		}
		return "";
    }
	public String getJSONSave(){
		try {
			JSONObject Graph = new JSONObject();
			JSONArray Nodes = new JSONArray();
			JSONArray Edges = new JSONArray();
			JSONObject jRoot = new JSONObject();
			jRoot.put("id", Integer.toString(root.id));
			jRoot.put("label", "Node" + Integer.toString(root.id));
			jRoot.put("path", root.data.path);
			Nodes.put(jRoot);
			for(int j = 0; j < root.children.size(); j++) {
				 Node c = root.children.get(j);
				 JSONObject edge = new JSONObject();
				 edge.put("target", Integer.toString(c.id));
				 edge.put("source", Integer.toString(root.id));
				 Edges.put(edge);
			}
			
			for(int i = 0; i < root.allChildren.size(); i++) {
				Node n = root.allChildren.get(i);
				JSONObject node = new JSONObject();
				node.put("id", Integer.toString(n.id));
				node.put("label", "Node" + Integer.toString(n.id));
				node.put("path", n.data.path);
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
			Graph.put("Nodes", Nodes);
			Graph.put("Edges", Edges);
			
	        return Graph.toString();
		} catch (Exception e) {
			System.out.println("Exception Occured : getJSON()");
		}
		return "";
    }
	public String getJSONTest(){
		try {
			JSONArray Graph = new JSONArray();
			JSONArray Nodes = new JSONArray();
			JSONArray Edges = new JSONArray();
			JSONObject Node1 = new JSONObject();
			Node1.put("id", "0");
			Node1.put("group", 0);
			Node1.put("label", "Two");
			Node1.put("level", 0);
			JSONObject Node2 = new JSONObject();
			Node2.put("id", "1");
			Node2.put("group", 0);
			Node2.put("label", "One");
			Node2.put("level", 1);
			JSONObject Node3 = new JSONObject();
			Node3.put("id", "2");
			Node3.put("group", 0);
			Node3.put("label", "Three");
			Node3.put("level", 1);
			JSONObject Node4 = new JSONObject();
			Node4.put("id", "3");
			Node4.put("group", 0);
			Node4.put("label", "Four");
			Node4.put("level", 1);
			JSONObject Node5 = new JSONObject();
			Node5.put("id", "4");
			Node5.put("group", 0);
			Node5.put("label", "Five");
			Node5.put("level", 1);
			JSONObject Node6 = new JSONObject();
			Node6.put("id", "5");
			Node6.put("group", 0);
			Node6.put("label", "Six");
			Node6.put("level", 1);
			Nodes.put(Node1);
			Nodes.put(Node2);
			Nodes.put(Node3);
			Nodes.put(Node4);
			Nodes.put(Node5);
			//Nodes.put(Node6);
			
			JSONObject Edge1 = new JSONObject();
			Edge1.put("target", "0");
			Edge1.put("source", "1");
			Edge1.put("strength", 0.9);
			JSONObject Edge2 = new JSONObject();
			Edge2.put("target", "0");
			Edge2.put("source", "2");
			Edge2.put("strength", .9);
			JSONObject Edge3 = new JSONObject();
			Edge3.put("target", "2");
			Edge3.put("source", "3");
			Edge3.put("strength", .9);
			JSONObject Edge4= new JSONObject();
			Edge4.put("target", "2");
			Edge4.put("source", "4");
			Edge4.put("strength", .9);
			JSONObject Edge5 = new JSONObject();
			Edge1.put("target", "3");
			Edge1.put("source", "5");
			Edge1.put("strength", 0.9);
		
			Edges.put(Edge1);
			Edges.put(Edge2);
			Edges.put(Edge3);
			Edges.put(Edge4);
			//Edges.put(Edge4);
	
			Graph.put(Nodes);
			Graph.put(Edges);
			return Graph.toString();
		} catch (Exception e) {
			
		}
		return "";
	}
}
