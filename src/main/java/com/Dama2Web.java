package com;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Dama2Web {
    //错误码宝岛
    public final static int errCalcEncInfo = -1000001;        //计算加密信息失败
    public final static int errBalanceNotNumber = -1000002;        //返回的余额不是数字
    public final static int errIdNotNumber = -1000003;        //返回的ID不是数字
    public final static int errEncodingError = -1000004;        //编码错误
    public final static int errRespError = -1000005;        //HTTP响应错误，描述中包括HTTP的响应码
    public final static int errJsonParseError = -1000006;        //JSON分析错误
    public final static int errJsonMapError = -1000007;        //JSON映射错误
    public final static int errIOException = -1000008;        //IO异常（包括网络异常）
    public final static int errGetResultTimeout = -1000009;        //得到结果超时

    private final static String _urlPrex = "http://api.dama2.com:7777/app/";

    //一般请求的结果
    public class RequestResult {
        public int ret;            //返回码
        public String desc;        //返回码描述

        RequestResult(int ret, String desc) {
            this.ret = ret;
            this.desc = desc;
        }
    }

    //查询余额结果
    public class ReadBalanceResult extends RequestResult {
        public int balance;                //余额

        ReadBalanceResult(int ret, String desc, int balance) {
            super(ret, desc);
            this.balance = balance;
        }
    }

    //读用户信息结果
    public class ReadInfoResult extends RequestResult {
        public String name;
        public String qq;        //用户QQ号，不存在时为null
        public String email;    //用户邮箱，不存在时为null
        public String tel;        //用户电话号码，不存在时为null

        ReadInfoResult(int ret, String desc, String name, String qq, String email, String tel) {
            super(ret, desc);
            this.name = name;
            this.qq = qq;
            this.email = email;
            this.tel = tel;
        }
    }

    //打码结果(getResult返回)
    public class DecodeResult extends RequestResult {
        public String result;        //结果字串
        public String cookie;

        DecodeResult(int ret, String desc, String result, String cookie) {
            super(ret, desc);
            this.result = result;
            this.cookie = cookie;
        }
    }

    //构造函数
    //appID：应用ID（软件ID）
    //softKey:软件KEY
    //uname：用户名
    //upwd：用户密码
    public Dama2Web(int appID, String softKey, String uname, String upwd) {
        _appID = appID;
        _softKey = softKey;
        _uname = uname;
        _upwd = upwd;
    }

    //注册新用户，用户名和密码通过构造函数传入
    public RequestResult register(String qq, String email, String tel) {
        RequestResult result = preAuth();
        if (result.ret < 0)
            return result;

        String url = _urlPrex + "register";
        String encInfo = calcEncInfo(result.desc, _softKey, _uname, _upwd);
        if (encInfo == null) {
            return new RequestResult(errCalcEncInfo, "");
        }

        url = url + "?appID=" + _appID + "&qq=" + qq + "&email=" + email + "&tel=" + tel + "&encinfo=" + encInfo;

        return doRequest(url, new IResultHandler() {
            @Override
            public RequestResult handleSucc(Map<String, String> m) {
                return genRequestResult(0);
            }
        }, true, false);
    }

    //读取用户余额
    public ReadBalanceResult getBalance() {
        RequestResult result = handleRequest(new IActualHandler() {
            @Override
            public RequestResult process() {
                String url = _urlPrex + "getBalance";
                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        int[] balance = new int[1];
                        try {
                            balance[0] = Integer.parseInt(m.get("balance"));
                        } catch (Exception e) {
                            return genRequestResult(errBalanceNotNumber);
                        }
                        return new ReadBalanceResult(0, "", balance[0]);
                    }
                });
            }
        });

        if (result instanceof ReadBalanceResult) {
            return (ReadBalanceResult) result;
        } else {
            return new ReadBalanceResult(result.ret, result.desc, 0);
        }
    }

    //读取用户信息
    public ReadInfoResult readInfo() {
        RequestResult result = handleRequest(new IActualHandler() {
            @Override
            public RequestResult process() {
                String url = _urlPrex + "readInfo";

                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        return new ReadInfoResult(0, "", m.get("name"), m.get("qq"), m.get("email"), m.get("tel"));

                    }
                });
            }

        });

        if (result instanceof ReadInfoResult) {
            return (ReadInfoResult) result;
        } else {
            return new ReadInfoResult(result.ret, result.desc, null, null, null, null);
        }
    }

    //通过URL地址请求打码,不传递COOKIE和REFERER信息
    //urlPic：图片的URL地址，内部会进行URL编码
    //type：验证码类型ID
    //timeout：超时秒数
    public RequestResult decodeUrl(final String urlPic, final int type, final int timeout) {
        return decodeUrl(urlPic, null, null, type, timeout);
    }

    //通过URL地址请求打码
    //urlPic：图片的URL地址，内部会进行URL编码
    //type：验证码类型ID
    //timeout：超时秒数
    //RequestResult.ret < 0 failed, ret is error code;  > 0, ret is id
    public RequestResult decodeUrl(final String urlPic, final String cookie, final String referer, final int type, final int timeout) {
        return handleRequest(new IActualHandler() {

            @Override
            public RequestResult process() {
                String url = _urlPrex + "decodeURL";

                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        int id = 0;
                        try {
                            id = Integer.parseInt(m.get("id"));
                        } catch (Exception e) {
                            return genRequestResult(errIdNotNumber);
                        }
                        return new RequestResult(id, "");
                    }
                }, new IPostDataGettor() {

                    @Override
                    public byte[] getPostData() {
                        return null;
                    }

                    @Override
                    public Map<String, String> getProperties() {
                        HashMap<String, String> m = new HashMap<String, String>();
                        m.put("type", String.valueOf(type));
                        m.put("timeout", String.valueOf(timeout));
                        try {
                            m.put("url", URLEncoder.encode(urlPic, "utf-8"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String temp;
                        try {
                            if (cookie != null && (temp = URLEncoder.encode(cookie, "utf-8")) != null) {
                                m.put("cookie", temp);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            if (referer != null && (temp = (URLEncoder.encode(referer, "utf-8"))) != null) {
                                m.put("referer", temp);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        return m;
                    }

                }, true, true);
            }
        });
    }

    //通过URL地址请求打码，并且读取结果
    //urlPic：图片的URL地址，内部会进行URL编码
    //type：验证码类型ID
    //timeout：超时秒数
    //RequestResult.ret < 0 failed, ret is error code;  > 0, ret is id
    public DecodeResult decodeUrlAndGetResult(String urlPic, String cookie, String referer, int type, int timeout) {
        RequestResult result = this.decodeUrl(urlPic, cookie, referer, type, timeout);
        if (result.ret < 0) {
            return new DecodeResult(result.ret, result.desc, null, null);
        }

        return this.getResultUntilDone(result.ret, timeout * 1000);
    }

    //通过URL地址请求打码，并且读取结果
    //urlPic：图片的URL地址，内部会进行URL编码
    //type：验证码类型ID
    //timeout：超时秒数
    //RequestResult.ret < 0 failed, ret is error code;  > 0, ret is id
    public DecodeResult decodeUrlAndGetResult(String urlPic, int type, int timeout) {
        return decodeUrlAndGetResult(urlPic, null, null, type, timeout);
    }

    //通过图片数据请求打码
    //type：验证码类型ID
    //timeout：超时秒数
    //data：图片数据
    //成功时返回对象的ret > 0，表示验证码ID，用于调用getResult、getResultUtilDone等函数
    private RequestResult decode(final int type, final int timeout, final byte[] data, final String txt) {
        return handleRequest(new IActualHandler() {
            @Override
            public RequestResult process() {
                String url = _urlPrex + "decode";

                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        int id;
                        try {
                            id = Integer.parseInt(m.get("id"));
                        } catch (Exception e) {
                            return genRequestResult(errIdNotNumber);
                        }
                        return new RequestResult(id, "");
                    }
                }, new IPostDataGettor() {
                    @Override
                    public byte[] getPostData() {
                        return data;
                    }

                    @Override
                    public Map<String, String> getProperties() {
                        Map<String, String> p = new HashMap<String, String>();
                        p.put("type", Integer.toString(type));
                        p.put("timeout", Integer.toString(timeout));
                        if (txt != null && txt.length() > 0) {
                            try {
                                String temp;
                                temp = URLEncoder.encode(txt, "utf-8");
                                p.put("text", temp);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        return p;
                    }
                }, true, true);
            }
        });
    }

    //通过图片数据请求打码
    //type：验证码类型ID
    //timeout：超时秒数
    //data：图片数据
    //成功时返回对象的ret > 0，表示验证码ID，用于调用getResult、getResultUtilDone等函数
    public RequestResult decode(final int type, final int timeout, final byte[] data) {
        return decode(type, timeout, data, null);
    }

    //通过图片数据请求打码
    //type：验证码类型ID
    //timeout：超时秒数
    //txt：文本数据
    //成功时返回对象的ret > 0，表示验证码ID，用于调用getResult、getResultUtilDone等函数
    public RequestResult decode(final int type, final int timeout, final String txt) {
        byte[] data = null;
        try {
            data = txt.getBytes("gbk");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decode(type, timeout, data, txt);
    }

    //查询指定ID的结果，不管成功失败，立即返回
    //id：验证码ID，由decode或decodeUrl返回
    //返回值：DecodeResult.ret >=0时, 得到结果成功，返回ID； <0失败
    public DecodeResult getResult(final int id) {
        RequestResult res = handleRequest(new IActualHandler() {
            @Override
            public RequestResult process() {
                String url = _urlPrex + "getResult?id=" + id;

                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        String cookie;
                        try {
                            cookie = URLDecoder.decode(m.get("cookie"), "utf-8");
                        } catch (Exception e) {
                            cookie = "";
                        }
                        return new DecodeResult(id, "", m.get("result"), cookie);
                    }
                });
            }
        });

        if (res instanceof DecodeResult) {
            return (DecodeResult) res;
        } else {
            return new DecodeResult(res.ret, res.desc, null, null);
        }
    }

    //得到指定ID的请求的结果，直到打码完成、超时未取得结果或者服务器返回错误
    //id：验证码ID，由decode或decodeUrl返回
    //timeout: 单位，毫秒
    //返回值：DecodeResult.ret >=0时, 得到结果成功，返回ID； <0失败
    public DecodeResult getResultUntilDone(int id, int timeout) {
        long startTime = System.currentTimeMillis();
        while (true) {
            DecodeResult decodeResult = getResult(id);
            if (decodeResult.ret >= 0) {
                return decodeResult;
            }
            if (decodeResult.ret != -303) //验证码尚未打码完成
                return decodeResult;

            if (System.currentTimeMillis() - startTime > timeout) {
                return new DecodeResult(errGetResultTimeout, getErrorDesc(errGetResultTimeout), null, null);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //请求打码并读取打码结果，直到结果返回或者错误
    //type：验证码类型ID
    //timeout：超时秒数
    //data：图片数据
    //返回值：DecodeResult.ret >=0时, 得到结果成功，返回ID，可用于reportError； <0失败
    public DecodeResult decodeAndGetResult(int type, int timeout, byte[] data) {
        RequestResult result = decode(type, timeout, data);
        if (result.ret < 0) {
            return new DecodeResult(result.ret, result.desc, null, null);
        }

        return getResultUntilDone(result.ret, timeout * 1000);
    }

    //请求打码并读取打码结果，直到结果返回或者错误
    //type：验证码类型ID
    //timeout：超时秒数
    //txt：文本数据
    //返回值：DecodeResult.ret >=0时, 得到结果成功，返回ID，可用于reportError； <0失败
    public DecodeResult decodeAndGetResult(int type, int timeout, String txt) {
        RequestResult result = decode(type, timeout, txt);
        if (result.ret < 0) {
            return new DecodeResult(result.ret, result.desc, null, null);
        }

        return getResultUntilDone(result.ret, timeout * 1000);
    }

    //读取用户余额
    public RequestResult reportError(final int id) {
        RequestResult result = handleRequest(new IActualHandler() {
            @Override
            public RequestResult process() {
                String url = _urlPrex + "reportError?id=" + id;
                return doRequest(url, new IResultHandler() {
                    @Override
                    public RequestResult handleSucc(Map<String, String> m) {
                        return genRequestResult(0);
                    }
                });
            }
        });

        return result;
    }

    //执行HTTP请求并解析返回结果
    private RequestResult doRequest(String url, IResultHandler handler) {
        return doRequest(url, handler, true, true);
    }

    //执行HTTP请求并解析返回结果，支持设置是否保存授权信息和在请求中加入授权信息
    private RequestResult doRequest(String url, IResultHandler handler, boolean saveAuth, boolean loadAuth) {
        return doRequest(url, handler, null, saveAuth, loadAuth);
    }

    //执行HTTP请求，支持POST方法的数据获得
    @SuppressWarnings("unchecked")
    private RequestResult doRequest(String url, IResultHandler handler, IPostDataGettor postDataGettor, boolean saveAuth, boolean loadAuth) {
        try {
            //在URL地址中附带授权信息
            String auth = null;
            if (loadAuth) {
                synchronized (this) {
                    auth = _auth;
                }
                if (postDataGettor == null) {
                    if (url.indexOf('?') > 0) {
                        url += "&auth=" + auth;
                    } else {
                        url += "?auth=" + auth;
                    }
                }
            }

            //打开连接，设置连接属性
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
            conn.setUseCaches(false);
            conn.setConnectTimeout(15 * 1000);
            conn.setReadTimeout(40 * 1000);

            //处理POST方法的处理
            if (postDataGettor != null) {
                //处理属性
                Map<String, String> properties = postDataGettor.getProperties();
                Map<String, String> newProp = null;
                if (properties != null && auth != null) {
                    newProp = new HashMap<String, String>(properties);
                    newProp.put("auth", auth);
                } else if (properties == null) {
                    newProp = new HashMap<String, String>();
                    newProp.put("auth", auth);
                } else {
                    newProp = properties;
                }

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Charsert", "UTF-8");
                //处理存在文件数据的情况
                if (postDataGettor.getPostData() != null) {
                    String BOUNDARY = "------WebKitFormBoundary"; //数据分隔线
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

                    //写属性
                    OutputStream os = conn.getOutputStream();
                    if (newProp != null) {
                        Iterator<String> it = newProp.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            String value = newProp.get(key);
                            StringBuilder sb = new StringBuilder();
                            sb.append("\r\n--").append(BOUNDARY)
                                    .append("\r\nContent-Disposition: form-data;name=\"").append(key).append("\";")
                                    .append("\r\nContent-Type:plain/text\r\n\r\n").append(value);
                            os.write(sb.toString().getBytes());
                        }
                    }

                    //写文件数据
                    StringBuilder sb = new StringBuilder();
                    sb.append("\r\n--").append(BOUNDARY).append("\r\nContent-Disposition: form-data;name=\"data\";filename=\"pic.jpg\"\r\nContent-Type:image/jpg\r\n\r\n");
                    os.write(sb.toString().getBytes());
                    os.write(postDataGettor.getPostData());

                    //写结束符
                    StringBuilder sbEnd = new StringBuilder();
                    sbEnd.append("\r\n--").append(BOUNDARY).append("--\r\n");
                    os.write(sbEnd.toString().getBytes());
                    os.flush();
                } else if (newProp != null && newProp.size() > 0) {    //处理没有文件数据但有属性的情况
                    StringBuilder sb = new StringBuilder();
                    Iterator<String> it = newProp.keySet().iterator();
                    while (it.hasNext()) {
                        String key = it.next();
                        String value = newProp.get(key);
                        if (sb.length() > 0)
                            sb.append("&");
                        sb.append(key).append("=").append(value);
                    }

                    byte[] data = sb.toString().getBytes();
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", String.valueOf(data.length));

                    OutputStream os = conn.getOutputStream();
                    os.write(data);
                    os.flush();
                }
            }

            //连接服务器，与服务器通讯并分析响应码
            conn.connect();
            int ret = conn.getResponseCode();
            if (ret != 200) {
                return new RequestResult(errRespError, conn.getResponseMessage() + "(" + ret + ")");
            }

            //读取返回数据对象
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            ObjectMapper om = new ObjectMapper();
            Map<String, String> m = om.readValue(in, Map.class);
            ret = Integer.parseInt(m.get("ret"));
            String desc = m.get("desc");
            if (ret < 0) {
                return new RequestResult(ret, desc);
            }

            //保存授权信息
            if (saveAuth) {
                synchronized (this) {
                    _auth = m.get("auth");
                }
            }

            //成功发表的后续处理
            return handler.handleSucc(m);
        } catch (JsonParseException e) {
            e.printStackTrace();
            return genRequestResult(errJsonParseError);
        } catch (JsonMappingException e) {
            e.printStackTrace();
            return genRequestResult(errJsonMapError);
        } catch (IOException e) {
            e.printStackTrace();
            return genRequestResult(errIOException);
        }
    }

    //计算加密信息
    private String calcEncInfo(String preauth, String softKey, String uname, String upwd) {
        //压缩软件KEY为8字节，用作DES加密的KEY
        byte[] key16 = hexString2ByteArray(softKey);
        byte[] key8 = new byte[8];
        for (int i = 0; i < 8; i++) {
            key8[i] = (byte) ((key16[i] ^ key16[i + 8]) & 0xff);
        }

        try {
            byte[] pwd_data = upwd.getBytes("utf-8");
            java.security.MessageDigest md5 = java.security.MessageDigest.getInstance("MD5");
            md5.update(pwd_data, 0, pwd_data.length);
            String pwd_md5_str = byteArray2HexString(md5.digest()); //转为16进制字符串

            String enc_data_str = preauth + "\n" + uname + "\n" + pwd_md5_str;

            SecureRandom sr = new SecureRandom();
            DESKeySpec dks = new DESKeySpec(key8);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, sr);
            byte[] resultData = cipher.doFinal(enc_data_str.getBytes("utf-8"));
            return byteArray2HexString(resultData);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    //能用的处理请求函数，如果需要登陆，则请求预授权，则登陆，最后发出实际请求
    private RequestResult handleRequest(IActualHandler handler) {
        int tryTime = 0;
        RequestResult res = null;
        while (tryTime < 2) {
            tryTime++;

            boolean bNeedLogin = false;
            synchronized (this) {
                if (_auth == null) {
                    bNeedLogin = true;
                }
            }

            if (bNeedLogin) {
                do {
                    res = preAuth();
                }
                while (processBusy(res.ret));
                if (res.ret < 0) {
                    return res;
                }

                do {
                    res = login(res.desc, _appID, _softKey, _uname, _upwd);
                }
                while (processBusy(res.ret));
                if (res.ret < 0) {
                    return res;
                }
            }

            do {
                res = handler.process();

            } while (processBusy(res.ret));

            if (res.ret != -10003 &&    // encinfo timeout
                    res.ret != -10004 &&    //ip inconsistent
                    res.ret != -10001)        //invalid auth
            {
                return res;
            }

            synchronized (this) {
                _auth = null;
            }
        }

        return res;
    }

    //处理系统忙返回码，如果返回码是系统忙，睡眠500ms，返回true；否则，返回false
    private static boolean processBusy(int ret) {
        if (ret == -10000) { //busy
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    //预授权
    //成功时预授权信息从RequestResult.desc返回
    private RequestResult preAuth() {
        String url = _urlPrex + "preauth";
        return doRequest(url, new IResultHandler() {
            @Override
            public RequestResult handleSucc(Map<String, String> m) {
                return new RequestResult(0, m.get("auth"));
            }
        }, false, false);
    }

    //登陆
    private RequestResult login(String preAuthInfo, int appID, String softKey, String uname, String upwd) {
        String url = _urlPrex + "login";
        String encInfo = calcEncInfo(preAuthInfo, softKey, uname, upwd);
        if (encInfo == null) {
            return genRequestResult(errCalcEncInfo);
        }

        url = url + "?appID=" + appID + "&encinfo=" + encInfo;

        return doRequest(url, new IResultHandler() {
            @Override
            public RequestResult handleSucc(Map<String, String> m) {
                return genRequestResult(0);
            }
        }, true, false);
    }

    //16进制字符串转为BYTE数组
    private byte[] hexString2ByteArray(String hexStr) throws NumberFormatException {
        int len = hexStr.length();
        if (len % 2 != 0)
            throw new NumberFormatException();

        byte[] result = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            String s = hexStr.substring(i, i + 2);
            int n = Integer.parseInt(s, 16);
            result[i / 2] = (byte) (n & 0xff);
        }

        return result;
    }

    //转化BYTE数组为16进制字符串
    private String byteArray2HexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            String s = Integer.toHexString(b & 0xff);
            if (s.length() == 1) {
                sb.append("0" + s);
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    //根据错误码得到得到错误描述
    private String getErrorDesc(int ret) {
        switch (ret) {
            case 0:
                return "成功";
            case errCalcEncInfo:
                return "calc encinfo error";
            case errBalanceNotNumber:
                return "balance is not number";
            case errIdNotNumber:
                return "id is not number";
            case errEncodingError:
                return "encoding error";
            case errRespError:
                return "response error";
            case errJsonParseError:
                return "Json parse error";
            case errJsonMapError:
                return "Json map error";
            case errIOException:
                return "IO Exception";
            case errGetResultTimeout:
                return "Get Result timeout";
        }
        return "" + ret;
    }

    //根据返回码生成请求结果
    private RequestResult genRequestResult(int ret) {
        return new RequestResult(ret, getErrorDesc(ret));
    }


    //////////////////////////////////
    //          内部接口定义
    //////////////////////////////////
    private interface IActualHandler { //实际处理器
        RequestResult process();
    }

    private interface IResultHandler    //结果处理 器
    {
        RequestResult handleSucc(Map<String, String> m);
    }

    private interface IPostDataGettor    //POST数据提供者
    {
        byte[] getPostData();

        Map<String, String> getProperties();
    }

    public static void main(String[] argvs) {
        String key = "9503ce045ad14d83ea876ab578bd3184";
        String uname = "user";
        String upwd = "test";
        int idApp = 205;
        Dama2Web web = new Dama2Web(idApp, "", uname, upwd);
        String preAuth = "1234567890abcdef1234567890abcdef";
        String s = web.calcEncInfo(preAuth, key, uname, upwd);
        System.out.println(s);
    }


    //成员变量定义
    private String _auth;            //返回的授权信息

    private final int _appID;        //应用ID
    private final String _softKey;    //软件KEY
    private final String _uname;    //用户名
    private final String _upwd;        //用户密码
}
