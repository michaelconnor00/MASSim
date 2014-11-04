package experiments.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Devin Calado  -  This class is intended to be used with the CsvTool class.
 */
public class FileIO {

    FileWriter writer;
    FileReader reader;
    File f;
    String fileName;

    public FileIO(String name) {
        try {
            if (f == null || !f.exists()) {   // Create a new file if one does not exist already.
                createNewFile(name);
            }
            writer = new FileWriter(name, true);
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void createNewFile(String fileName) {
        this.f = new File(fileName);
        this.fileName = fileName;
    }

    public void appendToFile(String text) {
        try {
            writer.write(text + System.lineSeparator());//appends the string to the file
            writer.flush();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    public void closeWriter() {
        try {
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String readContentsToString(){
        String contents = "";
        try {
            contents = new Scanner( new File(fileName) ).useDelimiter("\\A").next();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        return contents;
    }
    
    public String[] readContentLinesToStringArray(){
    	
        String[] result;
        String contents = "";
        // get the contents of the file into a string
        try {
            contents = new Scanner(new File(fileName)).useDelimiter("\\A").next();
        }
        catch (FileNotFoundException ex){
            System.out.println("ERROR: No lines to be read from file: "+ this.fileName);
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // split to string array
        result  = contents.split("\\r?\\n");
        
        return result;
    }
    

    public void clearFile(){
        FileWriter eraseFile = null;
        try {
            eraseFile = new FileWriter(fileName, false);
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            eraseFile.write("");
        } catch (IOException ex) {
            Logger.getLogger(FileIO.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        
    }
  
}
