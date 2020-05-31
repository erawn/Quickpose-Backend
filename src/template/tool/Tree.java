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
}
