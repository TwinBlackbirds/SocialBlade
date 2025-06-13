// Scraper Template  Application
// Author: Michael Amyotte (twinblackbirds)
// Date: 6/12/25
// Purpose: Template for Java web scraper applications
// 

package tbb.apps.JScraper;

import java.time.Duration;
import java.util.Scanner;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Strings;

public class App 
{
	// TODO: change debugMode to program argument eventually
	private static Logger log = new Logger(true);
	private static ChromeDriver cd;
	private static String[] hosts = new String[0];
	
    public static void main( String[] args )
    {
    	// get the list of valid hosts from JSON
    	try {
    		Configurator c = new Configurator(log);	
    		hosts = c.getData().hosts;
    	} catch (Exception e) {
    		log.Write(LogLevel.ERROR, "Could not build configurator! Stack trace available in log.txt");
    		log.Dump(e);
    	}
    	log.Write(LogLevel.INFO, "Number of hosts: " + hosts.length);
    	
    	// set launch options
		log.Write(LogLevel.DBG, "Setting Chrome launch options");
    	ChromeOptions co = new ChromeOptions();
    	co.addArguments("headless");
    	
    	// point selenium to correct driver
    	log.Write(LogLevel.DBG, "Creating default ChromeDriverService");
    	ChromeDriverService cds = ChromeDriverService.createDefaultService();
    	
    	
    	// start driver
    	log.Write(LogLevel.INFO, "Starting Chrome browser");
    	cd = new ChromeDriver(cds, co);

    	// String s = loopUntilInput();
    	
    	search(); // search(s);    	
    	
    	log.Write(LogLevel.INFO, "Closing Chrome browser");
        // close browser + all tabs
        cd.quit();
    }
    
    static void search() {

    	Payload p = null;
    	for (String host : hosts) {

        	
    	}
    	
    }
    
    // queries the page every second until the DOM reports readyState = complete
    static void waitUntilPageLoaded() {
    	String pageName = cd.getTitle();
    	log.Write(LogLevel.INFO, String.format("Waiting for page '%s' to load", pageName));
    	new WebDriverWait(cd, Duration.ofSeconds(1)).until(
                webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    	log.Write(LogLevel.INFO, "Page loaded");
    }
    
    static String loopUntilInput() {
    	// loop and wait for a valid input from the user (to initiate searching)
    	Scanner s = new Scanner(System.in);
    	String searchTerm = "";
    	try {
    		// read input
    		while (true) {
        		System.out.print("Enter the name of the book you want: ");
        		String input = s.nextLine();
        		if (Strings.isNullOrEmpty(input)) {
        			continue;
        		}
        		System.out.flush();
        		System.out.print(String.format("You want the book ( %s )? [y/N]: ", input));
        		String confirm = s.nextLine();
        		if (confirm.strip().toLowerCase().equals("y")) {
        			break;
        		}
        	}

    	}
    	finally { 
    		// make sure scanner gets closed even if we get an interrupt
    		s.close();
    		log.Write(LogLevel.DBG, "Scanner closed");
    	}
    	return searchTerm;
    }
    
    class Payload {
    }
}

