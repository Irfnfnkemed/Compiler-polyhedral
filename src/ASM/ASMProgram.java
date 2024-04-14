package src.ASM;

public class ASMProgram {
    public Section sectionText;
    public Section sectionData;
    public Section sectionRodata;

    public ASMProgram(){
        sectionText = new Section("text");
        sectionData = new Section("data");
        sectionRodata = new Section("rodata");
    }



}
