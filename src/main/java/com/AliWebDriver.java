package com;

import com.Dama2Web.DecodeResult;
import com.Dama2Web.RequestResult;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Administrator on 2016/7/25.
 */
public class AliWebDriver {

    private Dama2Web dama2 = new Dama2Web(43311, "d191c0fd4d6f1957067350f171409441", "iamDW", "maosu1989");

    private AliUser user;
    private Logger logger = Logger.getLogger(getClass());

    public boolean needLogin = true;

    public WebDriver driver = null;

    public AliWebDriver(AliUser tempuser,Proxy proxy){
        DesiredCapabilities caps = DesiredCapabilities.phantomjs();
        caps.setJavascriptEnabled(true);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=" + proxy.proxy, "--proxy-type=http","--proxy-auth=darkwings_love:5iilwmvi"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=" + proxy.proxy, "--proxy-type=http"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=localhost:8888", "--proxy-type=http"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=114.215.174.98:16816", "--proxy-type=http","--proxy-auth=darkwings_love:5iilwmvi"});
        caps.setCapability("takesScreenshot", true);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "C:/Python27/phantomjs.exe");
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "/home/maojingwen/alitest/phantomjs");
        caps.setCapability("phantomjs.page.settings.userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12F70 Safari/600.1.4");
        driver = new PhantomJSDriver(caps);
        user = tempuser;
    }

    public AliWebDriver(AliUser tempuser){
        DesiredCapabilities caps = DesiredCapabilities.phantomjs();
        caps.setJavascriptEnabled(true);
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=" + proxy.proxy, "--proxy-type=http"});
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true", "--proxy=localhost:8888", "--proxy-type=http"});
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, new String[]{"--load-images=true"});
        caps.setCapability("takesScreenshot", true);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "C:/Python27/phantomjs.exe");
        //caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "/home/maojingwen/alitest/phantomjs");
        caps.setCapability("phantomjs.page.settings.userAgent", "Mozilla/5.0 (iPhone; CPU iPhone OS 8_3 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12F70 Safari/600.1.4");
        driver = new PhantomJSDriver(caps);
        user = tempuser;
    }

    public boolean Login()
    {
        boolean loginFlag = false;
        try{
            do {
                logger.info(String.format("正在登录，账号：%s，密码：%s",user.username,user.password));

                logger.info("正在打开登录页面，登录账号：" + user.username);
                driver.get("https://authsu18.alipay.com/login/index.htm");
//                Thread.sleep(3000);
//                snapshot((TakesScreenshot)driver,"input_keyWord.png");
//                logger.info(driver.getPageSource());

                logger.info("正在选取账号节点，登录账号：" + user.username);
                WebElement element = driver.findElement(By.id("J-input-user"));
                logger.info("正在填写账号，登录账号：" + user.username);
                element.clear();
                element.sendKeys(user.username);

                logger.info("正在选取密码节点，登录账号：" + user.username);
                element = driver.findElement(By.id("password_input"));

                logger.info("正在填写密码，登录账号：" + user.username);
                element.clear();
                element.sendKeys(user.password);

                logger.info("正在选取验证码图片节点，登录账号：" + user.username);
                element = driver.findElement(By.id("J-checkcode-img"));
                //element.click();

                logger.info("正在获取验证码图片，登录账号：" + user.username);
                BufferedImage authcodeimage = createElementImage(driver,element);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(authcodeimage,"png",out);
                byte[] authcodebyte = out.toByteArray();

                String AuthCode = "";
                boolean damaFlag = false;
                RequestResult result = null;
                do {
                    logger.info("正在打码，登录账号：" + user.username);
                    result = dama2.decode(42,30000,authcodebyte);
                    if(result.ret<0)
                    {
                        logger.info("打码失败：" + result.desc + "，登录账号：" + user.username);
                        continue;
                    }
                    else
                    {
                        logger.info("打码成功，登录账号：" + user.username);
                    }

                    logger.info("获取打码结果，登录账号：" + user.username);
                    DecodeResult decodeResult = dama2.getResultUntilDone(result.ret, 30000);
                    if(decodeResult.ret<0)
                    {
                        logger.info("获取打码结果失败：" + decodeResult.desc + "，登录账号：" + user.username);
                        continue;
                    }
                    else
                    {
                        logger.info("获取打码结果成功" + decodeResult.desc + "，登录账号：" + user.username);
                        damaFlag = true;
                        AuthCode = decodeResult.result;
                    }
                    logger.info("打码结果：" + decodeResult.result + "，登录账号：" + user.username);
                    break;
                }while (!damaFlag);

                logger.info("正在获取填写验证码节点，登录账号：" + user.username);
                element = driver.findElement(By.id("J-input-checkcode"));

                logger.info("正在填写验证码，登录账号：" + user.username);
                element.clear();
                element.sendKeys(AuthCode);

                logger.info("正在获取登录按钮节点，登录账号：" + user.username);
                element = driver.findElement(By.id("J-login-btn"));
                Thread.sleep(500);

                logger.info("正在点击登录按钮，登录账号：" + user.username);
                element.click();
                Thread.sleep(3000);

                if(driver.getTitle().contains("实名认证") || driver.getTitle().contains("我的支付宝"))
                {
                    logger.info("登录成功，登录账号：" + user.username);
                    loginFlag = true;
                }
                else
                {
                    if(driver.getTitle().contains("登录"))
                    {
                        try {
                            driver.findElement(By.cssSelector("i.iconfont"));
                            logger.info("验证码错误，登录账号：" + user.username);
                            logger.info("正在提交打码错误报告，登录账号：" + user.username);
                            RequestResult res = dama2.reportError(result.ret);
                            String s;
                            if (res.ret == 0) {
                                logger.info("提交打码错误报告成功：" + res.ret + "，登录账号：" + user.username);
                            } else {
                                logger.info("提交打码错误报告失败:" + res.ret + "原因："+res.desc + "，登录账号：" + user.username);
                            }
                            continue;
                        }catch (Exception e){
                        }
                    }
                }
                break;
            }while (true);

            needLogin = !loginFlag;
            return loginFlag;
        }catch (NoSuchElementException e){
            logger.error("没有找到相应节点:" + e.getMessage() + "，登录账号：" + user.username);
            return false;
        }
        catch (Exception e)
        {
            logger.error("登录失败:" + e.getMessage() + "，登录账号：" + user.username);
            return  false;
        }
    }

    public String getUserName(String account)
    {
        try{
            logger.info("当前登录账号：" + user.username+ "，登录账号：" + user.username + "，查询账号：" + account);
            logger.info("正在打开转账界面"+ "，登录账号：" + user.username + "，查询账号：" + account);
            driver.get("https://shenghuo.alipay.com/send/payment/fill.htm");
            Thread.sleep(500);
            if(driver.getPageSource().contains("<title>509 unused</title>"))//过快被禁用
            {
                return "too fast";
            }

            if(!driver.getTitle().contains("转账付款"))
            {
                logger.info("打开转账界面失败"+ "，登录账号：" + user.username + "，查询账号：" + account);
                return "error";
            }

            logger.info("正在查找收款人填写框"+ "，登录账号：" + user.username + "，查询账号：" + account);
            WebElement element = driver.findElement(By.id("ipt-search-key"));

            logger.info("正在填写收款人账号"+ "，登录账号：" + user.username + "，查询账号：" + account);
            element.clear();
            element.sendKeys(account);
            Thread.sleep(500);

            logger.info("正在点击其它位置"+ "，登录账号：" + user.username + "，查询账号：" + account);
            element = driver.findElement(By.id("amount"));
            element.click();
            Thread.sleep(800);

            element = driver.findElement(By.id("accountStatusMsg"));
            String username = element.getText();

            if(username.equals(""))
            {
                element = driver.findElement(By.id("ipt-search-key"));
                username = element.getAttribute("value");
            }
            else
            {
                if(username.equals("你的操作过于频繁，请稍后再试"))
                {
                    return "needlogin";
                }
            }
            return username;
        }catch (Exception e)
        {
            logger.error("获取收款人实名失败:" + e.getMessage()+ "，登录账号：" + user.username + "，查询账号：" + account);
            return "error";
        }
    }

    public static byte[] takeScreenshot(WebDriver driver) throws IOException {
        WebDriver augmentedDriver = new Augmenter().augment(driver);
        return ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.BYTES);
    }

    public static BufferedImage createElementImage(WebDriver driver, WebElement webElement) throws IOException {
        // 获得webElement的位置和大小。
        Point location = webElement.getLocation();
        Dimension size = webElement.getSize();
        // 创建全屏截图。
        BufferedImage originalImage =
                ImageIO.read(new ByteArrayInputStream(takeScreenshot(driver)));
        // 截取webElement所在位置的子图。
        BufferedImage croppedImage = originalImage.getSubimage(
                location.getX(),
                location.getY(),
                size.getWidth(),
                size.getHeight());
        return croppedImage;
    }

    public static void snapshot(TakesScreenshot drivername, String filename)
    {
        // this method will take screen shot ,require two parameters ,one is driver name, another is file name
        File scrFile = drivername.getScreenshotAs(OutputType.FILE);
        // Now you can do whatever you need to do with it, for example copy somewhere
        try {
            System.out.println("save snapshot path is:E:/"+filename);
            FileUtils.copyFile(scrFile, new File("E:\\"+filename));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("Can't save screenshot");
            e.printStackTrace();
        }
        finally
        {
            System.out.println("screen shot finished");
        }
    }
}
