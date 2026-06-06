import common.FatalDataException;
import persistence.FlatFileStore;
import service.AuthService;
import service.GuestService;
import service.HostService;
import service.IntegrityService;
import service.MonthlySystemService;
import service.PenaltyService;
import service.SuspensionService;
import ui.AppController;
import ui.ConsoleIO;

import java.nio.file.Path;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        Path projectRoot = Path.of("").toAbsolutePath();
        Path dataDir = projectRoot.resolve("data");

        ConsoleIO io = new ConsoleIO(System.in, System.out);
        FlatFileStore store = new FlatFileStore(dataDir);
        PenaltyService penaltyService = new PenaltyService(store);
        SuspensionService suspensionService = new SuspensionService(store);
        MonthlySystemService monthlySystemService = new MonthlySystemService(store, suspensionService);
        GuestService guestService = new GuestService(store, io, penaltyService, suspensionService);
        HostService hostService = new HostService(store, guestService);
        IntegrityService integrityService = new IntegrityService(
                store, io, guestService, penaltyService, suspensionService, monthlySystemService
        );
        AuthService authService = new AuthService(store);
        AppController controller = new AppController(io, store, integrityService, authService, guestService, hostService);

        try {
            controller.run();
        } catch (FatalDataException e) {
            io.println("파일에 잘못된 형식이 존재합니다.");
            io.println(e.getMessage());
        } catch (Exception e) {
            io.println("예상치 못한 오류가 발생했습니다: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
