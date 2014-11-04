package experiments.tools;

/**
 *
 * @author Devin Calado
 */

/*
This class provides tools for creating a csv file.
*/
public class CsvTool {
	
    private final FileIO fileio;
    private final int numberOfCols;
    
    public CsvTool(String filename, String[] cols){
        fileio = new FileIO(filename+".csv");
        setColumns(cols);
        numberOfCols = cols.length;
        
    }
    
    /*
    Private method used by the constructor in order to create the column header in the .csv file
    */
    private void setColumns(String[] cols){
        String titleRow = "";
        for (String col : cols){
            titleRow = titleRow.concat(col + " ,");
            
        }
        titleRow = (String) titleRow.subSequence(0, titleRow.length()-1);
        fileio.appendToFile(titleRow);
    }
    
    /*
    Append a row of data to the csv file. 
    */
    public void appendRow(String[] dataToAdd){
        String row = "";
        for(String data : dataToAdd){
            row = row.concat(data + " ,");
        }
        row = (String) row.subSequence(0, row.length()-1);
        fileio.appendToFile(row);
    }

    public FileIO getFileio() {
        return fileio;
    }

    public int getNumberOfCols() {
        return numberOfCols;
    }
    
    public void closeFileIO(){
        fileio.closeWriter();
    }
    
}

