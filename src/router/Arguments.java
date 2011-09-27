/**
 * Arguments 
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
package router;

import java.util.HashMap;
import java.util.Map;

/**
 * The <code>Arguments</code> class parses and stores command line arguments.
 * 
 * The arguments are parsed according to the following diagram:
 *                                             +--------->-------+
 *                                             |                 |
 * -->--+-->----------------------------->--+--+->-[ROUTER_ID]->-+-->--
 *      |                                   |
 *      +-<-[OPTION_VALUE]--[OPTION_NAME]-<-+
 * 
 */
public class Arguments {

    private int routerId = 0;
    private String config = null;
    private String group = null;
    private int port = 0;
    private boolean validate = false;
    private boolean loud = false;
    private Map<String, String> map = new HashMap<String, String>();

    
    
    /**
     * Parse the provided arguments into an <code>Arguments</code> object.
     * @param args Arguments to parse
     * @throws ArgumentException If the parsing resulted in an error.
     */
    private Arguments(String[] args) throws ArgumentException {
        if(0 == args.length) throw new ArgumentException("Not enough arguments");

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {

                if (args[i].equals("-config")) {
                    if(args.length >= i) config = args[++i];
                    else throw new ArgumentException("-config option parameter missing");
                    
                } else if (args[i].equals("-group")) {
                    if(args.length >= i) group = args[++i];
                    else throw new ArgumentException("-group option parameter missing");
                    
                } else if (args[i].equals("-port")) {
                    if(args.length >= i) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch(NumberFormatException ex) {
                            throw new ArgumentException("'"+args[i]+"' can't be convert to an integer.");
                        }
                    } else throw new ArgumentException("-port option parameter missing");
                    
                } else if (args[i].equals("-validate")) {
                    validate = true;
                } else if (args[i].equals("-loud")) {
                    loud = true;
                } else {
                    throw new ArgumentException("Unknown option: '"+args[i]+"'");
                }

            } else {
                try {
                    routerId = Integer.parseInt(args[i]);
                    break;
                } catch (NumberFormatException ex) {
                    throw new ArgumentException("Unknown option: '"+args[i]+"'");
                }
            }
        }

    }

    
    
    /**
     * Get the specified router ID
     * @return The specified router ID
     * @throws IllegalStateException If the router-id was not specified
     */
    public int getRouterId() {
        if(hasRouterId()) return routerId;
        else throw new IllegalStateException("No router ID was specified.");
    }
    
    
    
    /**
     * Get the value of the config parameter.
     * @return The value of the config parameter
     * @throws IllegalStateException If the config option was not included
     */
    public String getConfig() {
        if(hasConfig()) return config;
        else throw new IllegalStateException("The -config option was not included");
    }
    
    
    
    /**
     * Get the value of the group parameter.
     * @return The value of the group parameter
     * @throws IllegalStateException If the group option was not included
     */
    public String getGroup() {
        if(hasGroup()) return group;
        else throw new IllegalStateException("The -group option was not included");
    }
    
    
    
    /**
     * Get the value of the port parameter
     * @return The value of the port parameter
     * @throws IllegalStateException If the port option was not included
     */
    public int getPort() {
        if(hasPort()) return port;
        else throw new IllegalStateException("The -port option was not included");
    }
    
    
    
    /**
     * Check if the router ID was specified
     * @return <code>true</code> if the router ID was specified, <code>false</code> otherwise
     */
    public boolean hasRouterId() { return routerId > 0; }
    
    
    
    /**
     * Check if the config option was included
     * @return <code>true</code> if the config option was included, <code>false</code> otherwise
     */
    public boolean hasConfig() { return null != config; }
    
    
    
    /**
     * Check if the group option was included
     * @return <code>true</code> if the group option was included, <code>false</code> otherwise
     */
    public boolean hasGroup() { return null != group; }
    
    
    
    /**
     * Check if the loud option was set
     * @return <code>true</code> if the loud option was set, <code>false</code> otherwise
     */
    public boolean hasLoud() { return loud; }
    
    
    
    /**
     * Check if the port option was included
     * @return <code>true</code> if the port option was included, <code>false</code> otherwise
     */
    public boolean hasPort() { return port > 0; }
    
    
    
    /**
     * Check if the validate option was set
     * @return <code>true</code> if the validate option was set, <code>false</code> otherwise
     */
    public boolean hasValidate() { return validate; }

    
    
    /**
     * Parse the provided arguments into an <code>Arguments</code> object.
     * @param args Arguments to parse
     * @throws ArgumentException If the parsing resulted in an error.
     */
    public static Arguments parse(String[] args) throws ArgumentException {
        return new Arguments(args);
    }
    
    
    
    
    
    /**
     * The <code>ArgumentException</code> class represents an error that occurred
     * while parsing the arguments.
     */
    public static class ArgumentException extends Exception {
        
        
        
        /**
         * Create an <code>ArgumentException</code> with the specified message.
         * @param message The message to associate with the Exception. 
         */
        private ArgumentException(String message) {
            super(message);
        }
    }
}
