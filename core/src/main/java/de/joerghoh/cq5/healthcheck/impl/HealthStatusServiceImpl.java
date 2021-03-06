package de.joerghoh.cq5.healthcheck.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.joerghoh.cq5.healthcheck.HealthStatus;
import de.joerghoh.cq5.healthcheck.HealthStatusProvider;
import de.joerghoh.cq5.healthcheck.HealthStatusService;
import de.joerghoh.cq5.healthcheck.SystemHealthStatus;

/**
 * @author joerg
 */
@Component(metatype = true, immediate = true)
@Service(value = HealthStatusService.class)
@Reference(name = "healthStatusProvider", referenceInterface = HealthStatusProvider.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
public class HealthStatusServiceImpl implements HealthStatusService {

	private List<HealthStatusProvider> providers = new ArrayList<HealthStatusProvider>();
	private final Logger log = LoggerFactory
			.getLogger(HealthStatusServiceImpl.class);

	private static int DEFAULT_NUMBER_BUNDLES = 10;

	@Property
	private static String BUNDLE_NUMBER_THRESHOLD_PROP = "bundle.threshold";
	private int bundleNumberThreshold;

	/**
	 * Get overall status
	 */
	public SystemHealthStatus getOverallStatus() {

		int finalStatus = 0;
		List<HealthStatus> results = new ArrayList<HealthStatus>();
		String message = "";

		for (HealthStatusProvider p : providers) {
			HealthStatus s = p.getHealthStatus();
			if (s.getStatus() > finalStatus) {
				finalStatus = s.getStatus();
			}
			results.add(s);
		}

		// if not all requested services are available, go critical!
		if (results.size() < bundleNumberThreshold) {
			finalStatus = HealthStatusProvider.CRITICAL;
			message = "Only " + results.size() + " out of "
					+ bundleNumberThreshold + " monitoring services available";
		}
		log.info("Processed " + results.size() + " providers");
		return new SystemHealthStatus(finalStatus, results, message);
	}

	/** helper routines **/

	protected void bindHealthStatusProvider(HealthStatusProvider provider) {
		providers.add(provider);
	}

	protected void unbindHealthStatusProvider(HealthStatusProvider provider) {
		providers.remove(provider);
	}

	@Activate
	protected void activate(ComponentContext context) {
		Dictionary<?, ?> properties = context.getProperties();
		bundleNumberThreshold = PropertiesUtil.toInteger(
				properties.get(BUNDLE_NUMBER_THRESHOLD_PROP),
				DEFAULT_NUMBER_BUNDLES);
	}
}
