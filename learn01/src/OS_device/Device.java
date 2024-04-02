package OS_device;

import java.io.*;
import java.util.*;

public class Device {
    public static final int DNUM = 5;  //设备数量
    public static final String deviceName[] = { "disk","mouse","displayer","printer","keyboard" };
    public static final String logname = "log.txt";  //日志
    public static String msg = "";  //日志信息
    private static List<Long> Device = new LinkedList<>();
    private static Map<String,List<String>> DeviceState = new HashMap<>();  //String：设备名称    List<Long>：进程等待队列

    public static boolean acquire(String pid, String device)
    {
        //检查进程是否重复申请设备
        List<String> list = DeviceState.get(device);//当前设备的排队表

        if(list.contains(pid))
        {
            //当前pid已在排队队列中
            msg = "process " + pid  + " repeat acquire device " + device;
            log(msg);
            //打印日志
            return false;
        }
        list.add(pid);
        DeviceState.put(device,list);
        if(list.size() > 1)
        {
            msg = "process " + pid  + " acquire device " + device + " and waiting";
            log(msg);
            return false;
        }
        msg = "process " + pid  + " acquire device " + device + " successfully";
        log(msg);
        return true;
    }

    public static boolean release(String pid, String device)
    {
        List<String> list = DeviceState.get(device);
        if(list.contains(pid))
        {
            list.remove(pid);
            DeviceState.put(device,list);
            msg = "process " + pid  + " release device" + device + " successfully";
            log(msg);
            return true;
        }
        else
        {
            msg = "process " + pid  + " haven't acquire device" + device ;
            log(msg);
            return false;
        }
    }

    public static void show_device()
    {
        System.out.println("------------------------ Device show ------------------------");
        Set<Map.Entry<String, List<String>>> entries = DeviceState.entrySet();
        for(Map.Entry<String, List<String>> entry : entries)
        {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            System.out.println("Device " + key + "  queue state: " + value);
        }
        System.out.println("--------------------------------------------------------------");
    }

    public static void log(String s)
    {
        s += '\n';
        try(BufferedWriter wr = new BufferedWriter(new FileWriter(logname,true)))
        {
            wr.write(s);
        }
        catch (IOException e) {
            System.out.println("file open fail");
            e.printStackTrace();
            System.exit(1);
        }
    }


    public static void clearFile(String filename) {
        try {
            File file = new File(filename);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(0);
            raf.close();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        clearFile(logname);

        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();
        list1.add("a");
        list1.add("b");
        list1.add("c");
        DeviceState.put("keyboard",list1);
        DeviceState.put("mouse",list2);

        acquire("12345","mouse");
        acquire("12345","mouse");
        acquire("12345","mouse");
        acquire("111","mouse");
        show_device();
        release("12345","mouse");
        show_device();

    }
}
