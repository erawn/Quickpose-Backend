package template.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;


import org.apache.commons.io.FileUtils;

import processing.app.syntax.JEditTextArea;

public final class Utils {
    public static java.util.logging.Logger archiver;
    public static org.slf4j.Logger logger;

    public static void init(org.slf4j.Logger log,java.util.logging.Logger archive){
        logger = log;
        archiver = archive; 
    }

    public static org.slf4j.Logger getLogger(){
        return logger;
    }
    
	public static String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}
	public static void copyFile(File in, File out) 
    {
        // File file = new File("/home/developer/test.iso");
        // File oFile = new File("/home/developer/test2");

        // long time1 = System.currentTimeMillis();
        // FileInputStream is = new FileInputStream(file);
        // FileOutputStream fos = new FileOutputStream(oFile);
        // FileChannel f = is.getChannel();
        // FileChannel f2 = fos.getChannel();

        // ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024);
        // long len = 0;
        // while((len = f.read(buf)) != -1) {
        //     buf.flip();
        //     f2.write(buf);
        //     buf.clear();
        // }

        // f2.close();
        // f.close();
        try{
            if(in.exists()){
                FileInputStream inStream = new FileInputStream(in);
                FileOutputStream outStream = new FileOutputStream(out);
                FileChannel inChannel = inStream.getChannel();
                FileChannel outChannel = outStream.getChannel();
       
                try {
                    long bytesTransferred = 0;
                    while(bytesTransferred < inChannel.size()){
                        bytesTransferred += inChannel.transferTo(bytesTransferred, inChannel.size(), outChannel);
                    }
                    inChannel.force(true);
                    outChannel.force(true);
   
                } 
                catch (IOException e) {
                    //archiver.info(e.getMessage());
                }
                finally {
                    if (outStream != null) outStream.flush();
                   
                    if (inChannel != null) inChannel.close();
                    if (inStream != null) inStream.close();

                    if (outChannel != null) outChannel.close();
                    if (outStream != null) outStream.close();
                }
            }
        }catch(IOException e){
            //archiver.info(e.getMessage());
        } 
    }
//oldFolder is base folder (aka sketchFolder), new folder is the version folder
//new folder is the folder thats *supposed* to be newer
    public static Boolean compareDirs(File oldFolder, File newFolder){
        if(oldFolder.isDirectory() && newFolder.isDirectory()){
            FileFilter processingFilefilter = new FileFilter() {
                public boolean accept(File file) {
                  if (file.getName().endsWith(".pde")) {
                    return true;
                  }
                  return false;
                }
              };
            for(File f_old : oldFolder.listFiles(processingFilefilter)){
                File f_new = new File(newFolder.getAbsolutePath()+"/"+f_old.getName());
                try {
                    if(FileUtils.directoryContains(newFolder, f_new)){
                        if(!FileUtils.contentEquals(f_new,f_old)){
                        //System.out.println("files arent the same"+f_new.getName()+f_old.getName());
                            return false;
                        }
                    }else{
                        //System.out.println("dir doesn't contain"+f_new.getName());
                        return false;
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    archiver.info(e.getMessage());
                }
            }
            return true;
        }else{
            return false;
        }
    }

  
    public static void commentMainFile(JEditTextArea textarea, String commentPrefix){
        // System.out.println(textarea.getFirstLine() + "first");
        // System.out.println(textarea.getLastLine() + "last");
        // System.out.println(textarea.getLineCount() + "count");
        for (int line = textarea.getFirstLine(); line < textarea.getLineCount(); line++) {
    
            //int location = textarea.getLineStartNonWhiteSpaceOffset(line);
            String lineText = textarea.getLineText(line);
            if (lineText.trim().length() == 0)
              continue; //ignore blank lines
              // add a comment
              int location = textarea.getLineStartOffset(line);
              textarea.select(location, location);
              
              if(location >= 0){
                textarea.setSelectedText(commentPrefix);
              }
              //System.out.println(line);
             
              textarea.getPainter().repaint();
          }
    }

    public static String readFile(File in){
        String record = "";
        String result = "";
        if(in.exists()){
            try { 
                FileReader fr = new FileReader(in);
                BufferedReader br = new BufferedReader(fr);
                record = new String();
                while ((record = br.readLine()) != null) {
                    result += record+"\n";
                } 
                br.close();
            } catch (IOException e) { 
                archiver.info(e.getMessage());
                return "";
            }
        }
        return result;
    }
}
