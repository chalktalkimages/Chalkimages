package server;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;

public class exitProgram extends TimerTask{

	private final static Logger logger = Logger.getLogger(exitProgram.class
			.getName());

    public void run() {
    	
    	if (Application.springApp != null)
    	{
    		logger.info("ChalkTalk Spring app exited");
    		System.exit(SpringApplication.exit(Application.springApp));
    	}
    }

}


