/**
 * ConfigurationParser
 * Copyright (c) 2011, Alex Westphal. All rights reserved
 * @author Alex Westphal
 * @version 14-Sep-2011
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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import router.Configuration.Link;

/**
 * The <code>ConfigurationParser</code> parses configuration files into configurations.
 */
public class ConfigurationParser {

    
    
    /**
     * Filename format for standard configuration files.
     */
    public static final String FILENAME_FORMAT = "conf/router-%d.cfg";
    
    
    
    private final Logger log;
    
    
    /**
     * Create a <code>ConfigurationParser</code> wit the specified logger.
     * @param log The logger to use
     */
    public ConfigurationParser(Logger log) {
        this.log = log;
    }
    
    
    
    /**
     * Get the <code>Configuration</code> for the specified router ID.
     * (Looks for the config file with the specified router ID)
     * @param routerId Router ID for which to find the configuration
     * @return The Configuration corresponding th the specified router ID
     */
    public Configuration getConfiguration(int routerId) {
        return getConfiguration(String.format(FILENAME_FORMAT, routerId));
    }
    
    
    
    /**
     * Get the <code>Configuration</code> from the specified file.
     * @param configFileName Name of the config file
     * @return  The Configuration read from the file
     */
    public Configuration getConfiguration(String configFileName) {
        Scanner scanner;
        try {
            scanner = new Scanner(new File(configFileName));
        } catch(FileNotFoundException ex) {
            log.write("Config File Not Found");
            return null;
        }
        
        int lineNum = 0;
        String routerName = "";
        int routerId = 0;
        int[] adminPorts = new int[0];
        Link[] links = new Link[0];
        
        try {
            while(scanner.hasNextLine()) {
                lineNum++;
                String line = scanner.nextLine();
                if(line.startsWith("#")) continue; // Ignore comment lines
                
                String[] parts = line.split(" ");
                
                if(parts[0].equals("admin-ports")) {
                    adminPorts = parsePorts(parts);
                    
                } else if(parts[0].equals("links")) {
                    links = new Link[parts.length-1];
                    for(int i=0; i<links.length; i++) {
                        links[i] = parseLink(parts[i+1]);
                    }
                    
                } else if(parts[0].equals("router-id")) {
                    if(parts.length > 1) try {
                        routerId = Integer.parseInt(parts[1]);
                    } catch(NumberFormatException ex) {
                        throw new ConfigurationParserException("'"+parts[1]+"' can't be convert to an integer.");
                    }
                    else throw new ConfigurationParserException("No router-id specified");
                    
                } else if(parts[0].equals("router-name")) {
                    if(parts.length > 1) routerName = parts[1];
                    else throw new ConfigurationParserException("No router-name specified");
                    
                } else {
                    throw new ConfigurationParserException("Unknown configuration option: "+parts[0]);
                }
            }
            
            log.write("Configuration Built from "+configFileName);
            return new Configuration(configFileName, routerId, routerName, adminPorts, links);
        } catch(ConfigurationParserException ex) {
            log.write("Configuration Error (line "+lineNum+"): "+ex.getMessage());
            return null;
        }
    }
    
    
    
    
    /**
     * Parse the the given <code>String</code> array into an array of integers
     * ignoring the first element.
     * @param parts String array to parse
     * @return array of integers
     * @throws ConfigurationParserException 
     */
    private int[] parsePorts(String[] parts) throws ConfigurationParserException {
        int[] result = new int[parts.length-1];
        for(int i=0; i<result.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i+1]);
            } catch(NumberFormatException ex) {
                throw new ConfigurationParserException("'"+parts[i+1]+"' can't be convert to an integer.");
            }
        }
        return result;
    }
    
    
    
    /**
     * Parse the given <code>String<code> into an <code>Link</code>.
     * <p>
     * (Performs the exact inverse of toString() )
     * @param str The <code>String</code> to parse
     * @return The resulting <code>Link</code>
     * @throws ConfigurationException If the <code>String<code> can't be parsed
     * as an <code>Link</code>.
     */
    public static Link parseLink(String str) throws ConfigurationParserException {
        String[] parts = str.split("-");
        int inputPort, outputPort, metric;

        if(parts.length != 3) throw new ConfigurationParserException("Can't parse Link from string ("+str+")");
        try {
            inputPort = Integer.parseInt(parts[0]);
            outputPort = Integer.parseInt(parts[1]);
            metric = Integer.parseInt(parts[2]);
        } catch(NumberFormatException ex) {
            throw new ConfigurationParserException("Can't parse Link from string ("+str+")");
        }

        return new Link(inputPort, outputPort, metric);
    }
    
    
    
    
    
    /**
     * The <code>ConfigurationParserException</code> class represents an error that 
     * occurred while parsing the building the <code>Configuration</code>.
     */
    public static class ConfigurationParserException extends Exception {
        
        
        
        /**
         * Create a <code>ConfigurationParserException</code> with the specified message.
         * @param message The message to associate with the Exception. 
         */
        private ConfigurationParserException(String message) {
            super(message);
        }
    }
    
    
    
}
