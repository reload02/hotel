package domain;

import java.time.YearMonth;

public class SystemState {
    private YearMonth lastCheckedMonth;

    public YearMonth getLastCheckedMonth() {
        return lastCheckedMonth;
    }

    public void setLastCheckedMonth(YearMonth lastCheckedMonth) {
        this.lastCheckedMonth = lastCheckedMonth;
    }
}
