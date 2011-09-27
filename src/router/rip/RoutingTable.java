/**
 * RIPPacket
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

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import router.rip.RIPPacket.RIPEntry;


/**
 * The <code>RoutingTable</code> class represents the RIP routing table.
 */
public class RoutingTable {
    
    public static final int METRIC_INFINITE = 16;
    
    private final RIPService service;
    private final Map<Integer, TableEntry> entries;
    private final Map<Integer, Neighbor> neighbors;
    
    
    
    /**
     * Create a <code>RoutingTable</code>.
     * @param service The <code>RoutingService</code> for the table to be associated with
     */
    public RoutingTable(RIPService service) {
        this.service = service;
        entries = new HashMap<Integer, TableEntry>();
        neighbors = new HashMap<Integer, Neighbor>();
    }
    
    
    
    /**
     * Update the routing table with the route defined by the given parameters
     * @param address Destination of the route
     * @param metric Metric of the route
     * @param iface Local interface of the route
     * @param nextHop Next hop router
     */
    public synchronized void updateEntry(int address, int metric, int iface, int nextHop) {
        
        if(neighbors.containsKey(nextHop)) {
            neighbors.get(nextHop).resetTimeout();
        } else {
            neighbors.put(nextHop, new Neighbor(nextHop, iface));
        }
        
        
        TableEntry entry;
        if(entries.containsKey(address)) {
            entry = entries.get(address);
            
            // Unreachable Route
            if(16 == metric && entry.iface == iface && entry.nextHop == nextHop) {
               entry.timerTask.cancel();
               entry.setGCTimer();
               service.sendDelete(address);
               return;
               
            // Worse Route
            } else if(metric > entry.metric) {
                return;
            // Same Route
            } else if(metric == entry.metric && entry.iface == iface && entry.nextHop == nextHop) {
                entry.resetTimeout();
                return;
            } else {
                entry.timerTask.cancel();
            }
        }
        
        //Create new entry if new route or better route
        entry = new TableEntry(address, metric, iface, nextHop);
        entries.put(address, entry);
        entry.resetTimeout();
            
    }
    
    
    
    /**
     * Get a <code>String</code> representation of the neighbors table
     * @return The <code>String</code> representation
     */
    public synchronized String getNeighborTableString() {
        String str =                "\n\t Address | Interface ";
        str +=                      "\n\t---------|-----------";
        for(int address: neighbors.keySet()) {
            str += String.format(   "\n\t   %3d   |   if%3d   ",
                address, neighbors.get(address).iface);
        }
        
        return str;
    }
    
    
    
    /**
     * Convert the <code>RoutingTable</code> to a <code>String</code> representation.
     * @return The <code>String</code> representation
     */
    @Override
    public synchronized String toString() {
        String str =                "\n\t Address | Metric | Interface | Next Hop ";
        str +=                      "\n\t---------|--------|-----------|----------";
        for(int address:  entries.keySet()) {
            TableEntry entry = entries.get(address);
            str += String.format(   "\n\t   %3d   |   %3d  |   if%3d   |    %3d   ",
                address, entry.metric, entry.iface, entry.nextHop);
        }
        return str;
        
    }
    
    
    
    /**
     * Convert the <code>RoutingTable</code> to a series of RIP packet entries and
     * generate a <code>RIPPacket</code> containing them.
     * @param routerId The router's router iD
     * @param iface The interface the packet will be sent on
     * @return The resulting <code>RIPPacket</code>
     */
    public synchronized RIPPacket toRIPPacket(int routerId, int iface) {
        RIPPacket update = new RIPPacket(RIPPacket.COMMAND_RESPONSE, (short) routerId);
        for(int addr: entries.keySet()) {
            TableEntry entry = entries.get(addr);
            if(16 == entry.metric) {
                continue;
            } else if(entry.iface != iface) { // Split Horizon (don't send updates on the interface they were learned from)
                update.addEntry(new RIPEntry(entry.address, entry.metric));
            }
        }
        update.addEntry(new RIPEntry(routerId, 0));
        return update;
    }
    
    
    /**
     * The <code>TableEntry</code> class represents an entry in the routing table.
     */
    public class TableEntry {
        
        private final int address;
        private int metric;
        private final int iface;
        private final int nextHop;
        private TimerTask timerTask = null;
        
        
        
        /**
         * Construct a <code>TableEntry</code>
         * @param address Destination address of the entry
         * @param metric Metric of the entry
         * @param iface Interface of the entry
         * @param nextHop Next hop of the entry
         */
        private TableEntry(int address, int metric, int iface, int nextHop) {
            this.address = address;
            this.metric = metric;
            this.iface = iface;
            this.nextHop = nextHop;
        }
        
        
        
        /**
         * Get the entry destination address
         * @return The address
         */
        public int getAddress() { return address; }
        
        
        
        /**
         * Get the entry metric
         * @return The metric
         */
        public int getMetric() { return metric; }
        
        
        
        /**
         * Get the entry interface
         * @return The interface
         */
        public int getIface() { return iface; }
        
        
        
        /**
         * Get the entry next hop
         * @return The next hop
         */
        public int getNextHop() { return nextHop; }
        
        
        
        /**
         * Reset the entry timeout
         */
        private void resetTimeout() {
            if(timerTask != null) timerTask.cancel();
            timerTask = new TimeoutTask(this);
            service.timer.schedule(timerTask, RIPService.TIMEOUT_LENGTH);
            service.timer.purge();
        }
        
        
        
        /**
         * Set the Garbage Collection timer
         */
        private void setGCTimer() {
            metric = METRIC_INFINITE;
            if(timerTask != null) timerTask.cancel();
            timerTask = new GarbageCollectionTask(this);
            service.timer.schedule(timerTask, RIPService.GC_LENGTH);
        }
        
        
    }
    
    
    
    /**
     * The <code>Neighbor</code> class represents an entry in the neighbors table.
     */
    public class Neighbor {
        
        private final int address;
        private final int iface;
        private NeighborTimeoutTask timerTask = null;
        
        
        
        /**
         * Construct a <code>Neighbor</code>.
         * @param address Address of the neighbor
         * @param iface Interface connecting to the neighbor
         */
        private Neighbor(int address, int iface) {
            this.address = address;
            this.iface = iface;
        }
        
        
        
        /**
         * Get the address of the neighbor
         * @return The neighbors address
         */
        public int getAddress() { return address; }
        
        
        
        /**
         * Get the interface connecting to the neighbor
         * @return The connected interface
         */
        public int getIface() { return iface; }
        
        
        
        /**
         * Reset the neighbor entry timeout
         */
        private void resetTimeout() {
            if(timerTask != null) timerTask.cancel();
            timerTask = new NeighborTimeoutTask(this);
            service.timer.schedule(timerTask, RIPService.NEIGHBOR_TIMEOUT_LENGTH);
            service.timer.purge();
        }
    }
    
    
    
    
    
    /**
     * The <code>TimeoutTask</code> class represents the task executed when the 
     * routing table entry timeout expires.
     */
    private class TimeoutTask extends TimerTask {

        private final TableEntry entry;
        
        
        
        /**
         * Construct a <code>TimeoutTask</code>.
         * @param entry Routing table entry that the task is associated with
         */
        private TimeoutTask(TableEntry entry) {
            this.entry = entry;
        }
        
        
        
        /**
         * Run the task
         */
        @Override
        public void run() {
            synchronized(RoutingTable.this) {
                if(entry.timerTask != this) return;
                entry.setGCTimer();
            }
        }
        
    }
    
    
    
    
    
    /**
     * The <code>GarbageCollectionTask</code> class represents the task executed
     * when the routing table entry garbage collection timer expires.
     */
    private class GarbageCollectionTask extends TimerTask {

        private final TableEntry entry;
        
        
        
        /**
         * Construct a <code>GarbageCollectionTask</code>.
         * @param entry Routing table entry that the task is associated with
         */
        private GarbageCollectionTask(TableEntry entry) {
            this.entry = entry;
        }
        
        
        
        /**
         * Run the task
         */
        @Override
        public void run() {
            synchronized(RoutingTable.this) {   
                if(entry.timerTask != this) return;
                entries.remove(entry.address);
            }
        }
        
    }
    
    
    
    
    
    /**
     * The <code>NeighborTimeoutTask</code> class represents the task executed 
     * when the neighbor table entry timeout expires.
     */
    private class NeighborTimeoutTask extends TimerTask {

        private final Neighbor neighbor;
        
        
        
         /**
         * Construct a <code>NeighborTimeoutTask</code>.
         * @param neighbor Neighbor table entry that the task is associated with
         */
        private NeighborTimeoutTask(Neighbor neighbor) {
            this.neighbor = neighbor;
        }
        
        
        
        /**
         * Run the task
         */
        @Override
        public void run() {
            synchronized(RoutingTable.this) {
                if(neighbor.timerTask != this) return;
                neighbors.remove(neighbor.address);
                service.sendDelete(neighbor.address);
                for(int address: entries.keySet()) {
                    TableEntry entry = entries.get(address);
                    if(neighbor.address == entry.nextHop) {
                        entry.setGCTimer();
                    }
                }
                
            }
            
        }
        
        
        
    }
    
    
}
