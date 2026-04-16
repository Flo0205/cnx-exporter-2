package de.uniko.bas.cnx;

public final class LogUtil {

    private LogUtil() { }

    public static void printSeparator() {
        System.out.println("\n====================\n");
    }

    public static <T> void log(Class<T> c, String text) {
        log(c.getSimpleName() + " : " + text);
    }

    public static void log(String text) {
        System.out.println(text);
    }
}
