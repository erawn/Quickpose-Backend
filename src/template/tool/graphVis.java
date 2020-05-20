package template.tool;
import java.net.URL;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class graphVis extends Application{
	
	private Scene scene;
	MyBrowser myBrowser;
	
	@Override
	public void start(Stage primaryStage) {
	    primaryStage.setTitle("java-buddy.blogspot.com");

	    myBrowser = new MyBrowser();
	    scene = new Scene(myBrowser, 640, 480);

	    primaryStage.setScene(scene);
	    primaryStage.show();
	}
	
	class MyBrowser extends Region{

	    final String hellohtml = "hello.html";

	    WebView webView = new WebView();
	    WebEngine webEngine = webView.getEngine();
	        
	    public MyBrowser(){

	        URL urlHello = getClass().getResource("hello.html");
	        webEngine.load(urlHello.toExternalForm());
	    
	        getChildren().add(webView);
	    }
	}
	
}
