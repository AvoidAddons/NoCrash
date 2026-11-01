package pl.olafcio.nocrash.crashpad.scanner;

import pl.olafcio.nocrash.crashpad.message.Message;

import java.util.regex.Matcher;

public interface IScanner {
    String regex();
    Message message(Matcher matcher);
}
