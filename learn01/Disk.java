package OS_device;

import OS_file.FCB;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Disk {
    //数值
    private static final int BLOCK_SIZE = 64;  //物理块大小
    private static final int MAX_BLOCK_NUM = 512;  //最大物理块数量
    private static final int MAX_FILE_NUM = 128;  //最大iNode数量
    private static final int MAX_FILE_SIZE = 128; //最大文件大小
    private static final int INODE_SIZE = 128;  //iNode大小（字节）

    private static final int READ_AND_WRITE = 1; //文件权限：可读可写
    private static final int READONLY = 2; //文件权限 ：只读
    private static final int WRITEONLY = 3; //文件权限：只写
    private static final int NEITHER_READ_NOR_WRITE = 4; //文件权限：既不可读也不可写

    public static String msg = "";
    public static final String LOG_PATH = "log.txt";
    private static final String SUPERBLOCK_PATH = "superblock.txt";
    private static final String INODES_PATH = "inodes.txt";
    private static final String IBITMAP_PATH = "ibitmap.txt";
    private static final String DATABITMAP_PATH = "databitmap.txt";
    private static final String DATA_PATH = "data.txt";
    private static final String FCB_PATH = "fcb.txt";
    private static int new_node() {
        //申请数据块 修改ibitmap 返回块号 无空闲块返回-1
            try (BufferedReader reader = new BufferedReader(new FileReader(IBITMAP_PATH))){

            String iBitMap = reader.readLine();
            String str = iBitMap.toString();
            int freeIndex = str.indexOf('0');  //格式：'null'0'null'1'null1
            if (freeIndex != -1) {
                //空闲iNode索引
                update_iBitmap((freeIndex-1)/2, '1');
                return (freeIndex-1)/2;  //返回空闲块号
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1; // 没有空闲iNode
    }
    private static int free_node(iNode node)
    {
        //link_num判断逻辑 ？
        int pos = node.i_num;
        char[] bitmap = read_Bitmap();
        if(bitmap[pos] == '0')
        {
            msg = "try to free an empty node";  //error
            System.out.println(msg);
            return -1;
            //错误处理信息  ？
            //try free empty node
        }
        //修改位图相应位置
        bitmap[pos] = '0';
        init_Bitmap(bitmap);

        //用一个空node替换原位置node内容
        iNode n = new iNode();
        iNode.init_iNode(n,pos);
        //n.size = 100;
        update_iNode(n);
        return 0;
    }
    public static void update_iNode(iNode node) {
        try (RandomAccessFile file = new RandomAccessFile(INODES_PATH, "rw")) {
            long position = (long) node.i_num * INODE_SIZE;
            file.seek(position);

            //file.writeInt(node.token);
            file.writeInt(node.size);
            file.writeInt(node.disk_position);
            file.writeInt(node.link_num);
            file.writeInt(node.i_num);
            file.writeInt(node.authority);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void init_iNodes(iNode[] nodes) {
    try (RandomAccessFile disk = new RandomAccessFile(INODES_PATH, "rw")) {
        // 初始化磁盘中的iNode内容
        for (int i = 0; i < MAX_FILE_NUM; i++)
        {
            long position = (long) i * INODE_SIZE;
            disk.seek(position);

            //disk.writeInt(nodes[i].token);
            disk.writeInt(nodes[i].size);
            disk.writeInt(nodes[i].disk_position);
            disk.writeInt(nodes[i].link_num);
            disk.writeInt(nodes[i].i_num);
            disk.writeInt(nodes[i].authority);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    public static iNode read_iNodes_Disk(int inum) {
        try (RandomAccessFile file = new RandomAccessFile(INODES_PATH, "r")) {
            long position = (long) inum * INODE_SIZE;
            file.seek(position);

            iNode node = new iNode();
            //node.token = file.readInt();
            node.size = file.readInt();
            node.disk_position = file.readInt();
            node.link_num = file.readInt();
            node.i_num = file.readInt();
            node.authority = file.readInt();

            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void init_Bitmap(char[] bitMap) {
        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(IBITMAP_PATH))) {
            for (char bit : bitMap) {
                outputStream.writeChar(bit);
            }

            //System.out.println("BitMap written to disk.");
        } catch (IOException e) {
            System.err.println("Error writing BitMap to disk: " + e.getMessage());
        }
    }
    private static char[] read_Bitmap() {
        //读取磁盘中bitmap内容 返回bitmap数组char[]
        List<Character> bitMap = new ArrayList<>();

        try (DataInputStream inputStream = new DataInputStream(new FileInputStream(IBITMAP_PATH))) {
            while (inputStream.available() > 0) {
                char bit = inputStream.readChar();
                bitMap.add(bit);
            }

            //System.out.println("BitMap read from disk.");
        } catch (IOException e) {
            System.err.println("Error reading BitMap from disk: " + e.getMessage());
        }

        char[] bitMapArray = new char[bitMap.size()];
        for (int i = 0; i < bitMap.size(); i++) {
            bitMapArray[i] = bitMap.get(i);
        }

        return bitMapArray;
    }
    private static void update_iBitmap(int index, char value) {
        index = index * 2 + 1;
        //更新iBitMap
        try (BufferedReader reader = new BufferedReader(new FileReader(IBITMAP_PATH));
             BufferedWriter writer = new BufferedWriter(new FileWriter("tmp.txt" ))) {
            String iBitMap = reader.readLine();
            StringBuilder updatedBitMap = new StringBuilder(iBitMap);
            updatedBitMap.setCharAt(index, value);
            writer.write(updatedBitMap.toString());
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileInputStream fis = new FileInputStream("tmp.txt");
             FileOutputStream fos = new FileOutputStream(IBITMAP_PATH)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void init_disk(int flag)
    {
        //初始化日志
        try {
            FileWriter logWriter = new FileWriter(LOG_PATH);
            logWriter.close();
        } catch (IOException e) {
            msg = "IOException";
            log(msg);
            System.exit(1);
        }
        if(flag == 1 || !is_disk_initialized())
        {
            //初始化超级块
            SuperBlock sb = new SuperBlock();
            sb.blockNum = MAX_BLOCK_NUM;
            sb.inodeNum = MAX_FILE_NUM;
            sb.maxFileSize = MAX_FILE_SIZE;

            //初始化iNode表
            iNode[] iNodes = new iNode[MAX_FILE_NUM];
            for (int i = 0; i < MAX_FILE_NUM; i++)
            {
                iNodes[i] = new iNode();
                iNode.init_iNode(iNodes[i],i);
            }

            //初始化iNodeBitMap
            char[] iBitMap = new char[MAX_FILE_NUM];
            for (int i = 0; i < MAX_FILE_NUM; i++) {
                iBitMap[i] = '0';
            }

            //初始化DataBitMap
            char[] dataBitMap = new char[MAX_BLOCK_NUM];
            for (int i = 0; i < MAX_BLOCK_NUM; i++) {
                dataBitMap[i] = '0';
            }

            //写入磁盘
            init_superblock(sb);
            init_iNodes(iNodes);
            init_Bitmap(iBitMap);
            init_Databitmap(dataBitMap);
        }
    }
    private static boolean is_disk_initialized() {
        // 判断磁盘是否已经初始化过的逻辑
        // ...
        return false;
    }

    public static void erase(int offset, int size) {
        try (RandomAccessFile file = new RandomAccessFile(DATA_PATH, "rw")) {
            file.seek(offset);

            StringBuilder emptyBuilder = new StringBuilder();
            for (int i = 0; i < size; i++) {
                emptyBuilder.append(" ");
            }
            String emptyString = emptyBuilder.toString();

            file.write(emptyString.getBytes());
            //System.out.println("指定空间已成功置为空");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static int write_block(int offset, String data, int blocknum) {
    //从offset * BLOCK_SIZE开始写，超过data长度的部分置为空
    if (data.length() > blocknum * BLOCK_SIZE) {
        msg = "Error: Data length exceeds block size.";  //error
        System.out.println(msg);
        log(msg);
        return -1;
    }
    try (RandomAccessFile file = new RandomAccessFile(DATA_PATH, "rw")) {
        file.seek(offset*BLOCK_SIZE);

        file.write(data.getBytes());  //写入内容

        int remaining = blocknum * BLOCK_SIZE - data.length();
        if (remaining > 0) {
            erase(offset*BLOCK_SIZE+data.length(),remaining);  //清空剩余段
        }
        return 0;
    } catch (IOException e) {
        e.printStackTrace();
        return -1;
    }
}


    public static String read_block(int offset, int blocknum) {
        //从偏移量offset处开始读取，读取block数量=blocknum，即读取大小为 blocknum*blocksize
        StringBuilder content = new StringBuilder();

        try (RandomAccessFile disk = new RandomAccessFile(DATA_PATH, "r")) {
            disk.seek(offset * BLOCK_SIZE);

            byte[] buffer = new byte[blocknum * BLOCK_SIZE];
            int bytesRead = disk.read(buffer);

            if (bytesRead != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content.toString();
    }
    public static int new_block(int type, int filesize, int authority){
        int node_num = new_node();  //分配iNode结点
        if(node_num == -1)  //无空闲inode
            return -1;

        //需要申请的块数
        int num = 0;
        if(filesize % BLOCK_SIZE == 0)
            num = filesize / BLOCK_SIZE;
        else
            num = (filesize / BLOCK_SIZE)  + 1;

        //查看是否有空闲block
        char[] dbmap = read_Databitmap();
        int[] index = new int[num];
        for(int i = 0; i < num; i++)
        {
            index[i] = -1;
        }
        int t = 0;
        for( int j = 0; j < num; j++)
        {
            for (int i = 0; i < dbmap.length; i++,t++) {
                if (dbmap[i] == '0' ) {
                    if(j == 0 ||(j != 0 && i == index[j-1] + 1))
                    {
                        //改位空闲且与上一个分配的位相邻
                        index[j] = i;  //存储已分配的块的索引
                        break;
                    }

                }
            }
        }
        if(index[num-1] == -1) {
            //最后一个块没有分配到空间 即找不到空闲物理块
            msg = "The pysical block is all full";  //error
            System.out.println(msg);
            log(msg);
            return -1;
        }
        else
        {
            for(int i = 0; i < num; i++)
            {
                //修改磁盘中块的位图，相应位置改为占用状态
                update_Databitmap(index[i],'1');
            }
            //修改iNode表内容 添加该项
            iNode node = new iNode();
            //node.token = type;
            node.size = filesize;
            node.i_num = node_num;
            node.disk_position = index[0];
            node.authority = authority;
            update_iNode(node);
            return node_num;
        }
    }
    public static int update_block(int i_num, String content)  //成功修改返回1 修改失败返回-1
    {
        iNode node = read_iNodes_Disk(i_num);
        int authority = node.authority;
        if (authority == READONLY || authority == NEITHER_READ_NOR_WRITE)
        //只读或不可读不可写
        {
            msg = "Didn't has authority";
            System.out.println(msg);
            log(msg);
            return -1;
        }
        else
        {
            int index = node.disk_position;
            byte[] info = content.getBytes();
            int size = node.size;
            int bnum = 0;
            if(size % BLOCK_SIZE == 0)
                bnum = size / BLOCK_SIZE;
            else bnum = size / BLOCK_SIZE + 1;

            int tag = write_block(index, content, bnum);//修改内容
            if (tag == -1)
            {
                msg = "fail to write block";
                System.out.println(msg);
                log(msg);
                return -1;
            }
            msg = "Update file "+ i_num + " successfully";
            //System.out.println(msg);
            log(msg);
            return 1;
        }
    }
    public static int read_block_disk(int i_num)//成功读取返回1 修改失败返回-1
    {
        iNode node = read_iNodes_Disk(i_num);
        int authority = node.authority;
        if(authority == NEITHER_READ_NOR_WRITE || authority == WRITEONLY)
        {
            msg = "File "+ i_num + "didn't has authotity";
            System.out.println(msg);
            log(msg);
            return -1;
        }
        else
        {
            int index = node.disk_position;
            int size = node.size;
            int bnum = 0;
            if(size % BLOCK_SIZE == 0)
                bnum = size / BLOCK_SIZE;
            else bnum = size / BLOCK_SIZE + 1;
            String info = read_block(index, bnum);
            System.out.println(info);
            return 1;
        }

    }
    public static int free_block(int inum){
        iNode node = read_iNodes_Disk(inum);
        int tag = free_node(node);
        if(tag == -1)
        {
            msg = "File " + inum + " try to free a block who's inode is empty";
            System.out.println(msg);
            log(msg);
            return -1;
        }
        int disk_pos = node.disk_position;
        int size = node.size;
        int bnum = 0;
        if(size % BLOCK_SIZE == 0)
            bnum = size/BLOCK_SIZE;
        else
            bnum = size/BLOCK_SIZE + 1;
        //修改该文件占用的所有block块为空闲状态
        for (int i = 0; i < bnum; i++)
        {
            update_Databitmap(disk_pos+i,'0');
        }
        write_block(disk_pos,"",bnum);
        msg = "File " + inum + " has been released successfully";
        log(msg);

        return 0;
    }
    private static void update_Databitmap(int index, char value)
    {
        try (RandomAccessFile file = new RandomAccessFile(DATABITMAP_PATH, "rw")) {
            file.seek(index);
            file.write(value & 0xFF);
            msg = "DataBitMap update successfully.";
            //System.out.println(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static char[] read_Databitmap() {
        List<Character> charList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(DATABITMAP_PATH), "UTF-8"))) {
            int charCode;
            while ((charCode = reader.read()) != -1) {
                char character = (char) charCode;
                charList.add(character);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        char[] result = new char[charList.size()];
        for (int i = 0; i < charList.size(); i++) {
            result[i] = charList.get(i);
        }

        return result;
    }
    private static void init_Databitmap(char[] dataBitMap) {
        try (FileWriter writer = new FileWriter(DATABITMAP_PATH)) {
            writer.write(dataBitMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void init_superblock(SuperBlock superBlock)  {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SUPERBLOCK_PATH))) {
            writer.write("BlockNum: " + superBlock.blockNum);
            writer.newLine();
            writer.write("INodeNum: " + superBlock.inodeNum);
            writer.newLine();
            writer.write("MaxFileSize: " + superBlock.maxFileSize);
            writer.newLine();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static SuperBlock read_superblock() {
        SuperBlock superBlock = new SuperBlock();
        try (BufferedReader reader = new BufferedReader(new FileReader(SUPERBLOCK_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("BlockNum: ")) {
                    superBlock.blockNum = Integer.parseInt(line.substring(10));
                } else if (line.startsWith("INodeNum: ")) {
                    superBlock.inodeNum = Integer.parseInt(line.substring(10));
                } else if (line.startsWith("MaxFileSize: ")) {
                    superBlock.maxFileSize = Integer.parseInt(line.substring(13));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return superBlock;
    }

    public static  void log(String info)
    {
        try(BufferedWriter wr = new BufferedWriter(new FileWriter(LOG_PATH,true)))
        {
            wr.write(msg);
            wr.newLine();
        }
        catch (IOException e) {
            System.out.println("file open fail");
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static void show_map(char[] map, int type){
        //打印位图
        int per_line = 128;
        int total = 0;
        String info = "";
        if(type == 1)
        {
            total = MAX_BLOCK_NUM;
            info = "---------------------------------------------------show DataBitMap---------------------------------------------------";
        }
        else
        {
            total = MAX_FILE_NUM;
            info = "---------------------------------------------------show iBitMap---------------------------------------------------";
        }
        System.out.println(info);
        int row = total/per_line;
        for (int i = 0; i < row; i++)
        {
            int t = i * per_line;
            for (int j = t; j < t + per_line; j++)
                System.out.print(map[j]);
            System.out.print('\n');
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------------");

    }
    public static void clear_file(String path) {
        try (FileWriter writer = new FileWriter(path, false)) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void file_show(int i_num)
    {
        iNode node = read_iNodes_Disk(i_num);

        System.out.println("-----------------"+ i_num +"-----------------");
        System.out.println("Positon in disk: "+ node.disk_position);
        System.out.println("File size: "+node.size);
        System.out.println("-------------------------------------");
    }

    // 从文件反序列化FCB_Table
    public static void readFCB() {
        try {
            FileInputStream fileIn = new FileInputStream(FCB_PATH);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            FCB.FCB_Table = (List<FCB>) objectIn.readObject();
            objectIn.close();
            fileIn.close();
            System.out.println("从文件" + FCB_PATH + "反序列化了FCB_Table");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //序列化到文件
    public static void writeFCB() {
        try {
            FileOutputStream fileOut = new FileOutputStream(FCB_PATH);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(FCB.FCB_Table);
            objectOut.close();
            fileOut.close();
            System.out.println("FCB_Table已序列化并保存到文件: " + FCB_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        int blockNumber = 0;
        clear_file(DATA_PATH);
        init_disk(1);

//        char[] ibm = readBitMap();
//        for (int i = 0; i < 5 ; i++)
//            System.out.println(i + " " + ibm[i]);
//
//        //test: iNode读写
////        iNode[] nodes = readINodesDisk(0,maxFileNum);
////        free_node(nodes[2]);
////        iNode[] t = readINodesDisk(0,3);
////        for(iNode node : t)
////            System.out.println("node : " + node.token + " " + node.size + " " + node.link_num + " " + node.disk_position);
//
//        SuperBlock sb = readSuperBlock();
//        System.out.println(sb.blockNum + " " + sb.inodeNum + " " + sb.maxFileSize);
////        char[] db = readDataBitMap();
////        for (int i = 0; i < 5 ; i++)
////            System.out.println(db[i]);
//
//        int index = 2;
//        updateIBitMap(index,'1');
//        for(int j = 0; j < 1; j++)
//        {
//            int tag = new_node();
//            System.out.println("空闲iNode为： " + tag + "   申请该iNode");
//            char[] tmp = readBitMap();
//            showmap(tmp,0);
//            int filesize = 128;
//            int inum = new_block(0, filesize,READ_AND_WRITE);
//            System.out.println("申请的iNode号为： " + inum);
//            char[] dbmap = readDataBitMap();
//            showmap(dbmap,1);
//
//            iNode node = readINodesDisk(2);
//                System.out.println("inode: " + node.i_num + "  size: " +node.size);
//            free_node(node);
//            tmp = readBitMap();
//            showmap(tmp,0);
//
//        }
        int inum1 = new_block(1, 64, 1);
        int inum2 = new_block(1, 64, 1);
        int inum3 = new_block(1,128,1);
        int inum4 = new_block(1, 256, 1);

        update_block(inum1,"this is 1");
        update_block(inum2,"this is 2");

        update_block(inum3,"this is 3");

        read_block_disk(inum1);
        //dbmp = readDataBitMap();
        //showmap(dbmp,1);
        read_block_disk(inum2);
        read_block_disk(inum3);

        update_block(inum4,"this is 4");
        read_block_disk(inum4);

        free_block(inum1);
        free_block(inum3);
        int inum5 = new_block(1, 64, 1);
        update_block(inum5,"this is 5");
        read_block_disk(inum5);
        char[] bmp = read_Bitmap();
        show_map(bmp,0);
        char[] dbmp = read_Databitmap();
        show_map(dbmp,1);
        iNode t = read_iNodes_Disk(inum3);  //problem

        file_show(inum3);


//
//        //char[] datas = readDataBitMap();
//        //System.out.println(datas);
//        String str = "this is test";
//        byte[] info = str.getBytes();
////        byte[] con ;
//        writeBlock(0,info);
////        byte[] bytes = readBlock(0);
//////        con = readBlock(DATA_PATH,1);
////        String string = String.valueOf(bytes);
////        System.out.println(string);
//
//        String content = readBlock(0, 1);
//        System.out.println("data： " + content);
//        FCB root = new FCB(FCB.FOLDER, "root", null, READONLY, false);
//        root.fid = "0";
//        root.content = "root content";
//        FCB fcb1 = new FCB(FCB.FILE,"file1.txt",root,READONLY, false);
//        fcb1.fid = "1";
//        fcb1.content = "File1 content";
//
//        FCB fcb2 = new FCB(FCB.FILE,"file2.txt",fcb1,READONLY, false);
////        fcb2.token = FCB.FILE;
//          fcb2.fid = "2";
////        fcb2.fname = "file2.txt";
////        fcb2.parent_folder = null;
////        fcb2.authority = FCB.READ_AND_WRITE;
//        fcb2.size = 2048;
//        fcb2.content = "File2 content";
////        fcb2.runnable = false;
//        fcb2.child_nodes = null;
//        fcb1.child_nodes.add(fcb2);
//        root.child_nodes.add(fcb1);
//        root.child_nodes.add(fcb2);
//        FCB.FCB_Table.add(root);
//        FCB.FCB_Table.add(fcb1);
//        FCB.FCB_Table.add(fcb2);
//
        // 序列化对象
        writeFCB();
        //反序列化对象
        readFCB();

        for (FCB fcb : FCB.FCB_Table) {
            System.out.println("-------------------");
            System.out.println("FID: " + fcb.fid);
            System.out.println("FName: " + fcb.fname);
            System.out.println("Token: " + fcb.token);
            System.out.println("Authority: " + fcb.authority);
            System.out.println("Size: " + fcb.size);
            System.out.println("Content: " + fcb.content);
            System.out.println("Runnable: " + fcb.runnable);
            if(fcb.child_nodes != null)
            {
                for(int i = 0; i <fcb.child_nodes.size(); i++)
                {
                    FCB x = fcb.child_nodes.get(i);
                    System.out.println(x.fname);
                }
            }
            System.out.println("-------------------");
        }

    }


}
