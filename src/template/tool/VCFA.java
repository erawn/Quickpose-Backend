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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.text.BadLocationException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import processing.app.Base;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.tools.Tool;
import processing.app.ui.Editor;


// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class VCFA implements Tool {
    Base base;
    boolean setup = false;
    private File sketchFolder;
    private File assetsFolder;
    private File versionsCode;
    private File versionsImages;
    private File versionsTree;
    private File sketchbookFolder;
    private SketchCode starterCode;
    public Tree codeTree;
    public int currentVersion;
    public Editor editor;
    ScheduledExecutorService windowExecutor;
    JSONObject nodePositions;
    private int serverPort = 8080;
    ScheduledExecutorService executor = null;
    private long lastModified = 0;
    private Lock renderLock = new ReentrantLock();
    public String getMenuTitle() {
        return "Version Control for Artists";
    }

    public void init(Base base) {
        // Store a reference to the Processing application itself
        this.base = base;

    }

    public void run() {

        if (setup == false) {
            networkSetup();
        }

        // // FOR TESTING ONLY
        // try {
        // FileUtils.deleteDirectory(new
        // File(base.getActiveEditor().getSketch().getFolder()+"/"+"versions_code"));
        // }catch(IOException e) {}
        // //FOR TESTING ONLY
        if(executor != null){
            executor.shutdownNow();
        }
        dataSetup();
        
        Runnable updateLoop = new Runnable() {
            public void run() {
                update();
            }
        };
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(updateLoop, 0, 100, TimeUnit.MILLISECONDS);
        setup = true;
        // System.out.println("Sketch Folder at : " + sketchFolder.getAbsolutePath());
        // System.out.println(editor.getMode().getIdentifier());

        editor.getSketch().reload();

        System.out.println("##tool.name## ##tool.prettyVersion## by ##author##");

    }

    private void update() {
        // check if files are modified first??
        File render = new File(sketchFolder.toPath()+"/render.png");
        File storedRender = new File(versionsCode.getAbsolutePath()+"/_"+currentVersion+"/render.png");
        boolean fileModified = base.getActiveEditor().getSketch().isModified();
        boolean renderModified = render.exists() && (!storedRender.exists() || render.lastModified() != storedRender.lastModified());

        if(fileModified){
            makeVersion(currentVersion,false);
            lastModified = render.lastModified();
            System.out.println("saving to version");
        }else if(renderModified){
            System.out.println("attempting to save render");
            File tempRender = new File(versionsCode.getAbsolutePath()+"/_"+currentVersion+"/renderTemp.png");
            copyFile(render, tempRender);
            renderLock.lock();
            try {
                storedRender.delete();
                tempRender.renameTo(storedRender);
            } finally {
                renderLock.unlock();
                System.out.println("upload giving up lock");
            }
            lastModified = render.lastModified();
            System.out.println("saving render");
        }  
    //     Lock lock = ...; 
    //     lock.lock();
    //     try {
    //         // access to the shared resource
    //     } finally {
    //         lock.unlock();
    //     }
    }

    private void networkSetup() {
        port(serverPort);
        System.out.println("Starting server on port:" + serverPort);
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

        // https://stackoverflow.com/questions/47328754/with-java-spark-how-do-i-send-a-html-file-as-a-repsonse
        get("/", (request, response) -> {
            // response.redirect("interface.html");
            return "";
        });
        get("/versions.json", (request, response) -> {
            response.type("application/json");
            return codeTree.getJSON();
        });
        get("/fork/:id", (request, response) -> {
            // System.out.println("Fork on :"+ request.params(":name"));
            return fork(Integer.parseInt(request.params(":id")));
        });
        get("/select/:id", (request, response) -> {
            // System.out.println("Select Node :"+ request.params(":name"));
            currentVersion = Integer.parseInt(request.params(":id"));
            changeActiveVersion(currentVersion);
            return currentVersion;
        });
        get("/currentVersion", (request, response) -> {
            // System.out.println("Current ID Request :" + currentVersion);
            return currentVersion;
        });
        get("/projectName", (request, response) -> {
            // System.out.println("Current ID Request :" + currentVersion);
            return sketchFolder.getName();
        });
        post("/positions.json", (request, response) -> {
            response.type("application/json");
            System.out.println(request.body());
            updatePositions(request.body());
            return "Success";
        });
        post("/tldrfile", (request, response) -> {
            File tempFile = new File(versionsCode.toPath() + "/quickpose.tldr");
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use
                                                                                                // same "name" as input
                                                                                                // field in form
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            // logInfo(request, tempFile.toPath());
            return "Success";
        });
        get("/tldrfile", (request, response) -> {
            File f = new File(versionsCode.toPath() + "/quickpose.tldr");
            if (f.exists()) {
                try (OutputStream out = response.raw().getOutputStream()) {
                    response.header("Content-Disposition", "filename=quickpose.tldr");
                    Files.copy(f.toPath(), out);
                    out.flush();
                    response.status(200);
                    return response;
                }
            } else {
                response.status(201);
                return response;
            }
        });
        get("/image/:id", (request, response) -> {
            File f = new File(versionsCode.getAbsolutePath() + "/_" + request.params(":id") + "/render.png");
            if (f.exists()) {
                response.status(200);
            } else {
                response.status(201);
                f = new File(sketchFolder.getParentFile().getAbsolutePath() + "/tools/VCFA/examples/noicon.png");
            }
            if(request.params(":id") == String.valueOf(currentVersion)){
                renderLock.lock();
                try (OutputStream out = response.raw().getOutputStream()) {
                    response.header("Content-Disposition", "inline; filename=render.png");
                    Files.copy(f.toPath(), out);
                    out.flush(); 
                }finally {
                    renderLock.unlock(); 
                }
            }else{
                try (OutputStream out = response.raw().getOutputStream()) {
                    response.header("Content-Disposition", "inline; filename=render.png");
                    Files.copy(f.toPath(), out);
                    out.flush(); 
                }
            }
            
            return response;
        });
        get("/assets/*", (request, response) -> {
            File f = new File(assetsFolder.getAbsolutePath() + "/" + request.splat()[0]);
            if (f.exists()) {
                response.status(200);
            } else {
                response.status(404);
                return response;
            }

            try (OutputStream out = response.raw().getOutputStream()) {
                response.header("Content-Disposition", "inline; filename=" + request.params(":name"));
                Files.copy(f.toPath(), out);
                out.flush();

                return response;
            }
        });
        delete("/assets/*", (request, response) -> {
            File f = new File(assetsFolder.getAbsolutePath() + "/" + request.splat()[0]);
            if (f.exists()) {
                if (f.delete()) {
                    response.status(200);
                    System.out.println("deleted file" + f.getAbsolutePath());
                    return response;
                }
            }
            response.status(201);
            return response;
        });
        put("/assets/*", (request, response) -> {
            File tempFile = new File(assetsFolder.getAbsolutePath() + "/" + request.splat()[0]);
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use
                                                                                                // same "name" as input
                                                                                                // field in form
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logUploadInfo(request, tempFile.toPath());
            return "Success";
        });

    }

    private static void logUploadInfo(Request req, Path tempFile) throws IOException, ServletException {
        System.out.println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '"
                + tempFile.toAbsolutePath() + "'");
    }

    private static String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private void dataSetup() {
        editor = base.getActiveEditor();
        sketchFolder = editor.getSketch().getFolder();
        String sketchPath = sketchFolder.getAbsolutePath();
        versionsCode = new File(sketchPath + "/" + "versions_code");
        versionsCode.mkdir();
        assetsFolder = new File(versionsCode.toPath() + "/" + "assets");
        assetsFolder.mkdir();
        File starterCodeFile = new File(Base.getSketchbookToolsFolder().toPath() + "/VCFA/examples/QuickposeDefault.pde");
       
        
        
        if (editor.getMode().getIdentifier() == "jm.mode.replmode.REPLMode") {
            System.out.println("Running in REPL Mode");
        } else {
            System.out.println("Please Run in REPL Mode!");
            System.out.println(base.getModeList());
            for (Mode m : base.getModeList()) {
                if(m.getIdentifier() == "jm.mode.replmode.REPLMode"){
                    base.changeMode(m);
                    System.out.println("switched to REPL mode");
                }
                
            }

        }

        versionsTree = new File(versionsCode.getAbsolutePath() + "/tree.json");
        // System.out.println(versionsTree.getAbsoluteFile());
        // System.out.println(versionsTree.getAbsolutePath());
        if (!versionsTree.exists()) {
            System.out.println("No Existing Quickpose Session Detected - creating a new verison history...");
            if(starterCodeFile.exists()){
                
            }
            //replaceCode(new SketchCode(new File(folder, filename), ext));
            if (starterCodeFile.exists() && editor.getText().isEmpty()) {
                starterCode = new SketchCode(starterCodeFile,".pde");
                try {
                    editor.setText(starterCode.getDocumentText());
                } catch (BadLocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            editor.handleSaveAs();
            String rootFolder = makeVersion(0,false);
            Data root = new Data(rootFolder);
            codeTree = new Tree(root);
            writeJSONFromRoot();
        } else {
            System.out.println("Existing Quickpose Session Found! Loading...");
            readJSONToRoot();
        }
    }

    private int fork(int id) {
        if (codeTree.idExists(id)) {
            Node parent = codeTree.getNode(id);
            Data data = new Data("");
            if (parent != null) {
                Node child = parent.addChild(data);
                child.data.path = makeVersion(child.id,false);
                changeActiveVersion(child.id);
                writeJSONFromRoot();
                return child.id;
            }
        }
        System.out.println("Attempted Fork: Node Doesn't Exist");
        return -1;
    }

    private void changeActiveVersion(int id) {

        if (!codeTree.idExists(id)) {
            System.out.println("Attempted to change active version to invalid Id");
            return;
        }
        File[] sketchListing = sketchFolder.listFiles();
        if (sketchListing != null) {
            for (File f : sketchListing) {
                if (FilenameUtils.equals(f.getName(), "render.png")) {
                    f.delete();
                }
            }
        }

        File versionFolder = new File(codeTree.getNode(id).data.path);
        File[] versionListing = versionFolder.listFiles();
        if (versionListing != null) {
            for (File f : versionListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde") || f.getName() == "render.png") {
                    File newFile = new File(sketchFolder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
            }
        }
        if (codeTree.getNode(id).children.size() == 0) {
            base.getActiveEditor().getSketch().getMainFile().setWritable(true);
        } else {
            base.getActiveEditor().getSketch().getMainFile().setWritable(false);
        }
        base.getActiveEditor().getSketch().reload();
        base.getActiveEditor().handleSave(true);
        currentVersion = id;
        // System.out.println("Switched version to "+ id );
    }

    private String makeVersion(int id, boolean renderOnly) {
        // base.getActiveEditor().getSketch().reload();
        
        base.getActiveEditor().handleSave(true);
        File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        folder.mkdir();
        File[] dirListing = sketchFolder.listFiles();
        if (dirListing != null) {
            for (File f : dirListing) {
                if (FilenameUtils.equals(f.getName(), "render.png") || 
                    FilenameUtils.isExtension(f.getName(), "pde")) {
                    File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
            }
        }
        return folder.getAbsolutePath();
    }

    private void updatePositions(String input) {
        try {
            nodePositions = new JSONObject(input);
        } catch (Exception e) {
        }
        // System.out.println(input);

    }

    private void writeJSONFromRoot() {
        if (versionsTree.exists()) {
            versionsTree.delete();
        }
        try {
            versionsTree.createNewFile();
        } catch (IOException e) {
            System.out.println("Exception Occured: " + e.toString());
        }
        try {
            FileWriter fileWriter;
            fileWriter = new FileWriter(versionsTree.getAbsoluteFile(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(codeTree.getJSONSave(currentVersion));
            bufferedWriter.close();

        } catch (IOException e) {
            System.out.println("Error Saving JSON to File");
        }
    }

    private void readJSONToRoot() {
        byte[] encoded;
        Tree importTree;
        String input = null;
        try {
            encoded = Files.readAllBytes(Paths.get(versionsTree.getAbsolutePath()));
            input = new String(encoded, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject graph = new JSONObject(input);
            JSONArray nodes = graph.getJSONArray("Nodes");
            JSONArray edges = graph.getJSONArray("Edges");

            for (int i = 0; i < nodes.length(); i++) {
                // System.out.println("Node Id : " + nodes.getJSONObject(i).getString("id"));
            }

            int rootInd = JSONSearch(nodes, 0);
            if (rootInd == -1) {
                System.out.println("Couldn't Find Root Node on Import");
            }
            Data root = new Data(nodes.getJSONObject(rootInd).getString("path"));
            importTree = new Tree(root);

            for (int i = 0; i < nodes.length(); i++) {
                // System.out.println("Node Id : " + nodes.getJSONObject(i).getString("id"));
            }

            while (importTree.getList().size() < nodes.length() - 1) {
                for (int i = 0; i < edges.length(); i++) {
                    int source = Integer.parseInt(edges.getJSONObject(i).getString("source"));
                    int target = Integer.parseInt(edges.getJSONObject(i).getString("target"));
                    if (importTree.idExists(source) && !importTree.idExists(target)) {
                        Node parent = importTree.getNode(source);
                        int childInd = JSONSearch(nodes, target);
                        Data data = new Data(nodes.getJSONObject(childInd).getString("path"));
                        Node child = parent.setChild(data, target);
                        // System.out.println("created child : " + target + " from parent : "+ source);
                    }
                    // System.out.println("Edge From : " + source + " to : " + target);

                }
            }

            codeTree = importTree;
            changeActiveVersion(graph.getInt("CurrentNode"));

        } catch (Exception e) {
            System.out.println("Exception in Building Tree : " + e.toString());
        }

        // if(codeTree.idExists(id)) {
        // Node parent = codeTree.getNode(id);
        // Data data = new Data("");
        // if(parent != null) {
        // Node child = parent.addChild(data);
        // child.data.path = makeVersion(child.id);
        // changeActiveVersion(child.id);
        // }
        // writeJSONFromRoot();
        // }else {
        // System.out.println("Attempted Fork: Node Doesn't Exist");
        // }
    }

    int JSONSearch(JSONArray arr, int id) {
        try {
            for (int i = 0; i < arr.length(); i++) {
                if (Integer.parseInt(arr.getJSONObject(i).getString("id")) == id) {
                    return i;
                }
            }
        } catch (Exception e) {

        }
        return -1;

    }

    private static void copyFile(File src, File dest) {
        try {
            if (!FileUtils.contentEquals(src, dest)) {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
