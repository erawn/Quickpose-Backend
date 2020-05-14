package template.tool;

import java.util.List;


public class Node {
	
	public int label;
	
	public Data data;
	public Tree tree;
	public Node parent;
	public List<Node> children;
	
	public List<Node> allChildren;
	
	public Node(Data d, Node p) {
		data = d;
		parent = p;
	}
	
	public void addChild(Data d) {
		Node child = new Node(d, this);
		children.add(child);
		indexAdd(child);
	}
	
	public boolean isRoot() {
		return parent == null;
	}
	
	public boolean isLeaf() {
		return children.size() == 0;
	}
	
	public void indexAdd(Node n) {
		allChildren.add(n);
		if(!this.isRoot()) {
			parent.indexAdd(n);
		}
	}
}
