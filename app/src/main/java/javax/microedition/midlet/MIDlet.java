package javax.microedition.midlet;

import java.util.HashMap;

public abstract class MIDlet {
    public static MIDlet instance;
    public static HashMap<String, String> appProperties = new HashMap<>();

    static {
        appProperties.put("MIDlet-Version", "1.24");
        appProperties.put("MIDlet-Name", "Bounce");
        appProperties.put("MIDlet-Vendor", "Nokia");
        appProperties.put("microedition.locale", "xx");
    }

    public MIDlet() {
        instance = this;
    }

    protected abstract void startApp() throws MIDletStateChangeException;
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public void notifyDestroyed() {}
    public void notifyPaused() {}

    public boolean platformRequest(String url) {
        return false;
    }

    public String getAppProperty(String key) {
        String val = appProperties.get(key);
        return val != null ? val : "";
    }
}
