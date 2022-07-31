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

import static spark.Spark.*;
import com.fasterxml.jackson.jr.ob.JSON;

import java.awt.Color;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.ZipFile;

import javax.servlet.MultipartConfigElement;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.eclipse.jetty.websocket.api.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.json.JSONArray;
import org.json.JSONObject;

import processing.app.Base;
import processing.app.syntax.JEditTextArea;
import processing.app.tools.Tool;
import processing.app.ui.Editor;
import processing.app.ui.EditorStatus;
// import processing.core.*;
// import processing.app.syntax.*;
import spark.Spark;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.time.LocalDateTime;  


// when creating a tool, the name of the main class which implements Tool must
// be the same as the value defined for project.name in your build.properties
public class Quickpose implements Tool {
    Base base;
    public Editor editor;
    private int serverPort = 8080;
    ScheduledExecutorService executor = null;
    private File sketchFolder;
    private File assetsFolder;
    private File archiveFolder;
    private File exportFolder;
    private File versionsCode;
    private File versionsTree;

    public Tree codeTree;
    public int currentVersion;
  
    volatile boolean setup = false;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(Quickpose.class);
    private java.util.logging.Logger archiver = java.util.logging.Logger.getLogger("ArchiveLog"); 
    FileHandler logFileHandler;

    private Lock renderLock = new ReentrantLock();
    private Lock tldrLock = new ReentrantLock();
    Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    Queue<String> messages = new ConcurrentLinkedQueue<>();
    ThumbnailWebSocket handler = new ThumbnailWebSocket(messages,sessions,archiver);

    public void run() {
        //make new if on existing file 
        if (base.getActiveEditor().getSketch().isUntitled()) {
            base.getActiveEditor().handleSave(false);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(updateLoop, 0, 100, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                resetState();
            }));

        base.getActiveEditor().addWindowListener(new WindowListener(){
            public void windowClosed(WindowEvent e) {
                resetState();
            }
            public void windowClosing(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowActivated(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
            public void windowOpened(WindowEvent e) {}
        });
    }

private void update() {
    if(setup == false){
        if(base.getActiveEditor().getSketch().isSaving()){
            return;
        }
        System.out.println("##tool.name## (v##tool.prettyVersion##) by ##author.name##");
        dataSetup();
        networkSetup();
        Spark.init();
        setup = true;
    }else{
        try {
            if(renderLock.tryLock(90, TimeUnit.MILLISECONDS)){
                try{
                    BSONObject obj = new BasicBSONObject();
                    obj.put("version_id", currentVersion);
                    obj.put("project_name", sketchFolder.getName());
                    String msg = messages.poll();
                    while(msg != null){
                        if(msg.contentEquals("/tldrfile") && !(obj.containsField("tldrfile"))){
                            File f = new File(versionsCode.toPath() + "/quickpose.tldr");
                            byte[] bytes = FileUtils.readFileToByteArray(f);
                            obj.put("tldrfile", bytes);
                            obj.put("versions",codeTree.getJSONSave(currentVersion, sketchFolder.getName()).getBytes());
                        }
                        msg = messages.poll();
                    }
                    File render = new File(sketchFolder.getAbsolutePath() + "/render.png");
                    File currentVersionFolder = new File(versionsCode.getAbsolutePath() + "/_" + currentVersion);
                    File storedRender = new File(currentVersionFolder + "/render.png");
                    boolean fileModified = base.getActiveEditor().getSketch().isModified();
                    boolean renderModified = render.exists() && (!storedRender.exists() || render.lastModified() != storedRender.lastModified());
                    codeTree.getNode(currentVersion).data.setCaretPosition(editor.getTextArea().getCaretPosition());
                    if (fileModified) {
                        makeVersion(currentVersion);
                    } else if (renderModified && FileUtils.sizeOf(render) > FileUtils.ONE_KB) {
                        Utils.copyFile(render, storedRender);
                        byte[] bytes = FileUtils.readFileToByteArray(storedRender);
                        obj.put("image", bytes);
                        if(!shouldCheckpoint(currentVersion)){
                            File checkpointFolder = new File(currentVersionFolder.getAbsolutePath() + "/checkpoint" + (codeTree.getNode(currentVersion).data.checkpoints-1));
                            File checkpointRender = new File(checkpointFolder.getAbsolutePath() +"/render.png");
                            Utils.copyFile(render,checkpointRender);
                        }
                    }
                    handler.broadcastData(ByteBuffer.wrap(BSON.encode(obj)));
                        
                    if (codeTree.getNode(currentVersion).children.size() != 0){
                        editor.statusMessage("Sketch is Read Only Because It Has Child Nodes",EditorStatus.NOTICE);
                    }else if (editor.getStatusMessage()=="Sketch is Read Only Because It Has Child Nodes"){
                        editor.statusEmpty();
                    }
                    if(sessions.isEmpty()){
                        editor.statusMessage("Quickpose: Looking for Browser Session...",EditorStatus.WARNING);
                    }else if (editor.getStatusMessage()=="Quickpose: Looking for Browser Session..."){
                        editor.statusMessage("Quickpose: Found Browser Session",EditorStatus.NOTICE);
                    }
                }finally {
                    renderLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            archiver.info(e.getMessage());
        } catch (IOException e) {
            archiver.info(e.getMessage());
        }
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
                    String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header("Access-Control-Allow-Headers",accessControlRequestHeaders);
                    }
                    String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header("Access-Control-Allow-Methods",accessControlRequestMethod);
                    }
                    return "OK";
                });
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));

        get("/", (request, response) -> {
            response.redirect("https://quickpose.vercel.app/");
            return response;
        });
        get("/versions.json", (request, response) -> {
            response.type("application/json");
            try{
                String json = codeTree.getJSONSave(currentVersion, sketchFolder.getName());
                return json;
            }catch(Error e){
                archiver.info(e.getMessage());
            }
            response.status(500);
            return response;
        });
        get("/fork/:id", (request, response) -> {
            String param = request.queryParams("Autorun");
            boolean autorun = (param.contentEquals("true")) ? true : false;
            int childID = fork(Integer.parseInt(request.params(":id")),autorun);
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
            String param = request.queryParams("Autorun");
            boolean autorun = (param.contentEquals("true")) ? true : false;
            currentVersion = changeActiveVersion(Integer.parseInt(request.params(":id")),autorun);
            return currentVersion;
        });
        post("/tldrfile", (request, response) -> {
            File dest = new File(versionsCode.toPath() + "/quickpose.tldr");
            File f = new File(versionsCode.toPath() + "/quickposeTemp.tldr");
            tldrLock.lock();
            try {
                String proj = request.queryParams("ProjectName");
                if(proj.contentEquals(sketchFolder.getName()) || proj.contentEquals("null")|| proj.contentEquals("")){
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
                    archiver.info("Old Tldr File Conflict: Project Name: "+proj+"|Sketch Name:"+sketchFolder.getName());
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
            archiver.info(request.body());
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
                            ZipParameters zipParameters = new ZipParameters();
                            zipParameters.setCompressionLevel(CompressionLevel.ULTRA);
                            //System.out.println(backups.toString());
                            try (ZipFile archiveZip = new ZipFile(archiveFolder.getAbsolutePath()+"/archive" + LocalDateTime.now().toString().replace(':', '-') + ".zip")) {
                                archiveZip.setRunInThread(false);
                                archiveZip.createSplitZipFile(fileList, zipParameters, true, 300000000);
                                for (File backup : backups){
                                    backup.delete();
                                }
                            }
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
                archiver.info("Export ids:"+ids+"|by color:"+color);
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
                                    Utils.copyFile(f, newFile);
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

    private int fork(int id, boolean autorun) {
        if (codeTree.idExists(id)) {
            Node parent = codeTree.getNode(id);
            if (parent != null) {
                changeActiveVersion(id,false);
                Node child = parent.addChild(new Data(""));
                child.data.path = makeVersion(child.id);
                archiver.info("Fork Version:"+id+"|To:"+child.id);
                changeActiveVersion(child.id, autorun);
                writeJSONFromRoot();
                return child.id;
            }
        }

        archiver.info("Quickpose: Attempted Fork: Node Doesn't Exist");
        return -1;
    }

    private int changeActiveVersion(int id, boolean autorun) {
        makeVersion(currentVersion);
        promptCheckpoint(id);
        if (!codeTree.idExists(id)) {
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
                    Utils.copyFile(f, newFile);
                }
                if (f.getName() == "render.png"&& FileUtils.sizeOf(f) > FileUtils.ONE_KB) {
                    renderLock.lock();
                    try{
                        File newFile = new File(sketchFolder.getAbsolutePath() + "/" + f.getName());
                        Utils.copyFile(f, newFile);
                    }finally{
                        renderLock.unlock();
                    }
                }
            }
        }
        if (codeTree.getNode(id).children.size() == 0){
            editor.getPdeTextArea().setEditable(true);
            editor.getTextArea().updateTheme();
            editor.statusEmpty();
        } else {
            editor.getPdeTextArea().setEditable(false);
            editor.statusMessage("Sketch is Read Only Because It Has Child Nodes",EditorStatus.WARNING);
            editor.getTextArea().getPainter().setBackground(Color.LIGHT_GRAY);
        }
        editor.getTextArea().setCaretPosition(codeTree.getNode(id).data.caretPosition);
        editor.getTextArea().blinkCaret();
        editor.getSketch().reload();
        editor.handleSave(true);
        //editor.getToolbar().addPropertyChangeListener(listener);
        if(autorun){
            editor.getToolbar().handleRun(0);
        }
 
        return id;
    }

    private String makeVersion(int id) {
        editor.handleSave(true);
        File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        folder.mkdir();
        File[] dirListing = sketchFolder.listFiles();
        if (dirListing != null) {
            for (File f : dirListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde")) {
                    File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                    Utils.copyFile(f, newFile);
                }
                if (FilenameUtils.equals(f.getName(), "render.png") && FileUtils.sizeOf(f) > FileUtils.ONE_KB) {
                    renderLock.lock();
                    try{
                        File newFile = new File(folder.getAbsolutePath() + "/" + f.getName());
                        Utils.copyFile(f, newFile);
                    }finally{
                        renderLock.unlock();
                    }
                }
            }
        }
    
        String relPath = sketchFolder.toPath().relativize(folder.toPath()).toString();
        return relPath;
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

        class checkpoint implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                promptCheckpoint(currentVersion);
            }
        }
        final ActionListener CHECKPOINT = new checkpoint();
        editor.getTextArea().getInputHandler().addKeyBinding("C+S", CHECKPOINT);
        editor.getTextArea().getInputHandler().addKeyBinding("M+S", CHECKPOINT);


        try {  
            // This block configure the logger with handler and formatter  
            logFileHandler = new FileHandler(archiveFolder.getPath()+"/quickpose_%g.log",FileUtils.ONE_MB * 50, 10000, true);  
            archiver.addHandler(logFileHandler);
            SimpleFormatter formatter = new SimpleFormatter();  
            logFileHandler.setFormatter(formatter);  
            archiver.setUseParentHandlers(false);
            //) logger);
    
        } catch (SecurityException e) {  
            archiver.info(e.getMessage()); 
        } catch (IOException e) {  
            archiver.info(e.getMessage());
        }  

        versionsTree = new File(versionsCode.getAbsolutePath() + "/tree.json");
        if (!versionsTree.exists()) {
            File starterCodeFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/QuickposeDefault.pde");
            File starterCatFile = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/cat.png");
            File startertldr = new File(Base.getSketchbookToolsFolder().toPath() + "/Quickpose/examples/quickpose.tldr");
            archiver.info("Quickpose: No Existing Quickpose Session Detected - creating a new verison history");
            //archiver.info("Creating New Quickpose Session");
            if (starterCodeFile.exists()) {
                JEditTextArea textarea = editor.getTextArea();
                if(textarea.getText().length()>0){
                    Utils.commentMainFile(textarea, editor.getCommentPrefix());
                }
                try {
                    textarea.setText(Utils.readFile(starterCodeFile)+"\n"+textarea.getText());
                    editor.getSketch().save();
                    editor.getSketch().getCode(0).save();
                    File catAsset = new File(assetsFolder.toPath()+"/cat.png");
                    File quickposeFile = new File(versionsCode.toPath()+"/quickpose.tldr");
                    Utils.copyFile(starterCatFile, catAsset);
                    Utils.copyFile(startertldr, quickposeFile);
                } catch (IOException e) {
                    archiver.info(e.getMessage());
                }
                editor.handleSave(true);
            }
            codeTree = new Tree(new Data(makeVersion(0)));
            makeVersion(-1); //Backup first state to compare for checkpoints
            changeActiveVersion(0,false);
            writeJSONFromRoot();
        } else {
            //archiver.info("Retriving Existing Quickpose Session");
            archiver.info("Quickpose: Existing Quickpose Session Found! Loading...");
            readJSONToRoot();
        }
    }
    private void promptCheckpoint(int id){
        if(shouldCheckpoint(id)){
            checkpoint(id);
        }  
    }
    private boolean shouldCheckpoint(int id){
        Boolean shouldCheckpoint = false;
        editor.handleSave(true);
        File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        folder.mkdir();
        if(codeTree.getNode(id).data.checkpoints == 0){
            File parentFolder;
            if(!codeTree.getNode(id).isRoot()){
                parentFolder = new File(versionsCode.getAbsolutePath() + "/_" + codeTree.getNode(id).parent.id);
            }else{
                parentFolder = new File(versionsCode.getAbsolutePath() + "/_" + -1);
            }
            if(!Utils.compareDirs(parentFolder,folder)){
                shouldCheckpoint = true;
            }
        }else{
            File prevCheckpointFolder = new File(folder.getAbsolutePath() + "/checkpoint" + (codeTree.getNode(id).data.checkpoints-1));
            if(!Utils.compareDirs(folder, prevCheckpointFolder)){
                shouldCheckpoint = true;
            }
        }
        return shouldCheckpoint;
    }

    private void checkpoint(int id){
        System.out.println("Quickpose: Created Checkpoint #"+codeTree.getNode(id).data.checkpoints+" of version:"+id);
        editor.statusMessage("Quickpose: Created Checkpoint #"+codeTree.getNode(id).data.checkpoints+" of version:"+id,EditorStatus.NOTICE);
        archiver.info("Quickpose: Created Checkpoint #"+codeTree.getNode(id).data.checkpoints+" of version:"+id);
        File folder = new File(versionsCode.getAbsolutePath() + "/_" + id);
        folder.mkdir();
        File checkpointFolder = new File(folder.getAbsolutePath() + "/checkpoint" + codeTree.getNode(id).data.checkpoints);
        checkpointFolder.mkdir();
        codeTree.getNode(id).data.checkpoints++;
        File[] dirListing = folder.listFiles();
        if (dirListing != null) {
            for (File f : dirListing) {
                if (FilenameUtils.isExtension(f.getName(), "pde")) {
                    File newFile = new File(checkpointFolder.getAbsolutePath() + "/" + f.getName());
                    Utils.copyFile(f, newFile);
                }
                if (FilenameUtils.equals(f.getName(), "render.png") && FileUtils.sizeOf(f) > FileUtils.ONE_KB) {
                    renderLock.lock();
                    try{
                        File newFile = new File(checkpointFolder.getAbsolutePath() + "/" + f.getName());
                        Utils.copyFile(f, newFile);
                    }finally{
                        renderLock.unlock();
                    }
                }
            }
        }
        writeJSONFromRoot();
    }
    private void writeJSONFromRoot() {
        if (versionsTree.exists()) {
            versionsTree.delete();
        }
        try {
            versionsTree.createNewFile();
        } catch (IOException e) {
            archiver.info("Exception Occured in writeJSONFromRoot: " + e.toString());
        }
        try {
            JSON.std.write(JSON.std.anyFrom(codeTree.getJSONSave(currentVersion,sketchFolder.getName())), versionsTree.getAbsoluteFile());
        } catch (IOException e) {
            archiver.info("Error Saving JSON to File");
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
            archiver.info(e.getMessage());
        }
        try {
            JSONObject graph = new JSONObject(input);
            JSONArray nodes = graph.getJSONArray("Nodes");
            JSONArray edges = graph.getJSONArray("Edges");

            int rootInd = JSONSearch(nodes, 0);
            if (rootInd == -1) {
                archiver.info("Couldn't Find Root Node on Import");
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
                        JSONObject node = nodes.getJSONObject(childInd);
               
                        Data data = new Data(node.getString("path"));
                        Iterator<String> keys = node.keys();

                        while(keys.hasNext()) {
                            String key = keys.next();
                            switch(key){
                                case "caretPosition":{
                                    data.setCaretPosition(node.getInt(key));
                                    break;
                                }
                                case "checkpoints":{
                                    data.checkpoints = node.getInt(key);
                                    break;
                                }
                            }
                        }
                        parent.setChild(data, target);
                        archiver.info("created child : " + target + " from parent : "+ source);
                    }
                }
            }
            codeTree = importTree;
            changeActiveVersion(graph.getInt("CurrentNode"),false);
        } catch (Exception e) {
            archiver.info("Exception in Building Tree : " + e.toString());
        }
    }
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
        Utils.init(logger,archiver);

    }

    private void resetState(){
        try {
            archiver.info("Session Shutdown");
            executor.shutdown();
            stop();
            if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            executor.shutdownNow();
            }
            renderLock = new ReentrantLock();
            tldrLock = new ReentrantLock();
            sessions = new ConcurrentLinkedQueue<>();
            messages = new ConcurrentLinkedQueue<>();
            handler = new ThumbnailWebSocket(messages,sessions,archiver);
            executor = null;
            setup = false;
            sketchFolder = null;
            assetsFolder = null;
            archiveFolder = null;
            exportFolder = null;
            versionsCode = null;
            versionsTree = null;
            codeTree = null;
            currentVersion = 0;
            editor = null;
        } catch (InterruptedException err) {
            executor.shutdownNow();
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


}

