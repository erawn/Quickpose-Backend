package template.tool;

import java.util.ArrayList;
import java.util.List;


public class Node {
	
	public int id;
	
	public Data data;
	public Tree tree;
	public Node parent;
	public List<Node> children;
	
	public List<Node> allChildren;
	
	public Node(Data d,Tree t, Node p) {
		tree = t;
		while(tree.idExists(tree.curId)) {
			tree.curId++;
		}
		id = tree.curId;
		tree.curId++;
		
		data = d;
		
		if(id != 0) {
			parent = p;
		}
		
		children = new ArrayList<>();
		allChildren = new ArrayList<>();
	}
	
	public Node(Data d,Tree t, Node p, int idSet) {
		tree = t;
		id = idSet;
		
		data = d;
		
		if(id != 0) {
			parent = p;
		}
		
		children = new ArrayList<>();
		allChildren = new ArrayList<>();
	}
	
	
	public Node addChild(Data d) {
		Node child = new Node(d,tree,this);
		children.add(child);
		indexAdd(child);
		return child;
	}
	public Node setChild(Data d, int id) {
		Node child = new Node(d,tree,this, id);
		children.add(child);
		indexAdd(child);
		return child;
	}
	
	public boolean isRoot() {
		return id == 0;
	}
	
	public void remove() {
		if(isRoot()) {
			System.out.println("Can't remove root node");
		}else {
			for(Node child : children) {
				parent.children.add(child);
			}
			for(int i = 0; i < parent.children.size(); i++) {
				if(parent.children.get(i).id == this.id) {
					parent.children.remove(i);
				}
			}
			indexRemove(this);
		}
		
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
	public void indexRemove(Node n) {
		for(int i = 0; i < allChildren.size(); i++) {
			if(allChildren.get(i).id == n.id) {
				allChildren.remove(i);
			}
		}
		if(!this.isRoot()) {
			parent.indexRemove(n);
		}
	}
}
