package ru.augspb.plugins.jira.workflow.validator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ResolutionManager;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@JiraComponent
public class ValidatorUtils {

    public static final String AVAILABLE_RESOLUTIONS = "availableResolutions";
    public static final String SUBMITTED_RESOLUTIONS = "submittedResolutions";
    public static final String SELECTED_RESOLUTIONS = "selectedResolutions";
    private final ResolutionManager resolutionManager;

    public ValidatorUtils() {
        this.resolutionManager = ComponentAccessor.getComponent(ResolutionManager.class);
    }

    public List<Resolution> getResolutions(Map<String, ?> args) {
        List<Resolution> ret = new ArrayList<Resolution>();
        String resolutions = (String) args.get(SUBMITTED_RESOLUTIONS);
        if (resolutions != null) {
            for (String resolution : resolutions.split("\\|")) {
                ret.add(resolutionManager.getResolutionByName(resolution));
            }
        }

        Collections.sort(ret, new ResolutionComparator());
        return ret;
    }

    public List<Resolution> getAllResolutions() {
        return resolutionManager.getResolutions();
    }

    public String resolutionsAsString(List<Resolution> resolutions) {
        StringBuilder sb = new StringBuilder();
        for (Resolution resolution : resolutions) {
            sb.append(resolution.getName()).append("|");
        }
        return sb.toString();
    }

    private static class ResolutionComparator implements Comparator<Resolution> {
        public int compare(Resolution o1, Resolution o2) {
            return o1.getId().compareTo(o2.getId());
        }
    }
}