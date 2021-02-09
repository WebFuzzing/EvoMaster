package org.evomaster.client.java.instrumentation;

/**
 * Exception used when we try to force the SUT to stop its background thread left
 * after each test evaluation
 */
public class KillSwitchException extends RuntimeException{
}
