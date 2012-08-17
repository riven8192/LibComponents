/*
 * Created on 23 jun 2009
 */

package craterstudio.net;

import java.net.InetAddress;

public interface BroadcastDiscoveryListener
{
   public void discoveredBroadcast(String serviceName, InetAddress address);
}
