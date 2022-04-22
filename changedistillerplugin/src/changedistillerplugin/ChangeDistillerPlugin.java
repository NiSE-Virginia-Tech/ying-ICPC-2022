package changedistillerplugin;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class ChangeDistillerPlugin extends Plugin{

	private static ChangeDistillerPlugin plugin;
	
	public ChangeDistillerPlugin() {
		plugin = this;
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static ChangeDistillerPlugin getDefault() {
		return plugin;
	}
}
