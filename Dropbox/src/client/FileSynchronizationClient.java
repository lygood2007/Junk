package client;

/**
 * Interface: FileSynchronizationClient
 * Description: General interface for a file synchronization client.
 */
interface FileSynchronizationClient {
    void run() throws Exception;
    boolean sync() throws Exception;
}
