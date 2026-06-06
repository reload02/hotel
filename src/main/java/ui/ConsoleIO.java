package ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ConsoleIO {
    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleIO(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public void println(String text) {
        out.println(text);
    }

    public String prompt(String text) {
        out.print(text);
        return scanner.nextLine();
    }
}
