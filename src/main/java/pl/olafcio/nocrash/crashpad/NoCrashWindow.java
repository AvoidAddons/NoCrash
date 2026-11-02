package pl.olafcio.nocrash.crashpad;

import org.jetbrains.annotations.NotNull;
import pl.olafcio.nocrash.crashpad.message.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public final class NoCrashWindow {
    private final Thread thread;

    public NoCrashWindow() {
        this.thread = new Thread(this::show, "NoCrash/GUI");
        this.thread.setDaemon(false);
        this.thread.start();
    }

    private final JLabel status
            = new JLabel("Initializing");

    private final JPanel progress
            = new JPanel();

    private final JPanel messages
            = new JPanel();

    public void setState(String state) {
        this.status.setText(state);
        this.progress.setBounds(0, 0, 0, 30);
    }

    public void setProgress(float percentage) {
        this.progress.setBounds(0, 0, (int) (500 * percentage), 30);
    }

    private final ArrayBlockingQueue<Runnable> callback
            = new ArrayBlockingQueue<>(16);

    public void exec(Runnable runnable) {
        if (Thread.currentThread() == thread) {
            runnable.run();
        } else {
            this.callback.add(runnable);
        }
    }

    private synchronized void show() {
        var frame = new JFrame();

        status.setBounds(5, 5, 500 - 5*2, 15);
        status.setOpaque(false);

        progress.setBackground(new Color(0x3A5EA1));

        messages.setLayout(new FlowLayout(FlowLayout.LEADING));
        messages.setBounds(5, 30, 500 - 5*2, 600 - 30);

        frame.add(status);
        frame.add(progress);
        frame.add(messages);

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
                callback.take().run();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public void addMessage(Message message) {
        IO.println("[NoCrashWindow/INFO] Showing message: \"%s\"".formatted(message.text()));

        exec(() -> {
            var flex = new JPanel(new FlowLayout(FlowLayout.CENTER));
            var box = new JPanel(new BorderLayout(0, 0));


            // Icon
            var icon = (ImageIcon) UIManager.get("OptionPane.errorIcon", this.messages.getLocale());
                icon = scale(icon, 17, 17);

            // Border
            box.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(5, 6, 3, 3)));

            // Label
            var label = new JLabel(message.text(), icon, SwingConstants.LEFT);
            box.add(label);

            AffineTransform affineTransform = new AffineTransform();
            affineTransform.translate(0, -1.4);
            label.setFont(label.getFont().deriveFont(affineTransform));


            flex.add(box);

            flex.add(addButton(message.autoFix(), "Delete mod", ICON_DELETE));

            this.messages.add(flex);
            this.messages.repaint();
        });
    }

    private static JLabel addButton(Runnable fix, String tooltip, Icon icon) {
        var button = new JLabel(icon);

        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createRaisedSoftBevelBorder()));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                fix.run();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createLoweredSoftBevelBorder()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xB5B1B1)), BorderFactory.createRaisedSoftBevelBorder()));
            }
        });

        button.setToolTipText(tooltip);

        return button;
    }

    @NotNull
    private static ImageIcon scale(ImageIcon icon, int width, int height) {
        var image = icon.getImage(); // transform it
        var newimg = image.getScaledInstance(width, height, Image.SCALE_SMOOTH); // scale it the smooth way

        icon = new ImageIcon(newimg);  // transform it back

        return icon;
    }

    private static byte[] read(String path) {
        try (var stream = NoCrashWindow.class.getResourceAsStream("/" + path)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: '%s'".formatted(path), e);
        }
    }

    private static final Icon ICON_DELETE = scale(new ImageIcon(read("icons/delete.png")), 20, 20);
}
