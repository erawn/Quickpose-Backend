package template.tool;

import java.util.List;
import org.json.*;

public class Tree {

	public Node root;
	public int curId = 0;

	public Tree(Data d) {
		root = new Node(d, this, null, 0);
	}

	public List<Node> getList() {
		return root.allChildren;
	}

	public Node getNode(int id) {
		if (id == 0) {
			return root;
		} else {
			for (int i = 0; i < root.allChildren.size(); i++) {
				if (root.allChildren.get(i).id == id) {
					return root.allChildren.get(i);
				}
			}
			return null;
		}

	}

	public boolean idExists(int id) {
		if (id == 0) {
			return true;
		} else {
			for (Node i : root.allChildren) {
				if (i.id == id) {
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
		for (Node n : root.children) {
			all += "----- Child ID:" + n.id + "\n";
		}
		for (Node n : root.allChildren) {
			all += "Node : " + n.id + "\n";
			for (Node j : n.children) {
				all += "----- Child ID:" + j.id + "\n";
			}
		}
		return all;
	}

	public String getJSONSave(int currentNodeId, String projectName, String usageDataID) {
		try {
			JSONObject Graph = new JSONObject();
			JSONArray Nodes = new JSONArray();
			JSONArray Edges = new JSONArray();
			JSONObject jRoot = new JSONObject();
			jRoot.put("id", Integer.toString(root.id));
			jRoot.put("label", "Node" + Integer.toString(root.id));
			jRoot.put("path", root.data.path);
			jRoot.put("caretPosition",root.data.getCaretPosition());
			jRoot.put("checkpoints",root.data.checkpoints);
			Nodes.put(jRoot);
			for (int j = 0; j < root.children.size(); j++) {
				Node c = root.children.get(j);
				JSONObject edge = new JSONObject();
				edge.put("target", Integer.toString(c.id));
				edge.put("source", Integer.toString(root.id));
				Edges.put(edge);
			}

			for (int i = 0; i < root.allChildren.size(); i++) {
				Node n = root.allChildren.get(i);
				JSONObject node = new JSONObject();
				node.put("id", Integer.toString(n.id));
				node.put("label", "Node" + Integer.toString(n.id));
				node.put("path", n.data.path);
				node.put("caretPosition", n.data.getCaretPosition());
				node.put("checkpoints",n.data.checkpoints);
				Nodes.put(node);
				for (int j = 0; j < n.children.size(); j++) {
					Node c = n.children.get(j);
					JSONObject edge = new JSONObject();
					edge.put("target", Integer.toString(c.id));
					edge.put("source", Integer.toString(n.id));
					Edges.put(edge);
				}
			}
			Graph.put("Nodes", Nodes);
			Graph.put("Edges", Edges);
			Graph.put("CurrentNode", currentNodeId);
			Graph.put("ProjectName", projectName);
			Graph.put("AnalyticsID", usageDataID);
			return Graph.toString();
		} catch (Error e) {
			Utils.getLogger().error(e.getMessage());
		}
		return "";
	}
}
