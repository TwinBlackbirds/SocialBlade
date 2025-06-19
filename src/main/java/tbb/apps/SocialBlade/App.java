// SocialBlade Application
// Author: Michael Amyotte (twinblackbirds)
// Date: 6/12/25
// Purpose: Scrapes YouTube and saves data to a database
// 

package tbb.apps.SocialBlade;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Strings;

import jakarta.persistence.EntityNotFoundException;
import tbb.db.Driver.Sqlite;
import tbb.db.Driver.ChannelCache;
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
	
	// db 
	private static ArrayList<String> blacklistedIDs = new ArrayList<String>();
	private static ChannelCache cachedIDs = new ChannelCache();
	
	private static final int MAX_LOAD_ATTEMPTS = 3;
	
	// video id regex
	private static final Pattern pattern = Pattern.compile("(?<=\\?v\\=)[\\w-]+(?=[&/]?)", Pattern.CASE_INSENSITIVE);
	
    public static void main( String[] args )
    {
    	// get the list of valid hosts from JSON
    	try {
    		ConfigPayload data = new Configurator<>(log, ConfigPayload.class)
    							 .getData();
    		hosts = data.hosts; // allowed hosts 
    		headless = data.headless;
    	} catch (Exception e) {
    		log.Write(LogLevel.ERROR, "Could not build configurator! Stack trace available in exception_log.txt");
    		log.Dump(e);
    		return;
    	}
    	
    	// turn off hibernate logs
    	java.util.logging.LogManager.getLogManager().reset();
    	
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
    	}   
    	try {
			Thread.sleep(5000);
		} catch (InterruptedException e) { }
    	System.out.println("Process terminated with return code 0");
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
    	// get starting amount of rows in database 
    	// every n iterations on the thread, refresh count
    	// get difference
    	Thread statusThread = new Thread(() -> {
    		int cnt = 0;
    		int base = sql.countChannels();
    	    char[] spinner = {'|', '/', '-', '\\'};
    	    int index = 0;
    	    int count = 0;
    	    try {
    	        while (true) {
    	        	if (count % 15 == 0) {
    	        		cnt = sql.countChannels();
    	        	}
    	            // Clear line manually with carriage return and enough spaces
    	        	String msg = "\rRunning... " + spinner[index];
    	        	int cntOfNew = cnt-base;
    	        	if (cntOfNew > 0) {
    	        		msg += " (new: " + (cntOfNew) + ")";
    	        	}
    	        	msg += "     ";
    	            System.out.print(msg);
    	            System.out.flush();

    	            Thread.sleep(300); // Approx. 7.5 seconds between DB length calls
    	            index = (index + 1) % spinner.length;
    	            count++;
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
    	
    	// check if we are getting 'search to get started'
    	List<WebElement> els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	int attempts = 0;
    	while (els.size() > 0 && attempts < MAX_LOAD_ATTEMPTS) {
    		log.Write(LogLevel.WARN, "Getting served the 'search to get started' page");
    		// we are on the page
    		cd.get("https://youtube.com/shorts/");
    		waitForElementClickable("#reel-video-renderer .video-stream");
    		WebElement vid = cd.findElement(By.cssSelector("#reel-video-renderer .video-stream"));
    		jsClick(vid);
    		Thread.sleep(3000); // 'watch' the short a little bit
    		cd.get("https://youtube.com/");
    		waitUntilPageLoaded();
    		els = cd.findElements(By.cssSelector("ytd-feed-nudge-renderer[contents-location*=\"UNKNOWN\"]"));
    	}
    	if (els.size() > 0) {
    		throw new Exception("Could not get passed the 'search to get started' page!");
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
    	
    	// OR -- find channel(s) on the sidebar
    	// (homepage is probably more efficient)
    	
    	// TODO: 'spider' crawling mode - it finds links on the page and leverages those to keep going
    	// find a url which will give us the most channels somehow? e.g. AI analysis of outlier?
    	
    	
		int successes = parseIDs(IDs);	
		int fails = IDs.size() - successes;
    	if (fails > 0) {
    		double perc = ((double) successes / IDs.size()) * 100;
    		String msg = String.format(
    				"Warning: Not all IDs provided to parseID resulted in a valid outcome! %d/%d (%.2f%%) operations succeeded", 
    				successes, IDs.size(), perc);
    		log.Write(LogLevel.WARN, msg);
    	}
    	
    }
    
    private static int parseIDs(List<String> IDs) {

    	// once we have the list of ids
    	// ^ maybe cache the last 100 or so channels in memory so that frequent-flyers can skip DB calls
    	int success = 0;
    	for (String ID : IDs) {
    		// check blacklist
    		if (blacklistedIDs.contains(ID)) {
    			log.Write(LogLevel.INFO, "Skipping blacklisted ID " + ID);
    			continue;
    		}
    		// check cache (we just got this channel within this SocialBlade session)
    		if (cachedIDs.contains(ID)) {
    			log.Write(LogLevel.INFO, "Skipping cached ID " + ID);
    			continue;
    		}
    		
    		// check duplicates
        	if (!sql.findChannel(ID)) {
    			// add the rest of the channel info and enter it into database
        		Channel c;
        		try {
        			c = getChannelInfo(ID);	
        		} catch (Exception e) {
        			// blacklist invalid id
        			log.Write(LogLevel.INFO, "Adding invalid ID to blacklist: " + ID);
        			blacklistedIDs.add(ID);
        			continue;
        		}
        		log.Write(LogLevel.INFO, "Inserting channel info for new ID " + ID);
        		try {
            		sql.writeChannel(c);
        		} catch (Exception e) {
        			log.Write(LogLevel.ERROR, "Insert operation failed for ID " + ID); 
        			continue;
        		}
    		} else {
    			// get existing channel to perform check-against logic
    			Channel c = null;
    			try {
    				c = sql.getChannel(ID);
    			} catch (Exception e) {
    				log.Write(LogLevel.WARN, "Could not find channel with ID " + ID);
    				continue;
    			}
    			
    			// last seen over a week ago
    			// OR
    			// they've uploaded a video since last pull
    			String videoID = "";
    			try {
    				videoID = getLastVideo(ID).trim();
    			} catch (Exception e) {
    				log.Write(LogLevel.WARN, "Could not get the last video for channel with ID " + ID);
    				continue;
    			}
    			
    			LocalDateTime aWeekAgo = LocalDateTime.now().minusDays(7);
    			if (c.getChecked().isBefore(aWeekAgo)
    				|| !videoID.equalsIgnoreCase(c.getLastVideo())
    				|| videoID.length() != 11) /*standard yt video id length*/ {
    				log.Write(LogLevel.WARN, "Overwriting channel info for existing ID " + ID);
    				c.setLastVideo(null); // clear so it can be overwritten
    				try {
        				Channel ch = getChannelInfo(ID, c);
        				sql.updateChannel(ch);
    				} catch (Exception e) {
    					log.Write(LogLevel.ERROR, "Could not get info and update channel with ID " + ID);
    					continue;
    				}
    			}
    			else {
    				log.Write(LogLevel.INFO, "Skipping existing ID " + ID);
    				continue;
    			}
    		}
        	success++;
        	cachedIDs.add(ID);
    	}
    	return success;
    }
    
    
    static String getLastVideo(String ID) throws Exception {
    	int cnt = 0;
    	// while the page is null
    	
    	List<WebElement> videoEl = List.of();
    	while (videoEl.size() == 0 && cnt < MAX_LOAD_ATTEMPTS) { // retry
    		log.Write(LogLevel.INFO, "Attempting to load videos page of channel " + ID + "... (" + cnt+1 + ")");
    		String url = "https://youtube.com/@" + ID + "/videos";
    		cd.get(url);
        	waitUntilPageLoaded();
        	videoEl = cd.findElements(By.cssSelector("a#video-title-link")); 
    		cnt++;
    	}
    	if (videoEl.size() == 0) {
    		throw new Exception("Could not get the channel's videos!");
    	}
    	String firstHref = videoEl.get(0).getAttribute("href");
    	
    	// make regex
    	Matcher m = pattern.matcher(firstHref);
    	m.find(); // run regex
    	String videoID = m.group(); // pull regex result
    	
    	if (videoID == null || videoID == "") {
    		throw new EntityNotFoundException("Could not find VideoID from href with regex!");
    	}
    	log.Write(LogLevel.INFO, "Latest video ID: " + videoID);
    	
    	return videoID.trim();
    }
    
    static Channel getChannelInfo(String ID) throws Exception {
    	return getChannelInfo(ID, null);
    }
    
    static Channel getChannelInfo(String ID, Channel existingChannel) throws Exception {
    	int cnt = 0;
    	// while the page is null
    	
    	List<WebElement> nameEl = List.of();
    	while (nameEl.size() == 0 && cnt < MAX_LOAD_ATTEMPTS) { // retry
    		log.Write(LogLevel.INFO, "Attempting to load channel page " + ID + "... (" + cnt+1 + ")");
    		String url = "https://youtube.com/@" + ID;
    		cd.get(url);
        	waitUntilPageLoaded();
        	nameEl = cd.findElements(By.cssSelector("span[class*='white-space-pre-wrap']")); // channel name
    		cnt++;
    	}
    	if (nameEl.size() == 0) {
    		throw new Exception("Could not get the channel name!");
    	}
    	String name = nameEl.get(0).getAttribute("innerText");
    	
    	// get actual subscriber count
    	WebElement subEl = cd.findElement(
    			By.cssSelector(".yt-content-metadata-view-model-wiz__metadata-row--metadata-row-inline > span:nth-child(1):not(:has(*))"));
    	String subElText = subEl.getAttribute("innerText");
    	
    	// parse formatted (e.g  20.3M) 
    	NumberFormat nf = NumberFormat.getInstance();
    	Number subElNum = nf.parse(subElText);
    	if (subElText.contains("M")) {
    		subElNum = subElNum.doubleValue() * 1000000;
    	} else if (subElText.contains("K")) {
    		subElNum = subElNum.doubleValue() * 1000;
    	}
    	
    	Channel c = new Channel();
    	c.setID(ID);
    	c.setName(name);
    	
    	c.setSubscriberCount(subElNum.intValue());
    	c.setChecked(LocalDateTime.now());
    	
    	// updating or making new record?
    	if (existingChannel == null) {
    		String videoID = getLastVideo(ID); 
    		c.setLastVideo(videoID);
    		c.setTimesEncountered(1);
    	} else {
    		// update previous time checked based off last record
        	c.setLastChecked(existingChannel.getChecked());
        	c.setLastSubscriberCount(existingChannel.getSubscriberCount());
        	
        	if (Strings.isNullOrEmpty(existingChannel.getLastVideo())) { // if ID got reset due to mismatch
        		c.setLastVideo(getLastVideo(existingChannel.getID()));
        	} else {
        		c.setLastVideo(existingChannel.getLastVideo());	
        	}
        	c.setTimesEncountered(existingChannel.getTimesEncountered() + 1);
    	}
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
    
    static void waitForElementClickable(String selector) {
    	new WebDriverWait(cd, Duration.ofSeconds(30)).until(
		    ExpectedConditions.elementToBeClickable(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(1000);
    	} catch (Exception e) { }
	}
    
    static void waitForElementVisible(String selector) {
    	new WebDriverWait(cd, Duration.ofSeconds(30)).until(
		    ExpectedConditions.visibilityOfElementLocated(By.cssSelector(selector))
		);
    	try {
    		Thread.sleep(1000);
    	} catch (Exception e) { }
	}
    
}

