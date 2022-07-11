package template.tool;

public class Data {
	String path; 
	public int caretPosition;
	
	public Data(String c) {
		path = c;
		caretPosition = 0;
	}

	public void setCaretPosition(int set){
		caretPosition = set;
	}
	public int getCaretPosition(){
		return caretPosition;
	}
}

