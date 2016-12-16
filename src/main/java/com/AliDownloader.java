package com;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;


/**
 * Created by Administrator on 2016/7/11.
 */
public class AliDownloader {

    private static volatile WebDriverPool webDriverPool;

    private Logger logger = Logger.getLogger(getClass());

    private void checkInit() {
        if (webDriverPool == null) {
            synchronized (this) {
                webDriverPool = new WebDriverPool();
            }
        }
    }

    public String getAliName(String account) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            checkInit();
            AliWebDriver webDriver;

            webDriver = webDriverPool.get();

            String result = webDriver.getUserName(account);

            if("too fast".equals(result))//过快处理
            {
                logger.error("访问过快，需要更换代理！！！");
                return "too fast";
            }

            int i = 0;
            while (result.equals("needlogin") || result.equals("error"))
            {
                i ++;
                if(i>3)
                {
                    logger.error("登录账号出现问题需要更换");
                    return "error";
                }
                logger.error("cookie过期，重新登录，重新获取");
                webDriver.needLogin = true;
                webDriverPool.returnToPool(webDriver);

                webDriver = webDriverPool.get();
                result = webDriver.getUserName(account);
            }

            webDriverPool.returnToPool(webDriver);
            logger.info("获取成功：" + result);
            return result;
        }catch (Exception e)
        {
            logger.error(e.getMessage());
            return "error";
        }
    }

    public void close() throws IOException {
        webDriverPool.closeAll();
    }
}
