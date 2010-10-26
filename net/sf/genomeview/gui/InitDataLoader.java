package net.sf.genomeview.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.data.DataSourceFactory;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.gui.task.ReadWorker;
import net.sf.jannot.Location;
import net.sf.jannot.source.DataSource;
import net.sf.jannot.source.FileSource;

public class InitDataLoader {

	private Model model;
	private Logger logger;
	
	public InitDataLoader(Model model){
		this.model = model;
		logger = Logger.getLogger(InitDataLoader.class.getCanonicalName());
	}
	
	
	public void init(String config, String cmdUrl, String cmdFile, String[] remArgs, String position)
					throws InterruptedException, ExecutionException{
		
		
		/* Load the additional configuration */
		if (config != null) {
			try {
				if (config.startsWith("http") || config.startsWith("ftp")) {
					Configuration.loadExtra(new URI(config).toURL().openStream());
				} else {
					Configuration.loadExtra(new FileInputStream(config));
				}
			} catch (MalformedURLException e) {
				logger.log(Level.SEVERE,"loading extra configuration", e);
			} catch (IOException e) {
				logger.log(Level.SEVERE,"loading extra configuration", e);
			} catch (URISyntaxException e) {
				logger.log(Level.SEVERE,"loading extra configuration", e);
			}
		}
		
		
		/*
		 * Select data source. If an URL or file are specified on the command
		 * line, that is selected. In other cases a dialog pops-up to let the
		 * user select.
		 * 
		 * If both file and url are specified, the URL is loaded.
		 */
		DataSource[] data = null;
		if (cmdFile == null && cmdUrl == null) {
			logger.info("File and url options are null!");
			// do nothing
			
		} else if (cmdUrl != null) {
			logger.info("URL commandline option is set: " + cmdUrl);
			try {
				data = new DataSource[] { DataSourceFactory.createURL(new URL(cmdUrl)) };
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (cmdFile != null) {
			logger.info("File commandline option is set: " + cmdFile);
			try {
				data = new DataSource[] { DataSourceFactory.createFile(new File(cmdFile)) };

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/* Load the source, if one was constructed */
		if (data != null) {
			
			assert (data.length == 1);
			logger.info("Loading with priority: "+data[0]);
			final ReadWorker rw = new ReadWorker(data[0], model);
			rw.execute();
			rw.get();
		}
		
		/* Load additional files */
		for (String s : remArgs) {
			logger.info("loading additional from commandline: " + s);
			try {
				if (!s.startsWith("http:") && !s.startsWith("ftp:") && !s.startsWith("https:")) {
					DataSource ds = DataSourceFactory.createFile(new File(s));

					ReadWorker rf = new ReadWorker(ds, model);
					rf.execute();

				} else {
					DataSource ds = DataSourceFactory.createURL(new URL(s));
					ReadWorker rf = new ReadWorker(ds, model);
					rf.execute();

				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (position != null) {
			String[] arr = position.split(":");
			assert arr.length==2||arr.length==3;
			if(arr.length==3){
				model.setSelectedEntry(model.entry(arr[0]));
				model.setAnnotationLocationVisible(new Location(Integer.parseInt(arr[1]), Integer.parseInt(arr[2])));
			}else if(arr.length==2){
				model.setAnnotationLocationVisible(new Location(Integer.parseInt(arr[0]), Integer.parseInt(arr[1])));	
			}
			

		}
		
	}
	
}
