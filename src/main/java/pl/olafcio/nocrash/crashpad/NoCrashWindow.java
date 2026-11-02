package pl.olafcio.nocrash.crashpad;

import pl.olafcio.nocrash.crashpad.message.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class NoCrashWindow {
    private final Thread thread;

    public NoCrashWindow() {
        this.thread = new Thread(this::show, "NoCrash/GUI");
        this.thread.setDaemon(false);
        this.thread.start();
    }

    private final Label status
            = new Label("Initializing");

    public void setState(String state) {
        this.status.setText(state);
    }

    public void setProgress(float percentage) {
        // TODO
    }

    private transient Runnable callback;

    public synchronized void exec(Runnable runnable) {
        if (Thread.currentThread() == thread) {
            runnable.run();
        } else {
            this.callback = runnable;
            this.notify();
        }
    }

    private synchronized void show() {
        var frame = new JFrame();
        status.setBounds(5, 5, 100, 15);
        frame.add(status);

        var button = new JButton("GFG WebSite Click");

        button.setBounds(150, 200, 220, 50);

        frame.add(button);
        frame.setSize(500, 600);
        frame.setLayout(null);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                IO.println("[NoCrashWindow/INFO] Closing");

                e.getWindow().dispose();
                thread.interrupt();
            }
        });

        while (true) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                return;
            }

            if (callback != null) {
                callback.run();
                callback = null;
            }
        }
    }

    public void addMessage(Message message) {
        // TODO
    }
}
