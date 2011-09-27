/**
 * RIPService
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
package router.rip;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import router.DatagramService;
import router.DatagramService.Datagram;
import router.Router;
import router.RouterDaemon;
import router.rip.RIPPacket.InvalidRIPPacketException;
import router.rip.RIPPacket.RIPEntry;
import router.RouterService;

/**
 * The <code>RIPService</code> class provides the RIP component of the router.
 */
public class RIPService extends RouterService {

    public static final int TIME_FACTOR = 6;
    
    public static final int ADVERT_INTERVAL         = 30000 /TIME_FACTOR;
    public static final int TIMEOUT_LENGTH          = 180000/TIME_FACTOR;
    public static final int GC_LENGTH               = 120000/TIME_FACTOR;
    public static final int NEIGHBOR_TIMEOUT_LENGTH = 60000 /TIME_FACTOR;
    
    
    private final DatagramService datagramService;
    private final RoutingTable table;
    protected final Timer timer = new Timer();
    
    
    
    /**
     * Create a <code>RIPService</code> with the given router and datagram service.
     * @param router The router to use
     * @param datagramService The datagram service to use
     */
    public RIPService(Router router, DatagramService datagramService) {
        super(router);
        this.table = new RoutingTable(this);
        this.datagramService = datagramService;
    }
    
    
    
    /**
     * Get a <code>String</code> representation of the routing table.
     * @return The <code>String</code> representation
     */
    public String getTableStr() {
        return table.toString();
    }
    
    
    
    /**
     * Get a <code>String</code> representation of the neighbor table.
     * @return The <code>String</code> representation
     */
    public String getNeighborTableStr() {
        return table.getNeighborTableString();
    }
    
    
    /**
     * Send a triggered delete message
     * @param address Address to announce the un-reachability of
     */
    protected void sendDelete(int address) {
        RIPPacket update = new RIPPacket(RIPPacket.COMMAND_RESPONSE, (short) getRouter().getRouterId());
        update.addEntry(new RIPEntry(address, RoutingTable.METRIC_INFINITE));
    }
    
    
    
    /**
     * Start the service by scheduling advertisements and sending out a startup request
     */
    @Override
    public void doStart() {
        timer.scheduleAtFixedRate(new AdvertisementTask(), 3000, ADVERT_INTERVAL);
        
        RIPPacket request = new RIPPacket(RIPPacket.COMMAND_REQUEST, (short) getRouter().getRouterId());
        //datagramService.sendToAll(request.getBytes());
    }
    
    
    
    /**
     * Stop the service by canceling the timer
     */
    @Override
    public void doStop() {
        timer.cancel();
    }
    
    
    
    /**
     * Interrupt the service
     */
    @Override
    public void interrupt() {
        super.interrupt();
    }
    
    
    
    /**
     * Run the service
     */
    @Override
    public void run() {
        try {
            Datagram datagram = datagramService.take();
            int linkMetric = datagramService.getLinkMetric(datagram.getInterface());
            if(null == datagram.getData()) {
                // Link is down (from ICMP)
            } else {
                try {
                    RIPPacket packet = new RIPPacket(datagram.getData());

                    String msg = "Update recieved from router-"+packet.getSource()+" on if"+datagram.getInterface();
                    for(RIPEntry entry: packet) {
                        if(entry.getAddress() == getRouter().getRouterId()) continue;
                        table.updateEntry(entry.getAddress(), entry.getMetric()+ linkMetric, datagram.getInterface(), packet.getSource());
                    }
                    msg += "\nRouting Table" + table.toString();
                    msg += "\nNeighbor Table" + table.getNeighborTableString();
                    if(RouterDaemon.loud) getLog().write(msg);


                } catch (InvalidRIPPacketException ex) {
                    getLog().write("Invalid Packet Dropped ("+ex.getMessage()+")");
                    //Drop Invalid Packet
                }
            }
        } catch (InterruptedException ex) {}
    }
    
    
    
    
    
    /**
     * The <code>AdvertisementTask</code> represents the task of sending out advertisements 
     */
    private class AdvertisementTask extends TimerTask {

        
        
        /**
         * Run the task
         */
        @Override
        public void run() {
            try {
                Thread.sleep((new Random().nextInt(2000)));
            } catch (InterruptedException ex) {}
            
            int routerId = getRouter().getRouterId();
            
            for(int i=0; i<datagramService.getLinkCount(); i++) {
                RIPPacket update = table.toRIPPacket(routerId, i);
                datagramService.send(i, update.getBytes());
            }
        }
    }
    
    
    
    
    
}
