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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javax.swing.*;
public class VCFAUI extends JFrame{
	
	private final JButton up = new JButton("Up");
	private final JButton down = new JButton("Down");

	 public VCFAUI(){
		 setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);		
		 setBounds(100, 100, 768, 695);	
		 initAndShowGUI();
	 }
	 private void initAndShowGUI() {
			// This method is invoked on Swing thread
			final JFXPanel fxPanel = new JFXPanel();
			this.add(fxPanel);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						initFX(fxPanel);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
	 
	 private static void initFX(JFXPanel fxPanel) {
	        // This method is invoked on the JavaFX thread
	        Scene scene = createScene();
	        fxPanel.setScene(scene);
	    }
	 
	 private static Scene createScene() {
	        Group  root  =  new  Group();
	        Scene  scene  =  new  Scene(root, Color.ALICEBLUE);
	        Text  text  =  new  Text();
	        
	        text.setX(40);
	        text.setY(100);
	        text.setFont(new Font(25));
	        text.setText("Welcome JavaFX!");

	        root.getChildren().add(text);
	        root.getChildren().add(up);
	        root.getChildren().add(down);

	        return (scene);
	    }
}
