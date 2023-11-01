import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Matching {
    private int groupSize;
    private int[][] prefs;
    private int iterCount;
    private int finalIterCount;
    private int numMembers;
    private int numGroups;
    private List<Integer> ungrouped;
    private List<Group> unfilled;
    private List<Group> filled;

    public Matching(int[][] prefs, int groupSize, int iterCount, int finalIterCount) {
        this.groupSize = groupSize;
        this.prefs = prefs;
        this.iterCount = iterCount;
        this.finalIterCount = finalIterCount;
        this.numMembers = prefs.length;
        this.numGroups = (int) Math.ceil((double) numMembers / groupSize);
        this.ungrouped = new ArrayList<>();
        for (int i = 0; i < numMembers; i++) {
            this.ungrouped.add(i);
        }
        this.unfilled = new ArrayList<>();
        this.filled = new ArrayList<>();
        Random random = new Random();
        for (int i : random.ints(0, numMembers).distinct().limit(numGroups).toArray()) {
            Group group = new Group(this, new ArrayList<>(Arrays.asList(i)));
            this.unfilled.add(group);
            this.ungrouped.remove((Integer) i);
        }
    }

    public static Matching fromCsv(String file_path, int r) {
        // You'll need to implement file reading and parsing in Java
        // and pass the preferences as a 2D array (prefs) to the constructor.
        int[][] prefs = {}; // Parse CSV data
        return new Matching(prefs, r, 2, 4);
    }

    public double getMemPrefForGroup(int mem, List<Integer> grp) {
        double pref = 0;
        for (int i : grp) {
            pref += prefs[mem][i];
        }
        pref = pref * (1.0 / grp.size());
        return pref;
    }

    public double getGroupPrefForMem(int mem, List<Integer> grp) {
        double pref = 0;
        for (int i : grp) {
            pref += prefs[i][mem];
        }
        pref = pref * (1.0 / grp.size());
        return pref;
    }

    public double getGroupScore(List<Integer> y) {
        if (y.size() <= 1) {
            return 0;
        }
        double score = 0;
        for (int i : y) {
            for (int j : y) {
                if (i != j) {
                    score += prefs[i][j];
                }
            }
        }
        score = score * (1.0 / (y.size() * y.size() - y.size()));
        return score;
    }

    public double getNetScore() {
        double score = 0;
        for (Group group : filled) {
            score += getGroupScore(group.getMembers());
        }
        return score / numGroups;
    }

    public List<Object> solve() {
        while (!ungrouped.isEmpty()) {
            addOneMember();
        }

        filled.addAll(unfilled);
        unfilled.clear();
        optimize(true);
        List<List<Integer>> grps = new ArrayList<>();
        for (Group group : filled) {
            grps.add(group.getMembers());
        }
        List<Object> result = new ArrayList<>();
        result.add(getNetScore());
        result.add(grps);
        return result;
    }

    public void optimize(boolean useFilled) {
        List<Group> grps = useFilled ? filled : unfilled;
        int iters = useFilled ? finalIterCount : iterCount;

        for (int a = 0; a < iters; a++) {
            for (Group grp1 : grps) {
                for (int mem1 : new ArrayList<>(grp1.getMembers())) {
                    if (mem1 == -1) {
                        break;
                    }
                    if (grp1 == grp2) {
                        continue;
                    }
                    for (Group grp2 : grps) {
                        if (mem1 == -1) {
                            break;
                        }
                        if (grp2 == grp1) {
                            continue;
                        }
                        for (int mem2 : new ArrayList<>(grp2.getMembers())) {
                            if (mem1 == -1) {
                                break;
                            }
                            if (mem2 == mem1) {
                                continue;
                            }
                            List<Integer> grp2mem1 = new ArrayList<>(grp2.getMembers());
                            grp2mem1.remove((Integer) mem2);
                            grp2mem1.add(mem1);
                            List<Integer> grp1mem2 = new ArrayList<>(grp1.getMembers());
                            grp1mem2.remove((Integer) mem1);
                            grp1mem2.add(mem2);

                            double grpOneNewScore = getGroupScore(grp1mem2);
                            double grpTwoNewScore = getGroupScore(grp2mem1);

                            if (grpOneNewScore + grpTwoNewScore > getGroupScore(grp1.getMembers()) + getGroupScore(grp2.getMembers())) {
                                grp1.addMember(mem2);
                                grp1.removeMember(mem1);
                                grp2.addMember(mem1);
                                grp2.removeMember(mem2);
                                mem1 = -1;
                            }
                        }
                    }
                }
            }
        }
    }

    public void addOneMember() {
        boolean[][] proposed = new boolean[ungrouped.size()][unfilled.size()];
        boolean[] isTempGrouped = new boolean[ungrouped.size()];
        double[][] tempPref = new double[ungrouped.size()][unfilled.size()];
        int[][] tempPrefOrder = new int[ungrouped.size()][unfilled.size()];

        for (int i = 0; i < ungrouped.size(); i++) {
            for (int j = 0; j < unfilled.size(); j++) {
                tempPref[i][j] = getMemPrefForGroup(ungrouped.get(i), unfilled.get(j).getMembers());
            }
        }

        for (int i = 0; i < ungrouped.size(); i++) {
            int[] indices = new int[unfilled.size()];
            for (int j = 0; j < unfilled.size(); j++) {
                indices[j] = j;
            }
            Arrays.sort(indices, (a, b) -> Double.compare(tempPref[i][b], tempPref[i][a]));
            tempPrefOrder[i] = indices;
        }

        while (Arrays.stream(isTempGrouped).filter(g -> !g).count() != 0) {
            for (int i = 0; i < ungrouped.size(); i++) {
                if (isTempGrouped[i]) {
                    continue;
                }
                if (Arrays.stream(proposed[i]).filter(p -> !p).count() == 0) {
                    isTempGrouped[i] = true;
                    continue;
                }
                for (int j : tempPrefOrder[i]) {
                    if (proposed[i][j]) {
                        continue;
                    }
                    Group grp = unfilled.get(j);
                    proposed[i][j] = true;
                    double pref = getGroupPrefForMem(ungrouped.get(i), grp.getMembers());
                    if (pref > grp.getTempScore()) {
                        if (grp.getTempMember() >= 0) {
                            isTempGrouped[ungrouped.indexOf(grp.getTempMember())] = false;
                        }
                        grp.addTemp(ungrouped.get(i));
                        isTempGrouped[i] = true;
                        break;
                    }
                }
            }
        }

        for (Group grp : unfilled) {
            if (grp.getTempMember() < 0) {
                continue;
            }
            ungrouped.remove((Integer) grp.getTempMember());
            grp.addPermanently();
        }

        optimize(false);

        for (Group grp : unfilled) {
            if (grp.size() >= groupSize || ungrouped.isEmpty()) {
                filled.add(grp);
            }
        }

        for (Group grp : filled) {
            unfilled.remove(grp);
        }
    }
}

class Group {
    private Matching game;
    private List<Integer> members;
    private int tempMember;
    private double tempScore;

    public Group(Matching game, List<Integer> members) {
        this.game = game;
        this.members = members;
        this.tempMember = -1;
        this.tempScore = -1;
    }

    public void addMember(int x) {
        members.add(x);
    }

    public void removeMember(int x) {
        members.remove((Integer) x);
    }

    public double addTemp(int x) {
        tempMember = x;
        tempScore = game.getGroupPrefForMem(x, members);
        return tempScore;
    }

    public void addPermanently() {
        if (tempMember == -1) {
            return;
        }
        addMember(tempMember);
        tempMember = -1;
        tempScore = -1;
    }

    public int size() {
        return members.size();
    }

    public List<Integer> getMembers() {
        return members;
    }

    public int getTempMember() {
        return tempMember;
    }

    public double getTempScore() {
        return tempScore;
    }
}
