package tileworld.utils;

public class Helpers {
    public static boolean has(int[] intset, int tar, int start){
        for (int i=start; i<intset.length; i++){
            if (tar==intset[i]) return true;
        }
        return false;
    }
}