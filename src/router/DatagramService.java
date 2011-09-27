/**
 * DatagramService
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
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import router.Configuration.Link;

/**
 * The <code>DatagramService</code> class provides datagram IO services.
 */
public class DatagramService extends RouterService {

    private Selector socketSelector;
    private final BlockingQueue<Datagram> inboundQueue = new ArrayBlockingQueue<Datagram>(20);
    private final BlockingQueue<Datagram> outboundQueue = new ArrayBlockingQueue<Datagram>(20);
    private final DatagramChannel[] channels;
    private final boolean[] linkStates;
    private final Link[] links;
    
    
    
    /**
     * Create a <code>DatagramService</code> for the specified router.
     * @param router The router for which to create the datagram service
     */
    public DatagramService(Router router) {
        super(router);
        
        this.links = getRouter().getConfig().getLinks();
        this.channels = new DatagramChannel[links.length];
        this.linkStates = new boolean[links.length];
        
    }
    
    
    
    /**
     * Take a datagram from the inbound queue
     * @return Th next datagram in the inbound queue
     * @throws InterruptedException If the thread was interrupted while waiting
     * for the queue to have a next element.
     */
    public Datagram take() throws InterruptedException {
        return inboundQueue.take();
    }
    
    
    
    /**
     * Send the given datagram
     * @param datagram The datagram to send
     */
    public void send(Datagram datagram) {
        if(datagram.iface < 0 || datagram.iface > channels.length) throw new IllegalArgumentException("Invalid interface");
        outboundQueue.add(datagram);
        channels[datagram.iface].keyFor(socketSelector).interestOps(SelectionKey.OP_WRITE);
        socketSelector.wakeup();
        
    }
    
    
    
    /**
     * Send the given data on the specified interface
     * @param iface The interface to send the data on
     * @param data The data to send
     */
    public void send(int iface, byte[] data) {
        send(new Datagram(iface, data));
    }
    
    
    /**
     * Send the given data on all interfaces
     * @param data The data to send
     */
    public void sendToAll(byte[] data) {
        for(int i=0; i<channels.length; i++) send(new Datagram(i, data));
    }
    
    
    /**
     * Get a <code>String</code> representation of the specified link
     * @param iface The link for which to get the representation
     * @return A <code>String</code> representation of the specified link.
     */
    public String getLinkStr(int iface) {
        return links[iface].toString();
    }
    
    
    /**
     * Get the metric of the specified link
     * @param iface The link for which to get the metric
     * @return The metric of the specified link 
     */
    public int getLinkMetric(int iface) {
        if(iface >= channels.length) throw new IllegalArgumentException("Invalid interface");
        return links[iface].getMetric();
    }
    
    
    
    /**
     * Get the number of links present on the router.
     * @return The number of links present
     */
    public int getLinkCount() {
        return linkStates.length;
    }
    
    
    
    /**
     * Check if the specified link is up
     * @param iface The link to check
     * @return <code>true</code> if the link is up, <code>false</code> otherwise.
     */
    public boolean isLinkUp(int iface) {
        return linkStates[iface];
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
        inboundQueue.clear();
        outboundQueue.clear();
        try {
            socketSelector.close();
        } catch (IOException ex) {
            getLog().write("Datagram Selector Close Failed (IOException) - "+ex.getMessage());
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
     * Run the datagram service
     */
    @Override
    public void run() {
        try {
            socketSelector.select(); //Block till a registered channel is ready
        } catch(IOException ex) {
            getLog().write("DatagramChannel Selection Failed (IOException) - "+ex.getMessage());
        }

            // Iterate over the set of keys for which events are available
            Iterator<SelectionKey> selectedKeys = socketSelector.selectedKeys().iterator();
            while (selectedKeys.hasNext()) {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();

                if (!key.isValid()) continue;

                if(key.isReadable()) {
                    this.read(key);
                } else if(key.isWritable()) {
                    this.write(key);
                }
            }
        
    }
    
    
    
    /**
     * Initialize the datagram selector, create the sockets and register them with the selector.
     * @return The created selector
     */
    private Selector initSelector() {
        try {
            Selector selector = SelectorProvider.provider().openSelector();
            
            
            for(int i=0; i<links.length; i++) {
                Link link = links[i];
                
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.socket().bind(new InetSocketAddress("localhost", link.getInputPort()));
                channel.connect(new InetSocketAddress("localhost", link.getOutputPort()));
                
                channel.register(selector, SelectionKey.OP_READ);
                channels[i] = channel;
                getLog().write("Interface created from "+link.getInputPort()+" to "+link.getOutputPort());
            }
            
            
            return selector;
        } catch(IOException ex) {
            getLog().write("Datagram Selector Init Failed (IOException) - "+ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
    
    
    
    /**
     * Read from the channel represented by the specified key 
     * @param key The selection key representing the channel
     */
    private void read(SelectionKey key) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        int idx = indexOf(channel);
        
        ByteBuffer buffer = ByteBuffer.allocate(128);
        buffer.clear();
        
        int numRead = 0;
        try {
            numRead = channel.read(buffer);
            
        } catch(PortUnreachableException ex) { // Link is down
            linkStates[idx] = false;
            inboundQueue.offer(new Datagram(idx, null));
            return;
            
        } catch(IOException ex) {
            getLog().write("DatagramChannel Read Failed (IOException) - "+ex.getMessage());
        }
        
        byte[] bytes = new byte[numRead];
        System.arraycopy(buffer.array(), 0, bytes, 0, numRead);
        
        
        linkStates[idx] = true;
        inboundQueue.offer(new Datagram(idx, bytes));
    }
    
    
    
    /**
     * Write the next datagram in the outbound queue to the channel represented 
     * by the specified key.
     * @param key The selection key representing the channel
     */
    private void write(SelectionKey key) {
        DatagramChannel channel = (DatagramChannel) key.channel();
        Datagram datagram = outboundQueue.poll();
        if(null != datagram) {
            try {
                channel.write(ByteBuffer.wrap(datagram.getData()));
            } catch(IOException ex) {
                getLog().write("DatagramChannel Write Failed (IOException) - "+ex.getMessage());
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }
    
    
    
    /**
     * Get the index of the specified channel
     * @param channel The channel for which to find the index
     * @return The index of the channel, or <code>-1</code> if the channel isn't
     * in the channel list
     */
    private int indexOf(DatagramChannel channel) {
        for(int i=0; i<channels.length; i++) {
            if(channel.equals(channels[i])) {
                return i;
            }
        }
        return -1;
    }
    
    
    
    
    
    /**
     * The <code>Datagram</code> class represents an array of bytes and the 
     * interface it was received on or is to be transmitted on.
     */
    public static class Datagram {
        
        private final int iface;
        private final byte[] data;
        
        
        
        /**
         * Construct a <code>Datagram</code>.
         * (Note: setting data to null is used to indicated the particular interface is down)
         * 
         * @param iface Interface the data was received on or is to be transmitted on
         * @param data Data received or to be transmitted
         */
        public Datagram(int iface, byte[] data) {
            this.iface = iface;
            this.data = data;
        }
        
        
        
        /**
         * Get the interface the data was received on or is to be transmitted on
         * @return The interface
         */
        public int getInterface() { return iface; }
        
        
        
        /**
         * Get the data received or to be transmitted
         * @return The data
         */
        public byte[] getData() { return data; }
        
    }
    
}
