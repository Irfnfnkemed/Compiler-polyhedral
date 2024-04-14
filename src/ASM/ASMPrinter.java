package src.ASM;

import src.ASM.instruction.*;
import src.ASM.instruction.binary.*;
import src.ASM.instruction.binaryImme.*;

import java.util.HashMap;
import java.util.function.Consumer;

public class ASMPrinter {

    public ASMProgram asmProgram;
    private final HashMap<Class<? extends ASMInstr>, Consumer<ASMInstr>> instrHandlers;

    public ASMPrinter(ASMProgram asmProgram_) {
        asmProgram = asmProgram_;
        instrHandlers = new HashMap<>();
        instrHandlers.put(LABEL.class, this::printLABEL);
        instrHandlers.put(LI.class, this::printLI);
        instrHandlers.put(LW.class, this::printLW);
        instrHandlers.put(LA.class, this::printLA);
        instrHandlers.put(SW.class, this::printSW);
        instrHandlers.put(MV.class, this::printMV);
        instrHandlers.put(ADDI.class, this::printADDI);
        instrHandlers.put(ADD.class, this::printADD);
        instrHandlers.put(SUB.class, this::printSUB);
        instrHandlers.put(MUL.class, this::printMUL);
        instrHandlers.put(DIV.class, this::printDIV);
        instrHandlers.put(REM.class, this::printREM);
        instrHandlers.put(AND.class, this::printAND);
        instrHandlers.put(ANDI.class, this::printANDI);
        instrHandlers.put(OR.class, this::printOR);
        instrHandlers.put(ORI.class, this::printORI);
        instrHandlers.put(XOR.class, this::printXOR);
        instrHandlers.put(XORI.class, this::printXORI);
        instrHandlers.put(SLL.class, this::printSLL);
        instrHandlers.put(SLLI.class, this::printSLLI);
        instrHandlers.put(SRA.class, this::printSRA);
        instrHandlers.put(SRAI.class, this::printSRAI);
        instrHandlers.put(SLT.class, this::printSLT);
        instrHandlers.put(SLTI.class, this::printSLTI);
        instrHandlers.put(SEQZ.class, this::printSEQZ);
        instrHandlers.put(SNEZ.class, this::printSNEZ);
        instrHandlers.put(BNEZ.class, this::printBNEZ);
        instrHandlers.put(J.class, this::printJ);
        instrHandlers.put(CALL.class, this::printCALL);
        instrHandlers.put(RET.class, this::printRET);
        instrHandlers.put(Init.class, this::printInit);
        instrHandlers.put(Restore.class, this::printRestore);
        instrHandlers.put(CallerSave.class, this::printCallerSave);
        instrHandlers.put(CallerRestore.class, this::printCallerRestore);
    }


    public void print() {
        print(asmProgram.sectionText);
        print(asmProgram.sectionData);
        print(asmProgram.sectionRodata);
    }

    public void print(Section section) {
        printOut(" .section ", section.sectionName, "\n");
        section.globalList.forEach(global -> printOut(" .globl ", global, "\n"));
        section.asmInstrList.forEach(list -> list.forEach(this::print));
        section.wordList.forEach(word -> printOut(word.varName, ":\n", "  .word ", Integer.toString(word.value), "\n"));
        section.constStringList.forEach(constString -> printOut(constString.varName, ":\n", "  .asciz ", constString.value, "\n"));
        printOut("\n");
    }

    public void print(ASMInstr asmInstr) {
        instrHandlers.get(asmInstr.getClass()).accept(asmInstr);
    }


    public void printLABEL(ASMInstr asmInstr) {
        var label = (LABEL) asmInstr;
        printOut(label.label, ":\n");
    }

    public void printLI(ASMInstr asmInstr) {
        var li = (LI) asmInstr;
        printOut("  li ", li.to, " ", Integer.toString(li.imme), "\n");
    }

    public void printLW(ASMInstr asmInstr) {
        var lw = (LW) asmInstr;
        if (lw.offset == -1) {
            printOut("  lw ", lw.to, " ", lw.from, "\n");
        } else {
            printOut("  lw ", lw.to, " ", Integer.toString(lw.offset), "(", lw.from, ")\n");
        }
    }

    public void printLA(ASMInstr asmInstr) {
        var la = (LA) asmInstr;
        printOut("  la ", la.to, " ", la.fromLabel, "\n");
    }

    public void printSW(ASMInstr asmInstr) {
        var sw = (SW) asmInstr;
        printOut("  sw ", sw.from, " ", Integer.toString(sw.offset), "(", sw.to, ")\n");
    }

    public void printMV(ASMInstr asmInstr) {
        var mv = (MV) asmInstr;
        printOut("  mv ", mv.to, " ", mv.from, "\n");
    }

    public void printADDI(ASMInstr asmInstr) {
        var addi = (ADDI) asmInstr;
        printOut("  addi ", addi.to, " ", addi.from, " ", Integer.toString(addi.imme), "\n");
    }

    public void printADD(ASMInstr asmInstr) {
        var add = (ADD) asmInstr;
        printOut("  add ", add.to, " ", add.lhs, " ", add.rhs, "\n");
    }

    public void printSUB(ASMInstr asmInstr) {
        var sub = (SUB) asmInstr;
        printOut("  sub ", sub.to, " ", sub.lhs, " ", sub.rhs, "\n");
    }

    public void printMUL(ASMInstr asmInstr) {
        var mul = (MUL) asmInstr;
        printOut("  mul ", mul.to, " ", mul.lhs, " ", mul.rhs, "\n");
    }

    public void printDIV(ASMInstr asmInstr) {
        var div = (DIV) asmInstr;
        printOut("  div ", div.to, " ", div.lhs, " ", div.rhs, "\n");
    }

    public void printREM(ASMInstr asmInstr) {
        var rem = (REM) asmInstr;
        printOut("  rem ", rem.to, " ", rem.lhs, " ", rem.rhs, "\n");
    }

    public void printAND(ASMInstr asmInstr) {
        var and = (AND) asmInstr;
        printOut("  and ", and.to, " ", and.lhs, " ", and.rhs, "\n");
    }

    public void printANDI(ASMInstr asmInstr) {
        var andi = (ANDI) asmInstr;
        printOut("  andi ", andi.to, " ", andi.from, " ", Integer.toString(andi.imme), "\n");
    }

    public void printOR(ASMInstr asmInstr) {
        var or = (OR) asmInstr;
        printOut("  or ", or.to, " ", or.lhs, " ", or.rhs, "\n");
    }

    public void printORI(ASMInstr asmInstr) {
        var ori = (ORI) asmInstr;
        printOut("  ori ", ori.to, " ", ori.from, " ", Integer.toString(ori.imme), "\n");
    }

    public void printXOR(ASMInstr asmInstr) {
        var xor = (XOR) asmInstr;
        printOut("  xor ", xor.to, " ", xor.lhs, " ", xor.rhs, "\n");
    }

    public void printXORI(ASMInstr asmInstr) {
        var xori = (XORI) asmInstr;
        printOut("  xori ", xori.to, " ", xori.from, " ", Integer.toString(xori.imme), "\n");
    }

    public void printSLL(ASMInstr asmInstr) {
        var sll = (SLL) asmInstr;
        printOut("  sll ", sll.to, " ", sll.lhs, " ", sll.rhs, "\n");
    }

    public void printSLLI(ASMInstr asmInstr) {
        var slli = (SLLI) asmInstr;
        printOut("  slli ", slli.to, " ", slli.from, " ", Integer.toString(slli.imme), "\n");
    }

    public void printSRA(ASMInstr asmInstr) {
        var sra = (SRA) asmInstr;
        printOut("  sra ", sra.to, " ", sra.lhs, " ", sra.rhs, "\n");
    }

    public void printSRAI(ASMInstr asmInstr) {
        var srai = (SRAI) asmInstr;
        printOut("  srai ", srai.to, " ", srai.from, " ", Integer.toString(srai.imme), "\n");
    }

    public void printSLT(ASMInstr asmInstr) {
        var slt = (SLT) asmInstr;
        printOut("  slt ", slt.to, " ", slt.lhs, " ", slt.rhs, "\n");
    }

    public void printSLTI(ASMInstr asmInstr) {
        var slti = (SLTI) asmInstr;
        printOut("  slti ", slti.to, " ", slti.from, " ", Integer.toString(slti.imme), "\n");
    }

    public void printSEQZ(ASMInstr asmInstr) {
        var seqz = (SEQZ) asmInstr;
        printOut("  seqz ", seqz.to, " ", seqz.from, "\n");
    }

    public void printSNEZ(ASMInstr asmInstr) {
        var snez = (SNEZ) asmInstr;
        printOut("  snez ", snez.to, " ", snez.from, "\n");
    }

    public void printBNEZ(ASMInstr asmInstr) {
        var bnez = (BNEZ) asmInstr;
        printOut("  bnez ", bnez.condition, " ", bnez.toLabel, "\n");
    }

    public void printJ(ASMInstr asmInstr) {
        var j = (J) asmInstr;
        printOut("  j ", j.toLabel, "\n");
    }

    public void printCALL(ASMInstr asmInstr) {
        var call = (CALL) asmInstr;
        printOut("  call ", call.func, "\n");
    }

    public void printInit(ASMInstr asmInstr) {
        var init = (Init) asmInstr;
        init.initList.forEach(this::print);
    }

    public void printRestore(ASMInstr asmInstr) {
        var restore = (Restore) asmInstr;
        restore.restoreList.forEach(this::print);
    }

    public void printCallerSave(ASMInstr asmInstr) {
        var callerSave = (CallerSave) asmInstr;
        callerSave.callerList.forEach(this::print);
    }

    public void printCallerRestore(ASMInstr asmInstr) {
        var callerRestore = (CallerRestore) asmInstr;
        callerRestore.callerList.forEach(this::print);
    }

    public void printRET(ASMInstr asmInstr) {
        printOut("  ret\n");
    }

    public void printOut(String... elements) {
        for (String ele : elements) {
            System.out.print(ele);
        }
    }
}
