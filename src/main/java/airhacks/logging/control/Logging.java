package airhacks.logging.control;

public interface Logging {
    static void info(Object message){
        System.out.println(message);
    }

    static void error(String string) {
        System.err.println(string);
    }
}
