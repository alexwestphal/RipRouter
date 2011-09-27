/**
 * ConsoleLogger
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

import java.io.FileWriter;
import java.util.Calendar;

/**
 * The <code>ConsoleLogger</code> class provides a logger that outputs to the console.
 */
public class ConsoleLogger extends Logger {
	
    private FileWriter writer;
    private boolean isClosed = false;

    
    
    /**
     * Create a <code>ConsoleLogger</code> for initialisation 
     */
    public ConsoleLogger() {
        super(0);
        write("Console Logger created for initialisation.");
    }
    
    
    
    /**
     * Create a <code>ConsoleLogger</code> with the specified router ID
     * @param routerId The router ID to use
     */
    public ConsoleLogger(int routerId) {
        super(routerId);
        write("Console Logger created for router-"+routerId);
    }
    
    
    
    /**
     * Write to the logger
     * @param str The string to write
     */
    @Override
    public synchronized void write(String str) {
        String time = String.format("%tT : ", Calendar.getInstance());
        System.out.print(time+str+"\n");
    }

    
    
    /**
     * Close the logger and the associated file
     */
    @Override
    public synchronized void close() {}

    
    
    

}

