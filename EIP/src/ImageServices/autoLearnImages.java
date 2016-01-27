package ImageServices;

import java.io.File;
import database.dbProperties;

//Used to delete stored images in batch class
public class autoLearnImages {
	
	public static void deleteImage(String image, String BatchClassID) {		
		dbProperties eipProp = new dbProperties();
		String PathToSharedFolders =eipProp.getPropertyValue("EphesoftSharedFolders");
		try{
    		File file = new File(PathToSharedFolders + "\\"+BatchClassID+"\\AutoLearnFiles\\"+image);
    		if(file.delete()){
    			//System.out.println(file.getName() + " is deleted!" );
    		}else{
    			System.out.println("Could not delete "+ image);			
    		}
    	}catch(Exception e){
    		e.printStackTrace();	
    	}	
	}
}
