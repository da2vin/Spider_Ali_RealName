package com;

import com.alibaba.fastjson.JSONArray;
import org.apache.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;


class WebDriverPool {

    private Logger logger = Logger.getLogger(getClass());

    private final static int DEFAULT_CAPACITY = 5;

    private final int capacity;

    private final static int STAT_RUNNING = 1;

    private final static int STAT_CLODED = 2;

    private AtomicInteger stat = new AtomicInteger(STAT_RUNNING);

    private AliWebDriver mDriver = null;

    private static final String CONFIG_FILE = "C:\\Users\\DK\\Desktop\\CrawlerAli2\\config.ini";
    private static Properties sConfig;
    private static LinkedList<AliUser> userList = null;
    private static Object object = new Object();

    public boolean configureExist(AliWebDriver driver) {
        if (driver.needLogin) {
            if (driver.Login()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean configure() throws IOException {
        AliUser user = null;
        synchronized (object) {
            if (userList == null)//初始化userlist
            {
                userList = new LinkedList<AliUser>();
                sConfig = new Properties();
                sConfig.load(new FileReader(CONFIG_FILE));
                String json = sConfig.getProperty("account");
                List<AliUser> templist = JSONArray.parseArray(json, AliUser.class);
                if (templist.size() == 0) {
                    return false;
                }
                for (AliUser tempuser : templist) {
                    userList.add(tempuser);
                }
            }
            if (userList.size() == 0) {
                return false;
            }
            user = userList.poll();
        }

        mDriver = new AliWebDriver(user);

        if (mDriver.Login()) {
            return true;
        } else {
            return false;
        }
    }

    private List<AliWebDriver> webDriverList = Collections.synchronizedList(new ArrayList<AliWebDriver>());

    private BlockingDeque<AliWebDriver> innerQueue = new LinkedBlockingDeque<AliWebDriver>();

    public WebDriverPool(int capacity) {
        this.capacity = capacity;
    }

    public WebDriverPool() {
        this(DEFAULT_CAPACITY);
    }

    public AliWebDriver get() throws InterruptedException {
        checkRunning();
        AliWebDriver poll = innerQueue.poll();
        if (poll != null) {
            if (configureExist(poll)) {
                return poll;
            }
        }
        if (webDriverList.size() < capacity) {
            synchronized (webDriverList) {
                if (webDriverList.size() < capacity) {
                    // add new WebDriver instance into pool
                    try {
                        if (configure()) {
                            innerQueue.add(mDriver);
                            webDriverList.add(mDriver);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return innerQueue.take();
    }

    public void returnToPool(AliWebDriver webDriver) {
        checkRunning();
        innerQueue.add(webDriver);
    }

    protected void checkRunning() {
        if (!stat.compareAndSet(STAT_RUNNING, STAT_RUNNING)) {
            throw new IllegalStateException("Already closed!");
        }
    }

    public void closeAll() {
        boolean b = stat.compareAndSet(STAT_RUNNING, STAT_CLODED);
        if (!b) {
            throw new IllegalStateException("Already closed!");
        }
        for (AliWebDriver webDriver : webDriverList) {
            logger.info("Quit webDriver" + webDriver);
            webDriver.driver.quit();
            webDriver = null;
        }
    }
}