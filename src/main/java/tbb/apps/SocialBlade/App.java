// SocialBlade Application
// Author: Michael Amyotte (twinblackbirds)
// Date: 6/12/25
// Purpose: Scrapes YouTube and saves data to a database
// 

package tbb.apps.SocialBlade;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import tbb.db.Driver.Sqlite;
import tbb.db.Schema.Channel;
import tbb.utils.Config.ConfigPayload;
import tbb.utils.Config.Configurator;
import tbb.utils.Logger.LogLevel;
import tbb.utils.Logger.Logger;

public class App 
{
	// TODO: change debugMode to program argument eventually
	private static Logger log = new Logger(LogLevel.ERROR);
	private static ChromeDriver cd;
	private static JavascriptExecutor js;
	private static String[] hosts = new String[0];
	
	private static Sqlite sql = new Sqlite(log);
	
	private static boolean headless = false;
	
    public static void main( String[] args )
    {
    	// get the list of valid hosts from JSON
    	try {
    		ConfigPayload data = new Configurator(log).getData();
    		hosts = data.hosts; // allowed hosts 
    		headless = data.headless;
    	} catch (Exception e) {
    		log.Write(LogLevel.ERROR, "Could not build configurator! Stack trace available in exception_log.txt");
    		log.Dump(e);
    		return;
    	}
    	log.Write(LogLevel.INFO, "Number of hosts: " + hosts.length);
    	
    	// set launch options
		log.Write(LogLevel.DBG, "Setting Chrome launch options");
    	ChromeOptions co = new ChromeOptions();
    	
    	if (headless) {
    		co.addArguments("headless");	
    	}
    	
    	// point selenium to correct driver
    	log.Write(LogLevel.DBG, "Creating default ChromeDriverService");
    	ChromeDriverService cds = ChromeDriverService.createDefaultService();
    	
    	
    	// start driver
    	log.Write(LogLevel.INFO, "Starting Chrome browser");
    	cd = new ChromeDriver(cds, co);
    	js = (JavascriptExecutor)cd;
    	startStatusMessageDaemon();
    	// String s = loopUntilInput();
    	cd.get(ensureSchema(hosts[0], true));
    	
    	try {
    		bot(); // main loop
    	} catch (Exception e) {
    		log.Write(LogLevel.ERROR, "Bot failed! " + e);
    		log.Dump(e);
    	} finally { // program cleanup
    		log.Write(LogLevel.INFO, "Closing Chrome browser");
            // close browser + all tabs
            cd.quit();
            // save log to file (destructor calls Dump)
            log.close();
            System.out.println("Process terminated with return code 0");
    	}   
     }
    
    // make sure we dont stray from the path
    static boolean checkHost() {
    	for (String host : hosts) {
			if (!cd.getCurrentUrl().contains(ensureSchema(host, false))) { // ensure valid host
    			log.Write(LogLevel.WARN, "We are on an invalid host! Host: " + cd.getCurrentUrl());
    			return false;
    		}	
		}
    	return true;
    }
    
    static void startStatusMessageDaemon() {
    	Thread statusThread = new Thread(() -> {
    	    char[] spinner = {'|', '/', '-', '\\'};
    	    int index = 0;

    	    try {
    	        while (true) {
    	            // Clear line manually with carriage return and enough spaces
    	            System.out.print("\r" + "Running... " + spinner[index] + "     ");
    	            System.out.flush();

    	            Thread.sleep(300);
    	            index = (index + 1) % spinner.length;
    	        }
    	    } catch (InterruptedException e) {
    	        System.out.println();
    	        System.out.println("Spinner stopped.");
    	    }
    	});

    	statusThread.setDaemon(true);
    	statusThread.start();
    }
    
    static void bot() throws Exception {

    	// this locks thread as it will loop forever
    	// this endlessly goes through youtube and collects data
    	// goes to the next video once data has been collected
    	// tries to find as many unique channels as possible
    	// grabs non-unique channels every so often to get a diff
    	
    	
    	//  -- logic to find a new video --
    	// search in sidebar until it finds a new creator
    	// if it cant find a new creator, go to the homepage and try the same
    	// rinse, repeat
    	
    	
    	// check if we are getting 'search to get started'
    	List<WebElement> els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	int attempts = 1;
    	while (els.size() > 0) {
    		log.Write(LogLevel.WARN, "Getting served the 'search to get started' page");
    		// we are on the page
    		cd.get("https://youtube.com/shorts/");
    		waitForElement("#reel-video-renderer .video-stream");
    		WebElement vid = cd.findElement(By.cssSelector("#reel-video-renderer .video-stream"));
    		jsClick(vid);
    		Thread.sleep(3000); // 'watch' the short a little bit
    		cd.get("https://youtube.com/");
    		waitUntilPageLoaded();
    		els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	}
    	log.Write(LogLevel.INFO, "Passed the 'search to get started' page!");
    	
    	// we have recommendations now, so we can use them to move our bot forward
    	
    	// find a video (channel) on the homepage
    	if (cd.getCurrentUrl() != "https://youtube.com") {
    		cd.get("https://youtube.com");
    		waitUntilPageLoaded();
    	}
		// -- look for a unique channel (compare to DB)
    	List<WebElement> channels = cd.findElements(By.cssSelector("[class*='ytd-channel-name'] a"));
    	ArrayList<String> IDs = new ArrayList<String>();
    	for (WebElement channel : channels) {
    		
    		String href = channel.getDomAttribute("href");
    		String ID = cleanChannelURL(href);
    		IDs.add(ID);
    	}
    	for (String ID : IDs) {
        	if (!sql.findChannel(ID)) {
    			// add the rest of the channel info and enter it into database
        		Channel c = getChannelInfo(ID);
    			sql.writeChannel(c);
    		}	
    	}
    	// quick-lookup to check if we have a channel
    	// ^ maybe cache the last 100 or so channels in memory so that frequent-flyers can skip DB calls
    	
    	// find a video (channel) on the sidebar
    	
    }
    
    static Channel getChannelInfo(String ID) {
    	String url = "https://youtube.com/@" + ID;
    	cd.get(url);
    	waitForElement("h1 > span");
    	WebElement nameEl = cd.findElement(By.cssSelector("h1 > span")); // channel name
    	String name = nameEl.getAttribute("innerText");
    	
    	Channel c = new Channel();
    	c.setID(ID);
    	c.setName(name);
    	
    	c.setLastChecked(c.getChecked());
    	c.setLastSubscriberCount(c.getSubscriberCount());
    	
    	c.setSubscriberCount(0);
    	c.setChecked(LocalDateTime.now());
    	
    	return c;
    }
    
    static String cleanChannelURL(String href) {
    	String[] split = href.split("/");
		return split[split.length-1].replaceAll("@", "").toLowerCase(); // case insensitive
    }
    
    static void jsClick(WebElement el) {
    	js.executeScript("arguments[0].click();", el);
    }
    
    static String ensureSchema(String url, boolean giveSchema) {
    	if (url.startsWith("https://")) {
    		if (giveSchema) {
    			return url;
    		}
    		return url.replace("https://", "");
    	} else {
    		if (giveSchema) {
    			return "https://" + url;
    		}
    		return url;
    	}
    }
    // queries the page every second until the DOM reports readyState = complete
    static void waitUntilPageLoaded() {
    	String pageName = cd.getTitle();
    	log.Write(LogLevel.INFO, String.format("Waiting for page '%s' to load", pageName));
    	new WebDriverWait(cd, Duration.ofSeconds(30)).until(
                webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState")
                    .equals("complete")
            );
    	log.Write(LogLevel.INFO, "Page loaded");
    	try {
    		Thread.sleep(2000);
    	} catch (Exception e) { }
    }
    
    static void waitForElement(String selector) {
    	new WebDriverWait(cd, Duration.ofSeconds(30)).until(
		    ExpectedConditions.elementToBeClickable(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(1000);
    	} catch (Exception e) { }
	}
    
    class Payload {
    }
}

