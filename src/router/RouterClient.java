/**
 * RouterClient
 * Copyright (c) 2011, Alex Westphal. All rights reserved
 * @author Alex Westphal
 * @version 18-Sep-2011
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import router.Arguments.ArgumentException;
import router.logging.ConsoleLogger;
import router.logging.Logger;

/**
 * <code>RouterClient</code> provides the client application for connecting to routers.
 */
public class RouterClient {
    
    private SocketChannel channel;
    private boolean stop = false;
    
    
    
    /**
     * Run the router client
     * @param args The command line arguments
     */
    public static void main(String[] args) throws IOException {
        Arguments arguments;
        try {
            arguments = Arguments.parse(args);
        } catch (ArgumentException ex) {
            System.out.println("Error: "+ex.getMessage());
            return;
        }
        
        Logger log = new ConsoleLogger();
        ConfigurationParser configParser = new ConfigurationParser(log);
        Configuration config;
        int port = -1;
        
        if(arguments.hasPort()) port = arguments.getPort();
        else if(arguments.hasConfig()) {
             config = configParser.getConfiguration(arguments.getConfig());
            if(config.getAdminPorts().length > 0) port = config.getAdminPorts()[0];
        } else if(arguments.hasRouterId()) {
            config = configParser.getConfiguration(arguments.getRouterId());
            if(config.getAdminPorts().length > 0) port = config.getAdminPorts()[0];
        }
        if(-1 == port) System.out.println("No usable port number found");
        
        else new RouterClient().run(port);
        
    }
    
    
    /**
     * Create a <code>RouterClient</code>
     * @throws IOException 
     */
    public RouterClient() throws IOException {
        channel = SocketChannel.open();
        
    }
    
    
    /**
     * Run the client
     * @param port The port to connect to
     * @throws IOException 
     */
    public void run(int port) throws IOException {
        
        channel.connect(new InetSocketAddress("localhost", port));
        
        ReadThread read = new ReadThread();
        WriteThread write = new WriteThread();
        
        read.start();
        write.start();
        
        try {
            read.join();
            write.join();
        } catch(InterruptedException ex) {}
        
        
    }
    
    
    
    /**
     * Close the socket
     */
    public synchronized void close() {
        if(stop) {
            System.out.println("Closing Connection...");
            
            try {
                channel.close();
            } catch(IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            stop = true;
        }
    }
    
    
    
    /**
     * The <code>WriteThread</code> class reads line from stdin and writes them 
     * to the socket.
     */
    private class WriteThread extends Thread {
        

        
        /**
         * Run the <code>Thread</code>
         */
        @Override
        public void run() {
            
            ByteBuffer buffer;
            Scanner scanner = new Scanner(System.in);
            
            System.out.print(">>");
            while(scanner.hasNextLine()) {
                synchronized(RouterClient.this) { if(stop) break; }
                
                String line = scanner.nextLine();
                
                buffer = ByteBuffer.wrap(line.getBytes());
                try {
                    channel.write(buffer);
                } catch(IOException ex) {
                    throw new RuntimeException(ex);
                }
                
                if(line.equals("exit") || line.equals("shutdown")) break;
            }
            
            RouterClient.this.close();
        }
    }
    
    /**
     * The <code>ReadThread</code> class reads data from the socket and writes 
     * it to stdout.
     */
    private class ReadThread extends Thread {
        
        
        
        /**
         * Run the <code>Thread</code>
         */
        @Override
        public void run() {
            
            int numRead;
            ByteBuffer buffer = ByteBuffer.allocate(128);
            byte[] bytes;
            
            while(true) {
                synchronized(RouterClient.this) { if(stop) break; }
                
                buffer.clear();
                
                try {
                    numRead = channel.read(buffer);
                } catch (IOException ex) { // Unclean shutdown
                    break;
                }
                
                if(numRead > 0) {
                    bytes = new byte[numRead];
                    System.arraycopy(buffer.array(), 0, bytes, 0, numRead);
                    System.out.println(new String(bytes));
                    System.out.print(">>");
                    
                } else if(numRead == -1) { //Clean shutdown
                    break;
                }
            }
            
            RouterClient.this.close();
            
        }
    }
}
