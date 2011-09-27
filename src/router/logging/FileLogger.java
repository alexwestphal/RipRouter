/**
 * FileLogger
 * Copyright (c) 2011, Alex Westphal. All rights reserved
 * @author Alex Westphal
 * @version 13-Sep-2011
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * 
 * The <code>FileLogger</code> class provides a logger that outputs to a file
 */
public class FileLogger extends Logger {
	
    private BufferedWriter writer;
    private boolean isClosed = false;

    
    
    /**
     * Create a <code>FileLogger</code> with the specified router ID
     * @param routerId The router ID to use
     */
    public FileLogger(int routerId) {
        super(routerId);
        try {
            writer = new BufferedWriter(new FileWriter("logs/router-"+routerId+".log", true));
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
        write("File Logger created for router-"+routerId);

    }
    
    
    
    /**
     * Write to the logger
     * @param str The string to write
     */
    @Override
    public synchronized void write(String str) {
        String time = String.format("%tT : ", Calendar.getInstance());
        try {
            writer.write(time+str+"\n");
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    
    
    /**
     * Close the logger and the associated file
     */
    @Override
    public synchronized void close() {
        if(!isClosed) {
        try {
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
            isClosed = true;
        }
    }

    
    

}

