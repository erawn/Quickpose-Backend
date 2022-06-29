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
 * 
 * 
 * 
 * @version  ##tool.prettyVersion##
 */

package template.tool;


import com.google.gson.Gson;

//import com.fasterxml.jackson.databind.*;
import spark.*;
import static spark.Spark.*;
import static spark.Filter.*;

import java.io.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;

import java.io.FileWriter;
import java.io.IOException;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;

import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import processing.app.Base;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.tools.Tool;
import processing.app.ui.Editor;

import java.util.Scanner;

// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class VCFA implements Tool {
  Base base;
  boolean setup = false; 
  private File sketchFolder;
  private File versionsCode;
  private File versionsImages;
  private File versionsTree;
  private File sketchbookFolder;
  public Tree codeTree;
  public int currentVersion;
  public Editor editor;
  ScheduledExecutorService windowExecutor;
  JSONObject nodePositions;
  private static Gson gson = new Gson();
  
  public String getMenuTitle() {
    return "Version Control for Artists";
  }


  public void init(Base base) {
    // Store a reference to the Processing application itself
    this.base = base;
    
  }


  public void run() {

    if(setup == false) {
        
//		// FOR TESTING ONLY
//		try {
//			FileUtils.deleteDirectory(new File(base.getActiveEditor().getSketch().getFolder()+"/"+"versions_code"));
//		}catch(IOException e) {}
//		//FOR TESTING ONLY
        
        dataSetup();
        networkSetup();
        GUISetup();
        Runnable updateLoop = new Runnable() {
              public void run() {
                  update();
              }
          };
          ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
          executor.scheduleAtFixedRate(updateLoop, 0, 200, TimeUnit.MILLISECONDS);
        setup = true;
    }else {
        GUISetup();
    }
     
     //System.out.println("Sketch Folder at : " + sketchFolder.getAbsolutePath());
     //System.out.println(editor.getMode().getIdentifier());
     
     
     editor.getSketch().reload();
  
    System.out.println("##tool.name## ##tool.prettyVersion## by ##author##");
  }
  
  private void update() {
     //check if files are modified first??
     saveCurrent();
      
  }

  
  private void GUISetup() {
      
      try{
          final File f = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
          System.out.println(f.getParentFile().getPath());
          //File webpage = new File(f.getParentFile().getParentFile().getPath() + "/examples/interface.html");
          //System.err.println(webpage.getAbsolutePath());
          //Desktop.getDesktop().browse(url.toURI());
      }
      catch(Exception E){
          System.err.println("Exp : "+E.getMessage());
      }

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
        
//https://stackoverflow.com/questions/47328754/with-java-spark-how-do-i-send-a-html-file-as-a-repsonse
      get("/", (request, response) -> {
              //response.redirect("interface.html");
              return "";
          });
      get("/versions.json", (request, response) -> {
          response.type("application/json");
          return codeTree.getJSON();
      });
      get("/fork/:id", (request, response) -> {
          //System.out.println("Fork on :"+ request.params(":name"));
          return fork(Integer.parseInt(request.params(":id")));
      });
      get("/select/:id", (request, response) -> {
          //System.out.println("Select Node :"+ request.params(":name"));
          currentVersion = Integer.parseInt(request.params(":id"));
          changeActiveVersion(currentVersion);
          return "Success";
      });
      get("/currentVersion", (request, response) -> {
          //System.out.println("Current ID Request :" + currentVersion);
          return currentVersion;
      });
      post("/positions.json", (request, response) -> {
          response.type("application/json");
          System.out.println(request.body());
          updatePositions(request.body());
          return "Success";
      });
      post("/tldrfile", (request, response) -> {
          File tempFile = new File(versionsCode.toPath()+"/quickpose.tldr"); //(versionsCode.toPath(), "quickpose","tldr");
          
          request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));	
          try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } 
          logInfo(request, tempFile.toPath());
          return "Success";
      });
      get("/image/:id", (request, response) -> {
        File f = new File(versionsCode.getAbsolutePath() + "/_" + request.params(":id") + "/render.png");
        if(f.exists()){
            response.status(200);
        }else{
            response.status(201);
            f = new File(sketchFolder.getParentFile().getAbsolutePath() + "/tools/VCFA/examples/noicon.png");
        }

        try (OutputStream out = response.raw().getOutputStream()) {
            response.header("Content-Disposition", "inline; filename=render.png");
            Files.copy(f.toPath(), out);
            out.flush();
            
            return response;
        }
      });
      
  }
  private static void logInfo(Request req, Path tempFile) throws IOException, ServletException {
    System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile.toAbsolutePath() + "'");
  }
  private static String getFileName(Part part) {
      for (String cd : part.getHeader("content-disposition").split(";")) {
          if (cd.trim().startsWith("filename")) {
              return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
          }
      }
      return null;
  }
  private void dataSetup(){
      editor = base.getActiveEditor();
      sketchFolder = editor.getSketch().getFolder();
      String sketchPath = sketchFolder.getAbsolutePath();
      versionsCode = new File(sketchPath+"/"+"versions_code");
      versionsCode.mkdir();
      
      if(editor.getMode().getIdentifier() == "jm.mode.replmode.REPLMode") {
             System.out.println("Running in REPL Mode");
         }else {
             System.out.println("Please Run in REPL Mode!");
         }
      
      versionsTree = new File(versionsCode.getAbsolutePath() + "/tree.json");
      //System.out.println(versionsTree.getAbsoluteFile());
      //System.out.println(versionsTree.getAbsolutePath());
      if(!versionsTree.exists()) {
         //System.out.println("No Existing tree.json detected - creating a new verison history...");

         // System.out.println(editor.getText().isEmpty());
          if(editor.getText().isEmpty()) {
              editor.setText("test");
                      }

          String rootFolder = makeVersion(0);
          Data root = new Data(rootFolder);
          codeTree = new Tree(root);
          writeJSONFromRoot(); 
      }else {
          //System.out.println("Existing tree.json detected! Reading...");
          readJSONToRoot();
      }	 
  }
  
  
 private int fork(int id) {
     if(codeTree.idExists(id)) {
         Node parent = codeTree.getNode(id);
         Data data = new Data("");
         if(parent != null) {
             Node child = parent.addChild(data);
             child.data.path = makeVersion(child.id);
             changeActiveVersion(child.id);
             writeJSONFromRoot();
             return child.id;
         }
        
         
     }else {
         System.out.println("Attempted Fork: Node Doesn't Exist");
     }
     
     return -1;
     
 }
 
 private void saveCurrent() {
     if(codeTree.getNode(currentVersion).children.size() == 0) {
         makeVersion(currentVersion);
     }else if(base.getActiveEditor().getSketch().isModified()){
         Sketch currentSketch = base.getActiveEditor().getSketch();
         base.getActiveEditor().handleSave(true);
         fork(currentVersion);
     }
 }
 

 private void changeActiveVersion(int id) {
     
     if(!codeTree.idExists(id)) {
         System.out.println("Attempted to change active version to invalid Id");
         return;
     }
     File[] sketchListing = sketchFolder.listFiles();
     if(sketchListing != null) {
          for(File f : sketchListing) {
              if(FilenameUtils.equals(f.getName(), "render.png")) {
                  f.delete();
              }
          }
      }
     
     File versionFolder = new File(codeTree.getNode(id).data.path);
     File[] versionListing = versionFolder.listFiles();
     if(versionListing != null) {
          for(File f : versionListing) {
              if(FilenameUtils.isExtension(f.getName(), "pde")) {
                  File newFile = new File(sketchFolder.getAbsolutePath()+"/"+f.getName());
                  copyFile(f,newFile);
              }
          }
      }
     //base.getActiveEditor().getSketch().reload();
     base.getActiveEditor().handleSave(true);
     currentVersion = id;
    // System.out.println("Switched version to "+ id );
 }
 
  private String makeVersion(int id) {
      //base.getActiveEditor().getSketch().reload();
      //base.getActiveEditor().handleSave(true);
      File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
      folder.mkdir();
      File[] dirListing = sketchFolder.listFiles();
      if(dirListing != null) {
          for(File f : dirListing) {
              if(FilenameUtils.isExtension(f.getName(), "pde") ||
                      FilenameUtils.equals(f.getName(), "render.png")) {
                  File newFile = new File(folder.getAbsolutePath()+"/"+f.getName());
                  copyFile(f,newFile);
              }
          }
      }
      return folder.getAbsolutePath();
  }
  private void updatePositions(String input) {
      try {
      nodePositions = new JSONObject(input);
      } catch(Exception e){}
      //System.out.println(input);

  }
  
  
  private void writeJSONFromRoot() {
      if(versionsTree.exists()) {
          versionsTree.delete();
      }
      try{
         versionsTree.createNewFile();
      } catch(IOException e) {
          System.out.println("Exception Occured: " + e.toString());
      }
      try {
         FileWriter fileWriter;
         fileWriter = new FileWriter(versionsTree.getAbsoluteFile(),true);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         bufferedWriter.write(codeTree.getJSONSave(currentVersion));
         bufferedWriter.close();
         
      } catch(IOException e) {
          System.out.println("Error Saving JSON to File");
      }
  }
  
  private void readJSONToRoot() {
    byte[] encoded;
    Tree importTree;
    String input = null;
    try {
        encoded = Files.readAllBytes(Paths.get(versionsTree.getAbsolutePath()));
        input = new String(encoded,"UTF-8");
    } catch (Exception e) {
        e.printStackTrace();
    }
    try {
        JSONObject graph = new JSONObject(input);
        JSONArray nodes = graph.getJSONArray("Nodes");
        JSONArray edges = graph.getJSONArray("Edges");
        
        for(int i = 0; i < nodes.length(); i++) {
            //System.out.println("Node Id : " + nodes.getJSONObject(i).getString("id"));
        }
        
        int rootInd = JSONSearch(nodes, 0);
        if(rootInd == -1) {
            System.out.println("Couldn't Find Root Node on Import");
        }
        Data root = new Data(nodes.getJSONObject(rootInd).getString("path"));
        importTree = new Tree(root);
        
        for(int i = 0; i < nodes.length(); i++) {
            //System.out.println("Node Id : " + nodes.getJSONObject(i).getString("id"));
        }
        
        
        while(importTree.getList().size() < nodes.length() - 1) {
            for(int i = 0; i < edges.length(); i++) {
                int source =  Integer.parseInt(edges.getJSONObject(i).getString("source"));
                int target =  Integer.parseInt(edges.getJSONObject(i).getString("target"));
                if(importTree.idExists(source) && !importTree.idExists(target)) {
                    Node parent = importTree.getNode(source);
                    int childInd = JSONSearch(nodes, target);
                    Data data = new Data(nodes.getJSONObject(childInd).getString("path"));
                    Node child = parent.setChild(data,target);
                    //System.out.println("created child : " + target + " from parent : "+ source);
                }
                //System.out.println("Edge From : " + source + " to : " + target);
                
            }
        }
        
        codeTree = importTree;
        changeActiveVersion(graph.getInt("CurrentNode"));
        
    }catch(Exception e) {
        System.out.println("Exception in Building Tree : "+ e.toString());
    }
    
    
    
    
    
//	 if(codeTree.idExists(id)) {
//		 Node parent = codeTree.getNode(id);
//		 Data data = new Data("");
//		 if(parent != null) {
//			 Node child = parent.addChild(data);
//			 child.data.path = makeVersion(child.id);
//			 changeActiveVersion(child.id);
//		 }
//		 writeJSONFromRoot();
//	 }else {
//		 System.out.println("Attempted Fork: Node Doesn't Exist");
//	 }
  }
  
  int JSONSearch(JSONArray arr, int id) {
      try {
          for(int i = 0; i < arr.length(); i++) {
                if(Integer.parseInt(arr.getJSONObject(i).getString("id")) == id) {
                    return i;
                }
            }
      }catch(Exception e) {
          
      }
          return -1;
        
  }
  
  private static void copyFile(File src, File dest){
      try {
        if(!FileUtils.contentEquals(src, dest)) {
            Files.copy(src.toPath(), dest.toPath(),StandardCopyOption.REPLACE_EXISTING);
        }
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
  }  
}
