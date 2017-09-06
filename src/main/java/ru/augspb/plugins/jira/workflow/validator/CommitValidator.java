package ru.augspb.plugins.jira.workflow.validator;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.workflow.WorkflowActionsBean;
import com.atlassian.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.loader.ActionDescriptor;

import java.util.List;
import java.util.Map;

public class CommitValidator implements Validator
{
    private static final Logger log = LoggerFactory.getLogger(CommitValidator.class);
    private final ValidatorUtils validatorUtils;
    private final I18nHelper i18n;
    private final WorkflowActionsBean workflowActionsBean = new WorkflowActionsBean();
    public static final String FIELD_WORD = "word";

    public CommitValidator() {
        validatorUtils = new ValidatorUtils();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        i18n = ComponentAccessor.getI18nHelperFactory().getInstance(user);
    }

    public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException {
        // hack: better way is create a settings menu
        String exceptedComponents = "Documentation";

        Issue issue = (Issue) transientVars.get("issue");
        List<Resolution> protectedResolutions = validatorUtils.getResolutions(args);

        SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        // check for the existence of a open PR
        boolean checker = checkOpenPullRequest(searchService, user, issue, protectedResolutions);
        if (!checker) { throw getException(transientVars, IssueFieldConstants.RESOLUTION,
                "commit-validator.missing.pullrequests"); }

        // exclude Documentation component
        log.info(issue.getComponents().toString());
        if (issue.getComponents().size() == 1)
            if (issue.getComponents().toString().contains(exceptedComponents)) return;


        //issue.getSubTaskObjects()
        if (issue.getSubTaskObjects().size() >=1) {
            long timespent = logTimeOnSubTasks(issue);
            if (timespent > 0){ return; }
            else { throw new InvalidInputException("Don't forget log work on subtask issues!");}
        }


        // check for the existence of a commits
        checker = checkExistCommits(searchService, user, issue, protectedResolutions);
        if (!checker) { throw getException(transientVars, IssueFieldConstants.RESOLUTION,
                "commit-validator.missing.commits"); }
    }
    private boolean checkExistCommits(SearchService searchService, ApplicationUser user,Issue issue, List<Resolution> protectedResolutions){
        String jqlQuery = String.format("issue in (%s) AND issue.property[development].commits > 0", issue.getKey());
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
        long resultsCount = 0;

        if (parseResult.isValid()) {
            try {
                Query query = parseResult.getQuery();
                resultsCount = searchService.searchCount(user, query);
                if (protectedResolutions.contains(issue.getResolution())) {
                    if (resultsCount == 0){
                        return false;
                    }
                }
            } catch (SearchException e) {
                log.error("Error running search", e);
            }
        } else {
            log.warn("Error parsing jqlQuery: " + parseResult.getErrors());
        }
        return true;
    }

    private boolean checkOpenPullRequest(SearchService searchService, ApplicationUser user,Issue issue, List<Resolution> protectedResolutions){
        String jqlQuery = String.format("issue in (%s) AND issue.property[development].openprs = 0 ", issue.getKey());
        SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
        long resultsCount = 0;
        if (parseResult.isValid()) {
            try {
                Query query = parseResult.getQuery();
                resultsCount = searchService.searchCount(user, query);
                if (protectedResolutions.contains(issue.getResolution())) {
                    if (resultsCount == 0) {
                        return false;
                    }
                }
            } catch (SearchException e) {
                log.error("Error running search", e);
            }
        } else {
            log.warn("Error parsing jqlQuery: " + parseResult.getErrors());
        }

        return true;
    }

    private long logTimeOnSubTasks(Issue issue) {
        //log.debug("CountingOnSubtask")
        long timeSpent = 0;
        for (Issue subTask : issue.getSubTaskObjects()) {
            //log.debug("found subtasks "+subTask)
            // Time on subtask linked (parent/child or epic) to stream will be counted on stream directly
            Long childTimeSpent = subTask.getTimeSpent();
            if (childTimeSpent != null) {
                timeSpent += childTimeSpent.longValue();
            }

        }
        return timeSpent;
    }

    private InvalidInputException getException(Map transientVars, String field, String key) {
        InvalidInputException invalidInputException = new InvalidInputException();

        if (isFieldOnScreen(transientVars, field)) {
            invalidInputException.addError(field, i18n.getText(key));
        } else {
            invalidInputException.addError(i18n.getText(key));
        }
        return invalidInputException;
    }

    private boolean isFieldOnScreen(Map transientVars, String field) {
        if (transientVars.containsKey("descriptor") && transientVars.containsKey("actionId")) {
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) transientVars.get("descriptor");
            Integer actionId = (Integer) transientVars.get("actionId");
            ActionDescriptor actionDescriptor = workflowDescriptor.getAction(actionId.intValue());
            FieldScreen fieldScreen = workflowActionsBean.getFieldScreenForView(actionDescriptor);
            if (fieldScreen != null) {
                for (FieldScreenTab tab : fieldScreen.getTabs()) {
                    for (FieldScreenLayoutItem fieldScreenLayoutItem : tab.getFieldScreenLayoutItems()) {
                        if (field.equals(fieldScreenLayoutItem.getFieldId())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
