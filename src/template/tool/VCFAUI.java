package template.tool;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.application.Application;
import javafx.scene.web.*;
import javafx.stage.Stage;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;

public class VCFAUI extends Application{

	
	public VCFAUI(){
		
	}
		 
	 @Override
	 public void start(final Stage primaryStage) {
		 Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		 Stage window = new Stage();
		 //need java 8 to implement lambda functions????
		 window.setOnCloseRequest(e->{Platform.exit(); try {
			this.stop();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} });//System.exit(0);});
		 WebView webView = new WebView();
	 	 WebEngine webEngine = webView.getEngine();
	 	 //System.out.println(System.getProperty("java.class.path"));
	 	 URL url = this.getClass().getResource(getPath("interface.html"));
	 	 webEngine.load(url.toExternalForm());
	     Scene scene = new Scene(webView,600,600);
		 window.setScene(scene);
		 window.setX((3*screenSize.width)/4);
		 window.setY((screenSize.height)/2);
		 window.show();
	 }
		 

	public String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}
}
