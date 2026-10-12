package com.pds.logistics;

/**
 * Launcher shim needed when running from a fat JAR.
 * Delegates to PDSApplication without extending Application directly,
 * working around the JavaFX module class-path restriction.
 */
public class PDSLauncher {
    public static void main(String[] args) {
        PDSApplication.main(args);
    }
}
