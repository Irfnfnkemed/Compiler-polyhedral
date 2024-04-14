package src.ASM.instruction;

abstract public class ASMInstr {
    public String[] use = new String[2];
    public String def;
    public int useNum = 0;
    public boolean notRemove = false;//在活跃分析时，不会被去除(主要用于最后ret)
    public boolean visited = false;//是否已经进行活跃分析
    public boolean ignore = false;//是否移除，用于内联后的入参消除
    public String preColoredFrom, preColoredTo;//预着色寄存器
}
