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
import static spark.Spark.*;
import com.fasterxml.jackson.jr.ob.JSON;

import java.awt.Color;
import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.ZipFile;

import javax.servlet.*;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.eclipse.jetty.websocket.api.*;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import processing.app.Base;
import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.tools.Tool;
import processing.app.ui.Editor;
import processing.app.ui.EditorStatus;
import spark.Spark;

import java.util.concurrent.*;

import java.time.LocalDateTime;  


// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class Quickpose implements Tool {
    Base base;
    boolean setup = false;
    private File sketchFolder;
    private File assetsFolder;
    private File archiveFolder;
    private File exportFolder;
    private File versionsCode;
    private File versionsTree;
    public Tree codeTree;
    public int currentVersion;
    public Editor editor;
    ScheduledExecutorService windowExecutor;
    JSONObject nodePositions;
    private int serverPort = 8080;
    ScheduledExecutorService executor = null;
    private Lock renderLock = new ReentrantLock();
    private Lock tldrLock = new ReentrantLock();
   
    private org.slf4j.Logger logger = LoggerFactory.getLogger(Quickpose.class);
    
    private java.util.logging.Logger archiver = java.util.logging.Logger.getLogger("ArchiveLog");  

    FileHandler logFileHandler; 
    

    Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    ThumbnailWebSocket handler = new ThumbnailWebSocket(sessions,logger,archiver);


    Runnable updateLoop = new Runnable() {
        public void run() {
            update();
        }
    };

    public String getMenuTitle() {
        return "Quickpose";
    }

    public void init(Base base) {
        // Store a reference to the Processing application itself
        this.base = base;
        
        Logger root = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.ERROR);
        archiver.setUseParentHandlers(false);
    }

    public void run() {
        
        

        if (base.getActiveEditor().getSketch().isUntitled()) {
            Messages.showMessage("Quickpose: Unsaved Sketch","Quickpose: Unsaved Sketch -- Please Save Sketch and Run Again");
            base.getActiveEditor().handleSave(false);

        } else {
            System.out.println("##tool.name## (v##tool.prettyVersion##) by ##author.name##");
            dataSetup();
            if (setup == false) {
                networkSetup();
            }else{
                Spark.init();
            }
            if (executor != null) {
                executor.shutdownNow();
            }
            executor = Executors.newScheduledThreadPool(2);
            executor.scheduleAtFixedRate(updateLoop, 0, 100, TimeUnit.MILLISECONDS);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                  stop();
                  executor.shutdown();
                  if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    System.out.println("Still waiting 100ms...");
                    executor.shutdownNow();
                  }
                  System.out.println("System exited gracefully");
                } catch (InterruptedException e) {
                  executor.shutdownNow();
                }
              }));
              setup = true;
        }
    }
private void update() {
    try {
        if(renderLock.tryLock(90, TimeUnit.MILLISECONDS)){
            BSONObject obj = new BasicBSONObject();
            obj.put("version_id", currentVersion);
            obj.put("project_name", editor.getSketch().getName());
            try{
                //System.out.println("update");
                File render = new File(sketchFolder.getAbsolutePath() + "/render.png");
                File storedRender = new File(versionsCode.getAbsolutePath() + "/_" + currentVersion + "/render.png");
                boolean fileModified = base.getActiveEditor().getSketch().isModified();
                boolean renderModified = render.exists() && (!storedRender.exists() || render.lastModified() != storedRender.lastModified());
                codeTree.getNode(currentVersion).data.setCaretPosition(editor.getTextArea().getCaretPosition());
                if (fileModified) {
                    makeVersion(currentVersion);
                } else if (renderModified && FileUtils.sizeOf(render) > FileUtils.ONE_KB) {
                    Utils.copyFile(render, storedRender);
                    byte[] bytes = FileUtils.readFileToByteArray(storedRender);
                    obj.put("image", bytes);
                }
            }finally {
                renderLock.unlock();
            }
            handler.broadcastData(ByteBuffer.wrap(BSON.encode(obj)));
        }
    } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    if (codeTree.getNode(currentVersion).children.size() != 0){
        //editor.statusNotice("Sketch is Read Only Because It Has Child Nodes");
        //editor.statusMessage("Sketch is Read Only Because It Has Child Nodes",EditorStatus.NOTICE);
    }else if (editor.getStatusMessage()=="Sketch is Read Only Because It Has Child Nodes"){
        editor.statusEmpty();
    }

    if(sessions.isEmpty()){
        editor.statusMessage("Quickpose: Looking for Browser Session...",EditorStatus.WARNING);
    }else if (editor.getStatusMessage()=="Quickpose: Looking for Browser Session..."){
        editor.statusMessage("Quickpose: Found Browser Session",EditorStatus.NOTICE);
    }
}

    private void networkSetup() {
        webSocket("/thumbnail", handler);
        port(serverPort);
        int maxThreads = 8;
        threadPool(maxThreads);
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
            int childID = 0;
            logger.info("Fork ID:" + Integer.parseInt(request.params(":id")));
            childID = fork(Integer.parseInt(request.params(":id")));
            if (childID > 0) {
                archiver.info("Forked"+Integer.parseInt(request.params(":id")) + "to" + childID);
                currentVersion = childID;
                return codeTree.getJSONSave(currentVersion, sketchFolder.getName());
            }
            response.status(500);
            
            return response;
        });
        get("/select/:id", (request, response) -> {
            archiver.info("Selected"+Integer.parseInt(request.params(":id"))+"| From:"+currentVersion);
            currentVersion = changeActiveVersion(Integer.parseInt(request.params(":id")));
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
            File dest = new File(versionsCode.toPath() + "/quickpose.tldr");
            File f = new File(versionsCode.toPath() + "/quickposeTemp.tldr");
            tldrLock.lock();
            try {
                String proj = request.queryParams("ProjectName");
                if(proj.contentEquals(sketchFolder.getName()) || proj.contentEquals("null")){
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
                    // getPart needs to use same "name" as input field in form
                    try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { 
                        Files.copy(input, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    if(editor.getStatusMessage().contentEquals("Quickpose: old tldr file in browser, please reload browser window")){
                        editor.statusMessage("Quickpose: Browser Reloaded",EditorStatus.NOTICE);
                    }
                }else{
                    editor.statusMessage("Quickpose: old tldr file in browser, please reload browser window",EditorStatus.WARNING);
                    //editor.statusMessage(message, type);
                    //editor.statusMessage("Quickpose: old tldr file in browser, please reload browser window",EditorStatus.WARNING);
                    request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
                    // getPart needs to use same "name" as input field in form
                    try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { 
                        Files.copy(input, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } finally {
                tldrLock.unlock();
            }
            return "Success";
        });
        post("/log", (request, response) -> {
            //System.out.println(request.body());
            tldrLock.lock();
            try{
                archiver.info(request.body());
            }finally{
                tldrLock.unlock();
            }
            return "Success";
        });
        post("/tldrfile_backup", (request, response) -> {
            File f = new File(archiveFolder.toPath() + "/quickpose"+LocalDateTime.now().toString().replace(':', '-') +".tldr");
            tldrLock.lock();
            try {
                String proj = request.queryParams("ProjectName"); //request.attribute("ProjectName");
                if(proj.contentEquals(sketchFolder.getName())){
                request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(""));
                // getPart needs to use same "name" as input field in form
                    try (InputStream input = request.raw().getPart("uploaded_file").getInputStream()) { 
                        Files.copy(input, f.toPath(), StandardCopyOption.REPLACE_EXISTING);    
                        archiver.info("Backed Up File"+f.getName());
                        // Copy a file into the zip file
                        List<File> backups = new ArrayList<File>();
                        List<File> fileList = new ArrayList<File>();
                            fileList.addAll(backups);
                        for (File backup : archiveFolder.listFiles()){
                            if(FilenameUtils.isExtension(backup.getName(), "tldr")){
                                backups.add(backup);
                                fileList.add(backup);
                            }
                        }
                        if(backups.size() > 100){
                            // archive.setRunInThread(true);
                            // archiver.info("Compressed Backups:"+archive.getFile().getName());
                            ZipParameters zipParameters = new ZipParameters();
                            zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
                            System.out.println(backups.toString());
                            try (ZipFile archiveZip = new ZipFile(archiveFolder.getAbsolutePath()+"/archive" + LocalDateTime.now().toString().replace(':', '-') + ".zip")) {
                                archiveZip.setRunInThread(false);
                                archiveZip.createSplitZipFile(fileList, zipParameters, true, 300000000);
                                for (File backup : backups){
                                    backup.delete();
                                }
                            }
                            // archive.addFiles(backups,zipParameters);
                            // archive.close();
                            
                        }
                        

                        
                    }
                }
            } finally {
                tldrLock.unlock();
            }
            return "Success";
        });
        get("/tldrfile", (request, response) -> {
            File f = new File(versionsCode.toPath() + "/quickpose.tldr");
            if (f.exists()) {
                tldrLock.lock();
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
            } else {
                response.status(201);
                return response;
            }
        });
        post("/exportbycolor", (request, response) -> {
            String ids = request.queryParams("ids");
            String color = request.queryParams("color");
            if(!ids.contentEquals("")){
                String [] versionsStrings = ids.split(",");
                ArrayList<Integer> versions = new ArrayList<Integer>();
                for(String s : versionsStrings){
                    versions.add(Integer.valueOf(s));
                }
                File export = new File(exportFolder.toPath()+"/"+color+"_export_"+LocalDateTime.now().toString().replace(':', '-'));
                export.mkdir();

                for(Integer i : versions){
                    File versionFolder = new File(versionsCode.getAbsolutePath() + "/_" + i);
                    if(versionFolder.exists()){
                        File destFolder = new File(export + "/_" + i);
                        destFolder.mkdir();
                        FileUtils.copyDirectory(versionFolder, destFolder);
                        for(File f : versionFolder.listFiles()){
                            if (FilenameUtils.equals(f.getName(), "render.png")) {
                                renderLock.lock();
                                try{
                                    File newFile = new File(export + "/" + "render"+i+".png");
                                    copyFile(f, newFile);
                                } finally {
                                    renderLock.unlock();
                                }
                            }
                        }
                    }
                }  
            }
            
            return "Success";
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
                try{
                    try (OutputStream out = response.raw().getOutputStream()) {
                        response.header("Content-Disposition", "inline; filename=render.png");
                        Files.copy(f.toPath(), out);
                        out.flush();
                    }
                }finally{
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
            archiver.info("Asset Upload"+request.splat()[0]);
            return "Success";
        });
    }

    private void dataSetup() {
        editor = base.getActiveEditor();
        sketchFolder = editor.getSketch().getFolder();
        String sketchPath = sketchFolder.getAbsolutePath();
        versionsCode = new File(sketchPath + "/" + "Quickpose");
        versionsCode.mkdir();
        assetsFolder = new File(versionsCode.toPath() + "/" + "assets");
        assetsFolder.mkdir();
        archiveFolder = new File(versionsCode.toPath() + "/" + "archive");
        archiveFolder.mkdir();
        exportFolder = new File(versionsCode.toPath() + "/" + "exports");
        exportFolder.mkdir();
        File starterCodeFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/QuickposeDefault.pde");
        File starterCatFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/cat.png");
        File startertldr = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/quickpose.tldr");


        
        try {  

            // This block configure the logger with handler and formatter  
            logFileHandler = new FileHandler(archiveFolder.getPath()+"/quickpose.log",true);  
            archiver.addHandler(logFileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            logFileHandler.setFormatter(formatter);  
            archiver.setUseParentHandlers(false);
    
        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  


        if (editor.getMode().getIdentifier() == "jm.mode.replmode.REPLMode") {
            logger.info("Quickpose: Running in REPL Mode");
            archiver.info("Start Session in REPL Mode");
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
        if (editor.getMode().getIdentifier() != "jm.mode.replmode.REPLMode") {
            archiver.info("Wasn't Able To Start in REPL Mode");
            editor.statusMessage("Quickpose: Wasn't Able To Start in REPL Mode", EditorStatus.WARNING);
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
                archiver.info("Fork Version:"+id+"|To:"+child.id);
                changeActiveVersion(child.id);
                writeJSONFromRoot();
            
                return child.id;
            }
        }
        logger.error("Quickpose: Attempted Fork: Node Doesn't Exist");
        return -1;
    }

    private int changeActiveVersion(int id) {

        if (!codeTree.idExists(id)) {
            logger.error("Quickpose: Attempted to change active version to invalid Id"+id);
            archiver.info("Quickpose: Attempted to change active version to invalid Id"+id);
            return -1;
        }
        File[] sketchListing = sketchFolder.listFiles();
        if (sketchListing != null) {
            for (File f : sketchListing) {
                if (FilenameUtils.equals(f.getName(), "render.png")) {
                    renderLock.lock();
                    try{
                        f.delete();
                    }finally{
                        renderLock.unlock();
                    }

                    
                }
            }
        }
        
        File versionFolder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        File[] versionListing = versionFolder.listFiles();
        if (versionListing != null) {
            for (File f : versionListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde")) {
                    File newFile = new File(sketchFolder.getAbsolutePath() + "/" + f.getName());
                    copyFile(f, newFile);
                }
                if (f.getName() == "render.png"&& FileUtils.sizeOf(f) > FileUtils.ONE_KB) {
                    renderLock.lock();
                    try{
                        File newFile = new File(sketchFolder.getAbsolutePath() + "/" + f.getName());
                        copyFile(f, newFile);
                    }finally{
                        renderLock.unlock();
                    }
                }
            }
        }
        
        editor.getSketch().reload();
        editor.handleSave(true);
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
            editor.statusMessage("Sketch is Read Only Because It Has Child Nodes",EditorStatus.NOTICE);
            //editor.statusNotice("Sketch is Read Only Because It Has Child Nodes");
            editor.getTextArea().getPainter().setBackground(Color.LIGHT_GRAY);
            //editor.repaint();
        }
        // base.getActiveEditor().getSketch().updateSketchCodes();
        
        editor.getTextArea().setCaretPosition(codeTree.getNode(id).data.caretPosition);
        //System.out.println("setcaretto"+codeTree.getNode(id).data.caretPosition);
    
        editor.getSketch().reload();
        // sketchListing = sketchFolder.listFiles();
        // if (sketchListing != null) {
        //     for (File f : sketchListing) {
        //         if (FilenameUtils.equals(f.getName(), "render.png")) {
        //             renderLock.lock();
        //             try{
        //                 f.delete();
        //             }finally{
        //                 renderLock.unlock();
        //             }
        //         }
        //     }
        // }
    
        
        return id;
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
                if (FilenameUtils.equals(f.getName(), "render.png") && FileUtils.sizeOf(f) > FileUtils.ONE_KB) {
                    renderLock.lock();
                    try{
                        File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                        copyFile(f, newFile);
                    }finally{
                        renderLock.unlock();
                    }
                    
                }
            }
        }
    
        String relPath = sketchFolder.toPath().relativize(folder.toPath()).toString();
        return relPath;
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

