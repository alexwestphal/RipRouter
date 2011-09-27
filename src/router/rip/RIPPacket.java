/**
 * RIPPacket
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
package router.rip;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import router.rip.RIPPacket.RIPEntry;

/**
 * The <code>RIPPacket</code> class represents a RIP packet consisting of a 
 * command, version source and up to 25 RIP Entries.
 * <p>
 * Format:
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  command (1)  |  version (1)  |          source (2)           |
 * +---------------+---------------+---------------+---------------+
 * |                                                               |
 * ~                         RIP Entry (20)                        ~
 * |                                                               |
 * +---------------+---------------+---------------+---------------+
 * 
 */
public class RIPPacket implements Iterable<RIPEntry> {
    
    /**
     * The value of the command field in RIP request packets.
     */
    public static final byte COMMAND_REQUEST    = 1;
    
    /**
     * The value of the command field in RIP response packets.
     */
    public static final byte COMMAND_RESPONSE   = 2;
    
    /**
     * The value of the version field in RIP packets.
     */
    public static final byte VERSION = 2;
    
    
    
    private final byte command;
    private final byte version;
    private final short source;
    private final List<RIPEntry> entries;
    
    
    
    /**
     * Construct a <code>RIPPacket</code> from the specified command and source router ID.
     * @param command Command to construct the packet with
     * @param source Source router ID to construct the packet with
     */
    public RIPPacket(byte command, short source) {
        
        if(!(COMMAND_REQUEST == command || COMMAND_RESPONSE == command)) throw new IllegalArgumentException("Invalid command value ("+command+")");
        if(source <= 0) throw new IllegalArgumentException("Invalid source value ("+source+")");
        
        this.command = command;
        this.version = VERSION;
        this.source = source;
        this.entries = new ArrayList<RIPEntry>();
    }
    
    
    
    /**
     * Construct a <code>RIPPacket</code> from the specified bytes.
     * @param bytes Array of bytes to construct the entry from
     * @throws router.RIPPacket.InvalidRIPPacketException If a valid 
     * <code>RIPPacket</code> can't be constructed from the bytes
     */
    public RIPPacket(byte[] bytes) throws InvalidRIPPacketException {
        // Check length of packet
        if(4 != bytes.length % 20) throw new InvalidRIPPacketException("Invalid Packet Length");
        
        this.command = bytes[0];
        this.version = bytes[1];
        this.source = ((short) ((bytes[2]<<8) + bytes[3]));
        
        if(!(COMMAND_REQUEST == command || COMMAND_RESPONSE == command)) throw new InvalidRIPPacketException("Invalid command value ("+command+")");
        if(VERSION != version) throw new InvalidRIPPacketException("Invalid version value ("+version+")");
        
        entries = new ArrayList<RIPEntry>((bytes.length-4) / 20);
        
        for(int i=4; i<bytes.length; i+=20 ) entries.add(new RIPEntry(bytes, i));
        
    }
    
    
    /**
     * Add an entry to the packet
     * @param entry Then entry to add
     */
    public void addEntry(RIPEntry entry) {
        if(entries.size() >= 25) throw new IllegalStateException("Entry limit per packet is 25");
        entries.add(entry);
    }
    
    
    
    /**
     * Get the entry iterator
     * 
     * (Note: the entries can't be modified from the iterator)
     * @return The entry iterator
     */
    @Override
    public Iterator<RIPEntry> iterator() {
        final Iterator<RIPEntry> iter = entries.iterator();
        
        return new Iterator<RIPEntry>() {
            @Override
            public boolean hasNext() { return iter.hasNext(); }
            @Override
            public RIPEntry next() { return iter.next(); }
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
    
    
    
    /**
     * Get the command represented by this packet.
     * @return The command
     */
    public byte getCommand() { return command; }
    
    
    
    /**
     * Get the source router ID of this packet.
     * @return The source router ID
     */
    public short getSource() { return source; }
    
    
    
    /**
     * Convert the <code>RIPPacket</code> to bytes.
     * @return A byte array representation of the packet
     */
    public byte[] getBytes() {
        
        byte[] response = new byte[(entries.size()*20)+4];
        response[0] = command;
        response[1] = version;
        response[2] = ((byte) ((source&0x0000ff00)>> 8));
        response[3] = ((byte) (source&0x000000ff));
        
        for(int i=0; i<entries.size(); i++) entries.get(i).toBytes(response, (i*20)+4);
        
        return response;
    }
    
    
    
    /**
     * Check if the specified bytes are zero
     * @param bytes Bytes to check
     * @param start position within the array to start checking
     * @param len number of bytes to check
     * @return <code>true</code> if all the checked bytes are zero, 
     * <code>false</code> otherwise 
     */
    private static boolean checkZero(byte[] bytes, int start, int len) {
        
        for(int i=start; i<start+len; i++) 
            if(bytes[i] != 0) return false;
        return true;
    }
    
    
    
    
    /**
     * The <code>RIPEntry</code> class represents an entry in a RIP packet and
     * consists of an address and a metric.
     * <p>
     * Format:
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                        must be zero (4)                       |
     * +---------------+---------------+---------------+---------------+
     * |                        IPv4 address (4)                       |
     * +---------------+---------------+---------------+---------------+
     * |                        must be zero (4)                       |
     * +---------------+---------------+---------------+---------------+
     * |                        must be zero (4)                       |
     * +---------------+---------------+---------------+---------------+
     * |                           metric (4)                          |
     * +---------------+---------------+---------------+---------------+
     * 
     */
    public static class RIPEntry {
        
        private final int address;
        private final int metric;
        
        
        
        /**
         * Construct a <code>RIPEntry</code> from an address and metric.
         * @param address Address to construct the entry with
         * @param metric Metric to construct the entry with 
         */
        public RIPEntry(int address, int metric) {
            if(address <= 0) throw new IllegalArgumentException("address must be > 0");
            if(metric < 0 || metric > 16) throw new IllegalArgumentException("metric must be >= 0 and < 16");
            
            this.address = address;
            this.metric = metric;
        }
        
        
        
        /**
         * Construct a <code>RIPEntry</code> from the specified bytes.
         * @param bytes Array of bytes to construct the entry from
         * @param offset Offset within the array to start from
         * @throws router.RIPPacket.InvalidRIPPacketException If a valid 
         * <code>RIPEntry</code> can't be constructed from the bytes
         */
        private RIPEntry(byte[] bytes, int offset) throws InvalidRIPPacketException {
            if(!checkZero(bytes, offset, 4)) throw new InvalidRIPPacketException("Bytes offset+0 to offset+3 must be 0");
            
            address = (bytes[offset+4]<<24) + (bytes[offset+5]<<16) + (bytes[offset+6]<<8) + bytes[offset+7];
            
            if(!checkZero(bytes, offset+8, 8)) throw new InvalidRIPPacketException("Bytes offset+8 to offset+16 must be 0");
            
            metric = (bytes[offset+16]<<24) + (bytes[offset+17]<<16) + (bytes[offset+18]<<8) + bytes[offset+19];
        }
        
        
        
        /**
         * Get the address represented by this entry
         * @return The address
         */
        public int getAddress() { return address; }
        
        
        
        /**
         * Get the metric represented by this entry
         * @return The metric
         */
        public int getMetric() { return metric; }
        
        
        
        /**
         * Convert the <code>RIPEntry</code> to bytes and put in the specified array.
         * @param bytes The array in which to put the bytes
         * @param offset The offset within the array at which to put the bytes
         */
        private void toBytes(byte[] bytes, int offset) {
            
            bytes[offset+4] = ((byte) ((address&0xff000000)>>24));
            bytes[offset+5] = ((byte) ((address&0x00ff0000)>>16));
            bytes[offset+6] = ((byte) ((address&0x0000ff00)>>8));
            bytes[offset+7] = ((byte) (address&0x000000ff));
            
            
            bytes[offset+16] = ((byte) ((metric&0xff000000)>>24));
            bytes[offset+17] = ((byte) ((metric&0x00ff0000)>>16));
            bytes[offset+18] = ((byte) ((metric&0x0000ff00)>>8));
            bytes[offset+19] = ((byte) (metric&0x000000ff));
            
        }
        
        
    }
    
    
    
    
    
    /**
     * The <code>InvalidRIPPacketException</code> represents an error related to
     * an invalid RIP packet.
     */
    public static class InvalidRIPPacketException extends Exception {
        
        
        
        /**
         * Construct an <code>InvalidRIPPacketException</code>.
         * @param message The reason the packet is invalid.
         */
        private InvalidRIPPacketException(String message) {
            super(message);
        }
    }
    
}
