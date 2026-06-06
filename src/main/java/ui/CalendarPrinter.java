package ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Predicate;

public final class CalendarPrinter {
    private CalendarPrinter() {
    }

    public static void print(ConsoleIO io, int year, int month, Predicate<LocalDate> blocked) {
        YearMonth ym = YearMonth.of(year, month);
        io.println(String.format("%04d/%02d", year, month));
        io.println("일\t월\t화\t수\t목\t금\t토");

        LocalDate first = ym.atDay(1);
        int startOffset = sundayBased(first.getDayOfWeek());

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < startOffset; i++) {
            line.append("\t");
        }

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            line.append(day).append(blocked.test(date) ? "(X)" : "(O)");
            if (sundayBased(date.getDayOfWeek()) == 6) {
                io.println(line.toString());
                line.setLength(0);
            } else {
                line.append("\t");
            }
        }
        if (line.length() > 0) {
            io.println(line.toString());
        }
    }

    private static int sundayBased(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() % 7;
    }
}
