package template.tool;

import java.util.List;

public class Tree {

	public Node root;
	public int curId = 0;
	
	public Tree(Data d) {
		root = new Node(d,null);
	}
	
	public List<Node> getList(){
		return root.allChildren;
	}
}
