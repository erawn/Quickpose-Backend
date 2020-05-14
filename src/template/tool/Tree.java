package template.tool;

import java.util.List;

public class Tree {

	public Node root;
	
	public Tree(Data d) {
		root = new Node(d,null);
	}
	
	public List<Node> getList(){
		return root.allChildren;
	}
}
