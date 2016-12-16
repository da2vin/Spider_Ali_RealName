package com;

/**
 * Created by Administrator on 2016/7/11.
 */
public class main {

    public static void main(String[] args) {
        AliDownloader ali = new AliDownloader();
        String name = ali.getAliName("13880320666");
        System.out.println(name);

    }
}