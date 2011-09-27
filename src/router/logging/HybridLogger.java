/**
 * HybridLogger
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
package router.logging;

/**
 * The <code>HybridLogger</code> class provides a logger that outputs to 
 * multiple destinations.
 * @param <A> The type of the first component logger
 * @param <B> The type of the second component logger
 */
public class HybridLogger<A extends Logger,B extends Logger> extends Logger {

    private final A loggerA;
    private final B loggerB;
    
    
    
    /**
     * Create a <code>HybridLogger</code>.
     * @param loggerA The first component logger
     * @param loggerB The second component logger
     */
    public HybridLogger(A loggerA, B loggerB) {
        super(loggerA.getRouterId());
        if(loggerA.getRouterId() != loggerB.getRouterId()) throw new IllegalArgumentException("Router ID's don't match.");
        
        this.loggerA = loggerA;
        this.loggerB = loggerB;
    }
    
    
    
    /**
     * Get the first component logger
     * @return The first component logger
     */
    public A getLoggerA() {
        return loggerA;
    }
    
    
    
    /**
     * Get the second component logger
     * @return The second component logger
     */
    public B getLoggerB() {
        return loggerB;
    }
    
    
    
    /**
     * Write to the logger
     * @param str The string to write
     */
    @Override
    public synchronized void write(String str) {
        loggerA.write(str);
        loggerB.write(str);
    }
    
    
    
    /**
     * Close the logger
     */
    @Override
    public void close() {
        loggerA.close();
        loggerB.close();
    }
}

