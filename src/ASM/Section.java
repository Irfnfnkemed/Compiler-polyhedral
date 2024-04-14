package src.ASM;

import src.ASM.instruction.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Section {
    public static class word {
        public String varName;
        public int value;

        public word(String varName_, int value_) {
            varName = varName_;
            value = value_;
        }
    }

    public static class constString {
        public String varName, value;

        public constString(String varName_, String value_) {
            varName = varName_;
            value = value_;
        }
    }

    public String sectionName;
    public List<String> globalList;
    public List<List<ASMInstr>> asmInstrList;
    public List<word> wordList;
    public List<constString> constStringList;

    public String nowFuncName;

    public Section(String sectionName_) {
        sectionName = sectionName_;
        globalList = new ArrayList<>();
        asmInstrList = new ArrayList<>();
        wordList = new ArrayList<>();
        constStringList = new ArrayList<>();
    }

    public void pushGlobal(String name) {
        globalList.add(name);
    }

    public void pushInstr(ASMInstr asmInstr) {
        if (asmInstr instanceof LABEL) {
            if (!Objects.equals(((LABEL) asmInstr).label, nowFuncName)) {
                ((LABEL) asmInstr).label = "." + nowFuncName + "-" + ((LABEL) asmInstr).label;
            } else {
                asmInstrList.add(new ArrayList<>());
            }
        }
        if (asmInstr instanceof J) {
            if (!Objects.equals(((J) asmInstr).toLabel, nowFuncName)) {
                ((J) asmInstr).toLabel = "." + nowFuncName + "-" + ((J) asmInstr).toLabel;
            }
        }
        if (asmInstr instanceof BNEZ) {
            if (!Objects.equals(((BNEZ) asmInstr).toLabel, nowFuncName)) {
                ((BNEZ) asmInstr).toLabel = "." + nowFuncName + "-" + ((BNEZ) asmInstr).toLabel;
            }
        }
        if (asmInstr instanceof MV mv) {
            if (Objects.equals(mv.from, mv.to)) {
                return;
            }
        }
        asmInstrList.get(asmInstrList.size() - 1).add(asmInstr);
    }

    public void pushWord(String varName, int value) {
        wordList.add(new word(varName, value));
    }

    public void pushConstString(String varName, String value) {
        constStringList.add(new constString(varName, value));
    }
}
