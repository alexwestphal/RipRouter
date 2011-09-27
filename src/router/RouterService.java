/**
 * RouterService
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

import router.logging.Logger;

/**
 * The <code>RouterService</code> class represents a runnable service provided 
 * by the router.
 */
public abstract class RouterService implements Runnable {

    private final Router router;
    
    
    
    /**
     * Create a <code>RouterService</code> for the specified router
     * @param router The router for which to built the service
     */
    public RouterService(Router router) {
        this.router = router;
    }
    
    
    
    /**
     * Get the logger for the router to which the service is attached
     * (Note: designed for use by subclasses only)
     * @return The router's logger
     */
    protected final Logger getLog() {
        if(null == router) throw new IllegalStateException("The logger for the router can't be obtained before the service is attached to a router.");
        return router.getLog();
    }
    
    
    
    /**
     * Get the router to which the service is attached
     * 
     * (Note: designed for use by subclasses only)
     * @return The router
     */
    protected final Router getRouter() {
        return router;
    }
    
    
    
    /**
     * Interrupt this service
     * 
     * (Should be overriden by subclasses that can self interrupt)
     */
    public void interrupt() {
        router.interruptService(this);
    }
    
    
    
    /**
     * Start the service
     * 
     * (Should be overriden by subclasses that need custom start routines)
     */
    public void doStart() {}
    
    
    
    /**
     * Start the service
     * 
     * (Should be overriden by subclasses that need custom stop routines)
     */
    public void doStop() {}
    
    
    
    /**
     * Run the service
     * 
     * (Note: This method is repeatedly called by the router so it shouldn't 
     * contain any infinite loops)
     */
    @Override
    public abstract void run();
}
