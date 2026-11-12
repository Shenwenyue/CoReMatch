package CoReMatch.esp.bridges.nat;

import org.kohsuke.MetaInfServices;
import CoReMatch.esp.api.bridge.EPBridge;
import CoReMatch.esp.api.bridge.EPBridgeFactory;

@MetaInfServices
public class NativeBridgeFactory implements EPBridgeFactory {

    @Override
    public EPBridge createBridge() {
        return new NativeBridge();
    }

    @Override
    public String toString() {
        return "NativeBridge Factory";
    }


}
