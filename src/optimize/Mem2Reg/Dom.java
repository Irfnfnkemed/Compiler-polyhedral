package src.optimize.Mem2Reg;

import java.util.*;

public class Dom {
    private int cnt = 0;

    public class DomInfo {//节点的支配集、直接支配节点和支配边界
        public String blockName;
        public DomInfo immeDom;//直接支配节点
        public DomInfo semiDom;//半支配节点
        public HashSet<String> domFrontier;//支配边界
        public Stack<DomInfo> semiBucket;//被当前节点半支配的节点
        public int dfn = -1;
        public int minDfn = -1;//当前到半支配节点上，sdom的dfn最小的点的dfn
        public DomInfo dfsFather;

        public DomInfo(String blockName_, DomInfo dfsFather_) {
            blockName = blockName_;
            semiBucket = new Stack<>();
            domFrontier = new HashSet<>();
            dfsFather = dfsFather_;
            semiDom = this;//半支配节点设为自身
            dfn = cnt++;
            dfnList.add(this);
        }
    }

    public HashMap<String, DomInfo> domMap;
    public List<DomInfo> dfnList;//按照dfn从小到大排列
    public int[] fatherDSU;//dfn->并查集中的fa
    public int[] minSdomDfn;//dfn->(对应节点 到 逆dfn序遍历dfs树过程中当前遍历到所有点的LCA 的路径上，sdom的dfn最小的点的dfn)
    public String entryLabel;
    public CFGDom cfgDom;

    public Dom(CFGDom cfgDom_) {
        this(cfgDom_, "entry");
    }

    public Dom(CFGDom cfgDom_, String entryLabel_) {
        cfgDom = cfgDom_;
        if (cfgDom.funcBlockDoms.containsKey(entryLabel_)) {
            entryLabel = entryLabel_;
        } else {
            for (var entry : cfgDom.funcBlockDoms.entrySet()) {
                if (entry.getValue().pre == 0) {
                    entryLabel = entry.getKey();
                    cfgDom.retLabel = entryLabel;
                }
            }
        }
        domMap = new HashMap<>();
        dfnList = new ArrayList<>();
        DFS();
        int size = dfnList.size();
        fatherDSU = new int[size];
        minSdomDfn = new int[size];
        Arrays.fill(fatherDSU, -1);
        for (int i = 0; i < size; ++i) {
            minSdomDfn[i] = i;
        }
        LengauerTarjan();
        buildDomFrontier();
    }


    public void DFS() {
        DomInfo rootDom = new DomInfo(entryLabel, null);
        domMap.put(rootDom.blockName, rootDom);
        DFS(rootDom);
    }

    public void DFS(DomInfo domInfo) {
        BlockDom blockDom = cfgDom.funcBlockDoms.get(domInfo.blockName);
        for (int i = 0; i < blockDom.suc; ++i) {
            if (!domMap.containsKey(blockDom.next.get(i).block.label)) {
                DomInfo nextDomInfo = new DomInfo(blockDom.next.get(i).block.label, domInfo);
                domMap.put(blockDom.next.get(i).block.label, nextDomInfo);
                DFS(nextDomInfo);
            }
        }
    }

    public int find(int now) {
        if (fatherDSU[now] == -1) {
            return now;
        }
        //minSdomDfn[now] = min(minSdomDfn[now], dfnList.get(now).semiDom.dfn);
        int tmp = fatherDSU[now];
        fatherDSU[now] = find(fatherDSU[now]);
        if (dfnList.get(minSdomDfn[tmp]).semiDom.dfn < dfnList.get(minSdomDfn[now]).semiDom.dfn) {
            minSdomDfn[now] = minSdomDfn[tmp];
        }
        if (dfnList.get(tmp).semiDom.dfn < dfnList.get(minSdomDfn[now]).semiDom.dfn) {
            minSdomDfn[now] = tmp;
        }
        return fatherDSU[now];
    }

    public void LengauerTarjan() {
        BlockDom nowBlockDom;
        DomInfo nowDom, tmpDom;
        for (int i = dfnList.size() - 1; i > 0; --i) {//逆dfn序
            nowDom = dfnList.get(i);
            nowBlockDom = cfgDom.funcBlockDoms.get(nowDom.blockName);
            for (var preBlock : nowBlockDom.prev) {//求半支配节点
                tmpDom = domMap.get(preBlock.block.label);
                if (tmpDom.dfn < nowDom.dfn) {
                    if (tmpDom.semiDom.dfn < nowDom.semiDom.dfn) {
                        nowDom.semiDom = tmpDom.semiDom;
                    }
                } else {
                    find(tmpDom.dfn);
                    if (dfnList.get(minSdomDfn[tmpDom.dfn]).semiDom.dfn < nowDom.semiDom.dfn) {
                        nowDom.semiDom = dfnList.get(minSdomDfn[tmpDom.dfn]).semiDom;
                    }
                }
            }
            (nowDom.semiDom).semiBucket.push(nowDom);
            while (!nowDom.dfsFather.semiBucket.isEmpty()) {
                tmpDom = nowDom.dfsFather.semiBucket.pop();
                find(tmpDom.dfn);
                tmpDom.minDfn = minSdomDfn[tmpDom.dfn];
            }
            fatherDSU[nowDom.dfn] = nowDom.dfsFather.dfn;
        }
        for (int i = 1; i < dfnList.size(); ++i) {
            nowDom = dfnList.get(i);
            if (dfnList.get(nowDom.minDfn).semiDom.dfn == nowDom.semiDom.dfn) {
                nowDom.immeDom = nowDom.semiDom;
            } else {
                nowDom.immeDom = dfnList.get(nowDom.minDfn).immeDom;
            }
        }
    }

    private void buildDomFrontier() {
        BlockDom nowBlockDom;
        DomInfo domInfoPre, domInfoNow;
        for (var entry : domMap.entrySet()) {
            nowBlockDom = cfgDom.funcBlockDoms.get(entry.getKey());
            domInfoNow = domMap.get(entry.getKey());
            if (nowBlockDom.pre > 1) {//汇合点
                for (var preBlock : nowBlockDom.prev) {
                    domInfoPre = domMap.get(preBlock.block.label);
                    while (domInfoPre != entry.getValue().immeDom) {
                        domMap.get(domInfoPre.blockName).domFrontier.add(domInfoNow.blockName);
                        domInfoPre = domInfoPre.immeDom;
                    }
                }
            }
        }
    }
}
