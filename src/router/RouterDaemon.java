/**
 * RouterDaemon 
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

import java.io.File;
import java.io.IOException;
import router.Arguments.ArgumentException;
import router.rip.RIPService;
import router.logging.Logger;
import router.logging.ConsoleLogger;

/**
 * 
 */
public class RouterDaemon {
    
    public static boolean loud = false;
    
    /**
     * Run the router daemon
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        Arguments arguments;
        try {
            arguments = Arguments.parse(args);
        } catch (ArgumentException ex) {
            System.out.println("Error: "+ex.getMessage());
            return;
        }
        
         Logger log = new ConsoleLogger();
        
        if(arguments.hasGroup()) {
            File dir = new File(arguments.getGroup());
            if(dir.isDirectory()) {
                for(String cfg: dir.list()) {
                    ProcessBuilder pb = new ProcessBuilder("./routerd", "-config", dir.getPath()+"/"+cfg);
                    try {
                        Process p = pb.start();
                    } catch (IOException ex) {
                        log.write("Error: (IOException) - "+ex.getMessage());
                    }
                }
            } else {
                log.write("'"+arguments.getGroup()+"' isn't a directory");
            }
            return;
        }
        
        if(arguments.hasLoud()) loud = true;
       
        ConfigurationParser configParser = new ConfigurationParser(log);
        Configuration config;
        if(arguments.hasConfig()) {
            config = configParser.getConfiguration(arguments.getConfig());
        } else if(arguments.hasRouterId()) {
            config = configParser.getConfiguration(arguments.getRouterId());
        } else {
            log.write("No usable config file found");
            return;
        }
        if(arguments.hasValidate()) {
            if(config.validate(log)) System.out.println("Configuration file is valid");
        }
        
        log = new ConsoleLogger(config.getRouterId());
        Router router = new Router(config, log);
        
        DatagramService ds = new DatagramService(router);
        RIPService rip = new RIPService(router, ds);
        VTYService vty = new VTYService(router, ds, rip);
        
        router.register(vty);
        router.register(rip);
        router.register(ds);
        router.start();
        
        router.join();
        
 
    }
    
}
