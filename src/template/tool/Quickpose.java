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
import com.fasterxml.jackson.jr.ob.JSON;

import java.awt.Color;
import java.io.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.CopyOption;
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

//import org.slf4j.Logger;
// import org.slf4j.impl.SimpleLoggerFactory;
// import org.slf4j.impl.SimpleLogger;
// import ch.qos.logback.classic.Level;
// import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.eclipse.jetty.util.log.Slf4jLog;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.tools.Tool;
import processing.app.ui.Editor;

// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class Quickpose implements Tool {
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
    private Lock tldrLock = new ReentrantLock();

    private org.slf4j.Logger logger = LoggerFactory.getLogger(Quickpose.class);

    public String getMenuTitle() {
        return "Quickpose";
    }

    public void init(Base base) {
        // Store a reference to the Processing application itself
        this.base = base;

    }

    public void run() {
       
        Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        if (base.getActiveEditor().getSketch().isUntitled()) {
            Messages.showMessage("Quickpose: Unsaved Sketch",
                    "Quickpose Can't Run on an Unsaved Sketch, Please Save Sketch and Run Again");
            base.getActiveEditor().handleSave(false);

        } else {
            System.out.println("##tool.name## (v##tool.prettyVersion##) by ##author.name##");
            dataSetup();
            if (setup == false) {
                networkSetup();
            }
            if (executor != null) {
                executor.shutdownNow();
            }
            

            Runnable updateLoop = new Runnable() {
                public void run() {
                    update();
                }
            };
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(updateLoop, 0, 100, TimeUnit.MILLISECONDS);
            setup = true;
        }
    }

    private void update() {
        File render = new File(sketchFolder.toPath() + "/render.png");
        File storedRender = new File(versionsCode.getAbsolutePath() + "/_" + currentVersion + "/render.png");
        boolean fileModified = base.getActiveEditor().getSketch().isModified();
        boolean renderModified = render.exists() && (!storedRender.exists() || render.lastModified() != storedRender.lastModified());

        codeTree.getNode(currentVersion).data.setCaretPosition(editor.getTextArea().getCaretPosition());
        //System.out.println(editor.getTextArea().getCaretPosition());
        //System.out.println(editor.getTextArea().);
        if (fileModified) {
            makeVersion(currentVersion);
            lastModified = render.lastModified();
        } else if (renderModified) {
            File tempRender = new File(versionsCode.getAbsolutePath() + "/_" + currentVersion + "/renderTemp.png");
            copyFile(render, tempRender);

            renderLock.lock();
            try {
                storedRender.delete();
                tempRender.renameTo(storedRender);
            } finally {
                renderLock.unlock();
            }
            lastModified = render.lastModified();
        }

        if (codeTree.getNode(currentVersion).children.size() != 0){
            editor.statusNotice("Sketch is Read Only Because It Has Child Nodes");
        }
    }

    private void networkSetup() {
        port(serverPort);
        System.out.println("Quickpose: Starting server on port:" + serverPort);
        System.out.println("Open ##tool.url## in a Browser to Start");
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
            response.redirect("https://quickpose.vercel.app/");
            return response;
        });
        get("/versions.json", (request, response) -> {
            response.type("application/json");
            try{
                String json = codeTree.getJSONSave(currentVersion, sketchFolder.getName());
                //System.out.println(json);
                return json;
            }catch(Error e){
                logger.error(e.getMessage());
            }
            response.status(500);
            return response;
            
        });
        get("/fork/:id", (request, response) -> {
            logger.info("Fork ID:" + Integer.parseInt(request.params(":id")));
            if (fork(Integer.parseInt(request.params(":id"))) > 0) {
                logger.warn("sent fork");
                return codeTree.getJSONSave(currentVersion, sketchFolder.getName());
            }
            response.status(500);
            return response;
        });
        get("/select/:id", (request, response) -> {
            logger.info("Selected ID:"+Integer.parseInt(request.params(":id")));
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
        post("/tldrfile", (request, response) -> {
            File f = new File(versionsCode.toPath() + "/quickpose.tldr");
            tldrLock.lock();
            try {
                request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
                // getPart needs to use same "name" as input field in form
                try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { 
                    Files.copy(input, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                tldrLock.unlock();
            }
            logUploadInfo(request, f.toPath(), logger);
            return "Success";
        });
        get("/tldrfile", (request, response) -> {
            File f = new File(versionsCode.toPath() + "/quickpose.tldr");
            if (f.exists()) {
                if(tldrLock.tryLock()){
                    try {
                        try (OutputStream out = response.raw().getOutputStream()) {
                            response.header("Content-Disposition", "filename=quickpose.tldr");
                            Files.copy(f.toPath(), out);
                            out.flush();
                            response.status(200);
                        }
                    } finally {
                        tldrLock.unlock();
                    }
                    return response;
                }else{
                    logger.warn("Quickpose: canvas file failed to save — if this happens often, your canvas might not be saved properly");
                    response.status(400);
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
                f = new File(sketchFolder.getParentFile().getAbsolutePath() + "/tools/Quickpose/examples/noicon.png");
            }
            if (request.params(":id") == String.valueOf(currentVersion)) {
                renderLock.lock();
                try (OutputStream out = response.raw().getOutputStream()) {
                    response.header("Content-Disposition", "inline; filename=render.png");
                    Files.copy(f.toPath(), out);
                    out.flush();
                } finally {
                    renderLock.unlock();
                }
            } else {
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
                    return response;
                }
            }
            response.status(201);
            return response;
        });
        put("/assets/*", (request, response) -> {
            File tempFile = new File(assetsFolder.getAbsolutePath() + "/" + request.splat()[0]);
            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            logUploadInfo(request, tempFile.toPath(), logger);
            return "Success";
        });
    }
    private static void logUploadInfo(Request req, Path tempFile,org.slf4j.Logger logger ) throws IOException, ServletException {
        logger.info("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '"+ tempFile.toAbsolutePath() + "'");
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
        File starterCodeFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/QuickposeDefault.pde");
        File starterCatFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/cat.png");
        File startertldr = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/quickpose.tldr");
        if (editor.getMode().getIdentifier() == "jm.mode.replmode.REPLMode") {
            logger.info("Quickpose: Running in REPL Mode");
        } else {
            logger.warn("Quickpose: Please Run in REPL Mode!");
            System.out.println(base.getModeList());
            for (Mode m : base.getModeList()) {
                if (m.getIdentifier() == "jm.mode.replmode.REPLMode") {
                    base.changeMode(m);
                    System.out.println("Quickpose: Switched to REPL mode");
                }
            }
        }

        versionsTree = new File(versionsCode.getAbsolutePath() + "/tree.json");
        if (!versionsTree.exists()) {
            logger.warn("Quickpose: No Existing Quickpose Session Detected - creating a new verison history");
            if (starterCodeFile.exists() && editor.getText().length() < 10) { //This is a hack
                    copyFile(starterCodeFile, editor.getSketch().getMainFile());
                    copyFile(starterCatFile, new File(assetsFolder.toPath()+"/cat.png"));
                    copyFile(startertldr, new File(versionsCode.toPath()+"/quickpose.tldr"));
                    editor.getSketch().reload();
                    editor.handleSave(false);
            }
            codeTree = new Tree(new Data(makeVersion(0)));
            changeActiveVersion(0);
            writeJSONFromRoot();
        } else {
            logger.info("Quickpose: Existing Quickpose Session Found! Loading...");
            readJSONToRoot();
        }
    }

    private int fork(int id) {
        if (codeTree.idExists(id)) {
            Node parent = codeTree.getNode(id);
            if (parent != null) {
                Node child = parent.addChild(new Data(""));
                child.data.path = makeVersion(child.id);
                changeActiveVersion(child.id);
                writeJSONFromRoot();
                return child.id;
            }
        }
        logger.error("Quickpose: Attempted Fork: Node Doesn't Exist");
        return -1;
    }

    private void changeActiveVersion(int id) {

        if (!codeTree.idExists(id)) {
            logger.error("Quickpose: Attempted to change active version to invalid Id");
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
            renderLock.lock();
            for (File f : versionListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde") || f.getName() == "render.png") {
                    File newFile = new File(sketchFolder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
            }
            renderLock.unlock();
        }
        // Something here breaks REPL mode
        if (codeTree.getNode(id).children.size() == 0){
            // editor.getSketch().getMainFile().setWritable(true);
            editor.getTextArea().setEditable(true);
            editor.getTextArea().getPainter().setBackground(Color.WHITE);
            //editor.getConsole().setName("Dont");
            editor.statusEmpty();
            //editor.repaint();
        } else {
            // editor.getSketch().getMainFile().setWritable(false);
            // editor.getSketch().getMainFile().setReadOnly();
            editor.getTextArea().setEditable(false);
            editor.statusNotice("Sketch is Read Only Because It Has Child Nodes");
            editor.getTextArea().getPainter().setBackground(Color.LIGHT_GRAY);
            //editor.repaint();
        }
        // base.getActiveEditor().getSketch().updateSketchCodes();
        editor.getSketch().reload();
        editor.handleSave(true);
        editor.getTextArea().setCaretPosition(codeTree.getNode(id).data.caretPosition);
        //System.out.println("setcaretto"+codeTree.getNode(id).data.caretPosition);
        currentVersion = id;
        // System.out.println("Switched version to "+ id );
    }

    private String makeVersion(int id) {
        // base.getActiveEditor().getSketch().reload();

        editor.handleSave(true);
        File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        folder.mkdir();
        File[] dirListing = sketchFolder.listFiles();
        if (dirListing != null) {
            for (File f : dirListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde")) {
                    File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
                renderLock.lock();
                if (FilenameUtils.equals(f.getName(), "render.png")) {
                    File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
                renderLock.unlock();
            }
        }
        return folder.getAbsolutePath();
    }
    private void writeJSONFromRoot() {
        if (versionsTree.exists()) {
            versionsTree.delete();
        }
        try {
            versionsTree.createNewFile();
        } catch (IOException e) {
            logger.error("Exception Occured in writeJSONFromRoot: " + e.toString());
        }
        try {
            JSON.std.write(JSON.std.anyFrom(codeTree.getJSONSave(currentVersion,sketchFolder.getName())), versionsTree.getAbsoluteFile());
        } catch (IOException e) {
            logger.error("Error Saving JSON to File");
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

            int rootInd = JSONSearch(nodes, 0);
            if (rootInd == -1) {
                logger.error("Couldn't Find Root Node on Import");
            }
            Data root = new Data(nodes.getJSONObject(rootInd).getString("path"));
            root.setCaretPosition(nodes.getJSONObject(rootInd).getInt("caretPosition"));
            importTree = new Tree(root);

            while (importTree.getList().size() < nodes.length() - 1) {
                for (int i = 0; i < edges.length(); i++) {
                    int source = Integer.parseInt(edges.getJSONObject(i).getString("source"));
                    int target = Integer.parseInt(edges.getJSONObject(i).getString("target"));
                    if (importTree.idExists(source) && !importTree.idExists(target)) {
                        Node parent = importTree.getNode(source);
                        int childInd = JSONSearch(nodes, target);
                        Data data = new Data(nodes.getJSONObject(childInd).getString("path"));
                        data.setCaretPosition(nodes.getJSONObject(childInd).getInt("caretPosition"));
                        Node child = parent.setChild(data, target);
                        logger.info("created child : " + target + " from parent : "+ source);
                    }
                }
            }
            codeTree = importTree;
            changeActiveVersion(graph.getInt("CurrentNode"));
        } catch (Exception e) {
            logger.error("Exception in Building Tree : " + e.toString());
        }
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
            System.out.println(e.getMessage());
        }
    }
}
