/*
 * Created on 25.12.2007
 * 
 * This class is involved in moving logs from files on disk to a database, 
 * where they can be more easily accessed by TASServer web interface.
 * 
 * This is the procedure it follows:
 * 1) Check if temp_logs folder exists. If not, create it.
 * 2) Check if there are any files in temp_logs folder.
 *    If there are, skip to 4), or else continue with 4).
 * 3) Lock access to logs globally in ChanServ, then move
 *    all log files to temp_logs folder.
 * 4) Transfer all logs from temp_logs folder to a database
 *    and remove them from disk.
 *  
 */

/**
 * @author Betalord
 *
 */

import java.sql.SQLException;
import java.util.*;
import java.io.*;
import java.sql.*;

public class LogCleaner extends TimerTask {

	final static String TEMP_LOGS_FOLDER = "./temp_logs";
	
	public void run() {
		// create temp log folder if it doesn't already exist:
		File tempFolder = new File(TEMP_LOGS_FOLDER);
		if (!tempFolder.exists()) {
			boolean success = (tempFolder.mkdir());
			if (!success){
				Log.log("Error: unable to create folder: " + TEMP_LOGS_FOLDER);
				return ;
			} else {
				Log.log("Created missing folder: " + TEMP_LOGS_FOLDER);
			}
		}
		
		// check if temp folder is empty (it should be):
		if (tempFolder.listFiles().length == 0) {
			// OK temp folder is empty, so now we will move all log files to this folder
			
			// lock writing logs to disk while we are moving them to temp folder:
			try {
				Log.logToDiskLock.acquire();
				
				File sourceFolder = new File(Log.LOG_FOLDER);
				File files[] = sourceFolder.listFiles();
				if (files.length == 0) {
					// great, we have no new logs since last time we checked, so we can exit immediately
					return ;
				}
				
				// move each file to temp folder:
				for(int i = 0; i < files.length; i++) {
					File source = files[i];
					if (!source.isFile()) continue;
					if (!source.getName().startsWith("#")) continue; // only move channel logs to database
					
					// move file to new folder:
					if (!source.renameTo(new File(tempFolder, source.getName()))) {
						Log.error("Unable to move file " + source.toString() + " to " + tempFolder.toString());
						return ;
					}
				}
			} catch (InterruptedException e) {
				return ; // this should not happen!
			} finally {
				Log.logToDiskLock.release();	
			}
		}
		
		// now comes the part when we transfer all logs from temp folder to the database:
		File[] files = tempFolder.listFiles();
		for(int i = 0; i < files.length; i++) {
			File file = files[i];

			try {
            	if (!ChanServ.database.doesTableExist(file.getName())) {
            		boolean result= ChanServ.database.execUpdate("CREATE TABLE '" + file.getName() + "' (" + Misc.EOL + 
            													 "id INT NOT NULL AUTO_INCREMENT, " + Misc.EOL +
            													 "stamp timestamp NOT NULL, " + Misc.EOL +
            													 "line TEXT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            		if (!result) {
            			Log.error("Unable to create table '" + file.getName() + "' in the database!");
            			return ;
            		}
            	}
				
    			String update = "start transaction;" + Misc.EOL;
            	
				BufferedReader in = new BufferedReader(new FileReader(file));
				String line;
				
				int lineCount = 0;
	            while ((line = in.readLine()) != null) {
	            	if (line.trim().equals("")) continue; // empty line
	            	
	            	long stamp;
	            	try {
						stamp = Long.parseLong(line.substring(0, line.indexOf(' ')));
	            	} catch (NumberFormatException e) {
	            		Log.error("Line from log file " + file.getName() + " does not contain time stamp. Line is: \"" + line + "\"");
	            		return ;
	            	}
					
	            	if (lineCount == 0) {
		            	update += "INSERT INTO '" + file.getName() + "' (stamp, line) values (" + stamp + ", '" + line.substring(line.indexOf(' ')+1, line.length()) + "')";
	            	} else {
	            		update += ","+ Misc.EOL + 
	            				  "(" + stamp + ", '" + line.substring(line.indexOf(' ')+1, line.length()) + "')";	            		
	            	}
	            	lineCount++;
		        }
	            in.close();
	            
	            update += ";" + Misc.EOL + "commit;";

	            if (lineCount == 0) {
	            	Log.error("Log file is empty: " + file.getName());
	            	// delete the file if possible:
	            	if (file.delete()) {
	            		Log.log("Empty log file was successfully deleted.");
	            	} else {
	            		Log.error("Unable to delete empty log file: " + file.getName());
	            	}
	            	return ;
	            }
	            
	            if (!ChanServ.database.execUpdate(update)) {
	            	Log.error("Unable to execute transaction on the database for the log file " + file.getName());
	            	return ;
	            }
	            
	            // finally, delete the file:
	            if (!file.delete()) {
	            	Log.error("Unable to delete log file, which was just transfered to the database: " + file.getName());
	            	return ;
	            }
			} catch (IOException e) {
				Log.error("Unable to read contents of file " + file.toString());
				return ;
			} catch (SQLException e) {
				Log.error("Unable to access database. Error description: " + e.toString());
				e.printStackTrace();
				return ;
			}
		}

	}

}
