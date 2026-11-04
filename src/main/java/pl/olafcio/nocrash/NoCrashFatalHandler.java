package pl.olafcio.nocrash;

import pl.olafcio.avoid_loader.interfaces.services.stage.IPlatformFatal;

public class NoCrashFatalHandler implements IPlatformFatal {
    @Override
    public void acknowledge(CrashEvent crashEvent) {
        try {
            crashEvent.shouldDisplay(false);
        } catch (Exception e) {
            // Ok
            return;
        }

        IO.println("[NoCrash] Preventing platform error GUI");
    }
}
