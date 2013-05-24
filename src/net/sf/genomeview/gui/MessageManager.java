package net.sf.genomeview.gui;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import net.sf.genomeview.core.Configuration;
/**
 * 
 * @author David Rold�n Mart�nez
 * @author Thomas Abeel
 * 
 *
 */
public class MessageManager {

	private static ResourceBundle rb;    
		
	private static Logger log=Logger.getLogger(MessageManager.class.getCanonicalName());
	
	private static Locale loc = Configuration.get("lang:current").equals("automatic")?Locale.getDefault():new Locale(Configuration.get("lang:current"));
    static{
    	log.info("Getting messages for lang: "+loc);
        rb = ResourceBundle.getBundle("lang.Messages", loc);
        log.finest("Language keys: "+rb.keySet());
     
    }
    
    public static String getString(String key){
    	String value = "[missing key] " + key;
    	try{
    		value = rb.getString(key);
    	}catch(Exception e){
    		log.warning("I18N missing: "+loc+"\t"+key);
    	}
    	return value;
    }
    
	public static Locale getLocale() {
		return loc;
	}
}
