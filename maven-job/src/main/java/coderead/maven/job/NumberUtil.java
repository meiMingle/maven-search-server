package coderead.maven.job;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class NumberUtil {
    public static boolean isInteger(String str) {
        if (str == null || str.trim().equals("")) {
            return false;
        }
        return str.chars().allMatch(Character::isDigit);
    }

    public static String stringForTime(long timeSec) {
        long seconds = timeSec % 60;
        long minutes = timeSec / 60 % 60;
        long hours = timeSec / 3600;
        return String.format("%s时%s分%s秒", hours, minutes, seconds);
    }

    public static String distanceCurrentTime(long beginTime) {
        return stringForTime((System.currentTimeMillis() - beginTime) / 1000);
    }


}
