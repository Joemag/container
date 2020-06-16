package org.opentosca.container.api.dto.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opentosca.container.api.dto.ResourceSupport;

@XmlRootElement(name = "PlanResources")
public class PlanListDTO extends ResourceSupport {

    @JsonProperty
    @XmlElement(name = "Plan")
    @XmlElementWrapper(name = "Plans")
    private final List<PlanDTO> plans = new ArrayList<>();

    public List<PlanDTO> getPlans() {
        return this.plans;
    }

    public void add(final PlanDTO... plans) {
        this.plans.addAll(Arrays.asList(plans));
    }

    public void add(final Collection<PlanDTO> plans) {
        this.plans.addAll(plans);
    }
}
