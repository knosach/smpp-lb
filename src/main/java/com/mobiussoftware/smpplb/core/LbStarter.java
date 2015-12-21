package com.mobiussoftware.smpplb.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppServerConfiguration;
import com.mobiussoftware.smpplb.impl.SmppServer;

public class LbStarter {

	private static final Logger logger = LoggerFactory.getLogger(LbStarter.class);

	public static void main(String[] args) {
		
		if (args.length < 1) {
			logger.error("Please specify balancer-config argument. Usage is : config.properties");
			return;
		}
		
		if(!args[0].startsWith("config.properties")) {
			logger.error("Impossible to find the configuration file since you didn't specify the mobicents balancer config argument. Usage is : config.properties");
			return;
		}
		
		String configurationFileLocation = args[0];
		LbStarter lbStarter = new LbStarter();
		lbStarter.start(configurationFileLocation);

	}
	
	Timer timer;
	long lastupdate = 0;
	
	public void start(final String configurationFileLocation){
		
		File file = new File(configurationFileLocation);
		lastupdate = file.lastModified();
        FileInputStream fileInputStream = null;
        try {
        	fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("the configuration file location " + configurationFileLocation + " does not exists !");
		}
        
        final Properties properties = new Properties(System.getProperties());
        try {
			properties.load(fileInputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to load the properties configuration file located at " + configurationFileLocation);
		} finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				logger.warn("Problem closing file " + e);
			}
		}
        //must reload property file in period 
        
        timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				File conf = new File(configurationFileLocation);
				if(lastupdate < conf.lastModified()) {
					lastupdate = conf.lastModified();
					logger.info("Configuration file changed, applying changes.");
					FileInputStream fileInputStream = null;
					try {
						
							fileInputStream = new FileInputStream(conf);
							properties.load(fileInputStream);
							logger.info("Changes applied.");
						
					} catch (Exception e) {
						logger.warn("Problem reloading configuration " + e);
					} finally {
						if(fileInputStream != null) {
							try {
								fileInputStream.close();
							} catch (Exception e) {
								logger.error("Problem closing stream " + e);
							}
						}
					}
				}
			}
		}, 3000, 2000);
        
        
        
        
        start(properties);
	}
	
	public void start(Properties properties)
	{
		
        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newCachedThreadPool();
        SmppServerConfiguration configuration = new SmppServerConfiguration();
        configuration.setName(properties.getProperty("name"));
        configuration.setHost(properties.getProperty("host"));
        configuration.setPort(Integer.parseInt(properties.getProperty("port")));
        configuration.setMaxConnectionSize(Integer.parseInt(properties.getProperty("maxConnectionSize")));
        configuration.setNonBlockingSocketsEnabled(Boolean.parseBoolean(properties.getProperty("nonBlockingSocketsEnabled")));
        configuration.setDefaultSessionCountersEnabled(Boolean.parseBoolean(properties.getProperty("defaultSessionCountersEnabled")));
		SmppServer smppLbServer = new SmppServer(configuration, executor, properties);
        smppLbServer.start();

	}

}
