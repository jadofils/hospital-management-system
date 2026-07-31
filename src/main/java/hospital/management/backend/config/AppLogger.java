package hospital.management.backend.config;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AppLogger {
    private final Logger logger;

    private AppLogger(Class<?> clazz) {
        this.logger = Logger.getLogger(clazz.getName());
    }

    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warning(message);
    }

    public void error(String message) {
        logger.severe(message);
    }

    public void debug(String message) {
        logger.fine(message);
    }

    public void trace(String message) {
        logger.finest(message);
    }

    public void fatal(String message) {
        logger.log(Level.SEVERE, "[FATAL] " + message);
    }

    // With Throwable
    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    public void warn(String message, Throwable t) {
        logger.log(Level.WARNING, message, t);
    }

    public void debug(String message, Throwable t) {
        logger.log(Level.FINE, message, t);
    }

    public void info(String message, Throwable t) {
        logger.log(Level.INFO, message, t);
    }
}
