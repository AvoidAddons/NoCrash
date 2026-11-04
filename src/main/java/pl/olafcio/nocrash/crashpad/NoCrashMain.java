package pl.olafcio.nocrash.crashpad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class NoCrashMain {
    public static void main(String[] args) {
        IO.println("NoCrash has started the crashpad");
        IO.println("");
        IO.println("[Main/DEBUG] resolving processhandle information");

        ProcessHandle parent = null;
        long parentPID = -1001;

        try {
            var self = ProcessHandle.current();

            parent = self.parent().orElseThrow(() -> new RuntimeException("self.parent() is null"));
            parentPID = parent == null ? -1000 : parent.pid();
        } catch (Throwable e) {
            IO.println("[Main/ERROR] unable to fetch parent information; did Minecraft crash at startup?");
        }

        IO.println("|---------------------|----------------|");
        IO.println("| Parent process PID  | %5s %s %-5d |".formatted(args[0], String.valueOf(parentPID).equals(args[0]) ? "==" : "!=", parentPID));
        IO.println("| Underlying platform | %-14s |".formatted(args[1]));
        IO.println("| Avoid distribution  | %-14s |".formatted(args[2]));
        IO.println("| Avoid version       | %-14s |".formatted(args[3]));
        IO.println("|---------------------|----------------|");

        State.modloader = args[1];

        IO.println("");

        try (var stream = NoCrashMain.class.getResourceAsStream("/nocrash_version.txt")) {
            var version = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            IO.println("---- Running NoCrash %s ----".formatted(version));

            State.nocrash = version;
        } catch (IOException e) {
            IO.println("[Main/ERROR] failed to resolve nocrash version");
        }

        IO.println("");
        IO.println("[Main/DEBUG] resolving host information");

        IO.println("|---------------------|------------|");
        IO.println("| Operating system    | %10s |".formatted(System.getProperty("os.name")));
        IO.println("| Java version        | %10s |".formatted(System.getProperty("java.version")));
        IO.println("|---------------------|------------|");

        IO.println("");

        if (parent == null) {
            IO.println("[Main/INFO] crash detected");
            IO.println("            opcode: 1");
            IO.println("");

            crash();
        } else {
            IO.println("[Main/DEBUG] starting crash polling");

            while (true) {
                if (!parent.isAlive()) {
                    IO.println("[Main/INFO] crash detected");
                    IO.println("            opcode: 2");
                    IO.println("");

                    crash();

                    break;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    IO.println("[Main/INFO] crash detected");
                    IO.println("            opcode: 0");
                    IO.println("");

                    crash();

                    break;
                }
            }
        }
    }

    public static void crash() {
        new NoCrashAction();
    }
}
