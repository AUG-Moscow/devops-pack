package ru.augspb.plugins.jira.workflow;
import ru.augspb.plugins.jira.workflow.validator.ValidatorUtils;
import static  ru.augspb.plugins.jira.workflow.validator.ValidatorUtils.*;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ValidatorDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommitValidatorFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginValidatorFactory {
    private final ValidatorUtils validatorUtils;

    public CommitValidatorFactory(ValidatorUtils validatorUtils) {
        this.validatorUtils = validatorUtils;
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        velocityParams.put(AVAILABLE_RESOLUTIONS, validatorUtils.getAllResolutions());
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;
        List<Resolution> selected = validatorUtils.getResolutions(validatorDescriptor.getArgs());
        List<Resolution> available = validatorUtils.getAllResolutions();
        available.removeAll(selected);

        velocityParams.put(AVAILABLE_RESOLUTIONS, available);
        velocityParams.put(SELECTED_RESOLUTIONS, selected);
        velocityParams.put(SUBMITTED_RESOLUTIONS, validatorUtils.resolutionsAsString(selected));
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;

        velocityParams.put(SUBMITTED_RESOLUTIONS, validatorUtils.getResolutions(validatorDescriptor.getArgs()));
    }


    public Map<String, ?> getDescriptorParams(Map<String, Object> validatorParams) {
        Map<String, String> params = new HashMap<String, String>(0);
        params.put(SUBMITTED_RESOLUTIONS, extractSingleParam(validatorParams, SUBMITTED_RESOLUTIONS));

        return params;
    }
}
