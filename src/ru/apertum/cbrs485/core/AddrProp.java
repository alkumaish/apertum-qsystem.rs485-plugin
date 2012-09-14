/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

/**
 *
 * @author Evgeniy Egorov
 */
public class AddrProp {

    final private HashMap<String, Addr> addrs = new HashMap<> ();

    public static class Addr {

        final String point;
        final byte addres;
        final int position;
        final byte arrow;

        public Addr(String point, byte addres, int position, byte arrow) {
            this.point = point;
            this.addres = addres;
            this.position = position;
            this.arrow = arrow;
        }
    }
    final static private File addrFile = new File("config/ClientboardRS485Plugin.adr");

    private AddrProp() {
        try (FileInputStream fis = new FileInputStream(addrFile); Scanner s = new Scanner(fis)) {
            while (s.hasNextLine()) {
                final String line = s.nextLine().trim();
                if (!line.startsWith("#")) {
                    final String[] ss = line.split("=");
                    final String[] ssl = ss[1].split(" ");
                    addrs.put(ss[0], new Addr(ss[0], Byte.parseByte(ssl[0]), Integer.parseInt(ssl[1]), ssl[2].getBytes()[0]));
                }
            }
        } catch (IOException ex) {
            System.err.println(ex);
            throw new RuntimeException(ex);
        }
    }

    public static AddrProp getInstance() {
        return AddrPropHolder.INSTANCE;
    }

    private static class AddrPropHolder {

        private static final AddrProp INSTANCE = new AddrProp();
    }

    public Addr getAddr(String point) {
        return addrs.get(point);
    }

    public static void main(String[] ss) {
        System.out.println("addrs:");
        for (String string : getInstance().addrs.keySet()) {
            byte[] bb = new byte[1];
            bb[0] = getInstance().getAddr(string).arrow;
            System.out.println("- " + getInstance().getAddr(string).point
                    + "=" + getInstance().getAddr(string).addres
                    + " " + getInstance().getAddr(string).position
                    + " " + new String(bb));

        }
    }
}
