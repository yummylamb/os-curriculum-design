package OS_device;

public class SuperBlock {
    //描述全局信息 硬盘上文件的整体信息
    public int blockNum;  //磁盘中物理块数量
    public int inodeNum;  //磁盘中iNode数量
    public int maxFileSize;   //文件最大长度
}
