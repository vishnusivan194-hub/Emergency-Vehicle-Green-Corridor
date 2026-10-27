import java.util.*;

 
class Input {
    private static final Scanner in = new Scanner(System.in);

    static String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) { System.out.println("\n(input closed - exiting)"); System.exit(0); }
        return in.nextLine().trim();
    }

     
    static int parseInt(String text, int min, int max) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Input cannot be empty.");
        int v;
        try { v = Integer.parseInt(text.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("'" + text.trim() + "' is not a whole number."); }
        if (v < min || v > max) throw new IllegalArgumentException("Value must be between " + min + " and " + max + " (you typed " + v + ").");
        return v;
    }
    static double parseDouble(String text, double min, double max) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Input cannot be empty.");
        double v;
        try { v = Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("'" + text.trim() + "' is not a number."); }
        if (Double.isNaN(v) || v < min || v > max) throw new IllegalArgumentException("Value must be between " + min + " and " + max + ".");
        return v;
    }

     
    static int askInt(String prompt, int min, int max, Integer defaultValue) {
        while (true) {
            String s = ask(prompt);
            if (s.isEmpty() && defaultValue != null) return defaultValue;
            try { return parseInt(s, min, max); } catch (IllegalArgumentException e) { System.out.println("  ! " + e.getMessage()); }
        }
    }
    static double askDouble(String prompt, double min, double max) {
        while (true) {
            try { return parseDouble(ask(prompt), min, max); } catch (IllegalArgumentException e) { System.out.println("  ! " + e.getMessage()); }
        }
    }
    static boolean askYes(String prompt) { return ask(prompt + " (y/n): ").toLowerCase().startsWith("y"); }
}
