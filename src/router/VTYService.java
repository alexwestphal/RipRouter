/**
 * VTYService
 * Copyright (c) 2011, Alex Westphal. All rights reserved
 * @author Alex Westphal
 * @version 19-Sep-2011
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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import router.logging.ConsoleLogger;
import router.logging.FileLogger;
import router.logging.HybridLogger;
import router.rip.RIPService;

/**
 * <code>RouterVTY</code> class provides the Router VTY services.
 */
public class VTYService extends RouterService {
    
    private Selector socketSelector = null;
    private boolean exit = false;
    
    private final DatagramService ds;
    private final RIPService rip;

    
    
    /**
     * Create a <code>VTYService</code> with the given router, datagram service
     * and RIP service.
     * @param router The router to use
     * @param ds The datagram service to use
     * @param rip The RIP service to use
     */
    public VTYService(Router router, DatagramService ds, RIPService rip) {
        super(router);
        this.ds = ds;
        this.rip = rip;
    }
    
    
    
    /**
     * Start the service by initialising the selector
     */
    @Override
    public void doStart() {
        socketSelector = this.initSelector();
    }
    
    
    
    /**
     * Stop the service by closing the selector
     */
    @Override
    public void doStop() {
        try {
            socketSelector.close();
            socketSelector = null;
        } catch (IOException ex) {
            getLog().write("VTY Close Failed (IOException) - "+ex.getMessage());
        }
    }
    
    
    
    /**
     * Interrupt the service by waking up the selector
     */
    @Override
    public void interrupt() {
        socketSelector.wakeup();
    }
    
    
    
    /**
     * Run the VTYService
     */
    @Override
    public void run() {
        
        try {
            socketSelector.select(); //Block till a registered channel is ready

            // Iterate over the set of keys for which events are available
            Iterator<SelectionKey> selectedKeys = socketSelector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();

                if (!key.isValid()) continue;

                // Check what event is available and deal with it
                if (key.isAcceptable()) {
                    this.accept(key);
                } else if(key.isReadable()) {
                    this.read(key);
                }
            }

        } catch(IOException ex) {
            getLog().write("VTY Selection Failed (IOException) - "+ex.getMessage());
        }

        
    }
    
    
    
    /**
     * Initialize the admin selector, create the sockets and register them with the selector.
     * @return The created selector
     */
    private Selector initSelector() {
        try {
            Selector selector = SelectorProvider.provider().openSelector();
            
            for(int port: getRouter().getConfig().getAdminPorts()) {
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.configureBlocking(false); // Set to non-blocking mode
            
                int retries = 20;
                
                bind: { 
                    while(retries-->=0) try {
                        serverChannel.socket().bind(new InetSocketAddress("localhost", port)); // Bind to port number
                        break bind;
                    } catch(IOException ex) {
                        getLog().write("VTY Socket Bind Failed... Retrying");
                        try { Thread.sleep(500); } catch(InterruptedException ex2) {}
                    }
                    getLog().write("VTY Socket Bind Failed");
                    
                }
            
                serverChannel.register(selector, SelectionKey.OP_ACCEPT); // Register with selector
                getLog().write("VTY Socket Created On Port "+port);
            }
            
            getLog().write("VTY Selector Created");
            return selector;
        } catch(IOException ex) {
            getLog().write("VTY Selector Init Failed (IOException) - "+ex.getMessage());
            throw new RuntimeException(ex);
        }
        
    }
    
    
    
    /**
     * Accept a connection on the channel represented by the specified key
     * @param key The selection key representing the channel
     * @throws IOException If an error occurred while accepting the connection
     */
    private void accept(SelectionKey key) throws IOException {
     
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false); // Set to non-blocking mode
        socketChannel.register(socketSelector, SelectionKey.OP_READ); //register channel with selector
        getLog().write("VTY Socket Accepted On Port: "+socketChannel.socket().getLocalPort());
        
        this.exit = false;
    }
    
    
    
    /**
     * Read from the channel represented by the specified key
     * @param key The selection key representing the channel
     * @throws IOException If an error occurred while reading from the channel
     */
    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        ByteBuffer readBuffer = ByteBuffer.allocate(128);
        readBuffer.clear();
    
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
        } catch (IOException e) { // Unclean shutdown by client
            socketChannel.close();
            key.cancel();
            return;
        }

        if (numRead == -1) { // Clean shutdown by client
            socketChannel.close();
            key.cancel();
            return;
        }
        
        byte[] bytes = new byte[numRead];
        System.arraycopy(readBuffer.array(), 0, bytes, 0, numRead);
        String str = new String(bytes);
        
        
        String res = processCommand(str);
        ByteBuffer writeBuffer = ByteBuffer.wrap(res.getBytes());
        socketChannel.write(writeBuffer);
        
        if(exit) {
            socketChannel.close();
            key.cancel();
        }
    }
    
    
    
    /**
     * Process the given command
     * @param cmd The command to process
     * @return The output text from the execution of the command
     */
    private String processCommand(String cmd) {
        if(cmd.equals("shutdown")) {
            getRouter().stop();
            return "Router Shuting Down...";
        } else if(cmd.equals("exit")) {
            exit = true;
            return "";
        } else if(cmd.startsWith("show ")) {
            cmd = cmd.substring(5);
            if(cmd.equals("name")) {
                return getRouter().getConfig().getRouterName();
            } else if(cmd.equals("if")) {
                String res = "Interfaces:";
                for(int i=0; i<ds.getLinkCount(); i++) {
                    res += String.format("\n  if%d %s : %s", i, ds.getLinkStr(i), ds.isLinkUp(i)?"Up":"Down");
                }
                return res;
            } else if(cmd.equals("table")) {
                return rip.getTableStr();
            } else if(cmd.equals("neighbors")) {
                return rip.getNeighborTableStr();
            } else if(cmd.startsWith("if ")) {
                try {
                    int iface = Integer.parseInt(cmd.substring(3));
                    return String.format("Interface if%d %s : %s",  iface,ds.getLinkStr(iface), ds.isLinkUp(iface)?"Up":"Down");
                } catch(NumberFormatException ex) {
                    return "Unknown show option ("+cmd+")";
                }
            } else {
                return "Unknown show option ("+cmd+")";
            }
            
        } else if(cmd.startsWith("set ")) {
            cmd = cmd.substring(4);
            if(cmd.startsWith("log ")) {
                cmd = cmd.substring(4);
                if(cmd.equals("console")) {
                    getRouter().setLog(new ConsoleLogger(getRouter().getRouterId()));
                    return "Log set to console";
                } else if(cmd.equals("file")) {
                    getRouter().setLog(new FileLogger(getRouter().getRouterId()));
                    return "Log set to file";
                } else if(cmd.equals("console|file") || cmd.equals("file|console")) {
                    ConsoleLogger console = new ConsoleLogger(getRouter().getRouterId());
                    FileLogger file = new FileLogger(getRouter().getRouterId());
                    getRouter().setLog(new HybridLogger<ConsoleLogger, FileLogger>(console, file));
                    return "Log set to console and file";
                } else {
                    return "Unknown set log option ("+cmd+")";
                }
            } else if(cmd.startsWith("loud ")) {
                if(cmd.equals("loud true")) {
                    RouterDaemon.loud = true;
                    return "Loud set to true";
                } else if(cmd.equals("loud false")) {
                    RouterDaemon.loud = false;
                    return "Loud set to false";
                } else {
                    return "Unknown set loud option ("+cmd+")";
                }
            } else {
                return "Unknown set option ("+cmd+")";
            }
            
        } else {
            return "Unknown command ("+cmd+")";
        }
    }

}
