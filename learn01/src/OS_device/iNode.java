package OS_device;

import java.nio.ByteBuffer;

public class iNode {
    //iNode内部属性
    public int token;  //  文件 / 文件夹
    public final static int FILE = 1;
    public final static int FOLDER = 2;
    public int size;  //文件大小
    public int disk_position ;  //文件数据block在磁盘中的位置
    public int link_num;  //被多少文件指向 判断iNode是否回收
    public int i_num; //inode号
    private static final int iNodeSize = 128;  //iNode大小（字节）
    public int authority;
    public static final int READ_AND_WRITE = 1; //文件权限：可读可写
    public static final int READONLY = 2; //文件权限 ：只读
    public static final int WRITEONLY = 3; //文件权限：只写
    public static final int NEITHER_READ_NOR_WRITE = 4; //文件权限：既不可读也不可写

    public static void init_iNode(iNode node, int n)
    {
        node.token = FILE;
        node.size = 0;
        node.link_num = 0;
        node.disk_position = -1;
        node.i_num = n;
        node.authority = READ_AND_WRITE; //默认可读可写
    }
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(iNodeSize);
        buffer.putInt(token);
        buffer.putInt(size);
        buffer.putInt(disk_position);
        buffer.putInt(link_num);
        buffer.putInt(i_num);
        buffer.putInt(authority);
        return buffer.array();
    }
    public void fromByteArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        this.token = buffer.getInt();
        this.size = buffer.getInt();
        this.disk_position = buffer.getInt();
        this.link_num = buffer.getInt();
        this.i_num = buffer.getInt();
        this.authority = buffer.getInt();
    }
}
