/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.cbrs485.core;

import gnu.io.SerialPortEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import ru.evgenic.rxtx.serialPort.IReceiveListener;
import ru.evgenic.rxtx.serialPort.ISerialPort;
import ru.evgenic.rxtx.serialPort.RxtxSerialPort;

/**
 * Создает поток, в потоке порт и им шлет.
 * @author Evgeniy Egorov
 */
public class Sender485 {

    final private LinkedBlockingQueue<Event> stream = new LinkedBlockingQueue<>();
    final private Thread sndThread;
    final private ISerialPort port;

    private Sender485() {
        // подготовим поток для отсыла
        sndThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    final Event event;
                    try {
                        event = stream.take();
                    } catch (InterruptedException ex) {
                        break;
                    }
                    final AddrProp.Addr addr = AddrProp.getInstance().getAddr(event.point);
                    if (addr == null) {
                        System.out.println("!!!!!!!!!!!!!  " + event.point + " NOT FOUND");
                        continue;
                    }
                    //1amA147>127
                    // 7й или 8й байт :  0x3C < знак «меньше» Светится стрелка влево | 0x3E > знак «больше» Светится стрелка вправо | 0x2D - знак «минус» Светится черточка без указания направления
                    final byte[] bytes = (("123" + event.ticket + "       ").substring(0, addr.position + 2) + "-" + event.point + "         ").subSequence(0, 11).toString().getBytes();
                    bytes[0] = 0x01; // начало
                    bytes[10] = 0x07; // конец
                    bytes[1] = addr.addres; // адрес
                    bytes[2] = 0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
                    bytes[addr.position + 2] = addr.arrow; // стрелочка


                    switch (event.state) {
                        case STATE_INVITED:
                            bytes[2] = 0x21; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно;
                            break;
                        case STATE_INVITED_SECONDARY:
                            bytes[2] = 0x21; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно;
                            break;
                        case STATE_WORK:
                            break;
                        case STATE_WORK_SECONDARY:
                            break;
                        case STATE_DEAD:
                            bytes[addr.position + 2] = 'd'; // стрелочка убирается
                            break;
                        case STATE_FINISH:
                            bytes[addr.position + 2] = 'd'; // стрелочка убирается
                            break;
                        case STATE_POSTPONED:
                            bytes[addr.position + 2] = 'd'; // стрелочка убирается
                            break;
                        case STATE_REDIRECT:
                            bytes[addr.position + 2] = 'd'; // стрелочка убирается
                            break;
                        default:// нужная вещь. чтобы отсечь состояния, которые не при чем в зональном табло
                            return;

                    }
                    try {
                        System.out.println("SEND TO RS485 " + new String(bytes));
                        port.send(bytes);
                    } catch (Exception ex) {
                        System.err.println("!!! ERROR !!! " + ex);
                    }


                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });
        sndThread.setDaemon(true);


        // подготовим порт для отсыла

        // параметры порта
        try (FileInputStream fis = new FileInputStream(propFile)) {
            props.load(fis);
        } catch (IOException ex) {
            System.err.println(ex);
            throw new RuntimeException(ex);
        }

        try {
            port = new RxtxSerialPort(props.getProperty("port.name", "COM1"));
        } catch (Exception ex) {
            System.err.println(ex);
            throw new RuntimeException(ex);
        }
        port.setSpeed(Integer.parseInt(props.getProperty("port.speed", "6900")));
        port.setDataBits(Integer.parseInt(props.getProperty("port.bits", "8")));
        port.setParity(props.getProperty("port.parity", "0").equals("1") ? 1 : 0);
        port.setStopBits(Integer.parseInt(props.getProperty("port.stopbits", "1")));
        try {
            port.bind(new IReceiveListener() {

                @Override
                public void actionPerformed(SerialPortEvent spe, byte[] bytes) {
                    System.out.println("!!!!!!!!!!!!!!!!!!!! " + bytes);
                }

                @Override
                public void actionPerformed(SerialPortEvent spe) {
                }
            });
        } catch (Exception ex) {
            System.err.println(ex);
            throw new RuntimeException(ex);
        }
    }
    final static private File propFile = new File("config/ClientboardRS485Plugin.property");
    private final Properties props = new Properties();

    public static Sender485 getInstance() {
        return Sender485Holder.INSTANCE;
    }

    private static class Sender485Holder {

        private static final Sender485 INSTANCE = new Sender485();
    }

    public void send(Event event) {
        if (sndThread.getState() == Thread.State.NEW || sndThread.getState() == Thread.State.TERMINATED) {
            sndThread.start();
        }
        try {
            stream.put(event);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }
}
