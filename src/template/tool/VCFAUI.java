package template.tool;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene.*;
import javafx.scene.control.Button;
import javafx.scene.web.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;

import javax.swing.*;
public class VCFAUI extends Application{

	private static final long serialVersionUID = 1L;
	
	public VCFAUI(){
		
	}
		
//		 setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);		
//		 setBounds(100, 100, 768, 695);	
//		 initAndShowGUI();
		 
	 @Override
	 public void start(final Stage primaryStage) {
		 Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		 Stage window = new Stage();
		 //need java 8 to implement lambda functions????
		 //window.setOnCloseRequest(e->{Platform.exit(); System.exit(0);});
		 WebView webView = new WebView();
	 	 WebEngine webEngine = webView.getEngine();
	 	 System.out.println(System.getProperty("java.class.path"));
	 	 URL url = this.getClass().getResource(getPath("interface.html"));
	 	 webEngine.load(url.toExternalForm());
	     Scene scene = new Scene(webView,600,600);
		 window.setScene(scene);
		 window.setX((3*screenSize.width)/4);
		 window.setY((screenSize.height)/2);
		 window.show();
	 }
		 
	 
	
	
//	
//	 private void initAndShowGUI() {
//			// This method is invoked on Swing thread
//			final JFXPanel fxPanel = new JFXPanel();
//			this.add(fxPanel);
//
//			Platform.runLater(new Runnable() {
//				@Override
//				public void run() {
//					try {
//						initFX(fxPanel);
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			});
//		}
//	 
//	private void initFX(JFXPanel fxPanel) {
//	        // This method is invoked on the JavaFX thread
//		 	WebView webView = new WebView();
//		 	WebEngine webEngine = webView.getEngine();
//		 	System.out.println(System.getProperty("java.class.path"));
//		 	URL url = this.getClass().getResource(getPath("interface.html"));
//		 	webEngine.load(url.toExternalForm());
//	        Scene scene = new Scene(webView,600,600);
//	        
//	        fxPanel.setScene(scene);
//	        fxPanel.setVisible(true);
//	    }
	public String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}
}
