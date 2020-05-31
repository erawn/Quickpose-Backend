/**
 * you can put a one sentence description of your tool here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author   ##author##
 * @modified ##date##
 * @version  ##tool.prettyVersion##
 */

package template.tool;

import static spark.Spark.*;
import static spark.Filter.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import javafx.application.Application;
import processing.app.Base;
import processing.app.tools.Tool;
import processing.app.ui.Editor;

// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class VCFA implements Tool {
  Base base;
  boolean setup = false; 
  private File sketchFolder;
  private File versionsCode;
  private File versionsImages;
  private File sketchbookFolder;
  public Tree codeTree;
  public int currentVersion;
  public Editor editor;
  ScheduledExecutorService windowExecutor;
  VCFAUI ui;
  
  public String getMenuTitle() {
    return "##tool.name##";
  }


  public void init(Base base) {
    // Store a reference to the Processing application itself
    this.base = base;
  }


  public void run() {

	if(setup == false) {
		
		// FOR TESTING ONLY
		try {
			FileUtils.deleteDirectory(new File(base.getActiveEditor().getSketch().getFolder()+"/"+"versions_code"));
		}catch(IOException e) {
			
		}//FOR TESTING ONLY
		
		
		
		networkSetup();
		
		dataSetup();
		GUISetup();
		Runnable updateLoop = new Runnable() {
			  public void run() {
				  update();
			  }
		  };
		  ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		  executor.scheduleAtFixedRate(updateLoop, 0, 1, TimeUnit.SECONDS);
		setup = true;
	}else {
		windowExecutor.shutdownNow();
		GUISetup();
	}
	 
     //System.out.println("Sketch Folder at : " + sketchFolder.getAbsolutePath());
     //System.out.println(editor.getMode().getIdentifier());
     
     
     editor.getSketch().reload();
  
    System.out.println("##tool.name## ##tool.prettyVersion## by ##author##");
  }
  private void update() {
	  //System.out.println("Updating...");
	  
  }

  
  private void GUISetup() {
	  Runnable window = new Runnable() {
		  public void run() {
			  Application.launch(VCFAUI.class,(String[])null);
		  }
	  };
	  windowExecutor = Executors.newScheduledThreadPool(2);
	  windowExecutor.schedule(window, 0, TimeUnit.SECONDS);
  }
  private void networkSetup(){
	  port(8080);
	  options("/*",
		        (request, response) -> {

		            String accessControlRequestHeaders = request
		                    .headers("Access-Control-Request-Headers");
		            if (accessControlRequestHeaders != null) {
		                response.header("Access-Control-Allow-Headers",
		                        accessControlRequestHeaders);
		            }

		            String accessControlRequestMethod = request
		                    .headers("Access-Control-Request-Method");
		            if (accessControlRequestMethod != null) {
		                response.header("Access-Control-Allow-Methods",
		                        accessControlRequestMethod);
		            }

		            return "OK";
		        });

		before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
	  get("/hello", (request, response) -> "Hello World!");
	  get("/hello/:name", (request, response) -> {
		  System.out.println("request : " + request.params(":name"));
		 return "Hello, " + request.params(":name"); 
	  });
	  get("/versions.json", (request, response) -> {
		  response.type("application/json");
		  return new JsonMaker().getJson();
	  });
	  get("/fork/:name", (request, response) -> {
		  System.out.println("Fork on :"+ request.params(":name"));
		  return "Success";
	  });
	  
	  
  }
  private void dataSetup(){
	  editor = base.getActiveEditor();
	  sketchFolder = editor.getSketch().getFolder();
	  String sketchPath = sketchFolder.getAbsolutePath();
	  versionsCode = new File(sketchPath+"/"+"versions_code");
	  versionsCode.mkdir();
	  
	  if(editor.getMode().getIdentifier() == "jm.mode.replmode.REPLMode") {
	    	 System.out.println("Running in REPL Mode");
	     }
	  
	  
	  String rootFolder = makeVersion(0);
	  Data root = new Data(rootFolder);
	  codeTree = new Tree(root);
	  
	  currentVersion = 0;
	  
	 
  }
  
  
 
  private String makeVersion(int id) {
	  File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
	  folder.mkdir();
	  File[] dirListing = sketchFolder.listFiles();
	  if(dirListing != null) {
		  for(File f : dirListing) {
			  if(FilenameUtils.isExtension(f.getName(), "pde")) {
				  File newFile = new File(folder.getAbsolutePath()+"/"+f.getName());
				  copyFile(f,newFile);
			  }
		  }
	  }
	  return folder.getAbsolutePath();
  }

  
  private static void copyFile(File src, File dest){
	  try {
		Files.copy(src.toPath(), dest.toPath(),StandardCopyOption.REPLACE_EXISTING);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  public class JsonMaker {
	    public String getJson() {
	        return "{ \"hello\" : \"world\"}";
	    }
	}
 
  
}
