/**
 * Configuration
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

import router.logging.Logger;
import java.util.Arrays;

/**
 * The <code>Configuration</code> class parses, stores and validate a configuration
 * for a particular router.
 */
public class Configuration {
    
    
    /**
     * The minimum allowed port number
     */
    public static final int MIN_PORT_NUMBER = 1024;
    
    /**
     * The maximum allowed port number
     */
    public static final int MAX_PORT_NUMBER = 65535;

    private final String configFileName;
    private final String routerName;
    private final int routerId;
    private final int[] adminPorts;
    private final Link[] links;
    

    
    /**
     * Construct a <code>Configuration</code> from the file specified by
     * the given filename.
     * @param configFileName The filename of the file from which to load the <code>Configuration</code>
     */
    protected Configuration(String configFileName, int routerId, String routerName, 
            int[] adminPorts, Link[] links) {
        
        this.configFileName = configFileName;
        this.routerId = routerId;
        this.routerName = routerName;
        this.adminPorts = adminPorts;
        this.links = links;
        
    }
    
    
    
    /**
     * Get the name of the config file that the <code>Configuration</code> was
     * created from.
     * @return The config file name
     */
    public String getConfigFileName() {
        return configFileName;
    }
    
    
    
    /**
     * Get the router-name.
     * @return The router-name
     */
    public String getRouterName() {
        return routerName;
    }
    
    
    
    /**
     * Get the router-id.
     * @return The router-id
     */
    public int getRouterId() {
        return routerId;
    }
    
    
    
    /**
     * Get the list of admin-ports.
     * @return The admin ports
     */
    public int[] getAdminPorts() {
        return adminPorts;
    }
    
    
    
    /**
     * Get the list of links
     * @return The links
     */
    public Link[] getLinks() {
        return links;
    }
    

    
    /**
     * Check that the supplied port numbers are valid.
     * @param log <code>Logger</code> to use.
     * @return <code>true</code> if the port numbers are all valid, <code>false</code> otherwise
     */
    public boolean validate(Logger log) {
        boolean result = true;
        if(routerId <= 0) {
            log.write("Invalid Configuration: routerId="+routerId+" (< 1)");
            result = false;
        }
        for(int port: adminPorts) { 
            if(!(port >= MIN_PORT_NUMBER && port < MAX_PORT_NUMBER)) {
                log.write("Invalid Configuration: Admin port ("+port+") is out of range.");
                result = false;
            }
        }
        
        for(Link link: links) {
            if(!link.validate()) {
                log.write("Invalid Configuration: Link ("+link+") is invalid");
                result = false;
            }
        }
        return result;
    }
    
    
    
    /**
     * Convert the <code>Configuration<code> to a <code>String</code>.
     * @return A <code>String</code> representation if the configuration
     */
    @Override
    public String toString() {
        return String.format(
            "Configuration(router-name=%s, router-id=%d, admin-ports=%s, links=%s)",
            routerName, routerId, Arrays.toString(adminPorts), Arrays.toString(links)
            );
    }

    
    
    
    
    /**
     * The <code>Link</code> class represents two ports and a metric that is 
     * defined in a configuration file.
     */
    public static class Link {
        
        private final int inputPort;
        private final int outputPort;
        private final int metric;
        
        
        /**
         * Construct an <code>Link</code> from the given input-port, output-port 
         * and metric combination.
         * @param inputPort Input port for the link
         * @param outputPort Output port for the link 
         * @param metric Metric of the link
         */
        public Link(int inputPort, int outputPort, int metric) {
            this.inputPort = inputPort;
            this.outputPort = outputPort;
            this.metric = metric;
        }
        
        
        
        /**
         * Get the link's input port
         * @return The input port
         */
        public int getInputPort() { return inputPort; }
        
        
        
        /**
         * Get the link's output port
         * @return The output port
         */
        public int getOutputPort() { return outputPort; }
        
        
        
        /**
         * Get the link's metric
         * @return The metric
         */
        public int getMetric() { return metric; }
        
        
        
        /**
         * Check if the ports and metric are valid.
         * @return <code>true</code> if the link is valid, <code>false</code> otherwise.
         */
        public boolean validate() {
            return inputPort >= MIN_PORT_NUMBER && inputPort <= MAX_PORT_NUMBER &&
                    outputPort >= MIN_PORT_NUMBER && outputPort <= MAX_PORT_NUMBER && 
                    metric > 0 && metric <= 16;
        }
        
        
        
        /**
         * Convert the <code>LInk</code> to a <code>String</code>.
         * @return A <code>String</code> representation of the link.
         */
        @Override
        public String toString() {
            return String.format("%d-%d-%d", inputPort, outputPort, metric);
        }
        
        
        
        /**
         * Equality operator for <code>Link</code>'s
         * Two <code>Link</code>'s are equal if and only if they have the same 
         * input-port, output-port and metric.
         * @param other The other <code>Link</code> to compare against.
         * @return <code>true</code> if the objects are equal, <code>false</code> otherwise
         */
        @Override
        public boolean equals(Object other) {
            if(!(other instanceof Link)) return false;
            Link otherLink = (Link) other;
            return otherLink.inputPort == this.inputPort &&
                    otherLink.outputPort == this.outputPort &&
                    otherLink.metric == this.metric;
        }

        
        
        /**
         * Compute the hashcode for the link
         * @return The link's hashcode
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + this.inputPort;
            hash = 59 * hash + this.outputPort;
            hash = 59 * hash + this.metric;
            return hash;
        }
        
        
        
        
    }
    
    
    
    
    
    
	
	
}
