package template.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;

import processing.app.syntax.JEditTextArea;

public class Utils {
	public static String getPath(String theFilename) {
		if (theFilename.startsWith("/")) {
			return theFilename;
		}
		return File.separator + "data" + File.separator + theFilename;
	}
	public static void copyFile(File in, File out) 
    {
        try{
            if(in.exists() && !FileUtils.contentEquals(in, out)){
                FileChannel inChannel = new FileInputStream(in).getChannel();
                FileChannel outChannel = new FileOutputStream(out).getChannel();
                try {
                    inChannel.transferTo(0, inChannel.size(),outChannel);
                } 
                catch (IOException e) {
                }
                finally {
                    if (inChannel != null) inChannel.close();
                    if (outChannel != null) outChannel.close();
                }
            }
        }catch(IOException e){
        } 
    }
    public static void commentMainFile(JEditTextArea textarea, String commentPrefix){
        System.out.println(textarea.getFirstLine() + "first");
        System.out.println(textarea.getLastLine() + "last");
        System.out.println(textarea.getLineCount() + "count");
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
              System.out.println(line);
             
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
            } catch (IOException e) { 
                System.out.println(e);
                return "";
            }
        }
        return result;
    }
    private static void copyFileSafe(File src, File dest) {
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
