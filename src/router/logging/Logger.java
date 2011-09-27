/**
 * Logger
 * Copyright (c) 2011, Alex Westphal. All rights reserved
 * @author Alex Westphal
 * @version 26-Aug-2011
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package router.logging;

/**
 * The <code>Logger</code> class defines a general logger.
 */
public abstract class Logger {

    private final int routerId;
    
    /**
     * Create a <code>Logger</code> with the specified router ID
     * @param routerId The router ID to use
     */
    public Logger(int routerId) {
        this.routerId = routerId;
    }
    
    
    /**
     * Get the router ID of the logger
     * @return The router ID
     */
    public int getRouterId() {
        return routerId;
    }
    
    
    
    /**
     * Write to the logger
     * @param str The string to write
     */
    public abstract void write(String str);

    
    
    /**
     * close the logger
     */
    public abstract void close();

    
    
    /**
     * Cleanup the logger in case of garbage collection without being closed.
     * @throws Throwable 
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.close();
    }
    
    

}
