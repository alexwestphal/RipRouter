/**
 * Router
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
import java.util.HashMap;
import java.util.Map;

/**
 * The <code>Router</code> class provides the core router functionality and 
 * manages the services provided by the router.
 */
public class Router {
    
    
    private Map<RouterService, Thread> services = new HashMap<RouterService, Thread>();
    
    private Logger log;
    private final Configuration config;
    private final int routerId;
    private boolean running;
    
    
    
    /**
     * Create a <code>Router</code>
     * @param config The configuration to use for the router
     * @param log The logger to use for the router
     */
    public Router(Configuration config, Logger log) {
        this.config = config;
        this.routerId = config.getRouterId();
        this.log = log;
    }
    
    
    
    /**
     * Register a <code>RouterService</code> with the <code>Router</code>.
     * If the specified service is already registered this method does nothing.
     * Note: Can't be called while the <code>Router</code> is running.
     * @param service Service to register
     */
    public synchronized void register(final RouterService service) {
        if(services.containsKey(service)) return;
        if(running) throw new IllegalStateException("RouterService's can't be registered while the router is running.");
        
        services.put(service, new Thread() {
            
            { this.setName(service.getClass().getName()); }
            
            @Override
            public void run() {
                service.doStart();
                while(running) service.run();
                service.doStop();
            }
        });
    }
    
    
    
    /**
     * Deregister a <code>RouterService</code> with the <code>Router</code>.
     * If the specified service hasn't been registered this method does nothing.
     * Note: Can't be called while the <code>Router</code> is running.
     * @param service Service to deregister
     */
    public synchronized void deregister(RouterService service) {
        if(running) throw new IllegalStateException("RouterService's can't be deregistered while the router is running.");
        services.remove(service);
    }
    
    
    
    /**
     * Start the <code>Router</code>.
     */
    public synchronized void start() {
        if(running) return;
        running = true;
        
        log.write("Router Starting ("+config.getRouterName()+")...");
        for(RouterService service: services.keySet()) {
            services.get(service).start();
        }
        
    }
    
    
    
    /**
     * Stop the <code>Router</code>.
     */
    public synchronized void stop() {
        if(!running) return;
        running = false;
        
        log.write("Router Stoping...");
        for(RouterService service: services.keySet()) {
            service.interrupt();
        }
        
    }
    
    
    
    /**
     * Wait for all the registered services to die.
     */
    public void join() {
        for(RouterService service: services.keySet()) {
            Thread thread = services.get(service);
            if(thread.isAlive()) try { thread.join(); } catch (InterruptedException ex) {}
        }
    }
    
    
    
    /**
     * Interrupt a service
     * @param service The sevice to interupt
     */
    protected void interruptService(RouterService service) {
        Thread thread = services.get(service);
        if(thread.isAlive()) thread.interrupt();
    }
    
    
    
    /**
     * Check if the router is running
     * @return <code>true</code> if the router is running, <code>false</code> otherwise
     */
    public boolean isRunning() {
        return running;
    }
    
    
    
    /**
     * Get the current logger for this router
     * @return The current logger
     */
    public Logger getLog() {
        return log;
    }
    
    
    
    /**
     * Get the configuration for this router
     * @return The router's configuration
     */
    public Configuration getConfig() {
        return config;
    }
    
    
    
    /**
     * Get the router ID
     * @return The router ID
     */
    public int getRouterId() {
        return routerId;
    }
    
    
    
    /**
     * Set the router's logger
     * @param log The logger to set
     */
    public void setLog(Logger log) {
        if(this.log != null) this.log.close();
        this.log = log;
    }
    
    
    
    
}
