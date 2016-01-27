package database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
 
//Class used to get the database properties
public class dbProperties {
    private Properties prop = null;
     
    public dbProperties(){
        InputStream is = null;
        try {
            this.prop = new Properties();
            is = this.getClass().getResourceAsStream("EIP-Prop.properties");
            prop.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String getPropertyValue(String key){
        return this.prop.getProperty(key);
    } 
}
