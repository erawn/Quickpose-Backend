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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.event.ChangeListener;

public class VCFAUI extends Application{
	WebView webView;
	WebEngine webEngine;
	URL url;
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
		 webView = new WebView();
	 	 webEngine = webView.getEngine();
	 	 webEngine.setJavaScriptEnabled(true);
	 	 webView.setContextMenuEnabled(false);

	 	 //System.out.println(System.getProperty("java.class.path"));
	 	 url = this.getClass().getResource(getPath("interface.html"));
	 	 webEngine.load(url.toExternalForm());
	     Scene scene = new Scene(webView,600,600);
	     window.setX((3*screenSize.width)/4);
		 window.setY((screenSize.height)/2);
		 window.setTitle("QuickPose");
		 window.setScene(scene);
		 window.show();
		 
		 
	 }
		 

	public void reload() {
		webEngine.reload();
	}
	public String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}
}
